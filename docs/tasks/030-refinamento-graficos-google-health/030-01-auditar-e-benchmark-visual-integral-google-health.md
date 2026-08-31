# 030-01 — Auditar e comparar superfícies com Google Health

- **Ticker:** `030`
- **Número:** `01`
- **Status:** `in_progress`

## Objetivo e resultado esperado

Inventariar todas as superfícies produtivas e registrar benchmark sanitizado do
Google Health/Fitbit, incluindo shell, navegação visual, hierarquia, cards,
listas, controles, estados, gráficos, densidade e interação observáveis. A
subtarefa só poderá ser marcada como concluída após matriz integral por tela ser
conferida e aceita.

## Requisitos e critérios cobertos

- R1, R2, R4, R6 e critérios 1–4 e 8 da Spec 030.
- Confirmar que períodos públicos/instalados são referência visual, não contrato
  para criar dados ausentes.

## Escopo incluído

- `core/designsystem`, `core/navigation` e `core/designsystem/chart`;
- Home, Saúde, Calendário, Exercícios, Evolução, Dispositivo, Perfil e
  Configurações;
- referências públicas e app `com.fitbit.FitbitMobile` quando disponível;
- matrizes de gaps por superfície e por gráfico.

## Escopo excluído

- código de produção;
- alteração de períodos, domínio, Room, fluxos ou componentes produtivos;
- screenshots com dados pessoais;
- testes automatizados.

## Regra de referência e implementação

A interface do Google Health/Fitbit pode ser replicada fielmente nesta
subtarefa, incluindo layout, geometria, espaçamento, navegação visual, controles
de período aplicáveis à superfície, gráficos, cores e comportamento de
apresentação. Não copiar código-fonte ou APK, dados pessoais ou credenciais,
nem reutilizar arquivos ou ativos extraídos do app de referência; a
implementação deve usar código próprio no Champion Companion.

## Dependências

- Nenhuma.

## Arquivos e símbolos prováveis

