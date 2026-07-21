# Task 005-00 — Overview Prova de Conceito BLE Nativa

## Status

Em andamento. `005-01` concluída em 2026-07-21. Auditoria confirmou reuso obrigatório dos contratos da Fase 04 e isolou três gaps estruturais antes da PoC funcional: resolver concreto de capabilities, fechamento do snapshot produtivo de discovery e dispatch reutilizável de `ChampionCommand` acima da fila existente. Próxima task obrigatória: `005-02`.

## Objetivo

Controlar ordem, dependências, checklist, riscos de conflito e protocolo de evidências da Fase 05.

## Gate A

Encerrar somente após dez ciclos completos e consecutivos no Galaxy S22 com Champion C001, sem reiniciar app entre ciclos, com relatório sanitizado por ciclo e cleanup confirmado.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/core/current-project-baseline.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/baseline/source-map.md`
- `docs/baseline/feature-parity-checklist.md`

## Status Inicial

- núcleo BLE da Fase 04 existe e está testado;
- `feature/device` ainda é placeholder;
- manifesto ainda não declara permissões BLE;
- há gap de wiring concreto para atingir `TransportReady` físico com resolver de capabilities e command path real;
- roadmap, ADRs, fases anteriores e input da fase devem permanecer intactos.

## Estado Inicial Esperado

- worktree pode estar suja; preservar mudanças alheias;
- hardware físico pode não estar disponível;
- tasks físicas devem ficar bloqueadas se Galaxy S22 ou Champion C001 não estiverem disponíveis;
- nenhum sucesso com fake substitui validação física.

## Limites Da Fase

- implementar somente PoC técnica física do fluxo BLE mínimo;
- não persistir passos, BPM, dispositivo ou estado PoC;
- não usar Room ou Health Connect;
- não implementar D1, D3, D4, background BLE ou auto-connect.

## Lista Completa Das Tasks

1. `005-00-overview.md`
2. `005-01-audit-phase-04-contracts-and-physical-gaps.md`
3. `005-02-close-phase-04-transport-readiness-gaps.md`
4. `005-03-prepare-ble-permissions-and-poc-entrypoint.md`
5. `005-04-create-poc-state-machine-and-coordinator.md`
6. `005-05-implement-command-dispatch-and-a1-handshake.md`
7. `005-06-implement-fea1-steps-poc.md`
8. `005-07-implement-d0-f002-heart-rate-session.md`
9. `005-08-implement-f002-bpm-estimator.md`
10. `005-09-create-technical-poc-screen.md`
11. `005-10-create-sanitized-cycle-report-and-fixtures.md`
12. `005-11-add-poc-unit-integration-and-ui-tests.md`
13. `005-12-run-first-physical-protocol-validation.md`
14. `005-13-resolve-physical-findings-and-update-protocol-docs.md`
15. `005-14-run-negative-physical-scenarios.md`
16. `005-15-run-ten-cycle-stability-gate.md`
17. `005-16-validate-and-close-native-ble-poc.md`

## Ordem Recomendada

1. `005-01`
2. `005-02`
3. `005-03`
4. `005-04`
5. `005-05`
6. `005-06`
7. `005-07`
8. `005-08`
9. `005-09`
10. `005-10`
11. `005-11`
12. `005-12`
13. `005-13`
14. `005-14`
15. `005-15`
16. `005-16`

## Dependências

- `005-00` deve ser lida antes de qualquer outra task.
- `005-01` antes de decisões de escopo e extensão do core.
- `005-02` antes de qualquer task funcional da PoC.
- `005-03` e `005-04` após `005-02`.
- `005-05` depende de `005-02` e `005-04`.
- `005-06` e `005-07` dependem de `005-05`.
- `005-08` depende de `005-07`.
- `005-09` depende de `005-03`, `005-04`, `005-06`, `005-07`, `005-08`.
- `005-10` depende de `005-04` a `005-08`.
- `005-11` depende de `005-03` a `005-10`.
- `005-12` depende de `005-11` e hardware físico.
- `005-13` depende de `005-12`.
- `005-14` depende de `005-13`.
- `005-15` depende de `005-14`.
- `005-16` é última.

## Tasks Paralelizáveis

- `005-03` e esqueleto inicial de `005-04` após `005-02`.
- `005-09` e `005-10` após `005-08`.
- preparação de testes de `005-11` pode começar em paralelo com `005-09` e `005-10`, mas só fecha depois delas.

## Tasks Obrigatoriamente Sequenciais

- `005-12 -> 005-13 -> 005-14 -> 005-15 -> 005-16`

## Arquivos Compartilhados Com Risco De Conflito

- `docs/specs/005-native-ble-poc.md`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionPacketRouter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/androidTest/**`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/champion-c001.md`

## Riscos

- hardware físico indisponível bloqueia metade final da fase;
- gap estrutural do core pode contaminar tasks funcionais se não for isolado cedo;
- respostas reais do relógio podem divergir da baseline MotionLog;
- permissões Android 12+/11- podem introduzir diferenças de comportamento se não forem separadas com clareza;
- callbacks tardios e subscriptions físicas precisam guarda forte para não poluir novo ciclo;
- tela técnica pode virar feature definitiva por acidente se debug gate não for aplicado;
- relatório pode vazar dado sensível se escopo sanitizado não for testado.

## Checklist

- [x] `005-01` contratos Fase 04 e lacunas físicas auditados
- [ ] `005-02` gaps mínimos de `TransportReady` físico fechados
- [ ] `005-03` permissões e entrypoint técnico preparados
- [ ] `005-04` coordenador e state machine da PoC criados
- [ ] `005-05` handshake A1 implementado e testado
- [ ] `005-06` passos FEA1 implementados e testados
- [ ] `005-07` sessão D0/F002 implementada e testada
- [ ] `005-08` estimador F002 implementado e testado
- [ ] `005-09` tela técnica criada
- [ ] `005-10` relatório sanitizado e fixtures criados
- [ ] `005-11` testes automatizados adicionados
- [ ] `005-12` primeira validação física exploratória executada
- [ ] `005-13` achados físicos resolvidos e protocolo atualizado
- [ ] `005-14` cenários negativos físicos executados
- [ ] `005-15` gate de dez ciclos aprovado
- [ ] `005-16` validação final e fechamento concluídos

## Comandos De Validação

```bash
./gradlew lint testDebugUnitTest assembleDebug assembleRelease
./gradlew app:testDebugUnitTest --tests "*Ble*"
./gradlew app:testDebugUnitTest --tests "*Poc*"
./gradlew app:connectedDebugAndroidTest
git status --short
```

## Protocolo Para Registrar Evidências

1. Cada cycle gera `cycleId` efêmero.
2. Registrar horários, transições de estado, capabilities e contagens, nunca payload ou valor de saúde.
3. Em task física, anotar build, aparelho, relógio, duração configurada e resultado do ciclo.
4. Divergência com MotionLog deve ser documentada como divergência, não corrigida por inferência.
5. Atualizar docs de protocolo somente após evidência física.

## Critérios Gerais De Conclusão

- PoC usa somente núcleo BLE existente com extensões mínimas;
- `TransportReady` permanece separado de A1 confirmado;
- FEA1 e F002 usam `ChampionNotificationHub`;
- respostas usam `ChampionPacketRouter`;
- nenhum dado da PoC entra em Room ou Health Connect;
- relatório exportado é sanitizado;
- dez ciclos consecutivos concluem Gate A.

## Instruções Para Atualização Do Checklist

- cada task altera somente próprio `Status`;
- ao concluir qualquer task da fase, atualizar item correspondente neste overview no mesmo trabalho;
- tasks físicas bloqueadas devem registrar bloqueio explícito;
- `005-16` consolida checklist e evidências finais.
