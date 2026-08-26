# 028-01 — Auditar gráficos atuais

- **Ticker:** `028`
- **Número:** `01`
- **Status:** `completed`

## Objetivo

Inventariar as implementações reais de visualização de dados antes de escolher contratos compartilhados ou migrar telas.

## Resultado da auditoria

### 1. Histórico de passos

Arquivo:

`app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/StepsHistoryChart.kt`

Estado encontrado:

- implementação própria com `Canvas`;
- cálculo manual de escala vertical;
- grid fixo em 25%, 50% e 75%;
- linha de meta desenhada manualmente;
- barras e gaps desenhados diretamente;
- labels de X externos ao gráfico usando primeiro/meio/último dia;
- sem eixo Y de valores reais;
- sem seleção, marker ou tooltip;
- sem scroll ou zoom;
- descrição acessível consolidada fora de interação por ponto.

### 2. Histórico de distância

Arquivo:

`app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/DistanceHistoryChart.kt`

Estado encontrado:

- implementação própria com `Canvas`;
- estrutura quase paralela ao gráfico de passos;
- escala, grid, barras, gaps e labels implementados novamente;
- sem eixo Y de valores reais;
- sem seleção, marker ou tooltip;
- sem scroll ou zoom.

A duplicação com `StepsHistoryChart` confirma que não existe engine compartilhada para gráficos de barras.

### 3. `HealthChart` do design system

Arquivo:

`app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionDisplayComponents.kt`

Estado encontrado:

- contrato `HealthChartPoint` contém apenas `value: Float` e descrição;
- valores são normalizados localmente para coordenada visual `0..1`;
- linha e círculos são desenhados manualmente com `Canvas`;
- eixo X é posição ordinal implícita;
- não há eixo Y, unidade, tempo, marker, tooltip, scroll ou zoom;
- componente não consegue representar gaps explicitamente.

Esse contrato é insuficiente como fundação futura porque perde a coordenada X real e semântica da unidade.

### 4. Distância acumulada no detalhe do treino

Arquivo:

`app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`

Função auditada:

`WorkoutCanonicalDistanceChart`.

Estado encontrado:

- `Canvas` local dentro da feature;
- X calculado por elapsed time real;
- Y calculado pela distância acumulada máxima;
- linha, pontos e baseline desenhados manualmente;
- horário inicial/final renderizado fora do Canvas;
- sem eixo Y detalhado;
- sem marker/tooltip;
- sem compartilhamento com outros gráficos.

### 5. Frequência cardíaca no detalhe do treino

Arquivo:

`app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`

Função auditada:

`WorkoutHeartRateChart`.

Estado encontrado:

- `Canvas` local dentro da feature;
- escala BPM calculada manualmente;
- guidelines de BPM e labels desenhados manualmente;
- uso de `nativeCanvas`/`android.graphics.Paint` para labels;
- linha e pontos desenhados manualmente;
- timestamps inicial/final externos ao gráfico;
- sem seleção, marker ou tooltip;
- sem scroll/zoom;
- implementação fortemente acoplada à tela.

## Problemas transversais confirmados

1. múltiplos motores locais de gráfico;
2. repetição de escala, grid, labels e desenho;
3. ausência de modelo comum de coordenada X;
4. ausência de unidade/formatter como contrato;
5. ausência de marker e tooltip;
6. ausência de política comum para gaps;
7. ausência de política comum para scroll/zoom;
8. acessibilidade definida caso a caso;
9. features precisam conhecer detalhes de desenho;
10. evolução futura de gráficos exigiria repetir lógica em cada tela.

## Direção resultante

A auditoria sustenta a criação de `core/designsystem/chart` como boundary único para visualizações de dados e a adoção de uma engine Compose dedicada encapsulada por essa boundary.

`Canvas` não será proibido globalmente. O guard rail aplica-se a novos gráficos de séries/métricas; desenho decorativo permanece permitido.

## Escopo de migração confirmado

Ordem recomendada:

1. passos;
2. distância histórica;
3. frequência cardíaca histórica;
4. BPM de atividade;
5. distância acumulada da atividade;
6. cadência D4 por intervalo.

## Verificação

Task documental/auditoria. Nenhum código, Gradle, teste, Room, BLE ou Health Connect foi alterado.
