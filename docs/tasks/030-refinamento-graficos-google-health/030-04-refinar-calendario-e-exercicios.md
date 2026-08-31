# 030-04 — Refinar Calendário e Exercícios

- **Ticker:** `030`
- **Número:** `04`
- **Status:** `pending`
- **Depends on:** `030-03`

## Objetivo e resultado esperado

Aplicar linguagem visual integral ao Calendário, detalhe diário, lista
watch-first de Exercícios e detalhe da atividade, incluindo BPM, distância
canônica e cadência D4, sem alterar workflow ou dados.

## Requisitos e critérios cobertos

- R1, R3–R6; critérios 1, 2 e 4–8 da Spec 030.

## Escopo incluído

- `CalendarScreen` e `CalendarDayScreen`: mês, seleção, indicadores, legenda,
  panorama diário e navegação contextual;
- `ExercisesScreen`: importação, progresso, retry, filtros, lista, recordes,
  feedback e estados de workflow;
- detalhe: título, tipo, métricas, revisão, calibração, intervalos, ações,
  sincronização, falha, descarte, confirmações e três gráficos;
- resumo, unidade, janela real, tooltip, marker, gaps, densidade e scroll.

## Escopo excluído

- read model, elegibilidade, derivação, persistência, sync e contratos;
- captura phone, GPS, rota, localização e timeline 1 Hz;
- novos filtros, métricas, períodos ou estados;
- PoC de mapa, que permanece debug-only e fora do fluxo produtivo.

## Dependências

- `030-03` deve estar concluída.

## Arquivos e símbolos prováveis

- `feature/calendar/*`;
- `feature/exercises/ExercisesScreen.kt`;
- `core/designsystem/*` e `core/designsystem/chart/*`.

## Passos de implementação

1. Mapear hierarquia de mês, lista, detalhe e ações para componentes comuns.
2. Preservar progresso, cancelamento, retry, revisão, calibração e sync explícito.
3. Uniformizar três gráficos com janela da atividade e unidades existentes.
4. Manter gaps, intervalos D4, distância canônica e estados de workflow.
5. Validar retorno, rolagem, acessibilidade, fonte, orientação e temas.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Não criar, alterar ou executar testes automatizados. Registrar evidência
sanitizada e marcar cenários não exercitados como `N/A`.

## Definição de pronto

- Calendário e Exercícios usam composição visual integral coerente;
- watch-first, revisão explícita e estados continuam funcionais;
- dados, granularidade D4, gaps e distância canônica permanecem intactos;
- smoke, acessibilidade e riscos estão documentados.

## Riscos e cuidados

- não quebrar confirmação de saída ou ações destrutivas;
- não reativar treino phone, GPS ou rota;
- não transformar pontos ausentes em linhas contínuas;
- não vazar título, observações ou métricas pessoais em screenshots.
