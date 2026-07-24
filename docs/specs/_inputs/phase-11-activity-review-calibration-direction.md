# Direção da revisão e calibração de atividades da Fase 11

## Contexto

A Fase 11 transforma as atividades D3/D4 importadas na Fase 10 em atividades revisadas e prontas para sincronização futura.

As fases anteriores já entregaram:

- modelos de domínio para atividades;
- estados de workflow;
- métricas importadas;
- intervalos;
- amostras de frequência cardíaca;
- tipo sugerido;
- validade do parser;
- persistência local;
- importação D3/D4;
- deduplicação por dispositivo, origem e CRC;
- reimportação preservando decisões existentes;
- payload diagnóstico privado;
- lista somente leitura de atividades pendentes;
- tela Exercícios funcional;
- atividades importadas em `PENDING_REVIEW`.

A Fase 11 deve implementar:

```text
Atividade importada
        ↓
Revisão
        ↓
Tipo confirmado
        ↓
Título e observações
        ↓
Calibração quando necessária
        ↓
READY_TO_SYNC
```

A Fase 11 não implementa o Health Connect.

## Objetivo

Permitir ao usuário:

- visualizar atividades importadas;
- filtrar atividades por estado;
- abrir os detalhes de uma atividade;
- visualizar métricas importadas;
- visualizar intervalos;
- visualizar frequência cardíaca;
- visualizar gráfico de frequência cardíaca;
- comparar tipo sugerido e tipo confirmado;
- alterar o tipo;
- informar título;
- informar observações;
- confirmar a revisão;
- calibrar atividades classificadas como esteira;
- informar a distância real exibida pela esteira;
- visualizar distância final;
- visualizar ritmo e velocidade recalculados;
- descartar atividades inválidas ou indesejadas;
- visualizar atividades prontas para sincronização;
- acessar pendências diretamente pela Home;
- preservar todas as decisões após fechar e reabrir o aplicativo.

## Princípio central

Revisar não significa sincronizar.

A fronteira obrigatória é:

```text
Fase 10
  → importar
  → PENDING_REVIEW

Fase 11
  → revisar
  → calibrar
  → READY_TO_SYNC

Fase 12
  → Health Connect
  → SYNCING
  → SYNCED ou SYNC_FAILED
```

A Fase 11 não deve:

- acessar `HealthConnectClient`;
- solicitar permissões do Health Connect;
- criar records;
- enviar sessão;
- criar operação real de sincronização;
- marcar atividade como `SYNCING`;
- marcar atividade como `SYNCED`;
- marcar atividade como `SYNC_FAILED`;
- apresentar envio como funcional;
- executar sincronização automática.

## Correção obrigatória do roadmap

O roadmap atual ainda associa à Fase 11:

- ação de sincronizar;
- retry do Health Connect;
- critério de que o Health Connect recebe a distância calibrada.

Essa fronteira deve ser corrigida documentalmente.

A primeira parte da Fase 11 deve criar uma ADR e atualizar o roadmap para registrar que:

- a Fase 11 termina em `READY_TO_SYNC`;
- a Fase 12 é responsável pela sincronização;
- o resultado calibrado produzido pela Fase 11 será consumido pela Fase 12;
- a ausência do Health Connect não bloqueia revisão e calibração;
- atividades prontas permanecem locais até uma ação de sincronização implementada posteriormente.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento;
3. ADRs vigentes;
4. spec da Fase 11;
5. implementação e documentos concluídos da Fase 10;
6. implementação e documentos concluídos das Fases 03 a 09;
7. roadmap já corrigido;
8. documentos de protocolo;
9. MotionLog apenas como referência funcional.

## Regra sobre testes

Não criar testes.

Não:

- criar arquivos em `app/src/test`;
- criar arquivos em `app/src/androidTest`;
- alterar testes existentes;
- expandir testes existentes;
- criar task de testes;
- executar suites de testes como etapa da fase;
- adicionar cobertura automatizada aos critérios de aceite;
- adicionar testes à Definition of Done.

