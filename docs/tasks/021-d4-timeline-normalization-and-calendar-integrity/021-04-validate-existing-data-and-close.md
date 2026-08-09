# 021-04 — Validar dados existentes e fechar a correção

- **Ticker:** `021`
- **Número:** `04`
- **Status:** `completed`

## Objetivo

Validar as duas correções da 021 no banco existente e no dispositivo físico,
sem wipe, sem recriar histórico e sem declarar sucesso além dos cenários
realmente exercitados.

## Resultado esperado

A atividade física que motivou a correção é reprocessada in-place com duração e
timeline normalizadas, e o calendário deixa de degradar uma fila saudável. Dados
já sincronizados permanecem preservados e não são reenviados automaticamente.

## Dependências

- `021-01` concluída;
- `021-02` concluída;
- `021-03` concluída;
- banco local preservado fornecido pelo usuário;
- Galaxy S22 e app oficial disponíveis para comparação física quando necessário.

## Escopo incluído

### Banco e migration

- instalar a versão nova sobre o app existente;
- confirmar abertura sem `clear data`;
- confirmar `PRAGMA integrity_check = ok`;
- confirmar versão do schema esperada (`v11` somente se `021-02` realmente
  precisar de migration);
- confirmar preservação de atividades, samples, snapshots e operações anteriores;
- não executar SQL corretivo manual como parte do aceite funcional.

### Duração física

Comparar novamente os casos conhecidos, sem registrar identificadores pessoais:

```text
15:04 + 24 s = 15:28
20:03 + 42 s = 20:45
29:01 + 14 s = 29:15
```

O resultado do app deve coincidir com as durações oficiais observadas.

### Atividade de 32 samples

- localizar a atividade existente, sem reimportar do relógio;
- executar o reprocessamento explícito disponível no app;
- preservar o mesmo `activityId`;
- obter duração oficial `29:15`;
- manter 32 samples/intervalos estruturais elegíveis;
- confirmar que a soma das janelas normalizadas é exatamente a duração da
  atividade;
- confirmar que nenhum sample foi descartado para caber;
- confirmar passos preservados;
- confirmar distância canônica/materialização e métricas temporais disponíveis;
- confirmar que o reprocessamento local não sincroniza automaticamente;
- revisar/sincronizar somente por ação explícita se o cenário físico for usado
  para validar Health Connect.

### Health Connect

- confirmar que operações previamente `SYNCED` não são recriadas apenas pelo
  upgrade ou pela abertura do app;
- confirmar ausência de duplicação observável nos cenários exercitados;
- caso a atividade reprocessada seja revisada e sincronizada durante o gate,
  validar `DISTANCE`/`SPEED` usando a timeline nova e registrar a limitação de
  observação manual do provider;
- manter integração write-only.

### Calendário

- abrir mês/dia com Bluetooth desligado ou relógio desconectado;
- confirmar que uma fila semanticamente íntegra não produz `Dados parciais do
  mês`;
- confirmar que cards sincronizados mostram status real em vez de `Status
  indisponível`;
- confirmar que navegação não altera nenhuma operação;
- preservar degradação para uma issue real se houver cenário seguro para
  observá-la; não fabricar corrupção no banco de produção do usuário.

## Escopo excluído

- limpar dados do app;
- apagar atividades para repetir importação;
- fabricar payload D4;
- executar leitura ampla do Health Connect;
- GPS, rota, phone workout, WorkManager ou BLE background;
- criar/rodar testes automatizados;
- declarar cobertura universal de firmwares.

## Critérios de aceite

1. App atualiza sobre o banco existente sem wipe.
2. Banco permanece íntegro e dados anteriores permanecem presentes.
3. Durações físicas conhecidas correspondem ao app oficial.
4. Atividade de 32 samples é normalizada/reprocessada in-place.
5. `Σ effectiveDuration = workout.duration` e último intervalo termina exatamente
   em `endedAt`.
6. Passos, ordem dos samples e distância total não são perdidos pela
   normalização.
7. Reprocessamento não dispara sync remoto automaticamente.
8. Operações anteriormente sincronizadas permanecem estáveis até ação explícita.
9. Falso `UNRESOLVED_AGGREGATE` desaparece com candidate hidratado.
10. Calendário não mostra estado parcial quando não há outra issue real.
11. Nenhum dado sensível é incluído em docs/logs/evidências.
12. Builds e instalação passam conforme comandos abaixo.
13. Revisão independente não encontra bloqueador.
14. Spec, ADR, protocolo, baseline, roadmap e overview são atualizados para o
    estado factual final antes de marcar `completed`.

## Comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar nem executar testes automatizados.

Consultas SQLite de diagnóstico são permitidas em modo read-only e devem ser
sanitizadas antes de entrar em documentação.

## Evidência mínima de fechamento

Registrar em `docs/integration/` um relatório sanitizado contendo:

- versão do app/schema/parser/derivação;
- status do upgrade sem wipe;
- duração e contagem de samples do cenário físico sem IDs;
- resultado do reprocessamento explícito;
- resultado do calendário;
- contagem agregada de operações por estado, sem IDs/clientRecordId;
- comandos executados e seus resultados;
- limitações físicas/N/A.

