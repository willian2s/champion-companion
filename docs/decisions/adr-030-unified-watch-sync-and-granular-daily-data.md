# ADR-030 — Sincronização unificada do relógio e dados diários granulares

- **Status:** accepted
- **Data:** 2026-09-11
- **Relacionado:** ADR-010, ADR-012, ADR-029; Specs 016, 027, 032 e 033

## Contexto

Após a Spec 032, workouts D4 v4 possuem fatos nativos, distância temporal e
calibração contextual robusta. A linha diária, porém, continua fragmentada:

- Home busca FEA1;
- Saúde inicia D1;
- Exercícios inicia D3/D4;
- D1 persiste somente BPM;
- Home ainda estima distância/calorias a partir de passos e métricas pessoais;
- Home/D1 podem tentar Health Connect automaticamente após persistência.

A inspeção do protocolo D1 do Champion C001 mostra que o pacote diário contém
fatos suficientes para passos, distância, calorias e BPM em granularidade de
aproximadamente cinco minutos. Evidência histórica também mostrou forte supressão
de BPM D1 durante workouts D4, reforçando a necessidade de composição explícita
entre as fontes e não de soma ingênua.

## Decisão

1. O pull-to-refresh da Home será o único entrypoint de produto para buscar dados
   do relógio.
2. Um `WatchSyncCoordinator` controlará gate foreground, sessão, handshake e
   sequência FEA1 -> D1 -> D3/D4, preservando partial success por componente.
3. FEA1 permanecerá agregado diário local de passos e sinal de validação, sem
   distribuição temporal inventada.
4. D1 evoluirá para parser/persistência de fatos diários completos (`d1-v2`), com
   BPM nullable e demais métricas preservadas quando BPM for inválido.
5. D4 será autoridade durante janelas de workout; D1 será autoridade diária fora
   dessas janelas. Bucket D1 que intersectar D4 será inicialmente suprimido por
   inteiro para as métricas compartilhadas.
6. Novas distâncias diárias não serão reconstruídas silenciosamente por
   comprimento de passo quando houver fatos nativos elegíveis.
7. Comprimento de passo e calibração por passo permanecem internamente para
   legacy/fallback, mas saem da experiência normal do usuário.
8. Saúde deixa de iniciar medição manual e D1; Exercícios deixa de iniciar
   importação D3/D4. Histórico local permanece preservado.
9. Sincronizar relógio e sincronizar Health Connect são ações distintas. Watch
   sync somente persiste/prepara fila local; escrita remota exige ação explícita.
10. Health Connect adotará cutover por data local inteira: antes do cutover,
    preservar projeções históricas; depois do cutover, não criar novo total
    `DailySummary` de passos e projetar D1/D4 granular sem sobreposição.
11. Não haverá backfill granular histórico nem reescrita silenciosa de records ou
    atividades já `SYNCED`.
12. Calorias só serão promovidas ao tipo Health Connect correto depois de decisão
    semântica sustentada por evidência; raw e escala permanecem preservados.
13. Room continua fonte operacional/visual primária e Health Connect continua
    write-only.

## Consequências positivas

- um único gesto previsível para obter dados do relógio;
- menos estados BLE espalhados por features;
- dados diários temporais deixam de depender de estimativa por passada;
- D1 preserva fatos que o firmware já transmite;
- D1 e D4 deixam de competir pela mesma janela temporal;
- Health Connect evita dupla contagem entre total diário e workout;
- step length deixa de ser conceito de produto sem quebrar histórico.

## Custos e riscos

- migration Room aditiva e novos contratos de identidade D1;
- necessidade de cutover persistido para HC;
- composição D1/D4 exige regra fail-closed para overlap;
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

### Somar D1 e D4 sem overlap policy

Rejeitada por risco de dupla contagem e pela evidência física de interação entre
Sport Model e histórico diário.

### Continuar auto-sync Health Connect após refresh do relógio

Rejeitada para manter fronteira explícita entre coleta local e efeito remoto.

### Backfill de histórico D1 antigo

Rejeitada porque o banco antigo preservou apenas BPM; reconstruir passos,
distância ou calorias seria inventar fatos ausentes.
