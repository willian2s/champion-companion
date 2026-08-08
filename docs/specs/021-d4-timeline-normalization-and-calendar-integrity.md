# 021 — Normalização temporal D4 e integridade do calendário

- **Ticker:** `021`
- **Status:** `completed`
- **Base:** `feat/014-watch-workout-derived-metrics`
- **Progresso:** `4/4`

## Contexto

A Fase 014 foi concluída com derivação canônica por passos, série granular e
projeções filhas no Health Connect. Evidência física posterior revelou duas
falhas independentes na implementação efetiva.

### Falha A — duração e timeline D4

No layout preferido atual, `ChampionD4WorkoutParser` interpreta o fim do pacote 1
somente até minuto e força `second = 0`. Comparação entre banco local preservado e
o aplicativo oficial do relógio confirmou três atividades em que o byte 6 do
pacote 2 completa exatamente os segundos finais esperados:

- duração persistida `15:04` + `24 s` → duração oficial `15:28`;
- duração persistida `20:03` + `42 s` → duração oficial `20:45`;
- duração persistida `29:01` + `14 s` → duração oficial `29:15`.

A terceira atividade também fornece uma contraevidência física ao contrato
`N × 60 s`: o app oficial mostra duração de `29:15`, zero suspensões e gráficos de
FC/calorias terminando em `32`, enquanto o bloco D4 contém 32 registros íntegros.
CRC, `recordCount`, sequência `0..31` e soma dos passos temporais são coerentes.
Logo, a duração oficial da atividade e a quantidade de samples são conceitos
relacionados, mas a quantidade de samples não é fonte de verdade da duração.

### Falha B — falso `UNRESOLVED_AGGREGATE` no calendário

O banco local usado na investigação possui 84 operações de Health Connect, todas
`SYNCED`, sem `lastError`, `clientRecordId` nulo ou `clientRecordVersion` nulo.
Mesmo assim, o calendário exibe `Dados parciais do mês` e cards sincronizados
mostram `Status indisponível`.

A causa está no read model semântico: `observeHealthSyncSemanticCandidateIndex()`
reconstrói atividades sem carregar o snapshot de derivação ativo nem a série
canônica, enquanto os predicados de `DISTANCE` e `SPEED` exigem exatamente esses
dados. A fila persistida está saudável; a reconstrução do candidato é que o torna
falsamente não resolvido.

## Objetivo

Corrigir duração e alinhamento temporal de `WATCH_D3_D4`, preservando os dados
granulares, e restaurar a integridade semântica da fila lida pelo calendário sem
apagar banco, reimportar via BLE ou alterar registros remotos automaticamente.

## Decisões obrigatórias

### 1. Fonte de verdade temporal

Para `WATCH_D3_D4`:

```text
startedAt + endedAt/duração oficial
    = fonte de verdade temporal da atividade

sampleIndex + decodedSteps + BPM + raw
    = fonte de verdade da sequência granular D4

60 s
    = granularidade nominal observada/proveniência
    ≠ duração obrigatória de cada sample
    ≠ fonte de verdade da duração da atividade
```

O contrato `T/N/60 s` da ADR-018 é supersedido para a nova derivação por ADR-019.

### 2. Segundo final do layout preferido

No layout preferido, `packet2[6]` deve ser interpretado como `endSecond` quando o
valor estiver em `0..59`. A implementação deve preservar fail-closed para valor
inválido e não inferir segundos a partir de `recordCount`.

O layout legado continua usando o segundo final já presente no pacote 1; não deve
ser alterado por essa evidência.

### 3. Versionamento

- novas importações corrigidas usam `parserVersion = d4-v3`;
- derivação normalizada usa nova versão, inicialmente
  `derivationVersion = watch-d3-d4-v2`;
- `d4-v1` e `d4-v2` permanecem identificáveis e seus dados brutos não são
  reescritos silenciosamente.

A troca do runtime para `d4-v3` só ocorre quando duração e normalização estiverem
coerentes no mesmo fluxo; uma subtarefa intermediária não deve publicar
semântica parcial como nova versão efetiva.

### 4. Normalização dos samples

Com duração oficial positiva `T` e `N > 0` registros estruturais elegíveis, os
samples são distribuídos determinística e contiguamente em toda a janela da
atividade.

A implementação deve usar divisão inteira com resto para que:

```text
Σ effectiveDuration[i] = T
start[0] = workout.startedAt
end[N-1] = workout.endedAt
end[i] = start[i+1]
```