A validação deve usar:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Além de:

- validação manual;
- validação no Galaxy S22;
- atividades reais importadas na Fase 10;
- screenshots das mudanças visuais.

## Escopo

A Fase 11 inclui:

- ADR da fronteira de prontidão;
- atualização do roadmap;
- evolução do domínio de atividade;
- título;
- observações;
- revisão;
- confirmação do tipo;
- persistência do resultado da revisão;
- migração Room;
- queries de lista;
- query de detalhe;
- resumo de workflows;
- filtros;
- tela de detalhes;
- métricas;
- gráfico;
- intervalos;
- amostras de BPM;
- calibração de esteira;
- distância informada;
- fator de calibração;
- distância final;
- ritmo recalculado;
- velocidade recalculada;
- descarte local;
- Home com banner de pendências;
- navegação Home → Exercícios;
- navegação Exercícios → Detalhes;
- estados vazios;
- estados de erro;
- persistência após reinício;
- preservação de revisão durante reimportação;
- preparação local para a Fase 12.

## Fora do escopo

A Fase 11 não inclui:

- Health Connect;
- criação de `HealthSyncOperation` para atividades;
- envio de sessão;
- permissões do Health Connect;
- retry do provider;
- sincronização em lote;
- exercícios iniciados pelo aplicativo;
- GPS;
- localização;
- rota;
- mapa;
- foreground service;
- WorkManager;
- nova importação D3/D4 automática;
- alteração do parser D3/D4 sem defeito comprovado;
- histórico de calendário;
- exportação;
- backup;
- testes automatizados.

## Estado atual do domínio

O domínio atual possui:

```text
suggestedType
confirmedType
workflowState
reviewedAt
calibrationId
```

Ainda não possui:

```text
title
notes
```

Esses campos devem ser adicionados ao domínio e à persistência.

## Migração Room

Planejar uma nova versão do banco.

Adicionar em `WorkoutActivityEntity` campos equivalentes a:

```text
title: String?
notes: String?
```

Preferir campos opcionais para permitir uma migration segura sobre atividades já importadas.

Regras:

- atividade existente permanece válida;
- título ausente continua permitido;
- observações ausentes continuam permitidas;
- migration não cria valores artificiais;
- migration não altera workflow;
- migration não altera tipo;
- migration não altera calibração;
- migration não altera métricas;
- schema exportado deve ser atualizado.

Avaliar `AutoMigration` quando suportada pela alteração.

## Título

O título é opcional.

Quando não houver título personalizado, a interface deve usar um fallback derivado, por exemplo:

```text
Corrida
Treino livre
Caminhada
Esteira
Treino do relógio
```

O fallback:

- não precisa ser persistido;
- deve usar o tipo confirmado quando disponível;
- antes da confirmação pode usar o tipo sugerido;
- não deve substituir silenciosamente um título informado.

O planejamento deve auditar a baseline MotionLog para identificar limites já existentes.

Quando não existir limite anterior, definir constantes de domínio razoáveis na spec, evitando truncamento silencioso.

## Observações

As observações são opcionais.

Regras:

- preservar quebras de linha;
- remover somente espaços externos desnecessários;
- não aceitar conteúdo composto apenas por espaços;
- não registrar observações em logs;
- não incluir observações no diagnóstico;
- não enviar observações nesta fase.

A Fase 12 poderá usar as observações ao construir a sessão do Health Connect.

## Tipo sugerido e tipo confirmado

O tipo sugerido continua sendo apenas uma sugestão.

Tipos selecionáveis:

```text
WALKING
RUNNING
FREE_WORKOUT
TREADMILL
```

`UNKNOWN` não deve ser uma escolha final válida.

Ao abrir a revisão:

- mostrar o tipo sugerido;
- pré-selecionar o tipo sugerido quando não houver confirmação anterior;
- exigir uma confirmação explícita;
- não alterar `confirmedType` apenas por abrir a tela.

