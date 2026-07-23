# Direção da importação de treinos D3/D4 da Fase 10

## Contexto

A Fase 10 transforma o protocolo D3/D4 do Champion C001 em um fluxo de produto capaz de importar atividades gravadas no relógio.

As fases anteriores já entregaram:

- aplicativo Android nativo;
- Room e DataStore;
- modelos de domínio para atividades;
- entidades de atividade, importação, intervalos e amostras de BPM;
- persistência transacional inicial;
- deduplicação por dispositivo, origem e CRC;
- payload diagnóstico privado;
- política de retenção de payload diagnóstico;
- núcleo BLE nativo;
- fila GATT serial;
- conexão controlada;
- handshake A1;
- associação persistente;
- gate compartilhado para operações BLE foreground;
- sincronização manual de passos;
- medição manual de BPM;
- importação D1;
- metadata genérica por tipo de importação;
- tela de Exercícios ainda como placeholder.

A Fase 10 deve entregar:

```text
Ação explícita do usuário
        ↓
Consulta D3
        ↓
Índice de CRCs
        ↓
Downloads D4 sequenciais
        ↓
Remontagem
        ↓
Parser
        ↓
Persistência local
        ↓
Atividade pendente
```

## Objetivo

Permitir ao usuário:

- solicitar manualmente a busca de treinos do relógio;
- acompanhar a consulta do índice D3;
- acompanhar o download dos blocos D4;
- cancelar;
- preservar blocos concluídos antes de uma falha;
- tentar novamente;
- retomar a importação sem repetir blocos válidos desnecessariamente;
- importar treino curto;
- importar treino longo e multipart;
- armazenar métricas agregadas;
- armazenar intervalos;
- armazenar amostras válidas de BPM;
- identificar importações completas e parciais;
- evitar duplicação;
- visualizar um resumo das atividades pendentes;
- manter toda atividade importada fora do Health Connect até revisão posterior.

## Princípio central

Importar não significa revisar, confirmar ou sincronizar.

A Fase 10 é responsável por:

```text
relógio
  → leitura
  → parsing
  → validação
  → persistência
  → atividade pendente
```

A Fase 11 será responsável por:

```text
atividade pendente
  → revisão
  → alteração de tipo
  → título e observações
  → calibração
  → confirmação
  → READY_TO_SYNC
```

A Fase 12 será responsável pelo Health Connect real.

A Fase 10 não deve:

- confirmar o tipo sugerido;
- marcar atividade como revisada;
- calibrar esteira;
- alterar título;
- alterar observações;
- criar operação do Health Connect;
- enviar atividade;
- marcar atividade como sincronizada;
- permitir envio automático.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento;
3. ADRs vigentes;
4. spec da Fase 10;
5. implementação e documentos concluídos da Fase 09;
6. implementação e documentos concluídos das Fases 03 a 08;
7. `docs/protocol/workouts-d3-d4.md`;
8. demais documentos em `docs/protocol/`;
9. roadmap;
10. MotionLog apenas como referência funcional.

## Divergência documental conhecida

`AGENTS.md` atualmente orienta a leitura de:

```text
docs/core/current-project-baseline.md
```

Esse caminho não existe no estado atual do repositório.

A baseline factual atual está em:

```text
docs/baseline/current-baseline.md
```

Durante o planejamento:

- considerar `docs/baseline/current-baseline.md` como baseline atual;
- não alterar `AGENTS.md`;
- criar na primeira task da Fase 10 uma correção documental pequena para alinhar o caminho;
- não permitir que essa divergência bloqueie o planejamento.

## Prioridade de desenvolvimento

A prioridade é entregar o fluxo funcional D3/D4.

Não criar task exclusiva de testes.

Não exigir:

- meta de cobertura;
- campanha ampla de testes Compose;
- expansão geral dos fakes;
- instrumentação de todos os estados;
- correção completa da suíte antiga;
- regressão completa das fases anteriores;
- reprodução automatizada de todas as variações possíveis do firmware.

Continuam obrigatórios:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Também são obrigatórios:

- validação física no Galaxy S22;
- Champion C001 real;
- treino curto;
- treino longo ou multipart;
- correção de regressões introduzidas pela Fase 10;
- screenshot das mudanças visuais conforme `AGENTS.md`.

