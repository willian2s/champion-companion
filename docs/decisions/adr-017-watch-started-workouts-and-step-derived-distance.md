# ADR-017 — Treinos iniciados pelo relógio e distância derivada de passos

## Status

Aceita como decisão documental preparatória para a reorientação de produto. Não
comprova implementação funcional, migração de dados ou validação física.

## Data

2026-08-05

## Contexto

A Fase 013 foi iniciada para treinos iniciados pelo celular, GPS, rota e
participação live do relógio. A execução deixou entregas parciais, bloqueios
físicos e infraestrutura persistida, mas a direção de produto foi reorientada.
O produto deve priorizar treinos gravados pelo Champion C001, que já possuem
protocolo de importação D3/D4, revisão local e sincronização controlada.

ADR-014 continua sendo o registro histórico da autorização estreita de rota e
do foreground service da sessão phone. Esta ADR sucede ADR-014 para decisões
futuras de produto sobre novos treinos e não autoriza remover migrations,
schemas, sessões, dados, contratos ou evidências existentes.

## Decisão

### 1. Relógio é owner temporal do treino

- O Champion C001 inicia e encerra o treino.
- O app não inicia sessão phone equivalente para substituir o relógio.
- D3/D4 são importados pelo app após ação explícita e permanecem fonte de
  proveniência do treino gravado.
- O app deriva métricas locais, permite revisão e só então prepara a
  sincronização controlada com Health Connect.

### 2. Distância canônica deriva de passos

A distância canônica do treino importado será derivada por:

```text
distância canônica = passos válidos × comprimento de passada pessoal
```

O comprimento de passada pessoal deve vir do perfil/configuração elegível. Se
passos ou comprimento pessoal não forem válidos, a distância canônica permanece
ausente; não há fallback silencioso.

A distância original informada pelo relógio é preservada somente como dado
diagnóstico e de comparação. Ela não substitui a distância canônica, não é
usada como fallback silencioso e não deve ser enviada como distância final sem
decisão específica posterior.

### 3. Revisão e Health Connect

- A atividade importada permanece local e revisável antes do sync.
- Tipo, duração, passos, distância derivada, calorias e BPM seguem proveniência
  e regras de revisão explícitas; ausência permanece ausência.
- A projeção futura para Health Connect deve ser granular por record, com
  identidade estável, retry idempotente e preservação de falhas parciais.
- O refinamento granular de Health Connect é trabalho futuro da direção
  redefinida da Fase 014; esta ADR não afirma que a projeção já foi implementada
  ou validada.

### 4. Sem GPS obrigatório, rota ou mapa

Treinos iniciados pelo relógio não exigem GPS, `ExerciseRoute`, persistência de
rota, mapa, tiles ou localização para calcular a distância canônica. Não há
expansão de GPS, rota ou `ExerciseRoute` em sessão phone como consequência desta
ADR.

### 5. WorkManager fora do objetivo funcional

WorkManager não define início, encerramento, importação interativa, revisão ou
sync imediato do treino. Pode ser considerado apenas detalhe técnico posterior
para tarefa finita e adiável, com política própria de retry e consentimento.
Não é objetivo funcional desta decisão nem controla sessão ativa.

## Relação com decisões anteriores

- **ADR-014:** sucedida para a direção futura de produto, sem apagar ou
  reescrever o histórico da autorização de rota/FGS phone. Dados e
  infraestrutura existentes permanecem preservados.
- **ADR-012:** mantém Room como fonte operacional, Health Connect write-only e
  sincronização explícita; o refinamento granular de treinos do relógio fica
  para trabalho futuro.
- **ADR-016:** mantém integridade temporal de reimportação D3/D4; esta ADR não
  altera migration, schema ou regras de `recordedAt`.

## Consequências

### Positivas

- reduz dependência de sessão phone, GPS e rota;
- preserva o relógio como fonte clara de início e fim;
- torna distância derivada determinística e dependente de dado pessoal explícito;
- mantém distância original disponível para diagnóstico sem contaminar o dado
  canônico;
- concentra evolução em importação, derivação, revisão e sync de treinos do
  relógio.

### Negativas e riscos

- comprimento de passada ausente ou desatualizado deixa distância sem valor;
- passos importados podem ter semântica ou completude variável entre firmwares;
- conversão da direção phone e dos pontos de entrada ainda exige operação
  explícita e não é afirmada como concluída;
- granularidade futura de Health Connect ainda exige contrato e validação
  próprios;
- infraestrutura de rota e sessões existentes aumenta superfície de retenção,
  mas deve ser preservada até decisão explícita de remoção.

## Alternativas consideradas

- **Usar distância original do relógio como canônica:** rejeitada para evitar
  misturar semânticas e preservar o dado original somente como diagnóstico.
- **Exigir GPS/rota no app:** rejeitada por não ser necessária para treino
  iniciado pelo relógio e por contrariar reorientação de privacidade e produto.
- **Usar WorkManager como owner do treino:** rejeitada; WorkManager trata apenas
  trabalho finito e adiável, não sessão ativa ou fluxo interativo.
- **Continuar corrigindo treinos phone antes da reorientação:** rejeitada por
  custo, gates físicos pendentes e decisão de produto registrada na Fase 013.

## Limites de implementação

Esta ADR preparou decisão e direção no momento de sua aprovação. Não declarava
que importação, derivação por passos, revisão, sincronização granular ou
desativação dos pontos de entrada phone já estivessem implementadas. A ausência
de implementação permaneceu visível nos documentos da Fase 013 e no roadmap
até a execução operacional de `013-17`.

### Atualização operacional de 2026-08-05

`013-17` implementou desativação fail-closed dos pontos de entrada e do runtime
phone, sem remover legado, dados, migrations, schemas ou evidências. Importação,
revisão e sincronização de treinos iniciados pelo relógio permanecem preservadas.
O aceite final da subtarefa foi confirmado por revisão independente.
