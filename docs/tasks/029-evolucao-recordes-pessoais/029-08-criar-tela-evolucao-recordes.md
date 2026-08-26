# 029-08 — Criar tela Evolução e superfície de recordes

- **Ticker:** `029`
- **Número:** `08`
- **Status:** `pending`
- **Depends on:** `029-07`

## Objetivo

Criar a superfície visual de evolução esportiva usando os read models e gráficos compartilhados já existentes.

## Escopo

A tela deve apresentar:

- seletor 7/30/90 dias;
- filtro por modalidade quando aplicável;
- resumo de volume/consistência;
- gráficos de tendências elegíveis;
- comparação com período anterior;
- seção de recordes pessoais atuais com data da atividade recordista;
- navegação para a atividade recordista quando houver `activityId` válido.

## UX

Priorizar leitura curta antes do detalhamento:

```text
Evolução
[7 dias] [30 dias] [90 dias]

Resumo do período
Tendências
Comparação
Recordes pessoais
```

Os recordes exibem fatos e unidade, sem linguagem de recomendação fisiológica.

## Gráficos

- usar exclusivamente `core/designsystem/chart`;
- marker/tooltip seguem a Spec 028;
- gaps permanecem gaps;
- scroll/zoom somente quando densidade justificar;
- light/dark e fonte ampliada devem permanecer utilizáveis.

## Navegação

Usar a decisão registrada em `029-01`, preservando Navigation 3 e back stack manual. Não reativar destinos legados de treino phone.

## Critérios de aceite

1. Usuário consegue alternar período e modalidade.
2. Cards e gráficos reconciliam com o read model.
3. Recordes mostram valor, tipo e data real da atividade.
4. Atividade recordista pode ser aberta quando elegível.
5. Tela preserva estados loading/empty/failed.
6. Nenhuma feature importa Vico diretamente.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validar fisicamente tema claro/escuro, fonte ampliada, rolagem, filtros, tooltips e estados disponíveis. Registrar screenshot sanitizado.