Testes pequenos devem acompanhar as tasks responsáveis por regras críticas.

## Escopo

A Fase 10 inclui:

- auditoria da baseline MotionLog;
- correção do caminho da baseline em `AGENTS.md`;
- contratos de importação;
- owner BLE específico;
- comando D3;
- parser D3;
- collector D3;
- índice de CRCs;
- comando D4;
- collector D4;
- remontagem;
- controle sequencial;
- parser de cabeçalhos e amostras;
- layouts preferido e legado já documentados;
- timestamps;
- duração;
- passos;
- distância total;
- distância por intervalo;
- calorias;
- BPM por intervalo;
- BPM médio, mínimo e máximo;
- sugestão de tipo;
- validade completa, parcial ou inválida;
- persistência;
- deduplicação;
- reimportação;
- retomada;
- payload diagnóstico privado;
- limpeza dos payloads;
- metadata da importação;
- ViewModel de Exercícios;
- tela de importação;
- lista simples e somente leitura de pendências;
- loading;
- progresso;
- cancelamento;
- retry;
- estados de erro;
- validação física;
- atualização do protocolo.

## Fora do escopo

A Fase 10 não inclui:

- revisão de atividade;
- edição do tipo;
- confirmação do tipo;
- título;
- observações;
- calibração;
- edição de distância;
- descarte definitivo pela interface;
- filtros completos de atividades;
- tela completa de detalhes;
- Health Connect;
- `HealthSyncOperation` para atividades;
- sincronização automática;
- D3 ou D4 ao abrir o aplicativo;
- D3 ou D4 ao abrir a tela;
- D3 ou D4 em background;
- foreground service;
- WorkManager;
- GPS;
- localização;
- rota;
- coordenadas;
- mapa;
- D8;
- A8;
- suporte a outros relógios;
- campanha ampla de testes.

## Arquitetura esperada

```text
ExercisesScreen
        ↓
ExercisesViewModel
        ├── WorkoutActivityRepository
        ├── WatchWorkoutImportMetadataRepository ou contrato equivalente
        └── WatchWorkoutImportCoordinator
                  ├── ForegroundBleOperationGate
                  ├── DeviceConnectionBootstrap
                  ├── ChampionConnectionManager
                  ├── ChampionNotificationHub / transport streams
                  ├── ChampionD3IndexProtocol
                  ├── ChampionD4BlockProtocol
                  ├── ChampionD4WorkoutParser
                  ├── WorkoutActivityRepository
                  ├── DiagnosticPayloadRepository
                  └── metadata da importação
```

Fronteiras:

- Compose não acessa BLE;
- Compose não acessa Room;
- Compose não interpreta bytes;
- ViewModel não acessa `BluetoothGatt`;
- ViewModel não implementa parser;
- parser não acessa Room;
- parser não controla conexão;
- repository não acessa BLE;
- coordinator não depende de Compose;
- todos os writes passam pela fila GATT;
- dependências são fornecidas por Hilt.

## Ação explícita

A tela Exercícios deve oferecer uma ação equivalente a:

```text
Buscar treinos no relógio
```

A importação não pode iniciar porque:

- o aplicativo abriu;
- a tela Exercícios abriu;
- o relógio conectou;
- o auto-connect terminou;
- o usuário retornou ao foreground;
- a Home foi atualizada;
- uma atividade pendente foi observada.

## State machine

Planejar estado explícito semelhante a:

```text
Idle
Preparing
Connecting
ReadingIndex
ReadingBlocks
Parsing
Persisting
Completed
Partial
Cancelled
Failed
```

O estado pode carregar:

- ID da tentativa;
- ID da sessão BLE;
- instante inicial;
- quantidade de CRCs;
- bloco atual;
- total de blocos;
- pacote atual;
- total de pacotes do bloco;
- quantidade importada;
- quantidade duplicada;
- quantidade parcial;
- quantidade inválida;
- quantidade com timeout;
- erro seguro.

Evitar booleanos independentes contraditórios.

## Gate BLE

Adicionar owner equivalente a:

```text
WATCH_WORKOUT_IMPORT
```

O gate deverá arbitrar:

```text
DAILY_STEPS_REFRESH
MANUAL_HEART_RATE
WATCH_D1_HISTORY
WATCH_WORKOUT_IMPORT
```

