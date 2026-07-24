# Direção da integração nativa com Health Connect da Fase 12

## Contexto

A Fase 12 implementa a primeira integração real do Champion Companion com o Health Connect.

As fases anteriores já entregaram:

- resumo diário com passos persistidos;
- medição manual de frequência cardíaca;
- histórico D1 persistido;
- operações `PENDING` para medições D1;
- importação D3/D4;
- revisão de atividades;
- título e observações;
- tipo confirmado;
- calibração de esteira;
- atividades em `READY_TO_SYNC`;
- descarte lógico;
- Room como fonte local;
- preferência `healthSyncEnabled`;
- estrutura inicial de `HealthSyncOperation`.

A Fase 12 deve transformar essa preparação em uma integração funcional e idempotente.

## Estado documental a reconciliar

Considere como fato autoritativo que a Fase 11 foi concluída.

Entretanto, no estado atual do repositório:

- o overview da Fase 11 ainda registra `011-12` e `011-13` como pendentes;
- a baseline factual ainda descreve a Fase 10 como última fase concluída;
- os documentos de fechamento precisam ser alinhados antes de usar a baseline como entrada da Fase 12.

A primeira task da Fase 12 deve:

- confirmar o estado real entregue;
- fechar documentalmente a Fase 11;
- atualizar baseline, overview, checklist e lacunas;
- não reabrir a implementação funcional da Fase 11.

## Objetivo

Implementar um pipeline local-first:

```text
Persistência local
        ↓
Operação de sincronização
        ↓
Disponibilidade e permissão
        ↓
Mapeamento Health Connect
        ↓
Upsert idempotente
        ↓
SYNCED ou FAILED
```

O banco local permanece como fonte operacional e visual primária.

Health Connect é um destino derivado.

## Regra absoluta sobre testes

Não criar, alterar, expandir, executar ou planejar testes automatizados.

Não:

- criar arquivos em `app/src/test`;
- criar arquivos em `app/src/androidTest`;
- alterar testes existentes;
- criar task de testes;
- executar `testDebugUnitTest`;
- adicionar cobertura;
- incluir testes na Definition of Done;
- incluir testes nos critérios de aceite.

A validação usa build, instalação, inspeção manual e Health Connect real.

## SDK

Adicionar ao catálogo de versões:

```text
androidx.health.connect:connect-client:1.1.0
```

Usar a versão estável.

Não adotar versão alpha sem requisito funcional comprovado e ADR específica.

## Princípios

### Local-first

Uma falha no Health Connect nunca deve:

- desfazer persistência local;
- apagar BPM;
- apagar resumo diário;
- apagar atividade;
- desfazer revisão;
- desfazer calibração;
- alterar métricas importadas;
- bloquear o uso offline do aplicativo.

### Escrita unidirecional

A Fase 12 escreve dados locais no Health Connect.

Não implementar leitura de dados produzidos por outros aplicativos.

Não usar Health Connect para popular:

- Home;
- Saúde;
- Exercícios;
- histórico;
- perfil.

### Permissões mínimas

Solicitar apenas permissões de escrita.

Não solicitar permissões de leitura nesta fase.

As permissões devem ser agrupadas por capacidade, sem exigir todas para habilitar uma sincronização parcial.

### Sem dependência de BLE

A escrita no Health Connect acontece depois da operação BLE ser encerrada.

Fluxo obrigatório:

```text
Buscar no relógio
        ↓
Persistir no Room
        ↓
Encerrar collectors
        ↓
Liberar ForegroundBleOperationGate
        ↓
Tentar Health Connect
```

Health Connect não deve manter:

- conexão BLE;
- permit do gate;
- observer F008;
- collector F002;
- timeout BLE;
- sessão GATT.

## Escopo

A Fase 12 inclui:

- SDK Health Connect;
- disponibilidade;
- provider ausente ou desatualizado;
- permissões;
- rationale de privacidade;
- configuração de manifesto;
- preferência de sincronização;
- IDs estáveis;
- versionamento;
- fila local;
- reconciliação de backlog;
- passos;
- frequência cardíaca manual;
- frequência cardíaca D1;
- sessão de exercício;
- distância da atividade;
- calorias da atividade;
- frequência cardíaca da atividade;
- peso persistido;
- altura persistida;
- update idempotente;
- retry manual;
- recuperação de operações interrompidas;
- estados visuais;
- sincronização explícita de atividades;
- tentativa automática após coleta explícita de passos e BPM;
- Settings funcional;
- Gate C físico.

