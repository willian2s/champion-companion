# 021-03 — Restaurar derivação no candidate semântico da fila

- **Ticker:** `021`
- **Número:** `03`
- **Status:** `in_progress`

## Objetivo

Eliminar o falso `UNRESOLVED_AGGREGATE` do calendário reconstruindo candidates de
`WORKOUT_ACTIVITY` com o snapshot de derivação ativo e a série canônica
persistida, sem reparar fila durante leitura e sem executar qualquer efeito
Health Connect.

## Resultado esperado

Operações `DISTANCE` e `SPEED` semanticamente válidas deixam de parecer
irresolvidas apenas porque o candidate builder omitiu a derivação. O calendário
volta a mostrar o status local real e só exibe `Dados parciais` quando existir
uma issue verdadeira.

## Contexto factual

No código atual:

```text
observeHealthSyncSemanticCandidateIndex()
    ↓
HealthSyncWorkoutCandidateRecord.toHealthSyncCandidateDomain()
    ↓
activity.toDomain(
    intervals = emptyList(),
    derivationSnapshot = null
)
```

Depois, `isWorkoutActivityEligibleForDistance()` exige snapshot elegível e
intervalo canônico correspondente; `isWorkoutActivityEligibleForSpeed()` também
exige snapshot elegível. O candidate é, portanto, construído sem o dado que o
próprio validador exige.

No banco local real da investigação:

- 84 operações existem;
- 84 estão `SYNCED`;
- nenhuma possui `lastError`;
- nenhuma está sem `clientRecordId`;
- nenhuma está sem `clientRecordVersion`.

A persistência da fila não é a causa do banner.

## Dependências

- Spec 021;
- Fase 014 e projeções filhas concluídas;
- Spec 018 continua fechada e independente;
- não depende funcionalmente de `021-01` ou `021-02`.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- contratos/records de `HealthSyncWorkoutCandidateRecord` e
  `WorkoutDerivationRecord`;
- `LocalCalendarRepository.kt` somente para confirmar contrato, sem mudar UI;
- spec/overview/task 021.

## Escopo incluído

- carregar/observar derivação ativa necessária aos candidates de treino;
- incluir intervalos derivados/série canônica suficientes para resolver filhos
  `DISTANCE`;
- incluir snapshot suficiente para resolver `SPEED`;
- reconstruir `WorkoutActivity` com o mesmo estado persistido usado pelos
  predicados semânticos;
- preservar comportamento de `EXERCISE_SESSION`, calorias e BPM;
- preservar filtros de operações legadas bloqueadas;
- manter `hasInvalidOperation` e `hasUnresolvedAggregate` para casos realmente
  inválidos;
- evitar N+1 desnecessário quando houver consulta/Flow agregada compatível;
- manter leitura reativa local-only.

## Escopo excluído

- mudar estado de `health_sync_operations`;
- recriar IDs/versões/payloads;
- disparar retry ou sync;
- ler provider Health Connect;
- alterar parser/timeline D4;
- esconder banner na UI;
- transformar operação inválida em válida por tolerância;
- migration de fila sem necessidade factual;
- testes automatizados.

## Passos de execução

1. Reproduzir o candidate atual sem derivação no mesmo checkout.
2. Mapear a menor consulta/combinação que forneça snapshot ativo + intervalos
   derivados para o índice semântico.
3. Hidratar `WorkoutActivity` com derivação antes dos predicados de elegibilidade.
4. Confirmar que ausência real de snapshot continua não resolvida quando uma
   operação realmente depende dele.
5. Confirmar que leitura do calendário não executa escrita.
6. Validar a fila local existente contra o novo índice semântico.
7. Revisar custo/reatividade para evitar consulta individual por operação.

## Critérios de aceite

1. Candidate de treino sincronizado com derivação ativa contém snapshot e série
   canônica antes da avaliação semântica.
2. `DISTANCE` filho válido resolve o intervalo correspondente.
3. `SPEED` válido resolve quando snapshot/payload cumprem o contrato.
4. Operação `SYNCED` válida permanece `SYNCED`; nenhuma linha da fila é reparada
   pela leitura.
5. No banco de referência, as 84 operações persistidas não geram falso
   `UNRESOLVED_AGGREGATE` por omissão de derivação.
6. Operação órfã, pair não suportado ou snapshot realmente ausente continua
   degradando fail-closed.
7. Calendário não consulta BLE nem Health Connect remoto.
8. Nenhuma mudança visual é necessária para resolver o problema.
9. Spec 018 não é reaberta nem seu fix de `recordedAt/reviewedAt` revertido.

