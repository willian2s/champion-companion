# Task 003-14 — Adicionar Testes Room e Guarda de Privacidade

## Status

Concluída em 2026-07-21.

## Dependência

- `003-12` concluída.

## Objetivo

Validar comportamento físico Room e impedir regressão de persistência de rota/dados proibidos.

## Pode modificar

- `app/src/androidTest/**`, testes locais de guarda e este arquivo.

## Passos

1. Criar testes Room in-memory/instrumentados para schema V1, FKs, índices únicos, DAOs, consultas, transações, rollback, reprocessamento e retenção.
2. Validar que filtros retornam daily, BPM pendente válido, revisão pendente, sessão recuperável, payload expirado e sync falho esperados.
3. Criar teste automatizado que busca nos escopos domínio, Room, mapeadores, repositories, logs, fixtures e testes os termos proibidos: latitude, longitude, altitude por ponto, rota, polyline, segmentos de rota, GPX, TCX, FIT e `ExerciseRoute`.
4. Permitir exceções somente no próprio teste/documentação de política, com escopo explícito; não mascarar violações.
5. Conferir manifest/data extraction rules: Auto Backup desativado.
6. Rodar testes afetados, `./gradlew testDebugUnitTest` e `./gradlew connectedDebugAndroidTest` quando houver dispositivo.

## Aceite

- Integridade física e retenção têm evidência automatizada.
- Busca de privacidade falha diante de introdução de rota/coordenada proibida.

## Validação

- `./gradlew testDebugUnitTest` passou.
- `./gradlew compileDebugAndroidTestKotlin` passou.
- `connectedDebugAndroidTest` não executado: nenhum dispositivo ADB conectado.
