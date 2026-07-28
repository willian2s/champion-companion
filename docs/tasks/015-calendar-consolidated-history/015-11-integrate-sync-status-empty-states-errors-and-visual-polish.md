# 015-11 — Integrar sync, estados, erros e acabamento visual

- **Status:** concluída em 28/07/2026; escopo revisado, integridade da fila
  corrigida no menor caminho read-only e gates de build concluídos.
- **Objetivo:** consolidar experiência final de calendário e históricos.
- **Resultado esperado:** estados coerentes, acessibilidade, privacidade,
  mensagens seguras e acabamento visual sem alterar fronteiras funcionais.

## Documentos obrigatórios

Spec 015, tasks 015-06 a 015-10, ADRs de privacidade/local-first, design system,
Health Connect spec e baseline visual.

## Dependências e entradas

- 015-08, 015-09 e 015-10 concluídas.

## Arquivos permitidos

Arquivos de UI/ViewModel temporal já pertencentes à Fase 15, recursos visuais,
documentação e screenshots. Para remover o bloqueio confirmado desta subtarefa,
também são permitidas mudanças aditivas mínimas em contrato de repository,
consulta Room somente leitura, implementação de repository e read model do
calendário, exclusivamente para diagnosticar integridade da fila e propagar
`CalendarDataIssue` seguro até `CalendarDaySummary`/UI.

## Arquivos proibidos

Novo sync, provider de sync, BLE, Health Connect, permissões, entities, schema,
migration, versão do banco, índice, escrita ou exclusão de dados, WorkManager,
foreground service, alteração de workflow, testes e roadmap/ADR.

## Escopo

Integrar `Sem operações`, `Pendente`, `Sincronizando`, `Sincronizado`, `Falha` e
`Parcial`; estados empty/loading/error/retry; mensagens sem dados sensíveis;
acessibilidade; consistência de unidades; espaçamento, contraste e hierarquia;
logs seguros e ausência de operação automática. Diagnosticar aggregate ausente,
operação órfã e registro inválido por leitura local, sem derivar data de
`syncedAt`, e degradar o status com `CalendarDataSource.HEALTH_SYNC_QUEUE` em vez
de presumir `Sem operações` ou estado completo.

## Fora do escopo

Corrigir provider, sincronizar fila, mudar dados, criar novos gráficos ou alterar
features fora do calendário/históricos.

## Passos

1. Revisar estados de todas as superfícies.
2. Integrar status local ao detalhe.
3. Padronizar empty/loading/error.
4. Revisar semantics, contraste e foco.
5. Revisar logs e privacidade.
6. Capturar screenshots finais.

## Critérios de aceite

- Status vem somente da fila local.
- Falha de sync não apaga dado local.
- Ausência, zero e erro permanecem distintos.
- Acessibilidade cobre controles, indicadores e listas.
- Nenhum log expõe métricas, IDs ou payloads.
- Sem operações produz `Sem operações`; sucesso combinado (`syncedCount > 0`)
  com qualquer pendência, sincronização ou falha produz `Parcial`.
- Sem sucesso combinado, falha produz `Falha`; sem falha, sincronizando produz
  `Sincronizando`; sem falha/sincronização, pendente produz `Pendente`; sucesso
  só aparece quando tudo está concluído.
- Aggregate ausente, operação órfã e degradação parcial não são mascarados.
- Acessibilidade e mensagens cobrem estados vazios, loading, erro e retry.

## Validação manual

Galaxy S22 offline, com cada estado local reproduzível; conferir leitor de tela,
contraste, screenshots e ausência de prompts/operações externas. Screenshot
exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar screenshots, estados revisados, logs permitidos e riscos. Atualizar
overview após aceite.

## Validação final

Confirmar acabamento sem refactor oportunista, sem provider e sem alteração de
escopo. Não criar, alterar ou executar testes automatizados.

## Contexto