## Definição de pronto

- todos os critérios aplicáveis têm evidência factual;
- bloqueios/N/A estão descritos sem serem convertidos em `PASS`;
- `021-01..04` concluídas;
- overview mostra `4/4`;
- spec 021 mostra `completed`;
- nenhuma correção funcional fica escondida dentro do fechamento documental.

## Riscos residuais

- somente firmware observado possui confirmação física de `packet2[6]`;
- outros treinos podem revelar outra política nominal de samples;
- Health Connect continua sem readback amplo pelo app;
- reprocessar atividade já sincronizada permanece condicionado à reconciliação
  explícita definida na Fase 014.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `docs/specs/021-d4-timeline-normalization-and-calendar-integrity.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`
- `docs/decisions/adr-019-authoritative-workout-duration-and-normalized-d4-timeline.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/database-reference.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/integration/021-04-validation-and-close.md`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-00-overview.md`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-03-restore-health-sync-semantic-candidate-derivation.md`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-04-validate-existing-data-and-close.md`

## Decisões e desvios

- A validação foi executada sobre o banco existente, sem `clear data`, exclusão
  de atividade, nova leitura BLE ou SQL corretivo.
- O gate foi concluído após reprocessar explicitamente o caso
  `15:28` e conferir `endSecond = 42` no envelope local preservado do caso
  `20:45`. A atividade `20:45` estava `SYNCED` e não foi reprocessada.
- O relatório registra somente contagens, estados, durações e resultados
  sanitizados; identificadores, payloads, BPM e valores pessoais foram omitidos.
- O calendário foi exercitado offline; Bluetooth foi restaurado ao final e não
  houve readback amplo do Health Connect.
- Cenário negativo de operação órfã/snapshot ausente ficou `N/A`: não havia issue
  real segura e não foi fabricada corrupção no banco de produção.
- Revisão independente encontrou uma janela transacional: a duração recuperada
  podia ser atualizada antes de uma falha de derivação retornada normalmente.
  `reprocessWatchWorkoutInternal` agora lança a exceção controlada do
  repositório para qualquer resultado não materializado após o update, incluindo
  `Success(null)`, preservando o rollback da atividade e de gravações parciais.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- `./gradlew installDebug` — passou, instalando sobre o pacote existente.
- Revalidação após a correção transacional: `git diff --check` — passou;
  `./gradlew lint assembleDebug assembleRelease` — passou em 4m57s;
  `./gradlew installDebug` — passou em 11s, instalando no Galaxy S22 conectado.
- `adb shell am force-stop ...` e extração read-only do banco — executados para
  diagnóstico sanitizado.
- Consultas SQLite read-only: `PRAGMA integrity_check`, `PRAGMA user_version`,
  contagens agregadas, estados da fila e invariantes da atividade de 32 samples
  — passaram.
- Navegação UI offline em Calendário, mês e detalhe diário — passou.
- Testes automatizados — não executados, conforme escopo da task e regras do
  repositório.

## Resultados e evidências

- Relatório completo: `docs/integration/021-04-validation-and-close.md`.
- Upgrade preservou 4 atividades, 98 intervalos raw, 98 intervalos derivados,
  98 amostras de frequência cardíaca, 4 payloads diagnósticos e 84 operações.
- Banco pós-upgrade: integrity `ok`, schema `v11`.
- Atividade existente de 32 samples reprocessada in-place com duração `29:15`,
  32 intervalos preservados, índices `0..31`, soma temporal exata e derivação
  `watch-d3-d4-v2`.
- Fila permaneceu com 84 operações `SYNCED`, sem erro ou identidade ausente.
- Calendário offline não exibiu `Dados parciais do mês`; cards sincronizados
  exibiram status real.
- `15:28` resultou de reprocessamento explícito; `20:45` foi confirmado em
  diagnóstico read-only, sem reprocessar atividade `SYNCED`.
- A atualização de duração/`endedAt` agora só confirma quando a derivação retorna
  snapshot materializado; qualquer falha pós-update atravessa
  `withWriteTransaction` como exceção controlada e desfaz o lote.
- Revisão independente final: aprovada com ressalvas; nenhuma correção adicional
  de código foi solicitada.

## Riscos residuais

- A tela do app oficial não foi reaberta neste gate; a confirmação de duração
  usou payload diagnóstico local preservado.
- O cenário negativo de degradação permanece `N/A` por ausência de issue real
  segura para observar.
- Health Connect permanece write-only, sem readback amplo.
- Evidência física continua limitada ao Galaxy S22 e firmware observado.

## Revisão independente

- Revisão final aprovada com ressalvas após correção da janela transacional em
  `RoomRepositories.reprocessWatchWorkoutInternal`.
- A correção força rollback quando a derivação não materializa snapshot após a
  atualização de duração/`endedAt`.
- Ressalvas aceitas: Health Connect write-only, cenário negativo `N/A`, tela do
  app oficial não reaberta neste gate e cobertura limitada ao firmware observado.
