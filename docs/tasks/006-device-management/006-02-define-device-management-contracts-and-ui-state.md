# Task 006-02 — Definir contratos e UI state do gerenciamento

## Status

Concluida em 2026-07-21.

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

## Implementação Executada

### Arquivos Alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementUiState.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementMappings.kt`

### Estados E Contratos Criados

1. Contrato final separado da PoC.
   - `DeviceManagementFeatureContract` exposto como contrato de estado + `onIntent(...)`.
   - `DeviceManagementIntent` cobre loading, adicionar dispositivo, retry, cancelamento de scan, selecao, conectar, desconectar, esquecer, abrir diagnostico e exportar.
   - `DeviceManagementEvent` cobre efeitos one-shot de permissao, Bluetooth, confirmacao de esquecimento, diagnostico e compartilhamento.

2. UI state final da feature.
   - `DeviceManagementUiState` passou a concentrar:
     - `screenState` primario;
     - `associatedDevice` persistido visivel;
     - `selectedCandidateId`;
     - `candidates` efemeros;
     - `connectionState`;
     - `activeOperation`;
     - `error` seguro;
     - `retryAction`.

3. Estados obrigatorios da spec representados.
   - `Loading`
   - `NoAssociatedDevice`
   - `PermissionsRequired`
   - `BluetoothDisabled`
   - `ReadyToScan`
   - `Scanning`
   - `ScanResults`
   - `Connecting`
   - `PreparingConnection`
   - `Connected`
   - `Disconnecting`
   - `Disconnected`
   - `DeviceNotFound`
   - `ConnectionFailed`
   - `Forgetting`
   - `Error`

4. Modelos de apresentacao criados.
   - `DeviceManagementAssociatedDevice`
   - `DeviceManagementScanCandidate`
   - `DeviceManagementError`
   - `DeviceManagementRetryAction`
   - `DeviceManagementPermission`

5. Separacao de estados exigida pela task.
   - associado: `associatedDevice`
   - candidato encontrado: `candidates`
   - candidato selecionado: `selectedCandidateId`
   - conexao atual: `connectionState`
   - operacao em andamento: `activeOperation`

6. Adaptadores leves para BLE core.
   - `DeviceManagementMappings.kt` traduz `ChampionBleState` para `DeviceManagementConnectionState`.
   - `ChampionBleError` agora pode ser mapeado para `DeviceManagementError` e `DeviceManagementRetryAction` sem expor tipos Android BLE.

### Preservacao Da PoC

- `DeviceFeatureContract.kt` nao foi alterado.
- `DeviceUiState` tecnico da PoC foi preservado.
- Nenhum comportamento de scan, conexao ou handshake foi executado por esta task.

### Validação Executada

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- primeira execucao falhou por erro local de compilacao em nome de enum (`Disconnected` vs `DISCONNECTED`);
- regressao corrigida na mesma task;
- segunda execucao concluiu com `BUILD SUCCESSFUL`.

### Evidência Final

- Estado final da feature foi definido sem tocar `DeviceScreen.kt`, `DevicePocCoordinator.kt`, `core/bluetooth/**`, `core/database/**` ou `data/**`.
- Nenhum scan ou conexao real foi executado.
