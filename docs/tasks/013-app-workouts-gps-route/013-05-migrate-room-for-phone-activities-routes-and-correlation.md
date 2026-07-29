# 013-05 — Migrar Room para atividades phone, rotas e correlação

- **Ticker:** `013`
- **Número:** `05`
- **Status:** blocked

## Objetivo e resultado esperado

Aplicar migration explícita `v5 → v6` e persistência transacional para atividade generalizada, sessão completa, rota incremental e correlação. Resultado: histórico D3/D4 preservado, schema `6.json` e APIs Room/data prontas.

## Contexto

v5 possui sessão, atividade D3/D4 e fila, mas sem rota. Mapper de sessão é assimétrico. Joins obrigatórios de import excluem phone. Migration deve evitar índices preventivos.

## Documentos obrigatórios

Spec 013; ADR-003/004/012/014; `013-01/03`; schema 5; docs de Room 3 e contratos aprovados.

## Dependências

`013-04` concluída; contratos de `013-03` congelados.

## Arquivos permitidos

`ChampionDatabase.kt`, `ChampionDaos.kt`, módulo/migration de database; `DatabaseMappers.kt`, `RoomRepositories.kt`, `RepositoryModule.kt`; schema `6.json`; modelos mínimos necessários; task/overview.

## Arquivos e símbolos prováveis

`WorkoutActivityEntity`, `WatchWorkoutImportEntity`, `AppWorkoutSessionEntity`, novos `WorkoutRouteEntity`/`WorkoutRoutePointEntity`, DAOs correspondentes, `AppWorkoutSessionEntity.toDomain()` e `RoomRepositories`.

## Arquivos proibidos

UI, GPS Android, FGS, BLE, Health Connect gateway/provider, testes.

## Requisitos cobertos

Migration v6; tabelas route/points; uma rota por sessão; segmentos; cascata; distância acumulada; generalização; recuperação; correlação e preservação histórica.

## Escopo

- remodelar campos import-only para não serem obrigatórios em atividade comum;
- manter `WatchWorkoutImport` como fonte de CRC/parser/device importado;
- adicionar route e points normalizados;
- sequence única por rota e sessão única;
- corrigir round-trip integral da sessão;
- DAOs de lote pequeno/checkpoint/deleção/candidato de correlação;
- tornar joins de atividade/import opcionais onde necessário;
- ajustar constraint da fila somente se consulta/projeção concreta da spec exigir.

## Fora do escopo

Coletar pontos, FGS, merge de protocolo, records Health Connect.

## Passos de implementação

1. Desenhar SQL v5→v6 e mapear preservação de cada coluna/ID.
2. Criar `workout_routes` e `workout_route_points` com FKs/cascatas e constraints comprovadas.
3. Generalizar `workout_activities` sem fabricar import/device para phone.
4. Adicionar campos mínimos de checkpoint/revisão/associação conforme contratos.
5. Corrigir `AppWorkoutSessionEntity.toDomain()` com restauração validada completa.
6. Implementar DAO/repository lifecycle, ponto/lote, segmentos, descarte físico pré-atividade e finalização atômica.
7. Adaptar queries de lista/calendário/BPM/sync para atividade sem import.
8. Implementar lookup de correlação sem merge ainda.
9. Atualizar versão, migration registration e exportar `6.json` pelo build permitido.
10. Registrar plano de rollback e overview.

## Critérios de aceite

- sem destructive migration;
- v5 histórico mantém IDs, workflows, importações, calibrações e sync;
- rota/pontos têm cascata e ordem determinística;
- uma sessão não recebe duas rotas/atividades;
- sessão round-trip preserva estado/tempos/agregados/vínculo;
- atividades phone aparecem em queries sem import fictício;
- nenhum índice sem query/constraint concreta.

## Validação manual

Instalar versão v5 com histórico real, atualizar para v6, abrir listas/detalhes/calendário e confirmar preservação. Inspecionar schema exportado e foreign keys. Não executar testes.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

SQL/migration, diff 5→6, contagens antes/depois sanitizadas, abertura do banco atualizado, histórico preservado e comandos.

## Screenshots

Capturar lista/detalhe/calendário preservados, sem dados sensíveis, se superfícies mudarem.

## Definição de pronto

Migration v6 aplicada manualmente sem perda; repositories compilam; overview atualizado.

## Riscos e cuidados

Migration é hotspot crítico e downgrade após abrir v6 não é suportado. Não logar conteúdo de pontos. Verificar limite SQLite e transações curtas.

## Rollback

Antes do rollout: reverter APK/schema para v5. Após banco aberto em v6: não fazer downgrade; restaurar ambiente de validação por reinstalação limpa somente em dispositivo de desenvolvimento, preservando evidência de que produção exigiria forward fix.

Não criar, alterar ou executar testes automatizados.

## Registro de execução — correção Room/KSP

### Evidência e causa raiz

- A reprodução inicial de `./gradlew app:lintDebug` falhou em
  `:app:kspDebugKotlin` com `extraneous input '.'`, erro SQLite próximo de
  `.`, parâmetro de query não conversível e `expectedRevision` não utilizado.
