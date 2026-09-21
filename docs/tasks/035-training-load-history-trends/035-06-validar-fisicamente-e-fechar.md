# 035-06 — Validar fisicamente e fechar

- **Ticker:** `035`
- **Número:** `06`
- **Status:** `completed`

## Objetivo e resultado esperado

Validar a implementação da Spec 035 sobre banco e workouts existentes da Spec
034, comprovar fechamentos numéricos, recomposição pela FC máxima, ausência de
efeitos persistentes/remotos e registrar evidência sanitizada para fechamento.

## Requisitos cobertos

- Todos os critérios de aceite da Spec 035.
- Preservação de Room v20 e ausência de migration/tabelas de carga.
- Integração visual e screenshot sanitizada.
- Fechamento do overview e documentação de cenários N/A.

## Escopo incluído

- `git diff --check`.
- `./gradlew lint assembleDebug assembleRelease`.
- Verificação física com banco existente, sem treino novo.
- Cálculo independente de períodos, comparação, buckets, tipos e cobertura.
- Comparação antes/depois de atividade, workflow, snapshots e operações Health
  Connect.
- Reopen/recompose para provar ausência de persistência analítica.
- Screenshot sanitizada da nova seção.
- Atualização de status, checklist e observações do overview.

## Escopo excluído

- Criar workout/sample artificial ou alterar dados para fabricar cenário.
- Criar teste automatizado novo.
- Adicionar migration, tabela, snapshot ou operação remota para facilitar prova.
- Expor payload BLE, BPM individual, IDs pessoais ou dados sensíveis.
- Expandir escopo para Home, Saúde, Profile, parser, BLE ou workflow.

## Dependências

- `035-02`, `035-03`, `035-04` e `035-05` concluídas.
- Banco e workouts reais já existentes da Spec 034.
- Acesso a evidência física sanitizada e instalação debug.

## Arquivos e símbolos prováveis

- `docs/tasks/035-training-load-history-trends/035-00-overview.md`;
- `docs/specs/035-training-load-history-trends.md`;
- evidência de integração em caminho documental definido durante execução;
- `ChampionDatabase`, `health_sync_operations`, `WorkoutActivity` e
  `WorkoutDerivationSnapshot` apenas para inspeção.

## Passos de implementação/validação

1. Inspecionar diff e confirmar somente arquivos autorizados.
2. Rodar lint e builds debug/release com wrapper do projeto.
3. Confirmar Room `user_version = 20`, schema v20 e nenhuma migration nova.
4. Registrar FC máxima atual sem expor o valor se não for necessário.
5. Calcular individualmente cargas existentes e comparar com a policy 034.
6. Calcular independentemente current7, previous7, current28 e previous28.
7. Conferir deltas, percentual com baseline e baseline zero sem infinito.
8. Conferir soma dos buckets, soma por tipo, contagens, cobertura e
   denominador `totalWorkoutDuration`.
9. Confirmar workout sem análise, quando existir naturalmente; se não existir,
   registrar N/A e risco residual.
10. Alterar temporariamente FC máxima, confirmar recomposição, restaurar valor e
    confirmar restauração dos resultados.
11. Comparar atividade, `workflowState`, snapshot de derivação e assinatura/
    contagem de `health_sync_operations` antes/depois.
12. Reabrir/recompor Exercícios e confirmar que analytics não persistem.
13. Capturar screenshot sanitizada somente com a seção necessária.
14. Registrar evidência, N/A e riscos; marcar subtarefa e overview.

## Testes e comandos de validação

Comandos obrigatórios:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validações físicas obrigatórias:

1. Room v20 e nenhuma migration nova.
2. Cargas individuais iguais à 034.
3. Fechamento dos quatro períodos.
4. Deltas e percentuais corretos.
5. Baseline zero sem infinito/`NaN`.
6. Buckets e tipos fechando current28.
7. `totalWorkoutCount`, `analyzedWorkoutCount`, cobertura e ratio corretos.
8. Ausência de análise distinguível de dia sem workout.
9. Recalculo e restauração por FC máxima.
10. Nenhuma alteração em fatos, workflow, snapshot ou Health Connect.
11. Nenhuma persistência após reopen/recompose.
12. Screenshot sanitizada.

