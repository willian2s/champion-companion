# BLE Transport - MotionLog Para Champion Companion

## Objetivo

Documentar transporte BLE observado no MotionLog e registrar contrato exigido para a camada Android nativa futura do Champion Companion.

## Fontes

- `../motionlog-app/app.config.ts`
- `../motionlog-app/android/app/src/main/AndroidManifest.xml`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/src/lib/ble/champion-runtime.ts`
- `../motionlog-app/src/lib/ble/heart-history-sync.ts`
- `../motionlog-app/src/lib/ble/watch-workout-sync.ts`
- `../motionlog-app/android/app/src/main/java/com/motionlog/app/MotionLogBackgroundBleService.kt`
- `docs/roadmap/champion-companion-roadmap.md`

## Permissoes

| Contexto | Permissoes | Classificacao | Observacao |
| --- | --- | --- | --- |
| Expo config Android | `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Confirmado pelo codigo | `app.config.ts` |
| Manifest gerado | permissao BLE legada, BLE Android 12+, location, foreground service connectedDevice/location, post notifications | Confirmado pelo codigo | `AndroidManifest.xml` |
| Runtime JS Android >= 31 | `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION` | Confirmado pelo codigo | `requestBlePermissionsAsync` |
| Runtime JS Android < 31 | `ACCESS_FINE_LOCATION`, `BLUETOOTH`, `BLUETOOTH_ADMIN` | Confirmado pelo codigo | `requestBlePermissionsAsync` |

## Scan Foreground

1. `startScanAsync` atualiza alvos salvos e limpa lista em memoria.
2. Tenta anexar a dispositivo Champion ja conectado.
3. Se nao anexar, chama `BleManager.startDeviceScan(null, { allowDuplicates: false }, ...)`.
4. Filtro local aceita `device.name` ou `device.localName` cujo texto normalizado comece com `champion`.
5. Dispositivos aceitos entram em `discoveredDevices`; dispositivo salvo pode disparar auto-connect.

Classificacao: `Confirmado pelo codigo`.

## Conexao Foreground

1. Para scan.
2. Estado `connecting`.
3. Reusa device ja conectado quando `connectedDevices([F008_SERVICE, F002_SERVICE, FEE7_SERVICE])` retorna mesmo `deviceId`.
4. Caso contrario chama `connectToDevice(deviceId, { autoConnect: false })`.
5. Chama `discoverAllServicesAndCharacteristics()`.
6. Estado `connected`.
7. `connectToDeviceAsync` chama `subscribeCoreNotificationsAsync`.
8. Envia handshake A1 com senha padrao `0000`.
9. Persiste device local com `bleId` e `lastConnectedAt`.

Classificacao: `Confirmado pelo codigo`.

## Discovery E Subscriptions

Foreground JS assina:

- FEA1 notify, service FEE7.
- F008 notify, service F008.
- F002 notify, service F002.

Background Kotlin assina:

- FEA1 notify, service FEE7.
- F008 notify, service F008.

FEA2 e FEC8 aparecem como indications opcionais no prototipo Web BLE e na spec MotionLog `003-ble-foreground.md`, mas o app atual JS/Kotlin nao assina essas characteristics.

Classificacao: FEA1/F008/F002 `Confirmado pelo codigo`; FEA2/FEC8 `Ainda nao confirmado` para o app atual.

## Escrita

| Fluxo | Metodo MotionLog | Characteristic | Classificacao |
| --- | --- | --- | --- |
| JS foreground | `writeCharacteristicWithResponseForDevice` | F008 write | Confirmado pelo codigo |
| Kotlin background | `BluetoothGatt.writeCharacteristic` | F008 write | Confirmado pelo codigo |
| CCCD Kotlin | `setCharacteristicNotification` + `writeDescriptor(ENABLE_NOTIFICATION_VALUE)` | descritor `00002902-0000-1000-8000-00805f9b34fb` | Confirmado pelo codigo |

Nao foi encontrada escrita para F002 write.

