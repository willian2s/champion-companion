# 015A — Integridade semântica da fila de sincronização

- **Status geral:** pending
- **Spec:** [015A-health-sync-queue-semantic-integrity.md](../../specs/015A-health-sync-queue-semantic-integrity.md)
- **Progresso:** 0/2 subtarefas concluídas

## Objetivo

Corrigir a validação read-only da fila para resolver cada operação pela
elegibilidade semântica do par `aggregate + recordType`, preservando atividades
`PARTIAL`, sincronização granular e histórico dedicado de frequência cardíaca.

## Checklist

- [ ] [015A-01-implementar-integridade-semantica-por-operacao.md](015A-01-implementar-integridade-semantica-por-operacao.md)
- [ ] [015A-02-revisar-cenarios-e-fechar-correcao.md](015A-02-revisar-cenarios-e-fechar-correcao.md)

## Observações

- Ticker `015A` foi solicitado explicitamente; não criar `015-15`.
- Execução obrigatória na ordem `015A-01 -> 015A-02`.
- `015A-01` concentra hotspots de DAO, mapper e repository para evitar divisão
  artificial e alterações concorrentes nos mesmos arquivos.
- `015A-02` executa uma única revisão final pela tabela contratual e fecha somente
  esta correção.
- Testes automatizados permanecem proibidos.
