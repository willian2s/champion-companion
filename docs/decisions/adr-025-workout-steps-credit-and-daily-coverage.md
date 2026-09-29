# ADR-025 — Passos de atividade, crédito pós-aceite e cobertura diária

## Status

Aceita para implementação incremental na Spec 027.

## Contexto

O app possui `DailySummary` cumulativo e fila incremental de `StepsRecord`
definida pela Spec 016. Uma atividade D3/D4 também possui total de passos e
intervalos com passos decodificados, mas a projeção atual de atividade não
publica `StepsRecord` nem `StepsCadenceRecord`.

O `DailySummary` pode ser sincronizado antes ou depois da atividade. O app é
write-only e não pode provar por leitura remota quais passos um record diário
extenso cobriu. Aumentar o total remoto por duplicidade é pior que bloquear uma
publicação ambígua.

## Decisão

### 1. Precedência e autoridade

`DailySummary.steps` continua referência cumulativa do resumo diário por data
local. Após o cutover granular D1, `WatchDailyD1Sample` válido é a autoridade
da projeção granular de passos no Health Connect. `WorkoutActivity.metrics.steps`
é quantidade autoritativa da atividade quando válida; `decodedSteps` por
intervalo é fonte temporal para derivados e cadência.

Nenhuma soma parcial de intervalos substitui o total do pacote. Divergência entre
`DailySummary.steps` e a soma D1 é sinal diagnóstico, mas não bloqueia a
publicação de buckets D1 válidos. Ausência de `DailySummary` também não invalida
uma linha D1 estruturalmente válida. Nenhum desses casos produz correção
heurística nem passos inventados.

### 2. Cobertura local sem readback

Somente operações próprias do Champion Companion podem provar cobertura. Uma
operação diária cobre janela apenas quando possui payload válido, mesmo fuso,
estado `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT` e reconciliação `SENT`.

Para atividade no mesmo fuso, usa-se a janela semiaberta
`[startedAt, endedAt)`. Qualquer sobreposição com janela diária aceita bloqueia
o filho de passos da atividade. Operações pendentes, falhas ou apenas
enfileiradas não cobrem.

Atividade que atravessa data local, possui janela inválida ou não permite prova
de não sobreposição falha fechada. Não ler Health Connect, não ler terceiros e
não apagar/repartir Daily aceito.

### 3. Crédito somente pós-aceite

Publicação de `WorkoutSteps` cria crédito identificável somente após efeito
remoto aceito e checkpoint local persistido. Aceite é observado como retorno
`Accepted` do gateway seguido de `SYNCED` + `AFTER_REMOTE_EFFECT`/`SENT`.

Falha, cancelamento ou falha ao persistir checkpoint não ativa crédito. Retry
mantém identidade, versão e payload congelados.

Operação Daily legada aceita sem `dailyStepsPayload` não prova cobertura. Antes
de qualquer elegibilidade de `WorkoutSteps`, deve ser reconciliada para uma
ponte `LEGACY_BRIDGE` com janela, fuso e total congelados. Enquanto a ponte não
existir, cobertura é desconhecida e `WorkoutSteps` fica bloqueado.

O crédito identifica, no mínimo:

- atividade e data local;
- quantidade real de passos;
- janela e fuso;
- identidade e versão de `WorkoutSteps`;
- estado de publicação e consumo.

O efeito remoto acontece fora da transação Room. Depois de `Accepted`, uma única
transação local deve persistir `SYNCED` + `AFTER_REMOTE_EFFECT`/`SENT` e ativar
ou recuperar idempotentemente crédito correspondente. Se essa transação falhar,
crédito permanece inativo, a operação continua marcada para reconciliação/retry
com a mesma identidade e Daily bloqueia qualquer desconto até a recuperação.

### 4. Residual diário

Para atividade aceita antes do próximo Daily:

```text
deltaBruto = totalAtual - ultimoTotalEnfileirado
residual = deltaBruto - creditosAtivosDoMesmoDia
```

