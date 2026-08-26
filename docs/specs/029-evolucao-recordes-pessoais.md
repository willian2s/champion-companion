# Spec 029 — Evolução esportiva e recordes pessoais

## Status

`planned`.

## Ticker

`029`.

## Contexto

O Champion Companion já possui histórico local de atividades importadas do Champion C001, métricas derivadas, calendário, históricos de saúde e uma infraestrutura unificada de gráficos interativos entregue pela Spec 028.

O app hoje responde bem **o que aconteceu em um treino ou período**, mas ainda não consolida de forma dedicada **como o treinamento está evoluindo**. A próxima evolução deve reutilizar os fatos locais já persistidos, sem criar novos sensores, scores fisiológicos ou dependência de leitura do Health Connect.

A funcionalidade também deve reconhecer recordes pessoais. A detecção não pode ocorrer durante a importação ou a partir do estado transitório do formulário: métricas como distância podem mudar durante revisão, distância manual de bicicleta, calibração de esteira ou rederivação explícita antes do sync.

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

Nenhuma média pode ser implementada como média ingênua de médias quando isso distorcer a métrica. A `029-01` deve registrar as fórmulas aceitas antes da implementação.

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

A primeira versão de recordes deve priorizar métricas factuais com semântica clara de conquista:

- maior distância final;
- maior duração;
- maior número de passos em treino elegível.

Recordes devem ser segregados por tipo de atividade quando misturar esportes produzir comparação enganosa. `CYCLING` não usa passos D4 como recorde de performance. Frequência cardíaca máxima e maior cadência não são recordes de performance nesta spec.

Pace/velocidade como recorde ficam fora da primeira versão até existir contrato comparável por distância/modalidade.

### R7 — Elegibilidade de recorde

Uma atividade só participa de recordes quando estiver em estado final local suficiente para suas métricas não dependerem de edição obrigatória pendente.

A auditoria deve consolidar os workflows exatos, mas a direção é:

- excluir `PENDING_REVIEW`;
- excluir `PENDING_CALIBRATION`;
- excluir `DISCARDED`;
- excluir tipo `UNKNOWN`;
- incluir estados revisados/finais compatíveis como `READY_TO_SYNC`, `SYNCED` e `SYNC_FAILED`, respeitando os guards atuais;
- `PARTIAL` pode participar quando o fluxo vigente permitir revisão final e a métrica específica for válida.

Cada record type também valida sua própria métrica: positiva/finita quando aplicável e proveniência correta para o tipo de treino.

### R8 — Detecção somente após persistência final

O alerta de novo recorde não ocorre na importação D3/D4 nem antes do commit da alteração.

Operações candidatas devem ser auditadas na `029-01`, incluindo pelo menos:

- confirmação de revisão de atividade não esteira;
- revisão de `CYCLING` com distância manual opcional;
- calibração de `TREADMILL` que conclui a distância final;
- rederivação explícita pré-sync que possa alterar métrica elegível.

A UI recebe resultado somente após a persistência bem-sucedida e recarregamento/obtenção do snapshot efetivamente salvo.

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
- a atividade atual é excluída da consulta das demais atividades para não comparar o novo valor consigo mesma.

Empates não geram novo recorde.

### R10 — Atividade antiga importada depois

O destaque pós-save representa **novo recorde atual conhecido no banco local**, não “recorde que existia na época cronológica da atividade”.

Uma atividade antiga importada hoje:

- gera destaque se superar todas as atividades elegíveis atualmente conhecidas no mesmo escopo;
- não gera destaque se já existir atividade conhecida com valor maior, mesmo que ela tenha ocorrido depois no tempo.

A tela de recordes mostra a data real da atividade recordista.

### R11 — Recordes como read model, conquista como evento efêmero

- A lista atual de recordes deve ser recalculável a partir das atividades elegíveis, evitando estado materializado obsoleto.
- O destaque pós-save é efeito efêmero de UI; não precisa virar entidade Room na primeira versão.
- Descartar ou alterar uma atividade antes do sync deve fazer o read model recalcular quem é o recordista atual.

### R12 — Apresentação pós-save

Quando houver uma ou mais conquistas novas, exibir destaque não bloqueante após o save:

```text
🏆 Novo recorde pessoal
Maior distância · Corrida
10,31 km
270 m acima do recorde anterior
```

Para múltiplos recordes, consolidar em um único componente em vez de abrir vários diálogos.

O destaque não deve impedir navegação nem converter sucesso de persistência em erro caso a apresentação falhe.

### R13 — Gráficos

Todos os novos gráficos usam `core/designsystem/chart` da Spec 028. Features não importam Vico diretamente nem criam novo engine Canvas para séries de dados.

### R14 — Sem novas inferências fisiológicas

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
13. Nenhum fluxo BLE, Health Connect, Room schema ou granularidade D4 é alterado apenas para suportar a feature.
14. Nenhum teste automatizado é criado ou executado sem solicitação explícita.

## Plano de execução

- `029-01` — auditar métricas, elegibilidade e pontos de persistência;
- `029-02` — criar contratos e read model de evolução;
- `029-03` — implementar períodos e filtros por esporte;
- `029-04` — implementar volume e consistência;
- `029-05` — implementar tendências e fórmulas agregadas;
- `029-06` — implementar comparação entre períodos;
- `029-07` — implementar read model e avaliador de recordes pessoais;
- `029-08` — criar tela Evolução e superfície de recordes;
- `029-09` — integrar detecção e destaque de recorde pós-save;
- `029-10` — validar, consolidar e fechar.
