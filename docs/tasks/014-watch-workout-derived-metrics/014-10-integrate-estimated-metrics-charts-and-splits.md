# 014-10 — Integrar métricas, gráficos e splits à UI

- **Ticker:** `014`
- **Número:** `10`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Substituir consumidores visuais de atividades `WATCH_D3_D4` pela série canônica,
exibindo proveniência, plausibilidade, gráficos e splits com estados ausentes.

Resultado: detalhe, resumo e calendário usam a mesma distância canônica e nunca
recorrem à distância original, calibrada histórica ou estimativa agregada.

## Dependências

- `014-09` concluída.

## Arquivos permitidos

- `feature/exercises/ExercisesScreen.kt` e `ExercisesViewModel.kt`;
- read models/mappers de exercícios e calendário;
- componentes visuais existentes necessários;
- recursos de texto;
- documentação/screenshot e overview.

## Arquivos proibidos

- parser, perfil, schema/migration e Health Connect writer;
- Gradle e manifesto;
- entradas ou telas phone descontinuadas.

## Escopo

- distância, ritmo, velocidade, acumulado, gráficos, splits e BPM/km;
- rótulos **Distância estimada** e **Ritmo estimado** quando aplicáveis;
- origem `stepLengthSource` e versão da derivação em contexto apropriado;
- estados sem comprimento do passo, série inelegível ou plausibilidade bloqueada;
- orientação para configurar perfil ou reprocessar atividade;
- workflow `TREADMILL` de relógio sem exigir calibração histórica;
- acessibilidade e screenshots.

A distância original/calibrada pode aparecer somente em diagnóstico claramente
separado do valor principal.

## Fora do escopo

- mapa, rota ou GPS;
- edição da fórmula;
- reprocessamento operacional, tratado em `014-11`;
- sincronização Health Connect;
- refatoração visual não relacionada;
- testes automatizados.

## Passos de implementação

1. Adaptar read models para a série canônica.
2. Remover fallbacks visuais para original, calibrada e estimativa agregada.
3. Exibir ausência com motivo e ação apropriada.
4. Integrar acumulado e métricas temporais sem suavização.
5. Exibir splits e BPM temporal com formatos locais.
6. Exibir plausibilidade sem alterar fatos.
7. Ajustar a apresentação/workflow de `TREADMILL` para origem relógio.
8. Revisar resumo, detalhe, calendário e acessibilidade.
9. Registrar screenshots e atualizar overview.

## Critérios de aceite

- Todas as superfícies usam a mesma distância canônica.
- O total exibido termina exatamente no acumulado da série.
- Original, calibrada histórica e estimativa agregada não aparecem como fallback.
- Ausência não vira zero, salvo intervalo real com zero passos.
- Splits/BPM correspondem ao read model de `014-09`.
- Série implausível é claramente identificada e não apresentada como confiável.
- `TREADMILL` de relógio não pede calibração incompatível.
- Conteúdo possui descrições acessíveis e screenshots.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots dos estados principais;
- matriz superfície → fonte canônica;
- casos sem perfil, inelegível, implausível, zero e esteira;
- builds e roteiro manual.

## Definição de pronto

Consumidores visuais estão alinhados à série canônica, screenshots registradas e
item `014-10` marcado.

## Rollback

Ocultar superfícies novas e retornar ausência para métrica canônica. Nunca
restaurar distância original ou calibrada como fallback funcional.

## Implementação

- Exercícios usa `canonicalSeries` persistida para distância acumulada, passos,
  ritmo, velocidade, cadência e intervalos; `snapshot.splits` fornece splits e
  BPM ponderado sem recálculo na UI.
- Estados `MISSING_STEP_LENGTH`, `NOT_MATERIALIZED`, `SERIES_INELIGIBLE`,
  `PLAUSIBILITY_BLOCKED`, `SPLITS_NOT_MATERIALIZED` e `AVAILABLE` orientam
  ausência, ação e confiabilidade.
- Calendário recebe distância canônica e metadados do snapshot ativo para
  `WATCH_D3_D4`; atividades phone preservam contrato legado.
- Fluxos de atividades observam snapshots ativos, atualizando a UI após
  materialização ou substituição explícita da derivação.
- `TREADMILL` de relógio exibe orientação de derivação e não oferece calibração
  histórica como requisito funcional. Calibração existente permanece diagnóstico.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `docs/tasks/014-watch-workout-derived-metrics/screenshots/`.

## Decisões e desvios

- Distância principal de `WATCH_D3_D4` usa somente o último acumulado da série
  canônica, que equivale à soma persistida dos intervalos. Distância original,
  calibrada e estimativa agregada não são fallback.
- Metadados escalares do snapshot ativo bastam para o resumo do calendário;
  splits completos permanecem disponíveis no detalhe, pelo read model
  persistido da derivação.
- Textos permanecem nos componentes Compose existentes; não foi criada nova
  camada de recursos para evitar refatoração visual fora do escopo.
- O smoke físico não possuía atividade canônica do relógio para abrir detalhe,
  splits ou estados de plausibilidade. Esses estados foram cobertos por
  caminhos tipados, revisão independente e validação de compilação/lint; a
  confirmação visual com dados reais permanece risco para `014-15`.

## Comandos executados

- `./gradlew app:compileDebugKotlin` — PASS;
- `./gradlew app:lintDebug` — PASS;
- `./gradlew lint assembleDebug assembleRelease` — PASS;
- `./gradlew installDebug` — PASS; APK instalado no Galaxy S22 `SM-S901E`;
- `git diff --check` — PASS;
- `git status --short` — PASS; somente arquivos da implementação e screenshots;
- testes automatizados — não criados nem executados, conforme escopo.

## Evidências e riscos residuais

- Matriz de fonte: card/lista Exercícios, detalhe e calendário → distância
  canônica persistida; detalhe → série temporal persistida; splits/BPM/km →
  `snapshot.splits`; proveniência → `stepLengthSource` e
  `derivationVersion` do snapshot ativo.
- Screenshots smoke: `screenshots/014-10-installed-smoke.png`,
  `screenshots/014-10-exercises-screen.png` e
  `screenshots/014-10-calendar-smoke.png`. `014-10-exercises-smoke.png` também
  registra navegação inicial; não há atividade canônica disponível no aparelho.
- Revisão independente: aprovada com ressalvas, sem blocker funcional.
- Risco residual: alteração do perfil pode manter a distinção dinâmica de
  comprimento do passo até nova emissão/navegação da atividade; não há Flow de
  perfil nesta subtarefa.
- Risco residual: screenshots não validam visualmente detalhe canônico real,
  splits/BPM, série implausível, zero passos ou esteira; validação física fica
  para `014-15`.