Regras:

- aquisição atômica;
- apenas um owner;
- rejeição segura quando outra operação estiver ativa;
- liberação em `finally`;
- cancelamento libera;
- timeout libera;
- erro de persistência libera;
- callback tardio não readquire;
- retry usa nova tentativa e novo permit;
- tela Dispositivo deve respeitar o novo owner;
- auto-connect não deve derrubar a sessão utilizada pela importação.

## Conexão

Ao iniciar a importação:

1. verificar dispositivo associado;
2. verificar permissões BLE;
3. verificar Bluetooth;
4. adquirir o gate;
5. reutilizar sessão funcional compatível;
6. conectar de forma controlada quando necessário;
7. garantir `TransportReady`;
8. garantir handshake A1;
9. observar F008;
10. iniciar D3.

Não:

- conectar outro relógio;
- iniciar scan amplo sem relação com o associado;
- entrar em retry infinito;
- solicitar permissão ao abrir a tela;
- derrubar sessão saudável sem necessidade.

## Identidade da tentativa

Cada tentativa deve possuir:

```text
importAttemptId
bleSessionId
deviceId
startedAt
parserVersion
```

Cada collector D3 ou D4 deve validar:

- tentativa ainda ativa;
- mesma sessão BLE;
- mesmo dispositivo;
- pacote recebido depois do início;
- canal F008 notify;
- comando esperado.

Callbacks tardios devem ser ignorados.

## D3 — requisição

Enviar:

```text
D3 01 00 00
```

Canal:

```text
F008 write
```

Resposta:

```text
F008 notify
```

Timeout inicial:

```text
10 segundos
```

Retry automático interno:

```text
nenhum
```

## D3 — parser

O parser D3 deve:

- aceitar somente comando `0xD3`;
- copiar os bytes defensivamente;
- interpretar CRCs como `uint16` little-endian;
- ignorar `0x0000`;
- preservar a ordem de aparição;
- atribuir `blockIndex` sequencial iniciado em zero;
- eliminar entradas duplicadas de forma determinística;
- não confundir CRC com índice externo;
- não atribuir semântica a campos não confirmados.

A implementação deve portar fielmente a regra confirmada pelo MotionLog.

Não assumir que o cabeçalho multipart D3 possui exatamente o mesmo layout de D1 sem evidência.

A spec deve documentar claramente:

- offsets do cabeçalho usado;
- início real dos pares de CRC;
- comportamento sem cabeçalho plausível;
- comportamento com pacote curto;
- comportamento com CRC repetido.

## D3 — conclusão

Quando existir cabeçalho multipart plausível:

- `totalPackets` entre 1 e 256;
- `packetIndex` entre 1 e `totalPackets`;
- encerrar quando `packetIndex >= totalPackets`.

Quando não existir cabeçalho plausível:

- a primeira notificação D3 válida pode encerrar a coleta, conforme baseline.

### Timeout D3 com CRCs

- preservar CRCs já encontrados;
- continuar para D4;
- marcar tentativa como potencialmente parcial;
- informar timeout no resultado;
- permitir nova tentativa posterior.

### Timeout D3 sem CRCs

- encerrar sem importar atividade;
- mostrar erro ou estado sem treinos;
- permitir retry manual.

### Índice vazio

Índice D3 válido, mas sem CRCs:

- considerar consulta concluída;
- mostrar `Nenhum treino novo encontrado`;
- não tratar como falha técnica;
- não criar atividade;
- não criar payload falso.

## D4 — requisição

Para cada entrada D3:

```text
D4 <blockIndex> 00 00
```

Regras:

- `blockIndex` é a posição sequencial produzida pelo D3;
- não usar o CRC como `blockIndex`;
- o comando utiliza o byte menos significativo;
- índice fora do intervalo suportado deve gerar erro seguro;
- executar um bloco por vez;
- não criar comandos D4 concorrentes.

## D4 — timeout

Timeout inicial por bloco:

```text
15 segundos
```

Não criar retry automático oculto.

Quando um bloco falhar:

- registrar o resultado daquele bloco;
- limpar collector;
- continuar os próximos blocos quando a sessão permanecer saudável;
- permitir nova tentativa manual;
- não abortar todos os blocos por uma falha isolada;
- não repetir blocos válidos já persistidos sem necessidade.

