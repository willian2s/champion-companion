# ADR-027 — Engine unificada de gráficos interativos

## Status

Aceito.

## Contexto

O Champion Companion possui diferentes gráficos implementados diretamente com Jetpack Compose `Canvas`. Passos e distância possuem implementações locais paralelas; o design system possui um `HealthChart` manual limitado; o detalhe de treino possui gráficos próprios para frequência cardíaca e distância acumulada.

Essas implementações não compartilham contratos para coordenada X, unidade, eixo, marker, tooltip, gaps, scroll, zoom ou formatação. A manutenção e evolução da apresentação exigem alterações repetidas em cada feature.

A Spec 028 busca criar uma única infraestrutura de visualização de dados sem alterar domínio, persistência ou granularidade das métricas.

## Decisão

Adotar **Vico 3.2.3** como engine de gráficos cartesianos Compose e encapsulá-lo em `core/designsystem/chart`.

As features não importarão tipos Vico diretamente. Elas fornecerão contratos neutros de apresentação para componentes Champion.

A dependência inicial será limitada aos módulos necessários para Compose e Material 3.

## Razões

- integração nativa com Jetpack Compose;
- suporte a Material 3;
- line e column charts atendem aos casos vigentes;
- suporte a eixos reais;
- marker e modelo de interação disponíveis;
- scroll e zoom disponíveis quando necessários;
- minSdk da biblioteca compatível com o projeto;
- biblioteca ativa e documentação vigente;
- permite remover cálculo/desenho repetido sem reimplementar um framework de gráficos dentro do app.

## Boundary arquitetural

```text
feature
  ↓
mapper de apresentação
  ↓
contratos Champion de gráfico
  ↓
core/designsystem/chart
  ↓
Vico
```

São proibidos como direção futura:

```text
feature -> Vico
ViewModel -> Vico
domain -> Vico
repository -> Vico
```

A troca futura de renderer deve ficar concentrada no design system e nos adapters internos da infraestrutura de gráficos.

## Contratos mínimos da camada Champion

A camada compartilhada deve modelar:

- X real/ordenável;
- Y opcional para representar gaps;
- séries e labels semânticos;
- unidade e formatação;
- eixo temporal ou categórico;
- referências/guidelines opcionais;
- descrição acessível;
- política de interação;
- política opt-in de scroll e zoom.

Esses contratos são de apresentação. Não serão persistidos em Room nem usados como novo domínio de saúde.

## Preservação dos dados

A engine não autoriza:

- interpolar gaps;
- suavizar séries;
- gerar timeline 1 Hz;
- alterar estatísticas autoritativas;
- reinterpretar granularidade D4;
- criar valores apenas para tornar o gráfico visualmente contínuo.

O gráfico renderiza os dados disponíveis e suas ausências.

## Interação

Marker/tooltip terão semântica comum na infraestrutura. Scroll e zoom existirão como capacidades, mas permanecerão desabilitados por padrão e serão ativados somente onde a densidade da série justificar.

## Consequências positivas

- consistência entre Saúde e Exercícios;
- eixos e labels de valores reais;
- interação por toque/drag;
- tooltip e marker padronizados;
- redução de Canvas e `nativeCanvas` local;
- menor custo para futuras telas de evolução/insights;
- evolução visual sem acoplamento a domínio/persistência.

## Consequências e riscos

- nova dependência de UI de terceiros;
- necessidade de adapter Champion para não espalhar API da biblioteca;
- migração incremental manterá temporariamente gráficos legados e novos lado a lado;
- comportamento de gestos deve ser validado com `verticalScroll` das telas;
- upgrades do Vico devem ser deliberados, não automáticos.

## Alternativas consideradas

### Manter Canvas próprio

Rejeitado como direção principal. Entregaria controle total, mas obrigaria o projeto a implementar e manter eixos, hit-testing, marker, tooltip, gestures, scroll, zoom e acessibilidade por conta própria.

### KoalaPlot

Não escolhido para a fundação atual. A decisão prioriza uma engine mais diretamente alinhada aos casos cartesianos e à interação necessária no app.

### Expor Vico diretamente nas features

Rejeitado por acoplamento. Isso apenas substituiria duplicação de Canvas por duplicação de configuração específica da biblioteca.

## Validação

A adoção inicial deve passar por:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Nenhum teste automatizado novo deve ser criado ou executado sem solicitação explícita.
