# Spec 029 — Evolução esportiva e recordes pessoais

## Status

`completed`.

## Ticker

`029`.

## Contexto

O Champion Companion já possui histórico local de atividades importadas do Champion C001, métricas derivadas, calendário, históricos de saúde e uma infraestrutura unificada de gráficos interativos entregue pela Spec 028.

O app hoje responde bem **o que aconteceu em um treino ou período**, mas ainda não consolida de forma dedicada **como o treinamento está evoluindo**. A próxima evolução deve reutilizar os fatos locais já persistidos, sem criar novos sensores, scores fisiológicos ou dependência de leitura do Health Connect.

A funcionalidade também deve reconhecer recordes pessoais. A detecção não pode ocorrer durante a importação ou a partir do estado transitório do formulário: métricas como distância podem mudar durante revisão, distância manual de bicicleta, calibração de esteira ou rederivação explícita antes do sync.

A auditoria `029-01` congelou as fontes de verdade, workflows elegíveis, fórmulas iniciais, pontos de persistência e direção de navegação em `docs/integration/029-01-evolution-records-audit.md`.

## Objetivo

Adicionar uma superfície de evolução esportiva com:

- volume e consistência por período;
- tendências de métricas elegíveis;
- comparação entre períodos equivalentes;
- recordes pessoais calculados a partir do histórico local;
- destaque de novo recorde somente depois de uma mutação persistida que deixe a atividade com métricas finais elegíveis.

A feature é descritiva. Não interpreta os dados como diagnóstico, prontidão, recuperação ou melhora fisiológica.

## Arquitetura alvo

```text
WorkoutActivity / séries locais / histórico
                 │
                 ▼
      read model de evolução
                 │
        ┌────────┴────────┐
        ▼                 ▼
EvolutionSnapshot   PersonalRecordsSnapshot
        │                 │
        └────────┬────────┘
                 ▼
         feature/evolution
                 │
                 ▼
      Champion charts da Spec 028
```

Recordes pós-save seguem fluxo separado de efeito de UI:

```text
snapshot persistido anterior
          +
mutação explícita da atividade
          ↓
persistência concluída
          ↓
snapshot persistido novo
          ↓
PersonalRecordEvaluator
          ↓
0..N conquistas novas
          ↓
banner/card pós-save
```

Falha ao avaliar recordes nunca desfaz nem transforma em falha uma persistência de atividade que já concluiu com sucesso.

## Requisitos

### R1 — Read model derivado

- Evolução deve ser derivada dos fatos locais vigentes.
- A primeira versão usa atividades `WATCH_D3_D4`; infraestrutura histórica de treino phone não é expandida nesta spec.
- Não criar tabela materializada de agregados na primeira implementação sem evidência real de necessidade de performance.
- Room continua fonte dos fatos persistidos; o read model pode combinar repositories existentes.
- Nenhum agregado pode reescrever atividades históricas.

### R2 — Períodos e filtros

A superfície deve suportar inicialmente:

- 7 dias;
- 30 dias;
- 90 dias;
- filtro por tipo de treino elegível;
- visão consolidada quando a métrica possuir semântica comparável entre tipos.

Comparações devem usar períodos imediatamente anteriores de mesmo tamanho.

### R3 — Volume e consistência

Expor, quando semanticamente elegível:

- quantidade de atividades;
- distância total;
- duração total;
- passos em atividades;
- quantidade de dias com atividade.

Ausência de dado não vira zero quando a diferença for semanticamente relevante.

### R4 — Tendências

Tendências podem incluir métricas já disponíveis, como:

- distância por atividade/período;
- duração;
- frequência cardíaca média quando houver base válida;
- cadência D4 para treinos elegíveis;
- pace ou velocidade somente após definição explícita de fórmula e população comparável.

Nenhuma média pode ser implementada como média ingênua de médias quando isso distorcer a métrica. A `029-01` registrou as fórmulas-base e a `029-05` deve respeitá-las.

### R5 — Comparação entre períodos

Comparar período atual e período imediatamente anterior usando a mesma população/filtro.

Exemplo:

```text
últimos 30 dias
vs.
30 dias anteriores
```

Mostrar diferenças absolutas e/ou percentuais apenas quando denominadores e unidades forem válidos. Divisão por zero deve degradar para comparação textual segura, não infinito ou percentual inventado.

