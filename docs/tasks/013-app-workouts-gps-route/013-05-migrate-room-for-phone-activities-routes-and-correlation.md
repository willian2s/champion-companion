# 013-05 — Migrar Room para atividades phone, rotas e correlação

- **Ticker:** `013`
- **Número:** `05`
- **Status:** completed

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

### Bloqueio registrado antes do gate físico final

- A execução física agora possui snapshot v5/v6 sanitizado: atividades,
  importações, intervalos, amostras BPM, `heart_rate_measurements` e sync
  preservaram contagens e hashes de colunas comuns; `user_version` passou de 5
  para 6 e as superfícies abriram sem estado parcial no segundo gate. A base
  fornecida não contém calibrações nem sessões (`0 → 0`), portanto esses dois
  caminhos ainda precisam de fixture histórica real. Migration agora normaliza
  `DISCARDED` legado sem término; criação e recuperação de rota rejeitam estado
  não canônico. Instruções estão em
  `docs/integration/013-05-room-v6-follow-up.md`.

### Riscos registrados antes do gate físico final

- Preservação de calibrações e sessões não foi exercitada com linhas reais,
  porque base v5 fornecida tinha zero registros nesses conjuntos.
- Recuperação física de rota aguarda rota real; API pública
  `loadRecoverySnapshot` agora restaura cursor, último ponto e segmentos com
  validação fechada.
- Downgrade depois de abrir v6 continua não suportado; rollback permanece
  operacional por restauração do artefato v5 antes da abertura do banco.

### Estado registrado antes do gate físico final

- Status naquele momento: bloqueada.
- Item `013-05` permanecia `[ ]` no overview; progresso permanecia `4/17`.
- Pendências operacionais estão registradas em
  `docs/integration/013-05-room-v6-follow-up.md`.
- Nenhuma subtarefa seguinte foi iniciada.

## Registro de execução — correção do snapshot de recuperação

### Causa do escalonamento

- A revisão final identificou que `WorkoutRouteRepository` permitia observar a
  rota, mas não restaurar cursor, último ponto aceito e segmentos persistidos.
  Após morte do processo, consumidores não conseguiam construir novo
  `AppendWorkoutRoutePointsInput` sem acessar Room diretamente.

### Correção delimitada

- `WorkoutRouteRecoverySnapshot` mantém rota, cursor de append, segmentos e
  último ponto aceito em contrato Android-free.
- `loadRecoverySnapshot(sessionId)` usa relação única sessão–rota e retorna
  `Success(null)` quando não existe rota para a sessão.
- `RoomRepositories` lê rota e pontos ordenados em `withReadTransaction`,
  restaura metadata de início e checkpoints de distância por segmento e rejeita
  com `UnexpectedState` domínio, metadata, ordenação ou métricas divergentes.
- Nenhuma tabela, índice, migration, coleta GPS, log ou teste foi alterado.
  `013-03` permaneceu inalterada: contrato em código já expressa API sem mudar
  requisitos aprovados.

