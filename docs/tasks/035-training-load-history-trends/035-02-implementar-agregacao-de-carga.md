# 035-02 — Implementar agregação de carga

- **Ticker:** `035`
- **Número:** `02`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Implementar domínio puro para agregar resultados da policy 034 em current7,
previous7, current28 e previous28, produzindo resumos, comparações, buckets
diários, grupos por tipo e estados seguros.

## Requisitos cobertos

- R2 a R7 da Spec 035.
- R10 quanto à ausência de persistência e efeitos colaterais.
- Fechamento de carga, cobertura, contagens, buckets, tipos e deltas.

## Escopo incluído

- Contratos equivalentes a `CardiovascularLoadHistory*`.
- `CardiovascularLoadHistoryPolicy` recebendo workouts, FC máxima, `asOf` e
  `ZoneId`.
- Reuso direto de `WorkoutHeartRateAnalysisPolicy` por workout.
- Estados `AVAILABLE`, `CONFIGURATION_REQUIRED`, `NO_WORKOUTS`,
  `NO_ANALYZABLE_WORKOUTS` e `UNAVAILABLE`.
- Comparação com `NO_PREVIOUS_BASELINE` quando baseline é zero.
- Buckets diários com zero legítimo para dia sem workout e contadores para dia
  com workout sem análise.
- Agrupamento por `confirmedType ?: suggestedType ?: UNKNOWN`.

## Escopo excluído

- Room, DAO, repository, ViewModel, Compose e Health Connect.
- Nova fórmula, classificação de zonas, sample-and-hold ou cobertura.
- Persistência de qualquer resultado.
- Alteração de `WorkoutActivity` ou workflow.
- Testes automatizados novos nesta entrega.

## Dependências

- `035-01` concluída.
- Policy e contratos da Spec 034.
- Sem dependência de migration ou schema.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutHeartRateAnalysis.kt` ou novo arquivo de histórico;
- `core/model/CalendarHistory.kt` para tipos temporais existentes;
- `WorkoutHeartRateAnalysisPolicy`;
- `WorkoutActivity`, `WorkoutType`, `ActivityWorkflowState`.

## Passos de implementação

1. Modelar ranges semiabertos derivados de `asOf`.
2. Validar elegibilidade `WATCH_D3_D4`, não descartado e `startedAt` na janela.
3. Construir janela canônica de cada atividade e chamar a policy 034.
4. Separar análise disponível, configuração requerida, sem samples e erro sem
   criar carga sintética.
5. Agregar duração total, cobertura, contagens, carga e `partial`.
6. Gerar comparações e bloquear divisão quando baseline for zero.
7. Gerar datas/buckets usando `ZoneId` explícito e fechar current28.
8. Agrupar por tipo e fechar current28.
9. Manter policy determinística, sem `Instant.now()` e sem escrita.

## Testes e comandos de validação

- Validar por cálculo independente os quatro ranges, deltas e fechamentos.
- Verificar que baseline zero não retorna infinito/`NaN`.
- Verificar que workout sem samples não aumenta carga.
- Verificar soma de buckets e tipos.
- Executar futuramente `./gradlew lint assembleDebug assembleRelease` quando
  implementação estiver integrada.
- Não criar testes automatizados novos.

## Definição de pronto

- Resultado tipado distingue todos os estados exigidos.
- Carga só soma `cardiovascularLoad` de `AVAILABLE`.
- Cobertura usa duração de todos os workouts elegíveis no denominador.
- current7/previous7/current28/previous28, buckets e tipos fecham.
- Alteração de FC máxima muda apenas resultado retornado em memória.
- Nenhuma dependência de infraestrutura ou relógio global.

## Riscos e cuidados

- Não contar `CONFIGURATION_REQUIRED` como workout analisado.
- Não usar `0` para mascarar ausência na camada de estado/UI.
- Não dividir workout por duração ou cruzamento; usar somente `startedAt`.
- Tratar datas locais e DST com `ZoneId` recebido, sem timezone persistido.
- Preservar precisão `Double`/`Duration` até a apresentação.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WorkoutHeartRateLoadHistory.kt` — contratos, janelas, agregação, comparações, buckets e grupos por tipo.
- `docs/tasks/035-training-load-history-trends/035-02-implementar-agregacao-de-carga.md` — status, decisões e evidências desta execução.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist e progresso da Spec 035.

## Decisões e desvios

- Decisão: manter toda a agregação em `core.model`, com `asOf` e `ZoneId` explícitos e sem relógio global, persistência ou efeitos colaterais.
- Decisão: reutilizar diretamente `WorkoutHeartRateAnalysisPolicy` uma vez por workout elegível; resultados `Unavailable` e carga disponível nula permanecem estados seguros.
- Decisão: expor status por resumo, resultado histórico opcional, comparações com `NO_PREVIOUS_BASELINE`, buckets civis preenchidos e grupos somente dos tipos presentes.
- Desvio: nenhum.

## Comandos executados

- `git diff --check`
- `./gradlew app:compileDebugKotlin`
- `./gradlew lint assembleDebug assembleRelease`

## Resultados e evidências

- `git diff --check`: passou.
- `app:compileDebugKotlin`: `BUILD SUCCESSFUL`.
- `lint`, `assembleDebug` e `assembleRelease`: `BUILD SUCCESSFUL`.
- Revisão independente: aprovada, sem blocker, major ou minor.
- Não foram criados nem executados testes automatizados, conforme escopo da Spec 035 e `AGENTS.md`.

## Riscos residuais

- Fechamentos numéricos com banco/workouts reais, recomposição por FC máxima, ausência de persistência e screenshot permanecem validação das subtarefas posteriores.
- Não há testes automatizados novos para cobrir combinações sintéticas de ranges, DST, baseline zero e workouts sem samples; a validação documental e de build não substitui a prova física posterior.
