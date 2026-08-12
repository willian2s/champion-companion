# 022-03 — Adaptar métricas e UI locais para bicicleta

- **Status:** `completed`
- **Depende de:** `022-02`
- **Desbloqueia:** `022-04`

## Objetivo

Tornar todos os consumers locais type-aware para que `CYCLING` nunca reutilize
métricas derivadas de passos como distância, velocidade, pace, cadência ou
splits, preservando ao mesmo tempo duração, BPM, calorias e dados raw.

## Política funcional

Quando `confirmedType == CYCLING`:

```text
Distância
→ manualDistanceMeters, se presente
→ ausente, caso contrário

Velocidade média
→ manualDistanceMeters / duração oficial, se ambos válidos
→ ausente, caso contrário

Pace
→ ausente

Cadência
→ não usar steps/min como cadência de bicicleta

Splits por km
→ não usar série derivada de passos
```

Snapshots e intervalos derivados existentes permanecem persistidos para
proveniência/diagnóstico, mas deixam de ser elegíveis como métricas de ciclismo.

## Consumers a adaptar

Usar o relatório `022-01` como checklist autoritativo. Incluir pelo menos:

- detalhe da atividade;
- cards/lista de exercícios;
- calendário e detalhe diário;
- agregações/históricos locais que usem distância de treino;
- labels e formatação de tipo;
- qualquer helper de distância final/velocidade/pace/splits;
- estados vazios/ausentes.

## UI de detalhe

Com distância:

```text
Bicicleta
Duração          <valor>
Distância        <km>
Velocidade média <km/h>
FC média         <valor quando disponível>
FC máxima        <valor quando disponível>
Calorias         <valor quando disponível>
```

Sem distância:

```text
Bicicleta
Duração          <valor>
Distância        —
Velocidade média —
FC média         <valor quando disponível>
Calorias         <valor quando disponível>
```

Não exibir como métrica principal de bicicleta:

- pace;
- cadência de passos;
- splits derivados por distância de passos;
- distância estimada por passada.

Passos D4 podem continuar existindo no modelo e diagnóstico, mas não devem ser
promovidos visualmente como deslocamento da bicicleta.

## Regras de cálculo

- usar duração oficial já persistida pela política D4 vigente;
- não persistir velocidade média redundante se puder ser calculada de forma
  determinística no read model;
- não alterar `watch-d3-d4-v2`;
- não recalcular/reescrever snapshots ao trocar o tipo confirmado;
- consumers devem falhar fechados quando distância manual estiver ausente ou
  inválida.

## Validação

- `git diff --check`;
- `./gradlew lint assembleDebug assembleRelease`;
- inspeção manual de atividade cycling com e sem distância;
- confirmar que running/walking/treadmill mantêm apresentação vigente.

## Execução

- **Status:** `completed` — revisão independente aprovada com ressalvas não
  bloqueadoras.
- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/CalendarHistory.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/WatchWorkoutActivity.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/calendar/CalendarDayScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- **Decisões:**
  - calendário transporta `manualDistanceMeters` no read model e usa esse valor
    somente para `CYCLING`, aceitando apenas valores positivos e finitos;
  - detalhe/lista calculam velocidade média ciclística de forma determinística
    com distância manual e duração oficial;
  - série/gráfico, intervalos, splits, pace, cadência de passos e estados de
    reprocessamento por passos ficam ocultos para `CYCLING`, enquanto snapshots
    permanecem persistidos;
  - `DailySummary` e históricos gerais de distância não foram alterados.
  - estado inconsistente `CYCLING + PENDING_CALIBRATION` falha fechado com
    mensagem explícita, sem abrir formulário de calibração de esteira.
  - aceite explícito de plausibilidade da série por passos é ocultado, limpo e
    rejeitado para `CYCLING` na UI, ViewModel e domínio.
- **Desvios:** nenhum requisito alterado. Nenhum parser, derivação, snapshot,
  Health Connect ou teste novo foi modificado.
- **Comandos executados:**
  - `git diff --check`
  - `./gradlew app:compileDebugKotlin`
  - `./gradlew lint assembleDebug assembleRelease` (primeira execução excedeu o
    timeout da ferramenta após concluir os assemblies; execução subsequente passou)
  - `./gradlew lint assembleDebug assembleRelease`
- **Resultados e evidências:** `git diff --check` passou; compilação Kotlin,
  lint, `assembleDebug` e `assembleRelease` passaram após correção. DAO e
  mapper transportam distância manual; UI de atividade e calendário falham
  fechados sem distância e não leem série por passos para `CYCLING`. Aceite de
  plausibilidade por passos também falha fechado. Revisão independente anterior
  encontrou esse bloqueio; correção aplicada e nova revisão aprovou a mudança
  com ressalva não bloqueadora. Nenhum teste automatizado foi executado,
  conforme regras do repositório.
- **Revisão independente:** aprovada com ressalva não bloqueadora em 2026-08-11;
  calibração histórica permanece visível somente como diagnóstico, sem consumo
  por métricas de `CYCLING`.
- **Riscos residuais:** inspeção manual em dispositivo ainda pendente. `022-04`
  continua responsável por projeções Health Connect e `022-05` por upgrade
  físico e validação final.

## Fora do escopo

- Health Connect;
- RPM/power;
- GPS/rota;
- alteração de parser;
- testes automatizados novos.

## Critérios de aceite

- [x] nenhum consumer local usa distância canônica por passos para `CYCLING`;
- [x] velocidade média só existe com distância manual válida;
- [x] pace, cadência de passos e splits de passos não aparecem como métricas de bicicleta;
- [x] BPM/calorias/duração continuam disponíveis;
- [x] snapshots raw/derived continuam preservados;
- [x] calendário/históricos respeitam ausência de distância;
- [x] outros tipos mantêm comportamento existente;
- [x] build/lint passam;
- [x] revisão independente aprova a mudança.
