# Task 001-09 — Validar Fundação Android

## Status

Pendente.

## Dependências

- Todas tasks `001-01` até `001-08`, `001-10`, `001-11` concluídas.

## Objetivo

Validar integração Fase 1; consolidar checklist `001-00` apenas após sucesso completo.

## Pode modificar

- Testes básicos.
- Este arquivo.
- `001-00-overview.md`, somente consolidar sucesso.

## Passos

1. Rodar `./gradlew lint`, testes unitários, `assembleDebug`, `assembleRelease`.
2. Com Galaxy S22 conectado, rodar instrumentados; instalar debug `.dev` paralelo release.
3. Abrir sete telas; navegar, retornar e confirmar inicialização.
4. Produzir erro controlado; confirmar JSON Logcat debug e mensagem UI segura.
5. Inspecionar manifest, dependências e fontes: sem permissões BLE/localização/Health Connect, Room, serviços, parsers ou coordenadas.
6. Rodar `git diff` para confirmar roadmap, ADR-001 e Fase 0 intactos; rodar `git status --short`.
7. Atualizar statuses e checklist somente se tudo passar. Ausência Galaxy S22 bloqueia apenas validação física; registrar evidência.

## Aceite

- Checks Gradle passam.
- Navegação, variantes, logs e limites escopo confirmados.
- Sem commit ou push.
