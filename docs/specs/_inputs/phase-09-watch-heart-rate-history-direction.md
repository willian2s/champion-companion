# Direção do histórico D1 de frequência cardíaca da Fase 09

## Contexto

A Fase 09 transforma o histórico automático D1 do Champion C001 em uma funcionalidade local utilizável.

As fases anteriores já entregaram:

- núcleo BLE nativo;
- fila GATT serial;
- conexão e handshake A1;
- associação persistente do Champion C001;
- controle compartilhado de operações BLE em foreground;
- medição manual D0/F002;
- persistência de resultado manual;
- consulta da última medição válida;
- lista de medições manuais recentes;
- Home com último BPM persistido;
- Saúde como hub de métricas;
- tela específica de frequência cardíaca;
- espaço reservado para:
  - buscar dados do relógio;
  - gráfico;
  - histórico completo.

A Fase 09 deve implementar:

```text
Ação explícita do usuário
        ↓
Consulta D1 do relógio
        ↓
Parsing e validação
        ↓
Deduplicação
        ↓
Persistência local
        ↓
Operações pendentes para Health Connect
        ↓
Histórico e gráfico de frequência cardíaca
```

## Objetivo

Permitir ao usuário:

- tocar em `Buscar dados do relógio`;
- importar o histórico D1 disponível para o dia atual;
- acompanhar o progresso da consulta;
- cancelar;
- preservar resultados válidos de uma consulta parcial;
- repetir a consulta sem duplicar registros;
- visualizar medições manuais e D1 em um único histórico;
- consultar os períodos Hoje, 7 dias e 30 dias;
- visualizar média, mínimo, máximo e quantidade;
- visualizar gráfico de frequência cardíaca;
- distinguir a origem de cada registro;
- visualizar a última importação D1;
- manter os registros elegíveis pendentes para futura sincronização com o Health Connect.

## Correção obrigatória de fronteira

O roadmap atual coloca a escrita real no Health Connect dentro da Fase 09.

Essa dependência é circular, porque:

```text
Fase 09 produz os registros D1
        ↓
Fase 12 implementa o cliente Health Connect
```

A Fase 09 não deve implementar diretamente:

- `HealthConnectClient`;
- disponibilidade do provider;
- solicitação de permissões do Health Connect;
- mapeamento para `HeartRateRecord`;
- escrita no provider;
- atualização ou exclusão de records;
- processamento real de lotes no Health Connect.

A Fase 09 deve:

- persistir todos os D1 válidos;
- criar ou garantir uma `HealthSyncOperation` pendente por registro elegível;
- preservar falhas e pendências;
- deixar a fila pronta para processamento pela Fase 12;
- não apresentar registros pendentes como sincronizados.

A primeira parte da Fase 09 deve formalizar essa correção em:

- uma nova ADR;
- atualização do roadmap;
- atualização dos documentos de protocolo quando necessário.

Este documento prevalece sobre o trecho antigo do roadmap até que a correção seja aplicada.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento;
3. ADRs vigentes;
4. spec da Fase 09;
5. implementação e documentos concluídos da Fase 08;
6. `docs/decisions/adr-009-health-metrics-hub-navigation.md`;
7. implementação e documentos das Fases 04 a 07;
8. domínio e persistência da Fase 03;
9. `docs/protocol/history-d1.md`;
10. demais documentos em `docs/protocol/`;
11. roadmap já corrigido;
12. MotionLog apenas como referência funcional.

## Prioridade de desenvolvimento

A prioridade é concluir o fluxo funcional D1 e o histórico de frequência cardíaca.

Não criar task exclusiva de testes automatizados.

Não exigir:

- meta de cobertura;
- campanha ampla de testes Compose;
- instrumentação de todos os estados visuais;
- expansão geral dos fakes;
- correção completa da suíte anterior;
- regressão de todas as fases;
- harness completo de todas as variações possíveis do firmware.

Continuam obrigatórios:

- `assembleDebug`;
- `assembleRelease`;
- `installDebug`;
- validação física no Galaxy S22 e Champion C001;
- correção das regressões introduzidas pela Fase 09.

Testes pequenos devem acompanhar as tasks responsáveis pelas regras críticas.

## Escopo

A Fase 09 inclui:

- correção documental da fronteira com Health Connect;
- ADR sobre D1 local e fila pendente;
- atualização do roadmap;
- comando D1;
- parser D1;
- collector multipart;
- timeout;
- cancelamento;
- resultado parcial;
- deduplicação em memória;
- deduplicação persistente;
- persistência de medições D1;
- criação idempotente de operações pendentes;
- metadados da última consulta/importação D1;
- consultas por período;
- agregações;
- gráfico;
- histórico;
- integração da ação na tela de frequência cardíaca;
- loading;
- progresso;
- retry;
- erros seguros;
- validação física.

## Fora do escopo

A Fase 09 não deve:

- escrever no Health Connect;
- solicitar permissões do Health Connect;
- implementar scheduler de reenvio;
- consultar D1 ao abrir o aplicativo;
- consultar D1 ao abrir a tela;
- importar D1 em background;
- implementar foreground service;
- implementar WorkManager;
- implementar D3;
- implementar D4;
- implementar treinos;
- alterar a medição manual D0/F002;
- persistir payload BLE bruto;
- persistir bytes desconhecidos do D1;
- armazenar dados simulados;
- suportar outro relógio;
- buscar dias anteriores no relógio sem evidência de protocolo;
- criar seleção individual de registros para sincronização.

## Limite real do protocolo

O comando D1 confirmado consulta o histórico automático do dia atual:

```text
D1 01 00 00
```

O payload confirmado não fornece:

- ano;
- mês;
- dia;
- timezone;
- período inicial;
- período final.

Portanto, nesta fase:

- a consulta ao relógio representa o dia corrente;
- Hoje, 7 dias e 30 dias são períodos de consulta do banco local;
- os períodos maiores são formados pelo acúmulo de importações locais ao longo dos dias;
- não afirmar que o relógio fornece sete ou trinta dias;
- não criar comandos de data não confirmados;
- não atribuir significado a bytes desconhecidos.

## Arquitetura esperada

```text
HeartRateDetailScreen
        ↓
HealthViewModel
        ├── HeartRateRepository
        ├── HeartRateHistoryRepository ou contrato equivalente
        └── D1HeartRateSyncCoordinator
                  ├── ForegroundBleOperationGate
                  ├── DeviceConnectionBootstrap
                  ├── ChampionConnectionManager
                  ├── ChampionNotificationHub / transport streams
                  ├── ChampionD1HistoryProtocol
                  ├── HeartRateRepository
                  └── metadados de importação D1
```

Para leitura e apresentação:

```text
Room
  ├── medições manuais
  └── WATCH_D1_HISTORY
            ↓
HeartRateRepository
            ↓
HeartRateHistoryPolicy
            ├── período
            ├── média
            ├── mínimo
            ├── máximo
            └── série do gráfico
            ↓
HealthViewModel
            ↓
Tela de frequência cardíaca
```

Regras:

- Compose não acessa BLE;
- Compose não acessa DAO;
- ViewModel não interpreta bytes;
- parser não acessa Room;
- coordinator não depende de Compose;
- repository não acessa Bluetooth;
- a tela raiz Saúde não inicia D1;
- somente a tela específica de frequência cardíaca oferece a ação;
- dependências são fornecidas por Hilt.

## Ação do usuário

O primeiro card da tela de frequência cardíaca deve concentrar as ações funcionais:

```text
Medir frequência cardíaca
Buscar dados do relógio
```

Regras:

- `Medir frequência cardíaca` mantém o fluxo da Fase 08;
- `Buscar dados do relógio` inicia D1;
- abrir a tela não inicia consulta;
- atualizar outra métrica não inicia D1;
- auto-connect não inicia D1;
- nenhuma ação aparece como concluída antes da persistência.

## Comando D1

Enviar:

```text
D1 01 00 00
```

Canal:

```text
F008 write
```

Resposta:

```text
F008 notify
```

Regras:

- write with response;
- passar pela fila GATT existente;
- usar sessão funcional do dispositivo associado;
- garantir handshake A1;
- observar F008 antes ou de forma atomicamente coordenada com o envio;
- não usar listener global permanente;
- não criar retry automático interno;
- não executar duas consultas D1 simultâneas.

## Parser D1

Criar componente reutilizável equivalente a:

```text
ChampionD1HistoryProtocol
ChampionD1PacketParser
```

