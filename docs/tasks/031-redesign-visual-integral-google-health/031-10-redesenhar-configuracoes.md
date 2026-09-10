# 031-10 — Redesenhar Configurações

- **Ticker:** `031`
- **Número:** `10`
- **Status:** completed

## Objetivo

Aplicar composição da referência a Configurações, mantendo Health Connect
write-only, permissões, fila e ações explícitas.

## Requisitos e aceite

- Configurações continua acessível pela ação `Configurações` do Perfil, com
  retorno ao Perfil, fora da barra inferior.
- Disponibilidade, provider, toggle, permissões, gerenciar acesso e sync now
  mantêm callbacks e estado.
- Fila local continua observável sem leitura remota do Health Connect.
- Textos de segurança, erro e indisponibilidade permanecem claros.

## Escopo

Inclui layout, agrupamento, filtros visuais, cards, headers e estados. Exclui
leitura Health Connect, novos tipos de record, permissões, sync automático ou
mudança de manifesto.

## Dependências

- `031-04` a `031-09` e ADR-012.
- Contratos de Settings e fila persistida.

## Arquivos e símbolos prováveis

- `feature/settings/SettingsScreen.kt`.
- `SettingsViewModel.kt`, `SettingsFeatureContract.kt`.

## Passos

1. Confirmar entrada contextual definida no shell.
2. Reorganizar integração, permissões e fila pela hierarquia da referência.
3. Preservar launcher, lifecycle, provider update e estados fail-safe.
4. Validar ações sem disparar sync apenas para evidência.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de abertura, rolagem, retorno, fonte ampliada, landscape e
temas. Não criar ou executar testes automatizados.

## Definição de pronto

Configurações acessível por origem documentada e visualmente aprovada, sem
mudança no contrato write-only, permissões, fila ou sync; estado inicial/restaurado
do aparelho e revisão independente aprovados.

## Riscos

Reorganização pode sugerir leitura remota ou sync automático. Manter linguagem e
estados que distinguem intenção local de confirmação remota.

## Implementação atual

- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/settings/SettingsViewModel.kt`
- **Decisões:**
  - Aplicado `ChampionRedesignTheme` somente à tela de Configurações, com
    superfície rolável, espaçamentos, formas e cards da fundação visual opt-in.
  - Header passou a usar `ChampionRedesignSurfaceHeader`, com retorno contextual
    para Perfil por `onNavigateBack` e `backLabel` opcionais.
  - Integração Health Connect, permissões por capacidade, fila local, toggle,
    launcher, intents de provider/acesso, sincronização explícita, lifecycle e
    mensagens write-only/fail-safe foram preservados.
- **Desvios:** nenhum desvio funcional. O fluxo usa o launcher nativo de
  permissões para abrir acesso individual; não houve alteração em Contract,
  leitura/escrita de records, Room, DataStore ou manifest.

## Correção da ação Gerenciar acesso

- **Status:** `completed`; correção física e revisão independente concluídas.
- **Causa:** o botão usava Intent de gerenciador geral, não a tela de
  permissões individuais. A ação específica `MANAGE_HEALTH_PERMISSIONS` também
  exige privilégio de gerenciamento quando iniciada diretamente pelo app.
- **Decisão:** `Gerenciar acesso` reutiliza
  `PermissionController.createRequestPermissionResultContract()` e lança o
  conjunto completo de permissões relevantes do app. Health Connect exibe a
  tela individual por capacidade, permitindo revisar concessões sem leitura
  remota, privilégio especial ou alteração do fluxo write-only.
- `requestedPermissions()` continua limitado ao conjunto pendente para o botão
  `Conectar`; `allRequestedPermissions()` fornece conjunto completo somente
  para `Gerenciar acesso`, permitindo também revisar permissões já concedidas.

## Comandos executados e resultados

- `git diff --check` — passou.
- `./gradlew app:compileDebugKotlin --no-daemon` — passou.
- `./gradlew lint assembleDebug assembleRelease --no-daemon` — passou na segunda
  execução.
- Primeira execução de `./gradlew lint assembleDebug assembleRelease --no-daemon`
  excedeu o timeout operacional após concluir os builds; rerun sequencial passou.
- `adb devices -l` — identificou `SM-S901E`; `./gradlew installDebug --no-daemon`
  — passou.
- Smoke ADB sanitizado — passou em Perfil → Configurações: header exibiu
  `Configurações`, descrição write-only, ação `Voltar para Perfil`, integração,
  provider, permissões e ações existentes; rolagem exibiu `Fila local` e grupos
  de permissões. Retorno pelo sistema voltou ao Perfil sem crash e o processo
  permaneceu ativo. Nenhuma permissão, sync, leitura remota ou outra operação de
  dados foi disparada.
- Fonte ampliada `1.3x` e tema escuro foram exercitados e restaurados para
  `1.0x`/modo automático. Tentativa de rotação landscape não alterou a
  orientação, que permaneceu portrait conforme a configuração efetiva do
  aparelho; não houve alteração persistida além da restauração confirmada.
- Testes automatizados não foram criados ou executados, conforme `AGENTS.md`.
- Correção pós-revisão: `syncStatusMessage` recebeu `LiveRegionMode.Polite`,
  mantendo o feedback de sucesso, erro e indisponibilidade anunciável sem alterar
  contratos ou estado.
- Validação pós-correção: `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon` — todos
  passaram. Smoke ADB repetido confirmou a entrada, o header `Voltar para
  Perfil`, estados visíveis e retorno ao Perfil; processo ativo sem crash.
- Validação da correção `Gerenciar acesso`: `git diff --check`; `./gradlew
  app:compileDebugKotlin app:lintDebug --no-daemon`; `./gradlew installDebug
  --no-daemon` — passaram. Toque físico abriu
  `com.google.android.healthconnect.controller/com.android.healthconnect.controller.permissions.request.PermissionsActivity`;
  a tela exibiu opções individuais e retorno pelo sistema voltou à
  `MainActivity`. Nenhuma permissão foi concedida/revogada e nenhuma
  sincronização foi iniciada.

## Revisão independente

Revisão independente aprovou a correção com ressalvas não bloqueantes sobre
TalkBack, landscape, screenshot sanitizada e estado vazio transitório.

## Riscos residuais

- Instalação/smoke ADB, rolagem física, fonte ampliada e tema foram exercitados;
  landscape permaneceu N/A por orientação efetiva portrait e TalkBack não foi
  exercitado.
- A referência visual foi aplicada por componentes próprios existentes; nenhuma
  evidência física ou screenshot foi versionada.
- **Estado final:** subtarefa concluída após correção e revisão independente;
  nenhuma próxima subtarefa foi iniciada.
