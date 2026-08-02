# ADR-016 — Integridade temporal na reimportação de treino

- **Status:** aceita
- **Data:** 2026-08-01

## Contexto

Reimportação de atividade D3/D4 atualizava `recordedAt` para o instante da nova
importação, mas preservava `reviewedAt` e decisões do usuário. Quando a nova
importação ocorria depois da revisão, o registro persistido ficava com
`reviewedAt < recordedAt`. O domínio rejeita essa ordem temporal; o índice
semântico da fila então não resolvia a atividade e o Calendário exibia dados
parciais, apesar das operações locais estarem sincronizadas.

## Decisão

Reimportação preserva `recordedAt` original quando preserva revisão e workflow.
O instante da nova importação permanece em `WatchWorkoutImport.importedAt`.
Registros legados que já possuem `reviewedAt` válido anterior a `recordedAt` são
corrigidos pela migration `7 → 8`, limitando `recordedAt` a `reviewedAt` apenas
quando `reviewedAt >= startedAt`. Linhas que não satisfazem essa condição não
são mascaradas pela migration e continuam sujeitas à degradação fail-closed.

## Consequências

- `WorkoutActivity.restore` continua protegendo invariantes temporais.
- Operações de Health Connect não são reparadas, apagadas ou reenfileiradas.
- O índice semântico volta a resolver atividades reimportadas válidas.
- Migration altera somente registros legados comprovadamente incompatíveis.
- Schema passa para versão 8; exportação Room deve ser atualizada.

## Alternativas rejeitadas

- aceitar `reviewedAt < recordedAt` no mapper;
- esconder issue no Calendário;
- ampliar janela temporal da atividade;
- apagar amostras ou operações;
- executar reparo durante consulta read-only.
