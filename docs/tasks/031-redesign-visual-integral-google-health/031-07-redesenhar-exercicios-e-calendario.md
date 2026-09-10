# 031-07 — Redesenhar Exercícios e calendário

- **Ticker:** `031`
- **Número:** `07`
- **Status:** in_progress

## Objetivo

Aplicar layout da referência ao fluxo de Exercícios e reproduzir calendário
exclusivo de exercícios, resumo e lista mensal agrupada por semana na aba `Mês`,
com navegação temporal também na aba `Semana`, mantendo importação e revisão locais.

## Requisitos e aceite

- Lista, filtros, CTA, progresso, retry, cancelamento e estados preservados.
- Detalhe mantém revisão, calibração, descarte, gráficos e sync explícito.
- A aba `Mês` da tela de Exercícios substitui o gráfico mensal por calendário
  exclusivo de exercícios, resumo, controles de métricas e lista agrupada por
  semana; setas de mês alteram todos esses dados. Home e Saúde não recebem
  integração direta com Calendário.
- Na aba `Semana`, o filtro por status fica abaixo do gráfico e antes da lista;
  duas setas alteram a semana e atualizam gráfico/lista; na aba `Mês`, ele não é
  exibido e o calendário usa somente exercícios.
- Na aba `Mês`, não há lista de atividades individuais; permanecem calendário,
  resumo, controles `Dias com exercícios`, `Duração` e `Distância`, e agregados
  por semana, no padrão observado na referência.
- O calendário não exibe passos, frequência cardíaca, peso ou Health Connect e
  não oferece botão `Abrir dia`.
- Cards de atividade seguem a lista das telas de Saúde: título à esquerda,
  distância à direita, horário e duração na linha seguinte, sem origem nem seta.
- A grade permite seleção visual de dia, sem botão `Abrir dia` ou navegação
  adicional; rotas legadas permanecem compatíveis.
- Nenhuma ação BLE/sync é disparada somente para renderizar tela.

## Escopo

Inclui composição visual do fluxo de Exercícios, calendário exclusivo de exercícios, lista
mensal agrupada por semana e posicionamento contextual do detalhe diário.
`CalendarScreen` permanece somente
compatibilidade de estado legado/restaurado. Exclui novo workflow, treino phone,
GPS, rota, mapa, localização e alteração de parser.

## Dependências

- `031-04`, `031-05`, `031-06`.
- Specs 010, 011, 014, 015, 021, 027, 028.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesScreen.kt`, shells e detalhes.
- `ExercisesViewModel.kt`, contratos e import coordinators.
- `feature/calendar/CalendarScreen.kt`, `CalendarDayScreen.kt` e contratos.

## Passos

1. Mapear blocos de exercícios/calendário da referência.
2. Integrar calendário, resumo e lista mensal agrupada à aba `Mês` de Exercícios
   sem duplicar read model ou ViewModel.
3. Preservar rotas legadas existentes, sem adicionar botão `Abrir dia` ao fluxo
   de Exercícios.
4. Validar lista, filtros, calendário, detalhe e ações não destrutivas.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado sem iniciar importação ou sync para gerar evidência. Cobrir
rolagem, filtros, calendário, retorno, fonte ampliada, landscape e temas.

## Definição de pronto

Exercícios contém calendário contextual, rotas não ficam órfãs e todos os
workflows existentes mantêm comportamento; evidência sanitizada, estado
inicial/restaurado do aparelho e revisão independente aprovados.

## Riscos

Duplicação entre Calendário e Exercícios pode causar divergência temporal. Reusar
contratos e manter uma única fonte de read model.

## Execução e evidências

- **Status da execução:** `in_progress`; implementação e smoke dos fluxos
  principais concluídos, mas revisão independente final ainda pendente.
- **Arquivos alterados:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarScreen.kt`,
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`,
  `docs/specs/031-redesign-visual-integral-google-health.md` e
  `docs/decisions/adr-028-redesign-visual-integral-google-health.md`.
- **Decisões:** Exercícios recebeu filtro independente `Semana`/`Mês`, resumo,
  gráfico semanal com lacunas reais, filtro por status abaixo do gráfico somente
  na aba `Semana`, lista paginada em cards no estilo das listas de Saúde.
  `Mês` reseta status restritivo para `Todos`, não exibe filtro nem lista
  individual e mostra somente agregados semanais em cards equivalentes. Semanas
  com zero exercícios são omitidas do agregado.
  `Mês` agora substitui o gráfico mensal por calendário embutido, seleção de dia,
  resumo `Total por semana` e agregados por semana civil. `CalendarScreen`
  fica compatível somente para estado legado/restaurado; não há CTA principal
  para página separada. Nenhuma consulta nova ou alteração de read model foi
  criada. A semana usa sete dias civis, domingo–sábado; não foi criada meta fixa
  de quatro dias, pois nenhum contrato local autoriza essa meta e a Spec 031
  proíbe inventar metas. Origem, estado, tipo, ícone, horário, duração e
  distância continuam visíveis nos cards.
- **Desvios:** a referência física confirmou `Semana`, `Mês`, `Esta semana`,
  `Este mês`, `Dias com exercícios`, `Total por semana` e gráfico acessível de
  dias de exercícios. A inspeção de Exercícios foi feita nesta execução; a
  referência usa um denominador visual de quatro dias na semana, mantido fora
  do produto por ausência de contrato. O detalhe diário legado ainda mantém
  métricas amplas por requisito de preservação, enquanto o calendário embutido
  usa somente exercícios.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin
  --no-daemon`; `./gradlew app:lintDebug --no-daemon`; `./gradlew lint
  assembleDebug assembleRelease --no-daemon`; `./gradlew installDebug
  --no-daemon`; `adb devices -l`; `adb shell pm path com.fitbit.FitbitMobile`;
  `adb shell cmd package resolve-activity --brief com.fitbit.FitbitMobile`;
  `adb shell am start -n com.fitbit.FitbitMobile/com.fitbit.HealthBrandedAlias`;
  dumps estruturais temporários e alternância `Semana`/`Mês` na referência.
