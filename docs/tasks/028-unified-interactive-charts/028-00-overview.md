# 028 — Sistema unificado de gráficos interativos

- **Status geral:** `completed`
- **Spec:** [028-unified-interactive-charts.md](../../specs/028-unified-interactive-charts.md)
- **Progresso:** `10/10` subtarefas concluídas

## Objetivo

Substituir motores locais de gráficos desenhados manualmente por uma infraestrutura compartilhada, interativa e consistente, preservando os dados e sua granularidade real.

## Guard rails

- features não importam Vico diretamente;
- novos gráficos de dados passam por `core/designsystem/chart`;
- não criar timeline 1 Hz, suavização ou interpolação para fins visuais;
- gaps continuam ausência de dado;
- não alterar Room, BLE ou Health Connect por necessidade de apresentação;
- não criar nem executar testes automatizados sem pedido explícito;
- scroll/zoom são opt-in por caso de uso;
- mudanças visuais exigem validação física/screenshot quando a task correspondente pedir aceite.

## Checklist

- [x] [`028-01-auditar-graficos-atuais.md`](028-01-auditar-graficos-atuais.md) — auditar todos os gráficos atuais
- [x] [`028-02-adotar-vico-e-registrar-adr.md`](028-02-adotar-vico-e-registrar-adr.md) — adotar Vico e registrar decisão arquitetural
- [x] [`028-03-criar-contratos-e-tema-compartilhados.md`](028-03-criar-contratos-e-tema-compartilhados.md) — criar contratos e tema compartilhados
- [x] [`028-04-implementar-eixos-formatters-marker-interacao.md`](028-04-implementar-eixos-formatters-marker-interacao.md) — implementar eixos, formatters, marker e interação
- [x] [`028-05-migrar-historico-de-passos.md`](028-05-migrar-historico-de-passos.md) — migrar histórico de passos
- [x] [`028-06-migrar-historico-de-distancia.md`](028-06-migrar-historico-de-distancia.md) — migrar histórico de distância
- [x] [`028-07-migrar-historico-de-frequencia-cardiaca.md`](028-07-migrar-historico-de-frequencia-cardiaca.md) — migrar histórico de frequência cardíaca
- [x] [`028-08-migrar-graficos-do-detalhe-do-treino.md`](028-08-migrar-graficos-do-detalhe-do-treino.md) — migrar gráficos do detalhe do treino
- [x] [`028-09-adicionar-grafico-de-cadencia-d4.md`](028-09-adicionar-grafico-de-cadencia-d4.md) — adicionar gráfico de cadência D4 ao detalhe do treino
- [x] [`028-10-remover-legado-validar-e-fechar.md`](028-10-remover-legado-validar-e-fechar.md) — remover legado, validar e fechar

## Dependências

```text
028-01
  ↓
028-02
  ↓
028-03
  ↓
028-04
  ↓
028-05 → 028-06 → 028-07 → 028-08 → 028-09
                                      ↓
                                    028-10
```

As migrações de tela são deliberadamente sequenciais para permitir revisão visual isolada e remoção segura do legado somente após substituição comprovada.

## Estado atual

- `028-01` confirmou cinco implementações manuais relevantes e duplicação de engine Canvas entre superfícies.
- `028-02` adotou Vico `3.2.3` com módulos Compose + Material 3 e registrou ADR-027.
- `028-03` criou a boundary neutra e `028-04` entregou renderer Vico com eixos,
  formatters, marker, tooltip, gaps, acessibilidade e interação opt-in.
- `StepsHistoryChart` foi migrado para `ChampionBarChart`, preservando pontos,
  lacunas, meta, acessibilidade e lista do histórico. Eixo Y mostra valores sem
  unidade redundante; eixo X usa datas compactas e tema compartilhado mantém
  barras/marker coloridos. Períodos 30/90 habilitam scroll horizontal; 7 dias
  permanece sem scroll/zoom.
- GitHub Actions run `32845432177` passou `./gradlew lint assembleDebug assembleRelease` após correção de uma edição acidental do ID do plugin Hilt durante a integração inicial.

## Observações

- Nenhum bloqueio ativo em `028-05`.
- Validação física registrou 7 dias, gaps, tap/drag, tooltip, 30/90 dias, scroll horizontal e rolagem vertical no `SM-S901E`; tema claro e fonte ampliada permanecem riscos residuais documentados na subtarefa.
- `DistanceHistoryChart` foi migrado para `ChampionBarChart`, preservando
  `estimatedDistanceMeters`, gaps, períodos 7/30/90, lista acessível e texto de
  ausência. Validação física registrou 7/30/90 dias, gaps, rolagem vertical,
  eixo em km, temas claro/escuro e fonte ampliada no `SM-S901E`; scroll
  horizontal e tap/drag permanecem riscos residuais documentados na subtarefa.
