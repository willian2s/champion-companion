# Spec 012A - Simplificação da tela de frequência cardíaca com pull-to-refresh D1

## Status

Concluída em 2026-07-27.

## Contexto

Antes desta mudança, a tela de detalhe de frequência cardíaca concentrava:

- card `Resumo atual e ações` com último BPM, origem, relógio associado e dois CTAs fixos;
- card `Medição manual` com CTA própria para iniciar a sessão de 60 segundos;
- painel D1 com progresso, resumo da importação e ações contextuais;
- gráfico e histórico consolidado.

O estado anterior gerava redundância visual e funcional:

- `Medir frequência cardíaca` aparece no overview e também na superfície dedicada de medição manual;
- `Buscar dados do relógio` fica preso ao overview, apesar de ser semanticamente uma atualização explícita do histórico local D1;
- a Home já usa `PullToRefreshBox` como padrão explícito de atualização manual no app.

Este pedido é deliberadamente fora do roadmap. A mudança deve permanecer restrita à experiência atual da tela sem reabrir escopo de fases anteriores ou futuras.

## Objetivo

Reduzir redundância na tela de frequência cardíaca, transformando o card `Resumo atual e ações` em resumo puro, renomeado para `Resumo atual`, e movendo o gatilho de importação D1 para o gesto de pull-to-refresh da própria tela.

## Estado anterior confirmado

- `HeartRateDetailScreen` usa `Surface + Column + verticalScroll`, sem `PullToRefreshBox`.
- `HeartRateOverviewCard` renderiza:
  - CTA `Medir frequência cardíaca`;
  - CTA `Buscar dados do relógio` com variações para cancelamento e retry;
  - painel `HeartRateD1ImportPanel`.
- `HeartRateSessionSurface` já é a superfície principal da medição manual e expõe CTA própria quando o estado está ocioso.
- `HealthIntent.Load` apenas recarrega estado local observado; ele não deve iniciar BLE automaticamente.
- O padrão de pull-to-refresh existente no app está em `HomeScreen`, com ação acessível explícita.

## Decisões

### 1. O card `Resumo atual e ações` deixa de ser owner de CTAs fixos e volta a ser apenas resumo

O overview passa a mostrar apenas:

- último BPM persistido, quando existir;
- horário e origem do último registro;
- relógio associado, quando existir;
- painel informativo do D1;
- feedback secundário do Health Connect.

Os botões fixos de `Medir frequência cardíaca` e `Buscar dados do relógio` saem desse card.

O título do card passa a ser `Resumo atual`.

### 2. A medição manual continua acessível somente pela superfície própria

O CTA principal de medição permanece em `HeartRateSessionSurface` / `IdleMeasurementCard`.

Não haverá novo CTA de medição no overview.

### 3. A importação D1 passa a ser disparada por pull-to-refresh explícito

`HeartRateDetailScreen` passa a usar `PullToRefreshBox`, seguindo o padrão já usado na Home.

Regras:

- o gesto de puxar para baixo no topo da tela é a nova ação explícita de importação D1;
- `HealthIntent.Load` continua sem iniciar BLE automaticamente;
- o refresh deve usar o estado existente de importação D1 para refletir spinner e bloqueios;
- o gesto não deve iniciar D1 quando já houver medição manual ativa ou importação D1 em andamento;
- o gesto não cria novo fluxo de sincronização fora do D1 já existente.

### 4. O painel D1 preserva feedback e recuperabilidade, sem ação de cancelamento

Após remover os botões fixos do overview:

- em falha recuperável, o painel D1 deve continuar oferecendo retry explícito;
- em estados terminais, o painel D1 deve continuar permitindo dismiss do aviso.
- durante importação em andamento, o painel D1 continua exibindo progresso e metadata disponível, sem expor botão ou ação contextual de cancelamento;
- a confirmação de retorno já existente durante D1 (`ConfirmExitD1Import`) permanece o único comportamento de saída segura da tela, sem alteração em lifecycle, `ViewModel` ou coordinator.

Assim, o overview perde redundância, enquanto o fluxo D1 preserva feedback, retry, dismiss e saída segura já existente.

### 5. A acessibilidade do refresh deve continuar explícita