## Validade do parser

### VALID

A atividade pode ser revisada.

### PARTIAL

A atividade deve:

- permanecer visível;
- mostrar que possui dados incompletos;
- permitir visualização;
- permitir descarte;
- permitir nova importação pelo fluxo existente;
- bloquear confirmação;
- bloquear calibração;
- bloquear `READY_TO_SYNC`.

O usuário não pode transformar manualmente `PARTIAL` em `VALID`.

### INVALID

A Fase 10 não deve criar atividade inválida.

Caso um registro legado ou inconsistente seja encontrado:

- mostrar erro seguro;
- impedir revisão;
- permitir descarte quando seguro;
- não inventar dados.

## State machine

```text
PENDING_REVIEW
    ├── confirmar tipo não esteira
    │       ↓
    │   READY_TO_SYNC
    │
    ├── confirmar tipo esteira
    │       ↓
    │   PENDING_CALIBRATION
    │       ↓
    │   calibrar
    │       ↓
    │   READY_TO_SYNC
    │
    └── descartar
            ↓
        DISCARDED
```

Estados reservados à Fase 12:

```text
SYNCING
SYNCED
SYNC_FAILED
```

A Fase 11 pode exibi-los quando já existirem futuramente, mas não deve produzi-los.

## Confirmação da revisão

Criar contrato equivalente a:

```text
SubmitWorkoutReviewInput
```

Com:

```text
activityId
confirmedType
title
notes
reviewedAt
```

A operação deve ser transacional.

Para tipo diferente de esteira:

```text
PENDING_REVIEW
    ↓
READY_TO_SYNC
```

Para esteira:

```text
PENDING_REVIEW
    ↓
PENDING_CALIBRATION
```

Regras:

- atividade deve existir;
- estado deve ser `PENDING_REVIEW`;
- parser deve ser `VALID`;
- tipo final não pode ser `UNKNOWN`;
- `reviewedAt` não pode ser anterior à importação;
- título e observações devem respeitar os contratos definidos;
- falha não deve produzir atualização parcial.

## Edição antes da confirmação

Enquanto a atividade estiver em `PENDING_REVIEW`:

- tipo;
- título;
- observações;

podem permanecer como estado de formulário.

Não persistir rascunho em cada tecla nesta fase.

Persistir quando o usuário confirmar a revisão.

Ao sair com alterações não salvas:

- solicitar confirmação de descarte das alterações;
- não alterar a atividade silenciosamente.

## Edição depois da confirmação

Nesta fase, `READY_TO_SYNC` deve ser tratado como estado local concluído.

Não é obrigatório implementar reabertura da revisão.

A interface deve mostrar os dados confirmados como somente leitura.

Uma futura necessidade de reabertura após sincronização deverá ser planejada com as regras de update do Health Connect.

Não criar transições adicionais sem decisão explícita.

## Classificação e validade

Não confundir:

```text
tipo da atividade
```

com:

```text
validade do parser
```

O usuário pode alterar o tipo.

O usuário não pode alterar:

- `ParserValidity`;
- origem;
- CRC;
- métricas importadas;
- timestamps importados;
- diagnóstico;
- validade dos pacotes.

## Distância

Diferenciar:

```text
distância original importada
distância estimada
distância calibrada
distância final
```

Política de exibição da distância final:

```text
1. calibratedMeters, quando disponível;
2. originalMeters, quando disponível;
3. estimatedMeters, quando disponível;
4. indisponível.
```

A distância original nunca deve ser substituída.

A distância calibrada deve permanecer ao lado da original.

## Edição de distância

Nesta fase, a correção manual de distância é permitida somente pelo fluxo de calibração de esteira.

Para tipos:

- caminhada;
- corrida;
- treino livre;

a distância importada permanece somente leitura.

Não criar edição arbitrária de distância para atividades externas sem regra confirmada pela baseline.

