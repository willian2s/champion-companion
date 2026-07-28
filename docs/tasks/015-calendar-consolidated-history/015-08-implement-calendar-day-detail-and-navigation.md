# 015-08 — Implementar detalhe diário e navegação

- **Status:** concluída em 28/07/2026.
- **Objetivo:** entregar detalhe diário e fluxo para detalhes relacionados.
- **Resultado esperado:** `CalendarDay(localDateIso)` seguro, consolidado e com
  retorno correto ao calendário.

## Documentos obrigatórios

Spec 015, 015-07, `AppDestination.kt`, `ChampionNavigation.kt`,
`HealthMetricDetailScreens.kt`, `ExercisesScreen.kt`, `ExercisesViewModel.kt` e
Navigation3.

## Dependências e entradas

- 015-07 concluída.
- Read model e ViewModel temporal concluídos.

## Arquivos permitidos

`core/navigation`, `feature/calendar` e adaptadores mínimos de detalhes
existentes; documentação e screenshots.

## Arquivos proibidos

Nova tela duplicada de atividade, DAO, migration, alteração de workflow, BLE,
Health Connect, testes e roadmap/ADR.

## Escopo

Criar destino parametrizado, validar `LocalDate.parse`, mostrar passos,
distância, BPM, peso, atividades e sync, abrir detalhes relacionados e manter
stack `Calendar → CalendarDay → ExerciseDetail`. Corrigir retorno por pop,
preservando mês e data.

## Fora do escopo

Gerenciamento de peso, edição, sincronização, atividade iniciada pelo app,
mudança de ExerciseDetail ou coleta externa.

## Passos

1. Adicionar destino com parsing seguro.
2. Integrar subtree Calendar no back stack manual.
3. Implementar tela de detalhe e estados.
4. Ligar rotas de métricas existentes.
5. Reutilizar ExerciseDetail.
6. Validar back, inválido e restauração durante processo.

## Critérios de aceite

- Data inválida não derruba a navegação.
- Detalhe usa apenas snapshot local.
- Atividade abre detalhe existente.
- Back de atividade retorna ao mesmo dia.
- Back de dia retorna ao mesmo mês/data.
- O detalhe mostra passos, meta atual, distância estimada, BPM, atividades, peso
  e sync.
- Cards de atividade mostram título/tipo, início, duração, distância final,
  calorias, BPM médio, workflow e status de sync.
- Peso mais recente aparece como resumo e demais medidas ficam ordenadas.
- Aggregate ausente, operação órfã e degradação parcial têm estado seguro e não
  são apresentados como sucesso.

## Validação manual

Galaxy S22: abrir data com dados, sem dados, atividade, BPM, passos e distância;
usar back em cada nível. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar rotas, comportamento do stack, parsing inválido e screenshots.
Atualizar overview após aceite.

## Validação final

Confirmar que nenhuma tela de atividade foi duplicada e que nenhuma operação
externa começa ao abrir detalhe. Não criar, alterar ou executar testes
automatizados.

## Contexto

Navigation3 usa back stack manual e `ExerciseDetail` é compartilhado; o detalhe
diário precisa preservar a origem sem duplicar a tela de atividade.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-08-calendar.png`;
- `docs/tasks/015-calendar-consolidated-history/015-08-calendar-day-detail.png`;
- `docs/tasks/015-calendar-consolidated-history/015-08-calendar-day-activity.png`;
- `docs/tasks/015-calendar-consolidated-history/015-08-exercise-detail.png`.

## Decisões e desvios

- `CalendarDay` mantém argumento bruto e expõe parsing via `LocalDate.parse` protegido
  por `runCatching`; rota inválida renderiza erro recuperável sem lançar durante
  composição.
- Back stack continua manual, mas usa `rememberSaveable` com `Saver` dos destinos.
  A subtree de calendário é removida antes de trocar para outro destino de topo;
  somente Calendário permanece selecionado nos detalhes relacionados.
- `CalendarViewModel` atualiza mês e data juntos quando uma rota válida restaurada
  aponta para outro mês, preservando invariante do estado.
- Detalhe usa exclusivamente `CalendarDaySummary`. Cards distinguem ausência,
  zero real e falha de fonte; falha da fila local não é apresentada como
  `Sem operações` ou sucesso.
- Métricas existentes e `ExerciseDetail` continuam compartilhados. Adaptadores
  receberam apenas rótulo de retorno contextual e pop contextual.
- Nenhum desvio aprovado; nenhuma tela de atividade, DAO, migration, workflow,
  BLE, Health Connect ou teste foi criado/alterado.

## Screenshot

Screenshots registrados:

- `015-08-calendar.png`: calendário mensal com seleção e indicadores;
- `015-08-calendar-day-detail.png`: detalhe de 28/07/2026 com passos e distância;
- `015-08-calendar-day-activity.png`: BPM e card de atividade relacionado;
- `015-08-exercise-detail.png`: `ExerciseDetail` compartilhado.

## Riscos residuais

- Rota inválida não possui deep link público no app; parsing foi validado por
  inspeção do destino e estado visual seguro, mas não por entrada externa manual.
- Morte completa do processo não foi reproduzida no aparelho; `rememberSaveable`
  e `Saver` cobrem restauração do back stack, enquanto rotação foi validada.
- Estado sintético de falha da fila local não foi provocado no aparelho; código
  mantém `Status indisponível` quando `HEALTH_SYNC_QUEUE` estiver em issue.
- Telas métricas existentes continuam mostrando dados atuais, não filtrados pelo
  dia; detalhe diário preserva contexto ao voltar e não altera essas telas.

## Comandos executados

- `./gradlew :app:compileDebugKotlin` — sucesso.
- `./gradlew lint assembleDebug assembleRelease` — sucesso.
- `./gradlew installDebug` — sucesso no Galaxy S22 `SM-S901E - 16`.
- `adb shell svc bluetooth disable` — Bluetooth desativado para validação offline.
- `adb shell am force-stop ... && adb shell monkey ...` — app reiniciado sem
  operação externa observável.
- Capturas via `adb exec-out screencap -p` — quatro screenshots registrados.
- `adb shell input keyevent 4` — back de atividade para dia e de dia para calendário.
- `adb shell settings put system user_rotation 1/0` — rotação landscape e retorno;
  data diária preservada.
- Testes automatizados não criados, alterados ou executados.

## Resultados e evidências

- Rota observada: `Calendar → CalendarDay(2026-07-28) → ExerciseDetail`.
- Back observado: atividade retorna ao detalhe de 28/07/2026; dia retorna ao
  calendário em julho de 2026 com data 28 selecionada.
- Detalhe exibiu passos registrados, `Meta atual` 8.000, distância indisponível,
  83 BPM em 31 leituras e duas atividades com campos de workflow/sync.
- Data sem registros em 29/07/2026 abriu sem valores sintéticos.
- Link de BPM abriu tela existente; somente Calendário permaneceu selecionado
  na barra inferior e retorno preservou a subtree.
- Rotação preservou `Detalhe diário` e 28/07/2026.
- Após limpar Logcat, abrir app/calendário com Bluetooth desativado não gerou
  eventos do app para BLE ou Health Connect.
- `git diff --check` sem ocorrências.

## Resultados da revisão independente

- Revisão inicial encontrou crash ao abrir data de outro mês, seleção ambígua,
  estado de fila enganoso e documentação incompleta.
- Correções foram aplicadas e recompiladas; revisão final independente aprovou
  implementação, documentação e evidências em 28/07/2026.

## Atualização do overview

Marcar somente o item 015-08 após aceite de 015-07. Item marcado após build,
instalação, screenshots e validação manual offline.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
