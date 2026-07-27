# 015-06 — Implementar ViewModels temporais e isolar shells

- **Status:** pending
- **Objetivo:** conectar estado temporal ao read model e separar ownership das
  telas de históricos antes do paralelismo visual.
- **Resultado esperado:** `CalendarViewModel` funcional e superfícies de passos
  e distância isoladas, sem iniciar operações externas.

## Documentos obrigatórios

Spec 015, 015-03/05, contratos de Saúde, `HealthViewModel.kt`,
`HealthMetricDetailScreens.kt`, `CalendarFeatureContract.kt`, Navigation3 e
baseline de UI.

## Dependências e entradas

- 015-05 concluída.

## Arquivos permitidos

`feature/calendar`, contratos/ViewModels temporais de `feature/health` e
arquivos de composição necessários para isolar passos/distância; documentação.

## Arquivos proibidos

DAOs, entities, migrations, BLE coordinators, Health Connect, workflows de
exercícios, testes e alterações de roadmap/ADR.

## Escopo

Implementar estado `selectedMonth`, `selectedDate`, snapshots, loadings e erro;
intents Load, navegação mensal, Hoje, seleção, abertura, retry e dismiss.
Separar shells de passos/distância para reduzir conflitos entre tasks 015-09/10.
Manter `HealthViewModel` sem gatilhos automáticos.

## Fora do escopo

Grade final, gráficos finais, detalhe diário final, nova rota, BLE, provider,
permissões e testes.

## Passos

1. Criar ViewModel temporal com lifecycle seguro.
2. Conectar repository por Hilt.
3. Implementar intents e estados transitórios.
4. Isolar componentes de passos/distância.
5. Garantir observação local passiva.
6. Documentar ownership de arquivos para paralelismo.

## Critérios de aceite

- Load não inicia BLE, Health Connect ou sync.
- Todos intents possuem estado determinístico.
- Erros podem ser descartados e repetidos.
- Shells isolados permitem trabalho paralelo sem conflito inevitável.

## Validação manual

Abrir/fechar tela, navegar estados vazios e erro local. Screenshot: exigido se
layout visível mudar; registrar na task.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Não executar testes.

## Evidências e atualização

Registrar fluxo de intents, arquivos isolados e screenshot quando aplicável.
Atualizar overview após aceite.

## Validação final

Confirmar que operações externas continuam proibidas e que 015-07/09/10 têm
ownership claro. Não criar, alterar ou executar testes automatizados.

## Contexto

Calendar não possui ViewModel real e os shells de passos/distância compartilham
arquivos com Saúde, criando conflito para implementação paralela.

## Arquivos alterados

Inicialmente nenhum; registrar ViewModels, contratos e componentes isolados.

## Decisões e desvios

Registrar ownership final dos shells e qualquer desvio aprovado.

## Screenshot

Exigido se layout visível mudar; registrar caminho e cenário.

## Riscos residuais

Separação pode tocar `HealthMetricDetailScreens.kt` e exigir serialização para
evitar conflito com tasks 015-09 e 015-10.

## Atualização do overview

Marcar somente o item 015-06 após aceite de 015-05.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
