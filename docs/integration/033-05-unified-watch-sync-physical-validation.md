# 033-05 — Validação física do watch sync unificado e cauda recente FEA1 × D1

- **Data:** 2026-09-13
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Escopo:** checkpoint físico pós-033-05 antes da 033-06
- **Privacidade:** evidência sanitizada; sem payload BLE integral, identificadores persistentes ou BPM individual

## Objetivo

Validar em Champion C001 real que o pull-to-refresh da Home executa FEA1, D1 e
D3/D4 em uma única tentativa composta, preserva idempotência entre refreshes e
não cria efeito remoto automático no Health Connect.

A validação também observou uma diferença temporal legítima entre o agregado FEA1
e a série D1, relevante para a classificação de cobertura usada pela 033-06.

## Primeiro refresh unificado observado

Um refresh da Home executou, em sequência, os três componentes:

```text
FEA1   -> sucesso
D1     -> sucesso
D3/D4  -> sucesso
```

O estado diário após a tentativa fechou:

```text
FEA1.steps:       4148
Σ D1.steps:       4148
D1 samples:       182
posições:         1..182, sem lacunas
```

Em relação ao checkpoint anterior, os novos buckets D1 somaram exatamente o
crescimento do FEA1.

A mesma tentativa descobriu e persistiu um novo workout D4 válido, elevando o
banco para 39 imports/atividades sem alterar os workouts anteriores.

O novo workout também reforçou a decisão da 033-04: havia métrica locomotora D1
não nula dentro de uma janela D4 com passos/distância/calorias nulos. Portanto,
overlap temporal continua não sendo prova suficiente para suprimir D1.

## Refreshes consecutivos e idempotência

Foram executados dois pulls adicionais consecutivos na Home.

No primeiro deles surgiu somente um novo bucket D1 real. No segundo, a metadata
terminal registrou uma tentativa sem mudança material:

```text
WATCH_DAILY_D1
  inserted:    0
  updated:     0
  duplicates:  183
  result:      COMPLETED

WATCH_D1_HISTORY
  inserted:    0
  duplicates:  48
  result:      COMPLETED

WATCH_WORKOUT_D3_D4
  inserted:    0
  result:      COMPLETED
```

O banco permaneceu com:

```text
39 workout imports
39 workout activities
5 headers D4 v4
```

As tabelas de workout/intervalos/séries permaneceram inalteradas no refresh
idempotente.

Isso confirma fisicamente:

- D1 não duplica samples ao repetir a Home;
- D3/D4 não recria workouts já válidos;
- um único entrypoint da Home atualiza as três famílias de dados;
- refresh repetido sem mudança material converge para no-op persistente.

## Health Connect permaneceu sem efeito automático

Entre os refreshes unificados, a fila Health Connect permaneceu com a mesma
quantidade e conteúdo de operações, sem tentativa remota adicional.

Isso confirma o contrato:

```text
watch sync da Home
  -> lê
  -> persiste localmente
  -> prepara/garante estado local elegível
  -> NÃO chama syncPending() automaticamente
```

## Evidência de cauda recente FEA1 × D1

No dump final dos refreshes consecutivos:

```text
FEA1.steps:       4323
Σ D1.steps:       4226
D1 samples:       183
posições:         1..183, sem lacunas
último bucket D1: 15:10
FEA1 coletado:    ~15:12:47
```

Diferença observada:

```text
4323 - 4226 = 97 passos
```

O D1 estava estruturalmente completo para o `totalPositions = 183`; não havia
posição faltante, duplicata ou falha de persistência. A diferença ocorreu porque
FEA1 é uma leitura atual do agregado diário enquanto D1 materializa fatos em
buckets discretos de aproximadamente cinco minutos.

Portanto, `FEA1 > Σ D1.steps` não é automaticamente divergência. Quando:

- a estrutura D1 recebida está completa até `totalPositions`;
- o último bucket D1 é recente em relação ao `collectedAt` do FEA1;
- FEA1 está à frente do D1, e não abaixo;

é plausível e esperado existir uma **cauda recente ainda não materializada no
D1**.

## Correção de contrato para 033-06

A classificação anterior da 033-04 tratava qualquer diferença FEA1 × D1 como
`DIVERGENT`. A 033-06 deve refiná-la.

Contrato alvo:

```text
FEA1 == ΣD1
  -> COMPLETE

FEA1 > ΣD1
+ D1 estruturalmente completo até o último bucket recebido
+ frescor compatível com uma cauda ainda aberta
  -> RECENT_TAIL / LAGGING
  -> não é divergência estrutural
  -> não fabricar bucket residual
  -> não atribuir timestamp aos passos residuais

FEA1 < ΣD1
ou estrutura D1 incompleta
ou diferença não explicável por frescor/cobertura
  -> DIVERGENT/PARTIAL conforme a causa
  -> falhar fechado para projeções que exijam reconciliação comprovada
```

O nome final do enum pode variar, mas a distinção semântica é obrigatória.

A cauda FEA1 não deve ser transformada em record temporal sintético. A próxima
leitura D1 real deve materializar esses passos em bucket(s) próprios do relógio.

## Impacto na 033-06 / Health Connect

Para datas pós-cutover:

- FEA1 continua sendo referência do total diário atual;
- D1 granular publica somente fatos temporalmente materializados e elegíveis;
- uma cauda recente FEA1 não autoriza criar `StepsRecord` artificial;
- a existência da cauda não deve, por si só, tornar o dia estruturalmente
  divergente;
- record que dependa de cobertura completa deve permanecer pendente/bloqueado
  quando a cauda ainda não foi materializada, sem inventar residual;
- retries devem preservar identidade e payload congelado dos buckets já
  materializados.

## Gates fechados antes da 033-06

- Home como entrypoint único FEA1 + D1 + D3/D4: confirmado fisicamente;
- D1 idempotente em refresh consecutivo: confirmado;
- D3/D4 idempotente/skip local: confirmado;
- ausência de auto-sync Health Connect pela Home: confirmada;
- FEA1 pode avançar à frente do último D1 sem erro estrutural: confirmado;
- residual FEA1 sem bucket temporal não deve ser fabricado: decisão congelada;
- partial success físico forçado por falha de componente continua para 033-09.
