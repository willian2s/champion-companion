# 034-01 — Auditar timeline de FC e congelar contrato

- **Ticker:** `034`
- **Número:** `01`
- **Status:** `pending`

## Objetivo

Consolidar evidência do contrato atual de série temporal, perfil pessoal, Room,
Profile e detalhe de Exercícios antes de qualquer implementação, transformando
as decisões de produto em limites verificáveis para as subtarefas seguintes.

## Resultado esperado

Documento/evidência de implementação atualizado ou anexado à revisão da task,
com janela canônica, pontos de extensão, estados de ausência, versão Room,
contratos remotos preservados e dependências de `034-02` a `034-05` congelados.

## Requisitos cobertos

- R1, R4, R5 e R7 da Spec 034;
- ADR-018 como autoridade única para sample-and-hold;
- confirmação de `WorkoutHeartRateSample`, `UserProfile`,
  `PersonalMetricsSnapshot`, `PersonalMetricsRepository`, Profile,
  `ExercisesScreen`/`ExercisesViewModel` e Room v19;
- ausência de alterações BLE, parser, workflow e Health Connect.

## Escopo incluído

- auditar `WorkoutHeartRateSample`, DAO, entidade, mapper e consumers;
- confirmar `[startedAt, startedAt + duration)` como janela canônica;
- mapear fronteiras e riscos de `endedAt`, samples fora da sessão e lacunas;
- mapear `UserProfile` e todos os reconstrutores que podem perder o novo campo;
- mapear editor de Perfil e ponto de composição do detalhe;
- confirmar migration/schema atuais e cadeia v19;
- registrar contratos que não podem ser reabertos.

## Escopo excluído

- qualquer alteração em código de produção;
- criação de testes automatizados;
- ajuste de parser, BLE, Health Connect ou snapshots;
- decisão de baseline para classificar carga.

## Dependências

- [ADR-018](../../decisions/adr-018-canonical-workout-time-series-and-health-projection.md);
- baseline atual e estado documental da Spec 033;
- código vigente em `core/model`, `core/database`, `data`, `feature/profile` e
  `feature/exercises`.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutActivity.kt` — `WorkoutHeartRateSample`;
- `core/model/WatchWorkoutDerivedMetrics.kt` — `weightedBpm`;
- `core/database/ChampionDatabase.kt` — entidades e versão;
- `core/database/ChampionDaos.kt` — queries da série e perfil;
- `core/database/ChampionMigrations.kt` — última migration;
- `data/DatabaseMappers.kt` e `data/RoomRepositories.kt`;
- `core/model/DeviceProfileDailySummary.kt` — `UserProfile`;
- `core/model/RepositoryContracts.kt` — repository e snapshot;
- `feature/profile/ProfileFeatureContract.kt`, `ProfileViewModel.kt`,
  `ProfileScreen.kt`;
- `feature/exercises/ExercisesFeatureContract.kt`, `ExercisesViewModel.kt`,
  `ExercisesScreen.kt`;
- `app/schemas/.../19.json`.

## Passos de implementação

1. Revalidar paths e linhas dos contratos já auditados.
2. Escrever matriz de fonte, janela, estado e consumer.
3. Fixar `maxHeartRateBpm` como `Int?` manual em `30..220`.
4. Fixar buckets por razão e pesos, incluindo `BELOW_ZONE_1`.
5. Fixar cobertura por duração de sample-and-hold, sem threshold.
6. Fixar ausência segura para configuração, samples e falha de leitura.
7. Fixar ordem de dependência: domínio e persistência em paralelo após esta
   task; read model depois; UI por último; validação final.
8. Registrar qualquer divergência como risco, não como comportamento implícito.

## Testes e comandos de validação

- leitura e inspeção dos arquivos auditados;
- conferência de Room v19 e schema 19;
- `git diff --check` quando houver documentação produzida;
- não criar ou executar testes automatizados.

## Definição de pronto

- janela temporal e fórmula não dependem de interpretação informal;
- pontos de extensão e preservação de dados estão listados;
- escopo watch-only e fronteira Health Connect estão explícitos;
- `034-02` e `034-03` podem ser implementadas sem decisão adicional material.

## Riscos e cuidados

- Não transformar a lacuna visual de 120 s do gráfico em nova política.
- Não usar `metrics.averageBpm` ou `maximumBpm` como fonte da análise.
- Não confundir `endedAt` com fim canônico quando duração e pausas divergem.
- Não assumir que `SYNCED` pode ser reprocessado.
- Preservar dados de saúde; não incluir BPM individual em logs ou evidências.

## Registro da execução

- **Status:** `pending` — planejamento criado; implementação ainda não iniciada.
- **Arquivos alterados:** nenhum arquivo de produto; esta task documenta a
  auditoria e seus pontos de extensão.
- **Decisões e desvios:** ADR-018 permanece autoridade temporal; nenhum desvio
  foi autorizado.
- **Comandos executados:** leitura de baseline, ADR-018, código de FC/perfil/UI,
  schema Room 19 e inspeção de paths.
- **Resultados e evidências:** Room confirmado v19; série persistida e helper
  `weightedBpm` localizados; Perfil e detalhe atuais confirmados.
- **Riscos residuais:** o helper existente precisa ser compartilhado sem deixar
  sample externo criar janela analítica; preservar esse limite na implementação.
