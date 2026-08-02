# 018 — Integridade do calendário após importação de treino

- **Status geral:** concluído
- **Spec:** [018-integridade-do-calendario-apos-importacao-de-treino.md](../../specs/018-integridade-do-calendario-apos-importacao-de-treino.md)
- **Progresso:** 1/1 subtarefa concluída

## Objetivo

Corrigir falso diagnóstico de dados parciais no Calendário após reimportação de
treino, preservando ordem temporal de atividades revisadas e degradações reais.

## Checklist

- [x] [018-01-alinhar-integridade-de-bpm-da-atividade.md](018-01-alinhar-integridade-de-bpm-da-atividade.md)

## Observações

- Specs 015, 015A e 017 permanecem concluídas; task 018 não reabre nenhuma delas.
- Banco local real foi consultado no dispositivo autorizado pelo usuário.
- Causa confirmada no APK: reimportação atualizava `recordedAt` após
  `reviewedAt`, tornando o aggregate não resolvido.
- Migration 7→8 estreita reparou registros legados elegíveis.
- Não previsto: escrita durante consulta do Calendário, testes automatizados ou
  alteração visual.
- Dispositivo reconectado; APK final instalado e Calendário validado sem banner.
- Banco pós-migration está na versão 8 e não possui atividades com
  `reviewedAt < recordedAt`.
- Revisão independente final aprovou correção; guard temporal foi validado no
  domínio e no planner.