## Fora do escopo

A Fase 12 não inclui:

- leitura do Health Connect;
- importação de dados de outros apps;
- Change Logs;
- WorkManager;
- retry em background;
- foreground service;
- sincronização periódica;
- sincronização ao abrir o app;
- sincronização agressiva;
- rota;
- GPS;
- `ExerciseRoute`;
- coordenadas;
- mapa;
- exclusão ampla de dados;
- edição de perfil;
- histórico de peso completo;
- exercícios iniciados pelo app;
- testes automatizados.

WorkManager e reenvio adiável pertencem à Fase 14.

## Arquitetura

```text
Home / Saúde / Exercícios / Configurações
                    ↓
        HealthConnectSyncCoordinator
                    ├── HealthSyncRepository
                    ├── HealthConnectGateway
                    ├── HealthRecordMapper
                    ├── DailySummaryRepository
                    ├── HeartRateRepository
                    ├── WorkoutActivityRepository
                    ├── PersonalMetricsRepository
                    └── AppPreferencesRepository
```

Fronteiras:

- Compose não acessa `HealthConnectClient`;
- ViewModel não cria records;
- mapper não acessa Room;
- gateway não interpreta domínio de produto;
- repository não acessa APIs Android do Health Connect;
- coordinator controla fila e transições;
- Hilt fornece todas as dependências.

## Disponibilidade

Modelar pelo menos:

```text
Available
ProviderUpdateRequired
Unavailable
```

Comportamento:

### Available

- permitir consultar permissões;
- permitir conexão;
- permitir sincronização.

### ProviderUpdateRequired

- mostrar orientação;
- oferecer abertura da loja ou configurações apropriadas;
- manter dados locais pendentes.

### Unavailable

- mostrar estado seguro;
- não solicitar permissão;
- não tentar escrita;
- preservar a fila.

## Manifesto

Adicionar as permissões de escrita necessárias aos tipos suportados.

Adicionar consulta ao package do provider para Android 13 ou inferior.

Adicionar uma Activity para rationale de permissões.

Adicionar configuração compatível com:

- Android 13 ou inferior;
- Android 14 ou superior.

A Activity de rationale deve explicar:

- quais dados são enviados;
- por que são enviados;
- que o banco local permanece como fonte;
- que nenhuma rota é enviada;
- como pausar a sincronização;
- como gerenciar permissões;
- como os dados são tratados.

Não alterar `allowBackup=false`.

## Política de permissões

### Passos

Exige somente permissão de escrita de passos.

### BPM manual e D1

Exige somente permissão de escrita de frequência cardíaca.

### Atividade

A sessão exige permissão de escrita de exercício.

Complementos exigem permissões próprias:

- distância;
- calorias;
- frequência cardíaca.

A ausência de uma permissão complementar não deve impedir automaticamente o envio da sessão.

### Peso e altura

Solicitar as permissões somente quando:

- existir dado local elegível;
- o usuário solicitar sincronização dessa capacidade;
- ou o fluxo futuro de Perfil ativar essa sincronização.

Não pedir peso e altura apenas porque o aplicativo abriu.

## Preferência

Reutilizar:

```text
AppPreferences.healthSyncEnabled
```

Evoluir `AppPreferencesRepository` com operação específica equivalente a:

```text
updateHealthSyncEnabled(enabled)
```

### Desativada

- não processar operações automaticamente;
- manter operações `PENDING` ou `FAILED`;
- não revogar permissões automaticamente;
- não apagar records existentes;
- permitir gerenciamento manual de acesso.

### Ativada

- verificar disponibilidade;
- solicitar permissões em contexto explícito;
- reconciliar backlog;
- processar os tipos autorizados.

## Gatilhos

### Passos

```text
Pull-to-refresh da Home
        ↓
FEA1 válido
        ↓
mergeWatchDailySummary
        ↓
garantir operação STEPS
        ↓
liberar BLE
        ↓
tentar sincronização
```

