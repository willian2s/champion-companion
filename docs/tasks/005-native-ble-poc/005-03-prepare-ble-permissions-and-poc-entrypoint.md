# Task 005-03 — Preparar Permissões BLE E Entrypoint Da PoC

## Status

Concluída em 2026-07-21.

Resumo: `AndroidManifest.xml` agora declara somente permissões BLE exigidas pela spec, com `BLUETOOTH_SCAN` marcado como `neverForLocation`, permissões legadas limitadas a `maxSdkVersion="30"` e `android.hardware.bluetooth_le` como obrigatório. `feature/device/DeviceScreen.kt` deixou de ser placeholder puro e passou a funcionar como entrypoint técnico debug da PoC: exibe estado de ambiente BLE, permissões observadas, bloqueios atuais do core e botão explícito para solicitar permissões runtime via Activity Result API. Release continua sem expor fluxo técnico. Validações `app:compileDebugKotlin` e `app:lintDebug` passaram.

## Objetivo

Adicionar permissões BLE necessárias, preparar solicitação runtime e criar entrypoint técnico da PoC na área de dispositivo, preferencialmente restrito ao debug.

## Contexto

PoC física depende de permissões corretas por SDK e de uma superfície de entrada explícita pelo usuário. Núcleo BLE já reporta pré-requisitos, mas não solicita permissões nem oferece UI.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/001-android-foundation.md`
- `docs/decisions/adr-005-compose-navigation3.md`

## Dependências

- `005-02` concluída.

## Pré-Condições

- caminho de `TransportReady` físico não depende mais de stubs do core.

## Entradas

- `AndroidManifest.xml`
- `feature/device/**`
- navegação atual.

## Arquivos Exatos Permitidos

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/**`, se entrypoint exigir ajuste mínimo
- `app/src/androidTest/**`, se necessário
- `docs/tasks/005-native-ble-poc/005-03-prepare-ble-permissions-and-poc-entrypoint.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/bluetooth/**`, salvo ajuste mínimo de consumo do readiness já existente
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- declarar permissões BLE por SDK;
- declarar `android.hardware.bluetooth_le`;
- preparar runtime permission request por ação do usuário;
- criar entrypoint técnico da PoC em `feature/device`.

## Fora Do Escopo

- handshake A1;
- FEA1;
- D0/F002;
- feature final de dispositivo.

## Passos

1. Atualizar manifesto com permissões Android 12+ e legadas.
2. Adicionar `neverForLocation` em `BLUETOOTH_SCAN`.
3. Declarar `android.hardware.bluetooth_le` conforme spec.
4. Criar entrypoint técnico debug para tela da PoC dentro da área de dispositivo.
5. Implementar solicitação runtime por Activity Result API e estado seguro para permissões ausentes.
6. Validar ausência de loops e de scan antes da concessão.
7. Atualizar `Status` e overview.

## Regras De Segurança

- não adicionar permissões não usadas;
- não solicitar localização no Android 12+;
- não abrir scan automaticamente;
- preservar `allowBackup=false` e regras de backup.

## Critérios De Aceite

- manifesto declara somente permissões necessárias;
- runtime permission request é iniciado pelo usuário;
- tela mostra permissões ausentes sem tratar negação como erro interno;
- entrypoint técnico não vira fluxo de produto final.

## Testes

- instrumentado para permission request e UI mínima, se necessário.

## Comandos De Validação

```bash
./gradlew app:lintDebug
./gradlew app:connectedDebugAndroidTest
```

## Evidências Esperadas

- estado de permissão em tela;
- screenshot do entrypoint/tela técnica para PR futuro.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que nenhuma permissão fora do escopo foi adicionada.

## Implementação Executada

### Manifesto

Foram adicionados:

- `android.hardware.bluetooth_le` com `required="true"`;
- `android.permission.BLUETOOTH` com `android:maxSdkVersion="30"`;
- `android.permission.BLUETOOTH_ADMIN` com `android:maxSdkVersion="30"`;
- `android.permission.ACCESS_FINE_LOCATION` com `android:maxSdkVersion="30"`;
- `android.permission.BLUETOOTH_SCAN` com `android:usesPermissionFlags="neverForLocation"`;
- `android.permission.BLUETOOTH_CONNECT`.

Não foram adicionados:

- `BLUETOOTH_ADVERTISE`;
- `ACCESS_BACKGROUND_LOCATION`;
- permissões de foreground service;
- localização no Android 12+.

### Entrypoint técnico debug

`feature/device/DeviceScreen.kt` agora:

- mantém placeholder seguro em builds não-debug;
- expõe superfície técnica apenas em build depurável;
- mostra se BLE é suportado, se Bluetooth está habilitado e se scan/conexão estão prontos segundo `ChampionBlePrerequisites`;
- mostra permissões observadas por SDK;
- mostra bloqueios atuais de scan e conexão sem tratá-los como erro interno;
- solicita permissões runtime somente por ação explícita do usuário;
- não inicia scan automaticamente;
- não tenta conectar automaticamente;
- registra refresh de estado ao retomar app, permitindo retry após ida a Settings.

### Runtime permission request

- usa `rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions())`;
- Android 12+: solicita apenas `BLUETOOTH_SCAN` e `BLUETOOTH_CONNECT` quando ausentes;
- Android 11 e inferiores: solicita apenas `ACCESS_FINE_LOCATION` quando ausente;
- permissões legadas `BLUETOOTH` e `BLUETOOTH_ADMIN` são apenas declaradas, não solicitadas em runtime;
- negação não é convertida em erro fatal nem dispara loop.

## Critérios De Aceite Atendidos

- manifesto declara somente permissões necessárias;
- `BLUETOOTH_SCAN` usa `neverForLocation`;
- runtime permission request é iniciado pelo usuário;
- tela mostra permissões ausentes e bloqueios de forma segura;
- entrypoint técnico não virou fluxo final de produto.

## Validação Executada

Comandos executados:

```bash
./gradlew app:compileDebugKotlin
./gradlew app:lintDebug
```

Resultados:

- `app:compileDebugKotlin`: `BUILD SUCCESSFUL`;
- `app:lintDebug`: `BUILD SUCCESSFUL` e relatório gerado em `app/build/reports/lint-results-debug.html`.

Observações:

- `connectedDebugAndroidTest` não foi executado nesta task;
- compilação emitiu warning de depreciação para `LocalLifecycleOwner` importado do pacote antigo, sem bloquear build.

## Evidência Final

- permissões BLE por SDK ficaram declaradas conforme spec;
- entrypoint técnico debug existe em `feature/device`;
- nenhuma permissão fora do escopo foi adicionada;
- nenhuma lógica de A1, FEA1 ou D0/F002 foi introduzida nesta task.
