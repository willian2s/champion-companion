# 028-03 — Criar contratos e tema compartilhados

- **Ticker:** `028`
- **Número:** `03`
- **Status:** `in_progress`
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

## Saída esperada

Boundary Champion pronta para receber o renderer Vico na `028-04`, ainda sem consumidores de feature.