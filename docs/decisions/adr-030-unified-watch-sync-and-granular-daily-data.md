# ADR-030 — Sincronização unificada do relógio e dados diários granulares

- **Status:** accepted
- **Data:** 2026-09-11
- **Última revisão:** 2026-09-13
- **Relacionado:** ADR-010, ADR-012, ADR-029; Specs 016, 027, 032 e 033

## Contexto

Após a Spec 032, workouts D4 v4 possuem fatos nativos, distância temporal e
calibração contextual robusta. A linha diária, porém, continuava fragmentada:

- Home buscava FEA1;
- Saúde iniciava D1;
- Exercícios iniciava D3/D4;
- D1 historicamente persistia somente BPM;
- Home estimava distância/calorias a partir de passos e métricas pessoais;
- Home/D1 podiam tentar Health Connect automaticamente após persistência.

A inspeção do protocolo D1 do Champion C001 mostra que o pacote diário contém
fatos suficientes para passos, distância, calorias e BPM em granularidade de
aproximadamente cinco minutos.

A primeira versão desta decisão assumiu, de forma conservadora, que D1 seria
fonte somente fora de workouts e que todo bucket D1 que intersectasse D4 poderia
ser suprimido. A validação física pós-033-02/033-03 invalidou essa premissa.

Em dois dumps sanitizados do mesmo dia, `Σ D1.steps` fechou exatamente com FEA1
em duas leituras sucessivas. Em uma corrida D4 válida, os buckets D1
correspondentes fecharam exatamente em passos e distância e praticamente em
calorias contra o D4. Em outro workout com métricas locomotoras D4 nulas, um
bucket D1 temporalmente intersectante ainda continha passos, distância e
calorias. Portanto, interseção temporal não prova propriedade exclusiva do
conteúdo do bucket.

Evidência: `docs/integration/033-03-d1-v2-physical-validation.md`.

A validação física pós-033-05 trouxe uma segunda correção importante. O watch sync
unificado foi exercitado com refreshes consecutivos e permaneceu idempotente para
D1 e D3/D4. Em uma tentativa posterior, o D1 estava estruturalmente completo até
o último `totalPositions` recebido, mas FEA1 já estava à frente da soma D1 por uma
pequena cauda temporal entre o último bucket de aproximadamente cinco minutos e o
instante da leitura agregada FEA1.

Isso confirma que:

```text
FEA1 > ΣD1
```

não implica automaticamente divergência. FEA1 é o total diário atual; D1 é a
materialização temporal discreta e pode atrasar alguns minutos sem qualquer
lacuna estrutural.

Evidência: `docs/integration/033-05-unified-watch-sync-physical-validation.md`.

## Decisão

1. O pull-to-refresh da Home é o único entrypoint de produto para buscar dados do
   relógio.
2. Um `WatchSyncCoordinator` controla gate foreground, sessão, handshake e
   sequência FEA1 -> D1 -> D3/D4, preservando partial success por componente.
3. FEA1 permanece agregado diário local autoritativo de passos e sinal de
   validação, sem distribuição temporal inventada.
4. D1 usa parser/persistência de fatos diários completos (`d1-v2`), com BPM
   nullable e demais métricas preservadas quando BPM for inválido.
5. D1 é a linha granular diária do relógio e pode conter fatos produzidos durante
   workouts. D4 é a fonte detalhada/canônica do workout. Não existe regra global
   de "D1 fora / D4 dentro" nem supressão integral por mera interseção.
6. A autoridade é definida por finalidade e métrica:
   - passos diários: FEA1 é o total; D1 fornece distribuição granular; D4 não é
     somado novamente ao agregado diário;
   - passos/cadência do workout: D4 permanece autoritativo no contexto da
     atividade;
   - distância e calorias diárias: D1 fornece a linha nativa diária quando
     elegível; D4 fornece o valor do workout e não é somado cegamente ao D1;
   - frequência cardíaca: D4 pode substituir visualmente D1 dentro do workout
     quando houver cobertura D4 válida; sem cobertura válida, D1 é preservado.
7. Qualquer substituição/ajuste de contribuição D1 por D4 exige prova local
   determinística de cobertura e não duplicação. Sem prova, preservar D1 no
   diário e D4 no workout, sem somá-los.
8. A cobertura de passos diferencia estado estrutural de frescor:
   - `FEA1 == ΣD1` com D1 completo pode ser `COMPLETE`;
   - `FEA1 > ΣD1` com D1 estruturalmente completo e último bucket recente é
     **cauda recente** (`RECENT_TAIL`/`LAGGING` ou equivalente), não divergência;
   - `FEA1 < ΣD1`, D1 estruturalmente incompleto ou diferença não explicável por
     frescor/cobertura é divergência/incompletude e falha fechada conforme o
     consumidor.
