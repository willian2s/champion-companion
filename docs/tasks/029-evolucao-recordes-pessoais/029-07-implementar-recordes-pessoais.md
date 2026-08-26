# 029-07 — Implementar read model e avaliador de recordes pessoais

- **Ticker:** `029`
- **Número:** `07`
- **Status:** `pending`
- **Depends on:** `029-06`

## Objetivo

Criar a leitura dos recordistas atuais e o avaliador capaz de detectar uma nova conquista a partir do snapshot anterior e do snapshot persistido novo.

## Recordes iniciais

- maior distância final por modalidade comparável;
- maior duração;
- maior quantidade de passos em modalidade elegível.

Ficam fora nesta versão:

- maior BPM;
- maior cadência;
- menor pace/melhor velocidade sem contrato de distância/modalidade específico;
- badges persistidos ou gamificação extensa.

## Avaliação

Para cada métrica/escopo:

```text
baselineOther = melhor valor das demais atividades elegíveis
oldEligibleValue = valor anterior da própria atividade, se antes elegível no mesmo escopo
previousBest = max(baselineOther, oldEligibleValue)

newValue > previousBest
    -> nova conquista
```

A atividade atual deve ser excluída de `baselineOther`.

## Regras de idempotência

- empate não é novo recorde;
- re-save sem mudança não gera conquista;
- se a atividade já era recordista e seu valor persistido aumenta, o valor anterior participa de `previousBest` e a melhoria pode gerar nova conquista;
- mudança de tipo compara contra a baseline do novo tipo;
- atividade antiga compara contra todo o histórico atualmente conhecido, não somente contra atividades cronologicamente anteriores.

## Falha segura

A avaliação acontece depois do commit funcional. Erro ao consultar/evaluar recordes:

- não desfaz o save;
- não muda workflow;
- não cria estado falso de recorde;
- pode ser registrado apenas por mecanismo diagnóstico seguro já vigente.

## Critérios de aceite

1. Recordistas atuais são deriváveis sem tabela de badges.
2. Distância usa o valor final válido por modalidade.
3. `CYCLING` não usa passos D4 como recorde.
4. Re-save sem alteração é idempotente para o alerta.
5. Melhoria da própria atividade recordista usa o valor anterior como baseline.
6. Atividade antiga só gera conquista se for o melhor valor atualmente conhecido.
7. Falha do evaluator não converte save bem-sucedido em falha.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```
