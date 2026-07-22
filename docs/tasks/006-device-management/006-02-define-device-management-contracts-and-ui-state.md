# Task 006-02 — Definir contratos e UI state do gerenciamento

## Status

Pendente.

## Objetivo

Definir modelos de estado, eventos, intents e contratos de feature para gerenciamento do dispositivo, sem implementar comportamento pesado.

## Contexto

Fase 06 precisa separar estados persistidos, candidatos efêmeros, sessão BLE e operação ativa. Estado atual da PoC usa `DeviceUiState` técnico e não deve virar estado final sem adaptação.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/004-native-ble-core.md`

## Dependências

- `006-01`.

## Entradas

- Estado técnico da PoC em `DeviceFeatureContract.kt`.
- Estados BLE em `ChampionBleState` e erros em `ChampionBleError`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceFeatureContract.kt`
- novos arquivos em `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/` com prefixo `DeviceManagement`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-02-define-device-management-contracts-and-ui-state.md`

## Arquivos Proibidos

- `DeviceScreen.kt`
- `DevicePocCoordinator.kt`
- `core/bluetooth/**`
- `core/database/**`
- `data/**`
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Criar contrato final separado da PoC quando necessário.
- Modelar candidato descoberto, dispositivo associado visível, estado de conexão, erro seguro e ação retryable.
- Garantir que estados obrigatórios da spec estejam representáveis.
- Não criar booleanos contraditórios.

## Fora Do Escopo

- Persistência.
- Conexão real.
- UI final.
- Testes extensos.

## Passos

1. Ler documentos obrigatórios.
2. Definir `DeviceManagementUiState` ou equivalente.
3. Definir eventos/intents da tela.
4. Definir modelos de item de scan com identificador seguro.
5. Definir enums/sealed classes para conexão e operação ativa.
6. Preservar contratos técnicos da PoC debug.
7. Atualizar overview.

## Critérios De Aceite

- Estado expressa sem dispositivo, permissões, Bluetooth desligado, scan, resultados, conectando, conectado, falha, retry, disconnect e forget.
- Estado separa associado, selecionado, conectado e candidato.
- Estado não usa `BluetoothGatt`, Room, DAO ou tipos Android BLE.
- PoC debug continua compilável por contrato.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Resumo de estados criados.
- Confirmação de que nenhum scan/conexão foi executado.

## Atualização Do Overview

- Marcar `006-02` como concluída.
- Registrar arquivos novos em risco de conflito se necessário.

## Validação Final

- Registrar comando executado e resultado.
- Registrar falhas como regressão se causadas pela task.
