# Spec 029 — Paridade visual com Google Health instalado

- **Ticker:** `029`
- **Status:** `in_progress`
- **Referência visual:** aplicativo instalado `com.fitbit.FitbitMobile`, tratado
  pelo pedido como Google Health/Fitbit
- **Data do planejamento:** 2026-09-01

## Contexto

A direção solicitada mudou: o aplicativo instalado deve ser a única referência
visual da evolução do Champion Companion. A inspeção deve usar o aparelho ADB
conectado, entrar em todos os cards alcançáveis e percorrer todos os filtros,
incluindo estados com dados do dia atual.

O repositório já possui a Spec 024, que registrou uma inspeção estrutural
sanitizada do mesmo tipo de aplicativo, e a Spec 028, que consolidou os gráficos
interativos em `core/designsystem/chart`. A Spec 028 ainda possui a subtarefa
final pendente; nenhuma nova engine de gráfico deve ser criada antes de seu
fechamento.

O pedido usa “pixel a pixel” como objetivo de fidelidade. As regras operacionais
vigentes permitem usar a referência instalada como fonte visual normativa, mas
exigem preservar identidade, contratos, privacidade e evidências sanitizadas.
Logo, este plano busca máxima equivalência visual observável sem copiar marca,
assets, fonte, textos proprietários ou screenshots brutos, nem persistir dados
da referência no aplicativo ou no repositório.

## Objetivo

Refatorar visualmente as superfícies existentes para reproduzir, com fidelidade
estrutural e comportamental verificável, a hierarquia, densidade, ordem dos
cards, agrupamento, filtros, gráficos, calendário, navegação e estados
observados no aplicativo instalado, preservando os dados e contratos próprios do
Champion Companion.

Casos específicos solicitados:

- frequência cardíaca usa amostras locais reais nos gráficos de todos os
  períodos com dados disponíveis; o filtro diário mantém série intradia;
  períodos sem amostras permanecem vazios ou com lacunas, sem dados sintéticos;
- a superfície de exercícios possui somente as abas `Semana` e `Mês`;
- `Mês` exibe calendário de atividades;
- filtros de exercícios ficam abaixo do gráfico ou calendário;
- todos os filtros são exercitados e visualmente comparados, mesmo quando a
  contagem é zero.

## Requisitos

### R1 — Referência instalada única

Usar somente a instância instalada do aplicativo de referência para decisões
visuais. Não usar Google Fit, Health Connect ou sites como referência visual.
Registrar pacote, versão, `versionCode`, Activity, aparelho, resolução,
densidade, escala de fonte, rotação, tema, região observável e data da captura.

### R2 — Baseline completo

Percorrer, sem ações destrutivas, todos os cards alcançáveis na referência e no
Champion. Para cada superfície, registrar tela inicial, detalhe, estados
visíveis, navegação de retorno e filtros. A matriz deve cobrir, no mínimo:

- Hoje/Home;
- Saúde;
- frequência cardíaca;
- peso;
- passos;
- distância;
- calendário e detalhe diário;
- exercícios, dias com exercícios e detalhe de exercício;
- dispositivo;
- perfil;
- configurações.

### R3 — Evidência visual

Capturar screenshots temporários durante a inspeção ADB e versionar apenas
evidências sanitizadas ou reconstruções sem dados pessoais. Cobrir cada card e
filtro relevante, com nome determinístico e matriz de cobertura. Valores de
BPM, peso, títulos de atividade, horários, conta, identificadores e metadados
de imagem não podem permanecer em arquivos versionados.

### R4 — Shell e identidade Champion

Aplicar a composição observada à UI existente sem criar rotas apenas para imitar
a referência. Preservar `AppDestination`, back stack manual Navigation 3,
`ChampionNavigation`, save tokens, guards, ações de acessibilidade, identidade
Champion, dynamic color e tema claro/escuro.

### R5 — Frequência cardíaca

Preservar `HeartRateHistoryPeriod.TODAY`, `LAST_7_DAYS` e `LAST_30_DAYS`.

- `TODAY`: mostrar pontos intradia reais disponíveis.
- `LAST_7_DAYS` e `LAST_30_DAYS`: mostrar dados reais locais dentro da janela,
  com lacunas para dias sem amostras.
- Sem dados: exibir estado vazio, sem reaproveitar dado de outro período.
- Fontes permitidas: medições manuais, histórico D1 e amostras reais de
  atividades, conforme `HeartRateRepository` atual.
