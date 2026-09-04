# 031-05 — Redesenhar Home

- **Ticker:** `031`
- **Número:** `05`
- **Status:** completed

## Objetivo

Aplicar composição da referência à Home, priorizando resumos e mantendo todas as
ações e estados atuais.

## Requisitos e aceite

- Resumo diário, passos/meta, BPM, distância, calorias e pendências aparecem
  somente quando dados existem.
- Refresh explícito, status do relógio e estados seguros permanecem.
- Header fornece acesso a Dispositivo e Perfil.
- Resumos encaminham a Saúde, Exercícios e calendário conforme matriz do shell.
- Registrar nomes dos componentes compartilhados usados também em `031-06`;
  promoção exige uso real nas duas superfícies, nunca apenas preview.

## Escopo

Inclui hierarquia, cards, listas, textos, ícones, espaçamento e estados visuais
de Home. Exclui novas métricas, coleta, sincronização e mudança de cálculo.

## Dependências

- `031-04` e fundação visual.
- `HomeUiState`, `HomeViewModel` e contratos existentes.

## Arquivos e símbolos prováveis

- `feature/home/HomeScreen.kt`.
- `HomeFeatureContract.kt`, `HomeViewModel.kt`.

## Passos

1. Mapear conteúdo atual para blocos da referência.
2. Reordenar composição sem alterar callbacks ou lifecycle.
3. Omitir blocos sem equivalente e preservar ausência segura.
4. Validar atalhos e refresh sem disparar operação artificial.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB sanitizado de estados observáveis, rolagem, atalhos, fonte ampliada,
landscape e temas. Não criar ou executar testes automatizados.

## Definição de pronto

Home visualmente aprovada contra baseline, com dados/estados/callbacks
equivalentes, evidência sanitizada, estado inicial/restaurado do aparelho e
revisão independente aprovados.

## Riscos

Redução de conteúdo pode esconder pendências. Conferir todos os estados do
contrato antes de omitir qualquer seção.

## Execução e evidências

- **Status da execução:** correção visual concluída após nova revisão
  independente aprovada. Implementação sem alteração de dados, callbacks,
  layout estrutural, navegação, estados ou contratos.
- **Arquivos alterados:** `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt` e
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`, além de
  `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionRedesignComponents.kt`.
- **Decisões:** a Home reproduz a estrutura sanitizada observada: card maior de
  `Passos` à esquerda e três linhas compactas à direita, na ordem `Exercícios`,
  `Distância` e `Freq. cardíaca`. Exercícios é atalho real. Distância e
  frequência cardíaca exibem zero quando não há dado; a mesma composição fica
  estável entre estados. Os cards clicáveis não exibem setas. O card de Passos usa preenchimento
  proporcional à meta da esquerda para a direita, sem barra de progresso, texto
  `passos hoje`, percentual visível ou fonte de dados. BPM não aparece no card maior. A Home não exibe
  link para Calendário; a rota continua acessível pelos contextos previstos em
  Exercícios e demais superfícies. Passos, distância e FC encaminham aos
  detalhes existentes. O único card inferior permanece sendo o de revisão
  quando há revisão pendente. Altura do card de Passos corresponde à soma das alturas
  das linhas laterais e dos espaçamentos entre elas.
  `ChampionRedesignTheme`, `ChampionRedesignSurfaceHeader`,
  `ChampionRedesignHeaderAction` e `ChampionRedesignSummaryCard` continuam
  usados em produção na Home e registrados para uso real também em `031-06`.
- **Correção de comportamento visual:** o aviso transitório de sincronização do
  Health Connect foi removido da Home; o feedback de refresh local e seus erros
  permanecem. Isso elimina a mensagem intermitente após o refresh sem alterar a
  fila ou o sincronismo write-only, que continuam observáveis em suas telas.
- **Desvios:** por solicitação explícita, calorias foi substituída por FC, valores
  ausentes de passos/distância/FC usam zero visual e o texto de fonte de dados
  foi removido. Contagem de exercícios realizados na
  semana e meta fixa `4` ficaram bloqueadas: não há campo/contrato existente e
  a subtarefa exclui nova métrica/cálculo; não foi exibido dado sintético.
- **Ajuste de estado:** por solicitação explícita, `Loading`/`Empty` mantêm a
  composição da Home com passos, distância e frequência cardíaca em zero visual;
  erros de dispositivo, permissões, Bluetooth e refresh continuam expondo estado
  seguro acionável quando aplicável. Card de revisão só aparece quando há
  pendência de revisão; calibração e atividades recuperáveis permanecem em
  Exercícios, sem duplicar card na Home.
- **Comandos executados:** `git diff --check`; `./gradlew app:compileDebugKotlin`;
  `./gradlew lint assembleDebug assembleRelease`; `./gradlew installDebug`;
  `adb shell am force-stop br.com.mwconsulting.championcompanion.dev`;
  `adb shell monkey -p br.com.mwconsulting.championcompanion.dev 1`;
  `adb shell uiautomator dump ...`; gesto de refresh, rolagem, ações de
  Dispositivo/Perfil e retorno; smoke com fonte ampliada, portrait/landscape e
  temas claro/escuro.
- **Resultados/evidências:** compilação, lint, builds debug/release e instalação
  debug passaram. No `SM-S901E`, a variante correta `br.com.mwconsulting.championcompanion.dev`
  expôs `Passos`, `Exercícios`,
  `Distância`, `Freq. cardíaca` e barra inferior `Hoje`/`Saúde`; dump estrutural temporário
  confirmou um contêiner rolável necessário ao pull-to-refresh e foi apagado.
  O gesto de refresh não exibiu mensagem transitória de Health Connect após a
  correção; o estado com dados expôs alvos clicáveis para métricas e revisão.
  As bounds confirmaram card maior alinhado à soma das três linhas laterais e
  espaçamentos. Fonte ampliada,
  landscape e temas foram exercitados. Estado restaurado para fonte 1.0,
  portrait, rotação automática desativada e tema escuro; Activity reiniciada.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md`.
- **Revisão independente:** aprovada após correção de zero visual sem resumo e
  altura responsiva das linhas em escala de fonte ampliada.
- **Riscos residuais:** smoke ADB não substitui validação completa com TalkBack;
  foco e contraste precisam de revisão consolidada em `031-11`. Contagem
  semanal permanece fora do escopo e bloqueada por ausência de contrato. O
  fallback sem resumo não foi forçado no aparelho para não apagar dados locais;
  foi verificado estaticamente.

- **Correção de paleta:** toda composição da Home usa somente a família azul
  própria do Champion (`primaryContainer`/`onPrimaryContainer`), com suporte
  explícito às variantes clara e escura, sem cores semânticas externas. O card
  de Passos mantém a área concluída no azul-base e aplica escurecimento sutil
  somente no trecho não concluído, proporcionalmente à meta. `ChampionRedesignSummaryCard`
  permanece stateless e recebeu apenas cores opcionais compatíveis.
- **Validação da correção:** `git diff --check`,
  `./gradlew app:compileDebugKotlin` e `./gradlew app:lintDebug` passaram.
- **Validação final:** `./gradlew lint assembleDebug assembleRelease` e
  `./gradlew installDebug` passaram. Smoke ADB na variante
  `br.com.mwconsulting.championcompanion.dev` confirmou composição azul, labels,
  ausência de labels antigos e ausência de Health Connect. Refresh também expôs
  erro transitório de conexão de forma visível e acionável.
- **Revisão independente:** aprovada; nenhum bloqueador ou achado importante.
- **Risco residual atualizado:** TalkBack completo permanece não validado; revisão
  consolidada de acessibilidade segue em `031-11`.
