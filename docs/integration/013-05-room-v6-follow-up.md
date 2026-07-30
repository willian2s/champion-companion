# Follow-up — validação Room v5 → v6

## Objetivo

Completar cobertura manual de cenários que não existem no banco v5 usado no
gate físico de `013-05`.

## Evidência já capturada

- Snapshot v5 e snapshot v6 foram capturados no pacote debug do dispositivo
  `SM-S901E`.
- Comparação sanitizada preservou linhas e hashes de atividades, importações,
  intervalos, amostras BPM, `heart_rate_measurements` e operações de sync.
- `user_version` passou de `5` para `6`; `foreign_key_check` não apontou
  violações.
- Exercícios, detalhe e Calendário abriram após migration.

## Pendências separadas de rota real

1. Quando houver rota real, exercitar `WorkoutRouteRepository.loadRecoverySnapshot`
   após reinício/process death e confirmar cursor, último ponto, segmentos,
   métricas e sequência antes de novo lote.
2. Alinhar invariantes de escrita e leitura de rota: validar `route.startedAt`
   contra `session.startedAt`, rejeitar primeiro ponto/segmento anterior à rota
   e impedir segmento novo iniciado antes do fim do segmento anterior. Garantir
   que todo append aceito seja aceito imediatamente por
   `loadRecoverySnapshot`.
3. Após corrigir invariantes, solicitar nova revisão independente do caminho
   append → recuperação e registrar decisão na task `013-05`.
4. Se qualquer divergência aparecer, abrir correção na subtarefa responsável;
   não fazer downgrade de banco v6.

## Restrições

- Não criar nem executar testes automatizados nesta fase.
- Executar somente em dispositivo de desenvolvimento.
- Não registrar conteúdo de pontos, localização, payload BLE, dados de saúde ou
  identificadores pessoais.

## Tentativa de continuação — 2026-07-30

- Branch esperada: `feat/013-phone-workouts`.
- Commit observado: `c28d7a9` (`feat: implement route recovery snapshot and normalize DISCARDED sessions in migration`).
- `adb devices -l` não listou nenhum dispositivo.
- `adb shell pm path br.com.mwconsulting.championcompanion.dev` não pôde ser
  executado: `adb: no devices/emulators found`.
- `adb shell run-as br.com.mwconsulting.championcompanion.dev ls -la databases`
  não pôde ser executado pelo mesmo motivo.
- Nenhum APK foi instalado ou aberto nesta tentativa.
- Nenhum comando de limpeza, desinstalação, downgrade ou instalação limpa foi
  executado.
- Nenhum snapshot v5, consulta SQLite, checkpoint WAL ou cópia de
  `champion-companion.db`/`-wal`/`-shm` foi produzido nesta tentativa.
- Build não foi executado: gate físico v5 não pôde ser confirmado antes da
  instalação.

### Resultado

Validação física permanece bloqueada por ausência de dispositivo ADB. Não há
evidência nova sobre pacote, banco v5, fixture, migration, snapshot v6,
superfícies ou hashes. Preservar fixture instalada e repetir a partir da
confirmação do pacote quando um dispositivo de desenvolvimento estiver
disponível.

## Gate físico concluído — 2026-07-30

### Ambiente e método WAL/SHM

- Pacote: `br.com.mwconsulting.championcompanion.dev`.
- Dispositivo: Galaxy S22 `SM-S901E`.
- Commit observado: `c28d7a9`.
- `PRAGMA wal_checkpoint(FULL)` não pôde ser executado pelo sandbox: o binário
  `sqlite3` não existe no ambiente `run-as` (`run-as: exec failed for sqlite3:
  No such file or directory`).
- Fallback usado nos dois gates: cópia conjunta de
  `champion-companion.db`, `champion-companion.db-wal` e
  `champion-companion.db-shm`; SQLite local abriu os três arquivos juntos.
- Nenhum arquivo de banco foi movido para o repositório ou versionado.

### Snapshot v5

- `user_version=5`.
- `foreign_key_check`: 0 violações.
- `calibration_count=1`.
- `broken_calibration_links=0`.
- `DISCARDED.quantity=2`.
- `DISCARDED.without_start=1`.
- `DISCARDED.without_end=2`.
- `DISCARDED.with_active_duration=1`.
- `DISCARDED.with_paused_duration=1`.
- Calibrações: `row_count=1`, `sha256=cc864b8db5c66b785da8fc026879d1d0602bfb8feb3bc25cff7b7eb6112cb71b`.
- Sessões canônicas: `row_count=2`, `sha256=25906b922d5b606ee22d3e7ee9a313109942e75a5e9bc2e499d7a9be6cc6e7ab`.

Hash de sessões excluiu `endedAtEpochMillis` para `DISCARDED` e todos os
campos adicionados em v6, incluindo última transição, dispositivo associado,
revisão, geração, razões de ausência e disponibilidade de passos. Não foram
registrados IDs nem valores temporais brutos.

### Migration e snapshot v6

- `./gradlew lint assembleDebug assembleRelease`: passou; permaneceram somente
  avisos de overrides depreciados e recomendação de índice FK já conhecida.
- `./gradlew installDebug`: passou; atualização sobre instalação existente,
  sem uninstall, `pm clear` ou instalação limpa.
- Aplicativo aberto uma vez após instalação; Room executou v5 → v6.
- `user_version=6`.
- `foreign_key_check`: 0 violações.
- `calibration_count=1`; `broken_calibration_links=0`.
- `DISCARDED.quantity=2`, `without_start=1`, `without_end=0`,
  `with_active_duration=1`, `with_paused_duration=1`.
