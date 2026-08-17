# 024-03 — Refatorar shell de navegação

- **Ticker:** `024`
- **Número:** `03`
- **Status:** completed

## Objetivo e resultado esperado

Aplicar nova apresentação ao shell, insets e barra inferior sem alterar a
máquina de navegação. Resultado: base visual coerente para telas migradas e
antigas, com comportamento atual preservado.

## Requisitos e critérios cobertos

- R2, R3, R5, R6 e R7.
- Critérios 4, 8, 9, 10 e 11 da spec.

## Escopo incluído

- `Scaffold`, fundo, insets, barra inferior, indicadores e rótulos;
- menu `Mais` e apresentação das transições;
- semântica e alvos de toque do shell;
- smoke de todas as rotas e retornos.

## Escopo excluído

- `NavController`, nova IA, novas rotas ou remoção de destinos;
- mudança de guards, saver, callbacks, ViewModels ou política phone;
- redesign do conteúdo interno das telas.

## Dependências

- `024-02` concluída.

## Arquivos e símbolos prováveis

- `core/navigation/ChampionNavigation.kt`;
- componentes de shell adicionados em `core/designsystem`;
- `AppDestination.kt` apenas como referência, sem edição prevista.

## Passos de implementação

1. Isolar apresentação do shell sem mover ownership do back stack.
2. Aplicar primitives de fundo, insets e navegação.
3. Preservar cinco entradas visíveis, menu `Mais` e seleção das subárvores.
4. Preservar animação, back contextual, restauração e confirmações de saída.
5. Validar destinos top-level e detalhes acessíveis.
6. Revisar diff para excluir mudança funcional.
7. Registrar evidências e atualizar overview.

## Testes e comandos de validação

Não executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke ADB: Início, Saúde, Calendário, Exercícios, menu `Mais`, Dispositivo,
Perfil, Configurações, detalhe de Saúde, voltar e restauração após recriação.

## Definição de pronto

- shell visual atualizado;
- destinos, seleção, menu, back stack, saver e guards equivalentes;
- barra funciona com fonte `1,3x` e landscape;
- capturas sanitizadas portrait/landscape registradas;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- `ChampionNavigation.kt` é hotspot funcional; limitar diff à apresentação;
- não esconder barra inferior em detalhes nesta entrega;
- português longo pode comprimir rótulos;
- não tocar caminhos bloqueados de treino phone.

## Execução registrada

### Status

`completed`; shell visual atualizado sem alteração da máquina de navegação.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `024-03-captura-real-sanitizada-shell-portrait.png`;
- `024-03-captura-real-sanitizada-shell-landscape.png`;
- `024-03-captura-sanitizada-shell-portrait.svg`;
- `024-03-captura-sanitizada-shell-landscape.svg`;
- `024-03-refatorar-shell-de-navegacao.md`;
- `024-00-overview.md`.

### Decisões e desvios

- `Scaffold` passou a explicitar fundo e conteúdo usando esquema atual; ponte de
  insets continua somente `innerPadding`, sem padding duplicado sobre edge-to-edge.
- `NavigationBar` recebeu superfície tonal, elevação zerada e cores explícitas
  para seleção, indicador, ícone e rótulo. Cinco entradas, seleção de subárvores e
  rótulos em português permanecem iguais.
- `Mais` mantém `DropdownMenu` e três destinos, com superfície tonal, shape do
  tema e alvo mínimo de `48 dp` por item.
- Nenhuma alteração em `AppDestination`, back stack, saver, guards, callbacks,
  ViewModels, política phone ou transição de `260 ms`.
- Screenshots reais portrait e landscape foram capturados com menu `Mais`
  expandido; conteúdo da superfície, barra de status e áreas externas ao menu
  foram cobertos antes de versionar. Screenshots brutos não foram persistidos.
- SVGs permanecem como apoio estrutural sanitizado, sem substituir as capturas
  reais.
- Primeiro script de smoke não encontrou itens do menu como nós diretamente
  clicáveis, embora os alvos estivessem presentes. Script corrigido para usar
  bounds dos itens; smoke final passou.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` na execução final |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Activity aberta |
| Captura ADB portrait com `Mais` expandido | Passou; screenshot real sanitizado antes de versionar |
| Captura ADB landscape com `Mais` expandido | Passou; screenshot real sanitizado antes de versionar |
| Smoke ADB top-level e `Mais` | Passou; Início, Saúde, Calendário, Exercícios, Dispositivo, Perfil e Configurações |
| Smoke ADB detalhe e retorno | Passou; detalhe de frequência cardíaca abriu e voltou para Saúde |
| Recriação por mudança de configuração | Passou; detalhe permaneceu após rotação para landscape e retorno a portrait |
| Fonte `1,3x` + landscape | Passou; cinco rótulos visíveis e bounds presentes |
| Tema claro + landscape + fonte `1,3x` | Passou; cinco rótulos visíveis |
| Restauração do aparelho | Passou; portrait, fonte `1.0` e tema escuro restaurados |

Não foram criados, alterados ou executados testes automatizados, conforme R8 e
critério 10. Operações BLE, importação, sincronização e confirmação destrutiva
ficaram `N/A`; não foram disparadas para produzir evidência.

### Evidências

- `024-03-captura-real-sanitizada-shell-portrait.png`;
- `024-03-captura-real-sanitizada-shell-landscape.png`;
- `024-03-captura-sanitizada-shell-portrait.svg`;
- `024-03-captura-sanitizada-shell-landscape.svg`;
- hierarquias ADB temporárias confirmaram cinco alvos visíveis, menu `Mais`,
  detalhes, retorno e bounds; dumps não foram versionados.

### Riscos residuais

- Estados raros de confirmação durante medição/importação não foram acionados;
  equivalência foi revisada no diff e caminhos funcionais permaneceram intactos.
- Dynamic color não foi alternado manualmente durante smoke; temas claro e escuro
  foram observados, preservando contrato dinâmico existente.
- Capturas reais foram mascaradas na área de conteúdo e status antes de entrar
  no repositório; não contêm métricas, identificadores ou dados pessoais.

## Revisão independente

`PASS`. Revisão confirmou capturas reais portrait `1080×2340` e landscape
`2340×1080`, barra inferior completa, menu `Mais` expandido, conteúdo/status
mascarados, escopo produtivo visual e consistência SDD. Riscos de dynamic color
não alternado manualmente e estados raros permanecem não bloqueadores.
