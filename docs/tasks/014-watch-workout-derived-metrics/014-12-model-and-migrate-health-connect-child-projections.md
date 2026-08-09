# 014-12 — Modelar e migrar projeções filhas Health Connect

- **Ticker:** `014`
- **Número:** `12`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Modelar uma operação persistida por filho Health Connect e executar uma evolução
Room aditiva separada da migration local de métricas.

Resultado: manifesto, payload congelado, identidade natural, ações e checkpoints
estão persistidos antes de qualquer fan-out ou deleção remota.

## Dependências

- `014-10` concluída.
- `014-03` registrou a versão efetiva da migration local.

## Arquivos permitidos

- contratos de sync/projeção em `core/model`;
- `ChampionDatabase.kt`, DAOs, migrations e `DatabaseModule.kt`;
- `DatabaseMappers.kt` e `RoomRepositories.kt` para persistência da fila;
- novo schema exportado;
- `docs/baseline/database-reference.md`;
- `docs/baseline/health-connect-mapping.md`;
- overview.

## Arquivos proibidos

- parser, perfil, UI e writer/gateway remoto;
- Gradle e manifesto;
- alteração do schema exportado da migration local anterior;
- GPS, rota, `ExerciseRoute` e código phone.

## Modelo obrigatório

Cada filho possui sua própria operação:

```text
aggregate + recordType + projectionChildKey
```

Exemplos:

```text
EXERCISE_SESSION / singleton
DISTANCE / interval-000
DISTANCE / interval-001
SPEED / series
HEART_RATE / series
TOTAL_CALORIES_BURNED / singleton
```

Persistir pelo menos:

- `projectionChildKey`;
- ação `INSERT` ou `DELETE`;
- payload congelado suficiente para retry;
- `clientRecordId` e `clientRecordVersion`;
- estado, tentativas e erro seguro;
- conjunto esperado/enviado/obsoleto ou estrutura equivalente;
- checkpoint de reconciliação;
- vínculo com a derivação/projeção que originou o payload.

## Regras

1. Não criar uma operação agregada com subfila interna de vários records.
2. Retry lê payload congelado; não recalcula atividade mutável.
3. ID não contém valor de saúde.
4. Versão é monotônica por `clientRecordId`.
5. Retry idêntico mantém ID, versão e payload.
6. Operações legadas permanecem preservadas e distinguíveis.
7. Migration não executa insert/delete remoto.
8. Migration não deriva atividade nem altera workflow.

## Fora do escopo

- mapear `DistanceRecord` ou `SpeedRecord`;
- inserir/deletar no provider;
- reconciliar agregado legado;
- WorkManager/background;
- testes automatizados.

## Passos de implementação

1. Fechar contratos de manifesto e filho a partir da ADR-018.
2. Definir a identidade natural com `projectionChildKey`.
3. Definir payload congelado e regras de versão.
4. Criar migration aditiva na próxima versão disponível.
5. Preservar operações legadas com defaults seguros.
6. Adicionar DAOs, mappers e transações de preparação.
7. Garantir unicidade de filho por projeção.
8. Exportar somente o novo schema.
9. Atualizar baselines e overview.

## Critérios de aceite

- Cada filho pode falhar e retomar de forma independente.
- Não existe subfila escondida em payload agregado.
- Operação contém payload suficiente para retry sem ler métricas mutáveis.
- IDs e versões respeitam ADR-018.
- Migration abre banco legado sem destructive migration.
- Nenhum efeito remoto ocorre durante upgrade.
- Schemas históricos permanecem imutáveis.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- matriz tipo/filho/identidade/payload;
- diff da migration e schema;
- round-trip de operações preparadas;
- abertura sanitizada de banco legado;
- builds.

## Implementação

- Contratos em `core/model` modelam chave filha, manifesto, ação, payload
  congelado, origem, estado de reconciliação e checkpoints.
- `HealthSyncRepository.prepareProjectionChild` prepara cada filho em transação
  local; exige payload congelado, origem e versão monotônica antes de retornar a
  operação.
- `ensureOperation` rejeita chaves filhas antes de qualquer insert; isso impede
  linha parcial sem payload e mantém `prepareProjectionChild` como única entrada
  de preparação granular.
