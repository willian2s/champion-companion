# 033-07 — Remover entrypoints BLE e calibração por passo da UI

- **Status:** completed
- **Depende de:** 033-05, 033-06 fisicamente validada

## Objetivo

Alinhar a experiência do usuário ao novo ownership sem apagar contratos legacy.

## Ownership de entrada

Contrato visual adotado durante a execução desta task:

```text
Home pull-to-refresh
  -> sincronizar relógio / atualizar dados locais

Home botão Sync
  -> sincronização explícita global com Health Connect
  -> reutiliza SettingsIntent.SyncNow

Configurações -> Gerenciar relógio
  -> DeviceScreen
  -> associação/conexão/manutenção BLE
```

O cabeçalho da Home deixa de usar o relógio como atalho de gerenciamento e passa
a expor a ação remota explícita. Saúde também deixa de expor atalho visual de
dispositivo. Ver ADR-032.

## Saúde

Remover da UI/contrato de produto:

- medir frequência cardíaca manualmente;
- buscar D1;
- cancel/retry/progresso BLE desses fluxos;
- atalho visual de gerenciamento do relógio.

Preservar histórico manual/D1 antigo como leitura.

### Estado da execução

- [x] detalhe de frequência cardíaca já estava somente leitura após o redesign 031;
- [x] atalho visual do relógio removido do cabeçalho Saúde;
- [x] contratos/guards de navegação exclusivos de medição manual/D1 foram removidos; state machines internas permanecem para compatibilidade e watch sync.

## Exercícios

Remover da UI de produto:

- buscar treinos no relógio;
- cancel/retry/progresso de importação própria.

Preservar review, referência de distância, treadmill, reprocessamento de manutenção
e sync explícito.

### Estado da execução

- [x] remover `Importar treinos` do menu;
- [x] remover progresso/snackbar/cancel/retry da importação própria;
- [x] manter status Health Connect, revisão e sync explícito do workout.

## Perfil

Remover affordances de:

- step length manual;
- sugestão por altura;
- perfis/histórico/correção de calibração por passo.

Preservar internamente legacy/fallback. Calibração contextual de distância D4
continua no exercício.

### Estado da execução

- [x] remover card de comprimento de passo;
- [x] remover diálogos de perfis aprendidos e histórico/correção de calibração por passo;
- [x] preservar altura, meta de passos e dados históricos internos.

## Home e Configurações

- [x] substituir ação de relógio do cabeçalho da Home por `Sincronizar com Health Connect`;
- [x] reutilizar o mesmo sync global já validado em Configurações;
- [x] exibir progresso foreground e feedback terminal na Home;
- [x] adicionar `Configurações -> Gerenciar relógio` abrindo o `DeviceScreen` existente;
- [x] preservar pull-to-refresh como única leitura normal do relógio;
- [x] preservar CTA contextual de associação quando a Home não possui dispositivo configurado.

## Guard rails

- não apagar dados históricos;
- não redesenhar superfícies além do necessário;
- preservar baseline visual pós-031;
- não criar segundo pipeline de Health Connect;
- não transformar watch refresh em auto-sync remoto;
- screenshot no PR por ser mudança visual.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/PhoneWorkoutAvailability.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/ChampionD1HeartRateSyncCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceManagementViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileViewModel.kt`
- `app/src/main/res/values/strings.xml`
- `docs/tasks/033-unified-watch-daily-sync/033-06-integrar-health-connect-granular-e-cutover.md` e `033-10-publicar-distancia-calorias-d1-granulares.md` (normalização documental de status já evidenciado)
- este documento e `033-00-overview.md`

## Decisões e desvios

- A UI de Saúde deixou de iniciar medição manual, importação D1 e atualização BLE; histórico continua somente leitura.
- `ManualHeartRateCoordinator`, `D1HeartRateSyncCoordinator`, `WatchWorkoutImportCoordinator` e state machines internas foram preservados para compatibilidade e para os componentes do `WatchSyncCoordinator`.
- Exercícios mantém revisão, calibração contextual de distância, reprocessamento, status Health Connect e sync explícito; somente importação BLE própria e seus feedbacks foram removidos.
- Perfil mantém altura e meta diária de passos; repositories, snapshots, schema, histórico e fallback legacy permanecem intactos.
- Mensagens de Exercícios/Calendário que apontavam para configuração de step length no Perfil foram substituídas por instruções de reprocessamento sem CTA removido.
- Status obsoletos de 033-06/033-10 foram alinhados às evidências físicas já registradas; nenhuma implementação dessas subtarefas foi alterada.
- Mensagens internas de conflito BLE foram neutralizadas para não citar telas removidas, sem alterar categorias, retry ou contratos.
- Mensagem do guard histórico de treino phone foi alinhada ao único entrypoint vigente de leitura do relógio: pull-to-refresh da Home.
- Não foram criados nem executados testes automatizados, conforme `AGENTS.md`.

## Validação executada

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
./gradlew installDebug
```

Validação física manual no `SM-S901E`:

- Home exibe `Sincronizar com Health Connect`, sem voltar a expor gerenciamento BLE no cabeçalho;
- Saúde exibe apenas leitura/análise, sem atalho de relógio, medição manual, importação D1, progresso, cancelamento ou retry;
- menu de Exercícios exibe `Status Health Connect` e `PoC técnica`, sem `Importar treinos`;
- Perfil exibe somente altura e meta diária de passos;
- `Configurações -> Gerenciar relógio` abriu `DeviceScreen` existente;
- screenshots capturadas em `/tmp/033-07-health.png`, `/tmp/033-07-health-heart-rate.png`, `/tmp/033-07-exercises-menu.png`, `/tmp/033-07-profile.png`, `/tmp/033-07-settings.png` e `/tmp/033-07-device.png`.

## Resultados e evidências

- `./gradlew app:compileDebugKotlin`: sucesso.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: instalado no `SM-S901E`.
- `git diff --check`: sem erros de whitespace.
- dumps UI via `adb shell uiautomator dump` confirmaram remoção das affordances descritas e preservação de `Status Health Connect`, revisão e sync explícito.

## Riscos residuais

- Contratos/state machines BLE legados continuam chamáveis internamente; não possuem entrypoint normal nas superfícies removidas.
- Pull-to-refresh da Home não foi alterado nesta subtarefa; comportamento local-first permanece coberto pelas evidências 033-05/033-06.
- Screenshots estão fora do repositório, aguardando eventual anexação ao PR conforme guard rail visual.