A tentativa somente ocorre quando:

- o merge local teve sucesso;
- `healthSyncEnabled` está ativo;
- a permissão de passos está concedida.

### BPM manual

```text
Medição manual concluída
        ↓
resultado final persistido
        ↓
garantir operação HEART_RATE
        ↓
liberar BLE
        ↓
tentar sincronização
```

### BPM D1

```text
Lote D1 persistido
        ↓
operações PENDING garantidas
        ↓
liberar BLE
        ↓
processar backlog HEART_RATE
```

Não permitir seleção individual dos pontos D1.

### Atividades

Atividades não sincronizam automaticamente ao alcançar `READY_TO_SYNC`.

Fluxo:

```text
READY_TO_SYNC
        ↓
usuário toca Sincronizar
        ↓
criar ou reparar operações
        ↓
processar bundle da atividade
```

Pode existir também uma ação explícita:

```text
Sincronizar atividades prontas
```

desde que a ação seja iniciada conscientemente pelo usuário.

### Peso e altura

Nesta fase:

- reconciliar dados locais existentes ao habilitar ou usar `Sincronizar agora`;
- preparar contratos para que a Fase 16 enfileire após salvar;
- não criar novos formulários de perfil.

## Reconciliação de backlog

Ao habilitar a integração ou tocar `Sincronizar agora`, reconciliar:

- operações D1 existentes;
- medições manuais válidas sem operação;
- resumos diários válidos sem operação;
- peso persistido elegível;
- altura persistida elegível.

Não enfileirar automaticamente atividades `READY_TO_SYNC` durante uma reconciliação genérica.

Atividades exigem ação explícita própria.

## Tipos de operação

Evoluir `HealthSyncRecordType` para incluir:

```text
STEPS
HEART_RATE
EXERCISE_SESSION
DISTANCE
TOTAL_CALORIES_BURNED
WEIGHT
HEIGHT
```

Evoluir `HealthSyncAggregate` para incluir conceitos equivalentes a:

```text
DailySummary
HeartRateMeasurement
WorkoutActivity
WeightMeasurement
UserProfileHeight
```

## Estado da operação

Manter:

```text
PENDING
SYNCING
SYNCED
FAILED
```

Adicionar metadados persistidos equivalentes a:

```text
clientRecordId
clientRecordVersion
syncedAt
```

Manter:

```text
attemptCount
lastAttemptAt
lastError
```

Avaliar também:

```text
createdAt
updatedAt
```

quando necessários para ordenação e diagnóstico.

## Migration

O banco está atualmente em `v4`.

Planejar migration para a próxima versão.

A migration deve contemplar:

- expansão de `health_sync_operations`;
- campos de identidade estável;
- versão;
- data de sincronização;
- índices;
- possível timezone do resumo diário.

Operações D1 existentes não podem ser perdidas.

Quando campos novos não puderem ser calculados diretamente pela migration:

- adicioná-los como nullable;
- executar repair idempotente no repository;
- preencher antes de processar a operação.

Atualizar schema exportado.

## Timezone dos passos

`DailySummary` possui uma data local, mas precisa produzir um intervalo inequívoco no Health Connect.

A auditoria deve decidir entre:

1. persistir `ZoneId` no resumo diário;
2. persistir o intervalo resolvido na operação de sync;
3. congelar o timezone na primeira criação da operação.

Não recalcular silenciosamente o mesmo dia em outro timezone em retries futuros.

Para dados novos, preferir persistir o timezone usado na coleta.

## Fila

Criar contratos equivalentes a:

```text
ensureOperation(...)
findProcessable(...)
observeSyncSummary(...)
findOperationsForAggregate(...)
markStarted(...)
markSynced(...)
markFailed(...)
resetInterrupted(...)
```

Regras:

- chave natural por agregado e tipo de record;
- operação duplicada não é criada;
- `SYNCED` não volta para `PENDING` sem nova versão local;
- nova versão local reabre a operação;
- operações `FAILED` podem ser repetidas;
- operações `SYNCING` abandonadas após morte de processo voltam a um estado recuperável;
- processamento tem limite;
- não criar loop infinito.

Limite foreground inicial recomendado:

```text
100 operações por execução
```

## IDs estáveis