## Calibração de esteira

Quando o usuário confirmar:

```text
confirmedType = TREADMILL
```

a atividade deve avançar para:

```text
PENDING_CALIBRATION
```

A interface deve solicitar:

```text
Distância exibida pela esteira
```

Exemplo:

```text
Distância importada: 4,72 km
Distância da esteira: 5,00 km
```

## Entrada da distância

A interface pode receber o valor em quilômetros.

Converter para metros antes de criar o domínio.

Regras:

- aceitar separador decimal adequado à localidade;
- exigir valor finito;
- exigir valor maior que zero;
- não aceitar NaN;
- não aceitar infinito;
- não arredondar antes da persistência;
- formatar apenas na exibição;
- mostrar mensagem de validação segura.

## Fonte para o fator

Quando houver distância original positiva:

```text
factor = reportedMeters / originalMeters
```

Quando não houver distância original positiva, mas houver estimativa positiva:

```text
factor = reportedMeters / estimatedMeters
```

Quando não existir distância-base positiva:

- aceitar a distância informada;
- deixar o fator indisponível;
- calcular ritmo e velocidade a partir da duração e da distância informada;
- não bloquear a calibração apenas pela ausência de fator.

O domínio atual deve ser evoluído para suportar atividades importadas sem distância-base positiva.

## Métricas recalculadas

Depois da calibração:

```text
distanceFinal = reportedMeters
averageSpeed = reportedMeters / durationSeconds
averagePace = durationSeconds × 1000 / reportedMeters
```

Quando a duração for zero:

- distância calibrada continua válida;
- ritmo fica indisponível;
- velocidade fica indisponível;
- não dividir por zero.

## Registro da calibração

A calibração deve ser um registro imutável.

Persistir:

- activity ID;
- revision;
- distância original;
- distância estimada;
- distância informada;
- distância calibrada;
- fator;
- data da aplicação;
- origem;
- ritmo derivado;
- velocidade derivada.

A atividade deve apontar para a calibração ativa.

A primeira calibração usa:

```text
revision = 1
```

O planejamento deve preservar suporte estrutural a revisões futuras, sem exigir interface completa de recalibração nesta fase.

## Persistência da calibração

Criar uma operação atômica equivalente a:

```text
calibrateImportedActivity(...)
```

Ela deve:

1. carregar a atividade;
2. validar `PENDING_CALIBRATION`;
3. determinar a próxima revisão;
4. criar a calibração;
5. persistir o registro;
6. atualizar a atividade;
7. preencher métricas calibradas;
8. avançar para `READY_TO_SYNC`.

Não deixar uma calibração persistida sem a atualização correspondente da atividade.

## Descarte

O descarte deve ser lógico:

```text
workflowState = DISCARDED
```

Não apagar:

- atividade;
- registro de importação;
- intervalos;
- amostras;
- calibração;
- diagnóstico.

Permitir descarte em:

```text
PENDING_REVIEW
PENDING_CALIBRATION
READY_TO_SYNC
```

Exigir confirmação explícita.

Após descarte:

- remover da lista padrão de pendências;
- permitir visualização pelo filtro de descartadas;
- não sincronizar;
- não criar operação de Health Connect.

## Reimportação

A reimportação D3/D4 deve continuar preservando:

- título;
- observações;
- tipo confirmado;
- workflow;
- reviewedAt;
- calibrationId;
- distância calibrada;
- métricas calibradas.

A Fase 11 deve revisar o plano de persistência da Fase 10 para garantir que os novos campos não sejam sobrescritos por valores nulos durante uma reimportação.

## Repository

Evoluir `WorkoutActivityRepository` com contratos equivalentes a:

```text
observeActivities(filter)
observeActivity(activityId)
observeWorkflowSummary()
submitReview(input)
discard(activityId)
```

Evoluir `TreadmillCalibrationRepository` com operação equivalente a:

```text
calibrateImportedActivity(input)
observeLatestForActivity(activityId)
```

