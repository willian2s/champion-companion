# 027-08 — Implementar BPM derivado em 1 Hz

- **Ticker:** `027`
- **Número:** `08`
- **Status:** `completed`
- **Depends on:** `027-07`

## Objetivo

Gerar uma série de BPM suavizada e reamostrada em grade de 1 segundo sem reescrever as amostras D4 de origem.

## Entrada

Usar somente amostras de BPM da atividade consideradas válidas pela camada de domínio/parser atual.

## Pipeline inicial

```text
SOURCE D4
  -> validação de faixa
  -> filtro robusto de outlier conforme contrato 027-07
  -> EMA sensível ao tempo
  -> interpolação entre pontos elegíveis
  -> RESAMPLED_1HZ
```

## EMA temporal

Usar forma baseada em intervalo real, equivalente a:

```text
alpha = 1 - exp(-deltaT / tau)
smoothed = alpha * current + (1 - alpha) * previous
```

`tau` e demais parâmetros devem vir do contrato/versionamento de `027-07`.

Não tratar amostras interpoladas como novas observações na etapa de suavização.

## Interpolação

- interpolar somente entre observações pertencentes à mesma região contínua elegível;
- não atravessar gaps estruturais;
- respeitar início/fim da sessão;
- preservar ausência quando não houver suporte suficiente;
- manter cálculo determinístico.

## Saída

Cada ponto derivado deve permitir conhecer:

- offset/segundo relativo da sessão ou `Instant` determinístico;
- BPM derivado;
- versão do algoritmo;
- natureza derivada, sem se confundir com medição real.

## Estatísticas

Não substituir silenciosamente média/mínimo/máximo existentes pela série 1 Hz.

Se a UI mostrar valores provenientes da série derivada, o contrato visual deve deixar claro o uso sem alterar métricas históricas autoritativas.

## Fora de escopo

- cadência 1 Hz — `027-09`;
- Health Connect BPM novo;
- ajuste fisiológico sofisticado/Kalman sem evidência concreta.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Validação executada:

```bash
./gradlew app:compileDebugKotlin
```

Resultado: `BUILD SUCCESSFUL`. `git diff --check` também passou. Nenhum teste
automatizado foi criado ou executado.

Evidência: `core/model/Workout1HzTimeline.kt` implementa derivação sob demanda
em memória, identidade/fingerprint versionados, grade `[startedAt, endedAt)`,
Hampel, EMA temporal, continuidade defensiva e reamostragem sem mutar SOURCE.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/Workout1HzTimeline.kt`;
- este arquivo de subtarefa;
- `docs/tasks/027-workout-steps-cadence-1hz-timeline/027-00-overview.md`.

## Decisões e desvios

- cálculo permanece sob demanda e somente em memória, sem tabela, migration,
  UI, Health Connect ou alteração de estatísticas existentes;
- identidade deriva parser e versão da derivação do contexto/snapshot da
  atividade; versões divergentes falham fechadas;
- nenhum desvio do contrato 027-07 foi registrado.

## Comandos executados e evidências

- `./gradlew app:compileDebugKotlin` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- `git diff --check` — passou, sem erros de whitespace;
- revisão independente — aprovada, sem bloqueadores.

## Riscos residuais e bloqueios

- parâmetros e comportamento físico da série ainda precisam de validação futura;
- cadência 1 Hz permanece na subtarefa `027-09`;
- integração Health Connect e validação final permanecem em `027-10` e `027-11`;
- não há testes automatizados, conforme regra explícita do projeto.

Próxima subtarefa elegível: `027-09`.
