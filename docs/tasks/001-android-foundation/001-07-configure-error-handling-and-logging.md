# Task 001-07 — Configurar Erros e Logs

## Status

Concluída em 2026-07-20.

## Dependências

- `001-03` e `001-04` concluídas.

## Objetivo

Criar contrato comum de erro seguro e log JSON estruturado somente para debug.

## Pode modificar

- `core/error/**`, `core/logging/**`
- Módulo Hilt próprio.
- Testes unitários desses contratos.
- Este arquivo.

## Regras de dados

- Nunca registrar payload BLE, coordenadas, tokens, IDs pessoais ou dados saúde.
- Mensagem UI não expõe stack trace ou detalhe interno.
- Detalhes técnicos ficam Logcat debug.

## Passos

1. Definir erros de infraestrutura mínimos.
2. Mapear cada erro para mensagem UI segura.
3. Implementar JSON com campos permitidos explícitos.
4. Criar testes para mapeamento e redaction.

## Aceite

- Logs release inativos ou sem detalhes diagnósticos.
- Testes provam que campos sensíveis não são emitidos.
