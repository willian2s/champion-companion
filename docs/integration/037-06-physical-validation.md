# 037-06 — Validação física e fechamento

- **Data:** 2026-09-29
- **Dispositivo:** Champion C001 real, validado pelo app no `SM-S901E`
- **Artefato:** Release `app-release.apk`, instalação incremental
- **Privacidade:** evidência sanitizada; sem IDs, MAC, payload BLE, localização,
  BPM individual ou dados pessoais desnecessários
- **Status:** `completed`, com N/A explícito para cenários inexistentes

## Validação técnica

```text
git diff --check                              -> passou
./gradlew lint assembleDebug assembleRelease  -> BUILD SUCCESSFUL
adb devices -l                                -> 1 dispositivo SM-S901E
adb install -r app-release.apk                -> Success
```

Nenhum teste automatizado novo foi criado ou executado. A instalação ocorreu
sobre dados existentes, sem limpar armazenamento, desinstalar, reimportar ou
rederivar.

## Banco físico e Room

Inspeção read-only antes da navegação e após instalação, abertura, navegação,
reabertura e recomposição confirmou:

```text
Room user_version:              20
workout_activities:             48
workout_heart_rate_samples:   1470
workout_derivation_snapshots:   71
workout_derived_intervals:    2209
workout_splits:                211
distance_calibration_evidence:   6
health_sync_operations:       8275
trend/performance tables:        0
```

Não houve migration, tabela, coluna, cache, snapshot analítico ou backfill.

## População e cálculos independentes

Foi usada uma janela explícita de 56 dias, com população SQL sanitizada:

```text
origin = WATCH_D3_D4
confirmedType = RUNNING
workflowState != DISCARDED
```

Resultado:

```text
corridas confirmadas RUNNING:                       21
snapshot ativo presente:                            21
escalares canônicos positivos:                      21
temporalidade ELIGIBLE:                             21
WATCH_NATIVE_CONTEXT_CALIBRATED / v4 comparáveis:   5
fallback por comprimento de passo:                  16
TREADMILL na população:                              0
suggestedType sem confirmação usado:                 0
```

Pace foi recalculado para as 21 linhas usando somente:

```text
(canonicalCoveredDurationNanos / 1e9)
/ (canonicalDistanceMeters / 1000)
```

Nenhum agregado persistido de pace, split, intervalo derivado ou header foi
usado. As 5 linhas nativas v4 compuseram os dois gráficos. As 16 linhas de
fallback apareceram na lista factual e ficaram fora dos gráficos comparáveis,
conforme a Spec 037. Portanto, não há pontos adicionais escondidos à direita do
gráfico; a diferença entre 21 linhas e 5 pontos é provenance, não clipping.

A FC foi recalculada com filtro da janela canônica, validação `30..220`,
timestamps crescentes e sample-and-hold compartilhado com a Spec 034:

```text
médias ponderadas recalculadas: 21
coverage parcial:               21
coverage completa:               0
sem samples:                     0
samples inválidos:               0
```

Não foram registrados valores individuais de BPM.

## UI, ordem e reabertura

A árvore de acessibilidade confirmou:

- entrada contextual `Tendência de corrida` em Exercícios;
- período `Últimos 56 dias`;
- 21 corridas na lista;
- provenance de distância estimada e calibrada;
- dois gráficos independentes, ritmo e FC;
- 5 pontos em cada gráfico, todos os pontos nativos comparáveis;
- recomposição da mesma saída após voltar, reabrir e aguardar carga.

Não surgiram naturalmente múltiplas corridas no mesmo dia nem empate de instante.
A ordenação e o desempate permanecem cobertos por inspeção do contrato e do
read model, sem fabricar cenário.

## Preservação e efeitos

Assinaturas SHA-256 sanitizadas e contagens foram iguais antes/depois da
instalação, após abertura e após reabertura para:

```text
workout_activities
workout_heart_rate_samples
workout_derivation_snapshots
workout_derived_intervals
workout_splits
distance_calibration_evidence
health_sync_operations
```

Health Connect não foi acionado pelo caminho da tela. Analytics permaneceu
efêmero e não apareceu no schema.

## N/A e riscos residuais

Ficaram `N/A`, por ausência natural no banco físico:

1. corrida sem FC;
2. FC completa;
3. amostra inválida ou timestamp não crescente;
4. pace inelegível ou snapshot ausente;
5. falha estrutural da fonte;
6. versão nativa não suportada ou método/proveniência desconhecida;
7. indicador de plausibilidade bloqueador ou fatos canônicos inválidos;
8. fonte de FC indisponível por falha batch;
9. múltiplas corridas no mesmo dia;
10. empate exato de instante.

Nenhum treino, sample, fixture ou estado foi criado para preencher esses casos.
Os estados tipados permanecem cobertos por contrato e policy, mas não receberam
exercício físico nesta rodada.
Permanece risco herdado de custo da query sem índice temporal composto.

## Screenshot

- [`037-06-running-performance.png`](037-06-running-performance.png)

Screenshot sanitizada da tela de tendência. Status bar, janela flutuante,
gráficos com valores e navegação inferior foram mascarados; nenhum identificador,
timestamp completo ou BPM individual foi preservado.
