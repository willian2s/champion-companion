# Task 004-05 — Implementar Scanner BLE Nativo

## Status

Concluida.

Resumo: criados contrato puro `ChampionBleScanner`, wrapper seguro `AndroidScanResultView` e implementação `AndroidChampionBleScanner` com pré-requisitos antes do scan, exclusão mútua, timeout, cancelamento, deduplicação e filtro Champion. Scanner para em sucesso, timeout, cancelamento ou erro. Nenhum `ScanResult` sai da implementação Android e não existe auto-connect.

## Objetivo

Criar scanner foreground nativo, com timeout, cancelamento, exclusão mútua e filtro Champion, sem conectar automaticamente.

## Contexto

O scan é primeiro ponto de contato com Android BLE. Precisa respeitar permissões, proibir scan simultâneo e limpar recursos em qualquer saída.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-03-prepare-permissions-and-bluetooth-prerequisites.md`
- `docs/tasks/004-native-ble-core/004-04-create-champion-uuid-catalog-and-device-filter.md`
- `docs/protocol/ble-transport.md`

## Dependências

- `004-03` e `004-04` concluídas.

## Entradas

- contratos BLE;
- filtro Champion;
- abstrações de permissões/pré-requisitos.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidScanResultView.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `AndroidManifest.xml`
- `feature/**`
- `data/**`

## Escopo

- iniciar e parar scan foreground;
- emitir resultados seguros, deduplicados e filtrados;
- tratar timeout, cancelamento, erro e sucesso;
- impedir scan simultâneo.

## Fora Do Escopo

- conexão GATT;
- discovery;
- handshake;
- PoC física com Champion C001.

## Regras De Segurança

- scan para sempre em sucesso, timeout, cancelamento ou erro;
- sem auto-connect;
- sem expor `ScanResult` fora da implementação Android.

## Passos De Implementação

1. Criar contrato puro de scanner e resultado seguro.
2. Implementar scanner Android com callback convertido para resultado interno estreito.
3. Aplicar pré-requisitos antes de iniciar scan.
4. Garantir timeout, cancelamento e cleanup idempotentes.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- scan simultâneo falha de forma tipada;
- timeout encerra scan e libera estado;
- sucesso encerra scan;
- cancelamento encerra scan;
- componentes externos não recebem `ScanResult`.

## Testes

- JVM para exclusão mútua, timeout e filtro;
- instrumentado apenas para wrappers Android necessários.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*BleScanner*"
```

## Validação Final

- confirmar ausência de conexão implícita ou callbacks Android vazados.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidScanResultView.kt`

Conclusoes:

- `ChampionBleScanner` expoe apenas contrato puro com `scan`, `cancelScan` e estado `isScanning`;
- `AndroidChampionBleScanner` valida permissao e prontidao via `ChampionBlePrerequisites.forScan` antes de iniciar qualquer scan;
- scan simultaneo falha tipadamente com `ScanAlreadyInProgress`;
- callback Android converte `ScanResult` para `AndroidScanResultView`, depois para `ChampionDiscoveredDevice` filtrado e deduplicado;
- sucesso encerra scan no primeiro candidato Champion aceito e devolve lista deduplicada acumulada ate aquele ponto;
- timeout encerra scan com `ChampionBleError.Timeout(ChampionOperationType.SCAN, ...)`;
- cancelamento encerra scan com `OperationCancelled`;
- cleanup de `stopScan` e estado ativo ficou idempotente e sempre limpa `isScanning`.

Limites mantidos:

- sem conexao GATT implicita;
- sem handshake;
- sem vazamento de `ScanResult` ou `BluetoothLeScanner` para contratos publicos.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*BleScanner*"`: falha por ausencia de testes com esse filtro nesta fase;
- busca por `ScanResult`/`BluetoothLeScanner` em `core/bluetooth`: ocorrencias somente em `core/bluetooth/android/**`.

Spec: nenhum ajuste factual necessario.