### R6 — Recordes pessoais

A primeira versão de recordes prioriza métricas factuais com semântica clara de conquista:

- maior distância final;
- maior duração;
- maior número de passos em treino elegível.

Recordes são segregados por tipo de atividade quando misturar esportes produzir comparação enganosa. `CYCLING` não usa passos D4 como recorde de performance. `FREE_WORKOUT` não participa do recorde inicial de maior distância. Frequência cardíaca máxima e maior cadência não são recordes de performance nesta spec.

Pace/velocidade como recorde ficam fora da primeira versão até existir contrato comparável por distância/modalidade.

### R7 — Elegibilidade de recorde e evolução

A primeira versão considera somente atividades `WATCH_D3_D4` com tipo confirmado diferente de `UNKNOWN`.

Workflows elegíveis quando a métrica específica for válida:

- `READY_TO_SYNC`;
- `SYNCING`;
- `SYNCED`;
- `SYNC_FAILED`.

Ficam fora:

- `PENDING_REVIEW`;
- `PENDING_CALIBRATION`;
- `DISCARDED`.

`ParserValidity.PARTIAL` pode participar quando o fluxo vigente permitir revisão final e a métrica específica for válida.

Cada record type valida sua própria fonte: distância positiva/finita quando aplicável, duração positiva e passos disponíveis/positivos quando aplicável.

### R8 — Detecção somente após persistência final

O alerta de novo recorde não ocorre na importação D3/D4 nem antes do commit da alteração.

Pontos aprovados na auditoria, quando produzirem snapshot elegível:

- `WorkoutActivityRepository.submitReview`;
- `TreadmillCalibrationRepository.calibrateImportedActivity`;
- `PersonalMetricsRepository.applyReferenceDistanceCalibration`;
- `WorkoutActivityRepository.rederiveWatchWorkout` / `reprocessWatchWorkout` quando uma rederivação explícita alterar métrica final.

`TREADMILL` em `PENDING_CALIBRATION` não dispara recorde até a calibração concluir. Importação/reimportação D3/D4 e transições de sync não disparam conquista.

A UI recebe resultado somente após a persistência bem-sucedida e obtenção do snapshot efetivamente salvo.

### R9 — Idempotência do destaque

O avaliador recebe o snapshot anterior e o novo snapshot persistido.

Para cada recorde:

```text
baselineOther = melhor valor elegível das demais atividades
previousBest = max(baselineOther, valor anterior da atividade quando antes elegível no mesmo escopo)
```

Emitir conquista somente quando:

```text
newValue > previousBest
```

Consequências:

- salvar novamente sem alteração não repete o alerta;
- uma atividade que já era recorde e recebe correção para valor ainda melhor pode gerar novo alerta;
- mudança de tipo usa a baseline do novo tipo;
- a atividade atual é excluída da consulta das demais atividades para não comparar o novo valor consigo mesma;
- empate não gera novo recorde.

### R10 — Atividade antiga importada depois

O destaque pós-save representa **novo recorde atual conhecido no banco local**, não “recorde que existia na época cronológica da atividade”.

Uma atividade antiga importada hoje:

- gera destaque se superar todas as atividades elegíveis atualmente conhecidas no mesmo escopo;
- não gera destaque se já existir atividade conhecida com valor maior, mesmo que ela tenha ocorrido depois no tempo.

A tela de recordes mostra a data real da atividade recordista, baseada no início do treino e não na data de importação.

### R11 — Recordes como read model, conquista como evento efêmero

- A lista atual de recordes deve ser recalculável a partir das atividades elegíveis, evitando estado materializado obsoleto.
- O destaque pós-save é efeito efêmero de UI; não precisa virar entidade Room na primeira versão.
- Descartar ou alterar uma atividade antes do sync deve fazer o read model recalcular quem é o recordista atual.

### R12 — Apresentação pós-save

Quando houver uma ou mais conquistas novas, exibir destaque não bloqueante após o save, contendo tipo de recorde, modalidade, novo valor e diferença para o recorde anterior quando disponível.

Para múltiplos recordes, consolidar em um único componente em vez de abrir vários diálogos.

O destaque não deve impedir navegação nem converter sucesso de persistência em erro caso a apresentação falhe.

### R13 — Navegação

