# 027-11 — Validar e fechar a Spec 027

## Objetivo

Executar validação final integrada e fechar a spec com baseline factual.

## Validar integridade de passos

- atividade antes do Daily;
- Daily antes da atividade;
- retry sem duplicidade;
- múltiplas atividades quando reproduzível;
- mudança de dia/reset conforme política final;
- total diário final não excede total observado por efeito desta spec;
- crédito não é consumido antes de efeito remoto seguro.

## Validar cadência

- cadência por intervalo local coerente;
- `StepsCadenceRecord` aceito quando elegível;
- resultado Health Sync/Strava documentado;
- comportamento quando passos da atividade estão bloqueados por cobertura diária.

## Validar timeline 1 Hz

- BPM e cadência compartilham a mesma grade temporal;
- gaps permanecem ausentes;
- pontos interpolados não substituem SOURCE;
- início/fim da sessão corretos;
- série longa não causa regressão visual evidente;
- versão do algoritmo permanece identificável;
- estatísticas históricas não mudam silenciosamente.

## Health Connect

Confirmar:

- identities estáveis;
- retry determinístico;
- nenhuma duplicação de `StepsRecord`;
- decisão final bucket vs 1 Hz para `StepsCadenceRecord`;
- sync continua explícito e write-only;
- nenhuma leitura HC introduzida.

## Documentação

Atualizar:

- `docs/specs/027-workout-steps-cadence-1hz-timeline.md`;
- `027-00-overview.md`;
- `docs/baseline/current-baseline.md`;
- ADR correspondente;
- docs de integração/evidência;
- `AGENTS.md` somente se houver nova regra operacional duradoura.

Não apagar histórico da Spec 016 nem reclassificar specs anteriores.

## Classificação final

Registrar separadamente:

- integridade de passos;
- Health Connect;
- Health Sync/Strava;
- timeline local 1 Hz.

É permitido fechar como `completed` com limitações externas claramente documentadas, desde que integridade local/remota de passos esteja preservada.

## Verificação

Para código/Gradle:

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.
