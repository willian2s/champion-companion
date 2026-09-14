# ADR-031 — Passos D1 granulares no Health Connect

- **Status:** accepted
- **Data:** 2026-09-13
- **Relacionado:** ADR-030; Specs 016, 027 e 033
- **Refina:** ADR-030, itens 14–16, somente para `STEPS`

## Contexto

A implementação inicial da 033-06 manteve a projeção D1 de passos `deferred`
porque o timestamp do sample ainda não havia sido promovido a uma janela remota.
Para não deixar o usuário sem passos, o caminho incremental FEA1/DailySummary foi
mantido temporariamente ativo.

A sequência de dumps físicos de 2026-09-13 forneceu evidência suficiente para
fechar a direção temporal do bucket D1 observado:

- todos os samples consecutivos do dia avançaram em intervalos exatos de cinco
  minutos;
- `Σ D1.steps` fechou exatamente com FEA1 em leituras completas anteriores;
- quando FEA1 ficou à frente do D1, a diferença apareceu depois do timestamp do
  último bucket (`RECENT_TAIL`), sem lacuna estrutural;
- na corrida física de `08:53:26` a `09:20:31`, os buckets D1 de `08:55` a
  `09:20` somaram exatamente `1935` passos, iguais aos `1935` passos D4;
- os buckets imediatamente anteriores eram nulos e o bucket `09:25` também foi
  nulo para essa atividade.

Esse conjunto é consistente com o timestamp D1 como **borda final** de um bucket
de cinco minutos. Para Health Connect, o sample em `t` representa a janela
semiaberta `[t - 5 min, t)`.

A mesma evidência reafirma que D1 já contém passos de workouts. Portanto publicar
simultaneamente D1 granular e `WorkoutSteps` duplicaria o mesmo fato.

## Decisão

1. `STEPS` D1 passa a ser elegível para Health Connect a partir do cutover
   granular efetivo.
2. O primeiro dia efetivo desta ativação é **2026-09-14**. A intenção de cutover
   persistida no Room v19 continua preservada; instalações cuja intenção seja
   posterior usam sua própria data.
3. Para cada sample D1 positivo materializado em `t`, congelar:

```text
start = t - 5 minutos
end   = t
steps = sample.steps
```

   com `zoneId`, offsets inicial/final, identidade e versão persistidos.
4. Buckets com `steps == 0` não geram `StepsRecord`.
5. Um dia só pode preparar D1 granular quando:
   - o multipart D1 recebido está estruturalmente completo para o
     `totalPositions` persistido;
   - existe FEA1 válido para o mesmo dispositivo/data;
   - `Σ D1.steps <= FEA1.steps`;
   - não existe nenhuma operação `DailySummary/STEPS` para aquela data.
6. `FEA1 - ΣD1` continua sendo apenas cauda/frescor. Nunca gerar residual
   sintético para fechar o total remoto.
7. Uma vez congelado, o payload D1 não é reescrito apenas porque novos buckets ou
   novo `totalPositions` chegam. Retry preserva identidade e payload.
8. A partir do cutover granular, `WorkoutSteps` de atividade do relógio não é
   publicado como um segundo `StepsRecord`. O ledger continua persistido e fica
   `BLOCKED/ALREADY_COVERED`, representando resolução local deliberada.
9. `WorkoutSteps` terminalmente `BLOCKED` não conta como `PENDING`, `FAILED` ou
   `SYNCING` acionável nas superfícies de status. Não é marcado `SYNCED`, porque
   nenhum efeito remoto correspondente ocorreu.
10. `STEPS_CADENCE` continua independente e elegível conforme o contrato do
    workout.
11. Distância diária D1 e calorias diárias D1 permanecem `deferred`; distância e
    calorias do workout continuam usando o contrato já validado da atividade.

## Cutover por família

A seleção é exclusiva por data:

```text
data < cutover efetivo
  -> FEA1/DailySummary STEPS incremental
  -> créditos da Spec 027 evitam WorkoutSteps duplicado

data >= cutover efetivo
  + já existe DailySummary/STEPS nessa data
  -> preservar família legacy daquele dia
  -> não preparar D1 granular

data >= cutover efetivo
  + não existe DailySummary/STEPS nessa data
  -> D1 granular é a família diária de STEPS
  -> WorkoutSteps fica resolvido localmente como ALREADY_COVERED
```

Uma data nunca mistura as duas famílias.

## Consequências

### Positivas

- passos passam a ter distribuição temporal real do relógio no Health Connect;
- não há residual inferido a partir de FEA1;
- workouts não duplicam passos já representados pela linha D1;
- o contador de pendências passa a representar somente trabalho remoto acionável;
- nenhuma nova migration Room é necessária.

### Riscos residuais

- a semântica de borda foi inferida por evidência física consistente, não por
  documentação oficial do firmware;
- o bucket em torno da meia-noite merece observação física adicional, embora a
  regra de cinco minutos permaneça determinística;
- mudanças de firmware podem exigir revalidação da duração/direção do bucket;
- distância/calorias diárias permanecem pendências separadas e não devem ser
  liberadas por analogia com passos.

## Guard rails

- Health Connect permanece write-only;
- nenhuma escrita remota ocorre no pull da Home;
- review-before-sync de workouts permanece;
- nenhum `D1 + D4` ingênuo;
- nenhuma reabertura de operação `SYNCED`;
- nenhum bucket ou timestamp sintético;
- nenhuma alteração em distância/calorias diárias nesta decisão.
