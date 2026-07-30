# Spec 016 — Sincronização incremental de passos diários

## Status

Concluída. `016-01` implementada e revisada; `016-02` revisão independente finalizada.

## Ticker

`016`, escolhido como maior prefixo numérico existente (`015`) mais um.

## Contexto

O armazenamento local mantém um único `DailySummary` cumulativo por
`deviceId + origin + localDate`. A fila atual reutiliza uma operação e um
`StepsRecord` cumulativo, alterando versão e payload quando o resumo cresce.
Isso pode fazer o Health Connect exibir temporariamente zero durante upserts.

## Objetivo

Manter `DailySummary.steps` agregado e alterar exclusivamente a infraestrutura
de sincronização para enfileirar intervalos incrementais, imutáveis e
determinísticos. Cada aumento positivo deve cobrir somente diferença ainda não
enfileirada, sem sobreposição, duplicação ou mistura com o modelo cumulativo
legado.

## Requisitos funcionais

1. O modelo e os fluxos locais de `DailySummary` permanecem inalterados.
2. A fila persiste, por resumo e dia, checkpoint de último total enfileirado,
   instante final e fuso, ou deriva esses valores somente de operações
   incrementais persistidas.
3. A primeira operação cobre `0 -> currentTotal` desde o início do dia local
   até `collectedAt`; as seguintes começam no fim da última operação válida.
4. Cada operação congela `previousTotalSteps`, `currentTotalSteps`,
   `stepDelta`, `startTime`, `endTime`, `timeZoneId`, identidade e versão.
5. O mapper recebe a operação persistida e cria `StepsRecord` usando somente seu
   payload; nunca consulta `DailySummary.steps` para calcular o count.
6. `currentTotalSteps <= lastEnqueuedTotalSteps` não cria operação, não envia
   zero ou negativo e não reduz checkpoint.
7. `endTime` não posterior a `startTime` não cria operação nem avança
   checkpoint.
8. Atualização de checkpoint e criação da operação ocorrem na mesma transação.
9. Retry mantém payload, `clientRecordId` e `clientRecordVersion`.
10. Novo dia possui sequência independente e começa em zero.
11. Operações cumulativas antigas não podem ser misturadas com incrementos
    novos no mesmo dia. A compatibilidade deve usar ponte segura sem leitura ou
    exclusão remota e sem converter históricos arbitrariamente.
12. Reconciliação preserva incrementos existentes e cria somente diferença ainda
    não enfileirada.

## Escopo

Inclui fila persistida, payload técnico, schema/migration necessária, transação
de enfileiramento, reconciliação, mapper de `StepsRecord`, identidade e caminho
de retry.

Exclui `DailySummary`, interface, histórico, distância e calorias locais,
exercícios, BPM, peso, altura, parser BLE, prioridade de fontes, leitura ou
limpeza do Health Connect, rota e testes automatizados.

## Abordagem escolhida

Adicionar payload incremental específico de `DailySummary + STEPS` à operação
persistida e permitir múltiplas operações desse par por resumo, com ordinal
local determinístico. O payload será a fonte exclusiva do mapper. Operações
não relacionadas continuam usando semântica atual.

Para operações cumulativas legadas, a primeira reconciliação deve congelar uma
ponte única antes de criar incrementos posteriores, preservando identidade
existente e evitando coexistência de modelos no mesmo dia. Nenhuma limpeza
remota será automatizada.

## Persistência e compatibilidade

O schema atual deve ser inspecionado antes da edição. Se as colunas/índice
atuais não comportarem múltiplos incrementos, criar migration explícita e
schema exportado. Payload legado nulo deve ser reconhecível e não pode ser
recalculado durante retry.

## Critérios de aceite

- `DailySummary` continua único, cumulativo e usado pela UI, histórico,
  distância e calorias estimadas.
- Crescimentos `5.991 -> 6.028 -> 6.040` geram `5.991`, `37`, `12` em
  intervalos contíguos.
- Igualdade, redução, zero, coleta temporal inválida e leitura duplicada não
  criam operação nem alteram checkpoint.
- Operações concorrentes não cobrem a mesma faixa.
- Payload e identidade permanecem iguais em retry e falha parcial.
- Mapper usa `count = stepDelta` persistido e offsets do fuso no início/fim.
- Reconciliador não recria cumulativo já coberto nem converte histórico antigo
  automaticamente.
- Outros tipos de record e fluxos não relacionados permanecem inalterados.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passam.
- Nenhum teste é criado, alterado ou executado.

## Riscos e mitigação

- Estado remoto cumulativo anterior é desconhecido: preservar identidade legada,
  usar ponte única e não excluir records.
- Crescimento da fila: aceitar cardinalidade incremental nesta experiência;
  compactação fica fora do escopo.
- Mudança de schema pode conflitar com trabalho da Fase 13: executar sem
  alterações concorrentes em Room, mapper ou coordinator.
- Provider pode tratar intervalos pequenos de modo inesperado: registrar como
  risco experimental, sem alegar validação física inexistente.

## Ordem das subtarefas

`016-01 -> 016-02`.

`016-01` implementa verticalmente a mudança e atualiza a decisão arquitetural.
`016-02` faz revisão independente e fechamento documental.

## Premissas

- O branch atual e schema real são fonte de verdade sobre versão, migration e
  APIs existentes.
- Room continua sendo fonte operacional e visual primária.
- A fila pode receber payload técnico sem criar entidade de domínio de deltas.
- O usuário quer mudança experimental restrita a passos de `DailySummary`.
