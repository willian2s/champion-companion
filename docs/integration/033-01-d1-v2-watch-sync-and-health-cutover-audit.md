# 033-01 — Auditoria D1 v2, watch sync e cutover Health Connect

- **Data original:** 2026-09-11
- **Revisão física:** 2026-09-13
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Base:** `main` após merge da Spec 032
- **Natureza original:** auditoria documental; nenhum runtime alterado em 033-01

## Estado real pós-032

A reauditoria da `main` confirmou:

- Room v17;
- novas importações D3/D4 em `d4-v4`;
- `WATCH_NATIVE` disponível desde a importação para série D4 íntegra;
- distância externa manual calibra D4 por fator contextual;
- `STEP_LENGTH_FALLBACK` permanece internamente para legacy/inelegibilidade;
- Home ainda possui coordinator próprio FEA1;
- Saúde ainda possui coordinator D1 e coordinator de medição manual;
- Exercícios ainda possui importação D3/D4 própria;
- Home e D1 ainda podem chamar Health Connect automaticamente após persistência.

## D1 runtime auditado e divergências

`ChampionD1HistoryProtocol` anterior (`d1-v1`) lia somente:

```text
1..2 packetIndex LE
3..4 totalPackets LE
5    hour direto
14   BPM
19   minute
```

E aplicava:

```text
hour 0..23
minute 0..59
BPM 30..220
```

Se BPM fosse inválido, `parsePayload()` retornava `null`; assim
passos/distância/etc. eram perdidos e um pacote final inválido podia impedir a
conclusão normal do collector.

## Contrato D1 v2 congelado

A inspeção anterior do layout oficial `Protocol4`/Champion C001 congelou:

```text
0       D1
1..2    currentPosition LE
3..4    total LE
5       packed dayOffset/hour
        dayOffset = u8 / 32
        hour      = u8 % 32
6..7    caloriesRaw BE
8..9    distanceRaw BE
10..11  steps BE
12..13  sportValue BE
14      BPM raw
15      pressureHigh raw
16      pressureLow raw
17      wearRaw
18      tempOne raw
19      minute
```

Faixa oficial de BPM observada: `30..200`.

Regras de v2:

- pacote estruturalmente válido não depende de BPM válido;
- BPM inválido produz `bpm = null`;
- `currentPosition`/`total` controlam progresso/completude;
- `dayOffset` resolve a data a partir de `baseLocalDate` congelada no início;
- hora e minuto continuam validados civilmente;
- raw desconhecidos permanecem raw.

## Unidades congeladas

Para o layout D1 observado:

```text
caloriesKilocalories = caloriesRaw / 10.0
distanceKilometers   = distanceRaw / 1000.0
distanceMeters       = distanceRaw
```

`sportValue` permanece campo numérico estrutural sem unidade fisiológica
inventada.

## Hipótese inicial D1 × D4 — superada

Antes de persistirmos o D1 completo, o banco histórico continha apenas BPM D1.
Naquela evidência, havia forte redução de BPM D1 durante workouts e poucos pontos
sobrepostos. Com informação incompleta, 033-01 adotou provisoriamente:

```text
D1 = fora do workout
D4 = dentro do workout
bucket D1 intersectante = suprimir por inteiro
```

Essa hipótese foi deliberadamente conservadora e dependia de validação após
033-02/033-03.

## Revisão física pós-033-03

Dois dumps sanitizados do banco Room v18, coletados após implementação do D1 v2,
mostraram:

```text
leitura 1: Σ D1.steps = 2713 = FEA1.steps
leitura 2: Σ D1.steps = 2752 = FEA1.steps
```

A reimportação preservou os samples existentes e inseriu somente dois buckets
novos. O crescimento de D1 e FEA1 também foi idêntico.

A mesma evidência continha dois workouts D4 válidos:

1. um workout com `D4 steps/distance/calories = 0`, mas com bucket D1
   temporalmente intersectante contendo passos, distância e calorias não nulos;
2. uma corrida em que os buckets D1 correspondentes fecharam exatamente com o D4
   em `1935` passos e `1349 m`, e praticamente em calorias (`88,2` D1 versus
   `88,224` D4, consistente com as escalas distintas).

Conclusão física:

- D1 é uma linha granular diária que pode incluir fatos do workout;
- D4 é a fonte detalhada/canônica do workout;
- interseção temporal não prova que todo o bucket D1 pertence à atividade;
- somar D1 + D4 duplica fatos em cenários observados;
- suprimir bucket D1 inteiro pode perder fatos externos ao workout.

Evidência completa e sanitizada:
`docs/integration/033-03-d1-v2-physical-validation.md`.

