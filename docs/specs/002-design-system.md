# Spec 002 — Design system e identidade visual

## Objetivo

Criar fundação visual compartilhada do Champion Companion, com identidade própria em Material 3, temas claro e escuro, suporte opcional a Material You e componentes reutilizáveis. A fase não implementa BLE, Health Connect, persistência, notificações ativas ou sincronização real.

## Documentos autoritativos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`
- `docs/specs/_inputs/phase-02-design-direction.md`

## Decisões

- Usar somente componentes e APIs estáveis de `androidx.compose.material3`; não usar Material 2.
- Criar identidade nova azul-petróleo, com seed primário `#006782`; não copiar identidade, paleta, tokens ou componentes do MotionLog.
- Criar `LightColorScheme` e `DarkColorScheme` próprios, completos e separados. Superfícies escuras não usam preto puro como padrão.
- `ChampionCompanionTheme` segue `isSystemInDarkTheme()` e expõe `useDynamicColors`, com padrão `true`. Em Android 12 ou superior, cores dinâmicas usam `dynamicLightColorScheme` ou `dynamicDarkColorScheme`; esquemas próprios permanecem fallback obrigatório.
- Usar fonte padrão do sistema e escalas Material 3. Não adicionar fonte externa.
- Centralizar spacing `4/8/12/16/24/32/40/48dp`, shapes `8/12/16/24dp` e elevação tonal no design system.
- Estados visuais sempre incluem rótulo, ícone e semântica. Cor não é sinal único.
- Criar marca vetorial mínima: ícone adaptativo, splash Android 12+ e ícone monocromático de notificação. Não criar ilustrações bitmap nem identidade complexa.
- Não criar tela de preferência nem persistir preferência de tema nesta fase.

## Contratos públicos

Criar em `core.designsystem`:

- `ChampionCompanionTheme(darkTheme, useDynamicColors, content)`.
- `ChampionSpacing`, typography, shapes e tokens de elevação.
- `LocalChampionStatusColors` e pares container/conteúdo para `connected`, `disconnected`, `syncPending`, `syncSuccess`, `syncFailure`, `heartRate`, `activityRunning` e `activityPaused`.
- `ConnectionVisualState`, `SyncVisualState` e `ActivityVisualState`, independentes de modelos BLE, Health Connect e domínio futuro.
- `HealthCard`, `MetricCard`, `ConnectionStatus`, `SyncStatusBadge`, `ActivityStatusBadge`, `PrimaryActionButton`, `EmptyState`, `ErrorState`, `LoadingState`, `HealthChart`, `PermissionBanner`, `PendingReviewBanner` e `CalibrationBanner`.

Componentes recebem textos, estados visuais, slots e callbacks. Não recebem DAOs, GATT, `HealthConnectClient`, exceções internas ou modelos de domínio. `HealthChart` apenas renderiza pontos fornecidos pelo chamador; não calcula nem persiste métricas.

## Tasks

| Task | Dependência | Objetivo |
| --- | --- | --- |
| `002-00-overview` | — | Regras, ordem, checklist e rastreabilidade. |
| `002-01-audit-visual-foundation` | `002-00` | Inventariar tema e recursos gerados; registrar exclusão MotionLog. |
| `002-02-define-theme-foundations` | `002-01` | Paletas, tipografia, spacing, shapes, elevação, tema e contraste. |
| `002-03-create-semantic-state-tokens` | `002-02` | Estados visuais, tokens semânticos, texto e semântica. |
| `002-04-create-action-and-status-components` | `002-03` | Botão, progresso, conexão e badges. |
| `002-05-create-display-components` | `002-03` | Cards, métricas e gráfico. |
| `002-06-create-feedback-and-banner-components` | `002-03` | Estados feedback e banners. |
| `002-07-apply-design-system-to-app-shell` | `002-04`–`002-06` | Tema, navegação, placeholders e remoção de cores locais. |
| `002-08-create-brand-assets-and-splash` | `002-02` | Launcher, splash e ícone de notificação. |
| `002-09-validate-design-system` | todas | Checks, dispositivo e consolidação final. |

As tasks `002-04`, `002-05` e `002-06` podem executar em paralelo. Cada task altera somente seu próprio status; `002-09` consolida o checklist de `002-00`.

## Validação

- Testes unitários para seleção claro/escuro/dinâmico/fallback, contraste e mapeamento de estados visuais.
- Testes Compose/instrumentados para renderização, estados loading/vazio/sucesso/erro, fonte ampliada, semântica e alvo de toque.
- Previews de todos componentes nos temas claro e escuro; estados e fonte ampliada quando aplicável.
- Validar manualmente no Galaxy S22: tema do sistema, splash, launcher, navegação, contraste e TalkBack básico.
- Executar `./gradlew lint testDebugUnitTest assembleDebug assembleRelease` e instrumentados no dispositivo conectado.
- Inspecionar fontes: sem `Color.White`, `Color.Black`, hex em features ou Material 2; sem tokens, cores ou componentes MotionLog.
- Preservar alterações locais pré-existentes no roadmap e no input da fase; não alterar roadmap, ADRs, documentação concluída, MotionLog, commits ou push.

## Matriz de rastreabilidade

| Requisito | Tasks | Artefatos |
| --- | --- | --- |
| Tema Material 3 centralizado | `002-02`, `002-07` | Tema, spec |
| Azul-petróleo próprio; sem MotionLog | `002-01`, `002-02` | Auditoria, paletas documentadas |
| Claro, escuro, dinâmico e fallback | `002-02`, `002-09` | Tema, testes |
| Tokens semânticos de domínio | `002-03` | Contratos e tokens |
| Cards, ações, progresso, gráfico e chips | `002-04`, `002-05` | Biblioteca Compose |
| Estados BLE, Health Connect e atividade | `002-03`, `002-04` | Badges e estados |
| Estados vazio, erro e loading | `002-06` | Biblioteca Compose |
| Banners de permissão, revisão e calibração | `002-06` | Biblioteca Compose |
| Previews, contraste e acessibilidade | `002-02`–`002-09` | Previews, testes, evidências |
| Shell sem cores locais | `002-07` | Navegação e placeholders |
| Launcher, splash e notificação | `002-08`, `002-09` | Recursos Android |
| Limites de escopo e validação final | `002-00`, `002-09` | Overview e checklist final |
