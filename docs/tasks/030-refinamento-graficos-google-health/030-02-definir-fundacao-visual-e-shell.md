# 030-02 — Definir fundação visual e shell

- **Ticker:** `030`
- **Número:** `02`
- **Status:** `completed`
- **Depends on:** `030-01`

## Objetivo e resultado esperado

Transformar benchmark integral aprovado em linguagem visual compartilhada para
shell, cabeçalhos, cards, listas, estados, controles, diálogos e gráficos,
preservando destinos, callbacks e limites dos dados reais. A referência
Google Health/Fitbit orienta apresentação; não cria funcionalidades.

## Requisitos e critérios cobertos

- R1–R3 e R6; critérios 2, 3, 4, 6 e 8 da Spec 030.

## Escopo incluído

- componentes stateless em `core/designsystem`;
- navegação inferior, menu Mais, cabeçalhos e transições visuais;
- cards, listas, botões, chips, banners, feedback e diálogos;
- estados de loading, vazio, erro, indisponível, pendência e sucesso;
- composição de gráficos, períodos `D`/`S`/`M`/`3M`/`A`, unidade, marker,
  tooltip, eixos, gaps, scroll, temas e fonte ampliada;
- política opt-in de adoção pelas features.

## Escopo excluído

- novos destinos, callbacks, queries ou estados de domínio;
- Room, DataStore, BLE, Health Connect, ViewModels e read models;
- migração visual específica de Home, Saúde, Calendário ou outras features;
- troca de engine, novo Canvas de série ou paridade funcional com Google Health.

## Dependências

- `030-01` deve concluir inventário e benchmark integral.

## Arquivos e símbolos prováveis

- `core/designsystem/*`;
- `core/designsystem/chart/*`;
- `core/navigation/ChampionNavigation.kt` somente para apresentação;
- ADR-023 e ADR-027 como decisões de referência.

## Passos de implementação

1. Modelar tokens, slots e estados neutros necessários à apresentação.
2. Padronizar hierarquia, espaçamento, formas, cores e descrições acessíveis.
3. Encapsular renderer e interação de gráficos sem expor Vico às features.
4. Aplicar mudanças de shell de modo opt-in, preservando back stack e callbacks.
5. Validar fonte ampliada, orientações, temas, rolagem e estados sem dados.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar, alterar ou executar testes automatizados. Registrar evidência visual
sanitizada e declarar `N/A` para cenários indisponíveis.

## Definição de pronto

- API visual é neutra, stateless e sem Vico nas features;
- shell e primitives compartilham linguagem coerente;
- períodos sem fonte não são expostos com ação;
- acessibilidade, temas, orientação e scroll foram validados;
- lint/build, smoke e decisões estão registrados.

## Riscos e cuidados

- evitar abstração prematura e mudança global involuntária;
- não transformar estado visual em novo contrato funcional;
- preservar `HealthChart` enquanto previews/testes exigirem sua API;
- não usar cor como único sinal de estado.

## Registro de execução

### Status

