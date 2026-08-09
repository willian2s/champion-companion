# 014-11 — Reprocessar atividades existentes explicitamente

- **Ticker:** `014`
- **Número:** `11`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Oferecer ação explícita e idempotente para derivar novamente atividades
`WATCH_D3_D4` existentes com o contrato atual, incluindo legado `d4-v1` quando o
raw for recuperável.

Resultado: atividades antigas podem receber novo snapshot sem mutação automática,
perda de revisão ou sincronização implícita.

## Dependências

- `014-08` concluída.

## Arquivos permitidos

- contratos/repository de derivação e reimportação;
- `RoomRepositories.kt` e DAOs necessários;
- `ExercisesViewModel.kt` e superfície de detalhe/lista para ação explícita;
- recursos de texto, screenshot e overview.

## Arquivos proibidos

- parser/protocolo, schema/migration e Health Connect writer;
- Gradle e manifesto;
- atividades phone, GPS, rota e WorkManager.

## Elegibilidade

### `d4-v2`

Usar raw/measured persistidos e o contrato de derivação vigente.

### `d4-v1`

Quando a migration de `014-03` preservou o valor legado como `rawMotionValue`:

```text
decodedSteps = rawMotionValue ushr 16
```

O reprocessamento cria measured/derived atuais sem alterar o valor legado.

### Falha fechada

- raw necessário ausente;
- parser version desconhecida;
- índices inelegíveis;
- perfil sem `stepLengthMeters` válido;
- inconsistência temporal não resolvível.

Não exigir BLE quando os fatos locais forem suficientes. Não reconstruir raw
com heurística não versionada.

## Preservação obrigatória

- `recordedAt`;
- revisão e tipo confirmado;
- título e observações;
- calibração histórica;
- raw e import identity;
- workflow compatível;
- operações Health Connect existentes até ação explícita futura.

Para `TREADMILL` de relógio, aplicar o contrato canônico vigente sem apagar a
calibração histórica.

## Fora do escopo

- lote silencioso, abertura do app ou scheduler;
- reparse remoto sem payload local;
- alterar distância original/raw;
- sincronizar ou reconciliar Health Connect;
- testes automatizados.

## Passos de implementação

1. Listar atividades sem snapshot atual ou com versão antiga.
2. Expor ação com resumo e confirmação explícita.
3. Resolver raw/measured conforme parser version conhecida.
4. Aplicar decoder atual a `d4-v1` recuperável.
5. Criar snapshot transacional com versão atual.
6. Preservar campos de usuário e ordem temporal da ADR-016.
7. Não preparar nem enviar operações Health Connect automaticamente.
8. Para `SYNCED`, exigir aviso específico antes de qualquer projeção futura.
9. Tornar repetição com mesmos inputs/versão idempotente.
10. Registrar screenshots e atualizar overview.

## Critérios de aceite

- Nenhuma atividade muda sem confirmação.
- `d4-v1` recuperável pode ser derivada sem BLE.
- Parser version desconhecida falha com motivo seguro.
- Mesmos inputs e versão não criam revisão inútil.
- Revisão, `recordedAt`, título, notas e calibração permanecem.
- Atividade sem perfil continua sem derivação.
- Reprocessamento não acessa Health Connect nem ressincroniza.
- `TREADMILL` usa a nova série sem perder histórico de calibração.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots da confirmação e resultado;
- matriz parser version/estado → resultado;
- registro sanitizado de preservação de campos;
- repetição idempotente e reinício do app.

## Definição de pronto

Reprocessamento explícito, idempotente e local funciona para legado recuperável;
item `014-11` marcado.

## Implementação e evidências

- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`;
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`;
  - este arquivo e `014-00-overview.md`.
