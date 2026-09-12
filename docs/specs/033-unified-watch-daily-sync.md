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

Além disso, o parser D1 efetivo ainda é `d1-v1` e persiste apenas frequência
cardíaca. A inspeção do protocolo do Champion C001 mostra que cada pacote D1
carrega também passos, distância, calorias, `sportValue` e outros campos raw.
O parser atual ainda trata o byte 5 como hora direta, aceita BPM 30..220 e descarta
o pacote inteiro quando o BPM não é válido.

A Home também continua criando distância/calorias estimadas a partir do resumo
FEA1 e de métricas pessoais, enquanto a direção pós-032 é priorizar fatos medidos
do relógio e deixar comprimento de passo apenas como fallback interno.

Por fim, refresh da Home e conclusão de D1 ainda podem disparar processamento do
Health Connect automaticamente. A direção vigente para a 033 é separar
explicitamente:

```text
sincronizar relógio != sincronizar Health Connect
```

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

### R3 — FEA1 permanece agregado diário de passos

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

Criar persistência aditiva, inicialmente planejada como
`watch_daily_d1_samples`, com identidade estável baseada no dispositivo, data
resolvida e posição do D1.

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

Room evolui de v17 para v18 apenas se a task de persistência confirmar esse
contrato. A migration é aditiva e não cria efeitos Health Connect.

`heart_rate_measurements` D1 existentes permanecem históricos. Não fabricar fatos
D1 antigos que nunca foram persistidos.

### R7 — D4 é autoridade durante cobertura de treino

Para composição diária inicial:

```text
D4 = autoridade durante workout
D1 = autoridade fora do workout
FEA1 = total diário/validação de passos
```

Qualquer bucket D1 de aproximadamente cinco minutos que intersecte a janela de um
workout D4 é inicialmente suprimido como fonte diária para:

- passos;
- distância;
- calorias;
- frequência cardíaca.

Não dividir proporcionalmente o bucket D1 para tentar recuperar a fração fora do
workout sem evidência do protocolo.

Totais de header D4 permanecem consistência/diagnóstico; comportamento usa série
por intervalo/range/soma.

### R8 — Dados diários medidos substituem estimativas novas por passada

Para dados novos pós-033:

- passos de Home continuam usando FEA1;
- distância diária deve ser composta de D1 fora de workout + D4 durante workout;
- calorias diárias devem usar fatos nativos somente após fechar a semântica de
  projeção;
- não criar nova distância diária via `steps × stepLength` quando fatos nativos
  elegíveis existirem;
- ausência de fato suficiente deve aparecer como ausente/parcial, não como
  estimativa silenciosa.

Histórico anterior permanece como registrado.

### R9 — Comprimento de passo sai da experiência normal

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

### R10 — Saúde vira leitura/análise para dados do relógio

Remover entrypoints de produto para:

- medição manual de frequência cardíaca;
- importação D1;
- retry/cancel específicos de D1;
- state machines BLE correspondentes na UI final.

Dados históricos manuais e D1 antigos permanecem visíveis. Não apagar histórico
somente porque o fluxo de criação saiu da interface.

Peso local continua uma funcionalidade independente e pode permanecer editável.

### R11 — Exercícios vira revisão/análise/sync

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

### R12 — Sincronizar relógio não escreve automaticamente no Health Connect

O watch sync deve:

```text
ler -> validar -> persistir -> preparar/garantir fila PENDING
```

mas não chamar `syncPending()` automaticamente.

Remover triggers automáticos atuais de Home/D1 que escrevem no provider após
persistência. Health Connect permanece write-only e local-first.

A escrita remota ocorre por ação explícita, incluindo `Settings -> Sincronizar
agora` e sync explícito de workout revisado.

### R13 — Cutover diário inteiro para projeção granular no Health Connect

Evitar coexistência no mesmo dia entre total diário FEA1 e records granulares
D1/D4.

Persistir um cutover por data local inteira.

Regra alvo para instalação com histórico:

```text
data < cutover
  -> política Health Connect histórica permanece intocada

data >= cutover
  -> não criar nova projeção STEPS de DailySummary/FEA1
  -> projetar fatos granulares D1 fora de workout + D4 durante workout
```

Preferência inicial para upgrade: próximo dia local após ativação do novo contrato.
Instalação nova sem legado pode iniciar granular no próprio dia, desde que a task
de HC confirme que não existe projeção diária legacy já preparada.

Não realizar backfill granular histórico, não apagar records remotos antigos e não
reinterpretar operações `SYNCED`.

A Spec 027/ledgers de crédito de passos permanecem necessários para histórico
pré-cutover. Para datas pós-cutover, o objetivo é não depender de compensação de
total diário porque D1 e D4 já serão mutuamente exclusivos na composição.

### R14 — Calorias exigem gate semântico próprio

Persistir os fatos confirmados:

```text
D1 caloriesRaw / 10.0
D4 caloriesRaw / 1000.0
```

Mas não escolher `ActiveCaloriesBurned` versus `TotalCaloriesBurned` no Health
Connect por inferência. A 033-01 registra candidatos; a projeção só é liberada se
houver evidência suficiente durante execução.

### R15 — Idempotência e partial success são obrigatórios

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

### R16 — Privacidade e produto watch-first permanecem

Não introduzir:

- GPS de produção;
- rota/mapa;
- Adidas/Strava;
- background BLE permanente;
- WorkManager para polling do relógio;
- logs com payload BLE integral, BPM individual, identificadores pessoais ou
  dados de saúde sensíveis.

## Health Connect pós-cutover

Projeção alvo, sujeita aos gates semânticos da execução:

```text
STEPS
  D1 fora de workout
  + D4 durante workout

DISTANCE
  D1 fora de workout
  + D4 nativo/calibrado durante workout

HEART_RATE
  D1 BPM válido fora de workout
  + D4 BPM durante workout

CALORIES
  somente quando a semântica for confirmada
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

`033-02` e `033-03` podem ser executadas em paralelo depois de `033-01`.

## Validação obrigatória

Mudanças de código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem pedido explícito do usuário,
conforme `AGENTS.md`.

A validação física final deve exercitar Champion C001 real, migration v17->v18,
idempotência, overlap D1/D4, partial success, ausência de auto-sync HC e cutover
sem duplicação.