A feature não deve acessar DAOs.

## Queries

As queries devem permitir:

- lista por estado;
- lista completa;
- detalhe por ID;
- atividade com intervalos;
- atividade com amostras de BPM;
- calibração ativa;
- resumo de quantidade por workflow;
- ordenação por data mais recente.

Não carregar todas as atividades para filtrar apenas na UI quando uma query clara puder ser usada.

## Filtros da lista

Filtros planejados:

```text
Todas
Aguardando revisão
Aguardando calibração
Prontas
Sincronizadas
Com erro
Descartadas
```

Mapeamento:

```text
Aguardando revisão → PENDING_REVIEW
Aguardando calibração → PENDING_CALIBRATION
Prontas → READY_TO_SYNC
Sincronizadas → SYNCED
Com erro → SYNC_FAILED
Descartadas → DISCARDED
```

`SYNCING` pode aparecer dentro de Todas ou em um estado visual temporário futuro.

A Fase 11 não cria registros sincronizados ou com erro.

## Tela Exercícios

A tela existente deve preservar a área de importação D3/D4.

Abaixo dela, criar a área de atividades.

Estrutura:

```text
Cabeçalho
Importação do relógio
Resumo de pendências
Filtros
Lista de atividades
```

A lista deve deixar de ser somente leitura.

Cada card deve:

- mostrar título ou fallback;
- mostrar data;
- mostrar duração;
- mostrar tipo sugerido ou confirmado;
- mostrar status;
- mostrar validade;
- mostrar distância;
- ser clicável;
- navegar para detalhes.

## Tela de detalhes

Criar rota específica equivalente a:

```text
ExerciseDetail(activityId)
```

A tela deve exibir:

### Cabeçalho

- título;
- status;
- tipo;
- data;
- origem;
- validade.

### Resumo

- duração;
- distância final;
- distância importada;
- passos;
- calorias;
- ritmo;
- velocidade;
- BPM médio;
- BPM mínimo;
- BPM máximo.

### Frequência cardíaca

- gráfico;
- estado sem amostras;
- resumo textual acessível.

### Intervalos

Para cada intervalo:

- índice;
- duração;
- passos;
- distância;
- calorias;
- BPM correspondente quando houver associação válida.

Não inventar BPM para intervalos sem amostra.

### Revisão

Quando `PENDING_REVIEW` e `VALID`:

- seletor de tipo;
- título;
- observações;
- confirmação.

### Calibração

Quando `PENDING_CALIBRATION`:

- distância importada;
- distância estimada;
- campo da distância da esteira;
- prévia de ritmo;
- prévia de velocidade;
- ação de confirmar calibração.

### Pronta

Quando `READY_TO_SYNC`:

- mostrar os dados finais;
- mostrar status `Pronta para sincronizar`;
- não mostrar ação funcional de sincronização;
- não citar número de fase na interface de produto.

### Parcial

Quando `PARTIAL`:

- mostrar aviso;
- bloquear confirmação;
- oferecer retorno à busca de treinos;
- permitir descarte.

### Descartada

Quando `DISCARDED`:

- mostrar estado somente leitura;
- não permitir revisão;
- não permitir calibração.

## Gráfico

Usar as amostras reais de `WorkoutHeartRateSample`.

Não adicionar biblioteca externa sem necessidade.

Preferir:

- Compose Canvas;
- padrões já usados no gráfico da área Saúde;
- componente compartilhado somente quando houver reutilização real.

O gráfico deve possuir alternativa textual.

## Intervalos e BPM

As amostras de BPM podem possuir timestamps, enquanto intervalos possuem índices.

Não associar uma amostra a um intervalo apenas pela posição da lista quando houver lacunas.

O planejamento deve definir associação usando:

- timestamp;
- início da atividade;
- duração dos intervalos;
- índice real;

conforme os dados persistidos.

Quando a associação não for segura, mostrar os intervalos e o gráfico separadamente.

