# Spec 028 — Sistema unificado de gráficos interativos

## Status

`completed`.

## Ticker

`028`.

## Estado final

A migração das seis superfícies previstas foi concluída. Histórico de passos,
distância, frequência cardíaca, BPM do detalhe, distância canônica do detalhe e
cadência D4 usam `core/designsystem/chart`; nenhuma feature migrada importa Vico
diretamente.

`HealthChart`, `HealthChartPoint` e `normalizedChartY` foram removidos de
`ChampionDisplayComponents.kt` após a remoção dos previews e dos testes técnicos
que ainda os referenciavam. O inventário final não encontrou `Canvas` ou
`nativeCanvas` para visualização de dados nas features migradas. Vico permanece
encapsulado em `ChampionCharts.kt`, e suas dependências continuam necessárias.

Não houve alteração de domínio, persistência, BLE, Health Connect, fórmulas ou
granularidade das métricas. Smoke integrado final foi repetido no `SM-S901E` com
build `.dev`, cobrindo históricos de passos, distância e BPM, detalhe de treino,
gaps e rolagem vertical. Screenshots sanitizados estão em
`docs/integration/028-10-final-*.png`; marker/tooltip reproduzível, tema claro,
fonte ampliada e alguns estados vazios continuam limitações documentadas.

## Contexto

O Champion Companion já apresenta séries e históricos de saúde e treino, porém os gráficos atuais foram implementados diretamente com `Canvas` em diferentes superfícies. Essa abordagem foi suficiente para representação visual inicial, mas não oferece uma camada de visualização de dados consistente.

O estado atual inclui, entre outros:

- `StepsHistoryChart`, com barras desenhadas manualmente;
- `DistanceHistoryChart`, com estrutura paralela de barras;
- `HealthChart`, no design system, com linha e pontos desenhados manualmente;
- gráficos locais no detalhe de treino para distância acumulada e frequência cardíaca.

Cada implementação decide localmente escala, grid, labels, cores, descrição acessível e desenho. Não existe contrato comum para eixos, seleção, marker, tooltip, scroll, zoom, unidade ou formatação temporal.

A Spec 027 encerrou passos e cadência D4 no Health Connect e aposentou integralmente a timeline local de 1 Hz. A nova evolução deve melhorar a apresentação dos dados já existentes, sem criar granularidade sintética, novas métricas fisiológicas ou outro pipeline de sensores.

## Objetivo

Criar uma infraestrutura única e reutilizável de gráficos para Jetpack Compose, com interação real e apresentação consistente, e migrar progressivamente os gráficos vigentes para essa camada.

A infraestrutura deve permitir que as features forneçam dados e semântica de apresentação sem conhecer detalhes do motor de renderização.

## Decisão de engine

A engine inicial é **Vico 3.2.3**, encapsulada pelo design system.

Motivos:

- suporte nativo a Jetpack Compose;
- integração Material 3;
- gráficos cartesianos de linha e coluna adequados aos casos atuais;
- eixos configuráveis;
- marker e interações de toque/movimento;
- scroll e zoom opcionais;
- minSdk compatível com o projeto;
- manutenção ativa e documentação vigente.

A escolha fica formalizada em ADR próprio. Features não devem importar Vico diretamente.

## Arquitetura alvo

```text
feature/health
feature/exercises
feature/calendar
        │
        ▼
presentation mapper
        │
        ▼
core/designsystem/chart
        │
        ├── contratos neutros
        ├── tema e formatters
        ├── ChampionLineChart
        ├── ChampionBarChart
        ├── ChampionTimeSeriesChart
        └── marker/interação
        │
        ▼
       Vico
```

O domínio não conhece Vico nem componentes de UI. As features convertem seus snapshots/séries para contratos de apresentação neutros.

## Requisitos

### R1 — Única camada compartilhada

- Novos gráficos de dados devem usar `core/designsystem/chart`.
- Features não devem criar novos motores de gráfico diretamente com `Canvas`.
- `Canvas` continua permitido para elementos decorativos ou componentes que não sejam visualização de séries/métricas.
- Vico não pode vazar para domínio, repositories, ViewModels ou contratos persistentes.

### R2 — Contratos neutros

A infraestrutura deve representar pelo menos:

- pontos com coordenada X ordenável e valor Y opcional;
- séries identificadas semanticamente;
- unidade e formatter de valor;
- formatter temporal/categórico de eixo X;
- gaps como ausência, nunca como zero implícito;
- referências opcionais como meta ou guideline;
- descrição acessível independente do renderer.

### R3 — Interação comum

Gráficos interativos devem seguir um comportamento comum:

```text
tap/drag
  -> ponto ou bucket mais próximo
  -> marker visual
  -> tooltip padronizado
```

