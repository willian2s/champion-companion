# 015-06 — Implementar ViewModels temporais e isolar shells

- **Status:** concluída em 28/07/2026.
- **Objetivo:** conectar estado temporal ao read model e separar ownership das
  telas de históricos antes do paralelismo visual.
- **Resultado esperado:** `CalendarViewModel` funcional e superfícies de passos
  e distância isoladas, sem iniciar operações externas.

## Documentos obrigatórios

Spec 015, 015-03/05, contratos de Saúde, `HealthViewModel.kt`,
`HealthMetricDetailScreens.kt`, `CalendarFeatureContract.kt`, Navigation3 e
baseline de UI.

## Dependências e entradas

- 015-05 concluída.

## Arquivos permitidos

`feature/calendar`, contratos/ViewModels temporais de `feature/health` e
arquivos de composição necessários para isolar passos/distância; documentação.

## Arquivos proibidos

DAOs, entities, migrations, BLE coordinators, Health Connect, workflows de
exercícios, testes e alterações de roadmap/ADR.

## Escopo

Implementar estado `selectedMonth`, `selectedDate`, snapshots, loadings e erro;
intents Load, navegação mensal, Hoje, seleção, abertura, retry e dismiss.
Separar shells de passos/distância para reduzir conflitos entre tasks 015-09/10.
Manter `HealthViewModel` sem gatilhos automáticos.

## Fora do escopo

Grade final, gráficos finais, detalhe diário final, nova rota, BLE, provider,
permissões e testes.

## Passos

1. Criar ViewModel temporal com lifecycle seguro.
2. Conectar repository por Hilt.
3. Implementar intents e estados transitórios.
4. Isolar componentes de passos/distância.
5. Garantir observação local passiva.
6. Documentar ownership de arquivos para paralelismo.

## Critérios de aceite

- Load não inicia BLE, Health Connect ou sync.
- Todos intents possuem estado determinístico.
- Erros podem ser descartados e repetidos.
- Shells isolados permitem trabalho paralelo sem conflito inevitável.

## Validação manual

Abrir/fechar tela, navegar estados vazios e erro local. Screenshot: exigido se
layout visível mudar; registrar na task.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar fluxo de intents, arquivos isolados e screenshot quando aplicável.
Atualizar overview após aceite.

## Validação final

Confirmar que operações externas continuam proibidas e que 015-07/09/10 têm
ownership claro. Não criar, alterar ou executar testes automatizados.

## Contexto

Calendar não possui ViewModel real e os shells de passos/distância compartilham
arquivos com Saúde, criando conflito para implementação paralela.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarViewModel.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- este arquivo.

## Decisões e desvios

- `CalendarViewModel` usa somente `CalendarRepository`, `viewModelScope` e um
  `ZoneId` congelado por instância; `Load` inicia observação passiva local e não
  inicia BLE, Health Connect, refresh ou sync.
- Estado inicial usa mês e data atuais. Navegação mensal preserva dia selecionado
  quando possível, limitando-o ao último dia do mês de destino; seleção inválida
  mantém o estado anterior e produz erro não repetível.
- Emissões com `CalendarDataIssue` produzem erro seguro no escopo correspondente,
  preservando snapshot parcial; `Retry` recria a observação mensal ou diária e
  `DismissError` somente remove o erro.
- `StepsHistoryShell.kt` passou a ser owner de `StepsDetailScreen` e do shell de
  histórico de passos. `DistanceHistoryShell.kt` passou a ser owner equivalente
  da distância. `HealthMetricDetailScreens.kt` mantém Saúde, Peso, BPM e helpers
  de composição compartilhados; `HealthViewModel` e workflows externos não foram
  alterados.
- Nenhum desvio aprovado. Extração preservou layout e textos existentes; não há
  screenshot aplicável nesta subtarefa.

## Screenshot

Não aplicável: nenhuma superfície visual foi alterada; apenas ownership de
composição foi separado. Calendário permanece placeholder até 015-07.

## Riscos residuais

- `CalendarScreen` ainda não consome `CalendarViewModel`; integração da grade e
  navegação fica para 015-07/015-08.
- Gráficos e listas funcionais continuam para 015-09 e 015-10, cada qual com
  ownership em arquivo próprio.
- Validação offline de calendário com dados reais depende das superfícies visuais
  das subtarefas posteriores.

## Comandos executados

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew app:lintDebug`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`;
- `git diff --check`;
- `adb shell am force-stop br.com.mwconsulting.championcompanion.dev && adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity && adb shell dumpsys activity activities`.

Não foram criados, alterados ou executados testes automatizados, conforme regra
da spec e desta subtarefa.

## Resultados e evidências

- `:app:compileDebugKotlin` e `app:lintDebug` concluíram com `BUILD SUCCESSFUL`
  após implementação e correção de retry por issues.
- `lint assembleDebug assembleRelease` concluiu com `BUILD SUCCESSFUL`; APKs
  debug/release e lint foram validados.
- `installDebug` concluiu com `BUILD SUCCESSFUL` e instalou o APK no dispositivo
  `SM-S901E`.
- Smoke manual de inicialização no mesmo dispositivo mostrou
  `MainActivity` retomada e `launchFailed=false`; nenhuma interação visual foi
  necessária porque não houve mudança de layout.
- `git diff --check` concluiu sem erros.
- `CalendarViewModel` despacha os oito intents, cancela observações anteriores,
  propaga cancelamento e expõe loading, snapshots e erros determinísticos.
- Erros de fonte e `CalendarDataIssue` são seguros, descartáveis e repetíveis;
  datas inválidas são explicitamente não repetíveis.
- Shells de passos e distância estão separados, sem alteração em
  `HealthViewModel`, BLE, Health Connect, sync ou permissões.

## Atualização do overview

Item `015-06` marcado `[x]`; progresso recalculado para `6/13`. Demais itens
permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
