# 028-09 — Adicionar gráfico de cadência D4 ao detalhe do treino

- **Ticker:** `028`
- **Número:** `09`
- **Status:** `completed`
- **Depends on:** `028-08`

## Objetivo

Adicionar ao detalhe do treino um gráfico de cadência baseado exclusivamente nos intervalos D4 reais já derivados pela Spec 027.

## Escopo

- derivar a série de apresentação a partir de `CanonicalWorkoutSeries.intervals` elegíveis;
- usar `cadenceStepsPerMinute` já existente por intervalo;
- representar cada intervalo no tempo usando sua janela efetiva real ou um ponto temporal determinístico coerente com o contrato visual;
- exibir eixo Y em `spm`;
- exibir eixo X temporal relativo ao treino;
- permitir tap/drag com marker e tooltip;
- tooltip deve poder mostrar, quando disponível no mesmo intervalo:
  - período do intervalo;
  - cadência em `spm`;
  - passos medidos do intervalo;
- manter estado vazio quando a atividade não possuir cadência elegível.

## Semântica obrigatória

A visualização não cria timeline 1 Hz.

```text
D4 intervalos reais
      ↓
cadência derivada por intervalo
      ↓
Champion chart
```

Não interpolar, suavizar ou gerar amostras intermediárias para tornar a linha visualmente mais contínua.

## Guard rails

- `CYCLING` continua sem cadência de passos derivada por D4 quando não elegível;
- não alterar a projeção `StepsCadenceRecord` do Health Connect;
- não alterar fórmulas da Spec 027 nesta task;
- não criar novo modelo persistente;
- não reabrir Gate B/timeline 1 Hz removido da Spec 027.

## Critérios de aceite

1. Treinos elegíveis exibem cadência D4 no detalhe.
2. O gráfico utiliza a infraestrutura comum e não Canvas local.
3. Eixo Y usa `spm` real.
4. Marker/tooltip referencia o intervalo real selecionado.
5. Não há pontos sintéticos entre intervalos.
6. Atividades inelegíveis apresentam ausência segura, sem valor inventado.
7. Nenhuma mudança ocorre no fluxo de sync Health Connect.

## Validação física

Usar atividade real com D4 válido e confirmar:

- quantidade/ordem dos pontos coerente com os intervalos;
- valor de cadência selecionado coerente com o detalhamento do intervalo;
- toque/drag;
- rolagem da tela;
- tema claro/escuro;
- ausência correta em atividade inelegível quando disponível.

Registrar screenshot sanitizado.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
- `docs/integration/028-09-workout-cadence-final.png`;
- este arquivo;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`.

## Decisões e desvios

- O gráfico usa somente `activity.derivationSnapshot.canonicalSeries` elegível e
  cria exatamente um ponto por `DerivedWorkoutInterval`.
- Cada ponto usa o ponto médio da janela efetiva real, com tempo relativo ao
  início do primeiro intervalo. Não há interpolação, suavização, reamostragem ou
  timeline de 1 Hz.
- O eixo Y usa `cadenceStepsPerMinute` e unidade `spm`. Zero permanece valor
  válido quando presente nos fatos D4.
- O tooltip compartilhado recebe período real e passos medidos em `xLabel`; o
  valor Y fornece a cadência formatada em `spm`. A descrição acessível também
  expõe período, cadência e passos por intervalo.
- O eixo X usa duração relativa formatada como `mm:ss`/`hh:mm:ss`; horários
  absolutos permanecem restritos ao período do intervalo no tooltip e na
  acessibilidade.
- `CYCLING`, série ausente, snapshot inelegível e lista sem intervalos mantêm
  ausência segura. Nenhum modelo persistente, fórmula D4 ou fluxo Health Connect
  foi alterado.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- `./gradlew lint assembleDebug assembleRelease installDebug` — passou (`BUILD SUCCESSFUL`).
- `./gradlew installDebug` — passou no `SM-S901E`.
- Não foram criados nem executados testes automatizados.

## Validação física e evidências

- `SM-S901E` / build `.dev`: detalhe de atividade real D4 com 6 intervalos,
  duração `05:30` e 639 passos exibiu o gráfico — passou.
- `SM-S901E` / build `.dev`: quantidade e ordem dos pontos foram coerentes com
  os 6 intervalos. A acessibilidade expôs cadências `116,7`, `125,5`, `124,4`,
  `133,1`, `130,9` e `66,5 spm`, com passos `107`, `115`, `114`, `122`, `120` e
  `61` — passou.
- `SM-S901E` / build `.dev`: segunda fixture real D4, corrida com 33 intervalos
  e 4.605 passos, confirmou a série longa, o scroll horizontal e os labels
  relativos `00:00`–`04:53` — passou.
- `SM-S901E` / build `.dev`: eixo Y exibiu `spm`, eixo X exibiu duração relativa
  (`00:00`, `00:58`, `01:57`, `02:56`, `03:54`, `04:53`), tema escuro e rolagem
  vertical foram observados — passou.
- Screenshot sanitizado: `docs/integration/028-09-workout-cadence-final.png`.
- Tap/drag foi tentado via ADB no gráfico; não houve screenshot reproduzível do
  marker/tooltip durante esta sessão. O caminho permanece coberto pelo renderer
  compartilhado e pelas descrições acessíveis, mas requer revalidação manual com
  toque físico.

## Revisão independente

- Revisão independente encontrou eixo X absoluto na primeira versão. Correção
  aplicada para formatar duração relativa; nova build e validação física
  confirmaram labels relativos.

## Riscos residuais

- Marker/tooltip do gráfico de cadência não teve evidência física reproduzível
  nesta sessão; validar com toque/drag físico em revisão independente.
- A validação física pós-correção confirmou `mm:ss` no eixo X; marker/tooltip
  continua dependente de revalidação manual com toque físico.
- Estado vazio de atividade inelegível foi preservado por inspeção de código;
  não havia fixture real inelegível disponível no dispositivo.
