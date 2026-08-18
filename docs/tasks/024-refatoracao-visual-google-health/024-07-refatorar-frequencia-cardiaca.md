# 024-07 — Refatorar frequência cardíaca

- **Ticker:** `024`
- **Número:** `07`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar detalhe de frequência cardíaca, organizando resumo, medição manual,
importação D1, gráfico e histórico. Resultado: ações e estados operacionais
claros sem alterar protocolos, persistência ou confirmação de saída.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- header e retorno;
- resumo atual e origem;
- medição manual, progresso, cancelamento e retry;
- pull-to-refresh D1, metadata e feedback;
- períodos, gráfico, resumo acessível e histórico.

## Escopo excluído

- `ManualHeartRateCoordinator`, protocolo D0/F002, coordinator D1;
- cálculo, repository, persistência, sync ou novo teste;
- nova coleta ou leitura Health Connect.

## Dependências

- `024-06` concluída;
- relógio/estado real disponível somente quando necessário e seguro.

## Arquivos e símbolos prováveis

- `feature/health/HealthMetricDetailScreens.kt`;
- `feature/health/HeartRateHistoryChart.kt`;
- `HeartRateDetailScreen` e Composables locais de resumo, operação e histórico.

## Passos de implementação

1. Mapear todos os ramos de `HealthUiState` antes do diff.
2. Separar visualmente resumo, ações, tendência e histórico.
3. Tornar medição e importação distinguíveis, mantendo gatilhos explícitos.
4. Preservar confirmação de saída e resultado parcial não persistido.
5. Manter gráfico com períodos, lacunas, unidade e descrição acessível.
6. Validar estados seguros disponíveis; marcar operações não exercidas `N/A`.
7. Revisar ausência de mudança em intents/callbacks e registrar evidências.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: Saúde → Frequência cardíaca, períodos, rolagem, back; medição/D1 somente
se seguro, sem gerar dado para evidência; fonte `1,3x`, landscape e temas.

## Definição de pronto

- resumo, ações, gráfico e histórico possuem hierarquia clara;
- medição/D1 e confirmações preservadas;
- nenhum valor sintético ou resultado parcial;
- evidência sanitizada e estados `N/A` registrados;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- tela é hotspot com operações bloqueantes;
- não cancelar operação real sem consentimento;
- histórico pode conter métricas pessoais: sanitizar captura;
- mudanças de composição não podem reiniciar intents ou collectors.

## Execução registrada

### Status

`completed`; refatoração visual, validação com dados persistidos e evidência
sanitizada concluídas.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HeartRateHistoryChart.kt`;
- `docs/tasks/024-refatoracao-visual-google-health/024-07-captura-real-sanitizada-heart-rate-portrait.png`;
- este arquivo;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`.

### Decisões e desvios

- Header próprio de frequência cardíaca passou a usar azul-petróleo nominal do
  Champion com conteúdo branco, sem alterar `MetricDetailScaffold` usado por
  outras métricas.
- Resumo atual, medição manual, importação D1, tendência e histórico passaram a
  ocupar superfícies visuais distintas. Callbacks, intents e ordem operacional
  foram preservados.
- Importação D1 manteve progresso, metadata, lacunas, resultado parcial, retry e
  fechamento de aviso; apenas ganhou título de estado e superfície própria.
- Períodos continuam `TODAY`, `LAST_7_DAYS` e `LAST_30_DAYS`; labels visuais
  passaram de `D`/`S`/`M` para `Hoje`/`7 dias`/`30 dias`, com estado selecionado
  também exposto semanticamente.
- Valor principal, BPM do histórico, progresso e gráfico usam acento nominal
  `primary` claro/escuro. Faixas visuais existentes do gráfico foram preservadas
  para não alterar significado ou dados.
- Após revisão independente, o hero de medição passou a usar o par nominal
  `primaryContainer`/`onPrimaryContainer` em cada tema; cancelamento e confirmação
  de saída usam `error`/`onError`, distinguindo ação destrutiva sem mudar callback.
- Após revisão independente, o gráfico quebra linhas entre datas com lacunas e os
  rótulos de 7/30 dias usam a janela temporal completa, sem criar pontos sintéticos.
- Revisão independente inicial reprovou contraste do hero escuro, ação destrutiva
  primária e lacunas/eixo do gráfico; os três achados foram corrigidos e a revisão
  independente final passou sem bloqueadores.
- Feedback existente do Health Connect permaneceu no detalhe; nenhuma integração,
  sincronização ou persistência foi modificada.
- Nenhuma medição manual ou importação D1 foi iniciada para gerar evidência. Dados
  persistidos existentes foram somente exibidos e a captura foi sanitizada.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew app:lintDebug` | Passou |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew lint assembleDebug assembleRelease` após revisão independente | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; APK instalado no SM-S901E Android 16 |
| `adb devices -l` | Passou; dispositivo autorizado e operacional |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Smoke com dados persistidos | Passou; resumo atual, medição manual e valor persistido renderizados |
| Smoke de rolagem e períodos | Passou; D1, tendência, `Hoje`, `7 dias` e `30 dias` acessíveis; períodos 7 e 30 exercitados |
| Smoke de retorno | Passou; frequência cardíaca retornou para Saúde |
| Smoke landscape e fonte `1,3x` | Passou; labels estruturais do detalhe permaneceram acessíveis, sem corte observado |
| Smoke tema claro/escuro | Passou; resumo e header renderizados nos dois temas |
| Restauração do aparelho | Passou; fonte `1,0x`, rotação bloqueada em portrait e tema escuro |

Não foram criados, alterados ou executados testes automatizados.

### Evidências e riscos residuais

- Evidência sanitizada: `024-07-captura-real-sanitizada-heart-rate-portrait.png`.
  Barra de status foi removida e valor, horário e identificação do relógio foram
  cobertos; hierarquia, header, resumo e ação de medição permanecem verificáveis.
- Estados reais disponíveis de resumo e medição ociosa foram exercitados com dados
  locais existentes. Loading, erro, retry, cancelamento, medição em andamento,
  resultado parcial e importação D1 em andamento ficaram `N/A`; não foram
  fabricados nem disparados.
- Não há risco residual funcional conhecido no escopo. Permanece risco visual
  residual de estados operacionais raros não observados fisicamente. Contraste do
  hero, ação destrutiva e lacunas do gráfico foram confirmados na revisão final.