Não há sobreposição, lacuna, cauda sintética nem descarte de samples apenas
porque `N × 60 s` excede `T`.

O valor nominal de `60 s` continua preservado como proveniência do D4. A duração
efetiva usada em distância temporal, cadência, velocidade, ritmo, splits e
projeções é a duração normalizada.

### 5. Passos e distância

Cada sample preserva `sampleIndex`/ordem e `decodedSteps`. A distância canônica
continua:

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
canonicalDistanceMeters = Σ canonicalIntervalDistanceMeters
```

A mudança temporal não altera passos nem comprimento pessoal do passo.

### 6. BPM e índice de origem

BPM válido continua sendo fato recebido do sample D4. Seu timestamp absoluto é
derivado da timeline normalizada e não deve ser descrito como timestamp medido
pelo relógio.

Para preservar a associação mesmo quando algum sample não contém BPM válido, o
modelo persistente deve guardar um índice de origem por amostra de FC de treino
D4, nullable para origens que já possuem timestamp real. A implementação deve
avaliar uma migration aditiva `v10 → v11`, sem efeitos remotos e sem derivar
métricas durante a migration.

Para legado `d4-v2`, o índice pode ser recuperado somente quando a relação com o
timestamp nominal antigo de `60 s` for exata e inequívoca; demais linhas ficam
sem índice e falham fechadas para reprocessamento temporal granular.

### 7. Parser validity

`outOfDurationSampleCount` calculado por `startedAt + sampleIndex × 60 s` não pode
mais tornar um bloco estruturalmente íntegro `PARTIAL`, pois essa comparação usa
a hipótese temporal supersedida.

`PARTIAL` continua válido para timeout, pacote ausente, duplicata, `recordCount`
divergente, CRC divergente e outras inconsistências estruturais reais.

### 8. Reprocessamento local existente

Nenhuma atividade é recalculada silenciosamente ao abrir o app, migrar schema,
alterar perfil ou observar o calendário.

Atividade existente elegível pode ser reprocessada explicitamente usando os
fatos persistidos e, quando necessário para recuperar `endSecond`, o payload
diagnóstico local já associado. Não deve ocorrer nova leitura BLE.

O cenário físico que motivou a task — atividade `PENDING_REVIEW`, `d4-v2`, 32
samples e payload diagnóstico preservado — deve ser normalizável in-place, sem
limpar dados do app e sem trocar o ID da atividade.

Atividade já `SYNCED` não recebe alteração ou ressincronização silenciosa. Qualquer
reprocessamento que mude projeção remota continua obedecendo os guards e a
reconciliação explícita da Fase 014.

### 9. Health Connect

Projeções de `DISTANCE`, `SPEED` e BPM do treino usam a timeline normalizada da
nova derivação. Retry continua usando payload congelado; abrir o calendário não
recalcula nem escreve no Health Connect.

### 10. Calendário e integridade semântica

O índice semântico da fila deve reconstruir `WorkoutActivity` com o snapshot
ativo e a série canônica necessária para avaliar `DISTANCE` e `SPEED`.

A leitura do calendário:

- não cria, repara, exclui ou altera operações;
- não executa sync;
- não consulta BLE;
- continua degradando para operação realmente inválida, aggregate órfão ou
  atividade semanticamente inelegível;
- não degrada uma operação `SYNCED` apenas porque o candidate builder omitiu a
  derivação persistida.

## Persistência planejada

A mudança pode exigir `Room v11` somente para preservar o índice de origem das
amostras de FC D4. Não criar migration de distância, snapshots ou fila se o
schema atual já comportar a timeline normalizada.

A migration deve ser aditiva e restrita a dados locais. É proibido:

- gerar snapshot derivado durante migration;
- mudar workflow;
- reprocessar atividades automaticamente;
- criar operação Health Connect;
- executar efeito remoto.

## Evidência sanitizada disponível

O planejamento parte de banco local real fornecido explicitamente pelo usuário:

- `PRAGMA integrity_check = ok`;
- schema atual `v10`;
- quatro atividades D3/D4 preservadas;
- uma atividade `PENDING_REVIEW` com 32 registros e inconsistência apenas contra
a antiga regra rígida de duração;
- 84 operações Health Connect, todas `SYNCED`, sem erro ou identidade ausente;
- três durações oficiais comparáveis fechando exatamente com `packet2[6]`.

IDs, payloads integrais e métricas pessoais não devem ser copiados para logs,
documentação pública ou evidência de PR.

## Fora do escopo

- GPS, rota, mapa, `ExerciseRoute` ou treino iniciado pelo celular;
- importação silenciosa ou background BLE;
- leitura ampla do Health Connect;
- alteração de distância original/calibrada como fonte canônica;
- interpretar os 16 bits inferiores de `rawMotionValue`;
- semântica nova para distância/calorias raw dos samples;
- suavização, ML ou stride dependente de cadência;
- apagar banco, limpar dados do app ou reiniciar histórico;
- testes automatizados novos ou execução de suites, salvo pedido explícito do
  usuário.

## Subtarefas

```text
021-01 ──→ 021-02 ──┐
                    ├──→ 021-04
