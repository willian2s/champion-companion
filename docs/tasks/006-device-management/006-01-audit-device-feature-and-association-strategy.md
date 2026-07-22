# Task 006-01 — Auditar feature de dispositivo e estratégia de associação

## Status

Concluida em 2026-07-21.

## Objetivo

Auditar estado atual de `feature/device`, BLE core, domínio, Room e data layer; confirmar estratégia mínima e sustentável para associação de um Champion C001.

## Contexto

Fase 05 deixou PoC debug funcional, mas tela release segue placeholder e `DeviceRepository` só observa associado. `DeviceEntity` V1 contém `associatedAtEpochMillis`, mas esquecer dispositivo sem apagar histórico exige separar associação atual de identidade histórica.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/specs/_inputs/phase-06-device-management-direction.md`
- `docs/specs/003-domain-and-persistence-foundation.md`
- `docs/tasks/003-domain-and-persistence-foundation/003-00-overview.md`
- `docs/decisions/adr-003-local-persistence-architecture.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`

## Dependências

- Nenhuma task anterior da Fase 06.

## Entradas

- Código atual em `feature/device`, `core/bluetooth`, `core/model`, `core/database` e `data`.
- Schema Room V1 exportado.
- Falhas automatizadas conhecidas herdadas da Fase 05.

## Arquivos Exatos Permitidos

- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`

## Arquivos Proibidos

- Qualquer arquivo `.kt`.
- Qualquer arquivo Gradle.
- `docs/core/current-project-baseline.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/**`
- Specs/tasks de fases concluídas.
- `docs/specs/_inputs/phase-06-device-management-direction.md`

## Escopo

- Confirmar gaps para associação, esquecimento, última conexão e última sincronização.
- Documentar decisão recomendada: entidade própria de associação.
- Registrar necessidade de migration e possível ADR.
- Ajustar spec/overview apenas se auditoria encontrar divergência concreta.

## Fora Do Escopo

- Implementar Kotlin.
- Criar migration.
- Rodar BLE, scan ou conexão.
- Criar ADR.
- Alterar baseline.

## Passos

1. Ler documentos obrigatórios.
2. Inspecionar `DeviceEntity`, `DeviceDao`, `DeviceRepository`, mappers e `RoomRepositories`.
3. Comparar alternativas: evolução da entidade, ponteiro DataStore, entidade de associação.
4. Confirmar impacto em histórico futuro e comportamento ao esquecer.
5. Atualizar esta task com achados e decisão final se necessário.
6. Atualizar checklist do overview.

## Critérios De Aceite

- Estratégia de associação documentada com justificativa.
- Impacto Room e migration documentados.
- Comportamento ao esquecer documentado.
- Relação com dados históricos documentada.
- Possível ADR identificado.

## Validação Mínima

- Leitura e inspeção estática somente.
- `git diff -- docs/specs/006-device-management.md docs/tasks/006-device-management/006-00-overview.md docs/tasks/006-device-management/006-01-audit-device-feature-and-association-strategy.md`.

## Evidência Manual Esperada

- Lista de arquivos inspecionados.
- Resumo da alternativa escolhida.
- Confirmação de que nenhum código foi alterado.

## Atualização Do Overview

- Marcar `006-01` como concluída no checklist.
- Registrar qualquer risco novo em “Arquivos Com Risco De Conflito” ou “Dívida Técnica Adiada”.

## Validação Final

- Confirmar `git status --short`.

## Auditoria Executada

### Arquivos Inspecionados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DevicePocCoordinator.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidChampionBleScanner.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/DeviceProfileDailySummary.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/1.json`

### Achados

1. `feature/device` ainda nao implementa feature final.
   - `DeviceScreen` release segue placeholder.
   - Fluxo debug ainda monta `AndroidChampionBleScanner`, `AndroidChampionBleClient`, resolver e `ChampionConnectionManager` dentro do Composable.
   - `DevicePocCoordinator` continua orientado a PoC tecnica, nao a associacao persistida e estado final de produto.

2. Core BLE ja sustenta fluxo funcional futuro, mas ainda sem semantica de associacao.
   - `ChampionConnectionManager` faz scan, conecta, discovery, resolve capabilities, sobe subscriptions default e emite `TransportReady`.
   - `AndroidChampionBleScanner` hoje encerra scan no primeiro resultado Champion filtrado, embora retorne lista. Isto confirma risco ja previsto para evolucao de lista real de candidatos.

3. `DeviceRepository` esta incompleto para Fase 06.
   - Contrato atual expoe apenas `observeAssociatedDevice()`.
   - `RoomRepositories` implementa somente observacao.
   - `DeviceDao.findByPlatformIdentifier()` existe, mas nao sobe para contrato de dominio nem para repository.