- `HeartRateHistoryChart` foi migrado para `ChampionTimeSeriesChart`, preservando
  manual, D1, amostras de atividades, buckets diários e gaps. Eixo Y usa BPM
  real; eixo X e tooltip usam horários/datas disponíveis; 7/30 dias preservam
  lacunas como ausência. Validação física observou série intradia densa, 7/30
  dias, scroll horizontal e rolagem vertical no `SM-S901E`. Tema claro, fonte
  ampliada, estado vazio e evidência reproduzível de tooltip permanecem riscos
  documentados na subtarefa. Screenshot:
  `docs/integration/028-07-heart-rate-history-final.png`.
- `WorkoutHeartRateChart` e `WorkoutCanonicalDistanceChart` foram migrados para
  `ChampionTimeSeriesChart`, sem Canvas local ou importação de Vico na feature.
  BPM usa somente amostras reais, tempo efetivo e unidade `bpm`; distância usa
  `CanonicalWorkoutSeries.intervals`, janelas efetivas e quilômetros acumulados.
  Validação física no `SM-S901E` usando build `.dev` confirmou treino real com 33 intervalos,
  série longa com scroll horizontal até ponto final, tema escuro e rolagem vertical. Screenshots:
  `docs/integration/028-08-workout-heart-rate-final.png` e
  `docs/integration/028-08-workout-distance-final.png`.
- O eixo compartilhado reserva padding dos labels extremos completos; rolagem até janela final
  confirmou tempos completos e tooltip BPM/distância foi revalidado no package `.dev`. Tema
  claro e fonte ampliada foram observados; não havia fixture real para estado indisponível/parcial,
  preservado por inspeção em `028-08`. Subtarefa `028-08` concluída.
- `028-09` concluiu `Cadência D4` usando um ponto por intervalo canônico elegível, no ponto
  médio da janela efetiva, com eixo `spm`, período/passos no tooltip compartilhado e descrição
  acessível. Fixture real curta exibiu 6 pontos e a corrida real exibiu 33 intervalos; a série
  longa confirmou eixo relativo com labels `00:00` a `04:53`; screenshot:
  `docs/integration/028-09-workout-cadence-final.png`.
- Revisão independente encontrou inicialmente eixo X absoluto; correção para `mm:ss`/`hh:mm:ss`
  foi aplicada e confirmada em nova build/validação física.
- Tap/drag do marker/tooltip foi tentado via ADB sem evidência reproduzível nesta sessão; fica
  como risco residual para revisão física independente. Estado vazio inelegível foi preservado
  por inspeção, sem fixture real disponível.
- `028-10` removeu `HealthChart`, `HealthChartPoint` e `normalizedChartY` de
  `ChampionDisplayComponents.kt` após retirar os dois blocos de preview e somente
  os testes específicos do legado em `ChampionDisplayComponentsTest.kt`; o teste
  dedicado `HealthChartScaleTest.kt` foi excluído. `HealthCard` e `MetricCard`
  foram preservados.
- Inventário final não encontrou `Canvas`/`nativeCanvas` de visualização de dados
  nas features migradas. Não houve remoção de Canvas decorativo. Vico permanece
  encapsulado em `core/designsystem/chart/ChampionCharts.kt`; dependências Vico
  não foram alteradas.
- Validação técnica de fechamento passou em `git diff --check` e em
  `./gradlew lint assembleDebug assembleRelease`. Não foram criados nem executados
  testes automatizados.
- Smoke integrado foi repetido no `SM-S901E`, build `.dev`, após `installDebug`:
  histórico de passos, histórico de distância e histórico de BPM exibiram os
  gráficos compartilhados com gaps; detalhe de treino exibiu distância canônica,
  BPM e cadência D4; rolagem vertical foi exercitada. Screenshots sanitizados:
  `docs/integration/028-10-final-steps.png`,
  `docs/integration/028-10-final-distance.png`,
  `docs/integration/028-10-final-health.png`,
  `docs/integration/028-10-final-health-scrolled.png`,
  `docs/integration/028-10-final-workout.png` e
  `docs/integration/028-10-final-workout-cadence-hr.png`.
- A interação marker/tooltip não teve captura reproduzível nesta repetição; as
  evidências anteriores de `028-08` permanecem válidas para BPM/distância.
- Revisão independente final: aprovado com ressalvas. Não foram encontrados
  bloqueios técnicos; permanecem ressalvas de evidência física para
  marker/tooltip, tema claro, fonte ampliada e estados vazios.
- Spec `028` encerrada. Não há próxima subtarefa elegível.

## Verificação

Para subtarefas com código ou Gradle:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar ou executar testes automatizados sem solicitação explícita.
