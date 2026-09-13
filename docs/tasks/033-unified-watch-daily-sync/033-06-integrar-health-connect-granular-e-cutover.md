# 033-06 — Integrar Health Connect granular e cutover

- **Status:** completed — correção pós-validação implementada; revalidação física pendente
- **Depende de:** 033-04 e 033-05
- **Evidência obrigatória:**
  - `docs/integration/033-03-d1-v2-physical-validation.md`
  - `docs/integration/033-05-unified-watch-sync-physical-validation.md`
  - `docs/integration/033-06-health-connect-clean-install-validation.md`

## Objetivo

Projetar dados diários para Health Connect sem dupla contagem, distinguir cauda
recente FEA1 × D1 de divergência estrutural, manter watch sync separado de efeito
remoto e preservar uma representação de passos funcional enquanto D1 granular
não puder substituir com segurança o caminho incremental legacy.

## Contratos preservados

### FEA1 × D1

```text
FEA1 == ΣD1
+ D1 estruturalmente completo
  -> COMPLETE

FEA1 > ΣD1
+ D1 estruturalmente completo
+ último bucket recente em relação ao FEA1
  -> RECENT_TAIL
  -> não é divergência estrutural

FEA1 < ΣD1
  -> DIVERGENT

D1 estruturalmente incompleto
  -> PARTIAL
```

Nunca converter `FEA1 - ΣD1` em bucket, intervalo, timestamp ou `StepsRecord`
sintético.

### D1 × D4

D1 pode conter passos, distância e calorias ocorridos durante workouts D4. Logo:

- não usar `D1 fora + D4 dentro` como regra global;
- não somar D1 + D4 ingenuamente;
- não suprimir D1 apenas por overlap temporal;
- D4 continua canônico no contexto do workout;
- substituição diária por D4 exige prova de cobertura e não duplicação.

## Implementação inicial

A primeira implementação da 033-06 entregou:

- Room v19;
- `health_sync_daily_cutovers` por dispositivo;
- `WatchDailyStepCoverage.RECENT_TAIL`;
- janela conservadora de dez minutos para cauda recente;
- payload/mapper D1 fail-closed, sem inventar fronteira temporal;
- remoção do auto-sync Health Connect após importação D1;
- preservação de ledgers/créditos das Specs 016/027;
- calorias Health Connect mantidas `deferred`;
- Health Connect write-only e local-first.

A projeção D1 granular não foi enfileirada para escrita remota porque o pacote
persistido fornece um ponto temporal, mas não prova início/fim do intervalo nem
não sobreposição segura com `WorkoutSteps`.

## Validação física e gaps encontrados

### 1. Instalação com legado

A migration real v18 → v19 preservou dados e persistiu cutover para o próximo dia.
Watch sync da Home continuou sem efeito remoto automático e `RECENT_TAIL` foi
observado fisicamente.

Ao usar `Configurações -> Sincronizar agora`, o backlog antigo mostrou outra
limitação: um único lote de 100 operações podia consumir retries antigos antes de
alcançar fatos atuais, exigindo vários cliques manuais.

### 2. Instalação limpa

Após limpar os dados do app, associar novamente o Champion C001, executar Home
sync e depois o sync explícito de Settings, o banco continha:

```text
cutover solicitado = 2026-09-13
FEA1.steps          = 5056
Σ D1.steps          = 5056
D1 distance         = 3731 m
D1 calories         = 244.0 kcal

workouts D4 v4      = 3
READY_TO_SYNC       = 1 RUNNING
PENDING_REVIEW      = 2
```

A fila Health Connect, porém, tinha somente:

```text
HEART_RATE  57  SYNCED
HEIGHT       1  SYNCED
```

Não havia operação para STEPS nem primeira projeção para a atividade
`READY_TO_SYNC`.

## Correção A — cutover efetivo de STEPS depende de substituto seguro

A data de cutover continua persistida por dispositivo como intenção/auditoria,
mas **não desliga o caminho DailySummary/FEA1 enquanto a projeção D1 granular
remota estiver `deferred`**.

Contrato efetivo:

```text
D1 granular remoto NÃO comprovado
  -> manter FEA1/DailySummary STEPS incremental
  -> manter créditos/ledgers Spec 016/027
  -> D1 granular permanece somente local
  -> não fabricar intervalo/residual

D1 granular remoto comprovado no futuro
  -> ativar explicitamente o cutover efetivo
  -> somente então impedir novos DailySummary STEPS
```

A persistência Room v19 não é reescrita nem migrada novamente. O domínio expõe a
data solicitada e uma data efetiva de fallback enquanto
`GRANULAR_D1_STEPS_REMOTE_PROJECTION_READY = false`.

Isso evita a lacuna observada em instalação limpa sem inventar representação
nova.

## Correção B — Settings inclui atividades READY_TO_SYNC

