# Task 006-05 — Criar ViewModel de gerenciamento do dispositivo

## Status

Pendente.

## Objetivo

Criar ViewModel Hilt que exponha estado da feature, combine associação persistida, ambiente BLE, estado do `ChampionConnectionManager` e ações do usuário.

## Contexto

Telas não podem acessar Room nem BLE infra diretamente. ViewModel será fronteira da UI para scan, connect, retry, disconnect, forget, diagnóstico e export.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-02-define-device-management-contracts-and-ui-state.md`
- `docs/tasks/006-device-management/006-03-implement-device-association-persistence.md`
- `docs/tasks/006-device-management/006-04-promote-reusable-poc-connection-components.md`

## Dependências

- `006-03`
- `006-04`

## Entradas

- Contratos de UI state.
- `DeviceRepository` ampliado.
- Coordinator/bootstrap final.
- `ChampionBlePrerequisites` e ambiente BLE.

## Arquivos Exatos Permitidos

- novos arquivos em `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/` com prefixo `DeviceManagement`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt` somente para binding mínimo ausente
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-05-create-device-management-viewmodel.md`

## Arquivos Proibidos

- `DeviceScreen.kt`
- `core/database/**`
- `data/**`, salvo se dependência de `006-03` ficou incompleta e for bloqueador direto.
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Criar `@HiltViewModel`.
- Expor `StateFlow<DeviceManagementUiState>`.
- Implementar handlers sem UI: refresh ambiente, request permissions intent, start scan, cancel scan, connect, retry, disconnect, forget.
- Não iniciar scan/conexão no `init`.
- Observar associação no `init` é permitido.

## Fora Do Escopo

- Composable final.
- Smoke físico.
- D1/D3/D4/Health Connect.

## Passos

1. Ler documentos obrigatórios.
2. Criar ViewModel com injeção Hilt.
3. Conectar repository e coordinator.
4. Mapear erros para mensagens seguras.
5. Garantir que `init` não inicia BLE.
6. Adicionar testes pequenos para regra crítica se simples.
7. Rodar validação mínima.
8. Atualizar overview.

## Critérios De Aceite

- ViewModel compila com Hilt.
- Estado inicial não inicia scan/conexão.
- Associação observada aparece no estado.
- Ações são explícitas.
- UI não precisa acessar infra diretamente.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Resumo dos eventos públicos da ViewModel.
- Confirmação de ausência de auto-connect/auto-scan.

## Atualização Do Overview

- Marcar `006-05` como concluída.

## Validação Final

- Registrar comando e resultado.
