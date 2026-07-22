# Task 006-09 — Implementar disconnect, esquecer e cleanup de lifecycle

## Status

Pendente.

## Objetivo

Finalizar ações de desconectar, esquecer e cleanup de lifecycle, garantindo associação preservada no disconnect e removida sem apagar histórico no forget.

## Contexto

Disconnect e esquecimento têm semânticas diferentes. Disconnect encerra sessão efêmera; esquecer remove associação persistida. Nenhum dos dois deve apagar histórico.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-03-implement-device-association-persistence.md`
- `docs/tasks/006-device-management/006-07-implement-saved-device-connection-and-retry.md`
- `docs/tasks/006-device-management/006-08-create-usable-device-screen.md`

## Dependências

- `006-07`
- `006-08`

## Entradas

- ViewModel.
- Repository de associação.
- Tela final.
- `ChampionConnectionManager.disconnect`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagement*.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt` somente se esquecer precisar ajuste mínimo
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/` ou `data/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-09-implement-disconnect-forget-and-lifecycle-cleanup.md`

## Arquivos Proibidos

- `core/bluetooth/android/**`
- `core/database/**`, salvo bug direto de associação.
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Implementar `Desconectar` explícito.
- Implementar `Esquecer` com confirmação.
- Garantir cleanup de jobs/collectors da feature.
- Impedir operação nova durante forget.
- Refletir Bluetooth desligado/disconnect inesperado.
- Definir comportamento ao sair da tela durante operação.

## Fora Do Escopo

- Foreground service.
- Background BLE.
- Auto-reconnect.
- Excluir dados históricos.

## Passos

1. Ler documentos obrigatórios.
2. Revisar actions existentes na ViewModel e tela.
3. Implementar disconnect explícito e idempotente.
4. Implementar confirmação de esquecimento.
5. Garantir que falha de disconnect não bloqueia esquecimento local seguro.
6. Limpar estado efêmero após forget.
7. Revisar lifecycle e collectors.
8. Rodar validação mínima.
9. Atualizar overview.

## Critérios De Aceite

- Disconnect preserva associação.
- Forget remove associação e preserva `DeviceEntity`/histórico.
- UI volta ao estado vazio após forget.
- Disconnect inesperado aparece como estado seguro.
- Não há reconnect automático.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Descrição do fluxo de cleanup.
- Confirmação de preservação de histórico ao esquecer.

## Atualização Do Overview

- Marcar `006-09` como concluída.

## Validação Final

- Registrar comando e resultado.
