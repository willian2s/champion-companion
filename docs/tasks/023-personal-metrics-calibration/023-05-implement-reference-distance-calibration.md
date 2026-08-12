# 023-05 — Implementar calibração por distância de referência

- **Status:** `pending`
- **Depends on:** `023-04`

## Objetivo

Permitir que uma atividade D3/D4 confirmada como corrida ou caminhada use uma
distância externa de referência para calibrar a própria atividade e gerar uma
evidência para o perfil aprendido do contexto.

## Elegibilidade

Somente:

- `WATCH_D3_D4`;
- `confirmedType == RUNNING` ou `WALKING`;
- série D4 elegível;
- soma positiva de `decodedSteps`;
- atividade não descartada;
- workflow permitido pela auditoria `023-01`.

Não oferecer a ação para `CYCLING`, `FREE_WORKOUT`, `TREADMILL` ou tipo
`UNKNOWN` nesta fase.

## UI no detalhe

Adicionar seção explícita:

```text
Calibrar com distância de referência

Distância atual estimada
4,08 km

Distância de referência
[ 4,23 ] km

Passos usados
4.987

Comprimento observado do passo
0,848 m

Perfil atual de corrida
0,844 m

Perfil após calibração
0,846 m
```

A preview deve ser calculada antes do aceite.

O texto precisa deixar claro que a distância foi informada pelo usuário a partir
de uma fonte externa e não coletada por GPS do Champion Companion.

## Aplicação

No aceite explícito:

1. validar distância e estado novamente;
2. persistir/substituir a evidência da atividade;
3. calcular `observedStepLengthMeters`;
4. rederivar a atividade com esse valor quando o workflow permitir;
5. congelar `StepLengthSource.REFERENCE_DISTANCE_CALIBRATION` no snapshot;
6. manter fatos D4 e BPM intactos;
7. atualizar a leitura do perfil aprendido;
8. não disparar sincronização automaticamente.

A operação deve evitar estado parcial local. A auditoria deve definir se a
persistência da evidência + novo snapshot precisa ocorrer na mesma transação ou
com compensação explícita.

## Resultado da própria atividade

A soma canônica dos intervalos deve fechar na distância de referência usando os
mesmos `decodedSteps` elegíveis usados como denominador da calibração.

Recalcular:

- distância por intervalo;
- acumulado;
- velocidade;
- pace;
- splits;
- speed series derivada.

Não alterar:

- timestamps D4;
- BPM;
- calorias de origem;
- parser;
- raw intervals.

## Atividade sincronizada

Não editar/rederivar silenciosamente `SYNCED`, `SYNCING` ou cenário remoto
parcial bloqueado pelos guards.

Se `023-01` autorizar usar uma atividade sincronizada somente como evidência de
perfil, a UI precisa diferenciar claramente:

```text
Atualiza perfil futuro
≠
Corrige esta atividade
```

Sem decisão explícita da auditoria, fail-closed.

## Correção/remoção

A superfície de perfil/histórico deve permitir corrigir ou remover a evidência.
Essa ação recalcula o perfil futuro, mas não desfaz automaticamente uma
rederivação histórica já aplicada à atividade.

## Checklist

- [ ] elegibilidade por tipo/origem implementada
- [ ] distância externa validada
- [ ] preview observada calculada
- [ ] preview do perfil futuro calculada
- [ ] evidência salva sem duplicação
- [ ] snapshot calibrado usa source correto
- [ ] distância da atividade fecha na referência
- [ ] BPM/raw preservados
- [ ] sem sync automático
- [ ] guards de atividade sincronizada respeitados
- [ ] correção/remoção de evidência disponível
- [ ] spec/overview atualizados

## Aceite

Uma corrida de referência deve produzir um snapshot cujo total canônico seja a
distância informada e, ao mesmo tempo, aparecer como uma única evidência no
perfil aprendido de corrida.