- `prepareProjectionChild` valida a correspondência entre `recordType` e filho e
  materializa o round-trip de domínio antes do insert; incompatibilidades falham
  fechado sem contaminar a fila.
- `health_sync_operations` recebeu colunas aditivas em `MIGRATION_9_10`; linhas
  legadas usam a chave sentinela `__legacy__` e defaults seguros, permanecendo
  distinguíveis sem abrir exceção de unicidade SQLite para `NULL`.
- A fila legada filtra `__legacy__` antes de aplicar `LIMIT`; filhos preparados
  não consomem capacidade nem chegam ao writer legado bloqueado.
- DAO, mappers e `RoomRepositories` preservam round-trip, unicidade por
  agregado/tipo/filho, IDs legados e IDs de filho sem valores de saúde.
- `markStarted` e `markSynced` persistem checkpoints antes e depois do efeito
  remoto futuro; o domínio rejeita filho `SYNCED` sem checkpoint concluído.
  Nenhum efeito remoto é executado nesta subtarefa.
- O bloqueio do writer legado de `WATCH_D3_D4` permanece intacto. Nenhum writer,
  gateway ou mapeamento remoto de `Distance`/`Speed` foi adicionado.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/10.json`;
- `docs/baseline/database-reference.md`;
- `docs/baseline/health-connect-mapping.md`;
- este arquivo e o overview da spec.

## Decisões e desvios

- `projectionChildKey` usa valores estáveis sem dados de saúde; intervalos usam
  `interval-<índice>` e operações seriadas usam chaves fixas.
- A operação filha exige payload congelado e vínculo de origem; operações
  legadas permanecem compatíveis com payload/origem nulos.
- IDs legados não foram recalculados. Somente IDs de filhos recebem o sufixo da
  chave filha.
- A preparação foi adicionada ao contrato do repositório para impedir que
  `ensureOperation` crie filho sem payload suficiente para retry.
- Não foram alterados parser, perfil, UI, writer, gateway, Gradle, manifesto,
  rotas, código phone ou schemas históricos.

## Comandos executados e resultados

- `./gradlew app:compileDebugKotlin` — PASS após a correção de compatibilidade do
  filho; somente warnings preexistentes de
  override deprecated e índice Room.
- `./gradlew lint assembleDebug assembleRelease` — PASS após a correção do gate
  de preparação filha.
- `./gradlew installDebug` — PASS; APK instalado no Galaxy S22 `SM-S901E`.
- `git diff --check` — PASS.
- `git status --short` — evidência registrada após validação final.
- Testes automatizados — não criados nem executados, conforme escopo.

## Evidências

- `MIGRATION_9_10` adiciona colunas e substitui índice sem operação remota.
- `10.json` registra banco v10 e índice único
  `aggregateId/aggregateType/recordType/projectionChildKey/operationSequence`.
- `HealthSyncOperationEntity.toDomain` e `toEntity` preservam chave, payload,
  ação, versão, estado, origem e checkpoints.
- `prepareProjectionChild` persiste uma linha por filho e reutiliza a mesma
  linha, ID, versão e payload em retry idêntico.
- Baselines registram matriz de tipos/filhos e defaults da migration.
- Revisão independente final: aprovada sem blockers ou majors; ressalva
  documental de progresso histórico da spec permanece fora dos arquivos
  permitidos desta subtarefa.

## Riscos residuais

- Abertura física sanitizada de banco legado e round-trip em dispositivo não
  foram executados, pois testes automatizados estão fora do escopo.
- Writer/gateway Health Connect ainda não processa filhos; implementação fica
  para `014-13`, mantendo o bloqueio atual de `WATCH_D3_D4`.
- Reconciliação de agregado legado e deleção remota ficam para `014-14`.
- A spec principal ainda exibe progresso histórico `11/16`; o overview desta
  task foi atualizado para `12/16`, conforme regra operacional desta subtarefa.

## Definição de pronto

Modelo e migration de projeções filhas estão prontos para o writer de `014-13` e
o item `014-12` está marcado.

## Rollback

Antes de release, reverter a migration. Após distribuição, usar migration forward
e preservar operações/projeções já persistidas.