- Calibrações: `row_count=1`, mesmo SHA-256 do v5; `equal=true`.
- Sessões canônicas: `row_count=2`, mesmo SHA-256 do v5; `equal=true`.
- Nenhuma sessão foi perdida ou duplicada; contagens e hashes comuns foram
  preservados.

### Normalização sanitizada das sessões `DISCARDED`

Sessão descartada antes do início:

```text
state_is_discarded = true
started_at_is_null = true
ended_at_is_null = false
ended_at_equals_created_at = true
last_transition_equals_ended_at = true
active_duration_preserved = true
paused_duration_preserved = true
round_trip_readable = true
```

Sessão descartada após o início:

```text
state_is_discarded = true
started_at_is_null = false
ended_at_is_null = false
ended_at_matches_accumulated_durations = true
last_transition_equals_ended_at = true
active_duration_preserved = true
paused_duration_preserved = true
aggregates_preserved = true
round_trip_readable = true
```

`round_trip_readable` foi confirmado pela comparação sanitizada das colunas
comuns, abertura do banco v6 pelo app e leitura sem erro nas superfícies
persistentes. Não houve exposição de valores temporais, IDs ou dados sensíveis.

### Superfícies e persistência após reabertura

- Exercícios abriu e exibiu histórico com 3 atividades importadas.
- Detalhe da esteira abriu com métricas e calibração ativa legível.
- Calendário abriu com histórico mensal e indicadores nos dias existentes.
- Após `am force-stop` seguido de nova abertura, Exercícios exibiu novamente o
  histórico e Calendário abriu novamente.
- Snapshot pós-reabertura confirmou `user_version=6`, uma calibração, duas
  sessões `DISCARDED` e 0 violações de foreign key.
- Screenshots ficaram somente em diretório temporário local; não contêm
  evidência necessária para versionamento nem foram adicionados ao repositório.

### Resultado do gate

Gate físico v5 → v6 concluído sem divergência. Invariantes estáticas do caminho
de escrita de rota foram corrigidas e aprovadas em revisão independente.
Conforme regra da task, ausência de rota real não bloqueia `013-05` enquanto o
fluxo funcional de captura ainda não produz pontos. Rota real,
`WorkoutRouteRepository.loadRecoverySnapshot` e process death permanecem
follow-up separado das tasks `013-08`/`013-09`.

## Bloqueio estático identificado pela revisão independente

Revisão independente de 2026-07-30 reprovou a conclusão por achado de alta
severidade em `data/RoomRepositories.kt`, nos caminhos `create`,
`appendAcceptedPoints`, `isValidRouteAppend` e
`restoreWorkoutRouteRecoverySnapshot`:

- escrita de rota não valida `route.startedAt` contra `session.startedAt`;
- primeiro ponto/segmento pode ser aceito antes do início da rota;
- novo segmento pode ser aceito antes do término do segmento anterior;
- recuperação rejeita essas combinações depois, permitindo append com sucesso
  seguido de snapshot irrecuperável após process death.

Correção exigida: aplicar invariantes equivalentes no caminho de escrita,
consultando sessão e último segmento persistido, e revalidar append → snapshot.
Correção estática foi aplicada em `RoomRepositories.kt`; nova revisão
independente e validação física de rota real continuam pendentes.

### Correção estática aplicada — 2026-07-30

- `create(route)` rejeita rota cujo início não coincide com sessão iniciada.
- `appendAcceptedPoints` rejeita primeiro ponto/segmento anterior ao início da
  rota ou sessão.
- Segmentos novos são comparados com o último ponto persistido e segmentos do
  lote mantêm ordem temporal, impedindo estado que
  `loadRecoverySnapshot` rejeitaria.
- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: passou.
- Nenhum teste automatizado foi criado ou executado.

### Correção residual de precisão temporal — 2026-07-30

- Revisão posterior identificou colisão quando dois `Instant` ordenados apenas
  por nanos eram convertidos para o mesmo epoch-millis.
- Escrita agora rejeita, antes do CAS/insert, pontos cuja representação
  epoch-millis não seja estritamente crescente dentro do lote ou contra o
  último ponto persistido.
- Cursor e inícios de segmento são validados quanto à representação persistida;
  timestamps não são ajustados silenciosamente.
- A validação agora exige representação sem perda: `Instant.ofEpochMilli` do
  valor convertido deve ser igual ao `Instant` original. Frações de
  milissegundo e overflow são rejeitados antes da escrita.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram;
  nenhum teste automatizado foi criado ou executado.
- Nova revisão independente aprovou a correção estática com ressalvas; validação
  física de rota real permanece pendente como follow-up separado.

### Resultado da revisão independente final

- Decisão: aprovada com ressalvas para correção estática lossless.
- Nenhum bloqueador estático restante encontrado.
- Checklist, ticker, progresso `4/17`, escopo e documentação foram considerados
  consistentes.
- Ressalvas: rota real, process death com pontos e
  `loadRecoverySnapshot` físico ainda não foram exercitados.
- Essas ressalvas permanecem follow-up separado das tasks `013-08`/`013-09`
  porque captura funcional ainda não produz pontos reais; não houve teste
  automatizado.

### Decisão documental

- `013-05` pode ser concluída conforme limitação explícita da task.
- A ausência de rota real não altera o status da migration v5 → v6.