## D4 — collector

O collector deve:

- aceitar somente comando `0xD4`;
- ler `packetIndex` em `bytes[1..2]` little-endian;
- ler `totalPackets` em `bytes[3..4]` little-endian;
- rejeitar pacote curto;
- armazenar cópia defensiva;
- aceitar pacotes fora de ordem;
- identificar índices duplicados;
- ordenar por `packetIndex`;
- terminar quando `totalPackets > 0` e `packetIndex >= totalPackets`;
- calcular índices ausentes;
- remover observer em todos os caminhos.

Não usar quantidade recebida como substituto automático de `totalPackets`.

## D4 — pacotes obrigatórios

Pacotes:

```text
1
2
3
```

são obrigatórios para criar uma atividade.

Ausência ou falha estrutural de qualquer um desses pacotes:

- não criar `WorkoutActivity`;
- não inventar métricas;
- registrar falha segura;
- preservar payload diagnóstico quando permitido;
- continuar os próximos blocos.

## D4 — layouts

A implementação deve preservar os layouts já documentados:

- layout preferido;
- fallback legado.

Esses layouts são heurísticas confirmadas pelo código do MotionLog, mas ainda precisam de validação física no Champion Companion.

Não:

- remover fallback antes da validação;
- promover heurística a fato físico sem evidência;
- introduzir terceiro layout especulativo;
- reinterpretar bytes desconhecidos.

## Pacote 1

Resolver:

- início;
- fim;
- duração.

Regras:

- validar datas;
- exigir fim posterior ao início quando disponível;
- usar duração fallback somente conforme baseline;
- segundo final ausente usa zero;
- não corrigir datas inválidas com valores inventados;
- timestamps persistidos usam `Instant`.

## Pacote 2

Resolver quando válido:

- passos totais;
- distância total;
- calorias totais.

Regras:

- valores não negativos;
- unidades documentadas;
- `DomainValue.Absent` quando indisponível;
- não substituir total por soma de intervalos sem validação explícita.

## Pacote 3

Resolver:

- quantidade esperada de amostras;
- CRC reportado pelo bloco;
- duração fallback, quando layout legado exigir.

Quando CRC D3 e CRC D4 divergirem:

- manter o CRC D3 como chave externa da tentativa;
- marcar resultado como parcial ou inconsistente;
- vincular diagnóstico;
- não criar segunda atividade usando o CRC divergente;
- não inventar qual valor é correto;
- atualizar protocolo após evidência física.

## Pacotes 4+

Cada pacote representa uma amostra por intervalo.

Extrair quando confirmado:

- `minuteIndex`;
- BPM;
- passos;
- distância;
- calorias;
- bytes diagnósticos conhecidos apenas como campos opacos.

Não persistir bytes opacos nas entidades derivadas.

## Intervalos

A transformação de pacote D4 para `WorkoutInterval` deve definir claramente:

- índice;
- duração;
- passos;
- distância;
- calorias.

O planejamento deve auditar a baseline MotionLog para confirmar:

- duração padrão dos intervalos;
- tratamento do último intervalo;
- comportamento quando duração total não for múltipla de um minuto;
- relação entre `recordCount` e intervalos.

Não inventar duração de intervalo sem evidência.

## Amostras de BPM

Criar `WorkoutHeartRateSample` somente para BPM:

```text
30..220
```

Valores:

```text
0
255
fora da faixa
```

devem ser ignorados.

O timestamp da amostra deve ser calculado conforme regra confirmada na baseline.

O planejamento deve documentar:

- relação entre `minuteIndex` e `sampledAt`;
- tratamento do primeiro minuto;
- tratamento de amostras fora da duração total;
- deduplicação por timestamp.

## BPM agregado

Calcular apenas sobre amostras válidas:

- média;
- mínimo;
- máximo.

Sem amostra válida:

- campos ficam ausentes;
- não usar zero;
- não inventar BPM.

## Passos

Preservar:

- total importado;
- passos por intervalo.

A comparação entre soma dos intervalos e total é diagnóstico.

Divergência isolada não deve substituir automaticamente o total importado.

## Distância

Preservar:

- distância total original;
- distância por intervalo;
- valor estimado quando realmente existente;
- distância calibrada ausente nesta fase.

Não preencher:

