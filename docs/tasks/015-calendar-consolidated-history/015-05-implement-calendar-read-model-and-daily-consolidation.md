# 015-05 — Implementar read model e consolidação diária

- **Status:** pending
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

Inicialmente nenhum; registrar novos modelos, repository e bindings DI alterados.

## Decisões e desvios

Registrar composição escolhida, desempates e qualquer desvio aprovado.

## Screenshot

Não aplicável; task de read model.

## Riscos residuais

Combinação de Flows pode mostrar estado transitório durante commits relacionados;
registrar degradação parcial sem mascará-la como vazio.

## Atualização do overview

Marcar somente o item 015-05 após aceite de 015-04.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
