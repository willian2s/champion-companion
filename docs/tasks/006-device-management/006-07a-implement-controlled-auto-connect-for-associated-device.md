# Task 006-07A — Implementar auto-conexão controlada do dispositivo associado

## Status

Concluida em 2026-07-22.

## Objetivo

Adicionar tentativa automática e limitada de conexão do dispositivo já associado ao abrir app ou entrar na tela de dispositivo, sem scan agressivo, sem loop e sem background BLE permanente.

## Contexto

Usuário já espera comportamento do MotionLog: ao abrir app com dispositivo conhecido e pré-requisitos satisfeitos, app deve tentar reconectar sem exigir toque manual toda vez. Fase 06 continua sem foreground service, sem WorkManager BLE e sem reconexão infinita.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-05-create-device-management-viewmodel.md`
- `docs/tasks/006-device-management/006-06-implement-discovery-and-device-selection.md`
- `docs/tasks/006-device-management/006-07-implement-saved-device-connection-and-retry.md`
- `docs/roadmap/champion-companion-roadmap.md`

## Dependências

- `006-05`
- `006-06`
- `006-07`

## Entradas

- ViewModel da feature.
- Conexão do dispositivo lembrado.
- Associação persistida.
- Estado BLE e pré-requisitos.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagement*.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnection*.kt`
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-07a-implement-controlled-auto-connect-for-associated-device.md`

## Arquivos Proibidos

- `DeviceScreen.kt`, salvo wiring mínimo estritamente necessário para lifecycle de entrada.
- `core/database/**`
- `data/**`
- Gradle, baseline e fases concluídas.

## Escopo

- Tentar auto-conectar somente dispositivo associado primário.
- Disparar tentativa ao abrir app ou entrar na tela de dispositivo quando ViewModel/tela final ficarem ativos.
- Executar no máximo uma tentativa automática por entrada visível da feature.
- Exigir pré-requisitos já satisfeitos; não abrir diálogo de permissão automaticamente.
- Permitir resolução direta por `platformIdentifier` e scan curto direcionado somente para dispositivo associado quando necessário.
- Refletir estado de auto-conexão na UI sem bloquear navegação.
- Encerrar em falha segura e retry manual quando tentativa automática falhar.

## Fora Do Escopo

- Background BLE permanente.
- Foreground service.
- Retry infinito.
- Auto-conectar dispositivo diferente do associado.
- Scan amplo recorrente ao abrir app.

## Passos

1. Ler documentos obrigatórios.
2. Definir gatilho de entrada da feature para tentativa única.
3. Reutilizar fluxo de conexão do dispositivo lembrado já existente.
4. Impedir prompt automático de permissão.
5. Impedir loop entre falha, recomposição e nova tentativa.
6. Expor estado/erro seguro para falha de auto-conexão.
7. Rodar validação mínima.
8. Atualizar overview.

## Critérios De Aceite

- App ou tela de dispositivo com associação válida tenta conectar sem ação manual quando pré-requisitos já estão OK.
- Sem permissão ou Bluetooth desligado, UI mostra estado seguro sem prompt automático invasivo.
- Tentativa automática é única por entrada e finita.
- Falha de auto-conexão não troca associação nem entra em loop.
- Retry manual continua disponível.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Resumo do gatilho exato da tentativa automática.
- Confirmação de ausência de loop e de prompt automático de permissão.

## Atualização Do Overview

- Adicionar `006-07A` à sequência e marcar conclusão quando aplicável.

## Validação Final

- Registrar comando e resultado.

## Execucao

- `DeviceManagementIntent.Load` passou a representar entrada visivel da feature. Cada chamada abre uma nova janela de entrada e permite no maximo uma tentativa automatica do dispositivo associado.
- `DeviceManagementViewModel` agora verifica pre-requisitos em `Load`; se permissoes ou Bluetooth nao estiverem prontos, atualiza estado seguro (`PermissionsRequired` ou `BluetoothDisabled`) sem emitir prompt automatico.
- Quando pre-requisitos ja estao OK e existe associacao primaria, a `ViewModel` dispara auto-conexao limitada via `connectAssociatedDevice(automatic = true)`.
- `DeviceConnectionBootstrap.connectAssociatedDevice(...)` tenta primeiro conexao direta pelo `platformIdentifier`; se isso nao bastar por `DeviceNotFound`, `ConnectionFailed` ou `UnexpectedDisconnect`, faz scan curto unico de 5 segundos e conecta apenas se reencontrar exatamente mesmo `transportKey` associado.
- `autoConnectInFlight` mantem estado distinto de resolucao do dispositivo lembrado usando `SEARCHING_REMEMBERED_DEVICE`, sem cair em scan amplo recorrente.
- Falha de auto-conexao nao troca associacao, nao reabre prompt e nao entra em loop; novo auto-connect so ocorre em nova entrada visivel futura (`Load`).

## Evidencia Final

- Gatilho exato: `DeviceManagementIntent.Load` em `DeviceManagementViewModel.onFeatureEntry()` incrementa `currentEntryId`, refresca pre-requisitos e tenta auto-conectar no maximo uma vez para essa entrada.
- Confirmacao de ausencia de loop/prompt automatico: `lastAutoConnectAttemptEntryId` bloqueia repeticao na mesma entrada e `handlePrerequisiteFailure(..., emitResolutionEvents = false)` impede `RequestPermissions` ou `OpenBluetoothSettings` automáticos durante auto-connect.

## Validacao Final

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- `BUILD SUCCESSFUL` com configuration cache reutilizado.