Evitar dois parsers diferentes.

Pacote mínimo:

```text
20 bytes
```

Campos confirmados:

| Offset | Campo          | Regra                  |
| ------ | -------------- | ---------------------- |
| `0`    | comando        | deve ser `0xD1`        |
| `1-2`  | `packetIndex`  | `uint16` little-endian |
| `3-4`  | `totalPackets` | `uint16` little-endian |
| `5`    | hora           | `0..23`                |
| `14`   | BPM            | `30..220`              |
| `19`   | minuto         | `0..59`                |

Campos `6-13` e `15-18` permanecem sem semântica confirmada.

Não:

- interpretar data nesses campos;
- interpretar segundos;
- interpretar checksum;
- interpretar timezone;
- persistir os bytes desconhecidos;
- registrar payload ou hexadecimal.

## Modelo de pacote interpretado

O parser deve produzir um resultado puro equivalente a:

```text
packetIndex
totalPackets
hour
minute
bpm
```

O timestamp completo deve ser resolvido fora do parser de bytes, usando o contexto da tentativa.

Separar:

```text
bytes → D1Packet
D1Packet + D1ImportContext → HeartRateMeasurement
```

## Contexto da tentativa

Cada tentativa deve possuir identidade própria:

```text
importAttemptId
bleSessionId
deviceId
startedAt
baseLocalDate
zoneId
parserVersion
```

`baseLocalDate` e `zoneId` devem ser congelados no início da tentativa.

Um pacote somente pertence à tentativa quando:

- vem da sessão BLE atual;
- vem de F008 notify;
- possui comando D1;
- foi observado depois do início;
- a tentativa ainda está ativa;
- não pertence a tentativa anterior.

Callbacks tardios devem ser ignorados.

## Timestamp

Como D1 fornece apenas hora e minuto:

```text
baseLocalDate
    + hour
    + minute
    + second = 0
    + zoneId
    → Instant
```

Regras:

- usar a data local capturada no início da tentativa;
- usar o timezone capturado no início da tentativa;
- não usar UTC como dia local;
- não mudar a data-base durante a coleta;
- não inventar offset vindo do relógio;
- preservar o `Instant` final;
- registrar `importedAt` separadamente.

Caso a consulta atravesse meia-noite:

- manter a data-base congelada;
- registrar a limitação;
- não aplicar heurística não confirmada;
- permitir nova consulta manual no novo dia.

## Validação

Um ponto D1 é válido quando:

- pacote tem pelo menos 20 bytes;
- comando é `0xD1`;
- hora está em `0..23`;
- minuto está em `0..59`;
- BPM está em `30..220`;
- timestamp pode ser resolvido;
- timestamp pertence à data-base esperada;
- dispositivo está identificado;
- parser version existe.

Pacotes inválidos:

- não são persistidos;
- não interrompem toda a coleta;
- incrementam contadores sanitizados;
- não registram BPM ou payload no Logcat.

## Collector multipart

O collector deve:

- receber pacotes F008 da sessão atual;
- aplicar parser;
- manter resultados válidos da tentativa;
- substituir duplicata da mesma chave em memória;
- aceitar pacotes fora de ordem;
- ordenar antes da persistência;
- finalizar por pacote de conclusão ou timeout;
- remover o observer ao concluir.

Chave de coleta inicial:

```text
packetIndex
totalPackets
resolvedMeasuredAt
```

A saída deve ser ordenada por:

1. timestamp;
2. `packetIndex`, como desempate.

## Conclusão

Critério confirmado:

```text
totalPackets > 0
e
packetIndex >= totalPackets
```

Não exigir contiguidade que o protocolo legado não exigia.

A conclusão pode ocorrer mesmo com lacunas.

A operação deve registrar sanitizadamente:

- maior índice observado;
- total informado;
- quantidade de índices distintos;
- presença de lacunas;
- quantidade de pontos válidos;
- quantidade inválida.

## Timeout

Timeout inicial:

```text
15 segundos
```

A política deve ser centralizada e configurável.

Não criar:

- retry automático infinito;
- segundo comando D1 oculto;
- loop até preencher todos os índices.

Comportamentos:

### Timeout com pontos válidos

- persistir os pontos válidos;
- retornar resultado parcial;
- informar que a consulta foi parcial;
- permitir nova tentativa;
- deduplicação protege contra repetição.

