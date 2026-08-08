# ADR-019 — Duração autoritativa do treino e timeline D4 normalizada

- **Status:** aceita e implementada na Spec 021
- **Data:** 2026-08-08

## Contexto

A ADR-018 definiu a primeira série temporal canônica de treinos D3/D4 assumindo
quantum nominal de `60 s` por registro e exigindo compatibilidade entre duração
total e quantidade de registros. Evidência física posterior mostrou que essa
suposição é rígida demais.

O app oficial do Champion C001 exibiu, para uma atividade observada:

```text
duração oficial = 29:15
suspensões = 0
registros/gráfico D4 = 32
```

O bloco local preservado estava estruturalmente íntegro: `recordCount` e quantidade
de registros coincidiam, índices eram contínuos, CRC era coerente e a soma dos
passos temporais fechava com o total. Portanto, não há base para descartar os
últimos samples nem para promover `32 × 60 s` a duração do exercício.

A mesma investigação mostrou que o layout preferido perde os segundos finais no
parser atual: o fim do pacote 1 é completado com `second = 0`, enquanto
`packet2[6]` coincidiu exatamente com os segundos finais exibidos pelo app oficial
em três atividades físicas independentes.

## Decisão

### 1. Duração da atividade é autoritativa

Para `WATCH_D3_D4`, início e fim resolvidos do cabeçalho do treino determinam a
janela oficial. `recordCount`, quantidade de samples e quantum nominal não podem
substituir essa duração.

No layout preferido:

```text
endSecond = packet2[6], quando 0..59
```

No layout legado, permanece o segundo já presente no pacote 1.

Valor inválido não é corrigido por heurística baseada em samples; a implementação
preserva ausência/falha segura.

### 2. Registros D4 continuam granulares

Cada registro temporal preserva:

- ordem/índice de origem;
- `decodedSteps` quando semanticamente confirmado;
- BPM quando válido;
- valores raw já definidos pela ADR-018.

O quantum de `60 s` passa a significar **granularidade nominal observada**, não
duração física obrigatória do bucket.

### 3. Timeline derivada normalizada

Com duração positiva `T` e `N` registros estruturais elegíveis, a timeline
derivada distribui os registros contiguamente sobre toda a sessão.

A implementação deve dividir `T` por `N` de forma inteira/determinística e
repartir o resto sem perda, garantindo:

```text
start[0] = startedAt
end[N-1] = endedAt
end[i] = start[i+1]
Σ duration[i] = T
```

Nenhum sample é descartado apenas porque `N × 60 s > T`. Nenhum tempo é criado
fora da sessão.

### 4. Fatos e derivados permanecem separados

São fatos do D4:

```text
sampleIndex
decodedSteps
BPM
rawMotionValue
rawDistanceValue
rawCaloriesValue
```

São derivados pela política versionada:

```text
effectiveStart
effectiveDuration
BPM sampledAt normalizado
distância canônica
cadência
velocidade
ritmo
acumulado
splits
```

Timestamp normalizado de BPM não deve ser descrito como timestamp medido pelo
relógio.

### 5. Distância canônica não muda

Permanece a decisão da ADR-018:

```text
canonicalIntervalDistanceMeters = decodedSteps × stepLengthMetersUsed
canonicalDistanceMeters = Σ canonicalIntervalDistanceMeters
```

A normalização altera somente o eixo temporal e os derivados que dependem dele.
Não escala passos nem força distância a outro total.

### 6. Frequência cardíaca preserva índice de origem

A persistência deve conseguir associar BPM ao sample D4 original mesmo quando
samples intermediários não contêm BPM válido. Um índice de origem nullable deve
ser persistido para amostras D4; origens com timestamp efetivamente medido podem
permanecer sem esse índice.

Legado só recebe índice recuperado quando a relação com o timestamp nominal
antigo for inequívoca. Sem recuperação segura, o reprocessamento granular falha
fechado.

### 7. Versionamento

- parser completo da nova semântica: `d4-v3`;
- derivação normalizada: `watch-d3-d4-v2`;
- `d4-v1`, `d4-v2` e snapshots anteriores continuam preservados como
  proveniência histórica.

A nova versão não é aplicada parcialmente: o runtime só passa a importar como
`d4-v3` quando duração e timeline normalizada estiverem no mesmo contrato.

### 8. Reprocessamento é explícito

A atualização do app, migration, leitura de tela ou mudança de perfil não muda
atividades antigas silenciosamente.

Quando os fatos necessários estiverem preservados, o usuário pode reprocessar a
atividade localmente sem apagar banco e sem nova leitura BLE. Recuperar
`endSecond` pode usar o payload diagnóstico local já associado.

Atividade `SYNCED` continua protegida pelos contratos de projeção/reconciliação da
ADR-018; atualização não dispara ressync automático.

### 9. Projeções Health Connect usam a timeline normalizada

`DistanceRecord`, `SpeedRecord` e a posição temporal das amostras de BPM usam a
série congelada da nova derivação. IDs, versões, retry e payload congelado
continuam seguindo ADR-018.

## Supersessão parcial da ADR-018

Esta ADR substitui, para a nova derivação de `WATCH_D3_D4`:

- a política temporal do item 5 baseada em `T/N/60 s` e residual;
- a interpretação de `60 s` como duração efetiva obrigatória de cada intervalo;
- o uso de `startedAt + sampleIndex × 60 s` como timestamp absoluto necessário
  para validar samples de BPM.

Permanecem válidos os demais contratos da ADR-018, inclusive raw/measured/
derived/projection, distância por passos, snapshots imutáveis, splits,
plausibilidade, esteira watch-first, projeções filhas e reconciliação.

## Consequências

- atividades com mais samples que minutos oficiais deixam de falhar apenas por
  incompatibilidade com `N × 60 s`;
- granularidade de passos, distância e BPM é preservada;
- cadência/velocidade/ritmo passam a usar duração normalizada;
- BPM ganha timestamp derivado explicitamente, não falsa precisão de origem;
- reprocessamento existente pode recuperar atividades sem wipe quando os fatos
  estão preservados;
- pode ser necessária migration aditiva para índice de origem de BPM;
- snapshots antigos não mudam sem ação explícita.

## Alternativas rejeitadas

- usar `recordCount × 60 s` como duração do exercício;
- truncar samples que ultrapassem o `endedAt` calculado;
- descartar samples finais até a série caber;
- distribuir apenas o residual no último sample;
- alterar passos para compensar tempo;
- inferir pausas a partir de sample com zero passos;
- usar zero passos como evidência de suspensão;
- limpar banco/reimportar como mecanismo de correção;
- ressincronizar Health Connect automaticamente após upgrade.

## Evidência e limite

A decisão está apoiada por banco local real e screenshots do app oficial no
mesmo conjunto de treinos. A correspondência de `packet2[6]` com os segundos
finais foi exata em três atividades observadas. Isso é evidência física forte do
layout preferido conhecido, não garantia universal para firmware ainda não
observado; valores fora de `0..59` permanecem fail-closed.

## Estado de implementação

- Runtime novo usa `d4-v3` com duração autoritativa e timeline normalizada no
  mesmo contrato.
- Derivação ativa usa `watch-d3-d4-v2`; `d4-v1` e `d4-v2` permanecem preservados
  como proveniência legada.
- Room schema `v11` preserva índice de origem de amostras D4 por migration
  aditiva, sem efeitos remotos.
- Validação integrada da Spec 021 foi concluída no Galaxy S22, com ressalvas de
  firmware, Health Connect write-only e cenários físicos não repetidos.