`completed`; fundação visual opt-in, primitives stateless, shell compartilhado e
ajustes de apresentação do boundary de gráficos foram implementados sem alterar
contratos funcionais.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionTokens.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionFoundationComponents.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionShellComponents.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionChartContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/chart/ChampionCharts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`.

Nenhum domínio, ViewModel, read model, persistência, integração, manifesto,
dependência ou teste foi alterado.

### Decisões e desvios

- Tokens adicionais ficam em `ChampionVisualTokens` e são fornecidos por
  `LocalChampionVisualTokens`; `ChampionVisualFoundation` torna adoção explícita
  e não modifica tema raiz nem superfícies não migradas.
- `ChampionScreenHeader`, `ChampionListItem`, `ChampionFilterChip`,
  `ChampionPeriodSelector`, `ChampionStateCard` e
  `ChampionConfirmationDialog` são stateless, baseados em dados e slots, com
  descrições acessíveis e estados visuais neutros.
- O seletor usa `D`/`S`/`M`/`3M`/`A`, oculta opções sem suporte por padrão e não
  chama callback para opções desabilitadas. Features continuam responsáveis por
  mapear período para consulta existente.
- `ChampionNavigationShell` recebe projeções de itens e callbacks; o back stack
  manual, destinos, seleção contextual e confirmações continuam em
  `ChampionNavigation`.
- Após smoke, o callback do shell foi ajustado para acompanhar cada item como
  ação direta. Isso removeu lookup intermediário por `route`, que impedia o toque
  em `Início` de retornar à Home, sem alterar destinos ou back stack.
- O renderer continua isolado em `core/designsystem/chart`. Eixo Y passou a
  usar posição final, altura mínima passou a token opt-in, estado vazio aceita
  label configurável e ranges singleton recebem margem visual sem alterar pontos
  ou gaps.
- `HealthChart` legado foi preservado. Nenhum ADR novo foi necessário, pois as
  decisões permanecem cobertas por ADR-023 e ADR-027.
- Migração visual de features fora do shell não foi feita; permanece escopo das
  subtarefas seguintes.
- A revisão independente classificou a implementação como aprovada com
  ressalvas; não encontrou bloqueadores. A duração de transição customizável foi
  corrigida antes da validação final.
- A revisão final confirmou que o shell usa ações diretas também no item Mais;
  callback redundante foi removido, sem alteração de contrato funcional.
- Validação física cobriu inicialização, shell, menu Mais, navegação para
  Dispositivo e retorno em portrait/tema escuro. Fonte ampliada, landscape,
  tema claro, estados sem dados e períodos não suportados ficaram `N/A` nesta
  execução. Capturas temporárias foram usadas somente para inspeção local e não
  foram versionadas, pois o aparelho continha dados de saúde.

### Comandos executados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; nenhuma falha de whitespace. |
| `./gradlew :app:compileDebugKotlin` | Passou após correção de imports identificada na primeira tentativa do executor. |
| `./gradlew lint assembleDebug assembleRelease` | Passou; lint, APK debug e APK release gerados. Primeira execução excedeu timeout de 120 s; reexecução com 300 s concluiu. |
| `./gradlew installDebug` | Passou; instalado em `SM-S901E`/Android 16. |
| `adb devices -l` | Passou; dispositivo autorizado disponível. |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; aplicação aberta. |
| smoke manual no shell via ADB | Passou; Home exibida, menu Mais abriu Dispositivo/Perfil/Configurações, Dispositivo foi aberto e retorno restaurou Home. |
| smoke de regressão Home via ADB | Passou após correção; Saúde abriu e o toque em `Início` retornou à Home. |
| revisão independente final | Aprovada com ressalvas; nenhum bloqueador funcional. |
| testes automatizados | `N/A`; não foram criados, alterados ou executados conforme Spec 030. |

### Resultado e evidências

- API visual compartilhada ficou neutra, stateless e desacoplada de domínio;
- shell compartilhado preservou destinos, callbacks, back stack e confirmação de
  saída;
- opções de período sem fonte não produzem ação;
- estados loading, vazio, erro, indisponível, pendência e sucesso possuem
  representação textual e semântica distinta;
- gráficos preservam gaps e dados reais, mantendo Vico privado no design system;
- build, lint, instalação e smoke visual do shell passaram;
- revisão independente aprovou implementação com ressalvas; duração de transição
  passou a consumir o token opt-in local, eliminando o desvio apontado;
- regressão do toque em `Início` foi reproduzida e corrigida; retorno por ação
  inferior passou a funcionar no dispositivo validado;
- evidência física foi observada localmente e não contém arquivo versionado.

### Riscos residuais

- A adoção dos primitives por Home, Saúde, Calendário, Exercícios, Evolução,
  Dispositivo, Perfil e Configurações permanece para subtarefas posteriores;
- fonte ampliada, landscape, tema claro e cenários sem dados não foram exercitados
  fisicamente e permanecem `N/A`;
- interação de marker/tooltip e rolagem vertical dos gráficos mantém dependência
  de validação física nas superfícies migradas;
- abstrações sem dois consumidores de feature permanecem opt-in para evitar
  redesign involuntário;
- `HealthChart` legado continua presente até comprovação de ausência de uso.