## Autoridade corrigida

```text
FEA1
  -> total diário autoritativo de passos

D1
  -> linha granular diária do relógio
  -> pode conter fatos durante workouts

D4
  -> fonte detalhada/canônica do workout
```

A autoridade diária agora é por finalidade/métrica.

### Passos

- FEA1 = total do dia;
- D1 = distribuição granular do dia;
- D4 = passos/cadência do workout;
- D4 não é somado novamente ao agregado diário.

### Distância e calorias

- D1 = fatos nativos diários quando elegíveis;
- D4 = fatos/canonicalização do workout;
- não somar os dois;
- substituir contribuição D1 por D4 somente com prova determinística de cobertura;
- sem prova, preservar D1 no diário e D4 no workout.

### Frequência cardíaca

D4 pode substituir visualmente pontos D1 dentro da atividade quando houver
cobertura D4 válida. A mera existência da janela D4 não autoriza descartar D1 se
a atividade não possuir amostras D4 válidas.

## Identidade D1

A persistência implementada usa identidade por posição física:

```text
(deviceId, resolvedLocalDate, currentPosition)
```

A validação física confirmou reimportação idempotente: samples existentes
preservaram identidade e novos buckets foram inseridos incrementalmente.

## Partial success do watch sync

Um único sync da Home não implica uma transação única:

```text
FEA1 OK -> persistir
D1   OK -> persistir
D4   falha

resultado global = PARTIAL
```

O próximo refresh pode repetir D4 sem apagar FEA1/D1. D3/D4 mantém deduplicação
por CRC/import já validada pela 032.

Metadata mínima desejada:

```text
lastWatchSyncAttemptAt
lastFea1SuccessAt
lastD1SuccessAt
lastD3D4SuccessAt
```

Nomes finais podem variar, mas estado parcial não pode ser achatado em um único
`lastImportedAt` ambíguo.

## Cutover Health Connect corrigido

Problema a evitar:

```text
DailySummary/FEA1 total do dia
+
D1 granular
+
D4/workout
=
risco de dupla contagem
```

Contrato preservado:

- cutover é uma data local inteira persistida;
- datas anteriores preservam a política histórica;
- datas no/depois do cutover não criam nova projeção STEPS de DailySummary;
- não executar backfill granular histórico;
- não apagar records antigos;
- não reabrir `SYNCED` silenciosamente;
- instalação com legado deve preferir próximo dia local para ativar granular.

Correção adicional:

- pós-cutover não assumir `D1 fora + D4 dentro`;
- STEPS deve preservar o total FEA1 e os guards/créditos da Spec 027;
- D1 granular é candidato à distribuição de passos, mas coexistência com
  `WorkoutSteps` exige contabilidade que prove ausência de duplicidade;
- DISTANCE D1 e D4 também são potencialmente sobrepostos;
- ausência de prova segura deve bloquear/falhar fechada a projeção conflitante.

## Triggers Health Connect auditados

Home hoje chama `syncPending()` depois de persistir FEA1 quando sync está
habilitado. Saúde também chama `syncPending()` depois de um D1 concluído com dados
persistidos.

A 033 remove esses efeitos. Watch sync prepara/garante fila local; a escrita
remota fica explícita.

## UI/ownership auditados

Saúde ainda contém:

- `ManualHeartRateCoordinator`;
- `D1HeartRateSyncCoordinator`;
- intents de iniciar/cancelar/retry para ambos.

Exercícios ainda contém state/intent de importação D3/D4.

Perfil ainda contém:

- edição manual de step length;
- sugestão por altura;
- perfis aprendidos legacy;
- correção/histórico de calibração por passo.

Esses contratos podem permanecer internamente quando necessários para legado,
mas deixam de ser affordances de produto na 033.

## Calorias: decisão Health Connect ainda bloqueada

Escalas raw estão validadas localmente:

```text
D1 -> raw / 10.0
D4 -> raw / 1000.0
```

O tipo Health Connect correto continua não congelado. A 033 não deve escolher
`ActiveCaloriesBurned` ou `TotalCaloriesBurned` por palpite.

## Saídas atuais da auditoria

Concluídos/liberados:

- `033-02` parser/collector D1 v2;
- `033-03` persistência Room v18;
- `033-04` liberada após correção física do contrato.

Bloqueios deliberados:

- projeção HC de calorias aguarda evidência semântica;
- substituição parcial D1/D4 aguarda prova de cobertura suficiente;
- remoção física de tabelas/código legacy de step length não faz parte da 033;
- `dayOffset > 0` permanece para validação posterior e não bloqueia 033-04.