Usar formato determinístico equivalente a:

```text
<applicationId>:<aggregateType>:<aggregateId>:<recordType>
```

Exemplos conceituais:

```text
br.com...:daily-summary:<id>:steps
br.com...:heart-rate:<id>:heart-rate
br.com...:workout:<id>:exercise-session
br.com...:workout:<id>:distance
```

Regras:

- não usar valor aleatório por tentativa;
- não usar timestamp atual como ID;
- retries usam o mesmo ID;
- debug e release permanecem separados pelo application ID;
- ID não contém BPM, passos, distância ou informação pessoal.

## Versões

### Resumo diário

Usar uma versão monotônica derivada de:

```text
updatedAt
```

### BPM

Como a medição é imutável, usar versão estável derivada do registro local ou versão inicial fixa.

### Atividade

Usar versão derivada da revisão mais recente entre:

- importação;
- revisão;
- calibração.

### Peso

Usar `measuredAt` ou versão local equivalente.

### Altura

Usar `updatedAt` do perfil.

Uma atualização local deve produzir versão maior que a anteriormente enviada.

## Upsert

Preferir:

```text
insertRecords
```

com:

```text
clientRecordId
clientRecordVersion
```

como caminho idempotente de insert/update.

Não depender da leitura prévia do record remoto.

Não apagar Steps por intervalo antes de inserir.

Não excluir records de outros aplicativos.

## Mapeamento de passos

Origem:

```text
DailySummary
```

Record:

```text
StepsRecord
```

Regras:

- valor não negativo;
- uma operação por resumo diário;
- início no começo do dia local congelado;
- para o dia corrente, fim no instante coletado;
- para dia encerrado, fim no início do dia seguinte;
- mesmo `clientRecordId` em todos os refreshes;
- versão cresce conforme o resumo é atualizado;
- repetição não duplica.

Não criar `StepsRecord` separado para atividades D3/D4.

Os passos do treino já fazem parte do resumo diário do relógio e um segundo record poderia produzir dupla contagem.

A atividade pode continuar exibindo os passos localmente.

## Mapeamento de BPM manual

Origem:

```text
MANUAL_MEASUREMENT
```

Record:

```text
HeartRateRecord
```

Regras:

- uma amostra;
- timestamp exato da medição;
- metadata de medição iniciada pelo usuário;
- device identificado como relógio quando aplicável;
- uma operação por medição;
- retry idempotente.

## Mapeamento D1

Origem:

```text
WATCH_D1_HISTORY
```

Record:

```text
HeartRateRecord
```

Regras:

- enviar todos os registros válidos;
- uma operação por medição persistida;
- uma amostra por operação;
- metadata de origem automática do relógio;
- manter timestamp exato;
- não enviar somente média;
- não selecionar pontos manualmente;
- reparar operações D1 antigas sem identidade nova.

## Mapeamento da atividade

### Sessão

Record:

```text
ExerciseSessionRecord
```

Usar:

- início;
- fim;
- tipo confirmado;
- título;
- observações.

Exigir:

- atividade em `READY_TO_SYNC`, `SYNC_FAILED` ou fluxo explícito equivalente;
- tipo confirmado;
- duração positiva;
- início e fim válidos.

Não inventar duração mínima para atividade com duração zero.

### Tipo

Criar mapeamento explícito e documentado para:

```text
WALKING
RUNNING
TREADMILL
FREE_WORKOUT
```

Quando não houver correspondência exata, usar o tipo genérico mais conservador.

Não mapear `FREE_WORKOUT` para treino de força sem decisão de produto documentada.

### Distância

Record:

```text
DistanceRecord
```

Usar a distância final:

```text
1. calibrada;
2. original;
3. estimada;
4. ausente.
```

Para esteira, usar a distância calibrada quando existente.

Não criar record quando o valor estiver ausente, não finito ou não positivo.

### Calorias

Record:

```text
TotalCaloriesBurnedRecord
```

Criar apenas quando:

- valor presente;
- finito;
- positivo;
- permissão concedida.

### Frequência cardíaca da atividade

Record:

```text
HeartRateRecord
```

Usar todas as amostras válidas dentro do intervalo da sessão.

Não substituir amostras reais apenas por média.