### Timeout sem pontos válidos

- não persistir;
- retornar falha segura;
- permitir retry manual.

### Pacote de conclusão com lacunas

- persistir os pontos válidos;
- retornar resultado completo com aviso de lacunas ou resultado parcial, conforme contrato escolhido;
- não inventar os registros ausentes.

## Resultado da importação

Planejar resultado tipado semelhante a:

```text
Completed
Partial
Failed
Cancelled
```

O resultado deve carregar apenas metadados seguros, como:

```text
receivedPacketCount
validPointCount
insertedCount
duplicateCount
invalidCount
missingIndexCount
timedOut
startedAt
finishedAt
```

Não incluir BPMs no resultado diagnóstico.

## Deduplicação

### Durante a tentativa

Deduplicar por chave de coleta.

### Na persistência

Usar identidade determinística baseada em:

```text
source = WATCH_D1_HISTORY
deviceId
measuredAt
```

`D1RecordKey` pode reforçar a identidade quando necessário.

Regras:

- executar D1 duas vezes no mesmo dia não duplica;
- pacote repetido não duplica;
- pacote fora de ordem não duplica;
- retry depois de timeout não duplica;
- manual e D1 no mesmo horário continuam registros distintos por origem;
- dispositivo diferente mantém identidade distinta.

Não usar BPM como parte principal da chave, pois o mesmo timestamp não deve gerar duplicatas só porque o valor divergiu.

A spec deve definir o comportamento quando a mesma chave chegar com BPM diferente:

- preservar o registro inicial;
- atualizar deterministicamente;
- ou retornar conflito seguro.

A escolha deve ser documentada.

## Modelo de domínio

Criar medições D1 usando:

```text
source = WATCH_D1_HISTORY
bpm = valor validado
measuredAt = timestamp resolvido
validity = VALID
deviceId = dispositivo associado
importedAt = instante da importação
parserVersion = versão D1
deduplicationKey = D1History(...)
```

Não criar `MEASUREMENT_STREAM`.

Não usar `MANUAL_MEASUREMENT`.

## Persistência por lote

A persistência deve ser transacional quando possível.

Planejar contrato específico equivalente a:

```text
importD1Measurements(batch)
```

O retorno deve informar:

- inseridos;
- duplicados;
- conflitos;
- operações pendentes criadas;
- operações pendentes já existentes.

Não deixar a UI estimar esses números comparando listas.

Uma falha isolada não deve apagar registros que já tenham sido confirmados pela política transacional escolhida.

A spec deve escolher entre:

- lote atômico completo;
- processamento idempotente registro por registro dentro de uma transação controlada.

A escolha deve preservar dados válidos e produzir resultado determinístico.

## Operações pendentes do Health Connect

Para cada D1 elegível:

```text
HealthSyncOperation
state = PENDING
recordType = HEART_RATE
aggregate = HeartRateMeasurement
```

Regras:

- criar uma operação por medição;
- não duplicar operação existente;
- reparar ausência de operação para D1 elegível já persistido, quando detectada;
- não marcar `SYNCING`;
- não marcar `SYNCED`;
- não solicitar permissão;
- não executar write;
- não tratar `PENDING` como erro;
- não mostrar “enviado” ao usuário.

A Fase 12 será responsável por:

- disponibilidade;
- permissões;
- client record ID;
- client record version;
- lotes;
- write;
- retry;
- estados synced/failed.

## Metadados da consulta D1

O timestamp genérico de importação do dispositivo já é usado por outros fluxos e não deve ser tratado como última consulta D1.

A spec deve definir uma fonte específica para:

- última tentativa D1;
- última conclusão com dados;
- último resultado;
- parser version;
- quantidade inserida;
- quantidade duplicada;
- quantidade inválida;
- timeout parcial.

Avaliar uma solução pequena e durável:

1. metadado específico por dispositivo e tipo de importação no Room;
2. metadado D1 em storage local com chave do dispositivo;
3. derivação parcial a partir das medições, complementada por estado de tentativa.

Não reutilizar silenciosamente `DeviceImportMetadata.lastImportedAt` como se fosse exclusivamente D1.

Se houver nova entidade ou decisão generalizável para D3/D4, documentar no ADR ou em decisão específica.