### Comandos e resultados

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` em 1 min
  40 s; 103 tarefas, 28 executadas e 75 atualizadas. Permaneceram dois avisos
  preexistentes de overrides depreciados em `RoomRepositories`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Estado preservado naquele momento

- Status e overview permanecem inalterados para decisão do agente principal.
- Correção remove blocker de API de recuperação. Bloqueios documentais/manuais
  anteriores devem ser reconciliados pelo agente principal com nova evidência
  física informada antes de marcar somente `013-05` no checklist.

## Registro de execução — bloqueador estático final

### Causa do escalonamento

- Revisão final comprovou que linhas v5 em estado `DISCARDED` podiam migrar com
  `endedAtEpochMillis` nulo. O mapper v6 então rejeitava a sessão, pois estado
  persistido descartado exige término não nulo e duração temporalmente possível.
- Dois achados próximos permitiam criar rota com estado/agregados herdados e
  restaurar metadata temporal de segmentos sobrepostos.

### Correções aplicadas

- Migration `5 → 6` deriva `endedAtEpochMillis` somente para `DISCARDED` pelo
  máximo entre criação, início, término legado, marcadores ativo/pausado e
  início acrescido da duração acumulada. Conversão de nanos para milissegundos
  usa teto por quociente/resto, sem somar previamente os dois `Long`; o mesmo
  instante derivado alimenta `lastTransitionAtEpochMillis` nesse estado.
- Criação de rota reconstrói valor canônico antes do insert: `CAPTURING`,
  revisão `0`, término nulo e `WorkoutRouteMetrics.EMPTY`. Round-trip inválido
  retorna `UnexpectedState`; conflito de ID ou sessão já vinculada retorna
  `Conflict`.
- Recuperação rejeita primeiro segmento anterior ao início da rota e qualquer
  segmento posterior iniciado antes do fim do anterior. Validações existentes
  de primeiro ponto, metadata, checkpoints e métricas foram preservadas.
- Nenhuma tabela, índice, nova migration, teste ou arquivo fora da subtarefa foi
  alterado nesta correção; API pública de recuperação foi criada no registro
  anterior.

### Comandos e resultados

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL` em 1 min
  34 s; 103 tarefas, 28 executadas e 75 atualizadas. Permaneceram dois avisos
  preexistentes de overrides depreciados em `RoomRepositories`.
- Nenhum teste automatizado foi criado, alterado ou executado.

### Bloqueios preservados

- Status da subtarefa e overview não foram alterados. Cobertura física com
  calibração/sessão v5 real e recuperação com rota real continua pendente no
  follow-up existente para reconciliação pelo agente principal.

## Registro de execução — gate comparativo final

- Snapshot v5 foi capturado antes da abertura do APK v6 no `SM-S901E`: 3
  atividades, 3 importações, 94 intervalos, 94 amostras BPM de atividade, 82
  `heart_rate_measurements`, 95 operações de sync, 0 calibrações e 0 sessões.
- Após abertura do APK v6: `user_version=6`, `foreign_key_check` vazio e
  contagens preservadas. Comparação por colunas comuns retornou
  `common_rows_equal=True` e hashes iguais nos conjuntos não vazios.
- Exercícios/lista, detalhe e Calendário abriram com histórico e indicadores.
- `docs/integration/013-05-room-v6-follow-up.md` contém instruções para
  calibração, sessões `DISCARDED` reais e recuperação de rota com dados reais.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram.
- Nenhum teste automatizado foi criado, alterado ou executado.

## Registro de execução — continuação física bloqueada

### Resumo da execução

- Subtarefa selecionada explicitamente: `013-05`.
- Branch confirmada: `feat/013-phone-workouts`.
- Commit observado: `c28d7a9` (`feat: implement route recovery snapshot and normalize DISCARDED sessions in migration`).
- Contexto, spec, overview, ADRs referenciados e follow-up foram lidos.
- O planejamento usa ticker `013` de forma consistente; o overview possui uma
  única seção `## Checklist`, com um item para cada uma das 17 subtarefas.

### Resultado do gate

Bloqueado antes da validação do banco v5: `adb devices -l` não listou nenhum
dispositivo e as consultas `pm path`/`run-as` retornaram
`adb: no devices/emulators found`. Portanto, a fixture v5 não pôde ser
confirmada pelo dispositivo atualmente conectado.

### Comandos executados e evidências

- `adb devices -l`: nenhum dispositivo listado.
- `adb shell pm path br.com.mwconsulting.championcompanion.dev`: não executado
  no dispositivo; sem dispositivo ADB disponível.
- `adb shell run-as br.com.mwconsulting.championcompanion.dev ls -la databases`:
  não executado no dispositivo; sem dispositivo ADB disponível.
- Não foram executados `installDebug`, abertura do app, `pm clear`, `uninstall`,
  instalação limpa, consultas SQLite, checkpoint WAL ou cópia de banco.
