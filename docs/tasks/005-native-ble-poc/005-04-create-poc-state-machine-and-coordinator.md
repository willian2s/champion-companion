# Task 005-04 — Criar State Machine E Coordenador Da PoC

## Status

Planejada.

## Objetivo

Criar camada de orquestração técnica da PoC com state machine explícita, ownership de ciclo/sessão e regras de cleanup.

## Contexto

`ChampionBleState` representa somente transporte. Fase 05 precisa estado de protocolo e de ciclo sem contaminar o core com booleans concorrentes nem empurrar lógica para Compose.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/004-native-ble-core.md`

## Dependências

- `005-02` concluída.

## Pré-Condições

- dispatch de comando e `TransportReady` físico disponíveis.

## Entradas

- `ChampionConnectionManager`
- `ChampionTransportSession`
- `ChampionBleState`

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, somente se extensão mínima do contrato exposto for necessária
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-04-create-poc-state-machine-and-coordinator.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `app/src/main/AndroidManifest.xml`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- criar estados da PoC;
- criar coordenador técnico observável;
- implementar reset de ciclo e cleanup;
- guardar `sessionId` e `cycleId` para bloquear callbacks tardios.

## Fora Do Escopo

- UI completa;
- handshake A1 funcional;
- parsers FEA1/D0/F002 completos.

## Passos

1. Definir modelo de estado da PoC.
2. Implementar coordenador técnico com dependência de `ChampionConnectionManager` e transporte.
3. Definir regras de transição válidas e ações permitidas por estado.
4. Implementar reset por ciclo, invalidação de handshake e guardas de callback tardio.
5. Cobrir state machine e cleanup com testes JVM.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não redefinir `ChampionBleState` como estado de protocolo;
- não usar mar de booleans;
- não permitir que cleanup dependa do sucesso do stop.

## Critérios De Aceite

- state machine explicita estados principais da PoC;
- coordenador orquestra fluxo sem acessar `BluetoothGatt`;
- reset e cleanup limpam estado anterior;
- callbacks tardios não atualizam ciclo novo.

## Testes

- JVM para state machine, reset, cleanup e stale callback.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Poc*"
```

## Evidências Esperadas

- testes da state machine e de guards de sessão/ciclo.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar ausência de dependência do coordenador com `BluetoothGatt`, Room e Compose.
