# 015-03 — Definir contratos, datas e estado temporal

- **Status:** pending
- **Objetivo:** estabilizar modelos e contratos antes de tocar Room ou UI.
- **Resultado esperado:** modelos imutáveis, APIs, estados e intents prontos para
  implementação independente.

## Documentos obrigatórios

Spec 015, 015-01/02, `RepositoryContracts.kt`, `DomainTime.kt`, modelos de
DailySummary/BPM/atividade/peso/sync, contratos de Saúde e Exercícios, ADRs
relevantes e baseline de banco.

## Dependências e entradas

- 015-02 concluída.
- Contratos atuais e intervalo inclusivo de resumo auditados.

## Arquivos permitidos

`core/model` para modelos/contratos; `feature/calendar/CalendarFeatureContract.kt`
para estado e intents; documentação da task e overview.

## Arquivos proibidos

DAOs, entities, mappers, `RoomRepositories`, `RepositoryModule`, telas Compose,
navegação, migrations, testes, BLE e Health Connect.

## Escopo

Definir `CalendarRepository`, queries de mês/dia/histórico, snapshots, estados
de ausência/loading/erro, semântica `[fromInclusive, untilExclusive)`, ZoneId,
seleção de dispositivo, resumos de BPM/atividade/peso/sync e separação entre
ausência e zero. APIs antigas devem permanecer compatíveis.

## Fora do escopo

Implementar consultas, composição de Flows, UI, navegação, gráficos ou alterar
dados persistidos.

## Passos

1. Modelar entradas e saídas sem entities.
2. Definir invariantes e estados seguros.
3. Definir datas por aggregate.
4. Definir janelas de 7/30/90 dias.
5. Documentar comportamento sem dados e falha parcial.
6. Atualizar evidências desta task.

## Critérios de aceite

- `observeMonth(query)` e `observeDay(localDate, zoneId)` retornam modelos.
- Contratos de histórico suportam 7, 30 e 90 dias.
- Semântica de data é exata e testável manualmente por inspeção.
- BPM exclui amostras de atividades.
- Passos/distância não recebem atividades.
- Sync é derivado da fila local.
- `CalendarDayPresence` cobre passos, BPM, atividades, peso e pendências/falhas.
- `CalendarDaySummary` cobre resumo, meta, BPM, atividades, peso e sync.
- Datas seguem `key.localDate`, `measuredAt`, `startedAt` e `syncedAt` proibido
  como data do aggregate.
- Estado inclui `selectedMonth`, `selectedDate`, snapshots, loadings e erro.

## Validação manual

Revisar modelos contra spec e símbolos existentes. Screenshot: não aplicável;
task de domínio/contrato.

## Comandos de build

Durante execução futura: `./gradlew lint assembleDebug assembleRelease` e
`./gradlew installDebug`. Não executar testes.

## Evidências e atualização

Registrar arquivos e símbolos alterados, decisões de compatibilidade e riscos.
Atualizar item correspondente do overview após aceite.

## Validação final

Confirmar ausência de entity na UI, ausência de operação externa e nenhum schema
novo. Não criar, alterar ou executar testes automatizados.

## Contexto

Contratos atuais não possuem CalendarRepository, read model diário ou histórico
temporal de atividades e pesos exposto pela camada de domínio.

## Arquivos alterados

Inicialmente nenhum; registrar arquivos de domínio e contrato alterados durante
execução.

## Decisões e desvios

Registrar decisões de compatibilidade da API inclusiva e qualquer desvio aprovado.

## Screenshot

Não aplicável; task de domínio e contrato.

## Riscos residuais

Consumidores antigos podem manter semântica inclusiva e exigir adaptação aditiva.

## Atualização do overview

Marcar somente o item 015-03 após aceite de 015-02.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
