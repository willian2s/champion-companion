# 029-08 — Criar tela Evolução e superfície de recordes

- **Ticker:** `029`
- **Número:** `08`
- **Status:** `completed`
- **Depends on:** `029-07`

## Objetivo

Criar a superfície visual de evolução esportiva usando os read models e gráficos compartilhados já existentes.

## Escopo

A tela deve apresentar:

- seletor 7/30/90 dias;
- filtro por modalidade quando aplicável;
- resumo de volume/consistência;
- gráficos de tendências elegíveis;
- comparação com período anterior;
- seção de recordes pessoais atuais com data da atividade recordista;
- navegação para a atividade recordista quando houver `activityId` válido.

## UX

Priorizar leitura curta antes do detalhamento:

```text
Evolução
[7 dias] [30 dias] [90 dias]

Resumo do período
Tendências
Comparação
Recordes pessoais
```

Os recordes exibem fatos e unidade, sem linguagem de recomendação fisiológica.

## Gráficos

- usar exclusivamente `core/designsystem/chart`;
- marker/tooltip seguem a Spec 028;
- gaps permanecem gaps;
- scroll/zoom somente quando densidade justificar;
- light/dark e fonte ampliada devem permanecer utilizáveis.

## Navegação

Usar a decisão registrada em `029-01`, preservando Navigation 3 e back stack manual. Não reativar destinos legados de treino phone.

## Critérios de aceite

1. Usuário consegue alternar período e modalidade.
2. Cards e gráficos reconciliam com o read model.
3. Recordes mostram valor, tipo e data real da atividade.
4. Atividade recordista pode ser aberta quando elegível.
5. Tela preserva estados loading/empty/failed.
6. Nenhuma feature importa Vico diretamente.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Validar fisicamente tema claro/escuro, fonte ampliada, rolagem, filtros, tooltips e estados disponíveis. Registrar screenshot sanitizado.

## Registro de execução

- **Status:** `completed`
- **Arquivos alterados:** `EvolutionFeatureContract.kt`, `EvolutionViewModel.kt`,
  `EvolutionScreen.kt`, `AppDestination.kt`, `ChampionNavigation.kt` e
  `ExercisesScreen.kt`.
- **Decisões e desvios:** tela integrada como subárvore de Exercícios, usando
  Navigation 3 e back stack manual; read model permanece fonte dos cards,
  gráficos, comparação e recordes; recordes elegíveis navegam para detalhe de
  atividade; gráficos usam somente `core/designsystem/chart`. Eixos recebem
  padding e séries com mais de seis pontos habilitam rolagem horizontal para
  manter pontos e labels extremos acessíveis, sem alterar dados; nenhum desvio
  de escopo. Header usa mesma cor de destaque do header de Exercícios em ambos
  os temas.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`; `./gradlew clean installDebug`; `./gradlew installDebug`;
  `./gradlew :app:assembleDebug --rerun-tasks`; `adb devices`.
- **Resultados e evidências:** lint, debug e release passaram. Validação física
  em tema escuro e fonte ampliada confirmou navegação Exercícios → Evolução,
  seletor de período, filtros por modalidade, rolagem e estado failed. Em 30
  dias, dados locais faltantes permanecem fail-closed com “Evolução
  indisponível”; ao selecionar 7 dias, estado reconciliado renderiza resumo
  com 7 atividades e 12,6 km. Após correção, pontos e labels finais foram
  acessados por rolagem horizontal, incluindo `28/08`, sem crash. Screenshot
  sanitizado: `evidence/029-08-chart-scroll.png`.
- **Ajuste visual posterior:** header alinhado ao tom de Exercícios e validado
  fisicamente; evidência em `evidence/029-08-header-aligned.png`.
- **Correção posterior:** comparação negativa passa a exibir sinal único, sem
  duplicar o caractere de menos.
- **Riscos residuais:** validação física de tema claro, tooltip e abertura de
  atividade recordista não foi executada nesta sessão; coberta por contratos e
  integração compilada, mas requer conferência manual posterior.
