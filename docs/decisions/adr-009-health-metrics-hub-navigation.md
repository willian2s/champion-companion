# ADR-009 — Transformar Saúde em hub de métricas com rotas específicas

## Status

Aceita

## Contexto

Ao fim de `008-08`, Fase 08 já entregou medição manual funcional de frequência cardíaca, integração do último BPM persistido na Home, regras de lifecycle e prevenção de concorrência foreground BLE.

Implementação atual ainda concentra responsabilidades demais na rota raiz `Health`:

- `AppDestination` possui apenas destino top-level `Health`;
- `ChampionNavigation` renderiza `HealthScreen()` diretamente nessa rota;
- `HealthScreen` acumula cabeçalho da área Saúde, sessão de medição manual, última medição e histórico manual simples;
- `HealthViewModel` observa apenas dados de frequência cardíaca e dispositivo associado.

Ao mesmo tempo, código já oferece fontes persistidas suficientes para expansão inicial da área Saúde sem nova coleta nem novo schema:

- `HeartRateRepository.observeLatestValidMeasurement()` para última frequência cardíaca;
- `HeartRateRepository.observeRecentManualMeasurements(...)` para histórico manual simples atual;
- `PersonalMetricsRepository.observeSnapshot()` para último peso persistido;
- `DailySummaryRepository.observeSummaryForDate(...)` para passos do dia e distância estimada atual.

Direção aprovada após `008-08` exige que Saúde deixe de ser apenas superfície da medição manual e passe a ser hub de métricas. Mudança afeta arquitetura duradoura de navegação, ownership visual da medição manual, estrutura de telas e relação entre fases 08, 09, 12, 15 e 16.

## Decisão

Área Saúde passa a seguir arquitetura de hub de métricas.

### 1. Rota raiz de Saúde

`AppDestination.Health` permanece rota raiz da área Saúde e passa a representar hub agregador.

Rota raiz deve:

- exibir cards de resumo para frequência cardíaca, peso, passos e distância;
- navegar para telas específicas de cada métrica;
- usar apenas dados persistidos existentes ou estado vazio seguro;
- não iniciar BLE diretamente.

### 2. Rotas específicas de métricas

Rotas aprovadas para implementação futura no padrão atual do projeto:

- `AppDestination.Health`
- `AppDestination.HealthHeartRate`
- `AppDestination.HealthWeight`
- `AppDestination.HealthSteps`
- `AppDestination.HealthDistance`

Essas rotas continuam parameter-free e serializáveis, preservando ADR-005 e back stack manual atual.

### 3. Ownership visual da medição manual

Tela específica de frequência cardíaca torna-se owner visual da medição manual já entregue pela Fase 08.

Regras:

- não duplicar `ChampionManualHeartRateCoordinator`;
- não duplicar implementação D0/F002;
- não criar nova regra para resultado final;
- raiz de Saúde não dispara medição;
- tela de detalhe de frequência cardíaca pode compor ou mover UI atual da medição manual.

### 4. Estrutura comum das telas de métricas

Todas as telas específicas de métricas seguem estrutura comum:

```text
Card atual e ações
    ↓
Seção de gráfico
    ↓
Seção de histórico
```

Primeiro card deve mostrar:

- valor atual;
- unidade;
- data ou horário relevante;
- origem, quando aplicável;
- estado sem dados;
- ações disponíveis para a métrica.

### 5. Dados incompletos

Funcionalidades incompletas não devem usar dados simulados.

Regras:

- gráfico real fica vazio ou marcado como futuro quando dados ainda não existirem;
- histórico completo fica vazio ou marcado como futuro quando repository atual não suportar sem expansão relevante;
- distância permanece identificada como estimativa quando vier de `DailySummary`;
- valores ausentes não podem ser inventados.

### 6. Fronteiras de dados e arquitetura

- repositories continuam sendo fonte oficial dos dados persistidos usados pela UI;
- navegação e UI não acessam DAOs diretamente;
- UI de Saúde não cria nova sincronização FEA1 nem novo fluxo BLE apenas para abrir tela;
- Health Connect não se torna fonte visual primária da área Saúde.

### 7. Responsabilidades por fase

- Fase 08: hub inicial de Saúde, detalhe de frequência cardíaca com medição manual já entregue, shells de peso/passos/distância, sem gráficos e históricos completos.
- Fase 09: ação `Buscar dados do relógio`, D1, histórico de BPM, gráfico de frequência cardíaca e agregações de frequência cardíaca.
- Fase 12: integração Health Connect para tipos suportados, sem substituir repositories locais como fonte primária da UI.
- Fase 15: calendário e histórico consolidado para gráficos/históricos de passos e distância e navegação temporal.
- Fase 16: edição e gerenciamento de peso e demais dados pessoais relacionados.

## Consequências

### Positivas

- separa claramente hub, detalhe de métrica e execução de BLE;
- reduz acoplamento entre rota raiz de Saúde e fluxo manual de frequência cardíaca;
- reaproveita fontes persistidas já existentes sem ampliar schema nesta etapa;
- distribui responsabilidades futuras entre fases coerentes do roadmap;
- preserva navegação simples e serializável já adotada pelo projeto.

### Negativas

- exige expansão da navegação top-level atual com novas rotas parameter-free;
- pode exigir reorganização de `HealthScreen` e `HealthViewModel` atuais para evitar acoplamento excessivo;
- cria risco de duplicação visual com Home se componentes e formatação não forem bem coordenados;
- posterga gráfico e histórico completos para fases futuras, deixando shells inicialmente vazios.

## Alternativas consideradas

- manter medição manual permanentemente acoplada à rota raiz `Health`;
- criar apenas novos cards na rota atual sem rotas de detalhe;
- colocar gráficos/históricos fictícios para completar interface inicial;
- mover peso, passos e distância para Home ou Perfil em vez de criar hub de Saúde;
- acessar DAOs diretamente da UI para preencher shells rápidos.

Alternativas rejeitadas por manterem acoplamento excessivo, criarem dívida arquitetural, introduzirem dados simulados ou violarem fronteiras já adotadas no projeto.
