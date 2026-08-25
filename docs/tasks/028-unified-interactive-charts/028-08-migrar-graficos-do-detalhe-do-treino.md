# 028-08 — Migrar gráficos do detalhe do treino

- **Ticker:** `028`
- **Número:** `08`
- **Status:** `pending`
- **Depends on:** `028-07`

## Objetivo

Migrar os gráficos atuais do detalhe do treino para a infraestrutura compartilhada, removendo o desenho local de frequência cardíaca e distância acumulada sem alterar a série canônica nem as amostras reais do treino.

## Escopo

Migrar:

- frequência cardíaca do treino;
- distância canônica acumulada por intervalo.

Para frequência cardíaca:

- usar apenas `WorkoutHeartRateSample` já persistido/disponível;
- eixo X temporal real;
- eixo Y em `bpm`;
- marker/tooltip com instante e BPM;
- preservar guidelines de BPM apenas se puderem ser representadas pela infraestrutura comum sem manter engine local paralela.

Para distância canônica:

- usar `CanonicalWorkoutSeries.intervals` vigente;
- eixo X baseado nas janelas efetivas reais;
- eixo Y em distância acumulada real;
- marker/tooltip por intervalo/ponto;
- preservar aviso de diagnóstico quando a série estiver em estado de plausibilidade bloqueada.

## Guard rails

- não reamostrar BPM;
- não interpolar distância entre intervalos além do que o renderer naturalmente conecta entre pontos existentes;
- não alterar derivação D4;
- não tocar revisão, calibração, workflow ou Health Connect;
- scroll/zoom só quando necessário para série longa;
- não adicionar cadência nesta task; isso pertence à `028-09`.

## Critérios de aceite

1. `WorkoutHeartRateChart` deixa de manter engine Canvas própria.
2. `WorkoutCanonicalDistanceChart` deixa de manter engine Canvas própria.
3. Ambos usam a mesma infraestrutura compartilhada.
4. BPM e distância exibem valores/unidades reais nos eixos e tooltip.
5. Estados indisponíveis, parciais e de diagnóstico continuam coerentes.
6. A interação não prejudica a rolagem vertical do detalhe.
7. A feature de exercícios não importa Vico diretamente.

## Validação física

Validar em treino real importado:

- BPM com toque/drag;
- distância acumulada com toque/drag;
- treino com número representativo de intervalos;
- rolagem vertical da tela;
- tema claro/escuro;
- fonte ampliada;
- estados de série indisponível quando houver fixture real disponível.

Registrar screenshots sanitizados dos dois gráficos.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.