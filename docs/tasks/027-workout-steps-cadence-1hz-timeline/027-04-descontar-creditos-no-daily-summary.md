# 027-04 — Descontar créditos no Daily Summary

## Objetivo

Integrar passos de atividades já aceitos remotamente ao cálculo incremental da Spec 016 sem inflar o total diário.

## Regra central

```text
grossDelta = currentWatchTotal - lastWatchCheckpoint
eligibleCredits = créditos de atividades aceitos e ainda não consumidos
residualDelta = grossDelta - eligibleCredits
```

Somente créditos elegíveis para o mesmo dia/cobertura participam.

## Invariantes

- `residualDelta` nunca é negativo;
- crédito pendente/falhado não reduz Daily;
- crédito de outro dia não reduz Daily;
- checkpoint cumulativo do relógio representa o total observado, não o residual enviado;
- crédito só é consumido depois que o efeito remoto e o estado local permitem concluir a operação de forma idempotente;
- retry do Daily não consome crédito duas vezes;
- múltiplas atividades usam créditos independentes;
- redução/reset inesperado do contador mantém comportamento seguro da Spec 016.

## Janela temporal

Aplicar exatamente a política definida em `027-01` para evitar que `StepsRecord` residual conte passos sobre a mesma janela já ocupada por projeção de atividade.

Prioridade:

1. total diário correto;
2. ausência de duplicidade;
3. janela válida e não conflitante;
4. granularidade temporal intradiária, apenas quando suportada.

Se nenhuma representação remota segura existir, falhar fechado sem avançar estado de forma que perca passos.

## Ordem inversa

Quando um Daily já sincronizado cobre uma atividade, persistir cobertura suficiente para que `027-03` não publique posteriormente os mesmos passos.

Não deletar ou repartir automaticamente records diários remotos antigos nesta spec.

## Compatibilidade Spec 016

Preservar:

- incrementos imutáveis;
- payload congelado;
- retry determinístico;
- ponte legada;
- checkpoint transacional;
- ausência de zero/negativo;
- independência por dia local.

Se `DailyStepsSyncPayload` precisar mudar, versionar/migrar de forma compatível em vez de reinterpretar payloads antigos.

## Verificação

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.