As frentes visuais precisam de integração final sem transformar status local em
operação de sync nem mascarar ausência como sucesso.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryShell.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-screen.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-empty.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-day-empty.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-sync-status.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-sync-status-card-2.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-sync-summary.png`;
- `docs/tasks/015-calendar-consolidated-history/015-11-calendar-font-scale.png`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- este arquivo.

## Decisões e desvios

- Causa do escalonamento confirmada: detalhe diário precisava representar seis
  estados locais e degradação da fila, enquanto `SyncVisualState` global cobre
  somente pendência, sucesso e falha. A mudança também precisava preservar
  distinção entre vazio real e falha de fonte em duas superfícies históricas.
- Foi criado componente visual privado em `CalendarDayScreen`, evitando ampliar
  contrato global do design system e impacto em consumidores existentes. Ele usa
  pares de cores Material 3, indicador textual, `stateDescription` e região viva
  para `Sem operações`, `Pendente`, `Sincronizando`, `Sincronizado`, `Falha` e
  `Parcial` exatamente.
- Falha reportada pela fonte da fila produz `Status indisponível`, sem presumir
  sucesso. Cards de métricas permanecem visíveis; fila local nunca apaga dado
  válido e tela declara que não inicia sincronização.
- Históricos de passos e distância agora deixam de renderizar vazio, gráfico e
  lista quando fonte de resumo diário falha sem nenhum dado confirmado. Issues de
  outras fontes continuam coexistindo com vazio verdadeiro; registros reais com
  zero permanecem dados confirmados.
- Seletores de período expõem estado selecionado e cabeçalhos de paginação têm
  descrição unificada para leitor de tela. Listas mantêm data, unidade, origem,
  atualização e distinção entre lacuna e zero.
- A autorização inicial de UI foi ampliada pelo usuário após revisão
  independente confirmar bloqueio transversal: mudanças aditivas mínimas em
  contrato, consulta Room somente leitura, repository e read model passam a
  pertencer à própria `015-11`. Continuam proibidos schema, migration, escrita,
  exclusão, provider, BLE, Health Connect, permissão, workflow e operação
  automática.
- `HealthSyncRepository` ganhou `observeForAggregatesWithIntegrity` com
  implementação padrão sobre a API anterior. Consumidores e fakes existentes
  preservam compatibilidade; `observeForAggregates` continua disponível e com a
  mesma assinatura.
- `RoomRepositories` continua consultando operações pela API anterior em chunks
  de 500. A variante de integridade observa globalmente todas as operações e
  todos os candidatos de aggregate por seis queries reativas em lote, sem N+1,
  escrita, reparo ou exclusão.
- Validade estrutural não é mais reproduzida por predicados SQL. Resumos diários,
  medições de BPM, pesos e perfis passam pelos mappers `toDomain()` existentes;
  altura exige perfil mapeável com `heightCentimeters != null`. Atividades usam
  `CalendarWorkoutSummaryRecord.toCalendarSummary()`, mesma projeção leve do
  calendário, com vínculo de importação e exclusão de `DISCARDED` na query global.
- Cada operação passa por `HealthSyncOperationEntity.toDomain()`. Operação que
  não mapeia sinaliza dado local inválido; operação válida cujo aggregate e tipo
  não aparecem no conjunto estrutural global sinaliza aggregate não resolvido.
  Aggregate válido fora da janela observada não produz issue.
- `LocalCalendarRepository` converte diagnóstico em
  `CalendarDataIssue` de `HEALTH_SYNC_QUEUE`. Operação órfã não recebe data
  sintética e nunca usa `syncedAt`; enquanto integridade estiver degradada, UI
  mostra `Status indisponível` em vez de `Sem operações` ou sucesso incompleto.
- Diagnóstico é booleano e não expõe IDs, payloads, timestamps ou métricas.
  Queries adicionadas aos DAOs são somente leitura e reativas; a consulta SQL
  duplicada de integridade foi removida. Entities, banco v5 e schema exportado
  não mudaram. Não foi necessária nova decisão arquitetural ou ADR.

## Screenshot

Capturas realizadas no Galaxy S22 com Bluetooth desligado:

- `015-11-calendar-screen.png`: grade mensal com indicadores e seleção;
- `015-11-calendar-empty.png`: seleção de data sem registros;
- `015-11-calendar-day-empty.png`: detalhe diário vazio, sem valores sintéticos;
- `015-11-calendar-sync-status.png`: detalhe diário com dados locais;
- `015-11-calendar-sync-status-card-2.png`: card de atividade com status
  `Sincronizado`;
- `015-11-calendar-sync-summary.png`: fila local com status `Sincronizado`.
- `015-11-calendar-font-scale.png`: calendário em escala de fonte 1,3x;
  grade permanece utilizável e navegação continua acessível, mas rótulos da
  navegação inferior quebram em duas linhas.

Não foram reproduzidos `Pendente`, `Sincronizando`, `Falha`, `Parcial`, fila
indisponível por integridade, erro/retry dos históricos, TalkBack ou contraste
dinâmico. Escala de fonte 1,3x foi validada e restaurada para 1,0x. Usuário
aceitou cobertura manual representativa, sem exigir reprodução de todos os casos;
os itens não reproduzidos permanecem riscos residuais.

## Riscos residuais

- Contraste usa pares semânticos do esquema Material 3, mas ainda requer inspeção
  física em temas claro/escuro e com cores dinâmicas.
- TalkBack, foco, escala de fonte, rotação e estados não reproduzidos da fila
  ainda precisam de validação manual no Galaxy S22.
- Árvore UI Automator expôs controles da navegação e descrições acessíveis de
  dispositivo, perfil, cartões e navegação; foco de hardware/TalkBack não foi
  validado com leitor ativo.
- Dados locais disponíveis podem não reproduzir todos os estados da fila. Não
  criar operações nem alterar dados somente para produzir screenshots.
- Diagnóstico de integridade é global porque operação órfã não possui data local
  confiável. Enquanto houver degradação, todos os snapshots observados sinalizam
  fila indisponível; comportamento é conservador e evita associação por horário
  de sync.
- Leitura de integridade mapeia a fila e os cinco tipos de aggregate após
  invalidações das tabelas observadas. Custo é linear no volume local; monitorar
  antes de considerar índice ou materialização, ambos fora do escopo.
- Mappers de resumo diário e BPM podem lançar `DateTimeException` ao receber
  texto temporal persistido malformado. O diagnóstico converte somente essa
  falha de parsing em aggregate não resolvido; demais regras continuam
  pertencendo integralmente aos mappers autoritativos.

## Comandos executados

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew app:lintDebug`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`;
- `git diff --check`;
- `git diff -- app/schemas app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- buscas estáticas por logs e chamadas de BLE/Health Connect nos arquivos de UI
  alterados.

