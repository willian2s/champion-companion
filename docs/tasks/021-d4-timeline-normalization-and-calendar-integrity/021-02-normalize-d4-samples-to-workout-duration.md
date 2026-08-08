# 021-02 — Normalizar samples D4 na duração oficial

- **Ticker:** `021`
- **Número:** `02`
- **Status:** `completed`

## Objetivo

Substituir a política rígida de buckets efetivos de `60 s` por uma timeline
normalizada que preserve todos os samples D4 elegíveis dentro da duração oficial
do treino, mantendo passos e BPM granulares e permitindo reprocessar atividades
existentes sem limpar o app.

## Resultado esperado

`WATCH_D3_D4` passa a usar:

```text
duração oficial T
+ N samples estruturais ordenados
        ↓
N janelas derivadas contíguas que cobrem exatamente T
```

A nova derivação usa `watch-d3-d4-v2`; o parser completo passa a `d4-v3` somente
quando esta subtarefa estiver pronta.

## Dependências

- `021-01` concluída;
- ADR-019;
- contratos raw/measured/derived/projection da ADR-018;
- Fase 014 concluída com Room v10 e projeções filhas.

## Arquivos e símbolos prováveis

- `core/model/WatchWorkoutActivity.kt`;
- classes de derivação temporal e `WorkoutDurationResolver` em `core/model`;
- `core/bluetooth/ChampionD4WorkoutParser.kt`;
- `data/RoomRepositories.kt`;
- `data/DatabaseMappers.kt`;
- `core/database/ChampionDatabase.kt`;
- `core/database/ChampionDaos.kt`;
- `core/database/ChampionMigrations.kt`;
- `core/database/DatabaseModule.kt`;
- schema Room v11, se o índice de origem de BPM exigir coluna nova;
- UI de detalhe somente se necessária para expor/reusar ação explícita de
  reprocessamento já existente;
- spec/ADR/protocolo/overview 021.

## Escopo incluído

### Timeline normalizada

- exigir `T > 0`, `N > 0` e índices estruturais elegíveis `0..N-1`;
- dividir `T` deterministicamente entre `N` samples;
- distribuir resto inteiro sem perder nanos;
- garantir janelas positivas, contíguas, ordenadas e contidas na sessão;
- garantir soma exata das durações = `T`;
- preservar `60 s` apenas como duração nominal/proveniência;
- remover `INSUFFICIENT_TOTAL_DURATION` como bloqueio causado apenas por
  `N × 60 s > T`;
- não criar cauda sintética nem descartar samples para fazer a série caber.

### BPM granular

- preservar BPM no sample de origem;
- persistir associação ao índice D4 de origem de forma robusta;
- avaliar/adotar migration `v10 → v11` para índice nullable em
  `workout_heart_rate_samples`;
- manter origens com timestamp real compatíveis sem exigir índice D4;
- posicionar BPM derivado dentro da janela normalizada correspondente, com
  política determinística registrada na ADR/spec;
- não tratar timestamp normalizado como timestamp medido pelo relógio.

A política inicial recomendada é o midpoint da janela normalizada, mantendo a
mesma convenção temporal já usada para `SpeedRecord`.

### Parser validity

- deixar de usar `startedAt + index × 60 s > endedAt` como evidência isolada de
  bloco `PARTIAL`;
- preservar `PARTIAL` para timeout, faltas, duplicatas, `recordCount`
  divergente, CRC e demais inconsistências estruturais reais.

### Derivação

- atualizar versão para `watch-d3-d4-v2`;
- recalcular distância por sample com os mesmos `decodedSteps`;
- recalcular acumulado, cadência, velocidade, ritmo e splits sobre a duração
  normalizada;
- manter `canonicalDistanceMeters` como soma exata dos intervalos;
- manter `stepLengthMetersUsed` congelado por snapshot;
- preservar zero passos como distância/cadência/velocidade zero e pace ausente.

### Reprocessamento existente

- reprocessamento continua explícito;
- usar fatos locais persistidos e payload diagnóstico local quando necessário
  para recuperar `endSecond`/proveniência;
- não executar nova leitura BLE;
- preservar `activityId`, título, notas, revisão, workflow e calibração histórica
  conforme contratos existentes;
- atividade `PENDING_REVIEW` física com 32 samples precisa gerar nova derivação
  normalizada sem wipe;
- atividade `SYNCED` não é alterada nem ressincronizada silenciosamente.

### Ativação `d4-v3`

Somente ao final desta subtarefa:

- coordinator passa a solicitar `d4-v3` para novas importações;
- domínio passa a aceitar/restaurar `d4-v3`;
- parser `d4-v3` entrega duração corrigida + índices + timeline coerente;
- `d4-v1`/`d4-v2` continuam reconhecidos para legado e reprocessamento seguro.

## Escopo excluído

- alterar comprimento do passo ou fórmula de distância;
- interpretar raw de distância/calorias dos samples;
- corrigir candidate semântico do calendário (`021-03`);
- ressync automático;
- leitura remota Health Connect;
- GPS, rota, phone workout ou BLE background;
- testes automatizados.

## Algoritmo temporal mínimo

A implementação deve evitar ponto flutuante para construir janelas.

Exemplo conceitual:

```text
base = totalDurationNanos / N
remainder = totalDurationNanos % N

duration[i] = base + (i < remainder ? 1 ns : 0 ns)
start[i] = start[i-1] + duration[i-1]
```

Qualquer implementação equivalente é aceitável desde que seja determinística e
feche exatamente em `endedAt`.

## Critérios de aceite

