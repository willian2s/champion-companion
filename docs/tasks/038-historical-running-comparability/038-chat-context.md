# 038 — Resumo de contexto da conversa

- **Data:** 2026-09-29
- **Branch:** `feat/038-historical-running-comparability`
- **Base da branch:** `e2d141276ef30ee846d8aafba2c69ad425dd3e99`
- **Planejamento 038 antes deste resumo:** `506206f2314b738f818cfb2a4df0780c841c7593`
- **Objetivo deste arquivo:** preservar o contexto técnico e de produto que levou da conclusão da Spec 037 ao planejamento da Spec 038 e do roadmap 038–041.

Este documento é um resumo de conversa e não substitui Spec, ADR, task ou evidência física. Em caso de divergência, prevalecem as fontes autoritativas do repositório.

## 1. Estado após a Spec 037

A Spec 037 — Tendência de corrida: relação entre ritmo e frequência cardíaca — foi concluída, validada fisicamente e incorporada à `main`.

Merge:

```text
e2d141276ef30ee846d8aafba2c69ad425dd3e99
Merge pull request #33
Feat/037 running pace heart rate trend
```

A validação física da 037 encontrou, na janela bounded de 56 dias:

```text
corridas confirmadas RUNNING:                       21
snapshot ativo presente:                            21
escalares canônicos positivos:                      21
temporalidade ELIGIBLE:                             21
WATCH_NATIVE / WATCH_NATIVE_CONTEXT_CALIBRATED:      5
fallback por comprimento de passo:                  16
```

As 21 corridas aparecem na lista factual, mas apenas as 5 com lineage nativa comparável entram nos gráficos principais.

A 037 permanece com o contrato:

- população `WATCH_D3_D4`, `confirmedType == RUNNING`, não descartada;
- janela `[asOf - 56 dias, asOf)`;
- pace calculado de `canonicalDistanceMeters` e `canonicalCoveredDurationNanos`;
- comparabilidade somente para `WATCH_NATIVE` ou `WATCH_NATIVE_CONTEXT_CALIBRATED`;
- versão nativa suportada `watch-d3-d4-v4`;
- temporalidade elegível e sem indicador de plausibilidade bloqueador;
- fallback continua listável, mas fora do trend comparável;
- média de FC ponderada por sample-and-hold e coverage explícita;
- Room v20;
- nenhuma persistência analítica ou efeito Health Connect.

## 2. Incidente de sincronização Health Connect observado durante a 037

Durante a execução da 037 houve uma investigação separada de sincronização em produção.

### 2.1 Workout recém-finalizado não sincronizava

O banco mostrou um treino cujo fim ainda não estava coberto pela linha D1 disponível.

Estado observado:

```text
fim do workout:        08:10:44
último bucket D1:      08:10:00
```

O caminho de `WorkoutSteps` chegava a:

```text
reserveWorkoutStepsForGranularDailyLine
→ hasPreparedGranularCoverage
```

e falhava enquanto:

```kotlin
latestBucketEnd.isBefore(requiredThrough)
```

fosse verdadeiro.

Após o próximo bucket D1 chegar e cobrir temporalmente o fim do treino, a sincronização funcionou.

Conclusão:

- a race temporal é real;
- não era rejeição do provider Health Connect;
- a cobertura D1 precisa alcançar o fim do workout antes de considerar os passos já cobertos;
- não deve ser inventada tolerância temporal ou cobertura futura.

### 2.2 Passos posteriores deixaram de entrar na fila

Uma segunda inspeção mostrou:

```text
DailySummary.steps = 3716
Σ D1.steps         = 3732
divergência        = +16
```

Os buckets D1 continuavam sendo persistidos, mas depois de determinado ponto nenhuma nova operação granular `STEPS` era criada.

A causa estava em `GranularHealthSyncRepository.prepareGranularD1Records()`:

```kotlin
val d1Steps = eligibleDaySamples.sumStepsOrNull()
    ?: return@withWriteTransaction RepositoryResult.UnexpectedState

if (d1Steps > summary.steps) continue
```

e em uma guarda equivalente de `hasPreparedGranularCoverage()`.

A decisão de produto foi:

> divergência entre D1 granular e `DailySummary.steps` não deve bloquear a sincronização granular.

Após o cutover granular:

- `WatchDailyD1Sample` válido é autoridade para projeção granular D1;
- `DailySummary.steps` permanece referência cumulativa/read model e sinal diagnóstico;
- `ΣD1 != DailySummary.steps` não bloqueia criação ou envio dos buckets;
- anti-duplicidade com `WorkoutSteps` continua usando janela, identidade, operações e créditos;
- divergência quantitativa não substitui validação estrutural.

A correção foi incorporada à `main` em:

```text
9333b9ae4822fea692fc29299fa610e81b365eb5
Merge pull request #32
fix(sync): tolerate D1 summary divergence
```

Essa correção é independente da 037 e da 038.

## 3. Roadmap definido após a 037

Foi definido um roadmap pós-037 com specs independentes, sem reabrir as Fases 17–25:

| Spec | Tema | Pergunta principal |
| --- | --- | --- |
| 038 | Recuperação de comparabilidade histórica | Quanto do histórico antigo pode usar a pipeline nativa atual com segurança? |
| 039 | Volume e consistência de corrida | Quanto e com que frequência estou correndo ao longo do tempo? |
| 040 | Recordes pessoais e melhores esforços | Quais são minhas melhores marcas comparáveis? |
| 041 | Tendência de cadência | Como minha cadência varia entre corridas e ao longo do tempo? |

Ordem pretendida:

```text
038
 ↓
039
 ↓
040
 ↓
041
```

### Direção da 039

Visão factual de volume:

- quantidade de corridas;
- duração total;
- distância total comparável;
- distância/duração média;
- dias com corrida;
- distribuição semanal.

Não criar score de consistência, classificação bom/ruim, streak como métrica principal ou recomendação de treino.

### Direção da 040

Recordes pessoais baseados em fatos canônicos e provenance.

Candidatos iniciais:

```text
1 km
5 km
10 km
```

A spec deve diferenciar:

```text
split materializado
!=
melhor janela contínua dentro da atividade
```

e decidir explicitamente entre splits existentes e best-effort rolling window.

### Direção da 041

Tendência factual de cadência entre corridas:

```text
data → cadência média ponderada
```

Sem inferir cadência ideal, regra de 180 spm, overstriding, risco de lesão ou eficiência de passada.

## 4. Motivo para atacar a 038 primeiro

A 037 revelou uma limitação concreta:

```text
21 corridas
5 comparáveis
16 fallback
```

Antes de construir volume por distância, recordes ou novas relações temporais, é necessário entender se essas 16 corridas podem ou não ganhar comparabilidade nativa legitimamente.

O objetivo da 038 não é maximizar o número de pontos.

A pergunta é:

> Quanto do histórico fallback pode ser recuperado deterministicamente usando fatos locais já preservados, sem inventar dados e sem quebrar o contrato local/remoto?

Resultados válidos:

```text
GO
PARTIAL-GO
NO-GO
INCONCLUSIVO
```

Nenhuma meta percentual foi definida.

## 5. Descoberta importante antes do planejamento 038

Já existe infraestrutura de reprocessamento explícito desde a Spec 014:

```text
WorkoutActivityRepository.reprocessWatchWorkout(...)
RoomRepositories.reprocessWatchWorkoutInternal(...)
```

Ela é local, explícita e transacional.

O fluxo atual reconhece:

```text
d4-v1
d4-v2
d4-v3
d4-v4
```

Mas existe uma distinção essencial:

> reprocessável não significa recuperável para comparabilidade nativa.

### Legacy

`d4-v1/v2/v3` podem ser reprocessáveis quando os fatos de passos estão preservados, mas a pipeline vigente continua baseada em comprimento de passo.

Eles não viram `WATCH_NATIVE` apenas por serem reprocessados.

### Native

A derivação nativa atual exige:

```text
parserVersion == d4-v4
+
WatchWorkoutNativeDistancePolicy.isEligible(...) == true
```

Somente então o fluxo pode produzir:

```text
WATCH_NATIVE
WATCH_NATIVE_CONTEXT_CALIBRATED
```

