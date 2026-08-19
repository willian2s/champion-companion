# 024-19 — Colorir seleção da barra de navegação

- **Ticker:** `024`
- **Número:** `19`
- **Status:** completed

## Objetivo e resultado esperado

Aplicar cor de branding ao item selecionado da barra de navegação inferior,
mantendo contraste, rótulos, destinos e seleção de subárvores. Resultado: item
ativo visualmente distinto sem alteração da máquina de navegação.

## Requisitos e critérios cobertos

- R2, R3, R5, R6 e R7.
- Critérios 4, 8, 9 e 11 da spec aplicáveis ao shell.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- indicador, ícone e rótulo do item selecionado;
- seleção de Saúde, Calendário, Exercícios, Início e menu `Mais`;
- cores de branding em tema claro, escuro, dynamic color e fallback;
- contraste e semântica do item ativo.

## Escopo excluído

- mudança de destinos, back stack, saver, guards ou callbacks;
- alteração de conteúdo interno das telas;
- mudança de `ViewModel`, persistência, BLE ou Health Connect;
- reabertura ou alteração retroativa de `024-05`.

## Dependências

- `024-03` concluída;
- `024-04` concluída e estratégia cromática aprovada;
- hub Saúde `024-05` aceito, sem bloquear correção independente do shell.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `NavigationBarItemDefaults.colors`;
- `primaryItems`, `additionalItems` e seleção de subárvores.

## Passos de implementação

1. Inspecionar cores atuais do `NavigationBarItem` e matriz de branding aprovada.
2. Aplicar cor distinta ao indicador, ícone e rótulo selecionados.
3. Preservar seleção de Calendário, Saúde, Exercícios e `Mais` em detalhes.
4. Validar contraste sem usar cor como único sinal de estado.
5. Exercitar temas, dynamic/fallback, fonte `1,3x` e landscape.
6. Confirmar que back stack, rotas e menu permanecem equivalentes.
7. Registrar captura sanitizada e revisão independente.

## Testes e comandos de validação

Não executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: Início, Saúde, Calendário, Exercícios, `Mais`, Dispositivo, Perfil,
Configurações, detalhes e retorno; portrait, landscape, fonte `1,3x`, temas
claro/escuro e dynamic/fallback.

## Definição de pronto

- item selecionado usa branding sem perder contraste;
- cinco entradas, `Mais`, seleção de subárvores e rótulos preservados;
- sem alteração de máquina de navegação;
- smoke físico e captura sanitizada registrados;
- revisão independente aprovada e overview atualizado.

## Riscos e cuidados

- `ChampionNavigation.kt` é hotspot funcional; limitar diff à apresentação;
- indicador colorido não pode ocultar seleção em dynamic color acromático;
- rótulos longos não podem sofrer truncamento em fonte ampliada;
- não tocar caminhos bloqueados de treino phone.

## Execução registrada

### Status

`in_progress`; implementação e validação principal concluídas, aguardando
revisão independente final.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`;
- `024-19-captura-real-sanitizada-navegacao.png`;
- `024-19-colorir-selecao-navegacao.md`;
- `024-00-overview.md` atualizado após revisão final.

### Decisões e desvios

- Seleção usa `primaryContainer`, `onPrimaryContainer` e `primary` da paleta
  nominal clara/escura do Champion, seguindo estratégia aprovada em `024-04`.
- Itens não selecionados continuam usando `MaterialTheme.colorScheme`,
  preservando dynamic color e tema raiz para superfícies não migradas.
- Indicador, ícone, rótulo, cinco entradas, `Mais` e seleção de subárvores foram
  alterados somente no ramo de apresentação do shell.
- Fallback físico em API inferior a 31 não foi exercitado: aparelho autorizado
  disponível é API 36 e usuário dispensou uso de emulador. Contraste nominal foi
  confirmado por inspeção das paletas `LightColorScheme` e `DarkColorScheme`.
- Não foram alterados destinos, back stack, saver, guards, callbacks, ViewModels,
  persistência, BLE ou Health Connect.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` no rerun final |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Activity aberta |
| Smoke ADB do shell | Passou; Início, Saúde, Calendário, Exercícios, `Mais`, Dispositivo, Perfil, Configurações e detalhe de frequência cardíaca |
| Fonte `1,3x` + landscape + tema claro | Passou; rótulos permaneceram visíveis, com quebra natural em rótulos longos |
| Tema escuro + portrait | Passou; seleção nominal visível em Saúde |
| Restauração do aparelho | Passou; fonte `1,0x`, portrait e tema escuro restaurados |
| Testes automatizados | Não executados, conforme R8 |

A primeira execução de `./gradlew lint assembleDebug assembleRelease` excedeu
120 segundos sem resultado final; o rerun com timeout ampliado passou.

### Evidências

- `024-19-captura-real-sanitizada-navegacao.png` preserva somente shell e barra,
  com conteúdo e status mascarados.
- Hierarquia ADB confirmou cinco alvos, rótulos, seleção e bounds mínimos.
- Fallback nominal permanece verificável estaticamente nas paletas próprias; sua
  execução física ficou `N/A` por indisponibilidade de aparelho API inferior a 31
  e por decisão de não iniciar emulador.

### Riscos residuais

- Fallback sem dynamic color não teve smoke físico; não foi simulado como sucesso.
- Estados raros de confirmação e operações BLE/importação não foram acionados;
  nenhum foi iniciado para produzir evidência.
- Revisão independente final: `PASS`; nenhum bloqueador ou achado importante.