- `docs/specs/030-refinamento-graficos-google-health.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-00-overview.md`;
- `core/designsystem/chart/*`;
- `feature/health/*Chart.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- `feature/evolution/EvolutionScreen.kt`.

## Observação real do app de referência

Em 2026-08-31, o app instalado `com.fitbit.FitbitMobile` foi aberto no Galaxy
S22 autorizado, sem login, troca de conta, escrita, sincronização, medição ou
alteração de dados. A inspeção estrutural temporária observou:

- resumo dominante no overview e cards compactos de métricas;
- detalhe de Peso com seletor `D`/`S`/`M`/`3M`/`A`, navegação temporal, valor
  destacado, marker vertical, eixo Y discreto à direita e resumos agrupados
  abaixo da série.

Essa observação é referência de padrão, não evidência de que cada métrica do
Champion ou cada período do app de referência tenha sido aberto. Nenhum
screenshot, dump, valor ou identificador pessoal foi versionado.

## Matriz objetiva por consumidor

A matriz cruza código auditado no Champion com a observação real acima. O período
alvo é uma convenção visual; somente consultas e granularidades já suportadas
podem ser usadas. `Ocultar/desabilitar` significa não apresentar a opção ou,
se o componente comum exigir o conjunto completo, mantê-la sem ação e sem
query nova.

| Consumidor e código auditado | Período atual | Período-alvo e controle visual | Granularidade | Gaps e ausência | Interação | Fonte e limitação |
|---|---|---|---|---|---|---|
| **Passos** — `StepsHistoryShell.kt`, `StepsHistoryChart.kt`, `StepsHistoryFeatureContract.kt` | `CalendarHistoryPeriod.LAST_7_DAYS` (padrão), `LAST_30_DAYS`, `LAST_90_DAYS`; UI atual `7D/30D/90D`. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova. | Uma barra por dia em `CalendarStepsHistorySnapshot.entries`; valor diário de passos. | `steps == null` permanece lacuna; barra é omitida, lista mantém `Sem dados` e nenhum zero é criado. Falha de fonte mantém estado seguro. | `SelectPeriod` existente; marker mais próximo no renderer; scroll horizontal somente acima de 7 entradas; tela e lista continuam com rolagem vertical/paginação. | Resumos diários locais (`StepsHistoryViewModel`/`CalendarHistoryPeriod`). Não há contrato intradia ou anual. A observação real de referência sustenta apenas resumo, controle e densidade, não paridade de dados. |
| **Distância** — `DistanceHistoryShell.kt`, `DistanceHistoryChart.kt`, `DistanceHistoryFeatureContract.kt` | `CalendarHistoryPeriod.LAST_7_DAYS` (padrão), `LAST_30_DAYS`, `LAST_90_DAYS`; UI atual `7D/30D/90D`. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova. | Uma barra por dia em `CalendarDistanceHistorySnapshot.entries`; distância estimada diária em km. | `estimatedDistanceMeters == null` permanece sem estimativa; barra é omitida, lista e agregados preservam ausência e não usam zero. | `SelectPeriod` existente; marker mais próximo; scroll horizontal acima de 7 entradas; rolagem vertical/paginação da superfície. | Estimativas de resumos diários locais. Não há fonte intradia ou anual; a referência real observada não autoriza criar esses dados. |
| **Frequência cardíaca histórica** — `HealthMetricDetailScreens.kt`, `HeartRateHistoryChart.kt`, `HealthFeatureContract.kt` | `HeartRateHistoryPeriod.TODAY` (padrão), `LAST_7_DAYS`, `LAST_30_DAYS`; UI atual `Hoje/7 dias/30 dias`. | `D` → `TODAY` intradia; `S` → 7 dias; `M` → 30. `3M` e `A` ocultos/desabilitados, sem callback e sem query nova; não converter HR 7/30 em 90. | `TODAY`: amostras reais intradia. 7/30: buckets diários com média, mínimo, máximo e contagem de amostras; mapper preserva posições sem dado como nulas. | Dias sem amostra permanecem ausentes/nulos; snapshot vazio permanece vazio; nunca zero BPM ou interpolação. | `SelectHistoryPeriod` existente; marker mais próximo; scroll horizontal acima de 12 pontos; rolagem vertical da tela e lista paginada. | `HeartRateHistoryQuery` usa fontes locais manual, D1 e atividade de treino elegível; `RoomRepositories` confirma intradia em TODAY e buckets diários em 7/30. Não existe contrato HR de 90 dias ou ano. |
| **BPM no detalhe de treino** — `ExercisesScreen.kt: ActivityHeartRateChartSection` e `WorkoutHeartRateChart` | Janela fixa da atividade, definida por `WorkoutHeartRateSample.sampledAt`; não há período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; replicar fielmente a composição de resumo, unidade e série, com janela real do treino e sem query nova. | Uma amostra real de BPM por timestamp; séries separadas quando timestamps não podem compartilhar lane. | Lista vazia exibe estado indisponível; amostras ausentes não viram pontos sintéticos, linha contínua ou zero. | Marker mais próximo; scroll horizontal acima de 6 pontos; rolagem vertical do detalhe. Não iniciar fluxo phone. | Amostras da atividade importada do relógio (`WorkoutHeartRateSample`). Limites 30–220 BPM e ausência dependem do snapshot; GPS/localização estão fora. |
| **Distância canônica no detalhe de treino** — `ExercisesScreen.kt: ActivityCanonicalChartSection` e `WorkoutCanonicalDistanceChart` | Janela fixa entre início/fim efetivos de `CanonicalWorkoutSeries`; não há período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; mostrar janela efetiva da atividade, sem rederivar distância e sem query nova. | Um ponto por término de intervalo D4; distância canônica acumulada em km. | Série nula ou sem intervalos exibe indisponibilidade; intervalos precisam ser contíguos no contrato; sem interpolação ou suavização. | Marker mais próximo; scroll horizontal acima de 6 intervalos; rolagem vertical do detalhe. | `CanonicalWorkoutSeries` derivada e congelada no snapshot da atividade. Não usar distância/velocidade por passos de bicicleta, GPS ou sessão phone. |
| **Cadência D4 no detalhe de treino** — `ExercisesScreen.kt: ActivityCadenceChartSection` e `WorkoutCadenceChart` | Janela fixa da atividade; somente intervalos D4 elegíveis, sem período de pesquisa nem seletor. | Sem seletor `D/S/M/3M/A`; conservar janela e contexto do treino, sem query nova. | Um ponto no ponto médio de cada intervalo D4, em passos/min; somente série canônica elegível. | Série inelegível, ausente ou sem intervalos exibe indisponibilidade; sem interpolação, suavização, reamostragem ou pontos sintéticos. | Marker mais próximo; scroll horizontal acima de 6 intervalos; rolagem vertical do detalhe. | `DerivedWorkoutInterval`/`CanonicalWorkoutSeries`; bicicleta não recebe cadência por passos. Valores D4 e estado de plausibilidade permanecem explícitos. |
| **Tendências de Evolução** — `EvolutionScreen.kt`, `EvolutionFeatureContract.kt` | `EvolutionPeriod.LAST_7_DAYS`, `LAST_30_DAYS` (padrão), `LAST_90_DAYS`; UI atual expõe `7/30/90 dias` e filtro por modalidade. | `S` → 7 dias; `M` → 30; `3M` → 90. `D` e `A` ocultos/desabilitados, sem callback e sem query nova; manter filtro de modalidade e comparação existente. | Séries diárias com exatamente a janela de `EvolutionVolumeSnapshot`; métricas de tendência mapeadas sem mudar unidade. | `GAP`, `ABSENT` e `NOT_APPLICABLE` permanecem estados distintos; pontos nulos não viram zero; `failed`, vazio e `Não aplicável` continuam explícitos. | `SelectPeriod` e filtro de modalidade existentes; marker mais próximo; scroll horizontal acima de 6 pontos; rolagem vertical, comparação com janela anterior de mesmo tamanho. | `EvolutionReadModel` local, somente atividades elegíveis e filtro atual. Não há fonte anual nem autorização para ampliar read model ou criar dados. |

### Leitura da matriz

- Os históricos de passos e distância compartilham 7/30/90; isso não altera a
  política distinta de frequência cardíaca, que permanece TODAY/7/30.
- Evolução também usa 7/30/90, enquanto o detalhe de treino não pesquisa
  períodos: sua unidade de leitura é a atividade importada e sua janela
  efetiva.
- O padrão real observado (`D/S/M/3M/A`, resumo dominante, marker, eixo discreto
  e resumos sob a série) pode ser reproduzido fielmente na composição. Dados e
  contratos do Champion continuam limitadores semânticos, sem fabricar dados.

## Matriz visual integral por superfície

Esta matriz complementa a matriz gráfica acima. “Cobrir” significa revisar
apresentação e comportamento dos elementos já existentes, não criar uma
funcionalidade equivalente do Google Health.

| Superfície | Composição e referência | Gaps a verificar | Estados e fluxos | Acessibilidade e adaptação | Fonte e limitações |
|---|---|---|---|---|---|
| Shell transversal | navegação inferior, menu Mais, cabeçalhos, transições, cards, botões, chips, banners e diálogos; referência de navegação e hierarquia Fitbit | consistência entre destinos, densidade, espaçamento e contraste | seleção, retorno, confirmação e rolagem | foco, ordem de leitura, fonte ampliada, portrait/landscape e claro/escuro | `ChampionNavigation` e design system; referência pública não define contrato de navegação |
| Home | overview diário, resumo dominante, passos/meta, métricas secundárias, pendências e refresh | hierarquia do resumo, cards compactos, texto auxiliar e feedback secundário | loading, vazio, pré-requisito, erro, retry e Health Connect | resumo acessível, conteúdo longo, temas e orientação | `HomeScreen`; dados locais e limites do resumo impedem paridade de dados |
| Saúde | hub de métricas e detalhes de BPM, peso, passos e distância; detalhe de Peso observado | consistência de cards, cabeçalho, valor, origem, unidade, listas e gráficos | medição manual, D1, edição, paginação, vazio e falha | ações acessíveis, foco, fonte ampliada, rolagem e lacunas | `feature/health`; referência instalada observou overview/Peso, demais padrões ficam `N/A` |
| Calendário | mês, navegação, grade, indicadores, legenda, panorama e detalhe diário | densidade da grade, hierarquia, indicadores por forma/cor e links | seleção, mês vazio, erro e links contextuais | leitura de indicadores sem depender de cor, fonte e orientação | `feature/calendar`; preservar read model e refinamento prévio da Spec 017 |
| Exercícios | importação, filtros, lista, detalhe, revisão, calibração, intervalos, sync e gráficos | hierarquia watch-first, densidade da lista, ações e estados de workflow | progresso, cancelamento, retry, descarte, confirmação e falha parcial | ações destrutivas, foco, fonte, rolagem e temas | `feature/exercises`; sem phone/GPS/rota e sem promoção da PoC debug |
| Evolução | filtro, resumo, tendências, comparação, recordes e links | relação entre período, filtro, resumo e série; redundância textual | loading, vazio, failed, `GAP`, `ABSENT` e `NOT_APPLICABLE` | comparação textual, fonte ampliada, orientação e contraste | `feature/evolution`/Spec 029; não ampliar read model ou criar ano |
| Dispositivo | pré-requisitos, associação, busca, conexão, manutenção e diagnóstico | próxima ação, prioridade de estados, cards e feedback de erro | Bluetooth/permissão, busca, retry, conexão e exportação | foco, mensagens longas, fonte e orientação | `feature/device`; não alterar BLE, permissões ou payloads |
| Perfil | altura, meta, passo, calibração, perfis e histórico | distinção entre valor atual, sugestão, perfil aprendido e correção | edição, correção, remoção, diálogos e estados vazios | labels de unidade, foco, fonte e conteúdo longo | `feature/profile`; preservar DataStore, snapshots e perfis independentes |
| Configurações | disponibilidade, permissão, toggle, fila e ações Health Connect | distinguir estado local, permissão e ação foreground | indisponível, acesso, sincronização explícita e falha | status acessível, foco, fonte e temas | `feature/settings`; Health Connect write-only, local-first e foreground |
| PoC GPS/mapa | não é superfície produtiva | não auditar nesta spec | somente debug | `N/A` | fora desta revisão; não promover a fluxo de produto |

### Limitação da referência

A inspeção real de 2026-08-31 observou overview e detalhe de Peso no pacote
`com.fitbit.FitbitMobile`. As demais linhas serão comparadas às fontes públicas e
ao padrão autorizado, declarando `N/A` quando não houver observação equivalente.

## Passos de implementação

1. Inventariar superfícies, componentes, destinos e políticas de período atuais.
2. Consultar fontes públicas e, se disponível, observar app instalado somente de
   forma estrutural e sanitizada.
3. Registrar padrões observados e limitações da referência.
4. Produzir matrizes por superfície e por consumidor gráfico, com composição,
   controles, estados, interação, limites e fonte/limitação.
5. Conferir matrizes contra código, contratos e observação real antes de
   solicitar aceite da subtarefa.

## Testes e comandos de validação

Task documental; não executar testes automatizados. Validar links, ticker,
consumidores e privacidade da evidência.

## Definição de pronto

- todas as superfícies produtivas estão listadas;
- cada superfície possui composição, controles, estados, fluxos, limites e
  fonte/limitação preenchidos na matriz;
- cada consumidor gráfico mantém período atual, período-alvo, granularidade,
  gaps, interação e fonte/limitação na matriz gráfica;
- períodos de referência e períodos existentes estão comparados;
- benchmark não contém dados pessoais, credenciais ou arquivos/ativos extraídos
  do app de referência;
- resultado, limitações e evidências estão registrados neste arquivo e no
  overview; o status só muda para `completed` após conferência da matriz.

## Riscos e cuidados

- páginas públicas podem não expor matriz completa de períodos;
- declarar limites e não transformar ausência de observação em requisito;
- não registrar labels livres, métricas ou identificadores pessoais.

## Registro de execução

### Status

`in_progress`; subtarefa reaberta após revisão do plano. A auditoria anterior
cobria somente gráficos e não satisfaz o novo inventário visual integral.

### Arquivos alterados

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/specs/030-refinamento-graficos-google-health.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-00-overview.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-02-definir-fundacao-visual-e-shell.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-03-refinar-home-e-saude.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-04-refinar-calendario-e-exercicios.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-05-refinar-evolucao-dispositivo-perfil-e-configuracoes.md`;
- `docs/tasks/030-refinamento-graficos-google-health/030-06-validar-e-fechar-refinamento-visual.md`;
- este arquivo.

