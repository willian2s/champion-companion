# Task 002-04 — Criar Componentes de Ação e Status

## Status

Pendente.

## Dependência

- `002-03` concluída.

## Objetivo

Criar `PrimaryActionButton`, `ConnectionStatus`, `SyncStatusBadge` e `ActivityStatusBadge` reutilizáveis.

## Pode modificar

- Arquivos novos de componentes em `core.designsystem`.
- Testes e previews destes componentes; este arquivo.

## Passos

1. Definir APIs com textos, estado visual, slots e callbacks mínimos.
2. Implementar botão com estado habilitado/carregando, área de toque Material 3 e semântica.
3. Implementar conexão e badges consumindo somente estados/tokens de `002-03`.
4. Garantir rótulo e ícone em cada status; não usar cor como único sinal.
5. Criar previews claro/escuro, fonte ampliada e testes Compose de renderização, clique, semântica e alvo de toque.

## Fora do escopo

- Chamada BLE, sincronização real, persistência e lógica de negócio.

## Aceite

- APIs não aceitam modelos técnicos/domínio.
- Estados exigidos renderizam rótulo, ícone e semântica nos dois temas.