`Configurações -> Sincronizar agora` agora é um gesto explícito global que:

1. processa a fila local existente;
2. depois consulta `WorkoutActivityFilter.READY_TO_SYNC`;
3. restringe a atividades `WATCH_D3_D4`;
4. chama `syncActivity(activity.id)` para cada uma;
5. reutiliza a preparação congelada/manifesto/guards já existentes no sync
   explícito de workout.

Não inicia primeira projeção para:

```text
PENDING_REVIEW
PENDING_CALIBRATION
DISCARDED
SYNCED
```

Portanto, `review-before-sync` permanece obrigatório.

## Correção C — backlog antigo em um único gesto explícito

O coordinator mantém limite padrão de 100 operações por `syncPending()` e a fila
preserva ordenação causal existente.

O gesto de Settings agora executa passes sucessivos enquanto:

- ainda existir fila processável;
- o passe anterior produzir progresso;
- não ocorrer falha integral sem avanço;
- o máximo de 20 passes não tiver sido alcançado.

Isso resolve o starvation físico observado para backlogs de centenas de operações
sem ordenar ingenuamente por recência/estado e sem inverter cadeias incrementais
de passos/créditos.

O loop é foreground, explícito e finito. Um passe sem progresso interrompe a
execução para não criar retry agressivo.

## Passos

Enquanto D1 granular estiver deferred:

```text
FEA1
  -> total diário local autoritativo
  -> fonte do caminho remoto incremental já comprovado

D1
  -> distribuição local/diagnóstico/composição
  -> NÃO cria intervalo remoto inventado

D4 / WorkoutSteps
  -> detalhe do workout
  -> coexistência com Daily usa contabilidade/créditos Spec 027
```

`RECENT_TAIL` continua sendo um estado de leitura local; não cria residual remoto.

## Distância

D1 fornece distância diária nativa e D4 fornece distância canônica do workout.
Como evidência física mostrou que D1 já inclui distância de workout, não publicar
ambos de forma sobreposta sem prova determinística de cobertura.

Distância **diária D1** continua `deferred`/fail-closed.

Isso não impede filhos de distância de um workout `READY_TO_SYNC` quando o
contrato congelado de workout já os considera elegíveis e seguros.

## Frequência cardíaca

D1 continua elegível por ponto BPM válido. A sincronização física confirmou BPM
funcional no Health Connect. Pontos D1 em read model combinado só cedem ao D4
quando houver cobertura D4 válida.

## Calorias

Calorias diárias continuam locais/deferred. Não escolher
`ActiveCaloriesBurned` versus `TotalCaloriesBurned` por inferência.

Escalas confirmadas permanecem:

```text
D1 raw / 10.0
D4 raw / 1000.0
```

## Guard rails

- sem backfill granular histórico;
- sem delete remoto amplo;
- sem reabrir `SYNCED` silenciosamente;
- sem leitura Health Connect;
- sem `D1 + D4` ingênuo;
- sem supressão D1 por mero overlap;
- sem bucket/residual sintético FEA1-D1;
- `RECENT_TAIL` preservado;
- Home/watch sync não chama Health Connect automaticamente;
- `PENDING_REVIEW`/`PENDING_CALIBRATION` não entram no sync global;
- ordem causal de operações incrementais não é substituída por “mais recente
  primeiro”;
- distância/calorias diárias continuam fail-closed.

## Arquivos da correção pós-validação

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthSyncCutover.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsViewModel.kt`
- `docs/integration/033-06-health-connect-clean-install-validation.md`
- `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- este documento.

## Validação técnica

Obrigatória após a correção:

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

## Revalidação física necessária

Antes de liberar 033-07:

```text
instalação limpa
-> associar relógio
-> Home pull-to-refresh
-> Settings -> Sincronizar agora
-> dump
```

Confirmar:

1. FEA1 volta a preparar STEPS incremental enquanto D1 granular estiver deferred;
2. `WorkoutSteps` não duplica o total diário;
3. a RUNNING `READY_TO_SYNC` recebe filhos elegíveis e pode chegar a `SYNCED`;
4. workouts `PENDING_REVIEW` continuam sem primeira projeção;
5. BPM/altura permanecem funcionais;
6. distância/calorias diárias continuam sem publicação insegura;
7. backlog antigo avança por múltiplos lotes em um único gesto sem loop infinito.

## Riscos residuais

- D1 granular remoto continua deferred até prova de intervalo e não sobreposição;
- a data persistida de cutover ainda não representa ativação efetiva de STEPS;
- o limite de 20 passes é deliberadamente conservador e finito; backlog maior pode
  exigir novo gesto, mas não fica oculto por um único lote de 100;
- migration/cutover corrigido e sync global de `READY_TO_SYNC` precisam do novo
  checkpoint físico descrito acima antes da 033-07.