O tooltip deve apresentar somente dados já disponíveis para aquele ponto/bucket, com unidade e tempo/período coerentes.

### R4 — Eixos e escala

- Exibir eixos quando ajudarem interpretação.
- Não normalizar toda série silenciosamente para `0..1` sem labels de valores reais.
- Escalas devem respeitar a métrica e preservar comparação visual honesta.
- Unidades devem permanecer explícitas quando necessárias: `bpm`, `spm`, `km`, `passos`, `min/km`.
- Eixo temporal deve usar instantes reais ou buckets reais já persistidos/derivados.

### R5 — Scroll e zoom por necessidade

- Scroll e zoom são capacidades da infraestrutura, não comportamentos obrigatórios.
- Históricos curtos, como 7 dias, devem priorizar leitura direta sem zoom.
- Séries densas ou longas podem habilitar scroll/zoom quando houver ganho concreto.
- Interação não pode impedir rolagem normal da tela sem necessidade.

### R6 — Tema e consistência visual

- Usar Material 3 e identidade Champion já vigentes.
- Light/dark devem compartilhar a mesma semântica de gráfico.
- Grid, eixos, marker, tooltip, linhas, barras e estados vazios devem ser padronizados.
- Cor não pode ser o único sinal de significado.

### R7 — Acessibilidade

- Cada gráfico deve ter resumo acessível do período, métrica, quantidade de dados e lacunas relevantes.
- Seleção visual não substitui conteúdo acessível.
- Lacunas devem ser descritas como ausência de dado, nunca como valor zero.
- Fonte ampliada não pode tornar tooltip ou labels essenciais inacessíveis.

### R8 — Preservação semântica

- Nenhuma migração de gráfico altera domínio, Room, BLE ou Health Connect.
- Não suavizar, interpolar ou reamostrar dados apenas para melhorar aparência.
- BPM de treino continua usando as amostras reais disponíveis.
- Cadência de treino usa a granularidade D4 por intervalo definida pela Spec 027.
- Distância acumulada usa a série canônica já existente.

### R9 — Migração incremental

Ordem inicial:

1. histórico de passos;
2. histórico de distância;
3. histórico de frequência cardíaca;
4. frequência cardíaca no detalhe do treino;
5. distância acumulada no detalhe do treino;
6. cadência D4 no detalhe do treino.

Cada migração deve remover somente o desenho legado efetivamente substituído.

### R10 — Sem testes novos por padrão

- Não criar nem executar testes automatizados sem pedido explícito do usuário.
- Mudanças de código/Gradle devem passar por `git diff --check` e `./gradlew lint assembleDebug assembleRelease`.
- Mudanças visuais devem ser validadas fisicamente e registrar screenshot sanitizado quando a task exigir aceite visual.

## Não objetivos

- timeline local de 1 Hz;
- suavização de BPM/cadência;
- score de recuperação, VO2max, training load ou métricas fisiológicas inferidas;
- leitura do Health Connect;
- GPS, rota ou treino iniciado pelo celular;
- dashboards de evolução/insights nesta spec;
- reescrever modelos persistentes apenas para alimentar gráficos;
- paridade obrigatória com Google Health/Fitbit ou outro produto.

## Critérios de aceite

1. Existe uma API de gráficos compartilhada no design system e nenhuma feature migrada importa Vico diretamente.
2. Passos e distância deixam de manter implementações Canvas próprias equivalentes.
3. Séries de frequência cardíaca usam eixos de valores reais e marker/tooltip interativo.
4. O detalhe do treino apresenta BPM e distância canônica via infraestrutura comum.
5. Cadência D4 pode ser apresentada por intervalo sem inventar timeline 1 Hz.
6. Gaps continuam gaps em visual, tooltip e acessibilidade.
7. Scroll/zoom são habilitados somente nos gráficos onde forem úteis.
8. Tema claro/escuro, unidades, formatters, marker e tooltip seguem contratos comuns.
9. Não há mudança de BLE, Room, Health Connect ou semântica das métricas.
10. Canvas legado de visualização de dados é removido somente após a migração de todos os consumidores correspondentes.

## Plano de execução

- `028-01` — auditar todos os gráficos atuais;
- `028-02` — adotar Vico e registrar decisão arquitetural;
- `028-03` — criar contratos e tema compartilhados;
- `028-04` — implementar eixos, formatters, marker e interação;
- `028-05` — migrar histórico de passos;
- `028-06` — migrar histórico de distância;
- `028-07` — migrar histórico de frequência cardíaca;
- `028-08` — migrar gráficos do detalhe do treino;
- `028-09` — adicionar gráfico de cadência D4 ao detalhe do treino;
- `028-10` — remover legado, validar e fechar.