- Não derivar BPM de passos, distância, calorias ou Health Connect; não
  interpolar, suavizar, reamostrar ou fabricar série.
- Manter Vico encapsulado no boundary compartilhado e impedir import direto
  pela feature.

### R6 — Exercícios e filtros

Representar a organização observada para dias com exercícios com somente as
abas `Semana` e `Mês`. A aba `Mês` contém calendário de atividades. O controle
de filtro aparece depois do gráfico ou calendário, sem alterar o significado
dos filtros de workflow atuais:

`ALL`, `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED`,
`SYNC_FAILED` e `DISCARDED`.

Filtros sem resultados continuam selecionáveis e exibem estado vazio explícito.
O período e o calendário são estado de apresentação; não devem criar migração,
persistência de filtro ou leitura remota.

### R7 — Preservação funcional

Não alterar importação D3/D4, derivação, revisão, calibração, exclusão,
Health Connect write-only, permissões, BLE, dados históricos, snapshots,
políticas de treino phone ou backup desabilitado. Mudanças de layout não podem
reescrever snapshots históricos ou atividades sincronizadas.

### R8 — Validação física

Validar em aparelho: portrait e landscape, tema claro e escuro, fonte ampliada,
rolagem vertical, rolagem horizontal de gráfico quando habilitada, marker,
tooltip, toque nos cards, retorno, seleção de cada período e cada filtro.
ADB indisponível bloqueia aceite físico e não pode ser convertido em aceite
presumido.

### R9 — Testes do projeto

Não criar nem executar testes automatizados sem solicitação explícita. Para
mudança de código, executar apenas validações de build e lint previstas pelo
projeto, além do smoke físico sanitizado.

## Critérios de aceite

1. A matriz versionada identifica referência, versão, configuração física e
   cobertura de todas as superfícies, cards, detalhes e filtros alcançáveis.
2. Cada card relevante da referência foi aberto ou marcado como `N/A` com
   motivo concreto; nenhum estado é declarado coberto sem evidência ou leitura
   de código correspondente.
3. A referência visual de aceite é somente `com.fitbit.FitbitMobile`; sites,
   Google Fit e Health Connect não são usados para decidir aparência.
4. Home, Saúde, métricas, calendário, exercícios, dispositivo, perfil e
   configurações reproduzem a hierarquia, ordem, densidade, superfícies,
   estados e affordances observados, usando componentes e identidade Champion.
5. Frequência cardíaca exibe série intradia hoje e dados locais reais nos demais
   períodos disponíveis; lacunas e ausência são preservadas sem série sintética.
6. Exercícios exibe somente `Semana` e `Mês`; `Mês` contém calendário de
   atividades; filtros aparecem abaixo do conteúdo gráfico ou calendário.
7. Os sete filtros de workflow continuam funcionais, inclusive filtros vazios,
   sem mudança de semântica, persistência ou sincronização.
8. Nenhuma feature importa Vico diretamente e não existe renderer paralelo ao
   boundary `core/designsystem/chart`.
9. Navigation 3, destinos, back stack, contratos de domínio, Room, DataStore,
   BLE, Health Connect e backup permanecem compatíveis.
10. Screenshots e dumps brutos são temporários; toda evidência versionada está
    sanitizada e não é usada como asset de produção.
11. Validações de lint, build, acessibilidade e smoke físico passam nos estados
    alcançados; limitações ficam registradas como `N/A`.

## Comportamento atual encontrado

- O entrypoint é `MainActivity` → `ChampionCompanionTheme` →
  `core/navigation/ChampionNavigation.kt`; a navegação usa back stack manual e
  não usa `NavHost`/`NavController`.
- `HealthScreen.kt` funciona como hub e abre frequência cardíaca, peso, passos e
  distância.
- `HomeScreen.kt` exibe resumo diário, status do relógio, BPM mais recente,
  distância e calorias estimadas; possui pull-to-refresh e atalhos de dispositivo
  e perfil.
- `HealthMetricDetailScreens.kt` concentra detalhes de frequência cardíaca e
  peso. Frequência usa `HeartRateHistoryPeriod` e peso mantém editor manual.
- Passos e distância usam `CalendarHistoryPeriod.LAST_7_DAYS`,
  `LAST_30_DAYS` e `LAST_90_DAYS`, com shells e gráficos próprios adaptados ao
  renderer compartilhado.
- `HeartRateHistoryChart.kt` já adapta `HeartRateChartSeries` ao gráfico
  compartilhado; resolução intradia/diária e gaps são definidos no read model.
