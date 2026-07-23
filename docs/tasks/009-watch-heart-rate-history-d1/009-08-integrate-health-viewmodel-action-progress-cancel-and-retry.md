# Task 009-08 - Integrar HealthViewModel, acao, progresso, cancelamento e retry

## Status

Pendente.

## Objetivo

Integrar `HealthViewModel` ao fluxo D1, com estados independentes da medicao manual, periodo selecionado, progresso, cancelamento, retry, metadata de ultima importacao e comportamento correto de lifecycle.

## Contexto

Depois de `009-06` e `009-07`, feature ja deve ter coordinator D1 funcional e camada de leitura local pronta. Falta ligar isso ao owner de produto da tela, mantendo isolamento entre medicao manual e importacao D1 e atualizando regras de back/background da subtree de Saude.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-06-implement-heart-rate-history-queries-aggregations-and-chart-policy.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-07-implement-d1-sync-coordinator-and-foreground-gate.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`

## Dependencias

- `009-06`
- `009-07`

## Entradas

- state machine D1 aprovada;
- queries locais de historico e agregacoes prontas;
- coordinator D1 pronto;
- necessidade de coexistencia com medicao manual ja entregue na Fase 08.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-08-integrate-health-viewmodel-action-progress-cancel-and-retry.md`
- `app/src/test/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModelD1IntegrationTest.kt`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/home/**`
- `feature/device/**`

## Escopo

- adicionar intents D1 a `HealthFeatureContract.kt`;
- manter medicao manual e importacao D1 em estados independentes;
- observar historico, agregacoes, serie e metadata D1 no `HealthViewModel`;
- tratar `Load`, `AppMovedToBackground`, back navigation, cancelamento e retry de D1;
- bloquear duas operacoes BLE simultaneas na mesma tela.

## Fora Do Escopo

- UI final do grafico e da lista historica;
- parser BLE;
- migration Room;
- Health Connect real.

## Passos

1. Expandir `HealthIntent` e `HealthUiState` com acao D1, periodo selecionado, historico, agregacoes, serie e ultima importacao.
2. Integrar `HealthViewModel` ao coordinator D1 e aos observers locais de historico/agregacao/metadata.
3. Garantir que `Load` e mudanca de periodo nao iniciem BLE.
4. Expandir `ChampionNavigation.kt` para proteger saida da tela quando D1 ou medicao manual estiverem ativos.
5. Adicionar testes pequenos de ausencia de auto-start, cancelamento em background, retry e estados independentes.
6. Atualizar spec e overview com nomes reais dos estados e intents.

## Criterios De Aceite

- `HealthViewModel` expone estados independentes para medicao manual e D1;
- periodo selecionado nao dispara BLE;
- background cancela D1;
- back navigation exige confirmacao quando D1 estiver em progresso;
- retry abre nova tentativa sem reaproveitar listener antigo;
- nenhum historico e inferido pela UI por comparacao de listas.

## Testes Pequenos Aplicaveis

- `Load` sem auto-start D1;
- mudanca de periodo sem BLE;
- cancelamento em background;
- retry cria nova tentativa;
- estados manual e D1 coexistem sem colisao logica.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew testDebugUnitTest` focado no `HealthViewModel`.

## Evidencias

- resultados de testes pequenos;
- diff mostrando intents, estados independentes e protecao de lifecycle.

## Atualizacao Do Overview

- marcar `009-08` como concluida;
- registrar nomes reais dos estados e intents D1;
- liberar `009-09`.

## Validacao Final

- confirmar que UI final do grafico e lista historica nao foi implementada aqui.
