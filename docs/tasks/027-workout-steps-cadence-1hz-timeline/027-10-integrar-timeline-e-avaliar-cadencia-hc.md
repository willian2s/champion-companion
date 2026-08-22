# 027-10 — Integrar timeline e avaliar cadência 1 Hz no Health Connect

## Objetivo

Usar a timeline comum de BPM/cadência nas superfícies locais adequadas e decidir se a projeção de cadência do Health Connect deve permanecer por bucket D4 ou migrar para série 1 Hz derivada.

## UI/local

Integrar somente onde acrescentar valor real, prioritariamente detalhe da atividade/gráficos.

A visualização deve:

- usar eixo temporal comum;
- preservar gaps;
- não mostrar ponto ausente como zero;
- não alterar silenciosamente métricas históricas já exibidas;
- manter desempenho adequado para treinos longos.

Se houver mudança visual relevante, capturar screenshot conforme `AGENTS.md`.

## Comparação Health Connect

Usar a evidência de `027-06` para comparar:

### Opção A — cadência por bucket

- menos pontos;
- máxima fidelidade ao dado derivado diretamente do D4;
- gráfico externo potencialmente mais grosseiro.

### Opção B — cadência derivada 1 Hz

- gráfico temporal mais contínuo;
- muito mais samples;
- valores interpolados, não observados;
- pode melhorar ou não a interpretação pelo Health Sync/Strava.

## Decisão

Somente promover 1 Hz para `StepsCadenceRecord` se:

- Health Connect aceitar a série com comportamento estável;
- Health Sync/Strava demonstrar benefício concreto ou houver outro consumidor real;
- não houver regressão de performance/tamanho/retry;
- a proveniência derivada permanecer clara no app;
- a mesma identidade/versionamento remoto puder ser atualizada com segurança.

Caso contrário, manter HC por buckets e usar 1 Hz apenas localmente.

## BPM no Health Connect

Esta task não deve substituir automaticamente as amostras de BPM da atividade por uma série interpolada. Qualquer mudança desse tipo exige decisão explícita separada.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Mudança visual exige evidência visual; não criar nem executar testes automatizados sem pedido explícito.
