# 033 — Sincronização unificada do relógio e dados diários granulares

- **Ticker:** `033`
- **Status:** `in_progress`
- **Tipo:** correção arquitetural de sincronização, protocolo D1, dados diários e projeção Health Connect
- **Branch:** `feat/033-unified-watch-daily-sync`
- **ADR:** [ADR-030](../decisions/adr-030-unified-watch-sync-and-granular-daily-data.md)
- **Base de integração:** `main` após conclusão da Spec 032

## Contexto

A Spec 032 foi integrada à `main` e consolidou `d4-v4`, distância nativa do relógio,
calibração contextual por referência externa e Room v17. Novas atividades D4 v4
íntegras podem nascer como `WATCH_NATIVE`, mantendo `STEP_LENGTH_FALLBACK` apenas
como compatibilidade/fallback técnico.

A área diária permanece fragmentada em três ações BLE de produto:

```text
Home pull-to-refresh  -> FEA1
Saúde                  -> D1
Exercícios              -> D3/D4
```

Além disso, o parser D1 efetivo antes desta spec persistia apenas frequência
cardíaca. A inspeção do protocolo do Champion C001 mostra que cada pacote D1
carrega também passos, distância, calorias, `sportValue` e outros campos raw.

A Home também continua criando distância/calorias estimadas a partir do resumo
FEA1 e de métricas pessoais, enquanto a direção pós-032 é priorizar fatos medidos
do relógio e deixar comprimento de passo apenas como fallback interno.

Por fim, refresh da Home e conclusão de D1 ainda podem disparar processamento do
Health Connect automaticamente. A direção vigente para a 033 é separar
explicitamente:

```text
sincronizar relógio != sincronizar Health Connect
```

### Correção pós-validação física de 033-02/033-03

A primeira versão do contrato assumiu que D1 seria fonte somente fora dos
workouts e que todo bucket D1 que intersectasse uma janela D4 poderia ser
suprimido. Dois dumps físicos sanitizados após implementação de `d1-v2`/Room v18
invalidaram essa premissa.

Foi observado:

- `Σ D1.steps == FEA1.steps` em duas leituras sucessivas do mesmo dia;
- uma corrida D4 válida em que os buckets D1 correspondentes fecharam exatamente
  em passos e distância e praticamente em calorias contra o D4;
- outro workout D4 com passos/distância/calorias nulos que ainda intersectava
  bucket D1 não nulo.

Portanto, D1 é a linha granular diária do relógio e pode carregar fatos do treino.
D4 permanece a fonte detalhada/canônica da atividade. Não existe mais regra global
`D1 fora / D4 dentro` nem supressão integral por mera interseção.

Evidência: `docs/integration/033-03-d1-v2-physical-validation.md`.

## Objetivo

Transformar o pull-to-refresh da Home no único gesto de produto para buscar dados
do Champion C001 e consolidar uma linha diária granular local:

```text
Home pull-to-refresh
        |
        v
WatchSyncCoordinator
  |       |       |
 FEA1     D1     D3/D4
  |       |       |
  +-------+-------+
          |
          v
   persistência local
          |
     +----+----+
     |         |
 read models  fila Health Connect PENDING
                   |
                   v
             sync explícito
```

A 033 deve preservar a arquitetura watch-first, Room como fonte operacional,
review-before-sync de workouts e ausência de GPS/rota em produção.

## Decisões obrigatórias

### R1 — Home é o único entrypoint de sincronização do relógio

O único gesto de produto que inicia leitura BLE de dados do relógio será o
pull-to-refresh da Home.

Esse gesto coordena:

1. FEA1;
2. D1;
3. D3/D4.

Saúde não inicia D1 nem medição manual. Exercícios não inicia importação D3/D4.
Abrir telas, conectar automaticamente, voltar ao foreground ou trocar filtros não
inicia leitura de histórico.

### R2 — Um coordinator de watch sync controla gate, sessão e handshake

Introduzir `WatchSyncCoordinator` ou nome equivalente como owner de produto da
sincronização composta.

Requisitos:

