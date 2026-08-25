# 028-09 — Adicionar gráfico de cadência D4 ao detalhe do treino

- **Ticker:** `028`
- **Número:** `09`
- **Status:** `pending`
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