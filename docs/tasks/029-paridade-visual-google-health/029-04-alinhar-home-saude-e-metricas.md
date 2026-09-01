# 029-04 — Alinhar Home, Saúde e métricas

- **Ticker:** `029`
- **Número:** `04`
- **Status:** `pending`
- **Depende de:** `029-03` e fechamento de `028-10` quando houver alteração no renderer

## Objetivo e resultado esperado

Aplicar o baseline às superfícies Home, Saúde e detalhes de frequência cardíaca,
peso, passos e distância, preservando read models, filtros, fontes de dados e
gráficos compartilhados.

## Requisitos e critérios de aceite cobertos

- R4, R5, R7 e R8 da Spec 029.
- Critérios 4, 5, 8, 9, 10 e 11.
- Cobrir todos os períodos de métricas e estados com dados ou vazio.

## Escopo incluído

- hierarquia, cards, resumo, títulos, unidades, loading, vazio e erro das
  superfícies Home/Saúde;
- detalhes e filtros de frequência cardíaca, peso, passos e distância;
- adequação visual dos gráficos ao baseline usando apenas
  `core/designsystem/chart`;
- heart rate diário intradia e períodos 7/30 com dados locais reais, gaps e
  estado vazio correto;
- screenshots Champion sanitizados por superfície e período.

## Escopo excluído

- nova métrica, nova fonte remota, Health Connect readback ou dados sintéticos;
- alteração de Room, DataStore, BLE, derivação ou sincronização;
- segunda engine de gráfico, import direto de Vico ou mudança de granularidade;
- calendário, abas de exercícios e filtros de workflow.

## Dependências

- fundação 029-03;
- decisão e matriz de 029-02;
- conclusão de `docs/tasks/028-unified-interactive-charts/028-10-remover-legado-validar-e-fechar.md`
  antes de tocar legado/renderer;
- `HeartRateRepository`, `HeartRateHistorySnapshot`,
  `CalendarHistoryPeriod` e shells atuais.

## Arquivos e símbolos prováveis

- `feature/home/HomeScreen.kt`;
- `feature/health/HealthScreen.kt`;
- `feature/health/HealthMetricDetailScreens.kt`;
- `feature/health/HeartRateHistoryChart.kt`;
- `feature/health/StepsHistoryShell.kt` e `StepsHistoryChart.kt`;
- `feature/health/DistanceHistoryShell.kt` e `DistanceHistoryChart.kt`;
- `HealthViewModel`, `StepsHistoryViewModel` e `DistanceHistoryViewModel`;
- `core/model/HeartRateAndSync.kt` e `RepositoryContracts.kt`, somente se
  adapter de apresentação exigir contrato já existente.

## Passos de implementação

1. Comparar cada superfície com matriz e separar divergência visual de contrato.
2. Migrar composição comum para fundação sem alterar fluxo funcional.
3. Alinhar cards, resumo, controles, unidades, estados e ordem de seções.
4. Aplicar seletores existentes conforme referência, preservando semântica.
5. Para BPM, validar fontes locais, janela, resolução, gaps e ausência antes de
   ajustar aparência do gráfico.
6. Validar tooltip, marker, eixos, scroll e gesto sem criar série adicional.
7. Capturar evidência sanitizada de cada período e estado alcançado.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Smoke físico: Home, hub Saúde, quatro detalhes, todos os períodos, dados do dia,
estado vazio quando disponível, tema, orientação, fonte ampliada, toque,
marker, tooltip e rolagem. Não criar testes automatizados.

## Definição de pronto

- quatro métricas e Home/Saúde correspondem à matriz observada;
- BPM fora do diário usa somente dados locais válidos e preserva lacunas;
- nenhum valor é carregado de período diferente ou sintetizado;
- renderer compartilhado, eixos, unidades e interação permanecem corretos;
- build, lint e smoke passam com evidências sanitizadas.

## Riscos e cuidados

- Dados reais do aparelho podem variar durante a captura; registrar janela e
  estado, sem congelar valores pessoais.
- Não transformar média de atividade em série temporal.
- Não colocar amostras de atividade no resumo diário sem decisão funcional nova.
- Gestos de gráfico não podem impedir rolagem vertical da tela.
