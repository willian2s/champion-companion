# Task 002-03 — Criar Tokens de Estado Semântico

## Status

Concluída em 2026-07-20.

## Dependência

- `002-02` concluída.

## Objetivo

Definir contratos visuais independentes de integrações futuras e tokens semânticos claro/escuro.

## Pode modificar

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/designsystem/`
- Este arquivo.

## Passos

1. Criar `ConnectionVisualState`, `SyncVisualState` e `ActivityVisualState`; não referenciar BLE, Health Connect ou domínio.
2. Criar `LocalChampionStatusColors` com pares container/conteúdo para `connected`, `disconnected`, `syncPending`, `syncSuccess`, `syncFailure`, `heartRate`, `activityRunning` e `activityPaused`.
3. Definir rótulo, ícone e descrição semântica para cada estado; cor nunca é sinal único.
4. Mapear estados para tokens e testar pares de cor, textos e semântica segura.
5. Criar previews dos estados claro/escuro.

## Aceite

- Estados são UI-only e extensíveis sem acoplamento técnico.
- Todos estados exigidos têm token, conteúdo, rótulo e ícone acessíveis.
