# 015-04 — Adicionar queries temporais e projeções de repository

- **Status:** pending
- **Objetivo:** fornecer consultas locais eficientes para o read model.
- **Resultado esperado:** intervalos semiabertos, projeção leve de atividades,
  pesos temporais e consulta em lote de sync, sem migration.

## Documentos obrigatórios

Spec 015, 015-03, `ChampionDaos.kt`, `ChampionDatabase.kt`, `DatabaseMappers.kt`,
`RoomRepositories.kt`, `RepositoryContracts.kt`, schemas v5 e baseline de banco.

## Dependências e entradas

- 015-03 concluída.
- Room v5 e queries existentes preservadas.

## Arquivos permitidos

`core/database/ChampionDaos.kt`, `core/model/RepositoryContracts.kt`,
`data/DatabaseMappers.kt`, `data/RoomRepositories.kt`, documentação e overview.

## Arquivos proibidos

Entidades/schema/migration sem necessidade demonstrada, telas, ViewModels,
navegação, ADR, roadmap, testes, BLE e Health Connect.

## Escopo

Adicionar queries de resumo válido em janela semiaberta; atividades por
`startedAt`, excluindo `DISCARDED`, com projeção leve; pesos por `measuredAt`;
heart rate reutilizando fontes manual/D1; sync em lote por aggregates e tipos;
adaptação compatível da API inclusiva existente; chunking quando necessário;
prevenção de N+1.

## Fora do escopo

Tabela nova, migration, índice preventivo, read model completo, UI, BLE,
Health Connect, mudança de workflow e soma de dispositivos.

## Passos

1. Mapear cada janela para colunas persistidas.
2. Implementar consultas em lote e filtros de validade.
3. Manter APIs existentes compatíveis.
4. Criar mappers de projeção somente se necessários.
5. Confirmar que não há alteração de schema.
6. Registrar desempenho esperado e limitações.

## Critérios de aceite

- Toda API nova usa `[fromInclusive, untilExclusive)`.
- Resumos inválidos são excluídos.
- Atividades descartadas são excluídas por padrão.
- Sync não gera consulta por aggregate individual.
- Mappers não alteram entidades existentes.
- Migration prevista permanece nenhuma.
- Atividades usam `startedAt` em janela semiaberta.
- Pesos usam `measuredAt` em janela semiaberta.
- Consulta de sync aceita `aggregateType` e `aggregateIds` em lote.
- Resumos filtram `VALID` e preservam origem/última atualização.
- BPM aceita somente fontes manual e D1.

## Validação manual

Inspecionar SQL, logs de execução locais permitidos e schema exportado sem
alteração. Screenshot: não aplicável; task de persistência.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar queries, arquivos alterados, contagem esperada de operações e
confirmação de schema inalterado. Atualizar overview após aceite.

## Validação final

Confirmar ausência de N+1 evidente, ausência de provider e ausência de
alteração de Room schema. Não criar, alterar ou executar testes automatizados.

## Contexto

Queries atuais divergem em intervalos e atividades/pesos/sync ainda não oferecem
as consultas em lote exigidas pelo read model.

## Arquivos alterados

Inicialmente nenhum; registrar DAOs, contratos, mappers e repositories alterados.

## Decisões e desvios

Registrar preservação da API inclusiva, chunking e qualquer desvio aprovado.

## Screenshot

Não aplicável; task de persistência.

## Riscos residuais

Histórico grande pode sofrer degradação sem índices dedicados; não adicionar
índice preventivamente.

## Atualização do overview

Marcar somente o item 015-04 após aceite de 015-03.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