1. `29:15 / 32` produz 32 janelas contíguas e termina exatamente em `29:15`.
2. Nenhum dos 32 samples físicos é descartado apenas por duração nominal.
3. A soma de `decodedSteps` permanece idêntica antes/depois do reprocessamento.
4. `canonicalDistanceMeters` permanece soma de `decodedSteps × stepLength`.
5. BPM válido mantém associação ao sample de origem mesmo com samples sem BPM.
6. A nova derivação é identificável como `watch-d3-d4-v2`.
7. Novas importações completas passam a `d4-v3` somente após todos os contratos
   desta task estarem ativos.
8. `outOfDurationSampleCount` derivado da antiga grade de 60 s não torna bloco
   íntegro `PARTIAL`.
9. Reprocessar a atividade motivadora não exige limpar app nem nova coleta BLE.
10. Abrir/migrar o app não reprocessa nem ressincroniza silenciosamente.
11. Se houver migration v11, ela é aditiva, exporta schema e não cria snapshots
    ou operações Health Connect.
12. Health Connect continua usando payload congelado e ação explícita.

## Validação manual

Com banco preservado:

- confirmar schema upgrade sem wipe, quando aplicável;
- conferir contagem de intervals e BPM source indexes;
- executar reprocessamento explícito da atividade de 32 samples;
- conferir duração oficial `29:15`;
- conferir cobertura temporal total e ausência de intervalos fora da sessão;
- conferir distância e métricas granulares no detalhe;
- não sincronizar remotamente como efeito colateral da validação local.

## Comandos permitidos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não criar nem executar testes automatizados.

## Definição de pronto

- timeline normalizada implementada e versionada;
- persistência de índice BPM resolvida de forma compatível;
- `d4-v3` ativado de forma completa;
- reprocessamento local preservado validado no banco real;
- documentação e overview atualizados;
- revisão independente sem bloqueadores.

## Riscos e rollback

- migration de índice BPM pode não conseguir recuperar linhas legadas ambíguas;
  manter `null` e fail-closed nessas linhas;
- atividades sincronizadas antigas não podem receber projeção nova sem
  reconciliação explícita; não tocá-las automaticamente;
- mudança de timeline altera pace/speed/splits, mas não passos/distância total;
- rollback deve preservar schema/dados e permitir continuar lendo snapshots
  antigos sem reprocessamento automático.

## Evidências de execução

- `WorkoutDurationResolver` divide `T` por `N` com divisão inteira em nanos,
  distribui o resto nos primeiros samples e fecha a última janela exatamente no
  fim oficial da sessão.
- A derivação ativa passou para `watch-d3-d4-v2`; snapshots `watch-d3-d4-v1`
  continuam legíveis pela política temporal legada.
- BPM D4 preserva `sourceSampleIndex`; novas linhas persistem segundo/nano exatos
  e usam o ponto médio da janela normalizada. Legado só recupera índice quando a
  posição nominal de 60 s é inequívoca; caso contrário, falha fechado.
- Migration `MIGRATION_10_11` é aditiva, não deriva dados e não cria operações
  Health Connect. Schema Room `11.json` foi exportado.
- Novas importações usam `d4-v3`; o parser não classifica como `PARTIAL` samples
  fora da antiga grade nominal. Reprocessamento explícito pode recuperar duração
  corrigida do envelope diagnóstico local, sem BLE, preservando identidade da
  atividade e bloqueando `d4-v2` quando a recuperação não é segura.
- O envelope diagnóstico local possui limites de contagem, consistência de
  índices/totais e validação integral dos pacotes antes do reprocessamento.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutBlockProtocol.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionD4WorkoutParser.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDatabase.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionMigrations.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/DatabaseModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutDerivedMetrics.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ChampionWatchWorkoutImportCoordinator.kt`
- `app/schemas/br.com.mwconsulting.championcompanion.core.database.ChampionDatabase/11.json`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-00-overview.md`
- `docs/tasks/021-d4-timeline-normalization-and-calendar-integrity/021-02-normalize-d4-samples-to-workout-duration.md`

## Decisões e desvios

- O timestamp normalizado de BPM é persistido com precisão de segundo e nano,
  mas a associação funcional permanece no índice D4 de origem; não é tratado
  como timestamp medido pelo relógio.
- O reprocessamento recupera `endSecond` reparseando somente o payload diagnóstico
  local no envelope `D4B1`. Payload ausente, inconsistente ou divergente do bloco
  persistido bloqueia reprocessamento legado `d4-v2`.
- A migration remove apenas a unicidade por milissegundo das amostras de BPM;
  identidade continua sendo o ID local. Nenhuma fila, snapshot remoto ou operação
  Health Connect é criada durante upgrade.
- Não foi executada validação física, instalação nem consulta no banco real nesta
  subtarefa; permanecem para `021-04`.

## Comandos executados

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- Testes automatizados — não executados, conforme escopo da subtarefa e regras do
  repositório.

## Resultados e evidências

- Compilação debug/release, lint e empacotamento concluídos com sucesso após a
  implementação da migration, parser `d4-v3`, timeline normalizada e
  reprocessamento local.
- Schema exportado em `app/schemas/.../11.json`.
- Nenhuma leitura BLE, operação Health Connect ou reprocessamento automático foi
  adicionado ao upgrade ou ao caminho de leitura.

## Riscos residuais

- A recuperação de duração depende do payload diagnóstico local preservado e do
  firmware/layout suportado; atividades `d4-v2` sem payload seguro falham fechado.
- A validação física do cenário `29:15 / 32`, upgrade sem wipe, soma de passos e
  estabilidade Health Connect ainda pertence a `021-04`.
- A revisão independente foi solicitada; revisão operacional completa depende da
  validação física e do fechamento integrado da task.
