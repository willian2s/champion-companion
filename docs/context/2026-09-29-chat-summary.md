# Resumo do chat — Champion Companion

- **Data:** 2026-09-29
- **Projeto:** `willian2s/champion-companion`
- **Branch de continuidade:** `feat/038-historical-running-comparability`
- **Objetivo:** preservar o contexto técnico, decisões, descobertas físicas e direção de produto discutidos neste chat.

Este arquivo é contexto de trabalho e não substitui Specs, ADRs, tasks, baseline ou evidências físicas. Em caso de divergência, prevalecem as fontes autoritativas do repositório.

## 1. Direção geral do produto

O Champion Companion é um aplicativo Android pessoal, local-first e watch-first, centrado no Champion C001.

Fluxo vigente: `C001 → BLE → persistência local → derivação/revisão → ação explícita → Health Connect`.

Diretrizes mantidas durante o chat:

- não expandir treino iniciado pelo celular, GPS, rota, mapa ou `ExerciseRoute`;
- Health Connect permanece explícito, local-first e write-only;
- analytics devem preferir fatos observados a inferências;
- ausência permanece ausência, nunca zero ou dado inventado;
- provenance precisa permanecer visível;
- não criar score, diagnóstico, recomendação fisiológica ou causalidade sem base factual;
- não alterar histórico silenciosamente;
- cenários físicos ausentes ficam `N/A`, não são fabricados;
- Room atual é v20;
- Fases 17–25 permanecem encerradas; novas evoluções são Specs independentes.

## 2. Convenções de execução

- Inspecionar repo, branch, documentação e código real antes de afirmar causa ou estado.
- Quando a task já estiver bem especificada, usar prompt curto, por exemplo: `Execute a task 038-01 da branch feat/038-historical-running-comparability.`
- Não criar nem executar testes automatizados sem pedido explícito.
- Para mudanças de código, gates usuais: `git diff --check` e `./gradlew lint assembleDebug assembleRelease`.
- Não apagar, reimportar ou fabricar dados físicos para preencher validação.
- Antes de confirmar uma task como concluída, reler task, overview e código atual da branch.

## 3. Base analítica anterior à 037

### Spec 034

Consolidou zonas de FC, sample-and-hold, Edwards load, coverage e FC máxima opcional. A análise usa `WorkoutHeartRateSample` e não confia em média BPM agregada do parser. A primitiva temporal de FC foi compartilhada com splits.

### Spec 035

Adicionou histórico bounded de carga cardiovascular. A principal lição física foi não reidratar derivação/splits completos quando o histórico precisa apenas de fatos mínimos.

### Spec 036

Adicionou histórico de zonas de FC e distribuição de intensidade, mantendo Room v20, analytics efêmero e sem efeitos no Health Connect.

## 4. Spec 037 — Tendência de corrida

Pergunta de produto: como ritmo e frequência cardíaca variam ao longo do tempo nas corridas?

População congelada: `WATCH_D3_D4`, `confirmedType == RUNNING`, não descartada, `startedAt ∈ [asOf - 56 dias, asOf)`.

Pace usa somente snapshot ativo, `canonicalDistanceMeters` e `canonicalCoveredDurationNanos`. Comparabilidade exige `WATCH_NATIVE` ou `WATCH_NATIVE_CONTEXT_CALIBRATED`, versão `watch-d3-d4-v4`, temporalidade elegível, escalares válidos e ausência de blocker de plausibilidade.

Fallback por comprimento de passo pode aparecer na lista factual, mas fica fora dos gráficos.

FC usa `WorkoutHeartRateSample`, média ponderada por sample-and-hold e denominator de duração coberta. Não usa `WorkoutActivity.metrics.averageBpm`, não exige FC máxima e não transforma ausência em zero.

A UI usa dois gráficos temporais independentes, um para ritmo e outro para FC, sem dual-axis ou scatter artificial. O read model é bounded e evita N+1/reidratação ampla.

## 5. Fechamento físico da 037

A 037 foi concluída e mergeada na main em `e2d141276ef30ee846d8aafba2c69ad425dd3e99`.

A validação física encontrou:

- 21 corridas `RUNNING`;
- 21 snapshots ativos;
- 21 com escalares canônicos positivos;
- 21 temporalmente `ELIGIBLE`;
- 5 `WATCH_NATIVE`/`WATCH_NATIVE_CONTEXT_CALIBRATED` comparáveis;
- 16 `STEP_LENGTH_FALLBACK`;
- 21 linhas factuais na lista e 5 pontos nos gráficos.

Isso era provenance, não clipping de UI. Room permaneceu v20, sem migration, sem persistência analítica e sem Health Connect acionado pela tela.