021-03 ──────────────┘
```

- `021-01` — corrigir fonte de duração D4 e contrato `d4-v3`;
- `021-02` — normalizar samples na duração oficial e permitir reprocessamento
  local preservado;
- `021-03` — restaurar derivação no candidate semântico da fila/calendário;
- `021-04` — validar banco existente, dispositivo e fechar documentalmente.

`021-03` é funcionalmente independente de `021-01/02` e não deve esperar a
correção D4 para eliminar o falso `UNRESOLVED_AGGREGATE`.

## Critérios de aceite globais

1. Novas importações preferidas usam segundos finais corretos e `d4-v3`.
2. Duração oficial não é substituída por `recordCount × 60 s`.
3. Todos os samples estruturais elegíveis permanecem granulares e cobrem
   exatamente a duração oficial após normalização.
4. Passos e BPM preservam valor e ordem de origem; timestamps derivados são
   identificáveis como normalizados.
5. A atividade física de 32 samples pode ser reprocessada explicitamente no
   banco existente, sem limpeza nem nova coleta BLE.
6. Distância, acumulado, cadência, velocidade, ritmo e splits usam a nova
   timeline sem perder nenhum sample elegível.
7. Operações Health Connect previamente sincronizadas não são duplicadas ou
   reexecutadas apenas pela atualização.
8. Candidate semântico do calendário carrega derivação ativa; fila saudável não
   gera `UNRESOLVED_AGGREGATE` falso.
9. `Dados parciais do mês` desaparece quando não existe outra issue real, e
   `Status da fila local` volta a refletir o estado persistido.
10. Falhas reais continuam fail-closed e observáveis.
11. Nenhum dado sensível é adicionado a logs ou docs.
12. `git diff --check`, `./gradlew lint assembleDebug assembleRelease` e
    `./gradlew installDebug` possuem resultado registrado na validação física.

## Validação

Não criar nem executar testes automatizados como etapa padrão. A validação final
usa o banco existente e o Galaxy S22, preservando dados do app.

Além dos comandos de build, a validação deve confirmar:

- upgrade do banco sem wipe;
- duração das atividades conhecidas contra o app oficial;
- reprocessamento explícito da atividade de 32 samples;
- soma das durações normalizadas igual à duração oficial;
- soma de passos preservada;
- ausência de duplicação de Health Connect observável nos cenários exercitados;
- calendário sem falso estado parcial quando a fila local estiver semanticamente
  íntegra.

## Relação com decisões anteriores

- ADR-017 continua definindo direção watch-first e distância por passos.
- ADR-018 continua definindo raw/measured/derived/projection, distância canônica e
  projeções filhas, exceto pela política temporal de `60 s` supersedida pela
  ADR-019.
- ADR-016 continua definindo integridade temporal de reimportação.
- Spec 018 continua fechada; `021-03` corrige uma causa nova e independente de
  `UNRESOLVED_AGGREGATE`.

## Estado factual da validação

- Upgrade sem wipe e migration `v10 → v11` foram observados no Galaxy S22.
- A atividade de 32 samples foi reprocessada in-place com duração `29:15`.
- O caso `15:28` foi reprocessado explicitamente; o caso `20:45` foi conferido
  por envelope diagnóstico preservado sem reabrir atividade `SYNCED`.
- Calendário offline não exibiu estado parcial falso e a fila permaneceu com 84
  operações `SYNCED`, sem erro ou identidade ausente.
- Evidência sanitizada: `docs/integration/021-04-validation-and-close.md`.
- Fechamento global concluído com revisão independente aprovada com ressalvas.
- O runtime efetivo usa `d4-v3`, derivação `watch-d3-d4-v2` e Room schema `v11`.
