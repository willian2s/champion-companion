# Task 002-06 — Criar Feedback e Banners

## Status

Pendente.

## Dependência

- `002-03` concluída.

## Objetivo

Criar `EmptyState`, `ErrorState`, `LoadingState`, `PermissionBanner`, `PendingReviewBanner` e `CalibrationBanner`.

## Pode modificar

- Arquivos novos de componentes em `core.designsystem`.
- Testes e previews destes componentes; este arquivo.

## Passos

1. Definir APIs de textos seguros, ícones, slots e callbacks opcionais; não aceitar exceções internas.
2. Implementar estados vazio, erro e carregamento com texto, ícone e semântica apropriados.
3. Implementar banners informativos com ação opcional e conteúdo acessível.
4. Usar erro seguro apresentado pelo chamador; não expor stack trace, mensagem interna ou dados sensíveis.
5. Criar previews claro/escuro, fonte ampliada e testes Compose de estados, semântica e ações.

## Aceite

- Todos componentes exigidos renderizam sem dependência de feature.
- Feedback comunica estado além de cor e preserva segurança de erros.
