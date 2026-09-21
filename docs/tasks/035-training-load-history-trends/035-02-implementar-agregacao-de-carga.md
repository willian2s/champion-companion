# 035-02 — Implementar agregação de carga

- **Ticker:** `035`
- **Número:** `02`
- **Status:** `pending`

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