- **Resultados/evidências:** compile, lint, builds Debug/Release e instalação
  Debug passaram. Inspeção sanitizada no aparelho autorizado encontrou a
  referência instalada, confirmou `com.fitbit.HealthBrandedAlias`, e observou
  `1 de 4 dias com exercício`, `Esta semana`, gráfico acessível, `Hoje`,
  `Semana`, `Mês`, `Este mês`, `Dias com exercícios` e `Total por semana`.
  Smoke do Champion confirmou tela Exercícios, alternância para `Mês`, ausência
  de CTA separado de Calendário, calendário embutido no lugar do gráfico mensal,
  seleção/descrição de dias, resumo semanal e ausência do filtro por status e da
  lista individual na visão mensal. Na visão semanal, `Filtro da lista` aparece
  após o gráfico e cards mantêm affordance de lista. Nenhuma
  importação, sincronização, medição ou exclusão foi iniciada. Dumps temporários
  foram removidos e suas ausências foram confirmadas no aparelho.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md`.
- **Revisão independente:** revisão preliminar solicitou correções de paginação,
  alinhamento domingo–sábado da grade e recorte de semanas; correções foram
  aplicadas. Após confirmação do usuário, o plano foi refeito para calendário
  embutido na aba `Mês` e a implementação foi validada novamente. Revisão final
  ainda necessária.
- **Riscos residuais:** smoke físico completo da variante Champion ainda não
  confirmou alternância dos filtros, calendário, retorno, fonte ampliada,
  landscape e temas. TalkBack e dados representativos continuam sem validação;
  nenhuma ação operacional foi disparada para fabricar evidência.
- **Atualização posterior:** uma tentativa de `./gradlew installDebug --no-daemon`
  falhou com `No connected devices` após reinício do daemon ADB; após o aparelho
  ser reconectado, a instalação foi repetida com sucesso. Smoke final confirmou
  cards da lista semanal, calendário embutido, ausência de status/lista individual
  no mês e ausência de semanas agregadas com zero exercícios. Nenhum dado bruto
  foi versionado.
- **Atualização visual final:** lista semanal foi ajustada para o padrão de lista
  de Frequência cardíaca: cards sem seta/origem, distância alinhada ao título e
  duração junto ao horário de início. Agregado mensal usa linhas sem cards e
  omite semanas com zero exercícios. `Filtro da lista` permanece somente abaixo
  do gráfico semanal. Rótulos `Distância` e `Distância estimada` foram removidos
  do card; ele exibe somente valor em quilômetros. Detalhes diagnósticos
  preservam nomenclatura técnica quando aplicável.
- **Atualização de referência:** a tela semanal da referência foi reaberta e a
  lista confirmou agrupamento por dia, com `Hoje` acima das atividades. A lista
  semanal do Champion passou a agrupar cards pelo mesmo cabeçalho; cada card
  mostra título à esquerda, valor em quilômetros à direita e horário/duração na
  linha seguinte. O estilo da lista mensal foi alinhado ao de Frequência
  cardíaca: linhas simples, cabeçalho de quantidade e valor alinhado à direita.
- **Última validação:** smoke ADB sanitizado confirmou agrupamento `Hoje`,
  ausência de `Origem` e do rótulo `Distância` nos cards, calendário embutido,
  `Total por semana`, ausência de filtro/lista individual na visão mensal e
  ausência de semanas agregadas com zero exercícios. Dump temporário foi
  removido e confirmado ausente.

- **Atualização de 2026-09-08:** inspeção somente leitura da referência e nova
  diretriz do usuário confirmaram que o filtro `Mês` deve manter calendário
  contextual dentro de Exercícios, além de resumo mensal, controles `Dias com exercícios`,
  `Duração` e `Distância`, e lista de cartões semanais com período à esquerda,
  total de atividades e quantidade de tipos de exercício à direita. A implementação
  foi alinhada a essa composição e os
  cartões individuais retornaram a `surfaceContainer`, altura mínima de `96 dp`,
  padding médio e borda colorida conforme status.
- **Arquivos alterados nesta atualização:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`,
  `docs/specs/031-redesign-visual-integral-google-health.md`,
  `docs/decisions/adr-028-redesign-visual-integral-google-health.md` e este
  arquivo. Alterações prévias de navegação/calendário permanecem preservadas.
