# 023-05 — Implementar calibração por distância de referência

- **Status:** `completed`
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

- [x] elegibilidade por tipo/origem implementada
- [x] distância externa validada
- [x] preview observada calculada
- [x] preview do perfil futuro calculada
- [x] evidência salva sem duplicação
- [x] snapshot calibrado usa source correto
- [x] distância da atividade fecha na referência
- [x] BPM/raw preservados
- [x] sem sync automático
- [x] guards de atividade sincronizada respeitados
- [x] correção/remoção de evidência disponível
- [x] spec/overview atualizados

## Aceite

Uma corrida de referência deve produzir um snapshot cujo total canônico seja a
distância informada e, ao mesmo tempo, aparecer como uma única evidência no
perfil aprendido de corrida.

## Resultado da execução

- **Status:** `completed`
- **Arquivos alterados:** contratos de métricas, repository Room, derivação e
  mapeadores de snapshot, detalhe/ViewModel de Exercícios e Perfil.
- **Decisões e desvios:** calibração externa usa operação transacional própria,
  preserva calibração de esteira, bloqueia estados/projeções remotas inseguros e
  não dispara sincronização. Histórico de evidências permite remoção; correção
  usa substituição idempotente pela mesma atividade. Perfil futuro é calculado
  pela soma ponderada de distância e passos persistidos.
- **Comandos executados:** `./gradlew :app:compileDebugKotlin --no-daemon`,
  `./gradlew :app:lintDebug --no-daemon`, `git diff --check`.
- **Resultados e evidências:** compile Kotlin, lint debug e verificação de diff
  passaram. Snapshot usa `watch-d3-d4-v3` e
  `REFERENCE_DISTANCE_CALIBRATION`; raw, timestamps, BPM e calorias permanecem
  fora da mutation.
- **Riscos residuais:** validação física em banco preservado, fechamento
  numérico e ausência de sync automático permanecem para `023-07`. Não foram
  criados nem executados testes automatizados.
- **Revisão posterior:** API de persistência isolada foi removida; perfil
  contextual aprendido recebeu provenance própria; preview passou a usar série
  canônica, duração fracionária e proteção contra overflow; histórico ganhou
  correção explícita e guards de remoção foram reforçados.
- **Validação final:** `./gradlew lint assembleDebug assembleRelease --no-daemon`
  passou; `git diff --check` passou.
- **Revisão final histórica:** correção de evidência futura não rederiva atividade; filhos
  Health Connect pendentes/failed bloqueiam mutação para evitar payload obsoleto;
  perfil contextual usa `watch-d3-d4-v3`. Nova validação após essas correções
  foi concluída no escopo local; validação física permanece em `023-07`.
- **Correções finais:** correção/remoção agora usam guard fail-closed comum;
  CTA fica oculto quando projeção HC impede mutação; preview valida finitude e
  positividade; UI explicita fonte externa sem GPS e passos/comprimento/perfil.
- **Validação final após correções:** `./gradlew lint assembleDebug
  assembleRelease --no-daemon` e `git diff --check` passaram.
- **Escopo residual:** validação física de banco preservado, retry e provider
  Health Connect pertence à `023-07`; não bloqueia aceite local desta subtask.
- **Correção pós-uso:** atividades `PENDING_REVIEW` agora exibem o campo de
  distância de referência junto da seleção corrida/caminhada. O valor fica no
  formulário até a revisão ser salva; depois o bloco completo de preview e
  aceite permanece disponível quando a atividade estiver elegível.
- **Revisão da correção:** trocar tipo para fora de corrida/caminhada limpa a
  distância contextual pendente, evitando reaproveitamento em esteira ou
  bicicleta.
- **Correção de métricas:** calibração de referência rederiva distância,
  acumulado, velocidade, pace, speed series e splits. Calculador agora também
  materializa último split parcial, inclusive atividades abaixo de 1 km; BPM,
  raw D4, timestamps e calorias permanecem preservados.
- **Correção de preservação:** amostras de BPM agora são usadas com seus fatos
  persistidos durante splits e snapshot; normalização temporal legada não é mais
  gravada durante calibração.
- **Exclusão local:** todas as atividades não sincronizadas que não possuam
  sessão/rota phone ou efeito remoto Health Connect iniciado agora podem ser
  excluídas permanentemente pelo detalhe. `SYNCING`, `SYNCED`, operações com
  efeito remoto e agregados com sessão permanecem bloqueados. Derived intervals
  e splits são removidos explicitamente antes dos snapshots para compatibilidade
  com bancos preservados.
- **Guard de origem:** exclusão aceita somente origens conhecidas locais
  (`WATCH_D3_D4`, `PHONE_TREADMILL`, `PHONE_FREE_WORKOUT`); import D3/D4 é
  opcional, mas quando presente precisa ter origem compatível.
- **Preview alinhada:** pace e velocidade usam duração coberta pela série
  canônica quando disponível, igual à derivação aplicada.
