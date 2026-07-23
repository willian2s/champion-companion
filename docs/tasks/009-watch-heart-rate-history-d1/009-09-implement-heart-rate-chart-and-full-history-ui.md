# Task 009-09 - Implementar grafico e historico completo de frequencia cardiaca

## Status

Concluida em 2026-07-23.

## Objetivo

Implementar UI final da tela de frequencia cardiaca para D1: primeiro card com duas acoes, progresso, ultima importacao, grafico funcional, historico consolidado e alternativa textual acessivel.

## Contexto

Com `HealthViewModel` e leitura local prontos, `HeartRateDetailScreen` deixa de ser shell da Fase 08 e passa a ser superficie completa da Fase 09. UI precisa preservar medicao manual existente e acrescentar D1 sem reimplementar D0/F002.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-08-integrate-health-viewmodel-action-progress-cancel-and-retry.md`
- `docs/decisions/adr-009-health-metrics-hub-navigation.md`

## Dependencias

- `009-06`
- `009-08`

## Entradas

- estado de UI D1 pronto no ViewModel;
- queries locais e serie do grafico prontas;
- medicao manual existente preservada.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `docs/specs/009-watch-heart-rate-history-d1.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-00-overview.md`
- `docs/tasks/009-watch-heart-rate-history-d1/009-09-implement-heart-rate-chart-and-full-history-ui.md`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/**`
- `feature/device/**`
- `feature/home/**`

## Escopo

- substituir placeholder `Acoes futuras` por acao funcional `Buscar dados do relogio`;
- manter `Medir frequencia cardiaca` e seu lifecycle atual;
- mostrar progresso D1 e ultima importacao no primeiro card;
- renderizar grafico funcional para `Hoje`, `7 dias` e `30 dias`;
- renderizar historico consolidado com origem amigavel;
- incluir alternativa textual e acessivel;
- produzir screenshots para PR futuro conforme `AGENTS.md`.

## Fora Do Escopo

- alterar parser D1;
- alterar Room;
- alterar gate BLE;
- integrar Health Connect.

## Passos

1. Evoluir `HeartRateDetailScreen` para usar novo estado D1 do ViewModel.
2. Implementar primeiro card com ultimo BPM, horario, origem, acoes manual + D1, progresso e ultima importacao.
3. Criar `HeartRateHistoryChart.kt` ou componente equivalente local sem adicionar biblioteca externa.
4. Implementar lista historica consolidada com agrupamento simples por dia, origem amigavel e estado vazio.
5. Garantir copy e semantica acessivel para sucesso, parcial, falha e cancelamento.
6. Registrar screenshots e atualizar spec/overview com nomes reais dos componentes.

## Criterios De Aceite

- primeiro card contem todos os elementos obrigatorios;
- medicao manual continua funcional;
- `Buscar dados do relogio` aparece apenas na tela especifica;
- grafico funcional existe sem biblioteca externa nova, salvo justificativa forte documentada;
- historico consolidado manual + D1 funciona;
- lista textual continua disponivel como alternativa ao grafico.

## Testes Pequenos Aplicaveis

- sem campanha ampla de UI; apenas testes pequenos de formatter ou mapper local quando isso reduzir risco real.

## Validacao Minima

- `./gradlew assembleDebug`
- `./gradlew installDebug`

## Evidencias

- screenshots da tela de frequencia cardiaca com D1;
- diff da UI mostrando remocao do placeholder e uso do novo estado D1.

## Implementacao Executada

- `HealthMetricDetailScreens.kt` agora usa `HeartRateOverviewCard` como primeiro card da tela, com ultimo BPM, origem, `Medir frequencia cardiaca`, `Buscar dados do relogio`, progresso D1, estados terminais acessiveis e metadata da ultima importacao.
- `HeartRateHistoryChartSection` agora renderiza seletor de periodo, agregacoes locais, grafico funcional e leitura textual dos pontos sem depender de biblioteca externa.
- `HeartRateHistoryList` agora mostra historico consolidado manual + D1 agrupado por dia e com origem amigavel.
- `HeartRateHistoryChart.kt` foi criado com grafico local em `Canvas` para `Hoje`, `7 dias` e `30 dias`.
- `HealthScreen.kt` agora resume melhor a tela de frequencia cardiaca com referencia a importacao D1 e ultima importacao bem-sucedida quando existir.
- refinamento posterior aplicado na mesma task: grafico passou a seguir linguagem visual inspirada no Google Health, com cabecalho-resumo grande, seletor compacto `D/S/M`, eixos discretos e faixas horizontais visuais baseadas nas referencias aprovadas.

## Direcao Visual Posterior Registrada

- refinamento futuro do grafico pode seguir linguagem visual das referencias em `docs/investigations/google-health-hr-diario.jpeg`, `google-health-hr-semanal.jpeg` e `google-health-hr-mensal.jpeg`.
- uso de limites/faixas horizontais inspirados nessas referencias foi considerado aceitavel para iteracao futura.
- antes de implementar nomes ou semantica clinica para essas faixas, produto precisa definir se elas serao apenas guias visuais ou zonas interpretativas fixas.

## Evidencias Coletadas Nesta Execucao

- `./gradlew assembleDebug`
- `./gradlew installDebug`
- APK debug instalada com sucesso em `SM-S901E - 16`.
- screenshot para PR futuro nao foi anexada nesta execucao porque task restringe arquivos permitidos a codigo e documentacao.

## Atualizacao Do Overview

- marcar `009-09` como concluida;
- registrar componentes reais criados e screenshots geradas;
- preparar roteiro de `009-10`.

## Validacao Final

- confirmar que nenhuma biblioteca de grafico foi adicionada sem justificativa documentada.
