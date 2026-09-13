# 033-06 — Integrar Health Connect granular e cutover

- **Status:** pending
- **Depende de:** 033-04 e 033-05
- **Evidência obrigatória:**
  - `docs/integration/033-03-d1-v2-physical-validation.md`
  - `docs/integration/033-05-unified-watch-sync-physical-validation.md`

## Objetivo

Projetar dados diários granulares sem coexistir com total diário FEA1 no mesmo
dia, sem presumir que D1 e D4 sejam fontes temporalmente exclusivas, distinguir
cauda recente FEA1 × D1 de divergência estrutural e remover auto-sync remoto do
watch sync.

## Correção de premissa D1 × D4

A validação física mostrou que D1 já pode conter passos, distância e calorias do
workout D4. Portanto, está rejeitada a estratégia genérica:

```text
D1 fora do workout + D4 dentro do workout
```

como regra automática para todos os records.

A projeção Health Connect deve ser definida por métrica e somente publicar
representações cuja não sobreposição possa ser provada localmente.

## Correção de cobertura FEA1 × D1

A validação física pós-033-05 observou um cenário legítimo em que:

```text
FEA1.steps = 4323
Σ D1.steps = 4226
D1         = posições 1..183 completas, sem lacunas
último D1  = 15:10
FEA1       = coletado ~15:12:47
```

A diferença de `97` passos ocorreu com D1 estruturalmente completo para o
`totalPositions` recebido. Isso confirma que FEA1 pode refletir passos mais
recentes que ainda não foram materializados no próximo bucket D1 de ~5 minutos.

Logo, a regra antiga abaixo está rejeitada:

```text
FEA1 != ΣD1
=> DIVERGENT
```

A 033-06 deve distinguir pelo menos:

```text
FEA1 == ΣD1
  -> COMPLETE

FEA1 > ΣD1
+ D1 estruturalmente completo para totalPositions recebido
+ último bucket D1 recente em relação ao collectedAt do FEA1
  -> RECENT_TAIL / LAGGING
  -> diferença temporal esperada, não divergência estrutural

FEA1 < ΣD1
  -> DIVERGENT, salvo evidência futura explícita que prove outra semântica

D1 estruturalmente incompleto
  -> PARTIAL/INCOMPLETE, independentemente do sinal da diferença

qualquer diferença sem prova suficiente de frescor/cobertura
  -> DIVERGENT ou fail-closed conforme o consumidor
```

O nome final do enum pode variar. A distinção semântica entre **cauda recente**,
**incompletude estrutural** e **divergência real** é obrigatória.

### Guard rail da cauda recente

Nunca converter:

```text
FEA1 - ΣD1
```

em bucket, intervalo, `StepsRecord` ou timestamp sintético.

FEA1 não fornece distribuição temporal. A cauda somente pode ser materializada
quando um D1 real posterior chegar do relógio.

## Escopo

- persistir data de cutover local;
- preservar datas/operações pré-cutover;
- pós-cutover bloquear novas projeções STEPS de `DailySummary`;
- usar FEA1 como total de referência/validação para passos;
- preparar granularidade D1 conforme contratos de 033-04;
- refinar `WatchDailyStepCoverage`/equivalente para representar cauda recente sem
  classificá-la como divergência estrutural;
- preservar `WorkoutSteps`/`STEPS_CADENCE` da Spec 027 sem publicar os mesmos
  passos duas vezes;
- tratar DISTANCE D1 e DISTANCE D4 como potencialmente sobrepostos até prova de
  cobertura/substituição;
- HEART_RATE pode preferir D4 em workout quando houver cobertura D4 válida;
- IDs estáveis, versões monotônicas e payload congelado;
- preservar ledgers/credits legacy e não removê-los por suposição;
- remover chamadas automáticas a `syncPending()` de Home/D1;
- manter `Settings -> Sincronizar agora` e sync explícito de workout.

## Passos

Para datas pós-cutover:

```text
FEA1 = total diário atual de referência
D1   = distribuição granular materializada
D4   = passos/cadência do workout
```

A implementação deve garantir que a soma efetivamente publicada para passos não
represente D1 e D4 duplicados.

