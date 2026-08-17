# 024-08 — Refatorar passos

- **Ticker:** `024`
- **Número:** `08`
- **Status:** pending

## Objetivo e resultado esperado

Refatorar detalhe de passos para comunicar valor diário, meta e tendência de
7/30/90 dias. Resultado: progresso e histórico legíveis, com lacunas reais
preservadas.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.

## Escopo incluído

- header, resumo diário, meta, seletor de período, gráfico e estado vazio;
- labels, unidade, descrição acessível e adaptação.

## Escopo excluído

- meta editável, agregação, repository ou FEA1;
- preenchimento de lacunas ou dados sintéticos.

## Dependências

- `024-07` concluída.

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
