# Task 006-03 — Implementar persistência de associação

## Status

Concluida em 2026-07-21.

## Objetivo

Adicionar persistência mínima para observar, associar, substituir, atualizar última conexão e esquecer dispositivo associado, preservando histórico futuro.

## Contexto

Estratégia definida: entidade própria de associação em Room. `DeviceEntity` continua representando identidade/histórico do dispositivo. Associação atual é vínculo removível.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`
- `docs/decisions/adr-003-local-persistence-architecture.md`
- `docs/specs/003-domain-and-persistence-foundation.md`

## Dependências

- `006-01`
- `006-02`

## Entradas

- `DeviceEntity` V1.
- `DeviceDao` atual.
- `DeviceRepository` atual.
- Room schema V1.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/2.json`
- testes pequenos diretamente necessários em `app/src/test/**` ou `app/src/androidTest/**`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-03-implement-device-association-persistence.md`

## Arquivos Proibidos

- `DeviceScreen.kt`
- `core/bluetooth/**`
- `app/build.gradle.kts`
- `docs/core/current-project-baseline.md`
- ADRs, roadmap, input da fase e fases concluídas.

## Escopo

- Adicionar `DeviceAssociationEntity` singleton.
- Elevar Room para V2 com migration V1→V2.
- Ampliar `DeviceRepository` com observar associado, associar/substituir, atualizar última conexão, atualizar metadados, esquecer e localizar por identificador.
- Garantir transações para upsert de dispositivo + associação.
- Preservar dados históricos ao esquecer.

## Fora Do Escopo

- UI.
- BLE.
- Sincronização real.
- Exclusão de histórico.
- Campanha ampla de testes.

## Passos

1. Ler documentos obrigatórios.
2. Criar modelo de associação no domínio se necessário.
3. Criar entity e DAO de associação.
4. Adicionar migration `1 -> 2`.
5. Atualizar `DeviceRepository` e implementação Room.
6. Atualizar mappers.
7. Exportar schema V2.
8. Adicionar teste pequeno para esquecer preservar dispositivo ou migration, se viável.
9. Rodar validação mínima.
10. Atualizar overview.

## Critérios De Aceite

- Associação é observável via repository.
- Associar substitui associação primária em transação.
- Falha de conexão não substitui porque API só será chamada após sucesso funcional.
- Última conexão atualiza separadamente.
- Esquecer remove associação sem apagar `DeviceEntity`.
- Migration e schema exportado existem.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Resumo da migration.
- Confirmação de schema V2 exportado.
- Resultado do comando.

## Atualização Do Overview

- Marcar `006-03` como concluída.
- Registrar migration criada em dívida/riscos se houver limitação.

## Validação Final

- Se teste informativo falhar por dívida anterior, documentar e não bloquear salvo regressão direta.

## Execução

- `DeviceAssociationEntity` singleton adicionada em Room V2 com FK para `devices` e schema exportado em `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/2.json`.
- `DeviceRepository` agora observa associação por tabela dedicada e expõe associar/substituir, atualizar última conexão, atualizar metadados, esquecer e localizar por identificador.
- `Device.create(...)` deixou de acoplar timestamps históricos ao `associatedAt`, permitindo re-associação sem invalidar importações antigas.
- Validação executada: `./gradlew assembleDebug` e `./gradlew app:testDebugUnitTest --tests "*DatabaseMappersTest" --tests "*DeviceDiagnosticsSnapshotTest"`.
