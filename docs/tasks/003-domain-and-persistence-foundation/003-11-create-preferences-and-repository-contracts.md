# Task 003-11 — Criar Preferências e Contratos de Repository

## Status

Concluída.

## Dependência

- `003-09` concluída.

## Objetivo

Persistir AppPreferences simples em DataStore e declarar interfaces de repository orientadas a agregado.

## Pode modificar

- `core/preferences/**`, contratos em `core/model/**` ou `data/**`, DI correspondente e testes JVM.
- Este arquivo.

## Passos

1. Criar AppPreferences DataStore: seguir sistema/claro/escuro, cores dinâmicas, preferências de cálculo/sync/diagnóstico estritamente previstas; fallback seguro para dados ausentes/corrompidos.
2. Não redefinir design system nem criar persistência de estados de domínio no DataStore.
3. Declarar interfaces específicas: device associado; resumo diário; importar/consultar BPM; persistir/revisar/descartar atividade; calibração; recuperar/finalizar sessão; consultar/marcar sync; armazenar/reprocessar/limpar payload; observar/atualizar preferências.
4. Contratos usam modelos de domínio e resultados seguros, nunca DAO/entity/Room/Android.
5. Testar serialização/migração de preferência ausente e contratos de observação quando aplicável.

## Aceite

- Preferências simples ficam em DataStore; perfil, saúde, sessões, filas e payloads permanecem Room.
- Nenhum repository é espelho genérico de DAO.
