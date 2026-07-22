# Task 006-08 — Criar tela utilizável de dispositivo

## Status

Concluida em 2026-07-22.

## Objetivo

Substituir placeholder release por tela utilizável de gerenciamento de dispositivo, mantendo PoC técnica debug acessível separadamente.

## Contexto

`DeviceScreen` release é placeholder. A tela final deve usar ViewModel Hilt e não construir scanner, GATT client ou connection manager. UI deve mostrar estados principais, dispositivo lembrado, lista de candidatos, ações, erros e informações seguras.

## Documentos Obrigatórios

- `AGENTS.md`
- `docs/specs/006-device-management.md`
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-05-create-device-management-viewmodel.md`
- `docs/tasks/006-device-management/006-06-implement-discovery-and-device-selection.md`
- `docs/tasks/006-device-management/006-07-implement-saved-device-connection-and-retry.md`
- `docs/tasks/006-device-management/006-07a-implement-controlled-auto-connect-for-associated-device.md`
- `docs/specs/002-design-system.md`

## Dependências

- `006-05`
- `006-06`
- `006-07`
- `006-07A`

## Entradas

- ViewModel e UI state.
- Componentes do design system existentes.
- PoC debug atual em `DeviceScreen.kt`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
- novos arquivos em `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/` com prefixo `DeviceManagement`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt` somente se rota de debug separada for indispensável e mínima
- previews/testes pequenos se já houver padrão local
- `docs/tasks/006-device-management/006-00-overview.md`
- `docs/tasks/006-device-management/006-08-create-usable-device-screen.md`

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- `core/bluetooth/android/**`
- Gradle, ADRs, roadmap, baseline e fases concluídas.

## Escopo

- Renderizar estado sem dispositivo.
- Renderizar permissões e Bluetooth desligado.
- Renderizar scan, cancelamento e resultados.
- Renderizar dispositivo associado com nome, modelo, identificador seguro, conexão, última conexão e última sincronização.
- Renderizar conectar/desconectar/retry/esquecer/diagnóstico/export.
- Garantir acessibilidade básica.
- Manter PoC debug separada e não como tela principal.

## Fora Do Escopo

- Novos fluxos BLE.
- D1/D3/D4/Health Connect.
- Campanha ampla Compose tests.

## Passos

1. Ler documentos obrigatórios.
2. Remover placeholder release.
3. Conectar tela final a `hiltViewModel`.
4. Migrar construção manual BLE para caminho debug separado ou removê-la da tela principal.
5. Criar cards/sections conforme design system.
6. Implementar diálogos/ações básicas, inclusive confirmação para substituição/esquecimento se já disponível.
7. Garantir identificador seguro.
8. Rodar validação mínima.
9. Atualizar overview.

## Critérios De Aceite

- Release não mostra placeholder.
- Nenhum Composable constrói scanner, client, resolver ou manager.
- Tela não acessa Room/DAO/`BluetoothGatt`.
- UI representa estados obrigatórios.
- Última sincronização não é inventada.
- PoC debug permanece separada quando aplicável.

## Validação Mínima

- `./gradlew assembleDebug`

## Evidência Manual Esperada

- Screenshot deve ser preparado no PR por mudança visual.
- Resumo da hierarquia da tela.
- Confirmação de que tela final usa ViewModel.

## Atualização Do Overview

- Marcar `006-08` como concluída.
- Registrar necessidade de screenshot no PR.

## Validação Final

- Registrar comando e resultado.

## Execucao

- `DeviceScreen` deixou de mostrar placeholder release e passou a renderizar tela funcional de gerenciamento usando `DeviceManagementViewModel` obtida do owner Hilt da Activity.
- Tela principal agora observa `DeviceManagementUiState`, dispara `Load` por entrada visivel, refresca prerequisitos no `ON_RESUME` e coleta eventos para permissoes, Bluetooth e confirmacoes.
- UI cobre estados de carregamento, permissoes, Bluetooth desligado, scan, resultados, dispositivo associado, conexao, erros e acoes principais com mensagens seguras.
- Card do dispositivo associado mostra nome, modelo, identificador mascarado, estado de conexao, ultima conexao e ultima sincronizacao somente quando existente; quando nao existe importacao, tela mostra fallback explicito sem inventar timestamp.
- Confirmacoes de esquecer e de possivel substituicao da associacao foram adicionadas na UI antes das intents correspondentes.
- PoC tecnica debug saiu do fluxo principal e foi movida para `DeviceManagementDebugPocScreen`, acessivel apenas por acao secundaria em build debug.
- Diagnostico e exportacao permanecem presentes como acoes da tela, com aviso controlado de integracao reservada para `006-10`.

## Evidencia Final

- Hierarquia principal da tela: resumo da conexao, banners de prerequisito, erro seguro, card do dispositivo associado, secao de candidatos/scan, secao de acoes e entrada separada para PoC debug.
- Confirmado: tela final usa `DeviceManagementViewModel` como fronteira unica de estado e intents; nenhum Composable constroi scanner, client, resolver ou manager BLE.
- Screenshot deve ser preparada no PR por se tratar de mudanca visual.

## Validacao Final

Comando executado:

```bash
./gradlew assembleDebug
```

Resultado final:

- primeira execucao falhou por imports Compose ViewModel ausentes no projeto e inferencia de tipo em helper de permissoes; wiring foi ajustado para `ViewModelProvider(activity)` com factory Hilt da Activity e helper corrigido;
- segunda execucao concluiu com `BUILD SUCCESSFUL`.