- `CalendarScreen.kt` e `CalendarDayScreen.kt` fornecem calendário mensal,
  marcadores e detalhe diário somente leitura.
- `ExercisesScreen.kt` possui filtro em diálogo acima da lista, paginação de
  cinco atividades e sete `WorkoutActivityFilter`; ainda não representa a
  organização solicitada de abas Semana/Mês com filtro abaixo do gráfico ou
  calendário.
- `WorkoutActivityRepository.observeActivities(filter)` preserva semântica de
  workflow; `observeActivitiesBetween` fornece resumo de calendário.
- `RoomRepositories` consulta histórico cardíaco por fontes manuais, D1 e
  atividades de treino. Não existe autorização para completar série via
  Health Connect readback.
- A Spec 024 já registrou baseline sanitizado e ADR-023 para rollout visual
  incremental. A Spec 028/ADR-027 concentra gráficos em
  `core/designsystem/chart`; a subtarefa `028-10` ainda está `pending`.
- A subtarefa 029-01 confirmou ADB autorizado no Galaxy S22 `SM-S901E`, via
  transporte TLS, e mantém baseline comparativo parcial sanitizado em
  `docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`.
  Ações de conta, pagamento, sync, medição, importação, exclusão e reset não
  foram acionadas. O smoke avançou em tema claro, fonte `1.3`, landscape,
  rolagem, retorno, filtros e períodos cardíacos, mas a cobertura completa tela
  a tela, acessibilidade, marker, tooltip e rolagem horizontal permanece
  pendente. Na solicitação de landscape, Fitbit carregou horizontalmente e o
  Champion permaneceu em portrait; nenhum workaround foi aplicado.

## Abordagem escolhida

1. Congelar baseline instalado completo e sanitizado antes de alterar código.
2. Registrar a decisão arquitetural da referência visual, equivalência desejada,
   limites de cópia e contrato de evidência antes de qualquer código.
3. Fechar Spec 028-10 antes de mexer nos gráficos, evitando segundo renderer.
4. Evoluir tokens e componentes opt-in, depois shell, Home e Saúde.
5. Migrar métricas e gráficos preservando read models, fontes, gaps e contratos.
6. Implementar organização de calendário/exercícios com estado de apresentação,
   sem nova persistência; manter filtros de workflow.
7. Alinhar superfícies restantes uma por vez, com aceite isolado e rollback por
   subtarefa.
8. Executar validação integrada e atualizar matriz, overview, spec e decisão
   somente com fatos observados.

## Alternativas descartadas

- **Cópia literal de screenshots, marca, ativos, fonte ou textos da referência:**
  rejeitada pelos guard rails de identidade, propriedade e privacidade.
- **Usar sites, Google Fit ou Health Connect como referência visual:** rejeitado;
  o pacote instalado é a única fonte visual desta task.
- **Persistir screenshots brutos no repositório ou no app:** rejeitado por
  exposição de dados de saúde e por não ser requisito funcional.
- **Fabricar dados cardíacos nos filtros sem dados:** rejeitado; gráficos usam
  somente amostras locais válidas e preservam ausência.
- **Criar nova engine de gráficos ou importar Vico nas features:** rejeitado por
  ADR-027 e pela consolidação da Spec 028.
- **Reescrever navegação inteira para igualar a referência:** rejeitado; rotas,
  back stack e contratos existentes permanecem estáveis.
- **Refatoração em lote sem checkpoints:** rejeitado; cada superfície recebe
  subtarefa isolada e evidência própria.

## Arquivos, módulos e contratos afetados