- um owner foreground compartilhado, preferencialmente `WATCH_SYNC`;
- reutilizar a mesma sessão BLE quando possível;
- executar handshake A1 uma vez por tentativa quando necessário;
- componentes FEA1, D1 e D3/D4 continuam módulos/protocolos independentes;
- falha de um componente não apaga sucessos anteriores;
- resultado global pode ser `COMPLETED`, `PARTIAL`, `FAILED` ou equivalente;
- timestamps/status por componente permanecem observáveis.

### R3 — FEA1 permanece agregado diário autoritativo de passos

FEA1 continua sendo a melhor leitura do total diário atual de passos do relógio.
`DailySummary.steps` permanece o agregado local autoritativo do dia para Home e
validação.

FEA1 não fornece distribuição temporal. A 033 não deve fabricar buckets a partir
do total FEA1.

### R4 — Introduzir parser `d1-v2` completo

Para o layout fixo observado do C001, o target D1 v2 é:

```text
byte 0      0xD1
1..2        currentPosition u16 LE
3..4        totalPositions u16 LE
5           dayOffset/hour packed
             dayOffset = u8 / 32
             hour      = u8 % 32
6..7        caloriesRaw u16 BE
8..9        distanceRaw u16 BE
10..11      steps u16 BE
12..13      sportValue u16 BE
14          bpmRaw u8
15          pressureHighRaw u8
16          pressureLowRaw u8
17          wearRaw u8
18          tempOneRaw u8
19          minute u8
```

Unidades congeladas para o layout observado:

```text
caloriesKilocalories = caloriesRaw / 10.0
distanceMeters       = distanceRaw
```

A interpretação de `pressureHighRaw`, `pressureLowRaw`, `wearRaw`, `tempOneRaw`
e o significado fisiológico de `sportValue` permanecem raw até evidência física
suficiente.

### R5 — BPM inválido não invalida o pacote D1

Faixa oficial adotada para D1 v2:

```text
30..200 BPM
```

BPM fora da faixa resulta em ausência de BPM naquele sample, mas não descarta:

- passos;
- distância;
- calorias;
- `sportValue`;
- raw adicionais;
- posição/completude do multipart.

A conclusão do collector depende da estrutura/posição recebida, não da validade do
BPM. Um último pacote com BPM inválido não pode forçar timeout artificial.

### R6 — Persistir D1 como fatos diários, não como frequência cardíaca somente

Criar persistência aditiva em `watch_daily_d1_samples`, com identidade estável
baseada no dispositivo, data resolvida e posição do D1.

Persistir pelo menos:

- device/import/parser;
- posição e total;
- data local, hora, minuto, instante e zoneId congelado;
- steps;
- distance raw/meters;
- calories raw/kcal;
- sportValue raw;
- BPM nullable;
- pressure/wear/temp raw;
- importedAt/updatedAt.

Room evolui de v17 para v18 de forma aditiva. A migration não cria efeitos Health
Connect.

`heart_rate_measurements` D1 existentes permanecem históricos. Não fabricar fatos
D1 antigos que nunca foram persistidos.

### R7 — D1 é a linha diária; D4 é a fonte do workout

A composição não usa mais uma autoridade temporal única.

```text
FEA1
  -> total diário autoritativo de passos

D1
  -> linha granular diária do relógio
  -> pode incluir fatos ocorridos durante workout

D4
  -> fonte detalhada/canônica do workout
```

Consequências:

- não suprimir bucket D1 inteiro apenas porque sua janela/timestamp intersecta D4;
- não somar D1 + D4 como se fossem fontes independentes;
- header D4 permanece consistência/diagnóstico;
- comportamento D4 usa série por intervalo/range/soma.

### R8 — Autoridade diária é definida por finalidade e métrica

#### Passos

- `DailySummary.steps`/FEA1 = total diário autoritativo;
- D1 = distribuição temporal/granular dos passos do dia;
- `Σ D1.steps` deve ser comparado com FEA1 para cobertura/diagnóstico;
- D4 = passos/cadência do workout;
- passos D4 não são somados novamente ao total diário.

#### Distância

- D1 = linha nativa diária de distância quando elegível;
- D4 = distância canônica do workout, incluindo calibração contextual quando
  aplicável;
