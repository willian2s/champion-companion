# Task 003-02 — Criar Primitivos de Domínio

## Status

Concluída em 2026-07-20.

## Dependência

- `003-01` concluída.

## Objetivo

Criar linguagem comum de domínio pura: IDs UUIDv7, origem, validade, tempo, proveniência, ausência e erros seguros.

## Pode modificar

- `core/model/**`, `core/common/**`, testes JVM correspondentes.
- Este arquivo.

## Passos

1. Criar value objects/tipos distintos para IDs de cada agregado; gerar UUIDv7 testável e validar formato/ordenação temporal esperada.
2. Modelar origem, validade, versão de parser, proveniência de métrica e campos ausentes sem sentinelas inventadas.
3. Definir tipos para `Instant`, `LocalDate`, `LocalDateTime`, `ZoneId`/offset e resolução de horário do relógio sem timezone; não converter horário observado em instante absoluto sem evidência.
4. Definir enums comuns mínimos, sem classes Android/Room/Compose.
5. Criar regras reutilizáveis de validação que retornem erro de domínio seguro, sem stack trace/mensagem interna.
6. Cobrir UUIDv7, timezone/DST, ausência e validações com testes JVM determinísticos.

## Aceite

- Domínio compila sem imports proibidos.
- Tempo canônico, valores ausentes e proveniência ficam explícitos.
- Nenhum identificador depende de autoincremento.

## Implementação

- `core.model` contém IDs distintos para todos os agregados, gerador UUIDv7 monotônico e parser que aceita somente UUIDv7 RFC 9562.
- Tempo canônico/local e horário observado do relógio são tipos separados. Horário sem timezone inicia sem resolução; gaps e overlaps DST permanecem explícitos.
- Origem, validade, proveniência, ausência e versão de parser são tipos de domínio puros. Validações retornam somente códigos seguros, sem mensagens internas ou stack traces.
- Testes JVM determinísticos cobrem UUIDv7, ausência, validação, timezone e DST.