Portanto, a presença de snapshot fallback não é suficiente para declarar uma corrida recuperável.

## 6. Diagnostic payload histórico

O código já usa diagnostic payload em alguns caminhos, inclusive para recuperação autoritativa de duração.

A 038 deve investigar se bytes físicos preservados de importações `d4-v1/v2/v3` poderiam sustentar uma interpretação moderna da estrutura nativa.

Isso é apenas hipótese de auditoria.

Não é permitido:

- alterar `importRecord.parserVersion`;
- relabelar `d4-v1/v2/v3` como `d4-v4`;
- inferir dados ausentes;
- promover apenas porque existe payload.

Uma interpretação legacy candidata precisaria fechar, entre outros:

- device/import identity;
- CRC;
- external block;
- `startedAt`;
- quantidade e índices;
- passos;
- BPM compatível quando disponível;
- duração/header;
- fatos persistidos;
- bytes físicos inalterados.

Se uma interpretação moderna de legacy exigir provenance/version que não caiba no modelo atual, isso é blocker para implementação, não autorização automática para migration.

## 7. Política 038 para atividades sincronizadas

O reprocessamento atual bloqueia:

```text
SYNCING
→ SYNC_IN_PROGRESS

SYNCED
→ SYNCED_REQUIRES_FUTURE_PROJECTION
```

Além disso, projection children inseguros bloqueiam mutação.

O planejamento 038 preserva esse guard.

Alternativas discutidas:

1. continuar bloqueando recuperação canônica de `SYNCED`;
2. permitir derivação local divergente do payload remoto histórico;
3. criar uma segunda leitura/derivação efêmera só para analytics.

A decisão do planejamento foi manter a alternativa 1.

Motivos:

- Health Connect é write-only;
- payloads/IDs/versões já aceitos são históricos congelados;
- não há readback para provar o estado remoto;
- não existe autorização para republicação automática;
- segunda verdade analítica duplicaria pipeline e aumentaria risco de inconsistência.

Leitura em memória pode existir apenas no Gate A para auditoria, nunca como fonte user-facing da 037.

## 8. Taxonomia planejada para a 038

O Gate A deve separar fonte de disposição de mutação.

Classes:

### `NATIVO_ATUAL`

Já satisfaz o contrato 037. Nenhuma recuperação.

### `V4_RECUPERAVEL_LOCALMENTE`

Fonte v4 completa e determinística, mas snapshot ativo ainda não comparável.

Pode ser candidato a ação local somente se o Gate autorizar.

### `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`

Existe payload histórico candidato, mas ainda falta prova integral ou provenance segura.

Não entra na 037.

### `LEGACY_SEM_FONTE_NATIVA`

Não há fatos suficientes para reconstrução nativa.

Permanece fallback legitimamente.

### `ESTRUTURALMENTE_INELEGIVEL`

Fonte falha em integridade, header/série, CRC, índices, temporalidade, fatos nativos ou plausibilidade.

### `BLOQUEADO_POR_ESTADO_REMOTO`

A fonte seria recuperável, mas workflow/projeção remota impede mutação canônica.

Importante:

`BLOQUEADO_POR_ESTADO_REMOTO` só pode ser atribuído depois de provar a fonte. Não deve mascarar ausência de dados.

## 9. Snapshots, calibração, Room e Health Connect

### Snapshots

Preservar:

```text
source/raw
!=
measured
!=
derived snapshot
!=
Health Connect projection
```

Nunca editar snapshot histórico para fazê-lo parecer nativo.

Uma recuperação futura, se autorizada:

- cria snapshot novo;
- preserva snapshot anterior;
- materializa derived intervals/splits ligados ao novo snapshot;
- troca ativo somente dentro de transação explícita e idempotente.

### Calibração

Recuperar fatos nativos é separado de aplicar calibração contextual.

Direção conservadora:

- recuperar primeiro como `WATCH_NATIVE`;
- não aplicar automaticamente o perfil RUNNING atual;
- não aprender perfil durante recuperação;
- não recalibrar outras atividades;
- `WATCH_NATIVE_CONTEXT_CALIBRATED` só pode surgir com evidence válida já associada e autorizada.

### Room

Alvo da 038:

