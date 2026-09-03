# 031-04 — Reorganizar shell e navegação

- **Ticker:** `031`
- **Número:** `04`
- **Status:** completed

## Objetivo

Reduzir barra inferior para Home e Saúde e reposicionar entradas secundárias em
contextos equivalentes à referência, preservando rotas e back stack.

## Requisitos e aceite

- Barra mostra somente Home e Saúde.
- Seleção, deduplicação, saver, animações, retorno e guards continuam corretos.
- Header da Home abre Dispositivo e Perfil.
- Home/resumo `Atividades` abre Exercícios; Home/resumo `Calendário de exercícios`
  abre Calendário; Saúde/resumo `Atividades` abre Exercícios; Saúde/card de
  métrica abre o detalhe correspondente.
- Exercícios/ação `Calendário` abre Calendário; Exercícios/item abre detalhe de
  atividade; Perfil/ação `Configurações` abre Configurações e seu retorno volta
  ao Perfil.
- `AppDestination.Calendar` não é apagado; vira destino contextual de Exercícios.

## Escopo

Inclui apresentação do shell, itens de navegação, insets e roteamento contextual.
Exclui migração para `NavController`, Room, BLE, ViewModels e fluxo phone.

## Dependências

- `031-03`.
- ADR-005, ADR-009, `AppDestination` e baseline de `031-01`.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`.
- `.../core/navigation/AppDestination.kt`.
- Headers e callbacks de Home, Saúde, Perfil e Dispositivo.

## Passos

1. Modelar matriz de origem/destino e retorno contextual.
2. Remover somente itens da barra inferior; preservar destinos serializáveis.
3. Ligar ações nomeadas do header, resumos de Home/Saúde, calendário de
   Exercícios e ação `Perfil → Configurações`.
4. Testar restauração, back, bloqueios de saída e acessibilidade.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke ADB de todos os destinos, retorno, fonte ampliada, landscape e temas;
evidências sanitizadas. Não criar ou executar testes automatizados.

## Definição de pronto

Somente dois itens selecionáveis na barra, matriz completa de destinos sem órfãos,
saver/back stack preservados, estado inicial/restaurado do aparelho e revisão
independente aprovada.

## Riscos

Remover item de topo pode quebrar entrada indireta ou restauração. Validar cada
origem, inclusive detalhes e diálogos de saída.

## Execução e evidências

- **Status da execução:** concluída.
- **Arquivos alterados:** `ChampionNavigation.kt`, `CalendarScreen.kt`,
  `CalendarDayScreen.kt`, `HomeScreen.kt`, `HealthScreen.kt`,
  `ExercisesScreen.kt` e `ProfileScreen.kt`.
- **Decisões:** a barra inferior mantém somente Home e Saúde; Calendário,
  Exercícios, Dispositivo, Perfil e Configurações permanecem destinos
  serializáveis e entram por ações contextuais. O retorno usa predecessor
  imediato da back stack, com animação reversa ao revelar uma área primária.
  A entrada em Exercícios limpa subtree phone bloqueado sem reativar fluxo phone.
  PoC debug-only foi preservada.
- **Desvios:** nenhum desvio funcional. Ajustes de 48 dp foram aplicados às
  ações adicionadas e às ações explícitas de Calendário/detalhe diário após
  revisão independente.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`; `./gradlew installDebug`.
- **Resultados/evidências:** todos os comandos passaram. Smoke ADB sanitizado
  no `SM-S901E` confirmou barra de duas áreas; header Home → Dispositivo/Perfil;
  Home e Saúde → Exercícios/Calendário; cards de métricas; Exercícios →
  Calendário/detalhe; Perfil → Configurações → Perfil; retorno contextual;
  fonte 1.3, landscape, tema escuro e restauração para fonte 1.0/portrait.
  Configurações do aparelho foram restauradas. App iniciou após instalação
  final.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md`.
- **Revisão independente:** aprovada após correção de labels contextuais,
  alvos de toque e limpeza de subtree phone.
- **Riscos residuais:** smoke ADB não substitui validação com TalkBack em todos
  os caminhos; sem evidência de regressão funcional nos comandos e fluxos
  observados. Subtarefas seguintes permanecem pendentes.
