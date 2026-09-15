# 033-04 — Compor dados diários FEA1 + D1 + D4

- **Status:** completed
- **Depende de:** 033-02 e 033-03
- **Evidência física de entrada:** `docs/integration/033-03-d1-v2-physical-validation.md`

## Objetivo

Criar read model diário medido sem dupla contagem entre FEA1, D1 e D4, usando
autoridade por finalidade/métrica em vez de suprimir buckets D1 por mera
interseção temporal com workout.

## Correção obrigatória do contrato anterior

A política abaixo está rejeitada:

```text
bucket D1 intersecta D4
=> suprimir bucket D1 inteiro para passos, distância, calorias e BPM
```

Validação física mostrou dois comportamentos simultâneos:

- corrida D4 válida cujos passos e distância já estavam integralmente presentes
  nos buckets D1 correspondentes;
- workout D4 com métricas locomotoras nulas que ainda intersectava bucket D1 com
  passos, distância e calorias não nulos.

Logo, interseção temporal não prova exclusividade nem duplicidade por si só.

## Escopo

### Passos diários

- `DailySummary.steps`/FEA1 permanece o total diário autoritativo;
- D1 fornece a distribuição temporal/granular do total observado;
- `Σ D1.steps` deve ser comparado com FEA1 para cobertura/diagnóstico;
- D4 continua autoritativo para passos e cadência do próprio workout;
- passos D4 não são somados novamente ao agregado diário;
- divergência D1 × FEA1 deve permanecer observável e não ser corrigida por
  substituição silenciosa de header/workout.

### Distância diária

- D1 fornece a linha nativa diária de distância quando a cobertura for elegível;
- D4 fornece a distância canônica do workout, inclusive
  `WATCH_NATIVE_CONTEXT_CALIBRATED` quando aplicável;
- não calcular `D1 + D4` como soma diária;
- não reconstruir nova distância diária por `steps × stepLength` quando D1 nativo
  elegível existir;
- uma eventual substituição/ajuste da contribuição D1 pelo D4 exige prova local
  determinística de cobertura e não duplicação;
- sem essa prova, o diário preserva D1 e o detalhe do workout preserva D4.

### Calorias diárias

- D1 fornece fatos locais diários com escala já validada `raw / 10.0`;
- D4 fornece calorias próprias do workout com escala `raw / 1000.0`;
- não somar D1 e D4 para formar total diário;
- manter a projeção Health Connect de calorias bloqueada até a semântica ser
  fechada.

### Frequência cardíaca

- D1 continua fornecendo pontos automáticos válidos do dia;
- D4 fornece amostras do workout;
- ponto D1 dentro da janela do workout só é omitido do read model combinado se
  houver cobertura D4 válida de frequência cardíaca para aquela atividade;
- ausência de cobertura D4 válida não autoriza eliminar D1;
- não inventar interpolação ou BPM sintético.

### Cobertura e provenance

- expor cobertura/completude suficiente para distinguir dado diário completo,
  parcial e divergente;
- preservar provenance FEA1/D1/D4 nos modelos internos;
- header D4 continua consistência/diagnóstico, nunca série comportamental;
- nenhuma composição altera snapshots de workout já persistidos.

### Consultas

- disponibilizar consultas/read models por dia e período para Home, Saúde e
  Calendário;
- histórico legacy continua legível sem reinterpretar fatos antigos ausentes.

## Critério de substituição D1 -> D4

Substituição de uma métrica diária por D4 não pode depender apenas de overlap de
janela. Exige evidência suficiente para provar que a contribuição D1 substituída
corresponde ao mesmo fato representado no D4.

A primeira implementação pode optar por não fazer substituição para distância e
calorias se essa prova não estiver disponível. Nesse caso:

```text
read model diário = D1 nativo
read model workout = D4 canônico
```

Essa diferença de contexto é preferível a perda ou dupla contagem.

## Guard rails

- não suprimir bucket D1 inteiro por mera interseção;
- não dividir bucket D1 proporcionalmente sem evidência de fronteira;
- não somar D1 + D4 como se fossem fontes independentes;
- não usar totais de header D4 como série;
- não inventar calorias Health Connect;
- não alterar `SYNCED` nem snapshots históricos;
- histórico legacy continua legível.

## Validação funcional esperada

Usar a evidência física já observada como regressão conceitual:

