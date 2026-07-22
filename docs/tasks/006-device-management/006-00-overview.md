# Task 006-00 — Overview Gerenciamento Do Dispositivo

## Status

Em andamento. `006-01`, `006-03`, `006-04` e `006-05` concluidas em 2026-07-21; `006-06`, `006-07`, `006-07A`, `006-08`, `006-09`, `006-10` e `006-11` concluidas em 2026-07-22; resta `006-12`.

## Objetivo

Controlar escopo, ordem, dependências, checklist, validação manual e dívida técnica da Fase 06, que transforma a PoC BLE da Fase 05 em feature utilizável de gerenciamento do Champion C001.

## Documentos Autoritativos

- `AGENTS.md`
- `docs/specs/_inputs/phase-06-device-management-direction.md`
- `docs/specs/006-device-management.md`
- ADRs vigentes em `docs/decisions/`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- todas as tasks concluídas em `docs/tasks/005-native-ble-poc/`
- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/tasks/003-domain-and-persistence-foundation/003-00-overview.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`
- `docs/protocol/history-d1.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/protocol/diagnostics-d8-a8.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/core/current-project-baseline.md` apenas como referência inicial; pode estar desatualizado.

## Estado Real Herdado Da Fase 05

- Fase 05 concluída em 2026-07-21.
- Gate A aprovado por confirmação explícita de dez ciclos físicos consecutivos no Galaxy S22 com Champion C001.
- PoC debug possui state machine explícita, A1, FEA1, D0/F002, estimador F002, relatório sanitizado e exportação explícita.
- `ChampionConnectionManager` ativa `defaultSubscribedChannels` disponíveis antes de `TransportReady`.
- A1 usa builder puro, matcher por `F008_NOTIFY`, delay inicial curto, retry limitado em timeout e tolerância a sucesso assumido quando write conclui e sessão fica íntegra.
- `DeviceScreen` release ainda é placeholder.
- `DeviceScreen` debug ainda constrói infra BLE dentro do Composable; Fase 06 deve remover isso da feature final.
- Hilt já fornece `ChampionBleScanner`, `ChampionBleClient`, `ChampionCapabilityResolver` e `ChampionConnectionManager`.
- Room V1 possui `DeviceEntity`; `DeviceRepository` só observa associado.
- Falhas automatizadas anteriores não bloqueiam Fase 06: teste antigo em `ChampionBleConnectionManagerTest`, flakes ignorados em `DevicePocCoordinatorTest`, falha de tooling no comando agregado de lint/test.

## Limites

- Entregar funcionalidade de gerenciamento de dispositivo.
- Não implementar Home, passos persistidos, distância, calorias, BPM de produção, D1, D3, D4, Health Connect, GPS, background BLE permanente, foreground service, suporte a outros dispositivos ou exclusão de histórico.
- Não criar task exclusiva de testes.
- Testes novos só quando pequenos e diretamente necessários para regra crítica.
- Falhas automatizadas anteriores e não relacionadas devem ser documentadas, não corrigidas por escopo amplo.

## Lista Completa De Tasks

1. `006-00-overview.md`
2. `006-01-audit-device-feature-and-association-strategy.md`
3. `006-02-define-device-management-contracts-and-ui-state.md`
4. `006-03-implement-device-association-persistence.md`
5. `006-04-promote-reusable-poc-connection-components.md`
6. `006-05-create-device-management-viewmodel.md`
7. `006-06-implement-discovery-and-device-selection.md`
8. `006-07-implement-saved-device-connection-and-retry.md`
9. `006-07a-implement-controlled-auto-connect-for-associated-device.md`
10. `006-08-create-usable-device-screen.md`
11. `006-09-implement-disconnect-forget-and-lifecycle-cleanup.md`
12. `006-10-integrate-safe-diagnostics-and-export.md`
13. `006-11-run-manual-device-management-validation.md`
14. `006-12-close-device-management-phase.md`

## Ordem Recomendada

1. `006-01`
2. `006-02`
3. `006-03`
4. `006-04`
5. `006-05`
6. `006-06`
7. `006-07`
8. `006-07A`
9. `006-08`
10. `006-09`
11. `006-10`
12. `006-11`
13. `006-12`

## Dependências

| Task | Depende De |
| --- | --- |
| `006-01` | leitura inicial completa |
| `006-02` | `006-01` |
| `006-03` | `006-01`, `006-02` |
| `006-04` | `006-02` |
| `006-05` | `006-03`, `006-04` |
| `006-06` | `006-05` |
| `006-07` | `006-05`, `006-06` |
| `006-07A` | `006-05`, `006-06`, `006-07` |
| `006-08` | `006-05`, `006-06`, `006-07`, `006-07A` |
| `006-09` | `006-07`, `006-08` |
| `006-10` | `006-05`, `006-08` |
| `006-11` | `006-03` a `006-10`, `006-07A` |
| `006-12` | `006-11` |

## Tasks Paralelizáveis

- `006-03` e `006-04` podem iniciar após `006-02`, se não editarem os mesmos contratos ao mesmo tempo.
- `006-06` e parte visual de `006-08` podem paralelizar após `006-05`, mas `DeviceScreen.kt` deve ter owner único por sessão.
- `006-07A` depende do fluxo de conexão lembrada já estável em `006-07`; evitar paralelizar ambos no mesmo owner de ViewModel.
- `006-10` pode paralelizar com `006-08` após a UI state da ViewModel estar estável.
- `006-11` e `006-12` são sequenciais.

## Arquivos Com Risco De Conflito

- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementUiState.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementMappings.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocHandshakeA1.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocReportExporter.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/`

