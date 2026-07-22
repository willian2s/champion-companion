# Task 006-04 — Promover componentes reutilizáveis da PoC

## Status

Concluida em 2026-07-21.

## Objetivo

Promover A1, bootstrap de conexão, permissões, erros seguros, exporter, diagnóstico e estados de protocolo da PoC para contratos reutilizáveis pela feature final.

## Contexto

PoC técnica funciona, mas seus componentes estão acoplados ao fluxo técnico. A feature final não deve importar a tela técnica inteira nem expor passos/BPM.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/ble-command-mapping.md`

## Dependências

- `006-02`.

## Entradas

- `DevicePocHandshakeA1.kt`
- `DevicePocCoordinator.kt`
- `DevicePocReportExporter.kt`
- `ChampionConnectionManager.kt`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHandshakeA1.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocReportExporter.kt`
- novos arquivos em `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/` com prefixo `DeviceConnection` ou `DeviceDiagnostics`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleError.kt` somente se precisar map seguro mínimo
- testes pequenos em `app/src/test/java/br/com/mwconsulting/championcompanion/feature/device/`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-04-promote-reusable-poc-connection-components.md`

## Arquivos Proibidos

- `DeviceScreen.kt`
- `core/database/**`
- `data/**`
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Extrair/reutilizar A1 sem duplicar lógica.
- Criar coordinator/bootstrap final para conexão funcional: connect → `TransportReady` → A1.
- Reutilizar mensagens seguras de erro.
- Preparar exporter/diagnóstico final sem saúde/payload.
- Manter PoC debug acessível separadamente.

## Fora Do Escopo

- UI final.
- Persistência de associação.
- D0/FEA1 na feature final.
- Novo BLE stack.

## Passos

1. Ler documentos obrigatórios.
2. Identificar partes reutilizáveis da PoC.
3. Criar componente de bootstrap final mínimo.
4. Reutilizar `DevicePocHandshakeA1` ou mover nomes com menor churn possível.
5. Garantir que A1 continua testável.
6. Atualizar referências da PoC se necessário.
7. Rodar validação mínima.
8. Atualizar overview.

## Critérios De Aceite

- Feature final consegue chamar conexão funcional sem duplicar A1.
- PoC debug continua compilando.
- Não há scanner/client/manager novo fora Hilt.
- Relatório/diagnóstico continuam sanitizados.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Lista de componentes promovidos.
- Confirmação de que tela técnica não foi convertida em tela final.

## Atualização Do Overview

- Marcar `006-04` como concluída.
- Registrar conflito se arquivos PoC foram tocados.

## Validação Final

- Documentar comando e resultado.

## Execução

- Componentes promovidos: `DeviceConnectionBootstrap`, `DeviceConnectionProtocolState`, `DeviceConnectionSafeError`, `DeviceDiagnosticsSnapshot` e `DeviceDiagnosticsReportExporter`.
- `DevicePocCoordinator` passou a delegar bootstrap funcional `connect -> TransportReady -> A1` para componente reutilizável sem duplicar lógica de handshake.
- `DevicePocReportExporter` passou a reutilizar exporter genérico de diagnósticos; tela técnica PoC continuou separada e não foi convertida em tela final.
- Validação executada: `./gradlew assembleDebug` e `./gradlew app:testDebugUnitTest --tests "*DatabaseMappersTest" --tests "*DeviceDiagnosticsSnapshotTest"`.