## Sequenciamento

Padrao observado em D1/D3/D4 foreground:

1. garantir device conectado;
2. chamar `subscribeCoreNotificationsAsync`;
3. se `handshakeOk` falso, enviar A1;
4. registrar listener F008 especifico do fluxo;
5. escrever comando F008;
6. coletar pacotes ate condicao de conclusao;
7. encerrar por timeout quando necessario;
8. remover listener especifico.

Nao existe fila GATT serial generica no MotionLog. `subscribeCoreNotificationsAsync` e `writeF008Async` podem ser chamados por fluxos diferentes; protecoes atuais sao flags por fluxo (`heartHistorySyncInProgress`, `watchWorkoutSyncInProgress`) e padrao de uso foreground.

Classificacao: `Confirmado pelo codigo`.

## Timeouts E Retries Observados

| Fluxo | Timeout | Retry | Classificacao |
| --- | --- | --- | --- |
| D1 foreground | `15_000 ms` default | sem retry interno; runtime faz rate-limit de 60s e suprime erro | Confirmado pelo codigo |
| D3 foreground | `10_000 ms` default | sem retry interno | Confirmado pelo codigo |
| D4 foreground | `15_000 ms` default por bloco | sem retry interno; bloco vazio/timeout entra em falha | Confirmado pelo codigo |
| D1 background | `12_000 ms` | ciclo agenda proxima janela; sem retry imediato do pacote | Confirmado pelo codigo |
| Handshake background | delay inicial `1_000 ms`, ate 3 tentativas, delay `1_500 ms` | retry limitado a 3 | Confirmado pelo codigo |
| Reconexao background | backoff fixo `5_000 ms` enquanto monitoramento ativo | repete sem limite enquanto servico ativo | Confirmado pelo codigo |

## Desconexao E Cleanup

Foreground:

- `disconnectAsync` para scan, remove subscriptions, cancela conexao do device conectado e limpa estado sensivel.
- `destroy` remove subscriptions e destroi `BleManager`.

Background:

- `handleStopMonitoring` limpa runnables, encerra ciclo, limpa pacotes, fecha GATT, remove foreground notification e chama `stopSelf`.
- `closeGatt` chama `disconnect()` e `close()` defensivamente.

Classificacao: `Confirmado pelo codigo`.

## Divergencias Entre MotionLog E Champion Companion

| Tema | MotionLog baseline | Champion Companion futuro |
| --- | --- | --- |
| Biblioteca | `react-native-ble-plx` + modulo Kotlin background | APIs Android BLE nativas |
| Fila GATT | nao ha fila generica | fila serial obrigatoria |
| Auto D1 ao abrir | runtime tenta sync D1 apos attach/autoconnect | nao consultar D1 ao abrir |
| Background BLE | foreground service com reconexao repetida | sem BLE permanente agressivo |
| Acesso a GATT | encapsulado em client/servico, mas sem fila formal | somente camada BLE; telas sem GATT direto |

## Requisitos De Transporte Para Implementacao Futura

- Criar `BleTransport`/equivalente como unica fronteira de GATT.
- Serializar connect, discover, enable notification/indication, write, read se existir, disconnect.
- Cada operacao deve ter timeout e cancelar/liberar fila.
- Retry deve ser explicito por comando/fluxo, com limite.
- Listeners devem ser multiplexados por characteristic sem duplicar subscription fisica desnecessaria.
- Commands A1/D0/D1/D3/D4/A8 devem usar F008 write; FEA1/F008/F002 devem entrar como streams tipados.
- Nenhuma tela deve chamar API Android GATT diretamente.

## Lacunas Para `000-14`

- Definir valores de timeout finais do app nativo por fluxo.
- Definir politica de retry por fluxo manual.
- Confirmar se Champion Companion deve assinar FEA2/FEC8 por padrao ou apenas em modo diagnostico.
- Confirmar comportamento real de disconnect/reconnect com Champion C001 fisico.
