# 036-06 — Validação física e fechamento

- **Data:** 2026-09-23
- **Dispositivo:** Champion C001 real, validado pelo app no `SM-S901E`
- **Branch:** `feat/036-heart-rate-zone-history`
- **Privacidade:** evidência sanitizada; sem BPM individual, samples, IDs,
  MAC, payload BLE ou dados pessoais desnecessários
- **Status:** `completed`, com N/A explícito para cenários que não existiam
  naturalmente no banco físico

## Validação técnica

```text
git diff --check                              -> passou
./gradlew lint assembleDebug assembleRelease  -> BUILD SUCCESSFUL
./gradlew installDebug                       -> instalado em 1 dispositivo
```

Nenhum teste automatizado novo foi criado ou executado. A instalação ocorreu
sobre dados existentes, sem limpar o armazenamento.

## Banco físico e escopo da fonte

Após instalação, abertura, recomposição e reabertura, o banco confirmou:

```text
Room user_version:              20
schema exportado:               20.json
workout_activities:             10
workout_heart_rate_samples:     182
workout_derivation_snapshots:   13
workout_derived_intervals:     231
workout_splits:                  33
health_sync_operations:        874
workflow:                       4 PENDING_REVIEW, 6 SYNCED
origem analítica:              10 WATCH_D3_D4
```

O schema v20 não possui tabela analítica cardiovascular. A inspeção de
`ChampionDatabase`, schema 20, DAO e repository confirmou que a fonte continua
bounded em 56 dias e lê somente atividades elegíveis e samples de FC. O
read-model não lê derivação, snapshots, splits, headers ou distância.

## Cálculo independente

O cálculo externo reutilizou os fatos existentes, janela canônica da sessão,
sample-and-hold da Spec 034, FC máxima pessoal atual e `America/Sao_Paulo`.
Nenhum treino, sample ou fixture foi criado.

```text
                        current7       current28
treinos                     3              10
analisados                  3              10
duração total         2825,000 s      10607,000 s
duração coberta       2743,284 s      10333,728 s
coverage ratio          0,971074        0,974237
carga                 195,3513         551,2448 pontos
soma das zonas        2743,284 s      10333,728 s
soma dos percentuais     100,000%        100,000%
```

Distribuição independente, em `current7`:

```text
BELOW_ZONE_1:     0,000 s /  0,000%
Z1:             147,885 s /  5,391%
Z2:             295,770 s / 10,782%
Z3:             176,839 s /  6,446%
Z4:             162,813 s /  5,935%
Z5:            1959,977 s / 71,446%
```

Distribuição independente, em `current28`:

```text
BELOW_ZONE_1:  1163,471 s / 11,259%
Z1:            2036,136 s / 19,704%
Z2:             802,372 s /  7,765%
Z3:             889,108 s /  8,604%
Z4:             446,722 s /  4,323%
Z5:            4995,919 s / 48,346%
```

As somas por zona fecham a duração analisada. Os percentuais usam somente o
tempo coberto, enquanto `coverageRatio` usa duração coberta sobre duração total.
As seis zonas, inclusive `BELOW_ZONE_1` e zeros, foram observadas na UI.

Os buckets civis de `current28` tocaram 29 datas, conforme contrato para janela
não alinhada ao início do dia. Seis datas tinham workouts; as demais exibiram
zero real de ausência de treino. O fechamento independente por zona teve erro de
`0,000 s` em todas as seis zonas.

## Recomposição, restauração e efeitos

A FC máxima foi alterada temporariamente para outro valor válido. A UI
recompôs em memória:

```text
configuração original: current7 195,4 pontos; Z5 32:39 / 71%
configuração temporária: current7 179,4 pontos; Z5 25:20 / 55%
após restauração: current7 195,4 pontos; current28 551,2 pontos
```

Após restauração, force-stop e reabertura, o histórico voltou à saída original.
Não houve tabela, cache, snapshot ou backfill analítico.