### Decisões e desvios

- Consumidores produtivos confirmados: passos, distância e frequência cardíaca
  em Saúde; BPM, distância canônica e cadência D4 em Exercícios; tendências em
  Evolução. Todos passam por `core/designsystem/chart`.
- A matriz gráfica existente foi preservada como submatriz, e uma matriz visual
  integral foi adicionada para cobrir shell e todas as superfícies produtivas.
- `030-01` volta a `in_progress`; o progresso do overview volta a `0/6` até a
  nova auditoria ser concluída.
- `HealthChart` em `ChampionDisplayComponents.kt` continua legado de
  preview/testes, sem consumidor produtivo confirmado; remoção fica para
  `030-06` somente após comprovação.
- App instalado foi confirmado no pacote `com.fitbit.FitbitMobile` e aberto no
  Galaxy S22 autorizado. A inspeção visual temporária mostrou overview com
  resumo dominante, cards compactos de métricas e detalhe de Peso com seletor
  `D`/`S`/`M`/`3M`/`A`, navegação temporal, valor destacado, marker vertical,
  eixo Y discreto à direita e resumos agrupados abaixo do gráfico.
- O padrão adotado para a task é esse seletor abreviado, mantendo descrição
  acessível completa. Opções sem suporte ficam ocultas ou desabilitadas e não
  geram query nova; `A` não é criado por ausência de fonte real.