- **Comandos e resultados desta atualização:** `git diff --check` passou;
  `./gradlew app:compileDebugKotlin --no-daemon` passou após correções de import
  e helper de distância; `./gradlew lint assembleDebug assembleRelease
  --no-daemon` passou; `./gradlew installDebug --no-daemon` passou e instalou em
  um dispositivo. A validação ADB foi somente leitura e não iniciou importação,
  sincronização, medição ou exclusão. Uma execução do build completo excedeu
  `120000 ms`, mas a repetição com `240000 ms` passou.
- **Revisão independente:** ainda pendente; a subtarefa permanece `in_progress`
  e o item do overview permanece `[ ]`.
- **Riscos residuais:** smoke visual da lista mensal com dados representativos,
  TalkBack, fonte ampliada, landscape e temas ainda não foi concluído. O arquivo
  estrutural temporário ADB usado no smoke foi removido do aparelho.
 - **Atualização de diretriz:** o calendário foi reintegrado à própria aba `Mês`
  de Exercícios, antes do resumo/lista semanal, usando somente `WorkoutActivity`.
  Setas de mês e semana atualizam período, calendário/gráfico e lista. Não há
  botão `Abrir dia`; `CalendarScreen` continua somente compatibilidade de rota
  legada/restaurada, sem CTA principal.
- **Validação adicional:** após essa alteração, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. `git diff --check` passou.
  Smoke ADB abriu a variante Debug e confirmou dispositivo conectado; nenhum
  fluxo operacional foi iniciado. Dump temporário foi removido.
- **Atualização final de escopo:** calendário passou a usar somente dados de
  `WorkoutActivity` em Exercícios. Estado temporal local controla mês e semana;
  setas de mês atualizam calendário/resumo/lista e setas de semana atualizam
  gráfico/lista. Integração `CalendarViewModel` e callback de navegação para
  calendário/dia foram removidos de Exercícios; botão `Abrir dia` não existe.
- **Validação final registrada:** `git diff --check`, `./gradlew lint
  assembleDebug assembleRelease --no-daemon` e `./gradlew installDebug
  --no-daemon` passaram após o ajuste. Dispositivo ADB permaneceu conectado;
  não houve importação, sincronização, medição ou exclusão.
- **Restrição temporal:** avanço semanal e mensal foi bloqueado nos períodos
  civis atuais, tanto nos callbacks quanto no estado habilitado das setas;
  semanas e meses futuros não podem ser selecionados.
