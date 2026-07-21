# Task 005-01 — Auditar Contratos Da Fase 04 E Lacunas Físicas

## Status

Concluída em 2026-07-21.

Resumo: Fase 04 já entrega scanner, fila GATT serial, `ChampionConnectionManager`, `ChampionTransportSession`, `ChampionNotificationHub`, `ChampionPacketRouter`, `ChampionCapabilityRegistry`, `ChampionBleState`, `ChampionBleError`, `ChampionProtocolLogger`, bindings Hilt principais e fakes JVM reutilizáveis. Auditoria confirmou três gaps estruturais reais antes da PoC funcional: `ChampionCapabilityResolver` ainda é stub em `ChampionBleModule.kt`, wrappers Android ainda não fecham snapshot concreto de discovery até resolver de capabilities no caminho produtivo, e não existe dispatch genérico completo de `ChampionCommand` para write + await de resposta acima da fila existente. Extensões permitidas devem fechar esses pontos sem redefinir `TransportReady`, sem criar stack BLE paralela e sem mover protocolo para UI.

## Objetivo

Confirmar pontos de reutilização obrigatória da Fase 04, lacunas reais para `TransportReady` físico e pontos de extensão mínimos antes de implementar a PoC.

## Contexto

Fase 05 deve reutilizar o núcleo BLE existente. Auditoria inicial precisa evitar que permissões, A1 e D0/F002 sejam construídos sobre core incompleto ou com atalhos fora das fronteiras.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-00` lida.

## Pré-Condições

- nenhuma.

## Entradas

- estado atual de `core/bluetooth/**` e `feature/device/**`.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-01-audit-phase-04-contracts-and-physical-gaps.md`
- `docs/specs/005-native-ble-poc.md`, somente se ajuste factual for necessário

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- qualquer arquivo Kotlin nesta task

## Escopo

- auditar contratos e bindings existentes;
- listar gaps estruturais reais do core para PoC física;
- confirmar dependências de tasks seguintes;
- registrar decisões que podem exigir ADR futura.

## Fora Do Escopo

- implementar código;
- alterar manifesto;
- criar ADR.

## Passos

1. Revisar `ChampionConnectionManager`, `ChampionTransportSession`, `ChampionGattQueue`, `ChampionCommand`, `ChampionPacket`, `ChampionPacketRouter`, `ChampionNotificationHub`, `ChampionCapabilityRegistry`, `ChampionBleState`, `ChampionBleError` e `ChampionProtocolLogger`.
2. Confirmar bindings Hilt e fakes existentes.
3. Registrar gaps que impedem `TransportReady` físico ou command path da PoC.
4. Mapear onde Fase 05 pode estender o core sem quebrar fronteiras.
5. Atualizar `Status` e resumo.

## Regras De Segurança

- não promover inferência física a requisito confirmado;
- não sugerir stack paralelo BLE;
- não alterar código nesta task.

## Critérios De Aceite

- contratos e reuso obrigatório ficam listados sem ambiguidade;
- gaps estruturais do core ficam identificados antes de tasks funcionais;
- extensões permitidas ficam delimitadas.

## Testes

- não aplicável; auditoria documental e de código.

## Comandos De Validação

- nenhum obrigatório.

## Evidências Esperadas

- lista de gaps reais, como resolver de capabilities, discovery snapshot e dispatch de comandos.

## Atualização Documental

- concluir `Status` com resumo factual.

## Validação Final

- confirmar ausência de alteração em Kotlin, Manifest e roadmap.

## Contratos E Reuso Obrigatório Confirmados

### `ChampionConnectionManager`

- continua autoridade principal para scan, conexão, discovery, capabilities, subscriptions obrigatórias, `TransportReady`, disconnect e cleanup;
- já isola eventos de sessão antiga pelo `sessionId`;
- já mantém semântica correta de `TransportReady` como transporte puro.

### `ChampionTransportSession`

- expõe `session`, `capabilities`, `queue`, `notifications` e `streams`;
- é ponto natural para camada PoC acima do transporte sem vazar `BluetoothGatt`.

### `ChampionGattQueue`

- fila serial por sessão;
- uma operação ativa por vez;
- timeout explícito por operação;
- retry limitado e explícito;
- fecha definitivamente em disconnect.

### `ChampionCommand`

- envelope imutável para código, payload, `writeType`, timeout, retry e matcher;
- já protege payload com cópia defensiva;
- é contrato obrigatório para A1, D0 start e D0 stop.

### `ChampionPacket` E `ChampionPacketRouter`

- `ChampionPacket` mantém bytes copiados, direção, canal, comando e `sessionId`;
- `ChampionPacketRouter` já suporta `awaitPacket`, observers por canal e fechamento seguro;
- resposta da PoC deve continuar passando por matcher estrutural, não por listener ad hoc na UI.

### `ChampionNotificationHub`

- segue obrigatório para F008, FEA1 e F002;
- já centraliza CCCD via fila e evita subscription física duplicada por characteristic;
- continua sendo único caminho permitido para subscriptions da PoC.

### `ChampionCapabilityRegistry`

- continua fonte única para classificação de channels obrigatórios e opcionais;
- mantém F008 write/notify como obrigatórios de transporte;
- mantém F002/FEA1 como opcionais no core, embora virem obrigatórios para ciclo vencedor da PoC.

### `ChampionBleState` E `ChampionBleError`

- `ChampionBleState` continua restrito ao lifecycle de transporte;
- `ChampionBleError` já cobre permissões, environment, capabilities, fila, timeout e payload estrutural inválido;
- estado de handshake deve nascer acima desse modelo, sem redefini-lo.

### `ChampionProtocolLogger`

- já restringe Logcat a metadados seguros;
- segue obrigatório para transporte e observação sanitizada;
- nenhum payload, MAC, passos, BPM, senha ou F002 bruto pode ser promovido para logs na Fase 05.

## Bindings Hilt E Fakes Confirmados

### Hilt

- `ChampionBleScanner <- AndroidChampionBleScanner`;
- `ChampionBleClient <- AndroidChampionBleClient`;
- `AndroidGattDiscoveryMapper` já existe;
- `ChampionConnectionManager` já é provido;
- `ChampionProtocolLogger` e `ChampionDiagnosticPacketSink` já estão ligados.

### Gap de binding confirmado

- `ChampionCapabilityResolver` ainda é fornecido como stub retornando `ChampionBleError.OperationUnavailable`;
- esse gap bloqueia caminho produtivo até capabilities reais e, por consequência, até `TransportReady` físico reutilizável.

### Fakes e helpers

- `FakeChampionBleScanner`;
- `FakeChampionBleClient`;
- `RecordedChampionPacketSource`;
- `FakeMonotonicClock`;
- `BleTestFixtures`.

Esses doubles já suportam testes de fila, roteamento, manager e parte do fluxo PoC sem substituir validação física.

## Gaps Estruturais Reais Confirmados

### G-01 — Resolver de capabilities ainda não produtivo

- Hilt injeta stub;
- PoC física não pode depender dele;
- `005-02` deve fechar esse gap primeiro.

### G-02 — Discovery snapshot concreto ainda não fecha caminho produtivo

- mapeador Android existe, mas callback/cliente ainda não entregam snapshot completo até o resolver concreto no fluxo real;
- caminho precisa permanecer restrito à fronteira Android BLE.

### G-03 — Dispatch genérico de `ChampionCommand` ainda incompleto

- envelope existe;
- match de resposta existe;
- falta caminho único e reutilizável para transformar `ChampionCommand` em write GATT + await de resposta usando fila e router;
- sem isso, A1 e D0 tenderiam a vazar para UI/coordenador de forma indevida.

### G-04 — Sem estado de protocolo acima do transporte

- `ChampionBleState` é suficiente para transporte, mas não para A1, steps e sessão D0/F002;
- Fase 05 deve criar state machine da PoC acima do core, não dentro dele por padrão.

### G-05 — Guardas de sessão/ciclo da PoC ainda inexistentes

- core já guarda `sessionId`, mas PoC ainda precisa `cycleId`, reset efêmero e bloqueio explícito de callback tardio entre ciclos.

## Dependências De Tasks Seguintes Confirmadas

- `005-02` deve acontecer antes de permissões, A1, FEA1 e D0/F002 funcionais;
- `005-03` depende só de `005-02`, porque entrypoint e permissões não resolvem core incompleto;
- `005-04` pode começar após `005-02`, porque state machine precisa depender de transporte reutilizável já fechado;
- `005-05` depende diretamente de dispatch de comando e resolver concreto concluídos em `005-02`.

## Extensões Permitidas Sem Quebrar Fronteiras

- implementar `ChampionCapabilityResolver` concreto com testes;
- enriquecer snapshot de discovery vindo de `core/bluetooth/android`;
- adicionar adaptador de dispatch de `ChampionCommand` sobre fila + router;
- adicionar camada PoC em `feature/device` ou pacote técnico adjacente, desde que sem `BluetoothGatt`;
- adicionar guardas de `sessionId` e `cycleId` acima do transporte;
- ampliar testes JVM e integração usando fakes já existentes.

## Extensões Não Permitidas

- criar segundo stack BLE para PoC;
- acessar callback Android diretamente da feature;
- mandar comandos fora de `ChampionGattQueue`;
- configurar CCCD fora de `ChampionNotificationHub`;
- redefinir `TransportReady` como handshake concluído;
- vazar payload, bytes, MAC ou saúde para Logcat ou relatório.

## Decisões Que Podem Exigir ADR Futura

- ownership duradouro do estado de handshake;
- coordenador de protocolo acima de `ChampionTransportSession`;
- dispatch genérico de `ChampionCommand` como contrato estável do core;
- política oficial de capabilities obrigatórias da PoC versus obrigatórias de transporte;
- estratégia de evidência sanitizada exportável.

## Evidência Final Da Auditoria

- reuso obrigatório da Fase 04 ficou delimitado sem ambiguidade;
- gaps reais do core para PoC física ficaram identificados antes de tasks funcionais;
- `005-02` permanece corretamente isolada como primeira task de implementação;
- nenhuma inferência física foi promovida a comportamento confirmado;
- nenhum arquivo Kotlin, Manifest, roadmap, ADR ou input da fase foi alterado nesta task.
