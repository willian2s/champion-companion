# Task 003-02 — Criar Primitivos de Domínio

## Status

Pendente.

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
