# 027-09 — Implementar cadência derivada em 1 Hz

## Objetivo

Gerar série temporal de cadência na mesma grade de 1 segundo usada pelo BPM.

## Entrada

Usar intervalos D4 válidos e duração normalizada da atividade.

Cadência base por intervalo:

```text
cadenceSpm = intervalSteps / intervalDurationSeconds * 60
```

## Pipeline

```text
intervalos D4
  -> cadência base por intervalo
  -> validação/outlier conforme 027-07
  -> suavização temporal conforme contrato
  -> interpolação entre regiões contínuas elegíveis
  -> RESAMPLED_1HZ
```

## Regras

- não inventar cadência onde bucket de passos estiver ausente;
- não atravessar gaps estruturais;
- não usar total de passos da atividade para preencher intervalos faltantes;
- não derivar cadência para tipos sem semântica de passos;
- usar a mesma origem temporal/segundos relativos da série BPM;
- manter versão do algoritmo identificável.

## Grade comum

Ao final:

```text
segundo | BPM? | cadenceSpm?
0       | 138  | 164
1       | 138  | 165
2       | 139  | 165
...
```

Cada coluna pode estar ausente independentemente quando a fonte não suportar aquele segundo/região.

## Health Connect

Não alterar automaticamente a projeção `StepsCadenceRecord` nesta task. A decisão de promover bucket -> 1 Hz pertence a `027-10` após comparação com a evidência física do Gate A.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.