- A captura temporária continha dados pessoais, não foi versionada nem usada
  como evidência; foi removida após inspeção. Nenhuma ação de gravação, sync ou
  medição foi disparada.
- As fontes públicas consultadas não definem contrato universal de períodos;
  a observação do app instalado foi usada para confirmar a referência visual e
  não para autorizar novas consultas.
- A governança da Spec 030 foi ampliada de gráficos para fidelidade visual
  integral, mantendo limites técnicos, legais e semânticos próprios.

### Comandos e consultas executados

| Comando/verificação | Resultado |
|---|---|
| `adb devices -l` | Passou; Galaxy S22 autorizado e operacional |
| `adb shell pm path com.fitbit.FitbitMobile` | Passou; pacote e splits instalados |
| `adb shell monkey -p com.fitbit.FitbitMobile 1` | Passou; Google Health aberto sem ação de escrita |
| `adb exec-out screencap -p` temporário | Passou; inspeção visual local, sem versionamento |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; app do projeto restaurado em primeiro plano |
| `https://healthapp.google/` | Consultado; hierarquia e proposta de visão holística confirmadas |
| Google Play `com.fitbit.FitbitMobile` | Consultado; screenshots públicas e descrição de padrões consultadas |
| `git diff --check` | Passou; nenhuma falha de whitespace nas alterações rastreadas |