- **Validação da restrição:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon` e `./gradlew lint assembleDebug
  assembleRelease --no-daemon` passaram após bloquear os períodos futuros.
- **Seleção visual:** células do calendário mensal permitem selecionar um dia
  e expõem estado acessível, sem botão `Abrir dia` ou navegação adicional.
- **Validação após seleção:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon`, `./gradlew lint assembleDebug
  assembleRelease --no-daemon` e `./gradlew installDebug --no-daemon` passaram.
- **Correção da revisão:** grupos mensais agora preservam a semana civil que
  começa no mês anterior quando ela contém exercícios do mês selecionado; a
  primeira semana parcial não desaparece da lista.
- **Validação da correção:** `git diff --check`, compile Debug, lint, assemble
  Debug/Release e `installDebug` passaram novamente. Revisão independente
  anterior identificou esse caso; nova revisão ainda é necessária.
- **Correção de restauração:** mês e semana persistidos/restaurados agora são
  normalizados para no máximo os períodos civis atuais; estado futuro não pode
  reaparecer após recriação da tela.
- **Validação final da normalização:** compile Debug, lint, assemble
  Debug/Release, `installDebug` e `git diff --check` passaram novamente.
- **Correção de restauração do filtro:** ao restaurar a tela em `Mês`, filtro de
  status restrito é normalizado para `Todas`, preservando calendário, métricas e
  agregados com todos os exercícios elegíveis.
- **Validação final do filtro:** compile Debug, lint, assemble Debug/Release,
  `installDebug` e `git diff --check` passaram novamente. Revisão independente
  identificou o caso; nova revisão permanece pendente.
- **Validação final do aparelho:** após reconexão, `./gradlew installDebug
  --no-daemon` passou; variante Debug iniciou sem crash e o processo
  `br.com.mwconsulting.championcompanion.dev` permaneceu ativo.

## Atualização de escopo — 2026-09-09

- **Diretriz do usuário:** as informações e os dados exibidos no detalhe devem
  permanecer iguais; o enriquecimento deve ocorrer somente pela adição de
  gráficos.
- **Inspeção física da referência:** a atividade foi aberta pelo fluxo
  `Exercícios` da referência, em sessão somente leitura. Foram confirmados
  barra com voltar e mais opções, título/tipo, data/hora/origem, estatísticas
  `Duração`, `Distância`, `Etapas`, `Ritmo médio` e `Frequência cardíaca média`,
  seção `Faixas de frequência cardíaca` e gráfico cardíaco acessível. Nenhum
  valor da referência foi reutilizado; a referência não recebeu alterações.
- **Implementação:** o detalhe do Champion preserva composição, informações,
  métricas, estados e ações existentes. Foram adicionados somente gráfico de
  ritmo por intervalos canônicos reais e gráfico de parciais por durações
  persistidas; cadência, frequência cardíaca, intervalos e lista de parciais
  continuam presentes. Lacunas e ausência segura permanecem explícitas.
- **Arquivos alterados nesta atualização:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  e este arquivo. Alterações não relacionadas pré-existentes foram preservadas.
- **Comandos e resultados:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon`, `./gradlew lint assembleDebug
  assembleRelease --no-daemon` e `./gradlew installDebug --no-daemon` passaram.
  Smoke ADB sanitizado abriu detalhe com atividade local existente e confirmou
  os dados anteriores, além das seções `Ritmo`, `Cadência D4`, `Frequência
  cardíaca`, `Splits e BPM` e semântica acessível do gráfico de parciais. Não
  houve importação, sincronização, medição, exclusão ou alteração de dados.
- **Privacidade e restauração:** temporários XML usados na inspeção estrutural
  foram removidos do aparelho e a ausência foi confirmada. A referência foi
  deixada na Home; o Champion permaneceu instalado na variante Debug.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md`.
- **Revisão independente:** permanece pendente; o item do overview continua
  `[ ]` e esta subtarefa permanece `in_progress`.
- **Riscos residuais:** inspeção visual completa em fonte ampliada, landscape,
  temas e TalkBack continua pendente; referência física foi observada em
  portrait/tema escuro e os dados representativos do Champion não são
  versionados.

## Atualização posterior de diretriz — 2026-09-09

- **Correção de direção:** a composição do detalhe deve seguir literalmente o
  detalhe de atividade observado em Google Health/Fitbit, e não uma tela de
  diagnóstico enriquecida. A alteração posterior substituiu a composição
  anterior por header simples, título do tipo, data/hora/origem, cinco métricas
  sem cards, `Faixas de frequência cardíaca`, média cardíaca e gráfico cardíaco.
