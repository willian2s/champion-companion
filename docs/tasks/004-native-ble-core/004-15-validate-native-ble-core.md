# Task 004-15 — Validar Núcleo BLE Nativo

## Status

Pendente.

## Objetivo

Consolidar evidências da Fase 04, validar critérios da spec e encerrar `004-00`.

## Contexto

Validação final deve confirmar que núcleo BLE está pronto para Fase 05 sem já implementar handshake ou PoC física.

## Documentos Obrigatórios

- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- todas tasks `004-01` a `004-14`

## Dependências

- `004-01` a `004-14` concluídas.

## Entradas

- código BLE criado;
- testes e evidências das tasks anteriores.

## Arquivos Exatos Permitidos

- `docs/tasks/004-native-ble-core/004-15-validate-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`, somente para checklist/status final

## Arquivos Que Não Podem Ser Modificados

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`

## Escopo

- revisar arquitetura, limites e rastreabilidade;
- executar validação de build e testes;
- confirmar ausência de A1/D0/D1/D3/D4 funcionais;
- consolidar lacunas físicas remanescentes para Fase 05.

## Fora Do Escopo

- commit;
- push;
- scan ou conexão reais com relógio;
- alterar roadmap ou ADRs.

## Regras De Segurança

- registrar bloqueios reproduzíveis em vez de mascará-los;
- não tratar `TransportReady` como handshake concluído;
- não validar fisicamente Champion C001 nesta fase.

## Passos De Implementação

1. Revisar componentes BLE contra a spec e checklist.
2. Confirmar fronteira Android BLE única e ausência de vazamento de `BluetoothGatt`.
3. Executar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`.
4. Executar instrumentados mínimos se houver e registrar resultado ou bloqueio reproduzível.
5. Confirmar que A1, D0, D1, D3 e D4 seguem fora do escopo funcional.
6. Atualizar `004-00` com checklist final e este arquivo com evidências.

## Critérios De Aceite

- build e testes relevantes passam ou possuem bloqueio explícito;
- núcleo BLE cumpre limites da spec;
- Fase 05 pode iniciar PoC física sem refatoração estrutural do core.

## Testes

- `lint`
- `testDebugUnitTest`
- `assembleDebug`
- `assembleRelease`
- `connectedDebugAndroidTest`, se aplicável

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Validação Final

- listar arquivos criados/alterados;
- resumir arquitetura, state machine, fila, timeout/cancelamento/retry;
- listar canais obrigatórios e opcionais;
- listar decisões que pedem ADR;
- confirmar ausência de implementação funcional A1/D0/D1/D3/D4;
- confirmar ausência de alteração em roadmap, ADRs e fases anteriores;
- mostrar `git status --short`;
- confirmar sem commit e sem push.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- checklist de `004-00` consolidado.