Quando não houver amostras:

- omitir o record;
- não criar amostras artificiais a partir da média.

### Passos da atividade

Não criar record separado.

A política evita dupla contagem com `DailySummary`.

### Rota

Nunca criar:

```text
ExerciseRoute
```

Nunca enviar:

- latitude;
- longitude;
- altitude;
- polyline;
- GPX;
- TCX;
- FIT.

## Granularidade da atividade

Uma atividade pode produzir operações independentes:

```text
EXERCISE_SESSION
DISTANCE
TOTAL_CALORIES_BURNED
HEART_RATE
```

`EXERCISE_SESSION` é obrigatória.

Os complementos só existem quando há dado local elegível.

Ordem recomendada:

1. sessão;
2. distância;
3. calorias;
4. frequência cardíaca.

Cada operação usa seu próprio `clientRecordId`.

## Resultado parcial

É possível que a sessão seja gravada e um complemento falhe.

Nessa situação:

- não apagar a sessão já gravada;
- marcar somente a operação complementar como `FAILED`;
- preservar as demais como `SYNCED`;
- atividade recebe estado geral de falha ou pendência;
- UI informa que a sincronização foi parcial;
- retry processa apenas operações ainda pendentes ou falhas;
- IDs estáveis impedem duplicação.

A atividade só vai para:

```text
SYNCED
```

quando todas as operações elegíveis estiverem sincronizadas.

Durante o processamento:

```text
READY_TO_SYNC ou SYNC_FAILED
        ↓
SYNCING
```

Falha total ou parcial:

```text
SYNCING
        ↓
SYNC_FAILED
```

Sucesso integral:

```text
SYNCING
        ↓
SYNCED
```

## Peso

Record:

```text
WeightRecord
```

Origem:

- medidas persistidas em `weight_measurements`.

Regras:

- valor positivo e finito;
- timestamp persistido;
- metadata de entrada manual quando aplicável;
- uma operação por medida;
- não usar `Instant.now()` como substituto silencioso.

## Altura

Record:

```text
HeightRecord
```

Origem:

- altura persistida no perfil.

Regras:

- converter centímetros para metros;
- valor positivo e finito;
- usar `updatedAt`;
- uma operação para a versão atual do perfil;
- não solicitar permissão quando não houver altura local.

## Gateway

Criar interface Android-free equivalente a:

```text
HealthConnectGateway
```

Operações:

```text
getAvailability()
getGrantedPermissions()
insert(records)
deleteByClientRecordIds(...)
openProviderUpdate()
openManageAccess()
```

A implementação Android concreta encapsula:

```text
HealthConnectClient
PermissionController
Intent de provider
Intent de gerenciamento
```

Delete pode ser implementado como capacidade de infraestrutura, mas nenhum fluxo de exclusão ampla é obrigatório nesta fase.

## Coordinator

Criar componente equivalente a:

```text
HealthConnectSyncCoordinator
```

Responsabilidades:

- reconciliar operações;
- verificar `healthSyncEnabled`;
- verificar disponibilidade;
- verificar permissões por record;
- marcar operação iniciada;
- carregar agregado;
- mapear record;
- executar upsert;
- marcar sucesso;
- marcar falha;
- atualizar workflow de atividade;
- retornar resultado seguro;
- limitar execução;
- não executar BLE.

## Resultado do sync

Planejar resultado tipado equivalente a:

```text
Completed
CompletedWithFailures
NothingToSync
PermissionRequired
ProviderUnavailable
Disabled
Failed
```

Resumo seguro:

```text
processedCount
syncedCount
failedCount
pendingCount
skippedBecausePermissionCount
```

Não incluir valores de saúde no resultado diagnóstico.

## Erros seguros

Evoluir `SafeSyncError` para cobrir pelo menos:

```text
PERMISSION_REQUIRED
PROVIDER_UNAVAILABLE
PROVIDER_UPDATE_REQUIRED
INVALID_LOCAL_DATA
WRITE_REJECTED
RETRY_LATER
INTERRUPTED
INTERNAL
```

Não persistir:

- stack trace;
- mensagem completa do provider;
- BPM;
- passos;
- distância;
- peso;
- altura;
- título;
- observações.

## Operação interrompida