4. V1 mistura identidade historica e associacao atual em `DeviceEntity`.
   - `DeviceEntity` possui `associatedAtEpochMillis` obrigatorio.
   - `DeviceDao.observeAssociated()` resolve associado por `SELECT * FROM devices ORDER BY associatedAtEpochMillis DESC LIMIT 1`.
   - `Device.create(...)` exige `associatedAt` e ainda valida `lastSuccessfulConnectionAt` e `lastImportedAt` a partir dele.
   - Resultado: associacao atual depende do proprio registro historico do dispositivo, nao de vinculo separado.

5. Esquecer dispositivo sem apagar historico nao cabe bem no modelo atual.
   - `DailySummary`, `HeartRateMeasurement`, `WorkoutActivity`, `WatchWorkoutImport` e `DiagnosticPayload` ja podem referenciar `DeviceEntity` por FK.
   - Se esquecer significar apagar `devices`, historico quebra ou fica sujeito a `SET NULL`/`RESTRICT`, contrariando escopo da fase.
   - Se esquecer significar apenas zerar ou nulificar `associatedAt`, V1 exige mudar regra de dominio, entidade, query de associado e semantica de um campo hoje historico.

### Alternativas Comparadas

1. Evoluir `DeviceEntity` com flag/campo de associacao atual.
   - Pro: menos tabelas.
   - Contra: continua misturando identidade historica com vinculo operacional; esquecer e substituir exigem updates delicados no mesmo registro historico; piora semantica de `associatedAt` legado.

2. Guardar ponteiro de associado em DataStore.
   - Pro: implementacao pequena.
   - Contra: associacao deixa Room, quebra fonte operacional definida em ADR-003, perde integridade transacional com `upsert device` e atualizacao de timestamps, cria estado repartido entre banco e preferencia.

3. Criar entidade propria de associacao.
   - Pro: separa identidade historica de vinculo atual; esquecer vira remocao local da associacao; preserva FKs historicas para `devices`; mantem Room como fonte operacional; favorece transacao clara para associar/substituir/atualizar ultima conexao.
   - Contra: exige migration `1 -> 2`, DAO novo, repository mais amplo e schema exportado V2.

### Decisão Recomendada

Adotar entidade propria de associacao, singleton primaria, em Room.

Modelo minimo recomendado:

```text
DeviceEntity
  id
  platformIdentifier
  name
  model
  firmware
  associatedAtEpochMillis   // legado V1; manter ate migracao semantica futura
  lastSuccessfulConnectionAtEpochMillis
  lastImportedAtEpochMillis
  importParserVersion

DeviceAssociationEntity
  id = "primary"
  deviceId
  associatedAtEpochMillis
  updatedAtEpochMillis
```

### Justificativa Final

- Preserva `DeviceEntity` como identidade historica reutilizavel por dados futuros.
- Permite esquecer sem apagar `devices` nem tocar FKs historicas.
- Mantem associacao persistida separada de sessao BLE efemera, como exige spec 006.
- Mantem Room, nao DataStore, como fonte operacional conforme ADR-003.
- Simplifica semantica de substituicao: conectar com sucesso, fazer upsert de `DeviceEntity`, trocar associacao singleton na mesma transacao.

### Comportamento Ao Esquecer

- Exigir confirmacao na UI.
- Desconectar sessao efemera quando existir.
- Remover apenas `DeviceAssociationEntity` singleton.
- Preservar `DeviceEntity` e qualquer dado historico futuro ja vinculado por FK.
- Retornar UI para estado sem dispositivo associado.

### Última Conexão E Última Sincronização

- `lastSuccessfulConnectionAtEpochMillis` continua no `DeviceEntity` e deve atualizar somente apos conexao funcional completa.
- `lastImportedAtEpochMillis` continua em `DeviceEntity` como metadado historico de importacao, nao substituto de associacao atual.
- Ultima sincronizacao futura segue derivada de `DeviceImportMetadata.lastImportedAt`, sem copiar ultima conexao.

### Impacto Em Room E Migration

- Necessaria migration `1 -> 2`.
- Criar tabela `device_associations` com PK singleton e FK para `devices(id)`.
- Ajustar `ChampionDatabase`, DAOs, repository e export de schema Room V2.
- Como V1 ainda nao teve feature final de associacao entregue ao usuario, migration pode criar tabela vazia sem backfill obrigatorio.

### Necessidade De ADR

- Recomendada ADR propria quando task de implementacao da persistencia entrar, porque decisao separa de forma duradoura identidade historica, associacao persistida e sessao BLE efemera.
- Nao criada nesta task por estar fora de escopo.

### Divergencia Encontrada Na Spec

Nenhuma divergencia concreta encontrada em `docs/specs/006-device-management.md`. Spec ja aponta estrategia correta. Nenhum ajuste necessario nela nesta task.

### Evidencia Final

- Estrategia escolhida: entidade propria de associacao em Room.
- Codigo Kotlin nao foi alterado.
- Mudancas desta task ficaram restritas a documentacao permitida.
