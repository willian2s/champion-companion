# 027-09 — Implementar cadência derivada em 1 Hz

- **Ticker:** `027`
- **Número:** `09`
- **Status:** `completed`
- **Depends on:** `027-07`, `027-08`

## Objetivo

Gerar série temporal de cadência na mesma grade de 1 segundo usada pelo BPM.

## Entrada

Usar intervalos D4 válidos e duração normalizada da atividade.

Cadência base por intervalo:

```text
cadenceSpm = intervalSteps / intervalDurationSeconds * 60
```

## Pipeline

```text
intervalos D4
  -> cadência base por intervalo
  -> validação/outlier conforme 027-07
  -> suavização temporal conforme contrato
  -> interpolação entre regiões contínuas elegíveis
  -> RESAMPLED_1HZ
```

## Regras

- não inventar cadência onde bucket de passos estiver ausente;
- não atravessar gaps estruturais;
- não usar total de passos da atividade para preencher intervalos faltantes;
- não derivar cadência para tipos sem semântica de passos;
- usar a mesma origem temporal/segundos relativos da série BPM;
- manter versão do algoritmo identificável.

## Grade comum

Ao final:

```text
segundo | BPM? | cadenceSpm?
0       | 138  | 164
1       | 138  | 165
2       | 139  | 165
...
```

Cada coluna pode estar ausente independentemente quando a fonte não suportar aquele segundo/região.

## Health Connect

Não alterar automaticamente a projeção `StepsCadenceRecord` nesta task. A decisão de promover bucket -> 1 Hz pertence a `027-10` após comparação com a evidência física do Gate A.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- este arquivo de subtarefa;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.

## Decisões e desvios

- cadência é calculada sob demanda sobre `temporalResolution` do snapshot
  imutável, sem persistência, UI ou Health Connect;
- somente atividades `WATCH_D3_D4` com parser `d4-v3`, validade `VALID` e tipo
  com semântica de passos são elegíveis; `CYCLING` permanece excluído;
- cada intervalo usa `decodedSteps / duração normalizada * 60`, com midpoint,
  Hampel, EMA temporal de 5 s, continuidade por índice/adjacência e fallback
  temporal limitado a 90 s;
- intervalos sem passos decodificados encerram segmento e preservam buckets
  `ABSENT`, sem preencher com total da atividade; passos e cadência zero são
  preservados como valores válidos;
- não houve desvio funcional do contrato ADR-026. A grade permanece
  `[startedAt, endedAt)`, com bucket final parcial, ausência sem extrapolação,
  provenance e identidade/fingerprint versionados.

## Comandos executados e evidências

- `./gradlew app:compileDebugKotlin` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- `git diff --check` — passou, sem erros de whitespace;
- revisão independente — aprovada, sem bloqueadores;
- nenhum teste automatizado foi criado ou executado, conforme regra explícita.

## Riscos residuais e bloqueios

- integração da timeline à UI/gráficos e decisão sobre promoção de cadência 1 Hz
  ao Health Connect permanecem em `027-10`;
- validação física e fechamento da Spec permanecem em `027-11`;
- comportamento interoperável da cadência 1 Hz ainda depende de evidência física.

Próxima subtarefa elegível: `027-10`.