- **Gráfico compacto:** o gráfico cardíaco usa altura de `160 dp`, para manter
  detalhe completo visível na viewport do usuário; continua usando somente
  amostras persistidas, duração real e semântica acessível. Gráficos de série
  canônica, ritmo, cadência e parciais não são renderizados nesta etapa; ficam
  para decisão posterior.
- **Preservação funcional:** revisão, calibração, descarte, exclusão, retry,
  estados e sincronização Health Connect permanecem disponíveis após a área
  visual de referência, com `Mais opções` no header.
- **Arquivos alterados nesta atualização:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  e este arquivo. Nenhum contrato, ViewModel, persistência, schema, parser,
  BLE, GPS ou navegação foi alterado.
- **Comandos e resultados:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon` e `./gradlew lint assembleDebug
  assembleRelease --no-daemon` passaram. `./gradlew installDebug --no-daemon`
  não pôde ser concluído nesta tentativa porque `adb devices -l` retornou zero
  dispositivos conectados; smoke físico final fica bloqueado até reconexão.
- **Status:** permanece `in_progress`; revisão independente e smoke físico
  após instalação ainda pendentes.

## Correções após revisão — 2026-09-09

- **Correções aplicadas:** header agora usa tipo da atividade e mantém
  `backLabel` na acessibilidade; detalhe não renderiza intervalos/parciais na
  composição Google Health; etapas phone só aparecem quando `stepsAvailable` é
  verdadeiro; distância/etapas canônicas só aparecem em estado `AVAILABLE`; a
  seção de FC é omitida sem amostras reais; o gráfico força `minimumHeight` de
  `160 dp` e não habilita rolagem horizontal, mantendo série inteira na
  viewport compacta.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  e este arquivo. Contratos, ViewModel, persistência, schema, parser, BLE, GPS
  e navegação permanecem sem alteração nesta correção.
- **Revisão independente:** a primeira revisão reprovou por cinco pontos de
  fidelidade/segurança de dados; todos foram corrigidos. Nova revisão permanece
  necessária.
- **Comandos e resultados:** `git diff --check`, `./gradlew
  app:compileDebugKotlin --no-daemon` e `./gradlew lint assembleDebug
  assembleRelease --no-daemon` passaram após as correções. `installDebug` e
  smoke físico continuam bloqueados: `adb devices -l` não lista dispositivo.
- **Riscos residuais:** validação visual em aparelho, fonte ampliada,
  landscape, temas e TalkBack ainda não foi executada após o ajuste compacto.

## Segunda correção após revisão — 2026-09-09

- **Correções adicionais:** seção de frequência cardíaca agora é totalmente
  omitida sem amostras; ritmo de atividade do relógio só usa métrica canônica
  quando o estado é `AVAILABLE`; metadata exibe origem real da atividade em vez
  de origem fixa. Intervalos e parciais não são chamados pela composição fiel.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon` e `./gradlew lint assembleDebug assembleRelease --no-daemon`
  passaram. `installDebug`/smoke físico continuam bloqueados porque `adb
  devices -l` não lista dispositivo.
- **Revisão:** segunda revisão independente encontrou esses três pontos; foram
  corrigidos. Nova revisão independente permanece necessária antes de concluir.

## Revisão final — 2026-09-09

- **Correção final:** `displayDistanceMetersOrNull()` agora também exige
  `canonicalReadStatus == AVAILABLE` para distância de atividades do relógio,
  evitando exposição de métrica canônica bloqueada em qualquer consumidor.