Não foram criados, alterados ou executados testes automatizados.

### Resultado e evidências

- Auditoria anterior encontrou cinco famílias produtivas de gráficos, incluindo
  Evolução, e uma implementação Canvas legada sem consumidor produtivo.
- A nova auditoria identificou shell, Home, Saúde, Calendário, Exercícios,
  Evolução, Dispositivo, Perfil e Configurações como superfícies produtivas a
  incluir antes do aceite.
- Problemas confirmados para a próxima subtarefa: alturas fixas de 190/210 dp,
  limiares de scroll diferentes, labels locais concorrentes e seleção que não
  alimenta resumo contextual.
- Benchmark sanitizado foi somente inspeção efêmera do aparelho; nenhum PNG foi
  mantido no repositório.
- A documentação atualizada registra replicação visual fiel, períodos `D`/`S`/`M`/`3M`/`A`
  e limites técnicos/legais sem alterar produção ou status da subtarefa.

### Reexecução e aceite

- Matriz gráfica anterior foi conferida contra contratos e consumidores atuais;
  matriz visual integral ainda aguarda conferência final.
- Confirmados: passos/distância em 7/30/90 dias; frequência cardíaca em
  `TODAY`/7/30; Evolução em 7/30/90; treino sem seletor de pesquisa.
- Divergências visuais permanecem para `030-02` em diante: shell, cards,
  estados, vocabulário de períodos, alturas fixas, texto auxiliar, resumo
  contextual e limiares de scroll.
- `030-02` não deve criar enum, query, read model ou período anual.
- Status permanece `in_progress` até matriz visual integral ser conferida.

### Riscos residuais

- Matriz completa de telas do Google Health não foi inferida de páginas públicas;
  o benchmark será limitado aos padrões observáveis e às superfícies reais do
  Champion.
- Estados vazios, fonte ampliada, landscape e tema claro do app de referência
  não foram exercitados nesta auditoria documental e permanecem `N/A`; a
  validação física visual do Champion fica prevista em `030-02` e nas subtarefas
  de código.
