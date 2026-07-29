# 015-05 — Implementar read model e consolidação diária

- **Status:** concluída em 28/07/2026.
- **Objetivo:** compor fontes locais em snapshots mensais, diários e históricos.
- **Resultado esperado:** `LocalCalendarRepository` read-only, com política de
  datas, dispositivos, agregados e estados de sync centralizada.

## Documentos obrigatórios

Spec 015, 015-03/04, ADR-003/009/010/011/012, contratos atuais, baseline e
implementações de Room/DataStore.

## Dependências e entradas

- 015-04 concluída.
- Queries temporais estabilizadas.

## Arquivos permitidos

Novos arquivos de repository/read model em `data/` e `core/model`,
`RepositoryModule.kt`, documentação e overview.

## Arquivos proibidos

UI Compose, DAOs adicionais fora do contrato 015-04, navigation, BLE,
Health Connect, migrations, testes e alteração de workflow.

## Escopo

Implementar composição local por janela; seleção de resumo associado/fallback;
agrupamento temporal; BPM manual/D1; atividades não descartadas; pesos;
distância e passos derivados de DailySummary; status da fila local; snapshots
de ausência/falha parcial; observação eficiente e cancelável.

## Fora do escopo

Sincronizar, importar, editar, escrever provider, materializar tabela, somar
atividades, histórico completo de peso e nova coleta.

## Passos

1. Implementar repository separado de `RoomRepositories` quando adequado.
2. Montar janela única por consulta.
3. Observar fontes em lote.
4. Aplicar seleção de dispositivos e datas.
5. Associar operações sync por aggregate, sem `syncedAt`.
6. Produzir snapshots imutáveis.
7. Integrar Hilt.

## Critérios de aceite

- UI receberia modelos consolidados, nunca entities.
- Cada dia tem no máximo um resumo selecionado.
- Dados ausentes não viram zero.
- BPM de atividade não entra em BPM diário.
- Distância e passos de atividade não entram nos totais.
- Sync usa somente fila local.
- Observação não inicia operação externa.
- Resumo diário expõe origem e última atualização.
- Card de atividade usa distância calibrada, original, estimada ou indisponível.
- Peso mais recente resume o dia e demais medidas permanecem ordenadas.
- Sync expõe `pendingCount`, `syncingCount`, `failedCount` e `syncedCount`.
- Atividades expõem título/tipo, início, duração, distância, calorias, BPM,
  workflow e status de sync.
- Operação órfã ou aggregate ausente não é associado por `syncedAt`.

## Validação manual

Com dados locais reais, revisar snapshots para dias com dados, ausência, zero,
conflito entre dispositivos e atividade cruzando meia-noite. Screenshot: não
aplicável; repository.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar arquivos, composição de fontes, política de desempate e riscos de
performance. Atualizar overview após aceite.

## Validação final

Confirmar read-only, sem BLE/Health Connect, sem tabela e sem N+1 por atividade
ou operação de sync. Não criar, alterar ou executar testes automatizados.

## Contexto

As fontes locais existem separadamente e precisam de uma política única de
datas, dispositivos, ausência, agregação e status de fila.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- este arquivo.

## Decisões e desvios

- `LocalCalendarRepository` foi mantido separado de `RoomRepositories` e compõe
  somente contratos locais existentes com `AppPreferencesRepository`.
- Cada consulta congela `ZoneId` e deriva uma única janela semiaberta para as
  fontes baseadas em `Instant`.
- A seleção de resumo prefere o dispositivo associado e usa, como fallback,
  `updatedAt` decrescente, `collectedAt` decrescente e ID estável crescente.
  Resumos de dispositivos diferentes nunca são somados.
- Operações de sincronização são observadas uma vez, em lote, para o conjunto de
  aggregates presentes. A data é resolvida pelo aggregate local, nunca por
  `syncedAt`.
- Falhas de fontes são isoladas em `CalendarDataIssue`; cancelamento de
  coroutines é preservado e mudanças do conjunto de aggregates cancelam a
  observação anterior com `flatMapLatest`.
- Valores ausentes permanecem ausentes. Zero persistido permanece dado real.
  BPM usa somente medição manual e histórico D1; passos e distância usam somente
  `DailySummary`.
- Atividades e pesos recebem status de sincronização por aggregate. Atividades
  ficam ordenadas por início e pesos da medida mais recente para a mais antiga.
- Nenhum desvio aprovado. Não houve mudança em DAO, entity, schema, migration,
  UI, navegação, BLE, Health Connect ou workflow.

## Comandos executados

- `./gradlew :app:compileDebugKotlin`;
- `./gradlew :app:lintDebug`;
- `./gradlew :app:assembleDebug :app:assembleRelease`;
- `./gradlew lint assembleDebug assembleRelease`;
- `./gradlew installDebug`;
- `git diff --check`.

Não foram criados, alterados ou executados testes automatizados, conforme regra
da spec e desta subtarefa.

## Resultados e evidências

- `:app:compileDebugKotlin` concluiu com `BUILD SUCCESSFUL` em 14 segundos.
- `:app:lintDebug` concluiu com `BUILD SUCCESSFUL` em 1 minuto.
- Assemblies debug e release concluíram com `BUILD SUCCESSFUL` em 1 minuto e
  4 segundos.
- O gate completo `lint assembleDebug assembleRelease` concluiu com
  `BUILD SUCCESSFUL`; 101 tasks, 1 executada e 100 `UP-TO-DATE`.
- `installDebug` concluiu com `BUILD SUCCESSFUL` em 11 segundos e instalou o APK
  no dispositivo `SM-S901E`.
- Revisão independente confirmou modelos consolidados sem entities, seleção
  determinística de no máximo um resumo por data, agrupamento por timezone,
  exclusão de BPM de atividade, métricas diárias exclusivas de `DailySummary`,
  ordenação de pesos, prioridade de distância da atividade e associação local
  da fila em lote.
- `git diff --check` concluiu sem erros.

## Screenshot

Não aplicável; task de read model.

## Riscos residuais

- Combinações de `Flow` podem emitir consistência transitória entre commits
  relacionados; cada fonte falha de forma isolada e explícita, sem mascarar a
  falha como vazio íntegro.
- Após uma exceção de fonte, a emissão degradada permanece até nova assinatura
  do fluxo, que será controlada pelo retry do ViewModel na subtarefa 015-06.
- Consultas históricas grandes continuam sem índices preventivos; desempenho
  deve ser observado antes de qualquer decisão de schema.

## Atualização do overview

Item `015-05` marcado `[x]`; progresso recalculado para `5/13`. Demais itens
permanecem `[ ]` e nenhuma subtarefa seguinte foi iniciada.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