## 6. Diagnóstico do banco de produção

Durante a 037 houve uma investigação separada de sincronização. O banco do pacote `br.com.mwconsulting.championcompanion` foi extraído com `run-as`, preservando `champion-companion.db`, WAL e SHM quando presentes. A regra adotada foi trabalhar sobre cópia e não checkpointar os originais.

### 6.1 Race temporal entre workout e D1

Um workout terminou por volta de `08:10:44`, enquanto o último bucket D1 disponível terminava em `08:10:00`.

O caminho `prepareWatchActivityOperations → workout-steps → prepareProjectionChild → reserveWorkoutStepsForGranularDailyLine → hasPreparedGranularCoverage` falhava enquanto `latestBucketEnd < workout.end`.

As operações estavam `PENDING`, com `attemptCount = 0`, `lastAttemptAt = null` e sem erro, provando que o provider Health Connect ainda nem havia sido chamado.

Após chegar bucket D1 posterior cobrindo o fim do treino, a sincronização funcionou. A decisão foi manter essa prova temporal exata, sem tolerância artificial.

### 6.2 Divergência D1 × DailySummary

Depois, os passos do dia deixaram de entrar na fila. O banco mostrou `DailySummary.steps = 3716` e `Σ D1.steps = 3732`, diferença de 16 passos.

A causa estava em `GranularHealthSyncRepository.prepareGranularD1Records()`, que bloqueava o dia quando `d1Steps > summary.steps`, e em guarda quantitativa equivalente dentro de `hasPreparedGranularCoverage()`.

A decisão de produto foi: após o cutover granular, `WatchDailyD1Sample` válido é autoridade da projeção granular no Health Connect. `DailySummary.steps` continua referência cumulativa/read model e sinal diagnóstico, mas não é hard gate.

Assim, `ΣD1 != DailySummary.steps` não bloqueia sync. Anti-duplicidade continua baseada em janela, identidade, operações e `WorkoutStepsCredit`, não em comparação global de totais.

A correção foi mergeada na main em `9333b9ae4822fea692fc29299fa610e81b365eb5` com `fix(sync): tolerate D1 summary divergence`.

## 7. Roadmap pós-037

Foram definidos quatro próximos itens:

| Spec | Tema | Pergunta principal |
| --- | --- | --- |
| 038 | Recuperação de comparabilidade histórica | Quanto do histórico antigo pode usar a pipeline nativa atual com segurança? |
| 039 | Volume e consistência de corrida | Quanto e com que frequência estou correndo? |
| 040 | Recordes pessoais e melhores esforços | Quais são minhas melhores marcas comparáveis? |
| 041 | Tendência de cadência | Como minha cadência varia ao longo do tempo? |

Ordem desejada: `038 → 039 → 040 → 041`.

039 deve ser factual, sem score de consistência ou prescrição. 040 deve distinguir split materializado de melhor janela contínua e decidir explicitamente a política de best effort. 041 deve mostrar tendência factual de cadência, sem inferir cadência ideal, overstriding, eficiência ou risco de lesão.

## 8. Por que a 038 vem primeiro

A descoberta-chave foi `21 corridas / 5 comparáveis / 16 fallback`. Antes de criar mais análises dependentes de distância comparável, é necessário entender se essas 16 podem ser recuperadas legitimamente.

A meta da 038 não é transformar 16 em 21. Resultados válidos do Gate A: `GO`, `PARTIAL-GO`, `NO-GO` ou `INCONCLUSIVO`.

## 9. Reprocessável não significa recuperável para native

Já existe infraestrutura de reprocessamento explícito desde a 014: `WorkoutActivityRepository.reprocessWatchWorkout(...)` e `RoomRepositories.reprocessWatchWorkoutInternal(...)`.

O fluxo reconhece `d4-v1`, `d4-v2`, `d4-v3` e `d4-v4`. Porém, legacy pode ser reprocessável e continuar baseado em comprimento de passo.

A derivação nativa atual exige `parserVersion == d4-v4` e `WatchWorkoutNativeDistancePolicy.isEligible(...) == true`. Só então pode produzir `WATCH_NATIVE` ou `WATCH_NATIVE_CONTEXT_CALIBRATED`.

Logo, snapshot fallback presente não prova recuperabilidade nativa.

## 10. Diagnostic payload histórico

Diagnostic payload pode preservar bytes físicos de importações antigas e já é usado em caminhos limitados, como recuperação autoritativa de duração.

A 038 deve auditar se payloads `d4-v1/v2/v3` sustentam uma interpretação moderna, mas não pode alterar `parserVersion`, relabelar legacy como v4, preencher fatos ausentes por heurística ou tratar mera presença de payload como prova.