`deltaBruto <= 0` não cria record. `residual < 0` bloqueia sem avançar
checkpoint. `residual == 0` pode avançar checkpoint sem `StepsRecord` zero,
consumindo crédito na mesma transação. `residual > 0` exige janela válida,
contida na data local e não sobreposta.

A aproximação representacional permitida é uma janela posterior ao último fim
de atividade aceita até `dailyEnd`, desde que permaneça no dia e não sobreponha
atividade creditada. Se essa representação não for possível, Daily não publica,
não consome crédito e não avança checkpoint. Isso é fail-closed: preserva total
local autoritativo sem duplicar remoto.

Os caminhos de aceite de atividade e processamento Daily usam a mesma exclusão
lógica por atividade/data. A transação que confirma aceite e ativa crédito deve
terminar antes de Daily calcular residual; o caminho inverso adquire a exclusão
antes de preparar `WorkoutSteps`. Room não torna chamada remota atômica, por isso
estado aceito sem crédito ativo é sempre estado de reconciliação bloqueado, nunca
permissão para avançar Daily.

### 5. Ordem inversa

Se Daily aceito já sobrepuser atividade, `WorkoutSteps` fica bloqueado como
`ALREADY_COVERED`. Cadência, sessão e demais filhos elegíveis permanecem
independentes. Histórico remoto não é reescrito automaticamente.

### 6. Independência da cadência

`StepsCadenceRecord` é filho independente. Representa taxa temporal derivada de
intervalos D4 válidos, não incrementa Daily, não cria crédito e pode ser
publicado quando `WorkoutSteps` estiver bloqueado.

### 7. Persistência

Créditos serão persistidos em entidade Room própria, com índice único lógico por
atividade/data e referência à operação `WorkoutSteps`. Não reutilizar campos de
`DailyStepsSyncPayload`, não criar contador global e não alterar semântica de
`HealthSyncOperation`.

A evolução exige migration aditiva desde Room v13 e schema exportado. Essa
migration pertence à subtarefa 027-02; ADR-025 não introduz schema ou escrita
remota.

### 8. Supersessão parcial de ADR-012

Esta ADR supersede somente `ADR-012 §9`, que anteriormente proibia
`StepsRecord` separado por atividade. A Spec 027 autoriza esse filho sob as
guardas de cobertura, aceite, identidade e crédito desta ADR. Todas as demais
decisões da ADR-012 permanecem vigentes, inclusive write-only, local-first,
sync explícito, falha parcial e ausência de leitura remota.

## Consequências

- atividade primeiro pode preservar total remoto sem duplicar Daily;
- Daily primeiro bloqueia promoção ambígua de passos, com possível subcontagem;
- divergência quantitativa entre D1 e `DailySummary` não bloqueia a projeção
  granular D1 após o cutover;
- falha parcial permanece retryável por filho e não ativa crédito prematuro;
- cadência pode evoluir sem acoplamento à contabilidade diária;
- uma fila residual insegura pode exigir retry ou decisão explícita;
- Health Connect continua write-only, local-first, foreground e explícito;
- nenhum treino phone, GPS, rota ou localização é reativado.

## Alternativas rejeitadas

- contador global `pendingWorkoutSteps`;
- considerar operação pendente como crédito;
- ler Health Connect para reconstruir cobertura;
- somar total da atividade ao Daily sem desconto;
- repartir passos desconhecidos por buckets D4;
- alterar ou apagar histórico remoto para promover atividade retroativamente;
- sobrecarregar `dailyStepsPayload` com semântica de atividade;
- usar cadência como contagem de passos.

## Relação com decisões anteriores

- **ADR-012:** preserva write-only, local-first, filhos independentes e falha parcial.
- **ADR-015:** preserva incrementos imutáveis, payload congelado e ponte legada.
- **ADR-016:** preserva identidade temporal da reimportação.
- **ADR-018/ADR-019:** preserva raw/measured/derived/projection, duração D4
  normalizada, distância por passos e snapshots explícitos.

## Limites

ADR-025 não define schema SQL final, algoritmo 1 Hz, smoothing, permissão ou
payload final de `StepsCadenceRecord`, compatibilidade Health Sync/Strava,
validação física, leitura remota, migração ou implementação de comportamento.