9. A diferença `FEA1 - ΣD1` nunca vira bucket, intervalo, timestamp ou
   `StepsRecord` sintético. FEA1 não carrega distribuição temporal; somente um D1
   real posterior pode materializar a cauda.
10. Novas distâncias diárias não são reconstruídas silenciosamente por
    comprimento de passo quando houver fatos nativos elegíveis.
11. Comprimento de passo e calibração por passo permanecem internamente para
    legacy/fallback, mas saem da experiência normal do usuário.
12. Saúde deixa de iniciar medição manual e D1; Exercícios deixa de iniciar
    importação D3/D4. Histórico local permanece preservado.
13. Sincronizar relógio e sincronizar Health Connect são ações distintas. Watch
    sync somente persiste/prepara fila local; escrita remota exige ação explícita.
14. Health Connect adota cutover por data local inteira: antes do cutover,
    preservar projeções históricas; depois do cutover, não criar novo total
    `DailySummary` de passos. A projeção granular pós-cutover não pode assumir
    `D1 + D4` como fontes mutuamente exclusivas.
15. Para Health Connect, cada record compartilhado entre D1/D4 exige uma
    estratégia comprovadamente não sobreposta. Passos devem preservar o total
    FEA1 e os contratos da Spec 027; se a implementação não puder provar uma
    representação segura, deve falhar fechada em vez de duplicar records.
16. Cauda recente FEA1 × D1 não autoriza residual remoto: publicar somente fatos
    temporais D1/D4 realmente materializados. Projeções que exigem fechamento do
    dia aguardam a cauda real ou ficam bloqueadas/pendentes explicitamente.
17. Não haverá backfill granular histórico nem reescrita silenciosa de records ou
    atividades já `SYNCED`.
18. Calorias só serão promovidas ao tipo Health Connect correto depois de decisão
    semântica sustentada por evidência; raw e escala permanecem preservados.
19. Room continua fonte operacional/visual primária e Health Connect continua
    write-only.

## Consequências positivas

- um único gesto previsível para obter dados do relógio;
- menos estados BLE espalhados por features;
- dados diários temporais deixam de depender de estimativa por passada;
- D1 preserva fatos que o firmware já transmite, inclusive durante workouts;
- evita perda de dados em buckets que apenas intersectam parcialmente uma
  atividade;
- separa claramente agregado diário, granularidade diária e detalhe de workout;
- diferença normal de frescor entre FEA1 e D1 não é tratada como corrupção;
- nenhum residual sem timestamp é inventado para fechar o total;
- Health Connect evita dupla contagem entre total diário, D1 granular e workout;
- step length deixa de ser conceito de produto sem quebrar histórico.

## Custos e riscos

- migration Room aditiva e novos contratos de identidade D1;
- necessidade de cutover persistido para HC;
- composição D1/D4 exige autoridade por métrica em vez de regra temporal única;
- classificação de cauda recente precisa usar estrutura e frescor, não apenas a
  diferença numérica FEA1 × D1;
- fronteiras de bucket D1 ainda não estão suficientemente provadas para
  substituição proporcional ou parcial;
- semântica de calorias D1 versus D4 ainda exige validação;
- UI e ViewModels carregam state machines legadas que precisarão ser retiradas
  sem apagar dados históricos;
- partial success aumenta a necessidade de metadata por componente.

## Alternativas rejeitadas

### Manter três CTAs BLE

Rejeitada porque expõe detalhes de protocolo ao usuário e aumenta concorrência e
estados de erro entre Home, Saúde e Exercícios.

### Continuar usando `steps × stepLength` para distância diária

Rejeitada como caminho normal porque D1/D4 fornecem distância nativa; passada
continua somente fallback/legacy.

### Suprimir todo bucket D1 que intersecta D4

Rejeitada após validação física. Um workout D4 com passos/distância/calorias nulos
intersectou bucket D1 com fatos não nulos; remover o bucket perderia dados reais.

### Somar D1 e D4 sem prova de cobertura

Rejeitada porque a corrida física mostrou que D1 já continha os mesmos passos e
distância observados no D4, o que causaria dupla contagem.

### Tratar qualquer `FEA1 != ΣD1` como divergência

Rejeitada após validação física pós-033-05. FEA1 pode avançar alguns minutos à
frente da série D1 mesmo quando todas as posições recebidas estão completas. Essa
situação é cauda recente, não falha estrutural.

### Fabricar residual `FEA1 - ΣD1`

Rejeitada porque FEA1 não informa quando os passos residuais ocorreram. Criar
bucket ou `StepsRecord` sintético inventaria distribuição temporal e poderia
duplicar o próximo D1 real.

### Continuar auto-sync Health Connect após refresh do relógio

Rejeitada para manter fronteira explícita entre coleta local e efeito remoto.

### Backfill de histórico D1 antigo

Rejeitada porque o banco antigo preservou apenas BPM; reconstruir passos,
distância ou calorias seria inventar fatos ausentes.
