# Task 003-13 — Adicionar Testes Unitários de Domínio

## Status

Concluída em 2026-07-21.

## Dependência

- `003-12` concluída.

## Objetivo

Consolidar cobertura JVM determinística para todas as regras não visuais da Fase 03.

## Pode modificar

- `app/src/test/**` e este arquivo.

## Passos

1. Cobrir IDs UUIDv7, origem/proveniência, valores ausentes, mapeadores e erro seguro.
2. Cobrir data local, timezone, DST e relógio sem timezone explícito.
3. Cobrir chaves de deduplicação e comportamento de conflito/atualização para cada agregado.
4. Cobrir workflow D3/D4, sugestão de tipo, revisão, descarte, reimportação e parser parcial/inválido.
5. Cobrir calibração, sessão interrompida/recuperada, sync BPM parcial e retenção/reprocessamento diagnóstico.
6. Usar nomes descritivos com crases e fixtures determinísticas; não inserir payloads pessoais/reais.
7. Rodar `./gradlew testDebugUnitTest`.

## Aceite

- Cada regra, mapeamento de erro e comportamento não visual possui teste unitário.
- Testes não usam modelos Room como domínio nem dados sensíveis.

## Validação

- `./gradlew testDebugUnitTest` passou.