- não calcular total diário como `D1 + D4`;
- não reconstruir nova distância diária por `steps × stepLength` quando D1 nativo
  elegível existir;
- uma substituição/ajuste de contribuição D1 pelo D4 exige prova determinística de
  cobertura e não duplicação;
- sem essa prova, o diário preserva D1 e o detalhe do workout preserva D4.

#### Calorias

- D1 = fatos diários locais com escala `raw / 10.0`;
- D4 = fatos do workout com escala `raw / 1000.0`;
- não somar D1 + D4 no diário;
- semântica Health Connect permanece bloqueada até evidência suficiente.

#### Frequência cardíaca

- D1 fornece pontos automáticos válidos do dia;
- D4 fornece amostras do workout;
- ponto D1 dentro da janela de atividade só é omitido do read model combinado se
  houver cobertura D4 válida de frequência cardíaca;
- ausência de D4 válido não autoriza eliminar D1;
- não criar BPM sintético/interpolado.

### R9 — Substituição D1 -> D4 exige prova de cobertura

Interseção temporal isolada não é prova suficiente.

Uma substituição/ajuste de uma métrica diária pelo D4 precisa demonstrar que a
contribuição D1 substituída corresponde ao mesmo fato representado no D4 e que a
operação não perde conteúdo externo ao workout.

A primeira implementação pode deliberadamente não substituir distância/calorias
D1 pelo D4 se essa prova não estiver disponível:

```text
read model diário  = D1 nativo
read model workout = D4 canônico
```

Essa diferença de contexto é preferível a perda ou dupla contagem.

Não dividir bucket D1 proporcionalmente sem evidência da semântica exata de sua
fronteira temporal.

### R10 — Comprimento de passo sai da experiência normal

Remover da UI normal do Perfil:

- edição manual de comprimento de passo;
- sugestão/aceite por altura;
- perfis aprendidos de step length;
- correção/histórico de calibração por passo.

Preservar domínio, schema e contratos legacy necessários a:

- `d4-v1/v2/v3`;
- `d4-v4` inelegível;
- snapshots históricos;
- reprocessamento explícito;
- fallback técnico.

A calibração de distância D4 nativa por referência externa permanece no detalhe
de exercício.

### R11 — Saúde vira leitura/análise para dados do relógio

Remover entrypoints de produto para:

- medição manual de frequência cardíaca;
- importação D1;
- retry/cancel específicos de D1;
- state machines BLE correspondentes na UI final.

Dados históricos manuais e D1 antigos permanecem visíveis. Não apagar histórico
somente porque o fluxo de criação saiu da interface.

Peso local continua uma funcionalidade independente e pode permanecer editável.

### R12 — Exercícios vira revisão/análise/sync

Remover da UI de Exercícios:

- `Buscar treinos no relógio`;
- retry/cancel/progresso de importação D3/D4 como ação própria da tela.

Exercícios continua responsável por:

- lista de workouts importados;
- review/classificação;
- calibração de esteira quando aplicável;
- referência externa de distância D4;
- reprocessamento explícito de manutenção quando necessário;
- sync explícito de atividade para Health Connect.

### R13 — Sincronizar relógio não escreve automaticamente no Health Connect

O watch sync deve:

```text
ler -> validar -> persistir -> preparar/garantir fila PENDING
```

mas não chamar `syncPending()` automaticamente.

Remover triggers automáticos atuais de Home/D1 que escrevem no provider após
persistência. Health Connect permanece write-only e local-first.

A escrita remota ocorre por ação explícita, incluindo `Settings -> Sincronizar
agora` e sync explícito de workout revisado.

### R14 — Cutover diário inteiro para projeção granular no Health Connect

Evitar coexistência no mesmo dia entre total diário FEA1 e nova projeção granular.
Persistir um cutover por data local inteira.

Regra alvo para instalação com histórico:

```text
data < cutover
  -> política Health Connect histórica permanece intocada

data >= cutover
  -> não criar nova projeção STEPS de DailySummary/FEA1
  -> usar somente representação granular comprovadamente não sobreposta
```