```text
Room v20
sem migration
```

Se o modelo atual não conseguir representar uma provenance necessária, parar e abrir nova decisão em vez de antecipar schema.

### Health Connect

Invariante forte:

- zero chamada remota;
- zero nova operação automática;
- zero alteração de `SYNCED`;
- zero reset de reconciliation;
- zero delete/retry/readback;
- zero alteração silenciosa de `clientRecordId`, `clientRecordVersion` ou `frozenPayload`;
- zero reabertura de workflow para reenviar treino.

## 10. Estrutura planejada da 038

Branch:

```text
feat/038-historical-running-comparability
```

Base:

```text
e2d141276ef30ee846d8aafba2c69ad425dd3e99
```

Planejamento inicial:

```text
506206f2314b738f818cfb2a4df0780c841c7593
docs(spec): planejar comparabilidade histórica 038
```

Arquivos principais:

```text
docs/specs/038-historical-running-comparability.md
docs/decisions/adr-038-historical-running-comparability.md

docs/tasks/038-historical-running-comparability/
  038-00-overview.md
  038-01-auditar-recuperabilidade-e-congelar-gate.md
  038-02-implementar-preflight-de-recuperacao.md
  038-03-implementar-recuperacao-local-explicita.md
  038-04-integrar-recuperacao-com-exercicios-e-037.md
  038-05-validar-preservacao-e-ganho-de-comparabilidade.md
  038-06-validar-fisicamente-e-fechar.md
```

Estado no planejamento:

```text
Status geral: pending
Progresso: 0/6
Gate A: INCONCLUSIVO até auditoria física read-only
```

## 11. Papel de cada subtarefa

### 038-01

Auditar a população real e congelar Gate A.

Deve produzir contagens sanitizadas por:

- parser version;
- método/versão de derivação;
- workflow;
- presença de diagnostic payload;
- header v4;
- fatos nativos completos;
- taxonomia de recuperabilidade;
- bloqueios remotos;
- inelegibilidade estrutural.

Sem banco físico disponível, permanece `INCONCLUSIVO`; não converter ausência de evidência em `NO-GO`.

### 038-02

Preflight read-only e determinístico somente se Gate A autorizar.

Deve responder:

```text
já comparável
recuperável
não recuperável + motivo
bloqueado + motivo
```

Sem escrita.

### 038-03

Recuperação local explícita somente para classes autorizadas.

Reusar a pipeline existente e preservar source facts, parser/import identity, workflow, snapshots históricos e HC.

### 038-04

Integrar com a ação existente de Exercícios quando possível e confirmar que a 037 incorpora naturalmente snapshots recuperados.

Não relaxar `RunningPerformanceHistoryPolicy`.

### 038-05

Validar preservação/idempotência e ganho de comparabilidade somente por fonte válida.

### 038-06

Validação física Release incremental e fechamento.

Cenários ausentes naturalmente permanecem `N/A`.

## 12. Convenções de execução mantidas

- Não criar nem executar testes automatizados salvo pedido explícito.
- Para alterações de código, usar os gates vigentes do repo:
  `./gradlew lint assembleDebug assembleRelease` e `git diff --check`.
- Para execução puramente documental, `git diff --check` é suficiente quando a task assim definir.
- Não fabricar cenário físico para preencher checklist.
- Não usar reimportação, wipe ou edição do banco para produzir evidência artificial.
- Preservar dados físicos existentes.
- Ao executar tasks da 038, seguir a documentação já criada; o prompt normal pode ser curto, por exemplo:

```text
Execute a task 038-01 da branch feat/038-historical-running-comparability.
```

Contexto adicional só é necessário quando surgir uma descoberta nova que ainda não esteja registrada na Spec/ADR/task.

## 13. Próximo passo

O próximo item formal é:

```text
038-01 — Auditar recuperabilidade e congelar Gate A
```

A decisão mais importante dessa task é descobrir, no banco físico real e em modo read-only, a composição das 16 corridas fallback.

A 038 não deve avançar para implementação apenas porque o planejamento existe.

Primeiro precisa responder com evidência:

```text
GO
PARTIAL-GO
NO-GO
ou
INCONCLUSIVO
```
