# 037-04 — Integrar tendência de corrida no ViewModel e navegação

- **Ticker:** `037`
- **Número:** `04`
- **Status inicial:** `pending`

## Objetivo e resultado esperado

Expor o histórico de corrida como estado tipado do Exercícios, compor read model
e policy fora do Compose e abrir entrada contextual em tela própria.

## Requisitos cobertos

- integração bounded no `ExercisesViewModel`;
- estados `AVAILABLE`, `NO_RUNS`, `NO_COMPARABLE_RUNS`, `UNAVAILABLE`;
- CTA contextual sem nova aba principal;
- separação da carga cardiovascular;
- `asOf`, `ZoneId` e janela 56 dias estáveis;
- nenhum cálculo em Compose ou acesso DAO pelo ViewModel.

## Escopo incluído

- campo de estado dedicado em `ExercisesUiState`;
- combinação do repository, `asOf`, `ZoneId` e policy;
- destino serializável contextual na subtree de Exercícios;
- callback/label/back stack manual conforme padrão cardiovascular;
- CTA acessível `Tendência de corrida`.

## Escopo excluído

- cálculo de pace/FC na tela;
- mudança do histórico de carga 035/036;
- nova aba, Home, Saúde, Perfil ou Settings;
- activity ID no contrato visual, salvo necessidade futura de navegação tipada;
- Health Connect, Room, parser, BLE ou workflow;
- testes automatizados novos.

## Dependências

- `037-02` e `037-03` concluídas;
- `ExercisesFeatureContract.kt`;
- `ExercisesViewModel.kt`;
- `ExercisesScreen.kt`;
- `AppDestination.kt` e `ChampionNavigation.kt`;
- padrão contextual de `CardiovascularLoadHistory`.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesFeatureContract.kt`;
- `feature/exercises/ExercisesViewModel.kt`;
- `feature/exercises/ExercisesScreen.kt`;
- `core/navigation/AppDestination.kt`;
- `core/navigation/ChampionNavigation.kt`;
- novo `RunningPerformanceHistoryScreen.kt` somente como destino de
  apresentação, se a implementação separar a tela.

## Passos de implementação

1. Adicionar estado de UI separado do cardiovascular.
2. Compor read model bounded e `RunningPerformanceHistoryPolicy` em dispatcher
   apropriado, com `asOf` estável.
3. Mapear `NO_HEART_RATE_SAMPLES`, `INVALID_SAMPLES`, cobertura válida e
   `SOURCE_UNAVAILABLE` sem transformar ausência em zero; falha de fonte mantém
   status top-level `UNAVAILABLE`.
4. Adicionar destino serializável contextual e incluir a subtree de Exercícios
   sem alterar navegação primária.
5. Adicionar CTA acessível e retorno contextual.
6. Confirmar que Compose recebe contratos prontos e não acessa DAO/calcula.

## Testes e comandos de validação

Não criar testes automatizados. Executar revisão e:

```bash
git diff --check
```

Build completo fica para execução futura autorizada de código.

## Definição de pronto

- estado 037 é independente da carga cardiovascular;
- entrada contextual abre/fecha corretamente;
- estados não confundem ausência com zero;
- ViewModel não consulta DAO nem calcula métricas;
- filtros Semana/Mês da lista existente não contaminam a população 037;
- nenhum destino aparece fora de Exercícios.

## Riscos e cuidados

- preservar saver/restorer e back stack manual;
- não reutilizar `CardiovascularLoadHistoryUiState` por conveniência;
- não chamar `Instant.now()` dentro da policy;
- não criar CTA em Home, Saúde, Dispositivo ou Configurações;
- manter linguagem factual e sem recomendação.
