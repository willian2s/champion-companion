# Task 007-00 — Overview Home e Resumo Diário

## Status

Planejada. Nenhuma task de implementação concluída ainda.

## Objetivo

Controlar escopo, ordem, dependências, paralelismo, checklist, validação e dívida técnica da Fase 07, que transforma FEA1 solicitado por pull-to-refresh em resumo diário local exibido na Home.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-07-daily-summary-home-direction.md`
- `docs/specs/007-daily-summary-home.md`
- ADRs vigentes em `docs/decisions/`
- `docs/decisions/adr-007-controlled-associated-device-auto-connect.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- todas as tasks concluídas em `docs/tasks/006-device-management/`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/tasks/003-domain-and-persistence-foundation/003-00-overview.md`
- `docs/protocol/`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/baseline/current-baseline.md`

## Estado Herdado Da Fase 06

- Home ainda mostra `Painel em preparação`.
- `HomeFeatureContract.kt` não possui estado real.
- Fase 06 entregou associação, conexão manual, auto-conexão controlada, retry, disconnect, forget e diagnóstico seguro.
- ADR-007 permite auto-connect controlado do associado, mas não permite sync automático.
- Parser FEA1 existe como `DevicePocFea1Steps`, ainda técnico.
- `DailySummaryRepository.save()` faz upsert simples e não protege valor maior contra leitura menor.
- `DeviceImportMetadata.lastImportedAt` existe, mas não há operação de atualização por import real.
- `AppPreferences` não tem `dailyStepsGoal`.

## Limites

- Pull-to-refresh é ação principal.
- Sem botão permanente de sync no estado normal.
- Sem sync automático ao abrir Home/app.
- Sem collector permanente salvando FEA1.
- Sem D1, D3, D4, Health Connect, BPM produtivo, último treino produtivo ou background BLE.
- Não criar task exclusiva de testes automatizados.
- Testes pequenos entram junto das tasks que protegem regra crítica.

## Lista De Tasks

1. `007-01-audit-home-daily-summary-and-fea1-flow.md`
2. `007-02-define-daily-sync-contracts-and-ui-state.md`
3. `007-03-promote-fea1-parser-and-create-sync-coordinator.md`
4. `007-04-implement-atomic-daily-summary-consolidation.md`
5. `007-05-implement-goal-and-estimation-policy.md`
6. `007-06-create-home-viewmodel.md`
7. `007-07-create-pull-to-refresh-home-ui.md`
8. `007-08-integrate-refresh-progress-retry-and-day-rollover.md`
9. `007-09-run-manual-daily-summary-validation.md`
10. `007-10-close-daily-summary-home-phase.md`

## Ordem

1. `007-01`
2. `007-02`
3. `007-03`
4. `007-04`
5. `007-05`
6. `007-06`
7. `007-07`
8. `007-08`
9. `007-09`
10. `007-10`

## Dependências

| Task | Depende De |
| --- | --- |
| `007-01` | leitura inicial completa |
| `007-02` | `007-01` |
| `007-03` | `007-02` |
| `007-04` | `007-02`, `007-03` |
| `007-05` | `007-02` |
| `007-06` | `007-02`, `007-04`, `007-05` |
| `007-07` | `007-02`, `007-06` |
| `007-08` | `007-03`, `007-04`, `007-05`, `007-06`, `007-07` |
| `007-09` | `007-08` |
| `007-10` | `007-09` |

## Paralelismo

- `007-03` e `007-05` podem paralelizar após `007-02` se não editarem `RepositoryContracts.kt` ao mesmo tempo.
- Parte visual de `007-07` pode preparar layout após contrato de `007-06`, mas `HomeScreen.kt` precisa owner único.
- `007-08` deve ser sequencial porque integra coordinator, ViewModel, UI e lifecycle.
- `007-09` e `007-10` são sequenciais.

## Arquivos Com Risco De Conflito

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceConnectionBootstrap.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocFea1Steps.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/AppPreferences.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DomainTime.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/AppPreferencesStore.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/preferences/PreferencesModule.kt`

## Checklist

- [ ] `007-01` auditoria concluída
- [ ] `007-02` contratos e UI state definidos
- [ ] `007-03` parser FEA1 promovido e coordinator criado
- [ ] `007-04` consolidação atômica implementada
- [ ] `007-05` meta e estimativas implementadas
- [ ] `007-06` HomeViewModel criado
- [ ] `007-07` UI pull-to-refresh criada
- [ ] `007-08` progresso, retry e day rollover integrados
- [ ] `007-09` validação manual executada
- [ ] `007-10` fase fechada

## Comandos Mínimos

Gate obrigatório:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Informativos:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

Falhas antigas e não relacionadas não bloqueiam. Regressões introduzidas pela Fase 07 devem ser corrigidas.

## Roteiro De Validação Manual

Galaxy S22 com Champion C001:

1. abrir a Home;
2. confirmar ausência de botão permanente de sincronização;
3. confirmar conteúdo rolável;
4. confirmar instrução de pull-to-refresh;
5. puxar para baixo;
6. confirmar indicador do gesto;
7. confirmar barra indeterminada;
8. confirmar dados anteriores ainda visíveis;
9. conectar quando necessário;
10. receber FEA1;
11. confirmar passos;
12. confirmar origem;
13. confirmar última atualização;
14. repetir com total maior;
15. confirmar substituição;
16. repetir com total igual;
17. confirmar ausência de soma;
18. reproduzir total menor;
19. confirmar ausência de redução;
20. tentar iniciar dois refreshes;
21. confirmar uma única operação;
22. fechar e reabrir o app;
23. confirmar persistência;
24. sincronizar com dispositivo desconectado;
25. validar conexão controlada;
26. desligar Bluetooth;
27. executar refresh;
28. confirmar erro seguro;
29. confirmar barra encerrada;
30. ligar Bluetooth;
31. tentar novamente;
32. negar permissão quando possível;
33. confirmar navegação preservada;
34. validar mudança de dia;
35. confirmar ausência de Health Connect.

## Dívida Adiada

- Campanha ampla Compose.
- Harness BLE determinístico completo.
- Correção ampla de flakes antigos.
- Testes extensivos de timezone.
- Edição de meta e perfil na Fase 16.
- Health Connect na Fase 12.
- Background BLE na Fase 14.

## Critérios De Conclusão

- Home exibe resumo diário persistido.
- Pull-to-refresh sincroniza FEA1 manualmente.
- Auto-connect isolado não persiste FEA1.
- Pacote aceito é fresco e da sessão/tentativa atual.
- Merge é atômico e não reduz passos.
- Última sync só muda após pacote válido e persistência.
- Barras e refresh encerram em todos os caminhos.
- Gate obrigatório e smoke manual registrados.

## Instruções De Atualização Do Checklist

- Cada task atualiza próprio `Status` e item correspondente neste overview no mesmo trabalho.
- Se task bloquear, registrar bloqueio no `Status` e manter checklist pendente.
- `007-10` consolida checklist, comandos, smoke manual, dívida e baseline.
