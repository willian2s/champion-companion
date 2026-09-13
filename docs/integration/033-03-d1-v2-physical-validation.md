# 033-03 — Validação física D1 v2 antes da composição diária

- **Data:** 2026-09-13
- **Branch:** `feat/033-unified-watch-daily-sync`
- **Escopo:** checkpoint físico pós-033-02/033-03 antes da implementação de 033-04
- **Privacidade:** evidência sanitizada; sem payload BLE integral, identificadores persistentes ou BPM individual

## Objetivo

Validar em banco real do Champion C001 o parser/persistência D1 v2, a
idempotência de reimportação e, principalmente, observar como D1 e D4 coexistem
antes de congelar a política de composição diária.

## Resultado D1 v2

Primeiro dump sanitizado do dia:

```text
D1 samples:       130
posições:         1..130, sem lacunas
Σ D1.steps:       2713
FEA1.steps:       2713
```

Segundo dump após reimportação e novo FEA1:

```text
D1 samples:       132
posições:         1..132, sem lacunas
Σ D1.steps:       2752
FEA1.steps:       2752
```

A reimportação preservou identidade dos 130 samples existentes e inseriu somente
os dois novos buckets. O crescimento de `39` passos no D1 fechou exatamente com
o crescimento de `39` passos do FEA1.

Também foi confirmado fisicamente:

- `distanceMeters == distanceRaw` nos samples observados;
- `caloriesKilocalories == caloriesRaw / 10.0` nos samples observados;
- BPM inválido fica nullable sem eliminar passos, distância, calorias ou raw do
  mesmo pacote;
- os dois workouts D4 existentes permaneceram inalterados durante a reimportação
  D1.

## Evidência D1 × D4

O banco continha dois workouts `d4-v4` válidos no mesmo dia.

### Workout com métricas locomotoras nulas

Um workout observado possuía no D4:

```text
steps:      0
distance:   0 m
calories:   0 kcal
```

Apesar disso, um bucket D1 que intersectava temporalmente a janela do workout
continha:

```text
steps:      45
distance:   36 m
calories:   2,4 kcal
```

Logo, "bucket D1 intersecta workout" não prova que todo o conteúdo daquele
bucket pertence ao workout. Suprimir o bucket inteiro perderia fatos diários
reais.

### Workout de corrida

Outro workout `d4-v4` observado fechou:

```text
D4 steps:       1935
D4 distance:    1349 m
D4 calories:    88,224 kcal
```

Os buckets D1 correspondentes à janela observada somaram:

```text
D1 steps:       1935
D1 distance:    1349 m
D1 calories:    88,2 kcal
```

Isso demonstra, neste cenário físico, que a linha D1 diária já carregava os fatos
nativos do exercício. A diferença de calorias é compatível com a resolução de
`0,1 kcal` do D1 versus `0,001 kcal` do D4.

A evidência não autoriza assumir que toda janela futura fechará exatamente dessa
forma, mas invalida a política anterior de tratar D1 como fonte exclusivamente
fora de workouts.

## Correção de contrato para 033-04

A composição local passa a distinguir finalidade e métrica.

### Passos diários

```text
FEA1 = total diário autoritativo
D1   = distribuição granular do total diário
D4   = passos/cadência do workout, sem soma adicional ao total diário
```

`Σ D1.steps` deve ser comparável ao FEA1 para cobertura/diagnóstico. D4 não é
adicionado novamente ao agregado diário.

### Distância diária

D1 é a fonte nativa diária de distância quando a cobertura for elegível. D4
permanece a fonte canônica da distância do workout, inclusive calibração
contextual, mas sua distância não é somada cegamente à distância D1.

Uma substituição/ajuste da contribuição D1 por distância D4 somente pode ocorrer
quando existir prova determinística de cobertura e de não duplicação. Sem essa
prova, o read model diário preserva D1 e o detalhe do workout preserva D4.

### Calorias diárias

A mesma regra estrutural de distância se aplica localmente: D1 fornece fatos
diários; D4 fornece fatos do workout. Não somar ambos. A projeção Health Connect
de calorias continua bloqueada por semântica.

### Frequência cardíaca

D4 pode substituir D1 como fonte visual durante a janela de workout porque possui
amostras próprias do exercício. Omitir um ponto D1 dentro da janela somente
quando houver cobertura D4 válida para frequência cardíaca; ausência de D4 válido
não autoriza eliminar o D1.

## Regra de overlap revisada

A regra removida é:

```text
bucket D1 intersecta D4
=> suprimir bucket inteiro para todas as métricas
```

A regra nova é:

```text
não existe supressão global por interseção

cada métrica decide sua autoridade/cobertura
substituição D1 -> D4 exige prova local suficiente
sem prova => preservar D1 no diário e D4 no workout, sem soma
```

A 033-06 deve aplicar a mesma cautela ao Health Connect: não projetar D1 e D4
como records sobrepostos apenas porque ambos estão disponíveis. A estratégia de
projeção deve preservar o total FEA1 de passos e falhar fechada quando não puder
provar uma representação não sobreposta.

## Gates fechados antes de 033-04

- Room v18 e D1 v2 físico: concluído;
- identidade/reimportação D1 idempotente: concluído;
- crescimento incremental D1 coerente com FEA1: concluído em duas leituras;
- coexistência D1/D4 observada: concluída;
- política de supressão global por bucket: rejeitada pela evidência;
- `dayOffset > 0`: permanece para validação posterior e não bloqueia 033-04.
