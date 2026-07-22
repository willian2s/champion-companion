# Task 007-07 — Criar UI Pull-To-Refresh Da Home

## Status

Pendente.

## Objetivo

Substituir placeholder da Home por UI rolável com pull-to-refresh, indicadores corretos e resumo diário.

## Contexto

A Home atual exibe `Painel em preparação`. Esta task entrega superfície visual principal sem criar sync automático.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- `007-02`
- `007-06`

## Dependências

- `007-02`
- `007-06`

## Entradas

- `HomeViewModel`
- `HomeUiState`
- Design system existente.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeViewModel.kt`
- docs desta fase

## Arquivos Proibidos

- `DeviceScreen.kt`
- `DeviceManagementViewModel.kt`
- `RoomRepositories.kt`
- DAOs/database
- `AndroidManifest.xml`
- Gradle files

## Escopo

- Conteúdo rolável.
- Pull-to-refresh.
- Indicador do gesto.
- Barra linear indeterminada.
- Manutenção do resumo anterior.
- Estados vazio, pronto, refreshing e erro.
- Instrução de uso.
- Retry secundário somente em erro.
- Acessibilidade.
- Remoção do placeholder.
- Ausência de botão permanente de sync.

## Fora Do Escopo

- Persistência.
- Coordinator BLE.
- Testes Compose extensivos.
- Captura de screenshot nesta task salvo exigência de PR visual posterior.

## Passos

1. Injetar/coletar `HomeViewModel`.
2. Criar layout rolável que aceita pull em vazio.
3. Exibir cabeçalho com data e relógio.
4. Exibir barra indeterminada quando `isRefreshing`.
5. Exibir passos, meta, progresso, estimativas, origem e última atualização.
6. Exibir erro seguro e `Tentar novamente` só em erro.
7. Adicionar alternativa acessível ao gesto.
8. Atualizar overview.

## Critérios De Aceite

- Placeholder removido.
- Pull-to-refresh é ação principal.
- Sem botão permanente de sincronização no normal.
- Dados anteriores permanecem durante refresh.
- Barra é indeterminada.

## Validação Mínima

```bash
./gradlew assembleDebug
```

## Evidência Manual

- Registrar inspeção visual local ou screenshot para PR se necessário.

## Atualização Do Overview

- Marcar `007-07` como concluída.

## Validação Final

- Confirmar acessibilidade básica e ausência de porcentagem fictícia.