- **Decisões e desvios:**
  - Falhas fechadas usam motivos tipados e mensagens sanitizadas, sem expor raw ou
    detalhes de persistência na interface.
  - `d4-v1` aplica `rawMotionValue ushr 16` somente no preflight/derivação em
    memória; `resolvedRawIntervals` preserva `decodedSteps = null` e o valor raw
    legado no banco.
  - `SYNCED` recebe confirmação com aviso específico, mas permanece bloqueada até
    a projeção futura; nenhuma operação Health Connect é criada.
  - Snapshot ausente, versão antiga e `SPLITS_NOT_MATERIALIZED` exibem ação
    explícita. Legado sem marcador seguro de ordem continua fail-closed conforme
    ADR-018.
  - Não foram adicionados recursos novos: superfície de Exercícios mantém textos
    locais existentes e não houve alteração de parser, mapper, schema ou writer.
- **Comandos executados:**
  - `./gradlew app:compileDebugKotlin`;
  - `./gradlew lint assembleDebug assembleRelease`;
  - `./gradlew installDebug`;
  - `git diff --check`;
  - `git status --short`.
- **Resultados e evidências:**
  - Uma compilação intermediária falhou durante a integração inicial do contrato
    transacional; a correção foi aplicada antes da validação final.
  - `app:compileDebugKotlin`, lint, builds debug/release, instalação debug e
    verificação de whitespace passaram.
  - Instalação concluída no Galaxy S22 `SM-S901E`.
  - Revisão independente encontrou e corrigiu a ausência de ação para
    `SPLITS_NOT_MATERIALIZED`; revisão final confirmou atomicidade, preservação,
    idempotência, bloqueio seguro de `SYNCED` e ausência de Health Connect.
  - Não foram criados nem executados testes automatizados. Screenshot funcional
    do diálogo/resultado não foi capturada porque dispositivo não possuía
    atividade elegível para abrir esse fluxo.
- **Riscos residuais:**
  - Validação física de reprocessamento `d4-v1`, preservação após reinício e
    screenshots funcionais permanecem para `014-15`.
  - Atividades legadas sem marcador persistido de ordem continuam bloqueadas sem
    inferência pelo `ORDER BY`.
  - Projeções e ressincronização Health Connect permanecem fora desta subtarefa.

- `WorkoutActivity.requiresWatchWorkoutReprocessing` também considera
  `SPLITS_NOT_MATERIALIZED` quando snapshot usa versão vigente.
- Exercícios exibe ação e confirmação explícitas para materializar splits
  ausentes; nenhuma ação ocorre sem confirmação.
- `d4-v1` mantém `decodedSteps` nulo em `workout_intervals`: a persistência usa
  `resolvedRawIntervals`; intervalos preparados servem somente ao preflight.
- `./gradlew compileDebugKotlin` — PASS.
- Testes automatizados não foram criados nem executados, conforme escopo.
- Mapper, parser, schema e Health Connect não foram alterados nesta correção.

## Diagnóstico da falha agregada de lint

- **Causa determinada:** não há causa real no diff de `014-11`. A falha
  reportada ocorreu durante a execução agregada, na análise de lint de fontes
  de teste, com artefato gerado Hilt ausente e erro FIR/lint; não há alteração
  em Gradle ou testes que explique regressão. O padrão é compatível com estado
  transitório de geração/cache do grafo de lint.
- **Validação focada:** `./gradlew lint` isolado concluiu com `BUILD SUCCESSFUL`
  em 16 s. Após regeneração forçada parcial (`./gradlew lint --rerun-tasks`,
  interrompido pelo limite de 120 s sem erro de lint reportado), nova execução
  isolada concluiu com `BUILD SUCCESSFUL` em 1 min 42 s; `lintAnalyzeDebugUnitTest`
  e `lintAnalyzeDebugAndroidTest` ficaram `UP-TO-DATE`, e `lintAnalyzeDebug`
  concluiu normalmente.
- **Evidência de artefato:** `app/build/generated/hilt/component_trees/release/`
  existe após a validação; não houve nova `FileNotFoundException` nem erro FIR.
  O mesmo padrão de artefato Hilt ausente e recuperação por lint focado já está
  registrado em `docs/integration/013-08-foreground-service-recovery-review.md`.
  `git diff --check` passou.
- **Decisão:** nenhuma correção de código, Gradle ou testes foi aplicada. Não
  foram criados nem executados testes automatizados.

## Rollback

Ocultar a ação e parar novos snapshots. Preservar snapshots já criados e não
alterar estados remotos.
