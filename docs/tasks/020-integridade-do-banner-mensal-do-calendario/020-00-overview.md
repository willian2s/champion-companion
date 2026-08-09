# 020 — Integridade do banner mensal do Calendário

- **Status geral:** concluído
- **Spec:** [020-integridade-do-banner-mensal-do-calendario.md](../../specs/020-integridade-do-banner-mensal-do-calendario.md)
- **Progresso:** 1/1 subtarefa concluída

## Objetivo

Evitar que operações Health Connect fora da janela consultada contaminem o
diagnóstico mensal do Calendário, preservando ausência segura de passos e FC.

## Checklist

- [x] [020-01-restringir-integridade-a-janela-do-calendario.md](020-01-restringir-integridade-a-janela-do-calendario.md)

## Observações

- Ticker, pasta, spec, overview e subtarefa usam `020`.
- Spec 018 permanece concluída; esta task trata escopo de integridade da fila,
  não reimportação temporal.
- Ausência de passos ou FC só deve permanecer ausente; não deve gerar operação
  artificial nem banner sem operação inelegível correspondente.
- `020-01` restringiu operações e flags de integridade aos agregados da janela;
  texto da UI e writer permaneceram inalterados.
- `git diff --check` e `./gradlew lint assembleDebug assembleRelease` passaram.
- Não houve reprodução física com banco contendo operação fora da janela; risco
  residual permanece para smoke manual.