- distância calibrada;
- fator de calibração;
- distância manual.

A comparação entre soma de intervalos e total pode usar a tolerância já documentada, mas permanece diagnóstica.

## Calorias

Quando confirmada a unidade:

```text
calories = caloriesRaw / 1000
```

Regras:

- preservar total;
- preservar intervalos;
- uma casa decimal conforme baseline;
- ausência permanece ausente;
- divergência entre soma e total permanece diagnóstico.

## Sugestão de tipo

Usar somente como sugestão:

```text
menos de 1.000 passos → FREE_WORKOUT
1.000 passos ou mais → RUNNING
```

Não:

- confirmar automaticamente;
- sugerir `TREADMILL` sem regra confirmada;
- sugerir `WALKING` por inferência nova;
- impedir alteração futura.

A Fase 11 será responsável pela confirmação.

## Validade do parser

Usar:

```text
VALID
PARTIAL
INVALID
```

### VALID

- pacotes obrigatórios válidos;
- estrutura reconhecida;
- quantidade de índices compatível;
- sem duplicidade relevante;
- amostras compatíveis com `recordCount`;
- sem timeout que comprometa o bloco.

### PARTIAL

Pode ocorrer quando:

- houve timeout após receber dados suficientes;
- existem índices ausentes;
- existem índices duplicados;
- quantidade de amostras diverge de `recordCount`;
- existe inconsistência de CRC;
- existe outra inconsistência que não impede a criação segura dos dados principais.

### INVALID

- pacote obrigatório ausente;
- data irrecuperável;
- estrutura não reconhecida;
- dados mínimos insuficientes;
- parser não consegue produzir métricas de domínio válidas.

## Política para atividades parciais

Uma atividade `PARTIAL` deve ser persistida quando:

- pacotes 1, 2 e 3 são interpretáveis;
- início e duração são válidos;
- dados mínimos permitem criar uma atividade;
- a inconsistência não exige inventar valores.

Ela deve nascer:

```text
workflowState = PENDING_REVIEW
parserValidity = PARTIAL
```

Regras:

- permanece visível como incompleta;
- pode ser reimportada;
- não pode avançar para `READY_TO_SYNC`;
- não cria Health Connect;
- não é apresentada como importação totalmente concluída;
- Fase 11 deverá respeitar o bloqueio existente para parser parcial.

Atividade `INVALID` não deve ser criada.

## Deduplicação

Chave natural:

```text
deviceId
origin = WATCH_D3_D4
crc
```

Regras:

- mesmo treino não duplica;
- reimportação atualiza saída do parser;
- atividade existente mantém identidade;
- decisões do usuário já realizadas permanecem autoritativas;
- tipo confirmado não é substituído;
- calibração não é apagada;
- workflow revisado ou sincronizado não é rebaixado;
- intervalos e amostras derivadas podem ser substituídos transacionalmente;
- CRC repetido no mesmo D3 não cria dois downloads desnecessários.

## Retomada

Retomada não significa restaurar uma sessão GATT morta.

Uma nova tentativa explícita deve:

1. executar D3 novamente;
2. obter a lista atual de CRCs;
3. consultar a persistência local;
4. pular blocos já importados como `VALID`, salvo necessidade explícita de reprocessamento;
5. tentar novamente blocos ausentes, parciais ou falhos;
6. preservar deduplicação.

Não persistir:

- observer ativo;
- sessão BLE;
- comando em andamento;
- índice de pacote em memória como sessão restaurável.

## Persistência

Reutilizar preferencialmente:

- `WorkoutActivity`;
- `WatchWorkoutImport`;
- `WorkoutInterval`;
- `WorkoutHeartRateSample`;
- `WorkoutActivityRepository.persistImported(...)`;
- `DiagnosticPayload`;
- metadata genérica por tipo de importação.

A persistência deve ser transacional.

Para novo treino:

1. criar atividade;
2. criar registro de importação;
3. inserir intervalos;
4. inserir amostras;
5. vincular diagnóstico quando aplicável.

Para reimportação:

1. localizar chave natural;
2. preservar decisões do usuário;
3. atualizar parser e métricas importadas;
4. substituir intervalos;
5. substituir amostras;
6. preservar calibração;
7. preservar workflow autoritativo.

## Health Connect

A Fase 10 não cria:

- `HealthSyncOperation`;
- fila de sessão;
- permissão;
- client record;
- escrita;
- sincronização.

Fluxo correto:

```text
D3/D4
  → Room
  → PENDING_REVIEW
```

Não:

```text
D3/D4
  → Room
  → Health Connect
```

## Payload diagnóstico privado

O projeto já possui:

```text
DiagnosticPayload
DiagnosticCaptureFlow.WATCH_WORKOUT
DiagnosticRetention
```

Retenção vigente:

```text
máximo de 14 dias
máximo de 10 MiB
```

Regras:

- bytes permanecem privados;
- não aparecem na UI;
- não aparecem no Logcat;
- não entram no relatório sanitizado;
- não são exportados;
- backup continua desabilitado;
- limpeza deve ser aplicada no fluxo de importação ou diagnóstico;
- registros derivados não são apagados quando payload expira.

Política recomendada:

### Release

Persistir payload bruto somente para:

- `PARTIAL`;
- `INVALID`;
- erro técnico relevante.

### Debug e validação física

Permitir captura explícita também para bloco `VALID`, exclusivamente para investigação local.

A decisão definitiva deve ser auditada na task de contratos.

Caso represente mudança duradoura de privacidade, criar ADR.

## Fixtures

Não versionar payload real com dados pessoais.

Fixtures devem ser:

- sintéticas;
- sanitizadas;
- com datas artificiais;
- com BPM artificial;
- com passos e distâncias artificiais;
- estruturalmente equivalentes;
- documentadas como artificiais.

Não copiar diretamente pacote real do usuário para o repositório.

## Metadata da importação

Reutilizar a tabela genérica de metadata por:

```text
deviceId
importType
```

Tipo sugerido:

```text
WATCH_WORKOUT_D3_D4
```

Registrar quando disponível:

- última tentativa;
- último sucesso;
- resultado;
- parser version;
- inseridos;
- duplicados;
- conflitos;
- inválidos;
- índices ausentes;
- timeout.

Não reutilizar metadata D1.

Não tratar `DeviceImportMetadata.lastImportedAt` genérico como fonte exclusiva de D3/D4.

## Resultado tipado da tentativa

Planejar resultado semelhante a:

```text
Completed
Partial
Empty
Cancelled
Failed
```

Metadados seguros:

```text
discoveredBlockCount
processedBlockCount
insertedCount
duplicateCount
partialCount
invalidCount
timedOutBlockCount
skippedValidCount
missingPacketCount
startedAt
finishedAt
```

Não incluir:

- BPM;
- passos;
- distância;
- calorias;
- payload;
- CRC na UI diagnóstica geral.

## Progresso

### D3

Usar progresso indeterminado.

### D4

Depois de obter o índice:

```text
overallProgress = blocos processados / blocos encontrados
```

Dentro do bloco, quando `totalPackets` for válido:

```text
blockProgress = packetIndex / totalPackets
```

Regras:

- limitar a `0..1`;
- não inventar percentual antes do D3;
- não usar progresso para decidir validade;
- mostrar bloco atual de forma amigável;
- não exibir CRC.

## Tela Exercícios

`ExercisesScreen` deve deixar de ser placeholder.

### Estado ocioso

Exibir:

- título;
- explicação curta;
- ação `Buscar treinos no relógio`;
- última importação;
- resumo de atividades pendentes;
- estado sem dispositivo quando aplicável.

### Importando

Exibir:

- etapa atual;
- progresso;
- blocos processados;
- ação `Cancelar`;
- conteúdo anterior preservado;
- navegação controlada.

### Concluída

Exibir:

- quantidade importada;
- quantidade já existente;
- quantidade parcial;
- quantidade com erro;
- orientação de que as atividades precisam ser revisadas;
- ação para nova busca.

### Lista simples

Exibir somente informações suficientes para confirmar a importação:

- data;
- duração;
- tipo sugerido;
- validade;
- status pendente.

A lista nesta fase é somente leitura.

Não incluir:

- edição;
- confirmação;
- calibração;
- sincronização;
- filtros completos;
- detalhes completos.

A Fase 11 substituirá ou ampliará essa lista.

## Home

A Fase 11 continua responsável pelo banner completo de pendências.

Na Fase 10:

- não criar o fluxo completo de revisão na Home;
- não criar CTA de calibração;
- não criar filtros;
- não criar sincronização;
- é aceitável manter a Home inalterada.

Caso já exista contador de pendências reaproveitável sem ampliar escopo, ele pode ser atualizado, mas não é requisito de conclusão.

## Cancelamento

Ao cancelar:

1. encerrar collector ativo;
2. cancelar timeout;
3. não iniciar próximo bloco;
4. liberar gate;
5. preservar blocos já persistidos;
6. não persistir bloco incompleto sem dados mínimos;
7. manter atividade válida já concluída;
8. retornar a estado estável.

Cancelamento não deve apagar importações anteriores.

## Lifecycle

Ao sair da tela durante importação:

- solicitar confirmação ou cancelar explicitamente;
- não deixar operação invisível utilizando BLE.

Ao ir para background:

- cancelar;
- remover observers;
- liberar gate;
- não iniciar serviço;
- preservar blocos já persistidos.

Em morte do processo:

- não restaurar sessão BLE;
- retornar a `Idle`;
- manter dados já persistidos;
- nova tentativa deve usar retomada por CRC.

## Retry

Retry:

- inicia nova tentativa;
- executa D3 novamente;
- usa novo attempt ID;
- usa novo permit;
- não reaproveita listener;
- não reaproveita timeout;
- pula imports válidos;
- tenta novamente parciais e falhos;
- não duplica atividades.

## Erros seguros

Considerar:

- sem dispositivo associado;
- permissões ausentes;
- Bluetooth desligado;
- operação BLE concorrente;
- conexão falhou;
- handshake falhou;
- F008 indisponível;
- envio D3 falhou;
- D3 vazio;
- D3 inválido;
- timeout D3;
- envio D4 falhou;
- timeout D4;
- resposta D4 vazia;
- pacote obrigatório ausente;
- layout inválido;
- CRC divergente;
- persistência falhou;
- disconnect inesperado;
- cancelamento;
- armazenamento diagnóstico indisponível;
- erro interno.

Falha de diagnóstico bruto não deve necessariamente apagar atividade derivada válida.

## Logging

Pode registrar somente metadados seguros:

- início e fim;
- duração;
- etapa;
- quantidade de CRCs;
- número de blocos;
- quantidade de pacotes;
- índices ausentes;
- quantidade válida;
- quantidade parcial;
- quantidade inválida;
- quantidade inserida;
- quantidade duplicada;
- categoria de erro.

Não registrar:

- payload;
- bytes;
- hexadecimal;
- CRC;
- BPM;
- passos;
- distância;
- calorias;
- datas exatas da atividade;
- MAC;
- device ID persistente;
- stack trace destinado à UI.

## Privacidade

Não persistir:

- latitude;
- longitude;
- rota;
- polyline;
- mapa;
- GPX;
- TCX;
- FIT.

Não enviar dados para servidor.

Não alterar as regras de backup.

Payload diagnóstico permanece local, privado, limitado e temporário.

## Acessibilidade

Anunciar estados equivalentes a:

```text
Buscando treinos do relógio
Baixando treino
Treino importado
Importação parcial concluída
Nenhum treino novo encontrado
Importação cancelada
Não foi possível importar os treinos
```

O progresso não deve ser a única indicação.

Atividades parciais devem possuir descrição textual.

## Estratégia reduzida de testes

Não criar task exclusiva de testes.

Testes pequenos recomendados:

- builder D3;
- CRC little-endian;
- CRC zero ignorado;
- CRC duplicado;
- D3 sem header;
- D3 multipart;
- timeout D3 parcial;
- builder D4;
- índice D4;
- header D4 little-endian;
- pacotes fora de ordem;
- pacotes duplicados;
- índices ausentes;
- pacotes obrigatórios;
- layout preferido;
- layout legado;
- calorias por mil;
- distância;
- BPM válido e inválido;
- agregados BPM;
- sugestão de tipo;
- parser `PARTIAL`;
- parser `INVALID`;
- deduplicação por CRC;
- reimportação preserva revisão;
- retomada pula `VALID`;
- gate impede concorrência;
- callback tardio ignorado;
- cancelamento não inicia próximo bloco.

## Validação mínima

Gate obrigatório:

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Falhas antigas e não relacionadas devem ser documentadas.

