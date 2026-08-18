# 024-09 — Refatorar passos

- **Ticker:** `024`
- **Número:** `09`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe de passos para comunicar valor diário, meta e tendência de
7/30/90 dias. Resultado: progresso e histórico legíveis, com lacunas reais
preservadas.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header, resumo diário, meta, seletor de período, gráfico e estado vazio;
- labels, unidade, descrição acessível e adaptação.

## Escopo excluído

- meta editável, agregação, repository ou FEA1;
- preenchimento de lacunas ou dados sintéticos.

## Dependências

- `024-08` concluída.

## Arquivos e símbolos prováveis

- `feature/health/HealthMetricDetailScreens.kt`;
- `StepsHistoryShell.kt`;
- `StepsHistoryChart.kt`;
- `StepsDetailScreen` e `StepsHistoryViewModel` somente como referência.

## Passos de implementação

1. Destacar valor/meta e contexto temporal.
2. Padronizar seletor 7/30/90 dias sem alterar período selecionado.
3. Refinar gráfico mantendo lacunas, escala e descrição.
4. Diferenciar indisponibilidade de zero real.
5. Preservar navegação e carregamento local.
6. Validar períodos, rolagem, fonte e orientação.
7. Registrar evidências e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: alternar 7/30/90 dias, verificar gráfico/estado vazio e voltar.

## Definição de pronto

- valor, meta, período e histórico são compreensíveis;
- lacunas e ausência permanecem explícitas;
- gráfico mantém semântica e unidade;
- layout adaptativo validado;
- comandos, capturas e overview registrados.

## Riscos e cuidados

- não converter ausência em zero;
- muitos rótulos podem comprimir seletor;
- screenshot deve ocultar valores pessoais;
- arquivo compartilhado exige isolamento do ramo de passos.

## Registro de execução

- **Arquivos alterados:** `StepsHistoryShell.kt`, `StepsHistoryChart.kt` e
  `024-09-captura-real-sanitizada-steps-portrait.png`.
- **Decisões:** detalhe recebeu header azul-petróleo nominal, resumo diário
  priorizado e acento terciário nominal para passos, sem alterar contratos ou
  tema raiz. Gráfico adotou composição inspirada em gráficos de passos do
  Google Health: barras arredondadas, grade discreta, linha de meta e três
  rótulos temporais. Lacunas continuam distintas de zero por marca vazada
  compacta na base, sem fabricar barras ou valores.
- **Desvios:** nenhum requisito funcional alterado. Marca de lacuna foi
  compactada para manter legibilidade em 90 dias e fonte ampliada.
- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease`; `./gradlew installDebug`; `adb devices -l`; comando de
  abertura via `adb shell am start` definido nesta task; smoke ADB com 7D/30D/
  90D, rolagem, retorno, portrait, fonte `1,3x`, landscape forçado e temas
  escuro/claro; restauração com `wm size reset`, fonte `1,0x`, portrait e tema
  escuro.
- **Resultados e evidências:** lint, builds debug/release e instalação debug
  passaram no Galaxy S22 `SM-S901E`. Períodos atualizaram resumo, agregados,
  gráfico e lista; lacunas permaneceram explícitas; fonte ampliada, landscape e
  tema claro foram observados sem corte da ação principal. Evidência sanitizada:
  `024-09-captura-real-sanitizada-steps-portrait.png`. Nenhum teste
  automatizado foi criado ou executado.
- **Riscos residuais:** gráfico de 90 dias permanece denso por preservar janela
  diária sem agregação; loading, erro e vazio não foram fabricados e ficaram
  `N/A` na sessão física.
- **Revisão independente:** primeira revisão bloqueou a evidência por exposição
  de série e datas; captura foi substituída por recorte sem barra de status e
  máscaras opacas. Revisão final aprovou, sem bloqueadores; ressalva opcional de
  verbosidade TalkBack foi corrigida usando descrição resumida no Canvas.
