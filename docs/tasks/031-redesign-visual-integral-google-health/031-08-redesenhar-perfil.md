# 031-08 — Redesenhar Perfil

- **Ticker:** `031`
- **Número:** `08`
- **Status:** completed

## Objetivo

Aplicar composição da referência ao Perfil, preservando edição de métricas
pessoais, calibração e navegação para atividade relacionada.

## Requisitos e aceite

- Altura, meta, comprimento de passo e estados de calibração permanecem.
- Diálogos, validação, cancelamento, remoção e correção vinculada preservados.
- Dados ausentes são omitidos ou exibidos como ausência segura.
- Header e ação de retorno seguem shell aprovado.

## Escopo

Inclui layout, cards, agrupamentos, ícones, textos e estados visuais. Exclui novo
perfil, nova métrica, persistência ou mudança de algoritmo.

## Dependências

- `031-04`, `031-05`, `031-06` e `031-07`, além da fundação visual.
- Spec 023 e contratos de Perfil.

## Arquivos e símbolos prováveis

- `feature/profile/ProfileScreen.kt`.
- `ProfileViewModel.kt`, `ProfileFeatureContract.kt`.

## Passos

1. Mapear seções da referência para dados de Perfil existentes.
2. Reordenar composição e manter editores/modais.
3. Preservar precisão, provenance e abertura de atividade.
4. Validar cancelamento e retorno sem persistência indevida.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de leitura, diálogos, retorno, fonte ampliada, landscape e
temas. Não criar ou executar testes automatizados.

## Definição de pronto

Perfil aprovado visualmente, sem regressão em edições/calibração, com evidência
sanitizada, estado inicial/restaurado do aparelho e revisão independente
aprovados.

## Riscos

Layout compacto pode cortar precisão de campos ou ocultar ação destrutiva.
Preservar clareza, validação e confirmação.

## Execução e evidências

- **Status da execução:** concluída; revisão independente aprovada.
- **Arquivos alterados:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt` e `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`.
- **Decisões:** `ChampionRedesignTheme`, tokens opt-in, `ChampionRedesignSurfaceHeader`, ações acessíveis e cards do redesign foram aplicados somente ao Perfil. Header recebeu retorno contextual e acesso por ícone a Configurações. Diálogos de perfis aprendidos, histórico de calibrações e confirmação de remoção permanecem sob o tema visual redesign.
- **Desvios:** nenhum desvio funcional. ViewModel, contrato, persistência, Health Connect, BLE, algoritmos, callbacks de edição/calibração e abertura de atividade foram preservados.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin --no-daemon`; `./gradlew lint assembleDebug assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`; `adb devices -l`.
- **Resultados/evidências:** todos os comandos passaram. Smoke ADB sanitizado no `SM-S901E` confirmou Perfil, retorno `Voltar para Home`, Configurações, campos de altura/meta/comprimento, diálogos de perfis aprendidos e histórico de calibrações, retorno à Home, fonte `1.3x`, landscape e temas claro/escuro. Processo permaneceu ativo sem crash. Configurações do aparelho foram restauradas; nenhuma ação de dados foi iniciada.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md`.
- **Revisão independente:** aprovada após correção que manteve os diálogos sob `ChampionRedesignTheme`; sem bloqueadores.
- **Riscos residuais:** TalkBack não foi exercitado explicitamente. Fidelidade à referência física do Perfil permanece inferida pela composição aprovada e pela fundação visual; não foram versionados screenshots ou dados não sanitizados.