Testes automatizados não foram criados, alterados ou executados.

## Resultados e evidências

- `:app:compileDebugKotlin`: `BUILD SUCCESSFUL` após substituição do diagnóstico
  SQL pelos mappers/projeções, em 17 segundos.
- `app:lintDebug`: `BUILD SUCCESSFUL`.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` após a
  correção técnica final, em 2 minutos e 12 segundos; 103 tasks, 26 executadas e
  77 `UP-TO-DATE`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E` após a correção técnica final, em 19 segundos.
- `git diff --check`: sem erros.
- Diff de `app/schemas` e `ChampionDatabase.kt`: sem saída; banco permanece v5,
  sem mudança estrutural.
- Validação manual confirmou calendário, seleção, data sem registros, detalhe
  com dados locais e status `Sincronizado`, sem prompt ou operação externa
  observada após Bluetooth ser desligado.
- Escala 1,3x confirmou renderização da grade sem corte interno; navegação
  inferior quebrou alguns rótulos em duas linhas, sem impedir acesso aos itens.
- Escala do dispositivo foi restaurada para 1,0x após captura.
- Árvore UI Automator confirmou controles focáveis e descrições semânticas nos
  destinos de navegação e cartões observados.
- Busca estática: nenhum log, `HealthConnectClient`, gate BLE, criação de operação
  ou gatilho de sync introduzido nas superfícies alteradas.
- API anterior de fila e derivação de `CalendarSyncSummary` foram preservadas. A
  nova observação propaga `UNRESOLVED_AGGREGATE` ou `INVALID_LOCAL_DATA` até
  `CalendarDaySummary` sem ocultar dados locais válidos.
- Busca estática confirmou remoção de `observeUnresolvedAggregateCount()` e dos
  predicados de elegibilidade duplicados. Diff de `app/src/test` e
  `app/src/androidTest` permaneceu vazio.
- Nenhum teste automatizado foi criado, alterado ou executado. Gates executados
  não incluíram tasks de teste.

## Resultados da revisão independente

- Revisão independente reprovou aceite em 28/07/2026.
- Bloqueio alto: `LocalCalendarRepository`/`RoomHealthSyncRepository` não
  propagam aggregate ausente, operação órfã ou operação inválida; consulta é
  limitada a aggregates locais conhecidos e conversões inválidas são descartadas.
- Segunda revisão independente encontrou bloqueio alto remanescente: consulta de
  integridade verificava somente existência da linha. Um `DailySummary` com
  `validity='INVALID'` resolvia a referência da operação, mas era excluído da
  projeção temporal e permitia mostrar `Sem operações` sem issue.
- Terceira revisão independente reprovou a tentativa de corrigir equivalência
  com SQL extenso: predicados duplicados ainda aceitavam campos estruturais
  malformados e impunham a atividades regras de sync que a projeção consumidora
  não exige. Causa definitiva foi duplicação das regras autoritativas dos
  mappers/projeções.
- Usuário aceitou cobertura manual representativa; não foram exigidos todos os
  estados reproduzíveis nem validação com leitor ativo. Escala 1,3x foi coberta
  com observação de quebra de rótulos na navegação inferior.
- Bloqueio médio: evidências de build completo estavam ausentes na documentação;
  comandos foram executados e registrados nesta atualização.

## Resposta ao bloqueio técnico

- Causa confirmada: `LocalCalendarRepository.observeWindow()` enviava apenas
  `aggregateDates.keys`, enquanto `RoomRepositories.observeForAggregates()`
  descartava silenciosamente entidades cujo mapper retornava `null`. A primeira
  correção ainda tratava qualquer linha referenciada como aggregate resolvido,
  sem aplicar validade/elegibilidade semântica da fonte.
- Correção concluída: observação aditiva retorna operações selecionadas e sinais
  seguros de operação inválida/aggregate não resolvido. Queries globais carregam
  candidatos em lote; `DatabaseMappers.kt` e
  `CalendarWorkoutSummaryRecord.toCalendarSummary()` formam os conjuntos válidos
  por tipo. A query SQL extensa foi removida, inclusive regras extras de
  `confirmedType`, parser e elegibilidade por record. Read model propaga issue e
  UI existente mostra `Status indisponível`.
- Proibições preservadas: nenhum sync foi iniciado, nenhum dado foi alterado ou
  apagado e não houve mudança em provider, BLE, Health Connect, permissões,
  schema, migration, WorkManager, workflow ou testes.

## Atualização do overview

Item `015-11` foi marcado `[x]` e progresso recalculado para `12/14` após
aceite explícito do usuário. Dependências `015-08`, `015-09` e `015-10` estão
concluídas. `015-12` não foi iniciada automaticamente.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
