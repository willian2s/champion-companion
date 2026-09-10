# 031-09 — Redesenhar Dispositivo

- **Ticker:** `031`
- **Número:** `09`
- **Status:** completed

## Objetivo

Aplicar composição da referência à gestão do Champion C001 sem alterar conexão,
permissões, diagnóstico ou isolamento da PoC.

## Requisitos e aceite

- Associação, scan, conexão, disconnect, retry, esquecer e substituir preservados.
- Permissões, erros, diálogos e diagnósticos permanecem seguros.
- Não adicionar nem mover CTA/Intent da PoC MapLibre para Dispositivo; a única
  affordance manual da PoC MapLibre continua na página Exercícios, debug-only,
  fora de release. Diagnóstico BLE existente permanece apenas se já fizer parte
  do contrato da tela, sem ser confundido com a PoC de mapa.
- Header e status de dispositivo seguem entrada da Home.

## Escopo

Inclui layout, hierarquia, cards, ações e estados visuais. Exclui protocolo BLE,
GPS, rota, mapa, persistência e alteração de `PhoneWorkoutPolicy`.

## Dependências

- `031-04` a `031-08`, além da fundação visual.
- Specs 006, 026 e ADR-024.

## Arquivos e símbolos prováveis

- `feature/device/DeviceScreen.kt`.
- `DeviceManagementViewModel.kt`, contratos e estados.
- Providers/Activity da PoC debug, sem alteração funcional.

## Passos

1. Mapear estados visuais para composição aprovada.
2. Reorganizar cards sem perder eventos one-shot ou launchers.
3. Manter diagnóstico sanitizado e CTA por variante.
4. Validar operações não destrutivas e cancelamentos.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de abertura, rolagem, permissões disponíveis, retorno,
fonte ampliada, landscape e temas. Não iniciar scan/conexão apenas para captura.

## Definição de pronto

Dispositivo visualmente aprovado, estados assíncronos preservados, nenhum CTA da
PoC MapLibre exposto na tela ou release, estado inicial/restaurado do aparelho e
revisão independente aprovados.

## Riscos

Compactação pode esconder erro ou ação de recuperação. Manter estados textuais e
alvos acessíveis.

## Implementação atual

- **Arquivos alterados:**
  - `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`
  - `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- **Decisões:**
  - Aplicado `ChampionRedesignTheme` somente à tela de Dispositivo, com header,
    espaçamentos e cards opt-in reutilizados da fundação visual.
  - Header passou a exibir status do relógio e retorno contextual para Home,
    Saúde ou predecessor salvo, sem alterar back stack manual.
  - Estados de conexão, operação ativa, próxima ação, associação, busca,
    diagnóstico e recuperação continuam textuais e acessíveis.
  - Metadados persistidos de associação, conexão e sincronização continuam sendo
    exibidos sem criação de valores sintéticos.
  - Affordance existente foi identificada explicitamente como `PoC BLE técnica`
    e continua somente em debug; nenhum CTA, `Intent` ou arquivo da PoC MapLibre
    foi adicionado ou movido.
- **Desvios:** nenhum desvio funcional. Operações BLE não foram iniciadas apenas
  para gerar evidência; ao entrar com associação persistida, o auto-connect
  controlado existente pôde apresentar estados assíncronos reais.

## Comandos executados e resultados

- `git diff --check` — passou.
- `./gradlew app:compileDebugKotlin app:lintDebug` — passou.
- `./gradlew lint assembleDebug assembleRelease` — passou.
- `./gradlew installDebug` — passou no `SM-S901E` (Android 16).
- `adb devices -l` — aparelho autorizado identificado.
- Smoke ADB sanitizado — passou: Home → Dispositivo, header/status, retorno
  `Voltar para Home`, rolagem, estado associado, ações preservadas, fonte `1.3x`
  restaurada para `1.0x`, landscape restaurado para portrait, temas claro/escuro
  restaurados, processo ativo sem `FATAL EXCEPTION`/`AndroidRuntime`.
- Permissões já disponíveis; nenhum prompt foi aberto. Scan, conexão manual,
  disconnect, retry, esquecer, exportação e diagnóstico não foram acionados para
  captura.
- Dumps XML temporários usados somente no aparelho foram removidos. Nenhum
  screenshot ou dado bruto foi versionado.
- Testes automatizados não foram criados ou executados, conforme `AGENTS.md`.

## Evidências

- UI dump sanitizado confirmou `Dispositivo`, `Estado atual`, status assíncrono,
  ação acessível `Voltar para Home`, `Diagnóstico seguro` e `PoC BLE técnica de
  debug` após rolagem.
- Variante release compilada sem alteração no isolamento debug-only da PoC
  MapLibre.

## Revisão independente

Revisão independente solicitada após implementação. Primeira revisão não apontou
bloqueador funcional, mas pediu registro documental, smoke completo e distinção
mais explícita da PoC BLE; os dois pontos de código foram ajustados ou
comprovados. Segunda revisão independente aprovou em 2026-09-10, sem blockers;
TalkBack completo, operações BLE e screenshot versionado permanecem riscos não
bloqueantes.

## Riscos residuais

- TalkBack completo não foi exercitado fisicamente.
- Fluxos destrutivos ou operacionais BLE não foram iniciados nesta captura; seus
  contratos permanecem sem alteração, mas a prova física desses estados pertence
  à validação funcional existente.
- Comparação visual por screenshot não foi versionada para evitar retenção de
  dados reais do aparelho; evidência estrutural sanitizada foi registrada.
