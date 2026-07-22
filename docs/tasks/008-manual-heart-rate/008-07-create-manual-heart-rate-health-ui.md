# Task 008-07 - Criar UI da Saude para medicao manual

## Status

Concluída em 2026-07-22.

## Resultado

- `HealthScreen` deixou de ser placeholder e passou a coletar `HealthViewModel` como fonte única de estado da tela Saúde.
- A UI agora cobre `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled` e `Failed` sem mover detalhes de protocolo ou BLE para Compose.
- Estado de medição exibe BPM grande, valor estimado quando aplicável, qualidade de sinal, tempo restante, progresso e CTA de cancelamento.
- Estados `Completed`, `Cancelled` e `Failed` receberam feedback de produto em pt-BR, com ações de concluir, medir novamente, fechar aviso e tentar novamente.
- A tela também mostra última medição e histórico simples de medições recentes fora da sessão ativa.
- `BackHandler` com confirmação de saída e hook de lifecycle para `AppMovedToBackground` foram adicionados sem introduzir auto start.

## Objetivo

Substituir placeholder da Saude por UI funcional de medicao manual, historico simples e estados de erro/conclusao.

## Contexto

Fase 08 precisa superficie final de produto na Saude, mas sem mover protocol/BLE para Compose e sem iniciar medicao automaticamente.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `008-06`

## Dependencias

- `008-06`

## Entradas

- `HealthViewModel`
- `HealthUiState`
- design system existente

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthViewModel.kt`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-07-create-manual-heart-rate-health-ui.md`

## Arquivos Proibidos

- `HomeScreen.kt`
- `RoomRepositories.kt`
- `ChampionDaos.kt`
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Remover placeholder.
- Exibir estados `Idle`, `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`, `Completed`, `Cancelled`, `Failed`.
- Exibir ultima medicao e recentes.
- Exibir BPM ao vivo, progresso, tempo restante, orientacao de sinal e botao cancelar.
- Exibir confirmacao de salvamento em `Completed`.
- Exibir retry seguro em `Failed`.
- Acessibilidade minima obrigatoria.

## Fora Do Escopo

- Health Connect.
- Graficos.
- Testes Compose amplos.

## Passos

1. Coletar `HealthViewModel` na tela.
2. Criar layout para `Idle` com ultima medicao, historico e CTA principal.
3. Criar layout de `Measuring` com BPM grande, tempo restante, progresso e cancelar.
4. Criar `Completed`, `Cancelled` e `Failed`.
5. Adicionar mensagens acessiveis e indicador discreto de valor estimado.
6. Atualizar overview.

## Criterios De Aceite

- Placeholder removido.
- Botao principal inicia medicao somente por toque.
- Historico e ultima medicao permanecem visiveis fora da sessao ativa.
- Erros mostram mensagem segura sem detalhe tecnico sensivel.

## Testes Pequenos Aplicaveis

- Nenhum exclusivo. Se houver helper puro de formatacao/estado, teste pequeno pode acompanhar.

## Validacao Minima

```bash
./gradlew assembleDebug
```

## Evidencia Manual

- Descrever estados visuais implementados e como cada um liga ao `HealthUiState`.

- `HealthUiState.surfaceState` define bloco principal da tela:
  - `Idle`: CTA principal e orientação para medição manual.
  - `Preparing`, `Connecting`, `Starting`, `Measuring`, `Stopping`: card de progresso da sessão.
  - `Completed`: confirmação de salvamento com resultado final.
  - `Cancelled`: aviso de cancelamento sem persistência parcial.
  - `Failed`: erro seguro com retry quando aplicável.
- `HealthUiState.latestMeasurement` alimenta card da última medição.
- `HealthUiState.recentMeasurements` alimenta lista simples de recentes.
- `HealthUiState.hasAssociatedDevice` controla disponibilidade do CTA principal.

## Atualizacao Do Overview

- Marcar `008-07` como concluida.

## Validacao Final

- Confirmar ausencia de auto start ao abrir a tela.

- Confirmado: abrir a tela envia apenas `HealthIntent.Load`; a medição só começa após toque explícito em `Medir frequência cardíaca`.
