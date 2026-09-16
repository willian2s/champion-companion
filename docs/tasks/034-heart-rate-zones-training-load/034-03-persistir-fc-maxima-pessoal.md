# 034-03 — Persistir FC máxima pessoal

- **Ticker:** `034`
- **Número:** `03`
- **Status:** `completed`

## Objetivo

Adicionar FC máxima manual ao perfil pessoal e atualizar Room de v19 para v20
sem perder dados nem produzir efeitos remotos.

## Resultado esperado

Perfil, snapshot, repository e editor de Perfil leem, validam, salvam, limpam e
preservam `maxHeartRateBpm`; bancos v19 são migrados aditivamente para v20 com
schema exportado atualizado.

## Requisitos cobertos

- R1, R7 e critérios de aceite 1 a 4 e 15 a 17 da Spec 034;
- consequência de FC máxima como métrica pessoal, não preferência;
- Health Connect sem mudanças funcionais.

## Escopo incluído

- `UserProfile`, `UserProfileEntity` e mappers;
- `PersonalMetricsSnapshot` e `PersonalMetricsRepository`;
- input/result e atualização local da métrica;
- preservação em `updateHeight` e `updateStepLength`;
- estado, intents, mensagens, parser inteiro e UI do Perfil;
- `MIGRATION_19_20`, registro na base e `20.json`;
- supporting text explicativo.

## Escopo excluído

- zonas/carga e policy;
- read model do detalhe de Exercícios;
- sincronização de FC máxima com Health Connect;
- mudança de altura/meta/peso além da preservação necessária;
- alteração de tabelas de activities, samples ou snapshots;
- testes automatizados novos.

## Dependências

- `034-01` concluída;
- `034-02` pode estar em paralelo, mas não é necessária para a migration;
- Room v19 e schema exportado 19;
- fluxo atual de `ProfileViewModel` e `PersonalMetricsRepository`.

## Arquivos e símbolos prováveis

- `core/model/DeviceProfileDailySummary.kt` — `UserProfile`;
- `core/model/RepositoryContracts.kt` — snapshot/input/repository;
- `core/database/ChampionDatabase.kt` — `UserProfileEntity`, `version`;
- `core/database/ChampionMigrations.kt` — `MIGRATION_19_20`;
- `core/database/DatabaseModule.kt` — registro;
- `data/DatabaseMappers.kt` e `data/RoomRepositories.kt`;
- `feature/profile/ProfileFeatureContract.kt`;
- `feature/profile/ProfileViewModel.kt`;
- `feature/profile/ProfileScreen.kt`;
- `app/src/main/res/values/strings.xml`;
- `app/schemas/.../20.json`.

## Passos de implementação

1. Adicionar campo opcional ao domínio com validação `30..220`.
2. Adicionar coluna nullable `INTEGER` na entidade.
3. Atualizar mappers e snapshot.
4. Criar input/método de repository que não chame Health Connect.
5. Garantir que updates de altura e passada preservem FC máxima.
6. Criar migration aditiva `19 -> 20` sem backfill.
7. Registrar migration e alterar versão da base.
8. Gerar e revisar schema exportado 20.
9. Adicionar editor no Perfil, limpeza obrigatória para `NULL` e mensagens de
   validação.
10. Confirmar que o fluxo de altura continua com seu contrato vigente, sem
    reutilização pelo novo fluxo.

## Testes e comandos de validação

- inspeção do SQL da migration e schema 20;
- conferência de mapeamento após reinício;
- `git diff --check`;
- não criar ou executar testes automatizados nesta task.

## Definição de pronto

- banco v19 abre em v20 sem wipe;
- perfis existentes iniciam com `NULL`;
- valores válidos persistem e valores fora de faixa são rejeitados;
- limpar valor retorna a estado não configurado;
- altura/passada não apagam FC máxima;
- nenhuma operação Health Connect é criada/alterada ao salvar FC máxima.

## Riscos e cuidados

- Não reutilizar `maximumBpm` de atividade.
- Não adicionar idade ou sugestão automática.
- Não registrar FC máxima em DataStore.
- Não alterar IDs, payloads ou fila Health Connect por conveniência do fluxo de
  Perfil.
- Não esquecer schema exportado 20 nem registrar migration fora da ordem.

## Registro da execução

- **Status:** `completed` — persistência e editor local implementados após o
  congelamento de contrato.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileFeatureContract.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileViewModel.kt`;
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt`;
  `app/src/main/res/values/strings.xml`;
  `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/20.json`.
- **Decisões e desvios:** `maxHeartRateBpm` permanece `Int?`, validado no
  domínio e no parser entre `30..220`, inclusive. A migration apenas adiciona
  coluna `INTEGER` nullable, sem backfill. O update local reconstrói o perfil
  preservando altura, comprimento de passo, origem e timestamps próprios; os
  caminhos de altura e passada também copiam a FC máxima. Campo vazio persiste
  `NULL`. O novo fluxo não chama Health Connect nem usa o gatilho remoto da
  altura. Nenhum desvio de requisito.
- **Comandos executados:** inspeção de `AGENTS.md`, baseline, Spec 034, ADR-034,
  overview, contratos e testes existentes; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint assembleDebug assembleRelease`; `git diff --check`; inspeção
  programática do schema 20 e da diferença entre schemas 19 e 20. Nenhum teste
  automatizado foi criado ou executado. A primeira execução do gate conjunto
  excedeu o limite de 120 s do executor; a repetição com limite de 300 s passou.
- **Resultados e evidências:** compilação Kotlin, lint e builds debug/release
  passaram. Schema 20 declara `user_profiles.maxHeartRateBpm` como `INTEGER`
  nullable e o delta em relação ao schema 19 contém somente essa coluna.
  `MIGRATION_19_20` está registrada depois de `MIGRATION_18_19`; todos os
  reconstrutores de `UserProfile` foram conferidos. A alteração de FC máxima
  persiste somente em `user_profiles`; o diff não altera parser, BLE, workflow,
  snapshots analíticos, zonas/carga, read model de Exercícios ou Health Connect.
- **Riscos residuais:** migração sobre banco físico existente, round-trip após
  reinício, limpeza efetiva, ausência de novas operações em
  `health_sync_operations` e recálculo visual ainda dependem de validação física
  em `034-06`. `034-04` está liberada após `034-02` e `034-03`, mas não foi
  iniciada por escopo desta execução; `034-05` depende de `034-04`, e
  `034-06` depende da validação das subtarefas anteriores. Nenhuma delas foi
  iniciada nesta execução.