- A causa foi o uso de `:next.field` em `UPDATE` Room 3. O parâmetro
  `AppWorkoutSessionEntity`/`WorkoutRouteEntity` não é bindável como objeto em
  `@Query`; o parser rejeita o ponto e não reconhece a revisão como parâmetro
  efetivamente usado.

### Correção aplicada

- `AppWorkoutSessionDao` e `WorkoutRouteDao` agora recebem somente parâmetros
  escalares em `updateCasFields`.
- `WHERE id = :id AND revision = :expectedRevision` foi preservado nas duas
  queries. `RoomRepositories` mapeia as entidades para os escalares, mantendo
  CAS real e retorno de conflito quando nenhuma linha é atualizada.
- Após eliminar o bloqueio KSP, a compilação revelou duas falhas já presentes
  no código parcial: `Migration.migrate` exige `suspend` em Room 3 e mappers
  com `return` não podem usar corpo de expressão. Ambas foram corrigidas
  minimamente nos arquivos já permitidos da subtarefa.

### Comandos e resultados

- `git diff --check`: passou.
- `./gradlew app:lintDebug`: primeira execução pós-correção excedeu timeout
  operacional de 120 s durante análise Lint; repetição com timeout de 240 s
  concluiu com `BUILD SUCCESSFUL` em 57 s. KSP, compilação Kotlin/Java e Lint
  concluídos.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Bloqueios

- Nenhum bloqueio novo na validação focada. Permanecem apenas dois avisos de
  override depreciado em `RoomRepositories`, sem falha de compilação.

## Registro de execução — implementação e validação final

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RepositoryModule.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/6.json`
- esta task e `013-00-overview.md`

### Decisões e desvios

- Migration manual `5 → 6` reconstrói somente tabelas que exigem alteração,
  preserva IDs e dados D3/D4 e registra explicitamente `MIGRATION_5_6` no
  `DatabaseModule`.
- `WorkoutActivityEntity` aceita proveniência phone sem dispositivo,
  parser/importação ou diagnóstico fictícios. `WatchWorkoutImport` continua
  fonte de CRC, índice, parser e dispositivo importado.
- Sessão persiste estado, tempos, agregados, razões de ausência, vínculo,
  revisão, geração e último instante de transição. Rotas e pontos usam tabelas
  normalizadas, cascatas, sequência única e lotes limitados a 32 pontos.
- Associação sessão–atividade exige atividade phone, vínculo reverso coerente e
  dispositivo compatível. Descarte pré-atividade remove fisicamente pontos,
  rota e amostras transitórias somente quando não existe atividade associada.
- Checkpoint valida cursor, contagens, distância, precisão média, segmentos e
  revisão CAS antes de atualizar rota e inserir pontos na mesma transação.
- Não foi alterada a constraint da fila Health Connect, pois nenhuma projeção
  nova da sessão exigiu essa mudança. Nenhum índice preventivo foi adicionado;
  o aviso Room sobre FK sem índice permanece intencional.

### Comandos, resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado em
  `SM-S901E - 16`.
- O banco privado do pacote debug foi aberto após instalação. `PRAGMA
  user_version` retornou `6`; tabelas `workout_routes` e
  `workout_route_points` existem.
- Contagens sanitizadas pós-migration: 5 atividades, 5 importações, 195
  intervalos, 195 amostras BPM, 0 calibrações e 0 sessões. Nenhum ID, horário,
  dado de saúde ou coordenada foi exposto.
- Com `PRAGMA foreign_keys=ON`, `PRAGMA foreign_key_check` não retornou
  violações. SQL confirmou FKs de sessão, rota e pontos, cascatas e índices
  únicos de atividade, rota por sessão e sequência de pontos.
- Após o upgrade, a superfície Exercícios abriu com 5 atividades sincronizadas;
  a lista mostrou atividades históricas e um detalhe abriu com métricas
  preservadas. Calendário abriu, mas exibiu estado seguro de “Dados parciais do
  mês”; não foi considerado confirmação plena da superfície.
- Revisão independente estática aprovou implementação, schema, escopo e
  constraints após correções. Revisão final manteve bloqueio documental por
  ausência de snapshot comparativo pré-migration e por calendário aberto em
  estado parcial no banco atualizado.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme regra
  explícita da Fase 13.

### Bloqueio atual

- A execução física confirmou abertura do banco em v6 e integridade estrutural,
  mas não registrou contagens/IDs/workflows/sync antes da migration. Também não
  confirmou calendário íntegro após o upgrade, pois a tela exibiu dados parciais.
  Repetir gate com snapshot sanitizado de base v5 contendo calibração e sync,
  comparar divergências e investigar o estado parcial do calendário antes de
  concluir.

### Riscos residuais

- Preservação integral de IDs, workflows, calibrações e estados de sync ainda
  não foi comprovada por comparação antes/depois.
- Lista e detalhe foram exercitados manualmente; calendário foi aberto em estado
  parcial e precisa de confirmação após investigação.
- Downgrade depois de abrir v6 continua não suportado; rollback permanece
  operacional por restauração do artefato v5 antes da abertura do banco.

### Estado final

- Status: bloqueada.
- Item `013-05` permanece `[ ]` no overview; progresso permanece `4/17`.
- Nenhuma subtarefa seguinte foi iniciada.
