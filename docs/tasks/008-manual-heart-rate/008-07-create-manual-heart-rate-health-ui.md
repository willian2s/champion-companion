# Task 008-07 - Criar UI da Saude para medicao manual

## Status

Planejada.

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

## Atualizacao Do Overview

- Marcar `008-07` como concluida.

## Validacao Final

- Confirmar ausencia de auto start ao abrir a tela.