Adicionar `AppDestination.Evolution` como subárvore de `Exercises`, mantendo a seleção da aba Exercícios. A entrada inicial ocorre por ação/card na superfície Exercícios. A Spec 029 não adiciona nova aba inferior.

### R14 — Gráficos

Todos os novos gráficos usam `core/designsystem/chart` da Spec 028. Features não importam Vico diretamente nem criam novo engine Canvas para séries de dados.

### R15 — Sem novas inferências fisiológicas

Ficam fora:

- VO2max estimado;
- recovery/readiness score;
- training load fisiológico;
- fitness/fatigue/form;
- previsão de prova;
- recomendação automática de treino;
- IA interpretando qualidade do treino.

## Não objetivos

- leitura do Health Connect;
- sync em background;
- GPS, rota ou treino iniciado pelo celular;
- reamostragem 1 Hz;
- rederivar métricas históricas para melhorar gráficos;
- persistir badges/conquistas como gamificação completa;
- metas esportivas; ficam como possível spec posterior;
- comparação detalhada treino contra treino; fica como possível evolução posterior.

## Critérios de aceite

1. Existe read model de evolução sem materialização prematura em Room.
2. Períodos 7/30/90 e filtros preservam população e unidades coerentes.
3. Volume, tendências e comparações não usam médias ou percentuais matematicamente enganosos.
4. Recordes usam somente atividades e métricas elegíveis.
5. Distância de recorde é a distância final efetivamente persistida para o tipo de treino.
6. Detecção ocorre somente depois do save/mutação persistida apropriada.
7. Resave sem alteração não repete conquista.
8. Melhoria real de uma atividade que já era recordista pode gerar nova conquista.
9. Atividade antiga só gera destaque se virar o melhor valor atualmente conhecido.
10. Múltiplos recordes do mesmo save são apresentados juntos e sem diálogo bloqueante.
11. Falha de avaliação/apresentação de recorde não desfaz save bem-sucedido.
12. Gráficos usam a infraestrutura da Spec 028.
13. Nenhum fluxo BLE, Health Connect read, Room schema ou granularidade D4 é alterado apenas para suportar a feature.
14. Nenhum teste automatizado é criado ou executado sem solicitação explícita.

## Plano de execução

- `029-01` — auditar métricas, elegibilidade e pontos de persistência — `completed`;
- `029-02` — criar contratos e read model de evolução — `completed`;
- `029-03` — implementar períodos e filtros por esporte — `completed`;
- `029-04` — implementar volume e consistência — `completed`;
- `029-05` — implementar tendências e fórmulas agregadas — `completed`;
- `029-06` — implementar comparação entre períodos — `completed`;
- `029-07` — implementar read model e avaliador de recordes pessoais — `completed`;
- `029-08` — criar tela Evolução e superfície de recordes — `completed`;
- `029-09` — integrar detecção e destaque de recorde pós-save — `completed`;
- `029-10` — validar, consolidar e fechar — `completed`.

## Fechamento factual

- A evolução permanece read model derivado, sem entidade, DAO, migration ou
  agregado materializado em Room.
- Fórmulas finais usam somas protegidas para volume, ponderação por amostras
  efetivas para BPM e razões agregadas para cadência, ritmo e velocidade; gaps,
  ausência e inaplicabilidade permanecem distinguíveis.
- Recordes usam métricas finais persistidas, segregação por modalidade e leitura
  all-history; conquista continua evento efêmero pós-save, sem bloquear a
  persistência.
- Nenhuma feature importa Vico diretamente; gráficos passam pela abstração
  `core/designsystem/chart`.
- Nenhum fluxo phone, GPS, Health Connect read, nova granularidade D4 ou score
  fisiológico foi introduzido.
- Validação física sanitizada no `SM-S901E` confirmou navegação Exercícios →
  Evolução, períodos 7/30/90, filtros Corrida/Bicicleta, resumo, rolagem,
  lacunas, distância manual de bicicleta sem passos de performance e conclusão
  de revisão pós-save sem bloqueio.
- Novo recorde observável foi confirmado com corrida de `20,00 km`, diferença de
  `10,00 km`, e banner pós-calibração automaticamente reposicionado no topo.
  Re-save idempotente, múltiplos recordes, calibração de esteira, abertura de
  recordista e tema claro/fonte ampliada ficaram `N/A` nesta sessão por ausência
  de cenário específico preparado.