## Histórico local

A tela de frequência cardíaca deve consolidar:

```text
MANUAL_MEASUREMENT
WATCH_D1_HISTORY
```

Nesta fase, não incluir automaticamente:

- BPM de atividade do relógio;
- BPM de exercício iniciado pelo app;
- streams técnicos;
- registros inválidos.

Essas fontes futuras exigem decisão de produto explícita.

## Períodos

Implementar:

```text
Hoje
7 dias
30 dias
```

Interpretação:

- Hoje: início e fim do dia local atual;
- 7 dias: hoje e os seis dias anteriores;
- 30 dias: hoje e os 29 dias anteriores.

Usar `ZoneId` atual da interface para resolver limites.

A mudança de período não executa BLE.

## Consultas

Evoluir o repository para operações observáveis equivalentes a:

```text
observeValidMeasurements(from, to, sources)
observeLatestValidMeasurement()
observeD1ImportMetadata(deviceId)
```

Regras:

- ordenar por `measuredAt`;
- permitir origem manual e D1;
- não carregar payload bruto;
- não expor entities;
- não acessar DAO pela tela;
- evitar carregar o banco inteiro para mostrar 30 dias.

## Agregações

Para o período selecionado, calcular:

```text
quantidade
média
mínimo
máximo
```

Regras:

- usar somente medições válidas;
- média aritmética;
- arredondamento apenas para apresentação;
- não persistir agregados derivados;
- não interpolar lacunas;
- não criar amostras sintéticas;
- estado vazio quando não houver dados.

## Série do gráfico

### Hoje

Usar pontos reais ordenados por horário.

### 7 e 30 dias

Preferir agregação diária para reduzir ruído e quantidade de pontos.

Cada dia pode fornecer:

```text
média
mínimo
máximo
quantidade
```

A visualização pode usar média como linha principal e faixa mínimo/máximo quando isso for simples.

Não adicionar biblioteca externa de gráfico sem justificativa clara.

Preferir:

- Compose Canvas;
- componentes já existentes;
- implementação pequena e acessível.

O gráfico não pode ser a única representação dos dados.

## Histórico visual

A seção de histórico deve deixar de ser shell.

Exibir:

- data;
- horário;
- BPM;
- origem amigável.

Rótulos possíveis:

```text
Medição manual
Champion C001
```

Não exibir nomes internos como:

```text
MANUAL_MEASUREMENT
WATCH_D1_HISTORY
```

A lista deve:

- usar o período selecionado;
- ordenar do mais recente para o mais antigo;
- permitir agrupamento por dia;
- mostrar estado vazio;
- evitar lista ilimitada fora do período.

## Primeiro card da frequência cardíaca

Manter a estrutura definida pela ADR-009.

O primeiro card deve apresentar:

- último BPM válido;
- horário;
- origem;
- ação `Medir frequência cardíaca`;
- ação `Buscar dados do relógio`;
- estado da importação D1;
- última importação D1.

A medição manual não deve ser reimplementada.

## Progresso

Antes do primeiro pacote válido:

```text
progresso indeterminado
```

Depois de conhecer `packetIndex` e `totalPackets` válidos:

```text
progress = packetIndex / totalPackets
```

Regras:

- limitar visualmente a `0..1`;
- não depender do progresso para determinar validade;
- não assumir que índices são contíguos;
- mostrar quantidade importada quando útil;
- não mostrar porcentagem quando `totalPackets == 0`.

## Estado da importação

Separar da state machine da medição manual.

Planejar estado equivalente a:

```text
Idle
Preparing
Connecting
Reading
Persisting
Completed
Partial
Failed
Cancelled
```

`HealthUiState` pode conter simultaneamente:

- estado ocioso da medição manual;
- estado ativo da importação D1;
- dados históricos observados.

Somente uma operação BLE pode estar ativa.

## Gate BLE

Adicionar owner equivalente a:

```text
WATCH_D1_HISTORY
```

ao `ForegroundBleOperationGate`.

Deve conflitar com:

```text
DAILY_STEPS_REFRESH
MANUAL_HEART_RATE
WATCH_D1_HISTORY
```

Também deve impedir conexão manual concorrente quando o fluxo de dispositivo respeitar o gate.

Regras:

- aquisição atômica;
- liberação em `finally`;
- cancelamento libera;
- timeout libera;
- persistência falha libera;
- callback tardio não readquire;
- não substituir owner ativo.

## Conexão

Ao iniciar D1:

1. verificar dispositivo associado;
2. verificar permissões BLE;
3. verificar Bluetooth;
4. reutilizar sessão funcional compatível;
5. conectar de forma controlada quando necessário;
6. garantir `TransportReady`;
7. garantir handshake A1;
8. observar F008;
9. enviar D1;
10. coletar.

Não:

- iniciar scan amplo sem relação com o associado;
- conectar outro Champion;
- entrar em retry infinito;
- solicitar permissões ao abrir a tela.

## Cancelamento e lifecycle

Ao cancelar:

- remover observer;
- cancelar timeout;
- impedir novas persistências;
- liberar gate;
- manter dados já persistidos de tentativas anteriores;
- retornar a estado estável.

Ao sair da tela durante importação:

- solicitar confirmação ou cancelar de maneira explícita;
- não deixar operação invisível controlando BLE.

Ao ir para background:

- cancelar a consulta;
- remover observer;
- liberar gate;
- não iniciar serviço;
- não continuar D1 silenciosamente.

Em morte do processo:

- não restaurar importação ativa;
- retornar a `Idle`;
- preservar dados já persistidos.

## Retry

Retry deve executar uma nova tentativa D1.

Não deve:

- reaproveitar listener antigo;
- reaproveitar attempt ID;
- reaproveitar timeout;
- apagar dados anteriores;
- repetir write do Health Connect;
- duplicar medições.

## Falhas seguras

Considerar:

- sem dispositivo associado;
- permissões ausentes;
- Bluetooth desligado;
- dispositivo não encontrado;
- conexão falhou;
- handshake falhou;
- F008 indisponível;
- envio D1 falhou;
- timeout vazio;
- timeout parcial;
- pacote inválido;
- nenhum registro válido;
- persistência falhou;
- operação BLE concorrente;
- disconnect inesperado;
- cancelamento;
- erro interno.

Resultado parcial não deve ser apresentado como perda total.

## Logging e diagnóstico

Pode registrar:

- tentativa iniciada;
- tentativa encerrada;
- duração;
- quantidade de pacotes;
- quantidade válida;
- quantidade inválida;
- quantidade inserida;
- quantidade duplicada;
- lacunas;
- timeout;
- categoria de erro.

Não registrar:

- BPM;
- horário individual de medição;
- payload;
- bytes;
- hexadecimal;
- MAC;
- device ID persistente;
- dados pessoais;
- stack trace destinado à UI.

## Privacidade

Os registros permanecem locais.

Não:

- enviar para servidor;
- exportar BPM no relatório diagnóstico;
- persistir payload bruto;
- tornar Health Connect fonte visual primária;
- alterar regras de backup sem decisão específica.

## Acessibilidade

Anunciar estados equivalentes a:

```text
Buscando dados do relógio
Dados do relógio importados
Importação parcial concluída
Não foi possível buscar os dados
Importação cancelada
```

O gráfico deve possuir:

- descrição textual;
- resumo de média, mínimo e máximo;
- histórico em lista como alternativa.

## Estratégia reduzida de testes

Não criar task exclusiva de testes.

Testes pequenos recomendados:

- builder `D1 01 00 00`;
- pacote menor que 20 bytes;
- comando incorreto;
- little-endian de índices;
- hora inválida;
- minuto inválido;
- BPM 30 e 220;
- BPM fora da faixa;
- resolução de timestamp;
- timezone;
- deduplicação em memória;
- pacote fora de ordem;
- conclusão;
- timeout parcial;
- timeout vazio;
- persistência idempotente;
- operação pendente criada uma vez;
- callback tardio ignorado;
- gate impede concorrência;
- agregações;
- limites Hoje, 7 dias e 30 dias.

## Validação mínima

Gate obrigatório:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Comandos informativos:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

Falhas antigas e não relacionadas devem ser documentadas.

Regressões introduzidas pela Fase 09 devem ser corrigidas.

## Smoke test manual

No Galaxy S22 com Champion C001:

1. abrir Saúde;
2. abrir Frequência cardíaca;
3. confirmar ação `Buscar dados do relógio`;
4. confirmar que nada inicia automaticamente;
5. tocar na ação;
6. observar conexão quando necessária;
7. observar progresso;
8. concluir a importação;
9. confirmar quantidade importada;
10. confirmar última importação;
11. confirmar histórico D1;
12. confirmar origem;
13. confirmar atualização do último BPM;
14. voltar para Home;
15. confirmar último BPM atualizado quando o D1 mais recente for o registro válido mais novo;
16. repetir a importação;
17. confirmar ausência de duplicatas;
18. alternar Hoje, 7 dias e 30 dias;
19. confirmar média, mínimo e máximo;
20. confirmar gráfico;
21. confirmar lista histórica;
22. cancelar uma consulta;
23. confirmar cleanup;
24. iniciar medição manual e tentar D1;
25. confirmar bloqueio;
26. iniciar refresh de passos e tentar D1;
27. confirmar bloqueio;
28. desligar Bluetooth;
29. confirmar erro seguro;
30. ligar Bluetooth e tentar novamente;
31. enviar app para background durante a consulta;
32. confirmar cancelamento;
33. confirmar que não houve solicitação de Health Connect;
34. confirmar que nenhum registro foi marcado como sincronizado;
35. confirmar ausência de BPM e payload no Logcat.

## Atualização do protocolo

Após validação física, atualizar `docs/protocol/history-d1.md` com:

- bytes confirmados fisicamente;
- comportamento real do índice;
- comportamento real do total;
- frequência dos registros;
- resultado multipart;
- timeout observado;
- lacunas;
- duplicatas;
- comportamento perto da meia-noite;
- fixtures sanitizadas;
- divergências do MotionLog.

Não promover bytes desconhecidos sem evidência.

## ADR e roadmap

A primeira task documental deve planejar uma ADR para registrar:

- Fase 09 é owner da importação local D1;
- registros válidos criam fila pendente;
- Fase 12 é owner da escrita real no Health Connect;
- UI local usa repositories como fonte;
- falha futura do Health Connect não desfaz dados locais;
- não existe seleção manual registro a registro.

Atualizar o roadmap para remover da Fase 09:

- disponibilidade do Health Connect;
- permissões;
- escrita;
- lotes reais;
- contadores de enviados e erros do provider.

Manter na Fase 09:

- importação D1;
- fila pendente;
- histórico;
- gráfico;
- agregações;
- última importação;
- retry local.

Atualizar a Fase 12 para explicitar:

- processar todas as operações D1 pendentes;
- escrever todas as amostras válidas;
- falhas parciais permanecem pendentes;
- reenvio não duplica.

## Dívida técnica

Registrar para fases futuras:

- escrita real no Health Connect na Fase 12;
- scheduler/reenvio na Fase 12 ou 14;
- foreground service para importações longas na Fase 14;
- histórico combinado com BPM de exercícios após fases de treino;
- calendário consolidado na Fase 15;
- cobertura ampla na Fase 21;
- fixtures físicas adicionais;
- investigação dos bytes D1 ainda desconhecidos.

## Critérios de aceite

A Fase 09 será concluída quando:

- ADR e roadmap corrigirem a fronteira com Health Connect;
- `Buscar dados do relógio` estiver funcional;
- D1 somente iniciar por ação explícita;
- comando passar pela fila GATT;
- F008 for observado pela sessão atual;
- parser validar os campos confirmados;
- bytes desconhecidos não receberem semântica inventada;
- timestamps usarem data e timezone congelados;
- pacotes fora de ordem forem aceitos;
- duplicatas não criarem registros novos;
- timeout com dados preservar pontos válidos;
- timeout vazio permitir retry;
- callbacks tardios forem ignorados;
- gate impedir concorrência;
- registros D1 válidos forem persistidos;
- operações pendentes forem criadas idempotentemente;
- nenhuma escrita no Health Connect ocorrer;
- medição manual e D1 aparecerem no histórico;
- períodos Hoje, 7 dias e 30 dias funcionarem;
- média, mínimo e máximo funcionarem;
- gráfico estiver funcional;
- histórico estiver funcional;
- última importação D1 estiver disponível;
- Home observar o último registro válido sem iniciar BLE;
- debug e release compilarem;
- fluxo físico for validado no Galaxy S22;
- dívida técnica ficar registrada.
