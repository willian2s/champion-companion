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

Gate físico v5 → v6 concluído sem divergência. `013-05` pode ser concluída.
Permanece separado somente follow-up de rota real e
`WorkoutRouteRepository.loadRecoverySnapshot`, pois fluxo funcional de captura
de rota ainda não produz pontos reais.