### Prováveis arquivos de produção

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/`;
- `core/navigation/ChampionNavigation.kt` e `AppDestination.kt`, somente se
  ajustes visuais puderem preservar destinos e back stack;
- `feature/home/HomeScreen.kt`;
- `feature/health/HealthScreen.kt`, `HealthMetricDetailScreens.kt`,
  `StepsHistoryShell.kt`, `DistanceHistoryShell.kt`,
  `HeartRateHistoryChart.kt`;
- `feature/calendar/CalendarScreen.kt` e `CalendarDayScreen.kt`;
- `feature/exercises/ExercisesScreen.kt`, `ExercisesViewModel.kt` e
  `ExercisesFeatureContract.kt`;
- `feature/device/DeviceScreen.kt`, `feature/profile/ProfileScreen.kt` e
  `feature/settings/SettingsScreen.kt`.

### Contratos a preservar ou adaptar somente quando necessário

- `HeartRateHistoryPeriod`, `HeartRateHistorySnapshot`,
  `HeartRateChartSeries` e `HeartRateRepository`;
- `CalendarHistoryPeriod`, `CalendarDaySummary` e repositórios de calendário;
- `WorkoutActivityFilter`, `WorkoutWorkflowSummary` e
  `WorkoutActivityRepository`;
- `ChampionTimeSeriesChart` e contratos em `core/designsystem/chart`;
- `AppDestination`, `ChampionNavigation` e save tokens.

### Arquivos que não devem mudar nesta task

- entidades, DAOs, migrations e schemas Room, salvo decisão nova e explícita;
- `HealthConnectSyncCoordinator` e contratos write-only;
- `WorkoutRecordingService`, `PhoneWorkoutPolicy`, BLE e localização;
- manifesto e regras de backup.

## Riscos e mitigação

| Risco | Mitigação |
|---|---|
| UI de referência muda por versão, região, conta ou feature flag | Registrar pacote/configuração e tratar baseline como fotografia datada; marcar variações como `N/A`. |
| ADB ou screenshot bloqueado | Manter 029-01 bloqueada até autorização; não declarar aceite físico por inferência. |
| Dados pessoais vazam em pixels ou metadados | Captura temporária, redaction antes de versionar, remoção de dumps e inspeção final dos arquivos. |
| Pixel parity quebra identidade ou acessibilidade | Usar equivalência observável com componentes Champion, sem copiar fonte/asset/texto; validar TalkBack, contraste e fonte ampliada. |
| Gráfico sem dados vira dado fabricado | Estado vazio e gaps explícitos; somente repositories locais e janelas selecionadas. |
| Duplicação de renderer ou mudança de granularidade | Depender de 028-10; features acessam somente contratos compartilhados; não alterar read model. |
| Filtro abaixo do calendário conflita com rolagem | Validar foco, gesto, rolagem vertical/horizontal e retorno em aparelho. |
| Nova aba exige contrato funcional não existente | Tratar Semana/Mês como estado de apresentação; parar e abrir decisão separada se exigir persistência ou novo domínio. |
| Refatoração ampla gera regressão | Rollout por superfície, aceite isolado e rollback por subtarefa. |

## Estratégia de testes e validação

Não criar nem executar testes automatizados. Validar cada subtarefa com:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Quando houver mudança de código, instalar a variante debug no aparelho e
executar smoke físico sanitizado. A matriz deve registrar cada tela, card,
filtro, período, estado vazio, rolagem, tema, orientação e escala de fonte
realmente alcançados. Screenshots brutos não entram no repositório.

## Ordem das subtarefas

1. `029-01-inspecionar-google-health-e-capturar-baseline`
2. `029-02-registrar-decisao-visual-e-contrato-de-evidencia`
3. `029-03-evoluir-fundacao-visual-e-shell`
4. `029-04-alinhar-home-saude-e-metricas`
5. `029-05-alinhar-calendario-e-detalhe-diario`
6. `029-06-alinhar-exercicios-abas-calendario-e-filtros`
7. `029-07-alinhar-dispositivo-perfil-e-configuracoes`
8. `029-08-validar-superficies-e-fechar`

`029-04` depende do fechamento de `028-10` quando tocar renderer ou legado de
gráficos. `029-06` depende das decisões e mapeamentos registrados em 029-01 e
029-02.

## Premissas explícitas

- “Google Health” corresponde ao aplicativo instalado identificado
  historicamente como `com.fitbit.FitbitMobile`; a subtarefa 029-01 deve
  confirmar versão e Activity atuais.
- O aparelho possui dados de hoje já existentes; nenhuma importação,
  sincronização, medição, exclusão ou troca de conta será disparada para obter
  evidência.
- A ausência de dados cardíacos na referência fora do filtro diário é uma
  observação a confirmar no baseline, não autorização para inventar dados.
- Dados locais de BPM existentes podem alimentar gráficos dedicados quando
  estiverem dentro da janela; a origem deve permanecer semanticamente clara.
- Abas Semana/Mês e posicionamento de filtros são mudanças de apresentação,
  não nova persistência ou integração remota.
- Capturas originais podem existir somente temporariamente fora do repositório;
  a evidência duradoura será sanitizada.
- A task não reabre fases encerradas nem transforma o produto em réplica de
  serviços Google; altera somente apresentação das superfícies existentes.