A tela deve expor ação acessível equivalente ao gesto, alinhada ao padrão da Home.

Direção:

- adicionar ação acessível com texto equivalente a `Atualizar histórico do relógio`;
- atualizar estados vazios e orientações textuais para mencionar o gesto de puxar para atualizar;
- manter dados já persistidos visíveis durante importação, erro, parcial e sucesso.

### 6. Não haverá mudança de contrato arquitetural ampla

A mudança deve permanecer localizada na feature de Saúde.

Decisão de implementação:

- reutilizar `D1HeartRateImportSurfaceState` como fonte de verdade para `isRefreshing`;
- não introduzir refresh automático em lifecycle;
- evitar expandir escopo para repositories, BLE core, Room, navegação, roadmap ou ADR.

## Mudanças previstas

Arquivos com maior probabilidade de alteração na implementação:

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt` somente se for necessário concentrar a semântica de refresh em um intent dedicado
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt` somente se a UI exigir novo intent explícito
- `docs/baseline/current-baseline.md` no fechamento da task, para refletir a nova UX factual

Direção escolhida para a subtarefa inicial:

- tentar primeiro uma solução sem novos contratos, roteando o pull-to-refresh para o fluxo D1 já existente e preservando regras de bloqueio com o estado atual;
- só introduzir novo intent se a implementação mostrar duplicação perigosa de lógica na UI.

## Critérios de aceite

- o card `Resumo atual e ações` não exibe mais a fileira fixa de botões de medição e busca;
- o card passa a se chamar `Resumo atual`;
- a tela de frequência cardíaca passa a aceitar pull-to-refresh como gatilho explícito da importação D1;
- o gesto não dispara nova importação quando medição manual ou D1 já estiverem ativos;
- `HealthIntent.Load`, `ON_START` e `ON_RESUME` continuam sem iniciar BLE automaticamente;
- o CTA de medição manual continua disponível pela superfície dedicada de medição;
- o painel D1 preserva progresso e metadata, oferece retry em falha recuperável e dismiss em estados terminais, sem ação de cancelamento na tela;
- a confirmação de retorno durante D1 continua preservada, sem mudanças em lifecycle, `ViewModel` ou coordinator;
- o spinner de refresh representa corretamente estados `Preparing`, `Connecting`, `Reading` e `Persisting` do D1;
- leitores de tela recebem ação equivalente de atualização do histórico;
- a rolagem vertical, o gráfico, o histórico e os estados atuais de Health Connect permanecem funcionais.

## Fora do escopo

- criar novo fluxo automático de sincronização ao abrir a tela;
- alterar parser, coordinator BLE ou persistência D1;
- alterar navegação, Home, Exercícios ou Settings;
- alterar roadmap;
- criar novos testes por padrão;
- introduzir ADR nova.

## Riscos e cuidados

- `PullToRefreshBox` precisa conviver corretamente com `verticalScroll` e só disparar no topo da tela;
- a remoção do botão fixo de importação não pode eliminar progresso, metadata, retry, dismiss ou a confirmação de retorno segura durante D1;
- a UI não pode voltar a iniciar BLE por carregamento passivo de tela;
- a comunicação textual do overview deve deixar de mencionar “toque em Buscar dados do relógio” e passar a orientar o gesto de puxar para atualizar;
- o baseline factual precisa ser atualizado no fechamento para não manter referência ao botão removido.

## Fechamento

- `HeartRateDetailScreen` passou a usar `PullToRefreshBox`, com indicador derivado dos estados ativos de D1 e guarda contra medição manual ou importação D1 já ativa.
- O card passou a se chamar `Resumo atual`, sem CTAs fixos; a medição manual continua acessível somente por `HeartRateSessionSurface`.
- O painel D1 preserva progresso, metadata, retry recuperável e dismiss terminal. Não há botão ou ação contextual de cancelamento; a confirmação de saída segura existente foi preservada.
- A validação técnica e física foi concluída no dispositivo `SM-S901E`: pull-to-refresh realizou D1, atualizou metadata, exibiu resultado com lacunas e permitiu dismiss, com gráfico e histórico ainda visíveis.

## Referências

- `AGENTS.md`
- `docs/baseline/current-baseline.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
