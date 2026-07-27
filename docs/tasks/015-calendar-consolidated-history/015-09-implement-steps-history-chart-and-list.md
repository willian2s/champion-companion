# 015-09 — Implementar gráfico e lista de histórico de passos

- **Status:** pending
- **Objetivo:** transformar shell de passos em histórico local funcional.
- **Resultado esperado:** períodos de 7, 30 e 90 dias com métricas, barras e
  lista acessível.

## Documentos obrigatórios

Spec 015, 015-06, 015-05, Health contracts, `HealthMetricDetailScreens.kt`,
`HealthViewModel.kt`, `DailySummary` e design system.

## Dependências e entradas

- 015-06 e 015-05 concluídas.

## Arquivos permitidos

Componentes isolados de passos, ViewModel temporal de passos e modelos de
apresentação; documentação e screenshots.

## Arquivos proibidos

BLE, refresh, D1, activities como fonte de passos, Room/schema, navigation,
Health Connect, testes e alteração de workflow.

## Escopo

Exibir total, média por dia registrado, máximo, dias registrados, dias na meta
atual, barras e lista. Diferenciar sem resumo de resumo com zero. Rotular meta
como **Meta atual**. Preservar lacunas e acessibilidade textual.

## Fora do escopo

Editar meta, criar histórico de metas, somar atividades, sincronizar ou mudar
Home.

## Passos

1. Conectar snapshot diário selecionado.
2. Implementar períodos 7/30/90.
3. Calcular métricas sem preencher lacunas.
4. Renderizar barras e lista acessível.
5. Integrar empty/loading/error.

## Critérios de aceite

- Dados vêm somente de DailySummary.
- Zero real não é confundido com ausência.
- Lacunas não entram como zero nas médias.
- Meta é identificada como atual.
- Períodos funcionam sem BLE ou provider.
- Histórico mostra data, valor, origem e última atualização.
- Períodos exibem total, média, máximo, dias registrados e dias na meta.
- Barras têm lacunas reais e lista textual acessível.
- A lista distingue `sem dados` de `0 passos` e identifica **Meta atual**.

## Validação manual

Galaxy S22 com dias ausentes, zero e acima/abaixo da meta. Conferir gráfico,
lista, rotação/escala e leitor de tela. Screenshot exigido.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar cálculos exibidos, períodos, lacunas e screenshots. Atualizar overview.

## Validação final

Confirmar nenhum passo de atividade, BLE ou Health Connect. Não criar, alterar
ou executar testes automatizados.

## Contexto

Passos possuem somente resumo diário atual e o shell existente não oferece
histórico consolidado.

## Arquivos alterados

Inicialmente nenhum; registrar componentes e ViewModel de passos alterados.

## Decisões e desvios

Registrar cálculo de lacunas/meta atual e qualquer desvio aprovado.

## Screenshot

Exigido. Registrar gráfico, lista e estados com lacunas.

## Riscos residuais

Escala de barras pode sugerir zero para lacuna; representação visual deve manter
diferença explícita.

## Atualização do overview

Marcar somente o item 015-09 após aceite de 015-06.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
