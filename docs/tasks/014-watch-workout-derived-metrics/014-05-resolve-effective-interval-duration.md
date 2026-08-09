# 014-05 — Resolver duração efetiva, índices e plausibilidade

- **Ticker:** `014`
- **Número:** `05`
- **Status:** completed
- **Concluída em:** 2026-08-06

## Objetivo e resultado esperado

Aplicar a política temporal da ADR-018 aos intervalos existentes, exigir índices
contínuos e únicos e registrar plausibilidade sem redistribuir passos.

Resultado: janelas positivas, ordenadas e sem síntese, com elegibilidade e
plausibilidade prontas para a derivação canônica.

## Dependências

- `014-04` concluída.

## Arquivos permitidos

- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- contratos temporais em `core/model`;
- mappers/repositories de intervalos estritamente necessários;
- documentação de protocolo e overview.

## Arquivos proibidos

- UI, perfil, splits e Health Connect;
- Gradle, manifesto, migrations e schemas;
- sessões phone e rota histórica.

## Escopo

### Índices

A série V1 exige:

```text
0, 1, 2, ..., N-1
```

São inelegíveis:

- primeiro índice diferente de zero;
- índice ausente;
- índice duplicado;
- ordem inconsistente após normalização autorizada.

Não calcular a posição temporal apenas pela quantidade de linhas quando houver
lacuna.

### Duração

Com `T`, `N` elegível e quantum de 60 segundos:

- `T = N×60`: todos completos;
- `(N−1)×60 < T < N×60`: último recebe o restante;
- `T <= (N−1)×60`: série inelegível;
- `T > N×60`: todos completos e cauda sem bucket.

O exemplo `1.637 s` produz 27 intervalos de 60 s e último de 17 s.

### Plausibilidade

- A duração residual permanece mesmo quando a cadência resultante for alta.
- Registrar `durationResolution = LAST_INTERVAL_RESIDUAL` quando aplicável.
- Calcular indicadores tipados de plausibilidade definidos em `014-02`.
- Não alterar passos, duração ou distância para “corrigir” plausibilidade.
- Projeção granular futura deve poder bloquear série implausível.

## Fora do escopo

- corrigir timestamps de origem sem evidência;
- sintetizar intervalo ausente;
- calcular distância, ritmo ou split;
- escolher novo limiar fora do contrato de `014-02`;
- testes automatizados.

## Passos de implementação

1. Validar índices antes da política `T/N`.
2. Centralizar o resolvedor temporal em função determinística.
3. Aplicar o restante positivo somente ao último intervalo elegível.
4. Preservar cauda sem criar bucket.
5. Persistir duração efetiva, resolução, elegibilidade e plausibilidade.
6. Propagar `endedAt` resolvido sem inflar duração ativa.
7. Garantir convenção temporal consistente para BPM.
8. Atualizar protocolo e overview.

## Critérios de aceite

- Lacuna, duplicata ou primeiro índice não zero bloqueia a série.
- `1.637 s` produz último intervalo de 17 s.
- `T=N×60` mantém todos com 60 s.
- Caso curto falha fechado sem truncar fatos.
- Caso longo preserva a sessão sem inventar intervalo.
- Cadência implausível gera indicador, não redistribuição.
- Janelas válidas são positivas, ordenadas, não sobrepostas e contidas.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- matriz índice → elegibilidade;
- matriz `T/N → duração/resolução`;
- registro sanitizado do exemplo de 1.637 s;
- matriz de plausibilidade;
- builds e smoke local.

### Implementação

- `TemporalSeriesValidator` continua sendo executado antes de qualquer cálculo
  `T/N` no `WorkoutDurationResolver`;
- séries com lacuna, duplicata, primeiro índice diferente de zero ou ordem
  inconsistente retornam fatos measured sem janelas efetivas sintetizadas;
- índices elegíveis preservam `EXACT`, `LAST_INTERVAL_RESIDUAL`,
  `INSUFFICIENT_TOTAL_DURATION` e `EXCESS_TOTAL_DURATION`, incluindo cauda sem
  bucket;
- `ChampionD4WorkoutParser` propaga `resolvedEndedAt` para `WorkoutMetrics`,
  mantendo duração ativa, fatos raw e fatos measured separados.

### Matriz validada por inspeção do contrato

| Entrada | Resultado esperado preservado pelo resolvedor |
| --- | --- |
| `0..N-1`, `T = N × 60 s` | todas as janelas com 60 s |
| `0..27`, `T = 1.637 s` | 27 janelas de 60 s e última de 17 s |
| `0..N-1`, `T <= (N-1) × 60 s` | inelegível, sem janelas efetivas |
| `0..N-1`, `T > N × 60 s` | janelas de 60 s e `tailUncovered`, sem bucket adicional |
| lacuna, duplicata, primeiro índice não zero ou ordem inconsistente | inelegível, sem síntese por posição da lista |
| indicador/política de plausibilidade existente | não altera duração nem passos |

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`;
- `docs/protocol/workouts-d3-d4.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-05-resolve-effective-interval-duration.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`.

### Decisões e desvios

- A alteração ficou restrita ao retorno fail-closed antes da política `T/N`; o
  contrato existente de quatro casos temporais foi preservado.
- Não foram alterados snapshot, distância canônica, Health Connect, UI,
  migration, schema, `persistImported` ou decodificação D4 v1/v2.
- Nenhum limiar de plausibilidade foi inventado; indicadores continuam
  independentes da resolução temporal.

### Validação executada

| Comando | Resultado |
| --- | --- |
| `./gradlew lint assembleDebug assembleRelease` | PASS |
| `./gradlew installDebug` | PASS; instalado em `SM-S901E - 16` |
| `git diff --check` | PASS |
| `git status --short` | PASS; somente arquivos desta subtarefa/documentação |
| Revisão independente | Aprovada com ressalvas; sem bloqueadores para 014-05 |
| Testes automatizados | Não executados, conforme solicitação |

### Riscos residuais

- A persistência do snapshot e das durações efetivas continua bloqueada até
  `014-07`; esta subtarefa somente corrige o contrato do resolvedor e a
  proveniência de `endedAt`.
- Duplicatas de pacotes D4 são normalizadas pelo parser antes da derivação; a
  `ParserValidity.PARTIAL` precisa continuar bloqueando derivação futura.
- A validação física de D4 e a política de plausibilidade continuam pendentes
  para subtarefas posteriores.

## Definição de pronto

Política temporal, índices e plausibilidade resolvidos no contrato sem síntese e
prontos para persistência posterior, com item `014-05` marcado. A criação do
snapshot e a gravação dos campos derivados permanecem fora desta subtarefa.

## Rollback

Desativar o resolvedor para novas derivações e preservar campos raw/measured já
adicionados. Não apagar intervalos.