- Não foram criados nem executados testes automatizados.

### Arquivos alterados nesta tentativa

- `docs/integration/013-05-room-v6-follow-up.md`
- esta task
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`

Nenhum arquivo de código, banco, snapshot ou build foi alterado.

### Decisões, desvios e riscos residuais

- Mantido `Status: blocked`; item `013-05` continua desmarcado no overview.
- Não houve instalação nem abertura do APK v6, preservando a regra crítica de
  capturar e validar v5 antes da migration.
- Snapshot v5/v6, hashes, comparação de calibração/sessões, normalização das
  duas sessões `DISCARDED`, superfícies e `foreign_key_check` permanecem sem
  evidência nova nesta tentativa.
- Rota real e `WorkoutRouteRepository.loadRecoverySnapshot` continuam
  follow-ups separados, conforme escopo da task.

## Registro de execução — gate físico v5 → v6 concluído

### Resumo

- Branch: `feat/013-phone-workouts`.
- Commit observado: `c28d7a9`.
- Dispositivo: Galaxy S22 `SM-S901E`.
- Fixture v5 confirmada antes de qualquer instalação: uma calibração e duas
  sessões `DISCARDED`, com os estados temporais e durações esperados.
- APK v6 instalado sobre a instalação existente e aberto uma vez para executar
  migration Room v5 → v6.
- Aplicativo fechado com `am force-stop`, reaberto e superfícies consultadas
  novamente.

### Método WAL/SHM e comandos

- Checkpoint remoto tentado; falhou porque `sqlite3` não existe no sandbox
  `run-as`.
- Snapshots v5, v6 e pós-reabertura foram copiados sempre com os três arquivos:
  `champion-companion.db`, `champion-companion.db-wal` e
  `champion-companion.db-shm`.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`, instalação atualizada em um
  dispositivo.
- Não foram criados nem executados testes automatizados.
- Não foram executados uninstall, `pm clear`, downgrade ou instalação limpa.

### Evidências sanitizadas

- v5: `user_version=5`, foreign keys sem violações, calibrações `1`, vínculos
  quebrados `0`, sessões descartadas `2`.
- v6: `user_version=6`, foreign keys sem violações, calibrações `1`, vínculos
  quebrados `0`, sessões descartadas `2`.
- Hash de calibrações: `row_count=1`, SHA-256 igual entre v5/v6.
- Hash de colunas comuns de sessões: `row_count=2`, SHA-256 igual entre v5/v6.
- Hashes completos e regras canônicas estão em
  `docs/integration/013-05-room-v6-follow-up.md`; nenhum ID ou timestamp bruto
  foi documentado.
- Sessão anterior ao início e sessão posterior ao início passaram normalização
  de término, última transição, durações, agregados e leitura sanitizada.
- Exercícios, histórico, detalhe da esteira, calibração e Calendário abriram.
- Após fechar/reabrir, histórico de Exercícios e Calendário permaneceram
  disponíveis; snapshot pós-reabertura manteve calibração, sessões e integridade.

### Arquivos documentais alterados

- `docs/integration/013-05-room-v6-follow-up.md`
- esta task
- `docs/tasks/013-app-workouts-gps-route/013-00-overview.md`

Nenhum arquivo de produção, schema ou banco foi alterado nesta validação.

### Decisões, desvios e riscos residuais

- Fallback `.db` + `-wal` + `-shm` foi usado porque checkpoint remoto não estava
  disponível.
- Ausência de rota real não bloqueia esta conclusão: captura funcional ainda
  não produz rota real. `WorkoutRouteRepository.loadRecoverySnapshot` permanece
  follow-up separado.
- Nenhuma divergência encontrada em calibração, sessões, hashes, foreign keys,
  normalização ou superfícies.

### Estado final

- Status: completed.
- Item `013-05` deve ser marcado `[x]` no overview.
- Progresso do overview: `5/17`.
