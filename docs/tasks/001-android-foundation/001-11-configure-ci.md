# Task 001-11 — Configurar CI

## Status

Pendente.

## Dependências

- `001-02`, `001-06`, `001-07`, `001-08`, `001-10` concluídas.

## Objetivo

Criar workflow GitHub Actions para checks Android sem dispositivo físico.

## Pode modificar

- `.github/workflows/**`
- Este arquivo.

## Passos

1. Acionar em push e pull request.
2. Usar checkout, setup JDK 21 e wrapper Gradle.
3. Rodar `lint`, testes unitários, `assembleDebug`, `assembleRelease`.
4. Usar permissões mínimas e cache Gradle seguro.

## Aceite

- Workflow YAML válido.
- Sem secrets, emulador ou dispositivo físico.
