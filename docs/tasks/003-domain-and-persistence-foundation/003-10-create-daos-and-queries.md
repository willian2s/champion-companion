# Task 003-10 — Criar DAOs e Consultas

## Status

Pendente.

## Dependência

- `003-09` concluída.

## Objetivo

Expor DAOs específicos, consultas indexadas e operações condicionais que preservem integridade.

## Pode modificar

- DAOs, projections e testes Room em `core/database/**`.
- Este arquivo.

## Passos

1. Criar DAOs por agregado; evitar DAO genérico e queries que exponham entidades fora da camada data.
2. Expor: device associado; resumo diário; BPM válido pendente; revisão pendente; sessão recuperável; payload expirado; sync falho.
3. Criar operações condicionais para transições de atividade, sessão e sync; resultado deve informar conflito/estado inesperado sem sobrescrever edição.
4. Criar queries para importação/reimportação por chaves naturais e derivados por payload.
5. Testar cada consulta, uso de índices, conflitos únicos, filtros de validade, transição concorrente simulada e FK.

## Aceite

- DAOs não decidem regra de produto complexa.
- Consultas necessárias da spec existem e retornam apenas dados consistentes.