O snapshot capturado antes da abertura tinha 9 activities, 12 snapshots e 227
derived intervals. Durante a inicialização preexistente do app, antes de a
validação da página terminar, esses fatos passaram a 10, 13 e 231. A causa não
foi isolável nesta rodada; portanto, a diferença não foi atribuída à 036. O
snapshot pós-inicialização permaneceu idêntico após recomposição, mudança e
restauração da FC máxima, e reabertura.

Assinaturas sanitizadas do snapshot pós-inicialização/restaurado:

```text
workout_activities:           98661221b31485a4513c5fd571de7c642bca3d1e078b3cb02edb63cb18e345d0
workout_heart_rate_samples:   f317a7a687a9d831c5546fe27afa6197cb70256207271058f37022110cbfd7ab
workout_derivation_snapshots: 0d24b03e36284531ab68e923fc69c446b06626c3308960322aa40be89896fc7d
workout_derived_intervals:    e0f4c53537f9c9a87746b8930be31c6ff5ba534c629fc2ae4acfa60c259496e6
workout_splits:               0e4d658d42816988a94c397cc31344a17bdea38bd7a35bf486b380da7c1c9e14
health_sync_operations:       5c0df5e232fa8aa7a4aa99734db8ced331c4d0450a3cd46c5819c499c9df9490
```

Para manter comparação auditável sem expor fatos pessoais, o snapshot inicial
teve estas assinaturas e contagens:

```text
workout_activities:           9   07fb0921370c49da328044d873b42267043d37345eb4e5d72195dc154577aaf0
workout_heart_rate_samples: 182   f317a7a687a9d831c5546fe27afa6197cb70256207271058f37022110cbfd7ab
workout_derivation_snapshots: 12   db3d6144abd183f8a20e492e21baa6f68f0547797917937157319268a0405441
workout_derived_intervals:   227   4ba4e4bc0ee4b0d1ef65b86824f32c67dbe78c42f79df54226a9070f34359188
workout_splits:               33   0e4d658d42816988a94c397cc31344a17bdea38bd7a35bf486b380da7c1c9e14
health_sync_operations:      874   5c0df5e232fa8aa7a4aa99734db8ced331c4d0450a3cd46c5819c499c9df9490
```

`health_sync_operations` permaneceu com 874 linhas e a mesma assinatura em
todos os snapshots pós-inicialização. A inspeção do call path confirmou que
`ExercisesViewModel` usa somente `observeCardiovascularLoadWorkoutsBetween`,
`observeSnapshot` e `CardiovascularLoadHistoryPolicy`; esse caminho não chama
Health Connect nem escreve no banco. Operações remotas não foram atribuídas à
036.

## Cenários N/A e riscos residuais

1. Nenhum workout com carga `0,0` existia naturalmente; ficou `N/A`. Nenhum
   workout ou sample foi fabricado.
2. Todos os 10 workouts existentes tinham samples válidos; workout sem análise
   ficou `N/A`. O contrato preserva ausência tipada e `partial`, mas o estado
   não recebeu exercício físico nesta rodada.
3. Falha estrutural, máxima ausente e população vazia não foram fabricadas;
   permanecem cobertos pelos estados tipados e pela evidência das subtarefas
   anteriores.
4. A diferença entre snapshots inicial e pós-inicialização não teve causa
   isolada. Ela permanece risco documental de atribuição, não evidência de
   mutação causada pela 036.
5. O custo da query sem índice temporal composto permanece risco herdado da
   Spec 035.

## Screenshot

Screenshot sanitizada da distribuição diária por zona:

- [`036-06-heart-rate-zone-history.png`](036-06-heart-rate-zone-history.png)

O recorte mostra gráfico de carga, seleção acessível de zona, duração diária,
distinção entre ausência de treino e parcialidade e contribuição agregada por
tipo. Não contém BPM individual, IDs, MAC, payload BLE ou dados pessoais.

## Conclusão

Todos os critérios exercitáveis receberam evidência ou N/A justificado. Room
permanece v20, a distribuição fecha por período e bucket, a recomposição por FC
máxima foi observada e restaurada, e nenhum efeito Health Connect foi criado
pelo caminho da 036.
