# Task 002-09 — Validar Design System

## Status

Pendente.

## Dependências

- `002-01` a `002-08` concluídas.

## Objetivo

Consolidar evidências, validar requisitos e encerrar checklist de `002-00`.

## Pode modificar

- Este arquivo.
- `002-00-overview.md` exclusivamente para checklist/status final.

## Passos

1. Rodar testes unitários e instrumentados relevantes, incluindo seleção/fallback, contraste, estados, semântica, fonte ampliada e alvo de toque.
2. Rodar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease`.
3. Em Galaxy S22 conectado, validar tema do sistema, Material You ligado/desligado quando disponível, splash, launcher, navegação, contraste e TalkBack básico.
4. Inspecionar fontes: nenhum Material 2, `Color.White`, `Color.Black`, hex em features, nem artefato/tokens/componentes MotionLog.
5. Conferir contratos públicos, limites de escopo, previews e recursos Android contra matriz de rastreabilidade da spec.
6. Registrar comandos, resultados, limitações reais e evidência manual; atualizar somente status próprio e checklist de `002-00`.

## Aceite

- Todos requisitos da spec têm evidência ou bloqueio explícito e reproduzível.
- Build completo passa; validação em dispositivo registrada.
- Nenhuma alteração em documentos autoritativos, MotionLog, commit ou push.
