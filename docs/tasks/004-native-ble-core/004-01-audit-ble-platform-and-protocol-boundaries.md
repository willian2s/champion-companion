# Task 004-01 — Auditar Fronteiras BLE, Plataforma e Protocolo

## Status

Concluida.

Resumo: fronteira Android BLE deve ficar isolada em implementacao concreta; `TransportReady` depende apenas de discovery, capabilities obrigatorias e subscriptions obrigatorias; F008 e canal critico obrigatorio, F002/FEA1 sao opcionais nao bloqueantes; payloads, MAC, dados de saude e persistencia BLE em Room/Health Connect continuam proibidos; validacao fisica de propriedades GATT, canais opcionais e politica final de timeout/retry fica aberta para Fase 05.

## Objetivo

Confirmar limites arquiteturais, lacunas físicas e contratos herdados da baseline antes de criar código BLE.

## Contexto

Fase 04 não pode copiar comportamento MotionLog cegamente. Precisa separar transporte, protocolo, domínio e UI com base em documentos persistidos.

## Documentos Obrigatórios

- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/baseline/source-map.md`

## Dependências

- `004-00` lida.

## Entradas

- documentação acima;
- estado atual de `app/src/main/java/br/com/mwconsulting/championcompanion`.

## Arquivos Exatos Permitidos

- `docs/tasks/004-native-ble-core/004-01-audit-ble-platform-and-protocol-boundaries.md`
- `docs/specs/004-native-ble-core.md`, somente se auditoria exigir ajuste factual.

## Arquivos Que Não Podem Ser Modificados

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- qualquer arquivo Kotlin nesta task.

## Escopo

- confirmar fronteiras permitidas;
- listar lacunas físicas ainda abertas;
- confirmar canais obrigatórios e opcionais;
- confirmar proibições de logging e acesso a Room/Health Connect.

## Fora Do Escopo

- criar código BLE;
- adicionar permissões;
- alterar manifest;
- criar ADR automaticamente.

## Regras De Segurança

- não promover informação `Ainda não confirmado` a requisito obrigatório;
- não planejar auto-connect, retry infinito ou background BLE;
- não planejar logs com payload.

## Passos De Implementação

1. Revisar documentos autoritativos e confirmar ordem de precedência.
2. Registrar fronteiras entre Android BLE, transporte, protocolo, domínio e UI.
3. Consolidar lacunas físicas que permanecem para Fase 05.
4. Registrar se alguma decisão precisa ADR futura.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- fronteiras arquiteturais documentadas sem ambiguidade;
- lacunas físicas listadas por impacto;
- canais obrigatórios/opcionais confirmados;
- nenhuma decisão especulativa promovida a fato.

## Testes

- não aplicável; auditoria documental.

## Comandos De Validação

- nenhum obrigatório.

## Validação Final

- confirmar que nenhum arquivo Kotlin, Gradle ou Manifest foi alterado.

## Atualização Documental Esperada

- `Status` deste arquivo concluído com resumo das conclusões.

## Ordem De Precedencia Confirmada

Auditoria executada na ordem definida por `docs/specs/_inputs/phase-04-native-ble-core-direction.md`:

1. `AGENTS.md`.
2. `docs/core/current-project-baseline.md`.
3. ADRs vigentes.
4. `docs/specs/_inputs/phase-04-native-ble-core-direction.md`.
5. `docs/specs/004-native-ble-core.md`.
6. Documentos de protocolo.
7. Roadmap.
8. MotionLog somente como referencia funcional.

Regra aplicada nesta auditoria: informacao marcada como `Ainda nao confirmado` permanece lacuna, nunca requisito obrigatório.

## Estado Atual Do App

- Nao existe `core/bluetooth` no codigo Kotlin atual.
- Nao existe import ou uso de `BluetoothGatt`, `BluetoothLeScanner`, `BluetoothAdapter`, `BluetoothManager`, `BluetoothDevice` ou `ScanResult` no app.
- `feature/device/DeviceScreen.kt` ainda e placeholder.
- `core/database`, `data/` e contratos de dominio ja existem e devem continuar desacoplados do nucleo BLE.
- `core/logging/StructuredLogger.kt` ja restringe Logcat debug a schema seguro; Fase 04 deve manter mesmo principio para eventos BLE.

## Fronteiras Arquiteturais Confirmadas

### 1. Android BLE

- Deve existir uma unica fronteira concreta para APIs Android BLE.
- Apenas camada concreta Android pode depender de `BluetoothManager`, `BluetoothAdapter`, `BluetoothLeScanner`, `BluetoothGatt`, `BluetoothGattCallback`, `BluetoothDevice`, `BluetoothStatusCodes` e correlatos.
- `BluetoothGattCallback` deve ser convertido em eventos internos serializaveis; callback nao executa regra de negocio nem parser funcional.

### 2. Transporte BLE

- `core/bluetooth` deve concentrar contratos puros, estados, erros, fila, sessao, catalogo UUID, capabilities, packets e commands.
- Toda operacao GATT deve passar por fila serial unica por sessao.
- `TransportReady` significa somente transporte pronto: conectado, discovery concluido, canais obrigatorios resolvidos e subscriptions obrigatorias configuradas.
- `TransportReady` nao significa handshake A1, autenticacao, D0, D1, D3 ou D4 prontos.

### 3. Protocolo Champion C001

- Protocolo deve permanecer separado de transporte: UUIDs, aliases de canal, envelopes, classificacao estrutural e match de resposta podem existir na camada BLE.
- Parser profundo de A1, D0, D1, D3 e D4 continua fora desta task e fora do caminho critico da Fase 04.
- MotionLog serve como evidencia de referencia, nao como contrato de implementacao automatica.

### 4. Dominio, Persistencia E UI

- `core/model` continua sem dependencia de tipos Android BLE.
- `data/` e `RoomRepositories` nao devem receber `BluetoothGatt`, payloads BLE, estado efemero de sessao ou persistencia automatica de transporte.
- `feature/*`, ViewModels, use cases e Compose devem consumir apenas contratos puros e estados seguros.
- `feature/device` pode orquestrar intencoes e estado futuro, mas nao pode acessar GATT diretamente.

## Canais Obrigatorios E Opcionais Confirmados

### Obrigatorios Para `TransportReady`

- Service F008 `f0080001-0451-4000-b000-000000000000`.
- F008 notify `f0080002-0451-4000-b000-000000000000`.
- F008 write `f0080003-0451-4000-b000-000000000000`.
- CCCD `00002902-0000-1000-8000-00805f9b34fb` acessivel para F008 notify.

### Opcionais Confirmados Como Capability Nao Bloqueante

- F002 notify `f0020002-0451-4000-b000-000000000000`.
- FEA1 notify `0000fea1-0000-1000-8000-00805f9b34fb`.

### Catalogados Sem Obrigatoriedade Atual

- F002 write `f0020003-0451-4000-b000-000000000000`.
- FEA2 `0000fea2-0000-1000-8000-00805f9b34fb`.
- FEC8 `0000fec8-0000-1000-8000-00805f9b34fb`.

Regras consolidadas:

- FEA2, FEC8 e F002 write nao bloqueiam `TransportReady`.
- FEA2 e FEC8 nao devem ser assinados por padrao sem validacao fisica nova.
- Filtro inicial de scan continua por `name` ou `localName` com prefixo `champion`, case-insensitive, sem exigir service UUID.

## Proibicoes Confirmadas

### Logging

- Nao registrar payload bruto, hex, bytes, MAC, ID persistente, BPM, passos, senha A1, credenciais ou dados pessoais no Logcat.
- Debug BLE pode registrar somente metadados seguros, como `session_id` efemero, `operation_id`, alias de canal, codigo do comando, quantidade de bytes, resultado, duracao e categoria de erro.
- Eventual sink diagnostico privado permanece opcional, desacoplado de Room, inativo por padrao e sem retencao indefinida.

### Persistencia E Integracoes

- Nucleo BLE nao pode acessar DAOs, `ChampionDatabase`, `RoomRepositories`, `Health Connect`, GPS ou DataStore de preferencias de produto.
- Fase 04 nao deve persistir automaticamente dispositivo encontrado, conexao, packet, command, payload, estado efemero ou erro de transporte.
- Nenhum componente BLE deve reintroduzir latitude, longitude, rota, polyline ou captura publica.

### Comportamentos Proibidos

- Sem scan automatico ao abrir app.
- Sem conexao automatica ao abrir.
- Sem handshake A1 automatico ao abrir app fora de acao iniciada pelo usuario.
- Sem background BLE permanente.
- Sem auto-connect.
- Sem reconexao infinita.
- Sem retry cego para comandos com potencial de efeito duplicado.

## Lacunas Fisicas Ainda Abertas

### Impacto Alto

- Confirmar no Champion C001 propriedades GATT reais de F008, F002, FEA1, FEA2 e FEC8.
- Confirmar se FEA2 e FEC8 precisam ser tratadas como indications reais no app nativo ou permanecem somente capability diagnostica.
- Definir valores finais de timeout por operacao no app nativo; MotionLog vira referencia, nao verdade automatica.
- Definir politica final de retry segura para conexao, discovery e comandos, sem reconexao infinita.
- Confirmar comportamento fisico de disconnect, callbacks tardios e reconexao curta com sessao nova.
- Confirmar timeout, retry e resposta completa de A1 no dispositivo fisico, inclusive comportamento com senha invalida.

### Impacto Medio

- Confirmar nome e `localName` vistos em scan fisico para evitar filtro `champion` estreito demais.
- Confirmar se F002 notify e FEA1 notify estao sempre disponiveis ou variam por firmware/cenario.
- Confirmar propriedades Android nativas exatas para decidir `notify` versus `indicate` sem inferencia indevida.

### Impacto Baixo

- Confirmar se F002 write possui uso real ou deve permanecer apenas catalogado.
- Confirmar se A0, A8, DF e E0 continuam relevantes apenas para diagnostico futuro.

## ADR Futura Recomendada

Auditoria nao cria ADR, mas confirma temas que merecem registro futuro antes de estabilizar implementacao:

- serializacao da fila GATT e autoridade unica de mutacao da sessao;
- ownership e lifecycle da sessao GATT;
- separacao estrutural entre transporte BLE e protocolo Champion;
- multiplexacao de notifications e subscriptions fisicas;
- politica geral de timeout, cancelamento e retry;
- politica de logging BLE e eventual captura diagnostica privada.

## Conclusao Da Auditoria

- Nenhum ajuste factual em `docs/specs/004-native-ble-core.md` foi necessario.
- Limites arquiteturais estao claros o suficiente para seguir para `004-02` sem promover hipoteses fisicas a requisito.
- Validacao final desta task: nenhum arquivo Kotlin, Gradle ou Manifest foi alterado.
