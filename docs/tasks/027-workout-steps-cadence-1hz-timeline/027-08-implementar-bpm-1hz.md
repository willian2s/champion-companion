# 027-08 — Implementar BPM derivado em 1 Hz

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

Não criar nem executar testes automatizados sem pedido explícito.