Regressões da Fase 10 devem ser corrigidas.

## Validação física inicial

No Galaxy S22 com Champion C001:

1. abrir Exercícios;
2. confirmar ausência de importação automática;
3. tocar `Buscar treinos no relógio`;
4. conectar quando necessário;
5. observar D3;
6. confirmar quantidade de blocos;
7. observar D4 sequencial;
8. importar treino curto;
9. confirmar atividade pendente;
10. confirmar métricas básicas;
11. confirmar intervalos;
12. confirmar BPM quando disponível;
13. repetir importação;
14. confirmar ausência de duplicação;
15. cancelar durante D4;
16. confirmar cleanup;
17. tentar novamente;
18. confirmar retomada.

## Gate B

Executar com os mesmos treinos usados na baseline MotionLog.

Validar ao menos:

- um treino curto;
- um treino longo ou multipart;
- vários blocos na mesma consulta quando disponíveis;
- reimportação;
- cancelamento;
- disconnect durante D4;
- retry;
- atividade parcial quando reproduzível;
- ausência de Health Connect;
- ausência de dados sensíveis no Logcat.

A sequência final somente é aceita quando:

- D3 encontra os blocos;
- D4 baixa sequencialmente;
- parser produz dados coerentes;
- treino longo é remontado;
- atividade é persistida;
- duplicação é evitada;
- atividade permanece pendente;
- nenhum envio automático ocorre.

## Atualização do protocolo

Depois da validação física, atualizar:

```text
docs/protocol/workouts-d3-d4.md
```

Registrar:

- offsets fisicamente confirmados;
- layout observado;
- fallback realmente necessário;
- tamanho dos pacotes;
- ordem;
- comportamento multipart;
- relação D3 CRC e D4 CRC;
- unidades;
- duração;
- timestamp de amostras;
- timeout real;
- duplicatas;
- lacunas;
- comportamento de treino longo;
- divergências em relação ao MotionLog.

Não promover hipótese sem evidência.

## ADRs

Avaliar necessidade de ADR para:

- política de persistência de atividades `PARTIAL`;
- política de payload diagnóstico em release;
- separação entre Fase 10, Fase 11 e Fase 12;
- retomada baseada em CRC;
- metadata genérica de importações.

Não criar ADR apenas para repetir regras já suficientemente cobertas pelo roadmap, domínio e protocolos.

## Dívida técnica

Registrar para fases futuras:

- revisão e calibração na Fase 11;
- Health Connect na Fase 12;
- foreground service na Fase 14;
- importações longas com tela apagada na Fase 14;
- calendário na Fase 15;
- testes amplos na Fase 21;
- confirmação dos bytes ainda desconhecidos;
- confirmação de D8 e A8;
- fixtures adicionais;
- política de reprocessamento avançado.

## Critérios de aceite

A Fase 10 será concluída quando:

- `ExercisesScreen` deixar de ser placeholder;
- importação começar somente por ação explícita;
- owner `WATCH_WORKOUT_IMPORT` existir;
- gate impedir concorrência;
- D3 passar pela fila GATT;
- D3 produzir índice de CRCs;
- D4 executar sequencialmente;
- cada bloco possuir timeout;
- callbacks antigos forem ignorados;
- pacotes fora de ordem forem remontados;
- pacotes obrigatórios forem validados;
- layout preferido e legado forem preservados;
- passos forem importados;
- distância total for preservada;
- intervalos forem persistidos;
- calorias forem interpretadas conforme protocolo;
- BPM válido for persistido;
- agregados BPM forem calculados;
- sugestão de tipo permanecer editável futuramente;
- atividade `VALID` for persistida;
- atividade `PARTIAL` segura for persistida e bloqueada para sync;
- atividade `INVALID` não for inventada;
- deduplicação por dispositivo, origem e CRC funcionar;
- reimportação preservar decisões do usuário;
- retomada pular blocos válidos;
- cancelamento preservar blocos concluídos;
- payload diagnóstico obedecer retenção e privacidade;
- nenhuma operação do Health Connect for criada;
- atividade nascer pendente;
- nenhuma revisão ou calibração for implementada;
- treino curto real for importado;
- treino longo real for importado;
- Gate B for validado;
- debug e release compilarem;
- screenshot da tela for registrada;
- baseline e protocolo forem atualizados.