Ao iniciar o aplicativo ou abrir Configurações:

- localizar operações antigas em `SYNCING`;
- retornar para `FAILED` com erro seguro `INTERRUPTED`;
- não assumir que o record remoto não foi criado;
- retry usa o mesmo `clientRecordId`;
- upsert idempotente reconcilia o estado.

Atividades em `SYNCING` sem operação ativa também devem voltar para `SYNC_FAILED`.

## Configurações

`SettingsScreen` deve deixar de ser placeholder.

Criar seção Health Connect com:

- disponibilidade;
- integração ativada ou pausada;
- permissões concedidas;
- permissões faltantes;
- ação Conectar;
- ação Pausar sincronização;
- ação Sincronizar agora;
- ação Gerenciar acesso;
- provider desatualizado;
- operações pendentes;
- operações com falha;
- última sincronização.

A tela não deve mostrar dados de saúde individuais.

## Permissões na UI

Exibir as permissões por grupos amigáveis:

```text
Passos
Frequência cardíaca
Exercícios
Distância
Calorias
Peso
Altura
```

Não mostrar constantes Android ao usuário.

Negar uma permissão não apaga dados locais.

## Saúde

Depois de uma medição manual ou importação D1:

- resultado local continua sendo a mensagem principal;
- sincronização aparece como estado secundário;
- falha no Health Connect não transforma importação local em falha.

Exemplos:

```text
12 medições importadas
10 sincronizadas
2 aguardando nova tentativa
```

Não criar seleção individual de D1.

## Home

Depois do pull-to-refresh:

- passos locais atualizados imediatamente;
- estado do Health Connect aparece separadamente;
- falha de sync não desfaz o resumo;
- pull-to-refresh não fica carregando enquanto Health Connect processa.

Não executar sync antes de liberar o gate BLE.

## Exercícios

Na tela de detalhe:

### READY_TO_SYNC

Exibir:

```text
Sincronizar com Health Connect
```

### SYNCING

Exibir progresso ou estado de envio.

### SYNCED

Exibir data da sincronização.

### SYNC_FAILED

Exibir:

- erro seguro;
- quais grupos permanecem pendentes;
- ação Tentar novamente.

A lista pode mostrar status resumido.

## Lifecycle

A sincronização foreground pode continuar enquanto o ViewModel responsável estiver ativo.

Não iniciar serviço.

Não iniciar Worker.

Em morte do processo:

- fila persiste;
- operações em andamento são reparadas;
- dados locais permanecem;
- retry continua idempotente.

## Logging

Pode registrar:

- disponibilidade;
- categoria da permissão;
- quantidade de operações;
- tipo de record;
- transição de estado;
- duração da execução;
- categoria do erro.

Não registrar:

- valores;
- records completos;
- metadata completa;
- clientRecordId completo;
- IDs locais completos;
- título;
- notas;
- payload;
- dados pessoais.

## Privacidade

A Fase 12 deve preservar:

- armazenamento local privado;
- `allowBackup=false`;
- ausência de servidor;
- ausência de rota;
- ausência de dados sensíveis em logs.

Registrar documentação para futura declaração de aplicativo de saúde no Google Play.

Solicitar somente os tipos usados por funcionalidades visíveis.

## ADR

Criar ADR para formalizar:

- Room como fonte primária;
- Health Connect como destino derivado;
- escrita unidirecional;
- gatilhos de passos e BPM;
- atividade somente com ação explícita;
- identidade por `clientRecordId`;
- versão monotônica;
- não enviar passos da atividade separadamente;
- granularidade por record;
- tratamento parcial;
- permissões por capacidade;
- ausência de WorkManager na Fase 12.

Usar o próximo número disponível.

## Roadmap

Atualizar a Fase 12 para registrar:

- tentativa automática de passos após pull-to-refresh persistido;
- tentativa automática de BPM manual após persistência;
- tentativa automática do backlog D1 após importação;
- atividade somente após solicitação explícita;
- não criar Steps separado para atividade;
- fila granular;
- `clientRecordId` e versão;
- peso/altura somente quando houver dados locais;
- write-only;
- sem WorkManager;
- sem leitura de outros apps.

## Documentação de baseline

Atualizar:

