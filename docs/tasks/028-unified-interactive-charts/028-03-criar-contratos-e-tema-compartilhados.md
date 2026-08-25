# 028-03 — Criar contratos e tema compartilhados

- **Ticker:** `028`
- **Número:** `03`
- **Status:** `completed`
- **Depends on:** `028-02`

## Objetivo

Criar a API neutra de apresentação que ficará entre as features e o Vico, além dos tokens visuais comuns dos gráficos.

Nenhuma tela deve ser migrada nesta task.

## Escopo

Criar em `core/designsystem/chart` contratos para representar:

- ponto com coordenada X ordenável e valor Y opcional;
- série identificada semanticamente;
- unidade e formatter de valor;
- formatter temporal ou categórico do eixo X;
- referências/guidelines opcionais;
- descrição acessível;
- política de interação;
- política opt-in de scroll e zoom;
- tema visual compartilhado baseado em Material 3.

## Regras semânticas

- `y = null` representa gap real e não pode ser convertido implicitamente para zero;
- os contratos são apenas de apresentação e não devem ser persistidos;
- domínio, ViewModel, repository, Room, BLE e Health Connect não conhecem Vico;
- não incluir tipos específicos de BPM, passos, cadência ou distância na camada base;
- não interpolar nem suavizar séries.

## Implementação esperada

A task deve resultar em uma estrutura equivalente a:

```text
core/designsystem/chart
├── ChampionChartContracts.kt
└── ChampionChartTheme.kt
```

A API deve ser pequena o bastante para continuar estável durante as primeiras migrações.

## Implementação realizada

- `ChampionChartPoint` preserva X ordenável e permite `y = null` como gap explícito.
- `ChampionChartSeries` exige identidade, label e ordem estritamente crescente dos pontos.
- `ChampionChartUnit` e `ChampionChartValueFormatter` representam unidade e formatação de valores sem tipos de domínio.
- `ChampionChartXAxisFormatter` mantém formatter temporal/categórico separado do formatter de valores.
- `ChampionChartReferenceLine`, `ChampionChartInteractionPolicy` e `ChampionCartesianChartData` completam referências, interação, scroll/zoom opt-in e descrição acessível.
- `ChampionChartTheme` deriva tokens de `MaterialTheme.colorScheme`, sem duplicar paletas.

Nenhuma tela, ViewModel, domínio, persistência, BLE ou Health Connect foi alterado.

## Decisões e desvios

- O eixo X recebeu contrato próprio para deixar explícita a distinção entre formatter temporal/categórico e formatter numérico de valores.
- Scroll e zoom permanecem desabilitados por padrão; seleção por ponto mais próximo permanece o comportamento inicial.
- Contratos que expõem `List` ou formatter foram mantidos sem `@Immutable`, evitando declarar imutabilidade que a API pública não garante.
- Não foram criados nem executados testes automatizados, conforme regra da task e `AGENTS.md`.

## Critérios de aceite

1. Features podem representar uma série sem importar Vico.
2. Gaps são representáveis explicitamente.
3. Existem contratos comuns para formatter, unidade e interação.
4. O tema deriva de `MaterialTheme.colorScheme` e não duplica paletas locais.
5. Nenhuma tela existente muda visualmente.
6. Nenhuma dependência de domínio/persistência é introduzida.

## Validação

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Evidências

- `git diff --check`: aprovado.
- `./gradlew lint assembleDebug assembleRelease`: aprovado (`BUILD SUCCESSFUL`, execução final após revisão).
- Busca por consumidores em `feature/`: nenhum consumidor de `core.designsystem.chart`; nenhuma tela foi migrada.

## Riscos residuais

- O adapter Vico, eixos, marker e interação ainda serão implementados na `028-04`.
- Os contratos não possuem consumidores até as migrações sequenciais das próximas subtarefas.

## Saída esperada

Boundary Champion pronta para receber o renderer Vico na `028-04`, ainda sem consumidores de feature.
