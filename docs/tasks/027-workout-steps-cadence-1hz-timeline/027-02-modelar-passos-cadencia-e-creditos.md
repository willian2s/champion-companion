# 027-02 — Modelar passos, cadência e créditos de atividade

- **Ticker:** `027`
- **Número:** `02`
- **Status:** `pending`
- **Depends on:** `027-01`

## Objetivo

Adicionar os contratos mínimos necessários para representar passos/cadência da atividade e o crédito idempotente contra o próximo Daily Summary, sem ainda alterar o comportamento remoto completo.

## Pré-condição

`027-01` concluída com ADR e política de cobertura/residual fechadas.

## Trabalho

### Projeções de atividade

Adicionar conceitos equivalentes a:

```text
WorkoutSteps
StepsCadenceSeries
```

na infraestrutura de projeções filhas, com:

- chave estável;
- tipo lógico correto;
- identidade determinística;
- versionamento monotônico;
- payload congelado suficiente para retry.

Não reutilizar `STEPS` de `DailySummary` de forma que colida identidade de agregado/filho.

### Crédito/cobertura

Persistir estado identificável por atividade e dia local, incluindo o mínimo necessário para distinguir:

- ainda não publicado;
- operação pendente/falhou;
- aceito pelo Health Connect e disponível como crédito;
- já consumido por incremento diário;
- atividade já coberta por Daily anterior;
- não elegível/fail-closed.

O modelo exato deve seguir o ADR de `027-01`.

### Room

Se o estado não couber de forma segura no schema v13:

- criar migration aditiva `13 -> 14`;
- atualizar schema exportado;
- não executar efeito remoto durante migration;
- não inferir créditos para atividades históricas automaticamente.

### Permissões

Preparar suporte explícito a `StepsCadenceRecord` e `StepsRecord` da atividade no domínio de permissões, sem solicitar permissão que não seja efetivamente usada.

## Invariantes

- uma atividade possui no máximo uma identidade de passos vigente por versão lógica;
- um aceite remoto de passos pode gerar no máximo um crédito;
- reprocessamento local não duplica crédito;
- `StepsCadenceRecord` não cria crédito;
- histórico existente permanece legível;
- sem leitura do Health Connect.

## Fora de escopo

- escrever records remotamente;
- alterar cálculo do delta diário;
- timeline 1 Hz;
- UI nova além do estritamente necessário para estado existente.

## Verificação

Se houver código/schema:

```bash
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem pedido explícito.