Não criar testes automatizados novos. Registrar N/A quando cenário não surgir
naturalmente, sem fabricar dados.

## Definição de pronto

- Todos os critérios exercitáveis possuem evidência ou N/A justificado.
- Nenhum critério numérico é liberado sem cálculo independente.
- Room permanece v20 e não há artefato de persistência analítica.
- Health Connect e fatos de workouts têm assinatura/estado preservados.
- Screenshot não contém dados sensíveis desnecessários.
- Overview mostra progresso e status finais coerentes.

## Riscos e cuidados

- Não alterar configuração pessoal permanentemente; restaurar valor original.
- Não criar dados apenas para completar checklist.
- Se falta de índice ou custo bounded aparecer, registrar risco e abrir decisão
  separada; não criar migration durante fechamento.
- Preservar N/A de activity sem samples, caso banco existente não ofereça esse
  cenário.
- Sanitizar dumps e screenshot; não registrar samples ou identificadores.

## Arquivos alterados

- `docs/integration/035-06-physical-validation.md` — evidência física
  sanitizada, cálculos independentes, recomposição e efeitos colaterais.
- `docs/integration/035-06-training-load-history.png` — screenshot sanitizada da
  seção histórica.
- Este arquivo — status, decisões, comandos, resultados e riscos residuais.
- `docs/tasks/035-training-load-history-trends/035-00-overview.md` — checklist,
  progresso e observações finais.
- `docs/specs/035-training-load-history-trends.md` — status final da Spec.
- `docs/decisions/adr-035-training-load-history-trends.md` — status da decisão
  após validação.

## Decisões e desvios

- Decisão: validar somente banco e workouts existentes, sem criar treino,
  sample, fixture ou alteração artificial de fatos.
- Decisão: usar cálculo independente externo para policy 034, quatro janelas,
  cobertura, buckets diários, tipos e baseline zero.
- Decisão: alterar temporariamente FC máxima por Perfil, restaurar o valor
  original e comparar atividade, workflow, snapshots e operações Health Connect
  por contagem e assinatura.
- Desvio: nenhum.
- N/A: não havia naturalmente workout sem samples, falha de leitura,
  configuração ausente ou período principal sem workouts; limitações ficaram
  explícitas na evidência.

## Comandos executados

- `git diff --check`
- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew installDebug`
- `adb devices -l`
- `adb exec-out run-as ... cat databases/champion-companion.db`
- `adb shell uiautomator dump /sdcard/window.xml`
- `adb exec-out screencap -p`
- consultas SQLite em cópias temporárias de `db`, `-wal` e `-shm`
- cálculo independente em Python SQLite
- force-stop/reopen do app e comparação de assinaturas

## Resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: instalado em 1 dispositivo.
- Room físico e schema exportado confirmaram v20; nenhuma tabela analítica,
  migration ou índice novo foi criado.
- Banco preservado com 8 atividades, 165 samples, 11 snapshots e 780 operações
  Health Connect; workflow agregado permaneceu `2 PENDING_REVIEW` e `6 SYNCED`.
- Cargas individuais e os quatro períodos fecharam com cálculo independente;
  buckets e tipos fecharam `current28`; baseline zero mostrou `Sem base anterior`.
- FC máxima recompôs `345,5 -> 355,0 -> 345,5` em current7 e
  `505,6 -> 528,4 -> 505,6` em current28.
- Assinaturas de `workout_activities`, `workout_derivation_snapshots` e
  `health_sync_operations` permaneceram iguais antes/depois e após reopen.
- Screenshot sanitizada: `docs/integration/035-06-training-load-history.png`.
- Não foram criados nem executados testes automatizados novos, conforme escopo
  da Spec 035 e `AGENTS.md`.

## Riscos residuais

- Cenários físicos sem samples, configuração ausente, falha de leitura e
  `NO_WORKOUTS` no período principal ficaram N/A por ausência natural; não houve
  fabricação de dados.
- Custo da query bounded em escala permanece risco documentado de `035-03`.
- Screenshot cobre estado disponível/parcial; estados N/A não foram fabricados
  apenas para obter imagens.