Preferência inicial para upgrade: próximo dia local após ativação do novo contrato.
Instalação nova sem legado pode iniciar granular no próprio dia, desde que a task
de HC confirme que não existe projeção diária legacy já preparada.

A projeção pós-cutover não pode assumir `D1 fora + D4 dentro`, porque a validação
física mostrou que D1 já contém fatos do workout.

Requisitos adicionais:

- `STEPS` pós-cutover deve preservar o total FEA1 e os contratos da Spec 027;
- D1 granular é o candidato natural para a distribuição diária de passos, mas
  qualquer coexistência com `WorkoutSteps` exige prova/contabilidade que impeça
  dupla publicação;
- `DISTANCE` também não pode publicar D1 e D4 sobrepostos sem seletor de cobertura
  comprovado;
- `HEART_RATE` pode preferir D4 durante workout somente quando houver cobertura D4
  válida;
- se a representação segura não puder ser provada, falhar fechada para aquele
  record em vez de duplicar.

Não realizar backfill granular histórico, não apagar records remotos antigos e não
reinterpretar operações `SYNCED`.

A Spec 027/ledgers de crédito de passos permanecem necessários para histórico
pré-cutover e podem continuar sendo necessários como mecanismo de segurança onde
`WorkoutSteps` coexistir com a nova granularidade. A 033 não os remove por
suposição.

### R15 — Calorias exigem gate semântico próprio

Persistir os fatos confirmados:

```text
D1 caloriesRaw / 10.0
D4 caloriesRaw / 1000.0
```

Mas não escolher `ActiveCaloriesBurned` versus `TotalCaloriesBurned` no Health
Connect por inferência. A projeção só é liberada se houver evidência suficiente
durante execução.

### R16 — Idempotência e partial success são obrigatórios

D1 deve aceitar reimportação sem duplicar samples. D3/D4 continua usando identidade
por CRC/import existente.

Exemplo obrigatório:

```text
FEA1 OK
D1   OK
D4   falha

=> FEA1 e D1 permanecem persistidos
=> watch sync = PARTIAL
=> próximo refresh tenta recuperar D4
```

Nunca usar transação global que faça rollback de componentes já válidos por uma
falha BLE posterior.

### R17 — Privacidade e produto watch-first permanecem

Não introduzir:

- GPS de produção;
- rota/mapa;
- Adidas/Strava;
- background BLE permanente;
- WorkManager para polling do relógio;
- logs com payload BLE integral, BPM individual, identificadores pessoais ou
  dados de saúde sensíveis.

## Health Connect pós-cutover

A projeção precisa ser decidida por métrica e cobertura, não por uma regra global
de janela.

```text
STEPS
  total de referência: FEA1
  distribuição candidata: D1 granular
  D4/WorkoutSteps: somente sem duplicar o mesmo total

DISTANCE
  diário: D1 nativo
  workout: D4 canônico
  coexistência remota: somente com prova de não sobreposição/substituição

HEART_RATE
  D1 automático
  D4 durante workout quando houver cobertura válida

CALORIES
  local: D1 diário + D4 no contexto do workout, sem soma
  remoto: somente quando a semântica for confirmada
```

FEA1 continua validando/mostrando total diário local de passos, mas não cria novo
`StepsRecord` pós-cutover.

## Sequência de execução

```text
033-01 Auditoria/contratos
   |\
   | +--> 033-02 Parser/collector D1 v2
   | +--> 033-03 Persistência Room v18
   |          |
   +----------+
              v
        033-04 Composição diária
              |
        +-----+-----+
        |           |
        v           v
033-05 Watch sync  033-06 HC cutover/granular
        |           |
        +-----+-----+
              v
        033-07 Produto/UI
              |
        033-08 Read models/compatibilidade
              |
        033-09 Validação física/fechamento
```

`033-02` e `033-03` foram executadas antes da correção física deste contrato; a
`033-04` deve seguir a versão revisada acima.

## Validação obrigatória

Mudanças de código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem pedido explícito do usuário,
conforme `AGENTS.md`.

A validação física final deve exercitar Champion C001 real, migration v17->v18,
idempotência, composição D1/D4 por métrica, partial success, ausência de
auto-sync HC e cutover sem duplicação.