```text
FEA1 == Σ D1.steps
```

em cenário elegível, e garantir que adicionar workouts ao read model não aumente
o total diário de passos.

Também validar que workout com métricas D4 nulas não cause desaparecimento de
bucket D1 não nulo apenas por interseção temporal.

## Validação técnica

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.

## Resultado da implementação

- Adicionado `WatchDailyReadModel` em `core/model`, com métricas diárias,
  provenance por fonte/métrica, cobertura FEA1 × D1, cobertura combinada de FC,
  completude, divergência e issues seguros.
- `WatchDailyReadModelComposer` mantém FEA1 como total diário de passos, usa D1
  para distribuição e para distância/calorias nativas, e mantém D4 somente no
  detalhe do workout, sem somá-lo ao diário.
- FC D1 só é omitida dentro de atividade quando há amostras D4 válidas; D4 com
  métricas locomotoras ausentes não remove fatos D1.
- Adicionado `RoomWatchDailyReadModelRepository`, com consultas reativas por dia
  e período sobre `DailySummaryRepository`, `WatchDailyD1Repository` e
  `WorkoutActivityRepository`.
- Foram adicionadas extensões aditivas aos contratos de leitura: consulta D1 por
  instante, consulta de workouts que intersectam o período e `endedAt` congelado
  na projeção de calendário. A consulta legacy por `startedAt` permanece intacta.
- Persistência de fatos, sync do relógio, Health Connect, snapshots e entrypoints
  de produto não foram alterados.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchDailyD1.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchDailyReadModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomWatchDailyReadModelRepository.kt`
- `docs/tasks/033-unified-watch-daily-sync/033-00-overview.md`
- `docs/tasks/033-unified-watch-daily-sync/033-04-compor-dados-diarios-fea1-d1-d4.md`

## Decisões e desvios

- FEA1 seleciona primeiro resumo válido do dispositivo associado; sem candidato
  associado, usa fallback determinístico pelo `updatedAt`, `collectedAt` e ID.
- D1 é consultado por `resolvedAt` no intervalo do `ZoneId` solicitado. A data
  local e o `zoneId` congelados no fato continuam preservados como provenance.
- Soma D1 incompleta permanece disponível somente para diagnóstico; distância,
  calorias e agregado elegível de passos exigem posições contínuas `1..total`.
- Workouts que cruzam meia-noite entram somente no read model composto, via
  consulta aditiva por interseção e `endedAt`; duração zero não é elegível.
- Nenhum ajuste D1 por overlap temporal foi feito. D4 continua no detalhe do
  workout e não participa dos totais diários.

## Comandos executados

```text
./gradlew app:compileDebugKotlin
./gradlew app:lintDebug
./gradlew lint assembleDebug assembleRelease
git diff --check
```

## Validação e evidências

- `./gradlew app:compileDebugKotlin` — sucesso.
- `./gradlew app:lintDebug` — sucesso.
- `./gradlew lint assembleDebug assembleRelease` — sucesso.
- `git diff --check` — sucesso.
- Não foram criados nem executados testes automatizados.

## Resultados e evidências

- O read model expõe cobertura FEA1 × D1, divergência, completude, provenance e
  issues de indisponibilidade sem criar fatos históricos.
- A implementação mantém `FEA1 == Σ D1.steps` observável e não adiciona passos
  D4 ao total diário.
- A validação física de D1 v2 que fundamenta o contrato permanece em
  `docs/integration/033-03-d1-v2-physical-validation.md`; validação física
  específica da composição pertence à 033-09.

Na conclusão desta task, 033-05, 033-06, 033-07 e 033-08 estão concluídas; 033-09
permanece bloqueada por gates físicos negativos e upgrade direto v17 → v19 sem
evidência final. A validação física do Champion C001, migration, idempotência,
partial success, cutover e ausência de auto-sync Health Connect continuam fora
desta subtarefa.

## Riscos residuais

- Testes automatizados não foram criados nem executados, conforme `AGENTS.md`.
- Validação física específica da composição, incluindo FC cross-midnight, e os
  gates negativos permanecem bloqueados na 033-09.
- Integração efetiva dos consumidores e read models legacy foi concluída na
  033-08; partial success e fechamento físico permanecem na 033-09. Entrypoints
  de produto foram removidos na 033-07.