- baseline atual;
- mapeamento Health Connect;
- checklist de paridade;
- lacunas;
- database reference;
- Fase 11, quando ainda estiver marcada como incompleta.

## Validação

Não executar testes.

Gate obrigatório:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Gate C

Validar no Galaxy S22 com Health Connect real.

### Disponibilidade

1. abrir Configurações;
2. confirmar disponibilidade;
3. confirmar provider ausente/desatualizado quando reproduzível;
4. abrir gerenciamento de acesso.

### Permissões

5. conceder somente passos;
6. atualizar passos;
7. confirmar Steps no Health Connect;
8. negar BPM;
9. confirmar que BPM local continua preservado;
10. conceder BPM posteriormente;
11. executar retry.

### Passos

12. fazer pull-to-refresh;
13. confirmar persistência local imediata;
14. confirmar sync após liberar BLE;
15. repetir refresh;
16. confirmar update sem duplicação;
17. fechar e reabrir o app;
18. confirmar estado persistido.

### BPM manual

19. realizar medição manual;
20. confirmar BPM local;
21. confirmar record no Health Connect;
22. repetir o processamento;
23. confirmar ausência de duplicação.

### D1

24. importar D1;
25. confirmar todas as medições válidas;
26. confirmar processamento do backlog anterior;
27. negar permissão durante retry;
28. confirmar pendência preservada;
29. conceder e concluir.

### Atividade

30. abrir uma atividade `READY_TO_SYNC`;
31. confirmar que não sincroniza automaticamente;
32. tocar Sincronizar;
33. confirmar sessão;
34. confirmar distância;
35. confirmar calorias;
36. confirmar BPM;
37. confirmar ausência de Steps separado;
38. confirmar ausência de rota;
39. repetir envio;
40. confirmar ausência de duplicação.

### Esteira

41. sincronizar atividade calibrada;
42. confirmar uso da distância calibrada;
43. confirmar que a distância original local permanece preservada.

### Falha parcial

44. revogar uma permissão complementar;
45. sincronizar atividade;
46. confirmar sessão sincronizada e complemento pendente;
47. restaurar permissão;
48. repetir;
49. confirmar conclusão sem duplicar sessão.

### Preferência

50. pausar sincronização;
51. coletar passos ou BPM;
52. confirmar fila local sem tentativa;
53. reativar;
54. sincronizar backlog.

### Recuperação

55. interromper o processo durante sync quando reproduzível;
56. reabrir o app;
57. confirmar reparo de `SYNCING`;
58. tentar novamente;
59. confirmar idempotência.

### Peso e altura

60. quando houver dados locais elegíveis, confirmar records;
61. quando não houver, confirmar que nenhuma permissão ou operação desnecessária é criada.

### Privacidade

62. confirmar ausência de dados de saúde no Logcat;
63. confirmar ausência de rota;
64. confirmar `allowBackup=false`;
65. registrar screenshots das telas alteradas.

## Critérios de aceite

A Fase 12 será concluída quando:

- SDK estável estiver integrado;
- disponibilidade funcionar;
- provider desatualizado for tratado;
- rationale existir;
- Settings deixar de ser placeholder;
- preferência pausar e retomar funcionar;
- permissões forem solicitadas por capacidade;
- fila suportar todos os tipos planejados;
- operações D1 existentes forem preservadas;
- `clientRecordId` for estável;
- versões forem monotônicas;
- passos sincronizarem após refresh persistido;
- refresh repetido não duplicar;
- BPM manual sincronizar após persistência;
- D1 processar todo backlog válido;
- atividades não sincronizarem automaticamente;
- atividade `READY_TO_SYNC` sincronizar por ação explícita;
- sessão usar tipo, título e observações confirmados;
- esteira usar distância calibrada;
- distância, calorias e BPM forem granulares;
- passos de atividade não forem enviados separadamente;
- falha parcial puder ser retomada;
- morte do processo não perder fila;
- peso e altura sincronizarem quando houver dados elegíveis;
- nenhuma rota for enviada;
- Room continuar como fonte visual;
- nenhum teste for criado, alterado ou executado;
- lint, debug e release concluírem;
- instalação funcionar;
- Gate C for validado fisicamente;
- documentação refletir o estado final.
