# 015-10 — Implementar gráfico e lista de histórico de distância

- **Status:** pending
- **Objetivo:** transformar shell de distância em histórico local funcional.
- **Resultado esperado:** períodos de 7, 30 e 90 dias usando exclusivamente
  distância estimada diária.

## Documentos obrigatórios

Spec 015, 015-06, 015-05, `DailySummary`, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt`, detalhes de exercício e design system.

## Dependências e entradas

- 015-06 e 015-05 concluídas.

## Arquivos permitidos

Componentes isolados de distância, ViewModel temporal de distância e modelos de
apresentação; documentação e screenshots.

## Arquivos proibidos

Distância de exercícios nos totais, BLE, D1/D3/D4, Health Connect, Room/schema,
navigation, testes e edição de peso.

## Escopo

Exibir total, média por dia com valor, máximo, dias com estimativa, dias sem
estimativa, gráfico, lista e estados acessíveis. Preservar unidade e origem
estimada. Distância de atividade permanece no card da atividade.

## Fora do escopo

Calibração nova, soma de workouts, conversão de fontes, edição, sincronização e
histórico de distância de exercícios.

## Passos

1. Conectar snapshot diário.
2. Implementar períodos 7/30/90.
3. Calcular somente dias com valor.
4. Renderizar gráfico e lista textual.
5. Integrar vazios, loading e erro.

## Critérios de aceite

- Fonte exclusiva é `DailySummary.estimatedDistanceMeters`.
- Dia sem estimativa não vira zero.
- Métricas ignoram dias sem valor.
- Cards de atividade não são alterados indevidamente.
- Períodos funcionam offline.
- Histórico mostra data, valor, origem, última atualização e indicação de
  estimativa.
- Domínio permanece em metros e apresentação usa quilômetros.
- Lista distingue dias com estimativa e dias sem estimativa.
- Total, média e máximo consideram somente dias com valor.

## Validação manual

Galaxy S22 com dias estimados e sem estimativa; conferir gráfico, lista, unidade,
acessibilidade e screenshot. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar fonte, cálculo, tratamento de ausência, preservação dos cards de
atividade e screenshots. Atualizar overview.

## Validação final

Confirmar nenhuma soma de exercício e nenhum acesso externo. Não criar, alterar
ou executar testes automatizados.

## Contexto

Distância diária estimada existe em DailySummary, enquanto o shell atual não
possui gráfico ou histórico.

## Arquivos alterados

Inicialmente nenhum; registrar componentes e ViewModel de distância alterados.

## Decisões e desvios

Registrar tratamento de dias sem estimativa e qualquer desvio aprovado.

## Screenshot

Exigido. Registrar gráfico, lista e estado sem estimativa.

## Riscos residuais

Usuário pode interpretar distância estimada como medida de exercício; origem e
rótulo devem permanecer explícitos.

## Atualização do overview

Marcar somente o item 015-10 após aceite de 015-06.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
