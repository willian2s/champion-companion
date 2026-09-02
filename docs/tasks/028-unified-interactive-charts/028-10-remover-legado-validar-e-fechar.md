# 028-10 — Remover legado, validar e fechar

- **Ticker:** `028`
- **Número:** `10`
- **Status:** `completed`
- **Depends on:** `028-09`

## Objetivo

Consolidar a Spec 028 após todas as migrações, remover somente o código legado de gráficos que ficou comprovadamente sem uso e registrar a validação final integrada.

## Escopo

- inventariar novamente consumidores de `Canvas` usados para visualização de dados;
- remover implementações antigas substituídas de passos, distância, BPM e detalhe de treino;
- remover helpers de normalização/desenho sem consumidores;
- remover `HealthChart` legado somente se nenhum consumidor legítimo permanecer;
- confirmar que features migradas não importam Vico diretamente;
- revisar dependências e imports sem uso;
- atualizar Spec 028, overview e ADR caso o resultado final tenha ajustes factuais;
- registrar evidência final das superfícies migradas.

## Guard rails

- não remover Canvas decorativo ou infraestrutura não relacionada apenas por busca textual;
- não fazer refatorações funcionais fora da Spec 028;
- não alterar Room, BLE, Health Connect ou métricas;
- não alterar a granularidade dos dados;
- não criar testes automatizados sem pedido explícito.

## Checklist final funcional

Confirmar:

- histórico de passos;
- histórico de distância;
- histórico de frequência cardíaca;
- BPM no detalhe do treino;
- distância canônica no detalhe do treino;
- cadência D4 no detalhe do treino;
- marker/tooltip comum;
- eixos e unidades reais;
- gaps preservados;
- tema claro/escuro;
- rolagem vertical convivendo com os gestos;
- scroll/zoom apenas onde explicitamente habilitados.

## Critérios de aceite

1. Nenhum gráfico migrado mantém engine Canvas paralela.
2. Nenhuma feature importa Vico diretamente.
3. O boundary `core/designsystem/chart` concentra renderer, interação e tema.
4. Código legado sem consumidores é removido de forma comprovada.
5. Não há mudança de domínio, persistência ou sincronização decorrente da consolidação.
6. A Spec 028 e o overview refletem o estado final real.
7. Validação física integrada não encontra regressão bloqueante nas superfícies migradas.

## Validação final

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Em aparelho, repetir smoke sanitizado das superfícies migradas e registrar screenshots representativos.

## Fechamento

Ao concluir, atualizar:

- `docs/specs/028-unified-interactive-charts.md` para `completed`;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md` para `10/10`;
- este arquivo com resultado final, limitações e evidências;
- ADR-027 somente se a decisão arquitetural efetiva tiver mudado.

## Resultado final

- `HealthChart`, `HealthChartPoint` e `normalizedChartY` foram removidos de
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionDisplayComponents.kt`.
- Os dois blocos de preview do legado e `previewChartPoints` foram removidos de
  `ChampionDisplayPreviews.kt`.
- Somente os testes específicos de `HealthChart` foram retirados do teste misto
  de cards; `HealthCard` e `MetricCard` continuam cobertos. O arquivo
  `HealthChartScaleTest.kt` foi excluído por testar helper removido.
- Busca final não encontrou `Canvas`/`nativeCanvas` de visualização de dados nas
  features migradas. Nenhum Canvas decorativo foi removido.
- Vico continua necessário e encapsulado em
  `core/designsystem/chart/ChampionCharts.kt`; não há import de Vico nas
  features migradas e dependências Gradle não foram alteradas.
- Domínio, persistência, BLE, Health Connect, métricas e granularidade não foram
  alterados. ADR-027 não mudou porque a decisão arquitetural permanece vigente.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionDisplayComponents.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionDisplayPreviews.kt`;
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionDisplayComponentsTest.kt`;
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/designsystem/HealthChartScaleTest.kt` (excluído);
- `docs/specs/028-unified-interactive-charts.md`;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md`;
- `docs/integration/028-10-final-steps.png`;
- `docs/integration/028-10-final-distance.png`;
- `docs/integration/028-10-final-health.png`;
- `docs/integration/028-10-final-health-scrolled.png`;
- `docs/integration/028-10-final-workout.png`;
- `docs/integration/028-10-final-workout-cadence-hr.png`;
- este arquivo.

## Comandos, evidências e limitações

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — primeira execução falhou
  por remoção indevida do import `contentDescription`, ainda usado por
  `MetricCard`; o import foi restaurado e a segunda execução passou
  (`BUILD SUCCESSFUL`).
- `./gradlew installDebug` — passou no `SM-S901E` (`BUILD SUCCESSFUL`).
- `adb devices` — `SM-S901E` disponível.
- Não foram criados nem executados testes automatizados, conforme AGENTS/spec.
- Smoke físico integrado no `SM-S901E` / build `.dev` confirmou os gráficos de
  passos, distância e BPM no período de 7 dias, com gaps e eixos/unidades
  visíveis; confirmou também distância canônica, BPM real e cadência D4 por
  intervalo no detalhe do treino, além de rolagem vertical. Screenshots
  sanitizados registrados nos arquivos `028-10-final-*.png` listados acima.
- Tema claro, fonte ampliada e estados vazios não foram repetidos nesta sessão;
  permanecem cobertos por inspeção e pelas evidências anteriores quando
  disponíveis. Marker/tooltip foi tentado, mas não houve captura reproduzível.

## Riscos residuais

- Não há bloqueio técnico identificado após lint, builds e smoke físico.
- Riscos residuais: marker/tooltip sem captura reproduzível nesta sessão; tema
  claro, fonte ampliada e estados vazios não foram repetidos integralmente.

## Revisão independente

- Resultado: **aprovado com ressalvas**.
- Checklist SDD, ticker, links, remoção do legado, boundary Vico, preservação
  de domínio e dependências foram aprovados.
- Ressalvas mantidas: evidência física reproduzível de marker/tooltip, tema
  claro, fonte ampliada e estados vazios permanece incompleta.
