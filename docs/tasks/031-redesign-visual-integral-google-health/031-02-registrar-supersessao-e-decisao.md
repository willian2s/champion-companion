# 031-02 — Registrar supersessão e decisão

- **Ticker:** `031`
- **Número:** `02`
- **Status:** pending

## Objetivo

Conferir e fechar governança documental que substitui direção da Spec 024 e
ADR-023, mantendo histórico, privacidade e decisões funcionais vigentes.

## Requisitos e aceite

- Spec 024 e ADR-023 aparecem como supersedidas, sem apagar fechamento,
  evidências ou subtarefas históricas.
- ADR-028 é aceita após revisão e referencia Spec 031.
- Guard rails de Room, BLE, Health Connect, privacidade e phone workout ficam
  explícitos.
- Ticker, links, status e rastreabilidade permanecem consistentes.

## Escopo

Inclui documentação de decisão e alinhamento dos artefatos SDD. Exclui código de
produção, remoção de histórico, mudança de roadmap macro e relaxamento de
privacidade.

## Dependências

- `031-01`.
- `docs/specs/024-refatoracao-visual-google-health.md`.
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`.

## Arquivos e símbolos prováveis

- `docs/decisions/adr-028-redesign-visual-integral-google-health.md`.
- Spec 024, ADR-023 e overview 024.
- Spec/overview da 031.

## Passos

1. Revisar baseline da referência e conflitos com ADR-023.
2. Registrar supersessão somente nos metadados e notas históricas.
3. Conferir links e matriz de requisitos.
4. Solicitar revisão independente antes de código.

## Validação e comandos

```bash
git diff --check
```

Não executar testes automatizados; não alterar código.

## Definição de pronto

Decisão única e rastreável; histórico preservado; nenhum guard de privacidade
removido; revisão independente aprovada. Nenhuma subtarefa posterior começa sem
esse aceite. Estado inicial/restaurado do aparelho é registrado quando houver
inspeção física.

## Riscos

Supersessão pode ser confundida com apagamento. Manter notas de fechamento e
deixar claro que somente direção visual/shell foi substituída.
