# Task 006-07 — Implementar conexão do dispositivo lembrado e retry

## Status

Concluida em 2026-07-22.

## Objetivo

Permitir conectar manualmente dispositivo lembrado ou candidato selecionado, executar handshake A1, atualizar associação/última conexão após sucesso funcional e oferecer retry controlado.

## Contexto

Conexão funcional exige `TransportReady` mais A1 confirmado ou assumido conforme regra da Fase 05. Falhas antes disso não podem substituir associação nem atualizar última conexão.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-03-implement-device-association-persistence.md`
- `docs/tasks/006-device-management/006-04-promote-reusable-poc-connection-components.md`
- `docs/tasks/006-device-management/006-05-create-device-management-viewmodel.md`
- `docs/tasks/006-device-management/006-06-implement-discovery-and-device-selection.md`
- `docs/protocol/handshake-a1.md`

## Dependências

- `006-05`
- `006-06`

## Entradas

- ViewModel.
- Coordinator/bootstrap final.
- Repository de associação.
- `ChampionConnectionManager`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagement*.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnection*.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHandshakeA1.kt` somente se ajuste mínimo necessário
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt` somente se contrato de conexão revelar operação faltante mínima
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt` somente se contrato mínimo faltante vier desta task
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-07-implement-saved-device-connection-and-retry.md`

## Arquivos Proibidos

- `DeviceScreen.kt`
- `core/database/**`, salvo se bloqueio direto de contrato criado em `006-03`.
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Conectar candidato selecionado.
- Conectar dispositivo lembrado por ação do usuário.
- Executar bootstrap completo com A1.
- Persistir associação após sucesso funcional.
- Atualizar última conexão somente após sucesso funcional.
- Implementar retry manual por último erro.
- Refletir disconnect inesperado no estado.

## Fora Do Escopo

- UI final.
- Auto-connect.
- Reconnect infinito.
- D1/D3/D4/Health Connect.
- Persistir última sincronização inventada.

## Passos

1. Ler documentos obrigatórios.
2. Implementar conexão manual de candidato.
3. Implementar conexão manual de dispositivo lembrado.
4. Persistir associação somente depois de bootstrap funcional.
5. Atualizar última conexão no mesmo ponto de sucesso funcional.
6. Implementar retry por ação atual/erro atual.
7. Garantir falha não substitui associação.
8. Rodar validação mínima.
9. Atualizar overview.

## Critérios De Aceite

- `Conectar` não roda sem ação do usuário.
- `Connected` só aparece após A1 concluído/assumido pela regra validada.
- Falha antes do sucesso não altera associação.
- Última conexão não atualiza antes de sucesso funcional.
- Retry é manual e finito.
- Disconnect inesperado vira estado seguro e retryable.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Descrição dos pontos exatos de persistência de associação e última conexão.
- Confirmação de que não há auto-connect nem loop.

## Atualização Do Overview

- Marcar `006-07` como concluída.

## Validação Final

- Registrar comando e resultado.

## Execucao

- `DeviceManagementViewModel` manteve conexao manual de candidato selecionado e de dispositivo lembrado somente por intents explicitas `ConnectSelectedCandidate` e `ConnectAssociatedDevice`; `init` continua sem auto-connect.
- Bootstrap funcional continua centralizado em `DeviceConnectionBootstrap`, que so retorna sucesso apos `ChampionConnectionManager.connect(...)` atingir `TransportReady` e `DevicePocHandshakeA1` concluir como `HANDSHAKE_CONFIRMED` ou `HANDSHAKE_ASSUMED`.
- Persistencia apos sucesso funcional foi fechada em ponto unico via `DeviceRepository.associateDeviceAndUpdateLastSuccessfulConnection(...)`, usado apenas depois de `connectAndHandshake(...)` retornar sucesso.
- Falhas antes desse ponto nao chamam persistencia, portanto nao substituem associacao existente nem atualizam `lastSuccessfulConnection`.
- Retry manual continua finito e orientado por `lastConnectIntent` + `retryAction`, repetindo somente reconexao do associado ou reconexao do candidato selecionado conforme ultimo erro.
- Disconnect inesperado continua chegando por `ChampionBleState.Failed`, que a `ViewModel` converte em estado seguro retryable com erro sanitizado.

## Evidencia Final

- Ponto exato de persistencia da associacao e ultima conexao: dentro do ramo de sucesso de `connectionBootstrap.connectAndHandshake(candidate)` em `DeviceManagementViewModel.persistAssociation(...)`, agora por chamada transacional unica `associateDeviceAndUpdateLastSuccessfulConnection(...)`.
- Confirmacao de ausencia de auto-connect/loop: `DeviceManagementViewModel.init` apenas observa repository, estado de transporte e `scanResults`; nao chama `scan`, `connect` nem `connectAndHandshake` automaticamente.

## Validacao Final

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- `BUILD SUCCESSFUL` com configuration cache reutilizado.
