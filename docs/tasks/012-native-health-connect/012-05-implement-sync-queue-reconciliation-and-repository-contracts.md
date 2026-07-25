# Task 012-05 - Implementar fila de sync, reconciliação de backlog e contratos de repositório

## Status

Concluída em 2026-07-24.

## Objetivo

Implementar a fila persistida genérica de sincronização, a reconciliação do backlog herdado e os contratos de repositório necessários para processar operações por agregado e tipo.

## Contexto

Após a expansão do domínio e do schema, a fase precisa transformar o estado atual fragmentado entre `HeartRateRepository` e `HealthSyncRepository` em uma API única e coerente para a fila persistida do Health Connect.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-04-expand-sync-domain-record-types-identities-and-room-schema.md`

## Dependências

- `012-04`.

## Entradas

- schema e domínio de sync expandidos;
- backlog real mapeado em `012-01`.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-05-implement-sync-queue-reconciliation-and-repository-contracts.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- `AndroidManifest.xml`
- `feature/settings/**`
- `docs/decisions/**`
- `docs/roadmap/**`

## Escopo

- implementar contratos equivalentes a `ensureOperation`, `findProcessable`, `observeSyncSummary`, `findOperationsForAggregate`, `markStarted`, `markSynced`, `markFailed`, `resetInterrupted`;
- reconciliar backlog de D1, manual, resumo diário, peso e altura;
- preservar operações D1 existentes;
- não enfileirar atividades `READY_TO_SYNC` em reconciliação genérica.

## Fora do escopo

- Health Connect SDK ou gateway;
- UI;
- sync explícito de atividades.

## Passos

1. Estabilizar contratos de fila no domínio.
2. Implementar operações persistidas no `RoomRepositories`.
3. Implementar reconciliação de backlog herdado.
4. Implementar repair de operações antigas quando necessário.
5. Atualizar spec e overview.

## Critérios de aceite

- fila suportando múltiplos tipos de record;
- backlog herdado reconciliável sem duplicar operações;
- `SYNCING` abandonado resetável;
- atividades `READY_TO_SYNC` continuam fora da reconciliação genérica.

## Validação manual

- revisão de regras de chave natural;
- inspeção do diff da camada `data` e contratos.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- diff dos contratos;
- diff do `RoomRepositories`;
- diff das regras de repair e reconciliação.

## Screenshot quando visual

- não se aplica.

## Atualização do overview

- marcar `012-05` como concluída;
- registrar estabilização dos contratos da fila.

## Execução

### Arquivos atualizados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-05-implement-sync-queue-reconciliation-and-repository-contracts.md`

### Implementação concluída

- `HealthSyncRepository` foi estabilizado com contratos de ensure, busca processável, resumo observado, consulta por agregado, transições, reset de interrupção e reconciliação de backlog.
- `HealthSyncOperationDao` passou a expor consultas por agregado, resumo agregado da fila, lote processável e operações `SYNCING` para repair.
- `RoomRepositories` agora reconcilia backlog herdado de resumo diário, BPM manual, D1, peso e altura do perfil atual sem enfileirar atividades `READY_TO_SYNC`.
- operações legadas sem `clientRecordId`, versão ou timestamps novos agora recebem repair idempotente na mesma camada de repositório.
- novas operações D1 criadas pela persistência já nascem com identidade estável e metadados coerentes com a fila expandida.

### Validação executada

- `./gradlew :app:compileDebugKotlin` concluído com sucesso.

### Observações

- `clientRecordId` estável foi fixado no formato `<applicationId>:<aggregateType>:<aggregateId>:<recordType>` usando prefixo estável do app.
- workflow explícito de atividades permaneceu fora da reconciliação genérica e segue para tasks posteriores da fase.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
