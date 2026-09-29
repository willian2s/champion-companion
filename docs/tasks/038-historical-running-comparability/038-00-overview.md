# 038 — Recuperação de comparabilidade histórica de corridas

- **Status geral:** pending
- **Spec:** [038-historical-running-comparability.md](../../specs/038-historical-running-comparability.md)
- **Progresso:** 0/6 subtarefas concluídas

## Objetivo

Auditar se corridas históricas `WATCH_D3_D4` confirmadas como `RUNNING` podem
recuperar comparabilidade nativa com fatos locais preservados, sem inventar
dados, falsificar parser/provenance, alterar snapshots históricos ou produzir
qualquer efeito Health Connect.

Gate A decide se existe implementação segura. `NO-GO` e ausência de banco
físico encerram o ciclo sem código artificial.

## Checklist

- [ ] [038-01-auditar-recuperabilidade-e-congelar-gate.md](038-01-auditar-recuperabilidade-e-congelar-gate.md)
- [ ] [038-02-implementar-preflight-de-recuperacao.md](038-02-implementar-preflight-de-recuperacao.md)
- [ ] [038-03-implementar-recuperacao-local-explicita.md](038-03-implementar-recuperacao-local-explicita.md)
- [ ] [038-04-integrar-recuperacao-com-exercicios-e-037.md](038-04-integrar-recuperacao-com-exercicios-e-037.md)
- [ ] [038-05-validar-preservacao-e-ganho-de-comparabilidade.md](038-05-validar-preservacao-e-ganho-de-comparabilidade.md)
- [ ] [038-06-validar-fisicamente-e-fechar.md](038-06-validar-fisicamente-e-fechar.md)

## Observações

- Room alvo: v20, sem nova migration nesta spec.
- Health Connect permanece write-only, explícito e sem efeito causado por
  recuperação.
- 037 mantém somente `WATCH_NATIVE`/
  `WATCH_NATIVE_CONTEXT_CALIBRATED` com `watch-d3-d4-v4` e temporalidade/
  plausibilidade válidas.
- 038-02 a 038-06 são condicionais ao Gate A. Em `NO-GO` ou
  `INCONCLUSIVO`, tarefas de implementação são fechadas como não aplicáveis,
  sem código.
- Evidência física sanitizada 037 conhece somente 21 corridas, 5 nativas e 16
  fallback; breakdown dos fallbacks ainda precisa de banco read-only.