Uma interpretação candidata precisaria fechar identidade, CRC, bloco, início, quantidade/índices, passos, BPM compatível quando disponível, duração/header, comparação com fatos persistidos e bytes inalterados.

Se uma provenance/version nova for necessária e o modelo atual não comportar, isso é blocker para decisão futura, não autorização para migration antecipada.

## 11. Política da 038 para SYNCED

O reprocessamento atual bloqueia `SYNCING` e `SYNCED`, além de projection children inseguros.

Foram consideradas: manter bloqueio, permitir derivação local divergente do payload remoto ou criar uma segunda derivação efêmera apenas para analytics.

O planejamento escolheu manter o bloqueio de recuperação canônica para `SYNCED`. Motivos: HC é write-only, payload/IDs/versões aceitos são históricos congelados, não há readback e uma segunda verdade analítica duplicaria pipeline/provenance.

Leitura em memória é permitida apenas para auditoria do Gate A, nunca como fonte user-facing da 037.

## 12. Taxonomia planejada da 038

- `NATIVO_ATUAL`: já atende 037.
- `V4_RECUPERAVEL_LOCALMENTE`: fonte v4 completa e determinística, mas snapshot ativo ainda não comparável.
- `LEGACY_COM_DIAGNOSTICO_POTENCIALMENTE_RECUPERAVEL`: existe payload candidato, sem prova completa/provenance segura.
- `LEGACY_SEM_FONTE_NATIVA`: sem fonte suficiente; permanece fallback.
- `ESTRUTURALMENTE_INELEGIVEL`: falha de integridade, header/série, CRC, índices, temporalidade, fatos nativos ou plausibilidade.
- `BLOQUEADO_POR_ESTADO_REMOTO`: fonte recuperável provada, mas workflow/projeção remota impede mutação.

`BLOQUEADO_POR_ESTADO_REMOTO` só pode ser usado depois de provar a fonte; não deve mascarar ausência de dados.

## 13. Snapshots, calibração, Room e HC na 038

Preservar `source/raw != measured != derived snapshot != Health Connect projection`.

Nunca editar snapshot histórico para fazê-lo parecer nativo. Recuperação futura autorizada cria snapshot novo, preserva o anterior, materializa derived intervals/splits do novo snapshot e troca ativo apenas em transação explícita/idempotente.

Recuperar fatos nativos é separado de aplicar calibração contextual. A direção conservadora é recuperar primeiro como `WATCH_NATIVE`, sem aplicar automaticamente perfil RUNNING atual, aprender perfil novo ou recalibrar outras atividades.

Room alvo: v20, sem migration. Se nova provenance exigir schema, abrir decisão específica.

Health Connect deve permanecer sem efeito: zero chamada remota, zero nova operação automática, zero alteração de `SYNCED`, zero reset de reconciliation, zero delete/retry/readback e zero mudança silenciosa de IDs, versões ou frozen payloads.

## 14. Planejamento da 038

Branch: `feat/038-historical-running-comparability`.

Base auditada: `e2d141276ef30ee846d8aafba2c69ad425dd3e99`.

Commit inicial de planejamento: `506206f2314b738f818cfb2a4df0780c841c7593` (`docs(spec): planejar comparabilidade histórica 038`).

Arquivos principais:

- `docs/specs/038-historical-running-comparability.md`;
- `docs/decisions/adr-038-historical-running-comparability.md`;
- `docs/tasks/038-historical-running-comparability/038-00-overview.md`;
- tasks `038-01` a `038-06`.

Estado inicial: `pending`, progresso `0/6`, Gate A `INCONCLUSIVO` até auditoria física read-only.

### Sequência das tasks

- `038-01`: auditar população real e congelar Gate A;
- `038-02`: preflight read-only, somente se Gate autorizar;
- `038-03`: recuperação local explícita, somente para classes aprovadas;
- `038-04`: integração com Exercícios e confirmação de incorporação natural pela 037;
- `038-05`: validar preservação, idempotência e ganho de comparabilidade;
- `038-06`: validação física Release incremental e fechamento.

## 15. Próximo passo formal

O próximo comando esperado é:

`Execute a task 038-01 da branch feat/038-historical-running-comparability.`

O objetivo principal da 038-01 é descobrir, no banco físico real e em modo read-only, a composição das 16 corridas fallback observadas na 037.

A 038 só deve seguir para implementação se o Gate A retornar `GO` ou `PARTIAL-GO`. `NO-GO` é legítimo e preserva fallback. `INCONCLUSIVO` significa falta de evidência física suficiente, não autorização para implementar por hipótese.