## Checklist

- [x] `006-01` auditoria e estratégia de associação concluídas
- [x] `006-02` contratos e UI state definidos
- [x] `006-03` persistência de associação implementada
- [x] `006-04` componentes reutilizáveis da PoC promovidos
- [x] `006-05` ViewModel de gerenciamento criada
- [x] `006-06` discovery e seleção implementados
- [x] `006-07` conexão do dispositivo lembrado e retry implementados
- [x] `006-07A` auto-conexão controlada do dispositivo associado implementada
- [x] `006-08` tela utilizável implementada
- [x] `006-09` disconnect, esquecer e lifecycle implementados
- [x] `006-10` diagnóstico e exportação integrados
- [x] `006-11` build, install e smoke manual executados
- [ ] `006-12` fechamento, dívida e baseline planejada concluídos

## Comandos Mínimos De Build

Obrigatórios no gate final da fase:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Informativos, não bloqueantes por falhas anteriores e não relacionadas:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

## Roteiro De Validação Manual

Galaxy S22 com Champion C001:

1. abrir Dispositivo;
2. solicitar permissões;
3. iniciar scan;
4. encontrar Champion C001;
5. conectar;
6. confirmar estado conectado;
7. sair e voltar para a tela;
8. confirmar dispositivo lembrado;
9. fechar e reabrir app;
10. confirmar tentativa automática única de reconexão do dispositivo associado;
11. desconectar;
12. conectar novamente;
13. desligar Bluetooth;
14. verificar estado;
15. ligar Bluetooth;
16. tentar novamente;
17. esquecer dispositivo;
18. confirmar estado vazio;
19. associar novamente;
20. abrir diagnóstico;
21. exportar relatório sanitizado.

## Dívida Técnica Adiada

- Reativar flakes do `DevicePocCoordinatorTest` com harness determinístico.
- Investigar falha antiga em `ChampionBleConnectionManagerTest` se ainda existir.
- Expandir fakes BLE e testes Compose na Fase 21.
- Cobrir estados visuais com campanha instrumentada na Fase 21.
- Diagnóstico avançado fica para Fase 19.
- Migration Room `1 -> 2` foi promovida via auto-migration para criar `device_associations`; seguir observando se próximas evoluções da associação exigirão backfill explícito.
- Evolucao do scanner para lista efemera continua com timeout unico e deduplicacao por chave de transporte; cenarios mais ricos de RSSI/nomes duplicados seguem como risco conhecido da spec.
- PR desta mudanca deve incluir screenshot da nova tela de dispositivo.
- Atualizar `docs/core/current-project-baseline.md` no fechamento da Fase 06, não antes.

## Critérios Gerais De Conclusão

- Release mostra feature real de dispositivo.
- Usuário controla scan e pode intervir na conexão; dispositivo associado pode auto-conectar de forma limitada ao abrir app ou tela.
- Associação persiste separada da sessão BLE.
- Conexão funcional inclui handshake A1.
- Esquecer preserva histórico.
- Diagnóstico/export são sanitizados.
- Não há reconnect infinito nem BLE permanente.
- Builds obrigatórios e smoke manual registrados.

## Instruções Para Atualização Do Checklist

- Cada task atualiza somente próprio `Status` e item correspondente neste overview.
- Ao concluir task, marcar item como `[x]` no mesmo trabalho.
- Se task ficar bloqueada, registrar bloqueio no `Status` da task e manter checklist pendente.
- `006-12` consolida checklist, resultados dos comandos, validação manual, dívida adiada e atualização planejada da baseline.
