# 033-01 — Auditoria D1 v2, watch sync e cutover Health Connect

- **Data:** 2026-09-11
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Base:** `main` após merge da Spec 032
- **Natureza:** auditoria documental; nenhum runtime alterado nesta task

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

## D1 runtime atual e divergências

`ChampionD1HistoryProtocol` atual (`d1-v1`) lê somente:

```text
1..2 packetIndex LE
3..4 totalPackets LE
5    hour direto
14   BPM
19   minute
```

E aplica:

```text
hour 0..23
minute 0..59
BPM 30..220
```

Se BPM for inválido, `parsePayload()` retorna `null`; assim passos/distância/etc.
não podem ser preservados e um pacote final inválido pode impedir a conclusão
normal do collector.

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

`sportValue` fecha como campo numérico estrutural, mas não recebe unidade
fisiológica inventada.

## Evidência histórica D1 x D4

Banco físico auditado anteriormente possuía 749 registros D1 de BPM persistidos
pela implementação antiga. Como só BPM era salvo, essa evidência não prova que o
pacote D1 inteiro deixava de existir durante Sport Model; ela prova apenas o
comportamento do componente BPM.

Na janela de um workout D4 observado:

```text
D1 HR dentro da janela: 1 / 13 buckets esperados (~7,7%)
D1 HR fora da janela:  266 / 274 (~97,1%)
```

Houve apenas quatro overlaps de BPM D1 no conjunto examinado, próximos a inícios
de workout. Isso sustenta uma política conservadora de composição: D4 tem
autoridade durante sua cobertura e buckets D1 intersectantes são suprimidos por
inteiro inicialmente.

A 033 não afirma que o firmware nunca envia D1 durante D4; essa pergunta só pode
ser respondida depois que o app persistir o pacote D1 completo.

## Autoridade congelada

```text
FEA1
  -> total diário atual de passos / validação

D1
  -> fatos granulares diários fora de workouts

D4
  -> fatos granulares durante workouts
```

Header D4 permanece consistência/diagnóstico. Nenhum total de header substitui a
série temporal.

## Identidade D1 alvo

A persistência deve preferir identidade por posição física, não por BPM:

```text
(deviceId, resolvedLocalDate, currentPosition)
```

O schema final deve garantir que reimportação do mesmo dia não duplique samples.
`resolvedAt` pode participar de índices/consulta, mas não deve ser a única chave
se posição física já estiver disponível.

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

## Cutover Health Connect

Problema a evitar:

```text
DailySummary/FEA1 total do dia
+
D1/D4 granular do mesmo dia
=
dupla contagem
```

Contrato congelado:

- cutover é uma data local inteira persistida;
- datas anteriores preservam a política histórica;
- datas no/depois do cutover não criam nova projeção STEPS de DailySummary;
- não executar backfill granular histórico;
- não apagar records antigos;
- não reabrir `SYNCED` silenciosamente;
- instalação com legado deve preferir próximo dia local para ativar granular;
- instalação sem legado pode começar no dia atual se preflight provar ausência de
  projeção diária legacy preparada.

A Spec 027 continua necessária para histórico pré-cutover. A linha pós-cutover
deverá evitar compensação porque D1 e D4 serão selecionados sem sobreposição.

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

## Calorias: decisão ainda bloqueada

Escalas raw são conhecidas, mas o tipo Health Connect correto ainda não está
congelado:

```text
D1 -> candidato a energia diária/total
D4 -> candidato a energia de workout/ativa
```

A 033 não deve escolher `ActiveCaloriesBurned` ou `TotalCaloriesBurned` por
palpite. Persistência local pode avançar antes dessa decisão.

## Saídas da auditoria

Liberado:

- `033-02` parser/collector D1 v2;
- `033-03` persistência Room v18.

Bloqueios deliberados:

- projeção HC de calorias aguarda evidência semântica;
- otimizações de bucket parcial D1/D4 ficam fora até evidência física;
- remoção física de tabelas/código legacy de step length não faz parte da 033.
