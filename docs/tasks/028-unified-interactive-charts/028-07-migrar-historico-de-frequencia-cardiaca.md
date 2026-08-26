# 028-07 — Migrar histórico de frequência cardíaca

- **Ticker:** `028`
- **Número:** `07`
- **Status:** `completed`
- **Depends on:** `028-06`

## Objetivo

Migrar o histórico consolidado de frequência cardíaca para o sistema compartilhado de gráficos, com eixos reais e interação padronizada.

## Escopo

- adaptar as amostras consolidadas já disponíveis na tela de saúde para contratos Champion de série temporal;
- substituir o `HealthChart` manual onde ele for o renderer vigente do histórico de BPM;
- exibir eixo temporal coerente com os dados reais;
- exibir eixo Y em `bpm`;
- permitir tap/drag com marker e tooltip contendo horário/período e BPM do ponto selecionado;
- preservar manual + D1 + amostras reais de atividades conforme o read model atual;
- preservar gaps e ausência de amostras.

## Guard rails

- não reamostrar BPM;
- não suavizar nem interpolar pontos;
- não criar timeline 1 Hz;
- não mudar o agrupamento ou consolidação já feito pelo read model;
- scroll/zoom somente se a densidade real da série justificar;
- remover `HealthChart` apenas quando não houver mais consumidor que dependa dele.

## Critérios de aceite

1. Histórico de BPM usa o renderer compartilhado.
2. Eixo Y apresenta BPM real, não escala normalizada sem labels.
3. Eixo X representa os tempos/buckets reais disponíveis.
4. Marker/tooltip seleciona pontos de forma previsível.
5. Gaps continuam ausências.
6. Acessibilidade resume período, quantidade de dados e lacunas relevantes.
7. A feature não importa Vico diretamente.

## Validação física

Validar:

- série curta e série mais densa quando disponíveis;
- toque e drag;
- rolagem da tela durante interação;
- tema claro/escuro;
- fonte ampliada;
- estado vazio.

Registrar screenshot sanitizado sem expor dados pessoais além do necessário à evidência.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`;
- `docs/integration/028-07-heart-rate-history-final.png`;
- este arquivo;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`.

## Decisões e desvios

- `HeartRateHistoryChart` passou a adaptar `HeartRateChartSeries` para
  `ChampionCartesianChartData` e renderizar com `ChampionTimeSeriesChart`.
- O eixo Y usa intervalo BPM válido (`30..220`) e unidade explícita `bpm`; o
  tooltip compartilhado usa horário/data do ponto e BPM real.
- Pontos intradia usam segundos decorridos desde primeira amostra como
  coordenada X, preservando espaçamento temporal real; `xLabel` e formatter
  continuam derivados dos timestamps reais, sem alterar a série consolidada
  nem reamostrar dados. Amostras com timestamp empatado usam séries visuais
  paralelas sem deslocar o tempo original.
- Pontos diários ausentes são materializados somente no adapter como
  `y = null`, preservando gaps sem alterar read model, agrupamento ou
  persistência.
- Scroll permanece opt-in e só é habilitado acima de 12 buckets visuais;
  zoom continua desabilitado.
- O arquivo `HealthChart` não foi removido: não era renderer vigente do
  histórico BPM e ainda possui consumidores técnicos em previews e testes.

## Comandos executados

- `./gradlew :app:compileDebugKotlin` — passou.
- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou (`BUILD SUCCESSFUL`).
- `./gradlew installDebug` — passou no `SM-S901E`.
- Não foram criados nem executados testes automatizados.

## Validação física e evidências

- `SM-S901E`: série intradia densa com 70 pontos e eixo em horários reais —
  passou.
- `SM-S901E`: série curta de 7 dias, com buckets presentes e gaps visíveis —
  passou.
- `SM-S901E`: série mais densa de 30 dias, com buckets e gaps preservados —
  passou.
- `SM-S901E`: scroll horizontal da série de 30 dias até buckets com dados —
  passou.
- `SM-S901E`: rolagem vertical da tela durante consulta do gráfico — passou.
- `SM-S901E`: tema escuro observado; marcador/tooltip foram configurados pelo
  renderer compartilhado, mas não houve captura física reproduzível do tooltip
  durante o gesto nesta sessão.
- Tema claro, fonte ampliada e estado vazio não foram exercitados nesta sessão.
- Screenshot sanitizado: `docs/integration/028-07-heart-rate-history-final.png`.

## Riscos residuais

- Tap/drag e tooltip permanecem sem evidência física reproduzível nesta sessão;
  dependem do comportamento compartilhado já implementado em `028-04`.
- Tema claro, fonte ampliada e estado vazio ainda exigem validação física
  dedicada.
- O marker/tooltip e cenários de lacuna ainda dependem de validação física mais
  ampla; a coordenada intradia agora preserva espaçamento temporal real.
