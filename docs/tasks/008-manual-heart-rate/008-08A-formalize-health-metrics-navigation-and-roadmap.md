# Task 008-08A - Formalizar hub de Saúde, navegação de métricas e roadmap

## Status

Concluída em 2026-07-22.

## Objetivo

Formalizar mudança arquitetural da área de Saúde antes de alterar interface funcional.

## Contexto

`008-08` já foi concluída e deve permanecer fechada. Depois da sua conclusão, foi aprovada extensão arquitetural que transforma a rota raiz de Saúde em hub de métricas e move ownership visual da medição manual para tela específica de frequência cardíaca. Como essa mudança afeta navegação, responsabilidades de fase e fronteiras duradouras de UI/dados, formalização documental precisa acontecer antes da implementação visual.

## Documentos Obrigatorios

- `AGENTS.md`
- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-08-integrate-latest-heart-rate-into-home-and-lifecycle.md`
- `docs/tasks/008-manual-heart-rate/008-08A-formalize-health-metrics-navigation-and-roadmap.md`
- `docs/roadmap/champion-companion-roadmap.md`
- ADRs vigentes em `docs/decisions/`
- `docs/specs/007-daily-summary-home.md`
- `docs/tasks/007-daily-summary-home/007-00-overview.md`
- implementacao atual de `core/navigation`, `feature/health`, `feature/home`, `feature/profile`, `core/model` e `data`

## Dependencias

- `008-08`

## Entradas

- estado real da navegacao atual;
- estado real da tela Saude atual;
- implementacao concluida de medicao manual e integracao com Home;
- fontes de dados persistidas ja existentes para frequencia cardiaca, peso, passos e distancia.

## Arquivos Exatos Permitidos

- `docs/specs/_inputs/phase-08-manual-heart-rate-direction.md`
- `docs/specs/008-manual-heart-rate.md`
- `docs/tasks/008-manual-heart-rate/008-00-overview.md`
- `docs/tasks/008-manual-heart-rate/008-08A-formalize-health-metrics-navigation-and-roadmap.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-*.md` usando proximo numero disponivel

## Arquivos Proibidos

- Kotlin
- Compose
- Room
- `008-08` concluida
- manifesto
- Gradle

## Escopo

- auditar navegacao e tela Saude existentes;
- criar ADR para hub de Saude e telas de metricas usando proximo numero disponivel;
- registrar contexto, decisao, alternativas e consequencias;
- atualizar roadmap sem renumerar fases existentes;
- registrar responsabilidades futuras de cada fase real do roadmap;
- atualizar spec da Fase 08 quando necessario;
- definir contratos de rotas e estrutura das telas;
- atualizar matriz de rastreabilidade;
- manter `008-08` concluida e preservada.

## Resultado

- ADR-009 criada para formalizar Saúde como hub de métricas com rotas específicas e estrutura comum de telas.
- Roadmap atualizado sem renumerar fases, alinhando Fase 08, Fase 09, Fase 12, Fase 15 e Fase 16 à nova direção.
- Contratos de rotas aprovados para implementação futura: `AppDestination.Health`, `AppDestination.HealthHeartRate`, `AppDestination.HealthWeight`, `AppDestination.HealthSteps` e `AppDestination.HealthDistance`.
- Spec, input e overview da Fase 08 ficaram alinhados à decisão arquitetural e ao roadmap atualizado.
- `008-08` permaneceu concluída e preservada.

## Conteudo Obrigatorio Da ADR

A ADR deve registrar decisao de que:

- Saúde sera hub de metricas;
- cada metrica tera rota propria;
- telas de metricas seguirao estrutura comum;
- primeiro card mostrara valor atual e acoes;
- grafico e historico serao secoes proprias;
- funcionalidades incompletas nao usarao dados simulados;
- cada fase futura sera responsavel por fornecer seus dados;
- tela de frequencia cardiaca sera owner visual da medicao manual;
- tela raiz de Saude nao executara BLE;
- repositories continuarao sendo fonte de dados persistidos;
- navegacao e UI nao acessarao DAOs diretamente.

## Atualizacao Obrigatoria Do Roadmap

Antes de editar roadmap, identificar numeros e nomes reais das fases correspondentes.

Atualizacao obrigatoria deve registrar:

### Fase 08

- hub inicial de Saúde;
- tela específica de frequência cardíaca;
- medição manual;
- shells das telas de peso, passos e distância;
- valores atuais vindos de dados já existentes;
- sem gráficos e históricos completos.

### Fase 09

- ação `Buscar dados do relógio`;
- D1;
- histórico de BPM;
- gráfico de frequência cardíaca;
- agregações de frequência cardíaca.

### Fase de Perfil

- edição e gerenciamento de peso;
- demais dados pessoais;
- ações relacionadas ao peso.

### Fase de Calendário ou Histórico

- gráficos de passos;
- histórico de passos;
- gráficos de distância;
- histórico de distância;
- navegação temporal.

### Fase 12

- Health Connect;
- sincronização dos tipos suportados;
- sem assumir que Health Connect seja fonte visual primária.

## Fora Do Escopo

- implementar cards;
- implementar telas funcionais;
- mover medição manual na UI;
- criar gráficos;
- criar históricos completos;
- alterar Room;
- implementar D1;
- implementar Health Connect;
- alterar regras BLE.

## Passos

1. Auditar navegacao atual, `HealthScreen` atual e contratos reais de `AppDestination`.
2. Confirmar fontes de dados persistidos existentes para frequencia cardiaca, peso, passos e distancia.
3. Criar ADR com proximo numero disponivel, registrando contexto, decisao, alternativas e consequencias.
4. Definir contratos de rotas e estrutura padrao das telas de metricas.
5. Atualizar roadmap com responsabilidades de Fase 08, Fase 09, Fase 12, fase de Perfil e fase de Calendario/Historico sem renumerar fases.
6. Alinhar spec/input/overview e matriz de rastreabilidade.

## Criterios De Aceite

- ADR criada com proximo numero disponivel.
- `008-08` permanece concluida e inalterada como task fechada.
- roadmap atualizado com responsabilidades futuras corretas.
- contratos de rotas e estrutura das telas ficam definidos.
- matrix de rastreabilidade da Fase 08 fica atualizada.
- nenhuma UI funcional e implementada nesta task.

## Achados

- Próximo número de ADR disponível era `009`.
- Fases reais do roadmap envolvidas na extensão são:
  - Fase 08 — Frequência cardíaca manual;
  - Fase 09 — Histórico D1 e sincronização integral de BPM;
  - Fase 12 — Health Connect nativo;
  - Fase 15 — Calendário e histórico consolidado;
  - Fase 16 — Perfil e cálculos pessoais.
- Navegação atual segue ADR-005 com destinos serializáveis e sem parâmetros. Formalização preserva esse modelo.
- Fontes de dados persistidos já existentes bastam para shells iniciais:
  - frequência cardíaca por `HeartRateRepository`;
  - peso por `PersonalMetricsRepository.observeSnapshot()`;
  - passos e distância por `DailySummaryRepository`.

## Testes Pequenos Aplicaveis

- Nenhum. Task documental/arquitetural.

## Validacao Minima

- Revisao de diff documental.
- `git diff -- docs/specs docs/tasks/008-manual-heart-rate docs/roadmap docs/decisions`

## Evidencia Manual

- ADR criada: `ADR-009`.
- Fases reais do roadmap atualizadas: 8, 9, 12, 15 e 16.
- Contratos finais de rotas aprovados:
  - `AppDestination.Health`
  - `AppDestination.HealthHeartRate`
  - `AppDestination.HealthWeight`
  - `AppDestination.HealthSteps`
  - `AppDestination.HealthDistance`

## Atualizacao Do Overview

- Marcar `008-08A` como concluida quando ADR, roadmap e spec estiverem alinhados.
- Confirmar dependencia de `008-08B` em `008-08A`.

- Overview atualizado com rotas aprovadas, ADR-009 e roadmap alinhado.

## Validacao Final

- Confirmar que nenhuma implementacao funcional de UI aconteceu nesta task.

- Confirmado: task permaneceu estritamente documental e arquitetural.