### FEA1 à frente do D1

Quando a cobertura for classificada como `RECENT_TAIL`/`LAGGING`:

- os buckets D1 já materializados continuam fatos locais válidos;
- não criar residual temporal com a diferença FEA1-D1;
- não alterar retroativamente IDs/payloads congelados de buckets já preparados;
- consumers locais podem mostrar FEA1 como total atual e D1 como série disponível;
- projeções que exijam fechamento completo do dia devem aguardar materialização
  real da cauda ou aplicar fail-closed explícito;
- a mera cauda recente não deve degradar o dia para `DIVERGENT`.

Se `WorkoutSteps` continuar sendo publicado, reutilizar ou estender a
contabilidade/guards da Spec 027 conforme necessário. Não desativar crédito ou
bloqueio apenas porque o Daily cumulativo antigo deixou de existir.

Se não houver prova segura de como coexistir D1 granular e `WorkoutSteps`, falhar
fechado para a operação conflitante em vez de publicar ambos.

`STEPS_CADENCE` permanece independente da quantidade diária e pode continuar
usando a série D4 elegível.

## Distância

D1 fornece distância diária nativa e D4 fornece distância canônica do workout.
Não publicar ambos de forma sobreposta sem prova determinística de cobertura.

Uma estratégia de substituição é aceitável somente quando a implementação puder
identificar com segurança a contribuição D1 correspondente ao mesmo fato D4.
Sem essa prova, manter o valor local e bloquear a projeção conflitante em vez de
duplicar distância no provider.

## Frequência cardíaca

D1 continua elegível por ponto BPM válido. Pontos D1 dentro de atividade podem
ser omitidos da projeção combinada somente quando houver cobertura D4 válida para
frequência cardíaca naquele workout. Não descartar D1 apenas pela existência da
janela da atividade.

## Calorias

Somente implementar record Health Connect de calorias se a semântica for fechada
com evidência suficiente. Caso contrário, manter local e documentar `deferred`.

Escalas locais permanecem distintas:

```text
D1 raw / 10.0
D4 raw / 1000.0
```

## Cutover

O cutover permanece por data local inteira:

```text
data < cutover
  -> preservar política histórica e operações existentes

data >= cutover
  -> não criar nova projeção STEPS de DailySummary/FEA1
  -> preparar somente granularidade comprovadamente segura
```

Para instalação com legado, preferir o próximo dia local após ativação do novo
contrato. Instalação nova sem estado legacy pode usar o dia atual somente após
preflight confirmar ausência de projeção diária antiga preparada.

Não fazer backfill granular histórico e não reabrir operações `SYNCED`.

## Guard rails

- sem backfill granular histórico;
- sem delete remoto amplo;
- sem reabrir `SYNCED` silenciosamente;
- sem leitura Health Connect;
- sem `D1 + D4` ingênuo para métricas compartilhadas;
- sem supressão de bucket D1 apenas por overlap temporal;
- sem bucket/residual sintético a partir de FEA1-D1;
- não classificar cauda recente comprovável como divergência estrutural;
- falhar fechado quando não houver prova de representação não sobreposta;
- não alterar payload/identidade já congelados por chegada posterior de novos
  buckets D1.

## Validação física esperada

Além dos builds, validar pelo menos:

1. `FEA1 == ΣD1` produz cobertura completa quando D1 está estruturalmente completo.
2. `FEA1 > ΣD1` com cauda recente e D1 estruturalmente completo produz
   `RECENT_TAIL`/equivalente, não `DIVERGENT`.
3. Nenhum `StepsRecord` sintético é criado para o residual FEA1-D1.
4. Próximo refresh que materializa novos buckets reduz/fecha a cauda sem reescrever
   payloads já congelados.
5. `FEA1 < ΣD1` ou cobertura estrutural inconsistente falha fechada/classifica
   divergência conforme contrato.
6. Projeção de passos pós-cutover não ultrapassa o total FEA1 por duplicação de
   D1/workout.
7. Retry mantém a mesma identidade/payload para fatos já preparados.
8. Watch sync da Home continua sem escrita remota automática.

## Validação técnica

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.
