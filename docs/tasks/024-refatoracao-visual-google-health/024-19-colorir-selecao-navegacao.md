# 024-19 — Colorir seleção da barra de navegação

- **Ticker:** `024`
- **Número:** `19`
- **Status:** pending

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
