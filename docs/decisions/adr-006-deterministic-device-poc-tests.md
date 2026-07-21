# ADR-006 — Migrar testes do DevicePocCoordinator para harness determinístico

## Status

Aceita

## Contexto

`DevicePocCoordinatorTest` cobre fluxo mais próximo da PoC BLE em JVM:

- scan;
- conexão até `TransportReady`;
- handshake A1;
- subscription FEA1/F002;
- start/stop D0;
- guards de sessão atual e stale callback.

Esses testes hoje dependem de stack fake baseada em coroutines reais:

- `FakeChampionBleClient` com `SharedFlow` de eventos;
- `ChampionConnectionManager` com collectors assíncronos;
- `ChampionGattQueue` com actor e timeout próprio;
- `ChampionNotificationHub` e `ChampionPacketRouter` com observers concorrentes.

Na prática, parte dessa suíte ficou flakey. Sintomas observados:

- timeout esperando operação enfileirada;
- timeout esperando estado de transporte;
- write A1 expirando mesmo com conclusão explícita da fila;
- observer lógico nem sempre armado a tempo para receber packet fake.

Em 2026-07-21, a task `005-11` ampliou a malha automatizada da PoC e estabilizou parte do harness de conexão:

- `./gradlew app:testDebugUnitTest` ficou verde;
- cobertura JVM do coordenador passou a incluir falha de scan, falha de connect, transição `TransportReady` e guards básicos de cleanup;
- o helper de conexão do teste deixou de depender de `Deferred` preso em `coroutineScope` filho, reduzindo uma fonte concreta de timeout artificial.

Mesmo assim, alguns cenários ainda precisaram ficar com `@Ignore` por instabilidade do fake, não por falta de regra de negócio.

## Decisão

Adotar como direção permanente para testes do `DevicePocCoordinator`:

- manter testes JVM como prioridade para comportamento do coordenador;
- reduzir dependência de scheduling implícito e de timeouts reais em fakes BLE;
- evoluir para harness determinístico que controle explicitamente:
  - operações GATT emitidas;
  - conclusão ou falha de cada operação;
  - ordem de subscriptions;
  - entrega de notifications por sessão e canal;
  - avanço de estado de transporte e protocolo;
- evitar asserts baseados em índices hardcoded de operações quando sequência puder variar;
- deixar cenários ainda flakey com `@Ignore` somente com motivo explícito até migração do harness;
- não tratar teste verde com `@Ignore` como cobertura suficiente para fluxos de ciclo BLE completo.

## Consequências

### Positivas

- reduz risco de regressão silenciosa no coordenador BLE;
- deixa falha de teste mais ligada a regra de negócio e menos a corrida de coroutine;
- já permitiu reativar parte da cobertura essencial sem ampliar escopo para instrumentados;
- facilita reativar cobertura de handshake, steps e D0/F002 sem depender de sleeps implícitos.

### Negativas

- exige investimento adicional em infraestrutura de teste;
- pode duplicar parte do comportamento já coberto em testes lower-level se escopo não for bem separado;
- até migração completa, suíte continua com pequeno conjunto de cenários ignorados no `DevicePocCoordinatorTest`.

## Alternativas consideradas

- manter `@Ignore` indefinidamente e confiar apenas em testes lower-level;
- mover toda cobertura sensível para testes instrumentados;
- aceitar timeouts maiores e mais `yield()` como solução principal.

## Próximos passos

- criar fake de transporte mais alto nível ou driver determinístico para `ChampionGattQueue`;
- reativar primeiro cenários hoje ignorados de A1 success, capability-missing, stale callback e observers FEA1/D0/F002;
- revisar `DevicePocCoordinatorTest` sempre que nova task BLE aumentar dependência de ordering assíncrono.