## Validação manual

Com o banco preservado:

- abrir Calendário offline/local-first;
- confirmar ausência do banner quando não houver outra issue real;
- abrir o dia com atividades sincronizadas;
- confirmar `Status da fila local` coerente com o estado persistido;
- confirmar que nenhuma operação muda de `updatedAt`, `state`, versão ou payload
  apenas por navegar no calendário.

## Comandos permitidos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar nem executar testes automatizados.

## Definição de pronto

- candidate semântico hidratado com derivação ativa;
- falso unresolved eliminado no banco real;
- degradação real preservada;
- leitura sem side effects confirmada;
- overview e evidências atualizados;
- revisão independente sem bloqueadores.

## Riscos e rollback

- observar derivação global de forma ineficiente pode aumentar custo do
  calendário; preferir consulta agregada e dados estritamente necessários;
- mapper permissivo pode esconder corrupção real; manter os mesmos predicados e
  apenas fornecer dados faltantes;
- rollback deve voltar ao read model anterior sem modificar nenhuma linha da
  fila, pois a task não autoriza writes corretivos.

## Evidências de implementação

- `WorkoutDetailDao` e `WorkoutDerivationDao` passaram a expor flows agregados
  para intervalos raw e records ativos de derivação.
- O índice semântico agrupa dados por `activityId`/`snapshotId` e reconstrói o
  candidate com fatos raw, snapshot ativo e filhos derivados antes dos
  predicados de elegibilidade.
- O record ativo é observado com relação transacional, e múltiplos snapshots
  ativos para a mesma atividade não são colapsados silenciosamente.
- A hidratação usa ponte em memória para `d4-v1`, preservando a validação
  parser-aware de `d4-v2`/`d4-v3` sem alterar dados persistidos.
- Ausência, corrupção, orfandade e pares não suportados continuam falhando
  fechados; nenhuma operação ou payload da fila é alterado.
- Validações executadas: `git diff --check` e
  `./gradlew lint assembleDebug assembleRelease` — sucesso; `installDebug`
  também concluiu.
- Nenhum teste automatizado foi criado ou executado. Validação física integrada
  permanece pendente para `021-04`; revisão independente foi solicitada e
  aprovada com ressalva factual.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-00-overview.md`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-03-restore-health-sync-semantic-candidate-derivation.md`

## Decisões e desvios

- Foi usado `observeActiveRecords()` transacional para carregar snapshot e
  filhos derivados no mesmo read model, evitando consultas individuais por
  operação e emissões parciais entre tabelas relacionadas.
- A preparação parser-aware dos intervalos ocorre somente em memória no índice:
  `d4-v1` recupera `decodedSteps` de `rawMotionValue`; `d4-v2`/`d4-v3` exigem a
  igualdade persistida. Nenhuma coluna raw/measured é reescrita.
- Snapshots ativos duplicados por atividade são omitidos do mapa semântico, em
  vez de selecionar uma linha arbitrariamente; a atividade continua fail-closed
  para operações que exigem derivação.
- Não houve migration, mudança de fila, alteração de payload, efeito remoto,
  mudança visual ou alteração em Spec 018.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou; execução final em
  4m30s; warnings
  preexistentes de índice Room e overrides deprecated não bloquearam o build.
- `./gradlew installDebug` — passou; APK instalado em um dispositivo conectado
  (`SM-S901E`, Android 16).
- Testes automatizados — não executados, conforme escopo da subtarefa e regras
  do repositório.

## Resultados e evidências

- Compilação debug/release e lint concluídos com sucesso após a correção da
  hidratação `d4-v1` e da leitura transacional dos records ativos.
- O índice semântico permanece local-only e reativo; o diff não contém writes em
  `health_sync_operations`, chamadas Health Connect ou leitura BLE.
- A validação física com banco preservado e a confirmação das 84 operações sem
  falso `UNRESOLVED_AGGREGATE` permanecem para `021-04`.
- Revisão independente aprovou a implementação estática após a correção da
  ponte `d4-v1`, sem bloqueador no diff; a ressalva é a validação factual
  integrada ainda não executada.

## Riscos residuais

- Flows agregados carregam dados locais relacionados em memória; não há N+1,
  mas custo global pode crescer com histórico grande.
- Validação física no dispositivo, estabilidade das operações existentes e
  ausência observável do banner ainda não foram executadas nesta subtarefa.
- Revisão independente foi concluída com aprovação estática e ressalva de
  evidência física; status só deve virar `completed` após essa evidência.
