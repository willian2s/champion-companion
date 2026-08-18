# 024-09 — Refatorar passos

- **Ticker:** `024`
- **Número:** `09`
- **Status:** pending

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
