# Task 011-10 - Implementar detalhe, revisão, métricas, gráfico e intervalos

## Status

Concluída em 2026-07-24. Correção visual solicitada concluída em 2026-07-28.

## Objetivo

Criar a tela de detalhe completa da atividade com cabeçalho, métricas, gráfico de BPM, intervalos, formulário de revisão, formulário de calibração e estados somente leitura.

## Contexto

Após `011-09`, a navegação para detalhe já existe, mas falta renderizar conteúdo real da atividade e permitir o fluxo local de revisão/calibração.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-08-extend-exercises-viewmodel-for-filters-review-and-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-09-implement-exercises-filters-list-and-detail-navigation.md`

## Dependências

- `011-09`.

## Entradas

- estado e intents finais do ViewModel;
- rota de detalhe já criada.

## Arquivos exatos permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-10-implement-activity-detail-review-metrics-chart-and-intervals.md`

Normalização de planejamento em 2026-07-28: `HeartRateHistoryChart.kt` foi incluído porque o gráfico de detalhe entregue em `011-10` reutiliza seu padrão visual e ambos apresentavam o mesmo desalinhamento entre limites horizontais do plot e legendas. A correção solicitada permanece restrita às legendas dos dois gráficos, sem alterar dados, escala ou contratos.

## Arquivos proibidos

- `feature/home/**`
- `core/database/**`
- `data/**`
- `core/navigation/**`
- `app/src/test/**`
- `app/src/androidTest/**`

## Escopo

- renderizar detalhe completo;
- renderizar gráfico com amostras reais de BPM;
- renderizar intervalos;
- renderizar revisão quando `PENDING_REVIEW` e `VALID`;
- renderizar calibração quando `PENDING_CALIBRATION`;
- renderizar somente leitura em `READY_TO_SYNC` e `DISCARDED`;
- renderizar aviso seguro em `PARTIAL`.

## Fora do escopo

- banner da Home;
- ajustes finais de validação cruzada e confirmações de descarte;
- qualquer sync real.

## Passos

1. Construir cabeçalho e resumo de métricas.
2. Reutilizar padrão visual do gráfico de BPM da área Saúde quando houver compatibilidade real.
3. Renderizar intervalos com associação segura de BPM ou degradação segura.
4. Conectar formulários de revisão e calibração ao ViewModel.
5. Atualizar spec e overview.

## Critérios de aceite

- detalhe mostra título, status, validade, tipo, data, origem e métricas principais;
- gráfico usa amostras reais;
- intervalos não inventam BPM;
- `PARTIAL` mostra bloqueio explícito;
- `READY_TO_SYNC` mostra `Pronta para sincronizar` sem botão funcional de envio.

## Validação manual

- revisar acessibilidade textual do gráfico;
- revisar estados `Loading`, `Ready`, `NotFound` e `Failed`.

## Comandos de build aplicáveis

- `./gradlew lint assembleDebug assembleRelease`

## Evidências

- diff da tela de detalhe;
- screenshots da lista e do detalhe durante implementação/PR.

## Correção visual de 2026-07-28

### Causa do escalonamento

- `Canvas` dos dois gráficos reservava `12 dp` à esquerda e `40 dp` à direita para o plot, enquanto as `Row` das legendas ocupavam a largura total.
- Dados, amostras, timestamps e escalas estavam corretos; defeito ficava restrito ao alinhamento visual.
- Correção atravessava `ExercisesScreen.kt` e o padrão reutilizado em Saúde, arquivo ainda ausente da lista exata permitida. Planejamento foi normalizado antes da edição de código.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`
- `docs/specs/011-activity-review-calibration.md`
- `docs/tasks/011-activity-review-calibration/011-00-overview.md`
- `docs/tasks/011-activity-review-calibration/011-10-implement-activity-detail-review-metrics-chart-and-intervals.md`

### Decisões e desvios

- Paddings horizontais foram promovidos a constantes locais em cada gráfico e compartilhados pelo `Canvas` e pela `Row` correspondente; `absolutePadding` preserva os mesmos limites físicos esquerdo/direito do desenho, evitando nova divergência entre plot e legenda.
- Histórico vazio de Saúde usa o mesmo `XAxisLabels` e recebeu o alinhamento corrigido sem caminho especial.
- Nenhum dado, cálculo de eixo, escala, contrato, estado ou fluxo foi alterado.
- Único desvio da lista original: inclusão planejada de `HeartRateHistoryChart.kt`, necessária para corrigir o mesmo defeito visual nos dois gráficos citados pelo usuário.

### Comandos e resultados

- `git diff --check`: sucesso.
- `./gradlew lint assembleDebug assembleRelease`: sucesso em 2026-07-28; validação final com `101` tasks acionáveis, `24` executadas e `77` atualizadas.
- Testes automatizados: não criados, não alterados e não executados, conforme `AGENTS.md` e spec.

### Evidências

- Legendas `0`/`23` do histórico usam os mesmos limites horizontais do plot.
- Labels de início/fim do treino usam os mesmos limites horizontais do plot.
- Diff restrito a constantes/padding de UI e documentação SDD autorizada.

### Riscos residuais

- Validação visual manual e screenshot em dispositivo não foram executadas nesta sessão; permanecem necessárias para evidência visual em eventual PR.
- `ExercisesScreen.kt` continua hotspot visual conhecido; extração estrutural permanece fora desta correção.

## Screenshot quando visual

- obrigatória.

## Atualização do overview

- marcar `011-10` como concluída;
- registrar novos riscos visuais, de acessibilidade ou de associação intervalo/BPM.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
