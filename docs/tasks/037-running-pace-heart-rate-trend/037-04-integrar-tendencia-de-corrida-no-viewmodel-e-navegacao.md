# 037-04 — Integrar tendência de corrida no ViewModel e navegação

- **Ticker:** `037`
- **Número:** `04`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Expor o histórico de corrida como estado tipado do Exercícios, compor read model
e policy fora do Compose e abrir entrada contextual em tela própria.

## Requisitos cobertos

- integração bounded no `ExercisesViewModel`;
- estados `AVAILABLE`, `NO_RUNS`, `NO_COMPARABLE_RUNS`, `UNAVAILABLE`;
- CTA contextual sem nova aba principal;
- separação da carga cardiovascular;
- `asOf`, `ZoneId` e janela 56 dias estáveis;
- nenhum cálculo em Compose ou acesso DAO pelo ViewModel.

## Escopo incluído

- campo de estado dedicado em `ExercisesUiState`;
- combinação do repository, `asOf`, `ZoneId` e policy;
- destino serializável contextual na subtree de Exercícios;
- callback/label/back stack manual conforme padrão cardiovascular;
- CTA acessível `Tendência de corrida`.

## Escopo excluído

- cálculo de pace/FC na tela;
- mudança do histórico de carga 035/036;
- nova aba, Home, Saúde, Perfil ou Settings;
- activity ID no contrato visual, salvo necessidade futura de navegação tipada;
- Health Connect, Room, parser, BLE ou workflow;
- testes automatizados novos.

## Dependências

- `037-02` e `037-03` concluídas;
- `ExercisesFeatureContract.kt`;
- `ExercisesViewModel.kt`;
- `ExercisesScreen.kt`;
- `AppDestination.kt` e `ChampionNavigation.kt`;
- padrão contextual de `CardiovascularLoadHistory`.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- `core/navigation/AppDestination.kt`;
- `core/navigation/ChampionNavigation.kt`;
- novo `RunningPerformanceHistoryScreen.kt` somente como destino de
  apresentação, se a implementação separar a tela.

## Passos de implementação

1. Adicionar estado de UI separado do cardiovascular.
2. Compor read model bounded e `RunningPerformanceHistoryPolicy` em dispatcher
   apropriado, com `asOf` estável.
3. Mapear `NO_HEART_RATE_SAMPLES`, `INVALID_SAMPLES`, cobertura válida e
   `SOURCE_UNAVAILABLE` sem transformar ausência em zero; falha de fonte mantém
   status top-level `UNAVAILABLE`.
4. Adicionar destino serializável contextual e incluir a subtree de Exercícios
   sem alterar navegação primária.
5. Adicionar CTA acessível e retorno contextual.
6. Confirmar que Compose recebe contratos prontos e não acessa DAO/calcula.

## Testes e comandos de validação

Não criar testes automatizados. Executar revisão e:

```bash
git diff --check
```

Build completo foi executado nesta implementação de código.

## Definição de pronto

- estado 037 é independente da carga cardiovascular;
- entrada contextual abre/fecha corretamente;
- estados não confundem ausência com zero;
- ViewModel não consulta DAO nem calcula métricas;
- filtros Semana/Mês da lista existente não contaminam a população 037;
- nenhum destino aparece fora de Exercícios.

## Riscos e cuidados

- preservar saver/restorer e back stack manual;
- não reutilizar `CardiovascularLoadHistoryUiState` por conveniência;
- não chamar `Instant.now()` dentro da policy;
- não criar CTA em Home, Saúde, Dispositivo ou Configurações;
- manter linguagem factual e sem recomendação.

## Resultado da implementação

Exercícios agora expõe `RunningPerformanceHistoryUiState` separado da carga
cardiovascular. O `ExercisesViewModel` compõe a consulta bounded existente,
congela `asOf`, `ZoneId` e a janela semiaberta de 56 dias, transportando `asOf`
e `ZoneId` no estado pronto para labels futuros; executa
`RunningPerformanceHistoryPolicy` em `Dispatchers.Default` e converte falhas de
fonte em `Unavailable`. Compose recebe somente o estado pronto.

Foi adicionado o CTA acessível `Tendência de corrida`, destino serializável
contextual `RunningPerformanceHistory`, persistência/restauração do token do
back stack manual e tela própria inicial para estados de carregamento, ausência
e indisponibilidade. A tela detalhada de cards e gráficos permanece na 037-05.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
  — estado dedicado de tendência.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
  — composição bounded, policy, janela, `asOf`, `ZoneId` e estado seguro.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  — CTA contextual acessível.
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/RunningPerformanceHistoryScreen.kt`
  — shell contextual inicial para o destino.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/AppDestination.kt`
  — destino serializável.
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
  — renderização, back stack, saver/restorer e subtree de Exercícios.
- este arquivo e `037-00-overview.md` — status, progresso e evidências.

## Decisões e desvios

- Decisão: manter estado e consulta de tendência independentes de
  `CardiovascularLoadHistoryUiState` e do fluxo cardiovascular.
- Decisão: manter `asOf`, `ZoneId` e range estáveis durante a vida do
  `ExercisesViewModel`; a policy continua sem relógio do sistema.
- Decisão: tratar resultado top-level `UNAVAILABLE` como indisponibilidade
  segura, inclusive quando a policy o produz por falha batch de FC; ausência
  natural permanece distinta.
- Decisão: usar destino contextual serializável e token próprio, sem aba
  principal ou entradas fora de Exercícios.
- Desvio controlado: a tela criada nesta subtarefa é shell de destino e estados;
  cards, gráficos, provenance, coverage e acessibilidade detalhada pertencem à
  037-05.
- O working tree já continha alteração não relacionada em
  `app/build.gradle.kts` (`release.isDebuggable = true`); ela foi preservada e
  não faz parte desta subtarefa.

## Comandos executados

- `git diff --check`
- `./gradlew lint assembleDebug assembleRelease`
- exploração dirigida dos contratos, repository, ViewModel, Compose e navegação;
  revisão de Spec 037, ADR-037, overview, AGENTS.md e subtarefas dependentes.

## Resultados e evidências

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou; lint, assembleDebug
  e assembleRelease concluídos.
- Primeira execução do build encontrou dois erros de integração: inferência do
  tipo do `Flow.catch` e import ausente de `fillMaxWidth`; ambos foram corrigidos
  antes da execução final aprovada.
- `ExercisesViewModel` não acessa DAO; usa somente
  `WorkoutActivityRepository.observeRunningPerformanceWorkoutsBetween` e a
  policy de domínio.
- Nenhuma alteração em Room, schema, migration, parser, BLE, Health Connect ou
  `CardiovascularLoadWorkoutInput`.

## Riscos residuais

- A renderização completa da lista, gráficos, provenance e coverage pertence à
  037-05; shell atual não substitui essa entrega.
- Validação física em banco real, smoke Release e screenshot sanitizada ficam
  pendentes para 037-06.
- Não foram criados nem executados testes automatizados, conforme escopo e
  regra do repositório.

## Revisão independente

- Agente `review` aprovou a implementação final sem bloqueadores.
- Revisão confirmou estado separado da carga cardiovascular, contexto estável
  de `asOf`/`ZoneId`, policy fora do Compose, CTA acessível, destino contextual,
  saver/restorer e coerência SDD em `4/6`.
- Riscos mantidos: renderização detalhada em 037-05 e validação física em
  037-06.
