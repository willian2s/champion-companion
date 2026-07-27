# Task 012A-01 - Integrar pull-to-refresh D1

## Status

pending

## Objetivo

Adicionar pull-to-refresh à tela de frequência cardíaca como novo gatilho explícito da importação D1, preservando regras de concorrência, acessibilidade e ausência de auto-BLE passivo.

## Resultado esperado

- `HeartRateDetailScreen` usa `PullToRefreshBox`;
- o indicador de refresh acompanha o estado D1 em andamento;
- o gesto não inicia D1 durante medição manual nem durante importação D1 ativa;
- a tela expõe ação acessível equivalente ao gesto.

## Requisitos cobertos

- substituir o botão permanente de busca por gesto explícito de refresh;
- preservar D1 como ação manual;
- bloquear concorrência com medição manual e com D1 já ativo;
- manter comportamento passivo de `Load`, `ON_START` e `ON_RESUME`.

## Escopo incluído

- `feature/health/HealthMetricDetailScreens.kt`
- `feature/health/HealthViewModel.kt`, se necessário para centralizar semântica do refresh
- `feature/health/HealthFeatureContract.kt`, somente se a solução exigir intent dedicada

## Escopo excluído

- Room, repositories, BLE core, coordinator D1 e navegação;
- roadmap, ADR e baseline;
- criação de testes novos.

## Dependências

- nenhuma.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `HeartRateDetailScreen`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`

## Passos de implementação

1. Envolver `HeartRateDetailScreen` em `PullToRefreshBox`, preservando `verticalScroll`, back handling e ciclo de vida atuais.
2. Derivar `isRefreshing` do estado D1 existente.
3. Rotear `onRefresh` para o fluxo D1 explícito já existente, sem ligá-lo a `Load`.
4. Garantir bloqueio do gesto quando houver medição manual ou importação D1 ativas.
5. Adicionar ação acessível equivalente ao gesto, alinhada ao padrão da Home.
6. Se a UI ficar com regra demais, promover a decisão para intent dedicada no contrato da feature.

## Testes e comandos de validação

Não criar testes novos.

Validação manual esperada nesta subtarefa:

- puxar para atualizar no topo da tela;
- confirmar início de apenas uma importação D1;
- confirmar que o gesto não inicia D1 durante medição manual;
- confirmar que retorno à tela e `Load` continuam passivos.

## Definição de pronto

- refresh explícito funcional na tela;
- concorrência preservada;
- acessibilidade do refresh explícita;
- subtarefa e overview atualizados com status e evidências.

## Riscos e cuidados

- não quebrar a convivência entre `PullToRefreshBox` e `verticalScroll`;
- não iniciar BLE por eventos passivos de lifecycle;
- não permitir múltiplas importações D1 concorrentes.
