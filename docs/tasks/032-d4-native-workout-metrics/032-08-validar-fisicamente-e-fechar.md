# 032-08 — Validar fisicamente e fechar

- **Status:** completed
- **Depende de:** 032-07

## Objetivo

Validar em Champion C001 real o parser v4, distância nativa, calibração e
integração final antes de encerrar a spec.

## Cenários mínimos

- importação D3 com slot zero entre slots válidos;
- atividade com variação forte de intensidade;
- `startedAt`, `endedAt`, `recordCount` e quantidade de samples coerentes com o
  header sem impor `N × 60 s`;
- soma de passos da série comparada ao header apenas como diagnóstico;
- soma de distância nativa comparada ao header;
- referência externa manual para observar fator de correção, aceitando erro da
  referência;
- atividade sem calibração contextual;
- atividade com fator RUNNING ou WALKING aprendido;
- fallback por comprimento de passo quando distância nativa for inelegível;
- review-before-sync e Health Connect explícito;
- nenhuma regressão em Daily Summary, cadência e `CYCLING`.

## Evidência

Registrar somente dados sanitizados e necessários. Não versionar payload BLE
integral, identificadores pessoais ou dump de banco completo.

## Fechamento

- atualizar Spec 032, ADR-029, overview, baseline e protocolo;
- registrar limitações de firmware/unidades não fechadas;
- executar `./gradlew lint assembleDebug assembleRelease`;
- executar `git diff --check`;
- revisão independente antes do merge em `main`.

## Execução

### Arquivos alterados

- `docs/integration/032-08-physical-validation.md`;
- `docs/tasks/032-d4-native-workout-metrics/032-08-validar-fisicamente-e-fechar.md`;
- `docs/tasks/032-d4-native-workout-metrics/032-00-overview.md`;
- `docs/specs/032-d4-native-workout-metrics.md`;
- `docs/decisions/adr-029-d4-native-metrics-and-distance-calibration.md`;
- `docs/baseline/current-baseline.md`;
- `docs/protocol/workouts-d3-d4.md`.

### Decisões e desvios

- Validação manual foi aceita como evidência física final, conforme confirmação
  do usuário, com conferência por dumps sanitizados do banco.
- Nenhum dump completo, payload BLE integral ou identificador pessoal foi
  versionado.
- A spec e o ADR foram fechados sem promover semântica ainda desconhecida de
  `pauseStateRaw`, `sportValue` ou firmware não observado.

### Comandos executados

- `./gradlew lint assembleDebug assembleRelease`;
- `git diff --check`.

### Resultados e evidências

- Validação física manual no Champion C001: passou para todos os cenários
  mínimos definidos nesta task.
- Conferência por dumps sanitizados do banco: passou para persistência,
  provenance, timeline, distância nativa, calibração, fallback e estados de
  sincronização.
- `lint`, `assembleDebug` e `assembleRelease`: passaram.
- `git diff --check`: passou.
- Nenhum teste automatizado foi criado ou executado, conforme `AGENTS.md`.
- Evidência detalhada: `docs/integration/032-08-physical-validation.md`.

### Riscos residuais

- `pauseStateRaw` não zero ainda não possui semântica operacional validada.
- Unidade funcional de `sportValue` permanece desconhecida.
- Campos reservados/desconhecidos, política de reprocessamento de históricos,
  tolerâncias futuras, retenção de payload diagnóstico e cobertura de samples
  acima de 255 passos permanecem sem fechamento universal.
- Variações de firmware/layout fora do Champion C001 validado permanecem sem
  cobertura.
- Referência externa manual pode conter erro e serve somente como referência de
  calibração, não como verdade absoluta.
