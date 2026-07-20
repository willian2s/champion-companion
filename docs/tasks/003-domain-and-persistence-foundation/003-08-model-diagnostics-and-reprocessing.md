# Task 003-08 — Modelar Diagnóstico e Reprocessamento

## Status

Pendente.

## Dependência

- `003-02` concluída.

## Objetivo

Modelar DiagnosticPayload, ParserValidity e vínculo seguro entre captura e derivados.

## Pode modificar

- Modelos/regras diagnósticas e entrada de parser em `core/model/**`.
- Testes JVM correspondentes.
- Este arquivo.

## Passos

1. Modelar captura UUID, comando/fluxo, bytes privados, device/firmware, versão do parser, resultado, erro seguro, data e referências de derivados.
2. Criar `ParserValidity`: `VALID`, `PARTIAL`, `INVALID`, separado de estado de produto; distinguir também erro técnico.
3. Definir vínculo único de derivação `payload + tipo de saída + ordinal`; reprocessamento usa transação e não duplica saídas.
4. Definir retenção: 14 dias ou 10 MiB globais; limpeza por mais antigo primeiro; exclusão não remove derivados de domínio.
5. Testar validade, idempotência conceitual, ordenação de limpeza e preservação de derivados.

## Aceite

- Payload não vira log nem dado de UI/rede.
- Diagnóstico suporta concluído, parcial, inválido e erro técnico.
