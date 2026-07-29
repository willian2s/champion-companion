# 015-10 — Implementar gráfico e lista de histórico de distância

- **Status:** concluída em 28/07/2026.
- **Objetivo:** transformar shell de distância em histórico local funcional.
- **Resultado esperado:** períodos de 7, 30 e 90 dias usando exclusivamente
  distância estimada diária.

## Documentos obrigatórios

Spec 015, 015-06, 015-05, `DailySummary`, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt`, detalhes de exercício e design system.

## Dependências e entradas

- 015-06 e 015-05 concluídas.

## Arquivos permitidos

Componentes isolados de distância, ViewModel temporal de distância e modelos de
apresentação; documentação e screenshots.

## Arquivos proibidos

Distância de exercícios nos totais, BLE, D1/D3/D4, Health Connect, Room/schema,
navigation, testes e edição de peso.

## Escopo

Exibir total, média por dia com valor, máximo, dias com estimativa, dias sem
estimativa, gráfico, lista e estados acessíveis. Preservar unidade e origem
estimada. Distância de atividade permanece no card da atividade.

## Fora do escopo

Calibração nova, soma de workouts, conversão de fontes, edição, sincronização e
histórico de distância de exercícios.

## Passos

1. Conectar snapshot diário.
2. Implementar períodos 7/30/90.
3. Calcular somente dias com valor.
4. Renderizar gráfico e lista textual.
5. Integrar vazios, loading e erro.

## Critérios de aceite

- Fonte exclusiva é `DailySummary.estimatedDistanceMeters`.
- Dia sem estimativa não vira zero.
- Métricas ignoram dias sem valor.
- Cards de atividade não são alterados indevidamente.
- Períodos funcionam offline.
- Histórico mostra data, valor, origem, última atualização e indicação de
  estimativa.
- Domínio permanece em metros e apresentação usa quilômetros.
- Lista distingue dias com estimativa e dias sem estimativa.
- Total, média e máximo consideram somente dias com valor.

## Validação manual

Galaxy S22 com dias estimados e sem estimativa; conferir gráfico, lista, unidade,
acessibilidade e screenshot. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar fonte, cálculo, tratamento de ausência, preservação dos cards de
atividade e screenshots. Atualizar overview.

## Validação final

Confirmar nenhuma soma de exercício e nenhum acesso externo. Não criar, alterar
ou executar testes automatizados.

## Contexto

Distância diária estimada existe em DailySummary, enquanto o shell atual não
possui gráfico ou histórico.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryChart.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryFeatureContract.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryViewModel.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- este arquivo.

## Decisões e desvios

- `DistanceHistoryViewModel` observa `CalendarRepository.observeDistanceHistory` com
  `ZoneId` congelado e janela semiaberta incluindo o dia atual. A observação é
  local e passiva; nenhum BLE, Health Connect, refresh ou sync é iniciado.
- A UI oferece `7D`, `30D` e `90D`, calcula total, média e máximo somente de
  `estimatedDistanceMeters` não nulos e mantém dias sem estimativa como lacunas.
- O gráfico usa barras para valores e contornos para ausência. A lista é
  ordenada do dia mais recente para o mais antigo, paginada em blocos de 10 e
  informa data, valor em quilômetros, origem, última atualização e estimativa.
- Distância permanece em metros no domínio e é convertida para quilômetros
  somente na apresentação. Distância de atividades não participa dos agregados.
- Desvio aprovado pelo usuário: como o dispositivo não possui dados pessoais
  suficientes, valores de distância não puderam ser reproduzidos na validação
  física. O estado vazio seguro foi preservado e a task foi concluída com o
  débito registrado abaixo.

## Screenshot

Não foi possível registrar screenshot de gráfico com valores: o dispositivo de
validação não possui dados pessoais suficientes para produzir estimativas. O
estado sem estimativa permanece coberto pela UI e deve ser fotografado junto com
dados reais quando o débito de perfil for resolvido.

## Riscos residuais

Usuário pode interpretar distância estimada como medida de exercício; origem e
rótulo devem permanecer explícitos.

- **Débito D-015-10:** implementar preenchimento/edição de perfil com dados
  necessários à política de estimativa, especialmente altura ou passada válida,
  e então recalcular/persistir `DailySummary.estimatedDistanceMeters`. Repetir
  validação física de dias com estimativa e sem estimativa após essa entrega.
- TalkBack, rotação e gráfico com valores positivos permanecem validações físicas
  pendentes até haver perfil preenchido.

## Atualização do overview

Item `015-10` marcado `[x]`; progresso recalculado para `11/14`. Itens seguintes
permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Comandos executados

- `./gradlew :app:compileDebugKotlin` — primeira execução falhou por importação
  incorreta de `LocalActivity`; import corrigido e nova execução concluiu com
  `BUILD SUCCESSFUL`;
- `./gradlew app:lintDebug` — `BUILD SUCCESSFUL`;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
- `./gradlew installDebug` — `BUILD SUCCESSFUL`, APK instalado no `SM-S901E`;
- `adb shell am force-stop br.com.mwconsulting.championcompanion.dev && adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity && adb shell dumpsys activity activities` — inicialização sem `launchFailed`;
- `git diff --check` — sem erros.

Não foram criados, alterados ou executados testes automatizados, conforme regra
da spec e desta subtarefa.

## Resultados e evidências

- Compilação Kotlin, lint e assemblies debug/release concluíram com sucesso.
- `installDebug` concluiu com sucesso no Galaxy S22 `SM-S901E`.
- Smoke de inicialização confirmou `launchFailed=false`.
- Implementação consome exclusivamente o snapshot local de
  `DailySummary.estimatedDistanceMeters`; não soma distância de exercícios.
- Ausência permanece distinta de zero em agregados, gráfico, lista e estado
  vazio. Valores físicos com estimativa aguardam resolução do débito D-015-10.
- Revisão independente aprovou o código condicionalmente; única pendência de
  fechamento visual é validação com perfil preenchido e screenshot correspondente.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