- **Resultado:** revisão independente final aprovada sem bloqueios de código.
  Header, cinco métricas sem cards, origem real, omissão segura, gráfico
  compacto de `160 dp`, ausência de rolagem horizontal e preservação dos
  callbacks foram confirmados.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon` e `./gradlew lint assembleDebug assembleRelease --no-daemon`
  passaram após o patch final. `installDebug` e smoke físico seguem bloqueados
  por `adb devices -l` sem dispositivo conectado.
- **Status:** subtarefa permanece `blocked`; `installDebug` ainda não pode ser
  executado porque o ADB não enxerga dispositivo. Não marcar overview até
  instalação e smoke físico com fonte ampliada, landscape, temas e TalkBack.

## Bloqueio de validação — 2026-09-09

- **Bloqueio:** após o usuário informar reconexão, nova tentativa de
  `./gradlew installDebug --no-daemon` falhou com `No connected devices`; nova
  execução de `adb devices -l` retornou somente o cabeçalho, sem dispositivo.
- **Impacto:** APK está compilado, mas smoke físico do detalhe compacto não pode
  ser confirmado nesta sessão. Nenhum dado do aparelho foi alterado.
- **Próximo desbloqueio:** reconectar dispositivo ADB de forma verificável e
  repetir instalação, abertura do detalhe, fonte ampliada, landscape, temas e
  TalkBack.

## Smoke físico após reconexão — 2026-09-09

- **Instalação:** `adb devices -l` identificou `SM-S901E`; `./gradlew
  installDebug --no-daemon` passou e instalou APK Debug.
- **Fluxo validado:** Home → `Exercícios` → semana → atividade. Dump estrutural
  confirmou header `Corrida`, métricas `Duração`, `Distância`, `Etapas`, `Ritmo
  médio` e `Frequência cardíaca média`, seção `Faixas de frequência cardíaca`,
  resumo e gráfico acessível. Área do gráfico ficou dentro da viewport
  compacta (`160 dp`); nenhum gráfico de série, cadência ou parciais foi
  renderizado.
- **Dados/evidência:** atividade local exibiu duração `42 minutos 22 segundos`,
  distância `5,50 km`, etapas `6.240`, ritmo `7 min 42 s` e FC `159 bpm`, todos
  provenientes do estado persistido. Processo permaneceu ativo sem crash.
- **Limpeza:** XML temporários foram removidos do aparelho e a ausência foi
  confirmada. Nenhuma importação, sincronização, medição, exclusão ou alteração
  de dados ocorreu.
- **Pendências:** validação com fonte ampliada, landscape, temas e TalkBack
  ainda não executada; revisão independente de código permanece aprovada.

## Ajuste restrito ao gráfico — 2026-09-09

- **Correção de escopo:** após comparação por screenshot, alterações de header,
  metadata visual, espaçamentos e demais composição foram revertidas. A
  formatação compacta dos dados (`42 min 22 s`, `7 min 42 s`) foi mantida,
  conforme confirmação do usuário.
- **Gráfico:** somente gráfico cardíaco foi alterado. Implementação agora usa
  Canvas compacto de `128 dp`, escala fixa inspirada na referência (`204`,
  `141`, `77`), labels laterais, linhas pontilhadas de zonas, linha por zona
  colorida, pontos de eixo e amostras persistidas. Não habilita rolagem.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram após o ajuste. Smoke estrutural
  anterior confirmou dados, acessibilidade e processo ativo; screenshot desta
  última troca visual ainda precisa ser revisado após navegação física.
- **Status:** permanece `in_progress`; overview continua com `031-07` em `[ ]`.

## Gráfico baseado no período D — 2026-09-09

- **Diretriz confirmada:** a referência correta é o gráfico de `Frequência
  cardíaca`, período `D`, já existente no próprio Champion Companion. Não usar
  gráfico da referência externa nem alterar header, métricas ou metadata.
- **Implementação:** o detalhe reutiliza `GoogleHealthIntradayHeartRateChart`
  existente, preservando bandas, zonas, escala diária, acessibilidade e
  dimensionamento. Novo parâmetro `showLine` troca somente pontos por linha
  contínua com amostras reais do treino; tela de Saúde mantém comportamento
  anterior com pontos.
- **Smoke:** detalhe abriu após instalação e dump confirmou métricas, resumo e
  semântica do gráfico com `44` amostras reais, duração `42 minutos 22 segundos`
  e zonas visuais. Temporários foram removidos do aparelho; processo permaneceu
  ativo sem crash.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram.

## Estado final do gráfico — 2026-09-09

- **Correção temporal:** detalhe usa `timeWindowSeconds` igual à duração real
  da atividade, com pontos relativos ao início da sessão; não usa janela de 24 h.
- **Comparação Vico:** gráfico Vico foi adicionado temporariamente abaixo para
  comparação e removido a pedido do usuário. Permanece somente gráfico baseado
  no período D de Frequência cardíaca, com pontos substituídos por linha.
- **Arquivos alterados:** `ExercisesScreen.kt` e `HeartRateHistoryChart.kt`.
  Demais superfícies permanecem sem alteração nesta rodada.
- **Validação final:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Smoke confirmou `44` amostras
  reais, duração `42 min 22 s`, gráfico acessível e processo ativo.
- **Status:** permanece `in_progress` até revisão independente e validação final
  do estado instalado; overview mantém `031-07` como `[ ]` e `6/11`.

## Remoção do comparativo Vico — 2026-09-09

- **Diretriz:** comparativo Vico removido do detalhe. Permanece somente gráfico
  intraday do período D, agora com linha contínua e eixo limitado à duração real
  da atividade.
- **Arquivos alterados:** `ExercisesScreen.kt` e `HeartRateHistoryChart.kt`;
  nenhuma alteração adicional de layout ou dados.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Comparativo Vico não aparece
  mais na composição do detalhe.

## Simplificação de status — 2026-09-09

- **Diretriz:** bloco visual `Status e origem` removido do detalhe. Header agora
  concentra `data • hora • origem • status`; demais ações operacionais continuam
  disponíveis sem o bloco diagnóstico.
- **Validação:** `git diff --check`, compile Debug, lint, assemble Debug/Release
  e `installDebug` passaram após a alteração. Revisão independente aprovou o
  estado sem blockers; smoke estrutural anterior confirmou gráfico, dados reais
  e processo ativo.
- **Status:** implementação parcial; revisão independente aprovada sem blockers,
  mas fechamento aguardando novas instruções do usuário.

## Fechamento — 2026-09-09

- **Arquivos alterados nesta execução:** `ExercisesScreen.kt`,
  `HeartRateHistoryChart.kt`, overview e esta subtarefa.
- **Decisão:** removido bloco morto `ActivityStatusOriginSection`/`ActivityBadge`;
  status permanece apenas na metadata do header após data, hora e origem.
- **Comandos:** `git diff --check`; `./gradlew app:compileDebugKotlin
  --no-daemon`; `./gradlew lint assembleDebug assembleRelease --no-daemon`; e
  `./gradlew installDebug --no-daemon`.
- **Resultado:** todos passaram; revisão independente aprovou sem blockers.
- **Riscos residuais:** smoke visual independente pós-última alteração não foi
  repetido; processo de build/instalação validado.
- **Comparação adicional:** não foi adicionado segundo gráfico porque “gráfico
  do vico” não identifica componente existente; aguarda nome/caminho exato para
  evitar nova alteração fora do escopo.

## Correção temporal do gráfico — 2026-09-09

- **Diretriz:** eixo X do detalhe representa duração da atividade, não janela
  fixa de 24 horas.
- **Implementação:** `GoogleHealthIntradayHeartRateChart` mantém período D como
  visual-base, mas aceita `timeWindowSeconds`. Detalhe envia duração persistida
  da atividade e pontos relativos ao início da atividade; Saúde continua usando
  janela diária de 24 horas. Linha permanece exclusiva do detalhe via
  `showLine = true`.
- **Arquivos alterados:**
  `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  e `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram. Segundo smoke visual ainda
  depende de abrir detalhe após esta correção; nenhum dado operacional foi
  alterado.

## Período D com duração da atividade — 2026-09-09

- **Diretriz final:** referência é gráfico de `Frequência cardíaca`, período
  `D`, do próprio app; eixo deve representar duração da atividade, não 24 horas.
- **Implementação:** componente intraday existente foi reutilizado com
  `showLine = true` e `timeWindowSeconds` igual à duração persistida. Saúde
  mantém pontos e janela de 24 horas; detalhe usa mesma composição visual, com
  linha contínua.
- **Smoke físico:** após instalação, detalhe foi aberto pelo fluxo
  `Exercícios` e screenshot confirmou bandas Z1–Z4, linhas pontilhadas, linha
  contínua ocupando largura do gráfico e dados da atividade. Dump confirmou
  duração `42 min 22 s`, `44` amostras reais e sem crash. Temporários foram
  removidos do aparelho.
- **Validação:** `git diff --check`, `./gradlew app:compileDebugKotlin
  --no-daemon`, `./gradlew lint assembleDebug assembleRelease --no-daemon` e
  `./gradlew installDebug --no-daemon` passaram.
- **Comparação:** segundo gráfico não foi adicionado; “vico” permanece sem
  referência identificável. Aguardando caminho/nome exato caso comparação ainda
  seja necessária.
