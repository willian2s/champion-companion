# 015-08 — Implementar detalhe diário e navegação

- **Status:** pending
- **Objetivo:** entregar detalhe diário e fluxo para detalhes relacionados.
- **Resultado esperado:** `CalendarDay(localDateIso)` seguro, consolidado e com
  retorno correto ao calendário.

## Documentos obrigatórios

Spec 015, 015-07, `AppDestination.kt`, `ChampionNavigation.kt`,
`HealthMetricDetailScreens.kt`, `ExercisesScreen.kt`, `ExercisesViewModel.kt` e
Navigation3.

## Dependências e entradas

- 015-07 concluída.
- Read model e ViewModel temporal concluídos.

## Arquivos permitidos

`core/navigation`, `feature/calendar` e adaptadores mínimos de detalhes
existentes; documentação e screenshots.

## Arquivos proibidos

Nova tela duplicada de atividade, DAO, migration, alteração de workflow, BLE,
Health Connect, testes e roadmap/ADR.

## Escopo

Criar destino parametrizado, validar `LocalDate.parse`, mostrar passos,
distância, BPM, peso, atividades e sync, abrir detalhes relacionados e manter
stack `Calendar → CalendarDay → ExerciseDetail`. Corrigir retorno por pop,
preservando mês e data.

## Fora do escopo

Gerenciamento de peso, edição, sincronização, atividade iniciada pelo app,
mudança de ExerciseDetail ou coleta externa.

## Passos

1. Adicionar destino com parsing seguro.
2. Integrar subtree Calendar no back stack manual.
3. Implementar tela de detalhe e estados.
4. Ligar rotas de métricas existentes.
5. Reutilizar ExerciseDetail.
6. Validar back, inválido e restauração durante processo.

## Critérios de aceite

- Data inválida não derruba a navegação.
- Detalhe usa apenas snapshot local.
- Atividade abre detalhe existente.
- Back de atividade retorna ao mesmo dia.
- Back de dia retorna ao mesmo mês/data.
- O detalhe mostra passos, meta atual, distância estimada, BPM, atividades, peso
  e sync.
- Cards de atividade mostram título/tipo, início, duração, distância final,
  calorias, BPM médio, workflow e status de sync.
- Peso mais recente aparece como resumo e demais medidas ficam ordenadas.
- Aggregate ausente, operação órfã e degradação parcial têm estado seguro e não
  são apresentados como sucesso.

## Validação manual

Galaxy S22: abrir data com dados, sem dados, atividade, BPM, passos e distância;
usar back em cada nível. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar rotas, comportamento do stack, parsing inválido e screenshots.
Atualizar overview após aceite.

## Validação final

Confirmar que nenhuma tela de atividade foi duplicada e que nenhuma operação
externa começa ao abrir detalhe. Não criar, alterar ou executar testes
automatizados.

## Contexto

Navigation3 usa back stack manual e `ExerciseDetail` é compartilhado; o detalhe
diário precisa preservar a origem sem duplicar a tela de atividade.

## Arquivos alterados

Inicialmente nenhum; registrar destinos, navegação e telas de detalhe alterados.

## Decisões e desvios

Registrar parsing seguro, política de pop e qualquer desvio aprovado.

## Screenshot

Exigido. Registrar calendário, detalhe diário e detalhe de atividade relacionado.

## Riscos residuais

Classificação incorreta da subtree pode produzir retorno para destino órfão;
validar stack após trocar abas.

## Atualização do overview

Marcar somente o item 015-08 após aceite de 015-07.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