## Home

Adicionar banner de pendências na Home.

O repository deve fornecer resumo equivalente a:

```text
pendingReviewCount
pendingCalibrationCount
readyToSyncCount
```

O banner deve aparecer quando houver:

- atividade aguardando revisão;
- atividade aguardando calibração.

Prioridade visual:

```text
1. calibração pendente;
2. revisão pendente.
```

Exemplos:

```text
1 atividade precisa de calibração
3 atividades aguardam revisão
```

Ao tocar:

```text
Home
  → Exercícios
```

Não abrir diretamente uma atividade específica sem decisão adicional.

Atividades `READY_TO_SYNC` podem aparecer em um resumo neutro, mas não devem gerar falsa indicação de sincronização disponível.

## Navegação

Planejar:

```text
Home
  → Exercícios
      → Detalhes da atividade
```

A navegação usa Navigation3 e back stack manual.

Não introduzir `NavController` ou `NavHost`.

A rota deve carregar somente o ID da atividade.

Não transportar o objeto completo pela navegação.

## Estado do ViewModel

O estado de Exercícios deve separar:

```text
importState
selectedFilter
activityList
workflowSummary
selectedActivity
reviewForm
calibrationForm
detailState
operationError
```

A importação D3/D4 e a revisão local são operações independentes.

Revisão e calibração:

- não usam BLE;
- não precisam do `ForegroundBleOperationGate`;
- não devem cancelar a sessão BLE global;
- devem funcionar offline.

## Estado dos formulários

O formulário de revisão deve distinguir:

- valor carregado;
- alteração local;
- salvando;
- erro;
- salvo.

O formulário de calibração deve distinguir:

- valor informado;
- erro de validação;
- prévia;
- salvando;
- concluído.

Não manter strings de formulário dentro do domínio.

## Concorrência

Impedir duplo submit.

Enquanto estiver salvando:

- desabilitar ação principal;
- não executar segunda revisão;
- não executar segunda calibração;
- não executar segundo descarte.

Operações do repository devem validar o estado atual novamente.

## Erros seguros

Considerar:

- atividade não encontrada;
- estado alterado por outra operação;
- parser parcial;
- tipo inválido;
- título inválido;
- observações inválidas;
- distância inválida;
- duração incompatível;
- calibração já aplicada;
- falha de persistência;
- atividade descartada;
- erro interno.

Não apresentar stack trace.

## Loading

A tela de detalhes deve representar:

```text
Loading
Ready
NotFound
Failed
```

Não mostrar formulário antes de carregar a atividade correta.

## Logging

Pode registrar apenas:

- operação iniciada;
- operação concluída;
- transição de workflow;
- categoria de tipo;
- presença ou ausência de calibração;
- categoria de erro.

Não registrar:

- título;
- observações;
- distância;
- passos;
- calorias;
- BPM;
- timestamps da atividade;
- payload;
- CRC;
- IDs persistentes.

## Privacidade

Não alterar:

- `allowBackup=false`;
- regras de backup;
- regras de device transfer.

Não persistir:

- localização;
- coordenadas;
- rota;
- mapa;
- GPX;
- TCX;
- FIT.

Título e observações permanecem locais até a futura sincronização autorizada.

## Acessibilidade

Anunciar estados equivalentes a:

```text
Atividade aguardando revisão
Atividade aguardando calibração
Revisão concluída
Calibração concluída
Atividade pronta para sincronizar
Atividade descartada
Não foi possível salvar a atividade
```

O gráfico não deve ser a única representação do BPM.

Campos devem possuir rótulos persistentes.

Erros de validação devem ser associados aos respectivos campos.

## Validação

Não criar nem executar testes.

Gate obrigatório:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Validação manual

No Galaxy S22, usando atividades importadas reais:

1. abrir a Home;
2. confirmar banner de pendências;
3. tocar no banner;
4. confirmar navegação para Exercícios;
5. alternar filtros;
6. abrir uma atividade;
7. visualizar métricas;
8. visualizar gráfico;
9. visualizar intervalos;
10. confirmar tipo sugerido;
11. alterar o tipo;
12. informar título;
13. informar observações;
14. confirmar atividade não esteira;
15. confirmar `READY_TO_SYNC`;
16. fechar e reabrir o app;
17. confirmar persistência;
18. revisar uma atividade como esteira;
19. confirmar `PENDING_CALIBRATION`;
20. informar distância da esteira;
21. confirmar distância final;
22. confirmar ritmo recalculado;
23. confirmar velocidade recalculada;
24. confirmar `READY_TO_SYNC`;
25. fechar e reabrir o app;
26. confirmar calibração persistida;
27. reimportar o mesmo treino;
28. confirmar preservação de título;
29. confirmar preservação das observações;
30. confirmar preservação do tipo;
31. confirmar preservação da calibração;
32. abrir uma atividade `PARTIAL`;
33. confirmar bloqueio da revisão;
34. descartar uma atividade;
35. confirmar presença no filtro Descartadas;
36. confirmar ausência de Health Connect;
37. confirmar ausência de novos arquivos de teste;
38. confirmar ausência de dados sensíveis no Logcat.

## ADR

Criar uma ADR para formalizar:

- a Fase 11 termina em `READY_TO_SYNC`;
- a Fase 12 controla sincronização;
- métricas importadas são imutáveis;
- calibração é uma camada derivada;
- título, observações e tipo confirmado são decisões do usuário;
- parser `PARTIAL` não pode ser confirmado;
- descarte é lógico;
- reimportação não apaga decisões do usuário.

Usar o próximo número disponível.

## Atualização do roadmap

Remover da Fase 11:

- ação funcional de sincronizar;
- retry do Health Connect;
- critério de envio ao Health Connect;
- responsabilidade por `SYNCING`;
- responsabilidade por `SYNCED`;
- responsabilidade por `SYNC_FAILED`.

Manter na Fase 11:

- revisão;
- título;
- observações;
- tipo;
- calibração;
- distância final;
- métricas derivadas;
- descarte;
- filtros;
- detalhes;
- banner da Home;
- `READY_TO_SYNC`.

Adicionar ou reforçar na Fase 12:

- consumir atividades `READY_TO_SYNC`;
- usar tipo confirmado;
- usar título;
- usar observações;
- usar distância calibrada;
- processar sessão, distância, calorias, passos e BPM;
- atualizar os estados de sincronização.

## Dívida técnica

Registrar para fases futuras:

- Health Connect na Fase 12;
- edição após sincronização;
- atualização de records existentes;
- exercícios iniciados pelo app;
- foreground service;
- calendário;
- exportação;
- cobertura automatizada somente mediante solicitação explícita do usuário.

## Critérios de aceite

A Fase 11 será concluída quando:

- ADR e roadmap estiverem alinhados;
- Home possuir banner de pendências;
- filtros funcionarem;
- cards forem clicáveis;
- tela de detalhes existir;
- métricas importadas forem exibidas;
- gráfico de BPM estiver funcional;
- intervalos forem exibidos;
- tipo sugerido estiver visível;
- tipo puder ser confirmado;
- título puder ser informado;
- observações puderem ser informadas;
- revisão não esteira produzir `READY_TO_SYNC`;
- revisão de esteira produzir `PENDING_CALIBRATION`;
- calibração produzir distância final;
- ritmo for recalculado;
- velocidade for recalculada;
- calibração produzir `READY_TO_SYNC`;
- atividade parcial não puder ser confirmada;
- descarte for lógico;
- descartadas puderem ser filtradas;
- reimportação preservar decisões;
- dados persistirem após reinício;
- nenhuma operação do Health Connect for criada;
- nenhum teste for criado ou alterado;
- lint, debug e release concluírem;
- instalação no Galaxy S22 funcionar;
- screenshots forem registradas.
