# 038-02 — Implementar preflight de recuperação

- **Ticker:** `038`
- **Número:** `02`
- **Status:** `pending`

## Objetivo

Disponibilizar, somente se Gate A autorizar uma classe concreta, classificação
read-only determinística antes de qualquer mutação local.

## Resultado esperado

Preflight retorna uma decisão segura que diferencia:

- já comparável (`NATIVO_ATUAL`);
- recuperável e mutável por ação explícita;
- não recuperável com motivo;
- bloqueado por estado remoto com fonte recuperável preservada;
- potencial legacy sem prova suficiente.

Em `NO-GO` ou `INCONCLUSIVO`, esta tarefa é encerrada como não aplicável, sem
criar camada ou código para preencher checklist.

## Requisitos cobertos

- Predicado congelado em 038-01/ADR-038.
- Leitura de parser/import, payload candidato, header, fatos nativos,
  temporalidade, plausibilidade, snapshot, calibração, workflow e projeções HC.
- Zero escrita e zero Health Connect.
- Leitura bounded, seleção mínima e ausência de N+1.

## Escopo incluído

- Reusar contratos, DAOs e validadores existentes quando forem semanticamente
  suficientes.
- Expor motivo seguro e provenance original.
- Separar `sourceAssessment` de `mutationDisposition`.
- Tratar `SYNCING`, `SYNCED`, children inseguros e workflows não autorizados.

## Escopo excluído

- Criar snapshot, derived intervals, splits, atualização de atividade ou
  workflow.
- Alterar `parserVersion`, schema, migration, parser, BLE ou Health Connect.
- Ler o provider Health Connect ou criar snapshot efêmero visível em 037.
- Batch obrigatório, reprocessamento automático ou preflight como efeito de
  abrir tela.

## Dependências

- Gate A `GO`/`PARTIAL-GO` com classe autorizada explicitamente.
- ADR-038 e contrato final de provenance legacy, se legacy for autorizado.
- Queries bounded existentes em `RoomRepositories`/`ChampionDaos`.

## Arquivos e símbolos prováveis

- `RepositoryContracts.kt`, `RoomRepositories.kt`, `ChampionDaos.kt`.
- `WatchWorkoutDerivedMetrics.kt`, especialmente policy nativa, temporalidade e
  plausibility.
- Entidades de import, intervalos, header, snapshots e operations HC.
- Contrato UI em `ExercisesFeatureContract.kt` somente se necessário.

## Passos de implementação

1. Confirmar classe autorizada e excluir todas as demais por default.
2. Montar consulta bounded da população/candidato sem hidratação completa.
3. Validar identity, parser/provenance, fatos, header/série, temporalidade,
   plausibility e calibração conforme Gate.
4. Avaliar workflow e projection children sem alterar guards.
5. Retornar motivo estável e seguro para cada saída.
6. Provar que a chamada não abre transação de escrita nem chama coordinator HC.
7. Em resultado negativo do Gate, registrar N/A e fechar tarefa sem código.

## Testes e comandos de validação

- Revisão estática de query, bounded loading, N+1 e ausência de chamadas de
  escrita/remotas.
- `git diff --check`.
- Validação física futura somente read-only sobre banco real autorizado.
- Nenhum teste automatizado criado ou executado sem pedido explícito.

## Definição de pronto

- Preflight não escreve nenhuma tabela e não altera estado em memória usado como
  snapshot ativo.
- Resultado cobre comparável, recuperável, inelegível e bloqueado por estado.
- Legacy sem provenance comprovada permanece potencial ou fallback.
- `SYNCED` e child inseguro continuam fail-closed.
- Não há N+1 ou reidratação completa não justificada.
- Em `NO-GO`/`INCONCLUSIVO`, tarefa consta como não aplicável e nenhum código
  é criado.

## Riscos e cuidados

- Não chamar `reprocessWatchWorkout` disfarçado de preflight.
- Não usar ordenação Room como prova de ordem física.
- Não relaxar `WatchWorkoutNativeDistancePolicy` para legacy sem decisão.
- Não retornar `V4_RECUPERAVEL_LOCALMENTE` quando somente header ou payload
  incompleto existe.
