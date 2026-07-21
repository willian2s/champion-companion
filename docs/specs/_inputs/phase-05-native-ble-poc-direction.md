# Direção da prova de conceito BLE da Fase 05

## Contexto

A Fase 05 valida no dispositivo físico o núcleo BLE nativo criado na Fase 04.

A Fase 04 já entregou:

- contratos BLE puros;
- fronteira única para APIs Android BLE;
- scanner nativo;
- filtro do Champion C001;
- fila GATT serial por sessão;
- callback bridge;
- cliente GATT;
- descoberta de services;
- capability registry;
- configuração de notifications via CCCD;
- multiplexação de streams;
- command envelope;
- packet envelope;
- response routing;
- connection manager;
- estados de transporte;
- timeout, retry, cancelamento e cleanup;
- logging seguro;
- bindings Hilt;
- fakes e testes.

A Fase 05 não deve criar uma segunda implementação BLE nem contornar o núcleo existente.

## Objetivo

Validar no Galaxy S22 e no Champion C001 o fluxo físico:

```text
Permissões
    ↓
Scan manual
    ↓
Conexão
    ↓
Discovery
    ↓
TransportReady
    ↓
Subscriptions F008, FEA1 e F002
    ↓
Handshake A1 confirmado
    ↓
Recebimento de passos FEA1
    ↓
Início D0
    ↓
Recebimento de D0/F002
    ↓
Definição do BPM final da sessão
    ↓
Parada D0
    ↓
Disconnect e cleanup
```

O resultado da fase deve ser uma prova técnica reproduzível. Ainda não é a experiência final de gerenciamento de dispositivo, resumo diário ou medição manual de saúde.

## Gate da fase

A Fase 05 representa o Gate A do roadmap.

O projeto somente deve avançar quando o fluxo completo for executado com sucesso dez vezes consecutivas no Galaxy S22 com o Champion C001.

Uma fase não pode ser marcada como concluída apenas com:

- testes unitários;
- fakes;
- emulador;
- compilação;
- sucesso de escrita sem resposta do relógio;
- execução parcial do fluxo;
- evidência herdada do MotionLog.

A validação física é obrigatória.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. `docs/core/current-project-baseline.md`;
3. ADRs vigentes;
4. este documento;
5. spec da Fase 05;
6. spec e implementação da Fase 04;
7. documentos de protocolo;
8. roadmap;
9. MotionLog como referência funcional.

Não promover automaticamente uma informação classificada como inferida ou não confirmada para comportamento definitivo.

## Limite entre transporte e protocolo

`TransportReady` significa somente:

- GATT conectado;
- services descobertos;
- channels obrigatórios resolvidos;
- F008 notify configurada;
- fila GATT operacional.

`TransportReady` não significa:

- handshake realizado;
- A1 confirmado;
- relógio pronto para comandos funcionais;
- passos recebidos;
- BPM disponível.

A Fase 05 deve introduzir um estado de protocolo separado para o handshake.

## Reutilização obrigatória da Fase 04

Toda operação deve reutilizar:

- `ChampionConnectionManager`;
- `ChampionTransportSession`;
- `ChampionGattQueue`;
- `ChampionCommand`;
- `ChampionPacket`;
- `ChampionPacketRouter`;
- `ChampionNotificationHub`;
- `ChampionCapabilityRegistry`;
- `ChampionProtocolLogger`;
- erros e estados já definidos.

Não permitir:

- escrita direta em `BluetoothGatt`;
- acesso ao callback Android pela feature;
- criação de scanner paralelo;
- subscriptions fora do `ChampionNotificationHub`;
- comandos enviados fora da fila;
- listeners independentes por tela;
- timeout implementado somente na UI;
- retries invisíveis fora da infraestrutura existente.

Caso o núcleo da Fase 04 precise de uma pequena extensão, ela deve preservar os contratos e possuir teste próprio.

## Escopo funcional da prova de conceito

A Fase 05 inclui:

- declaração de permissões BLE;
- solicitação explícita das permissões pelo usuário;
- tela técnica da prova de conceito;
- scan manual;
- conexão manual;
- capability report;
- handshake A1;
- leitura técnica de passos FEA1;
- início e parada D0;
- parsing de respostas D0;
- recepção de amostras F002;
- estimativa técnica de BPM baseada em F002;
- seleção do valor final da sessão;
- disconnect;
- relatório sanitizado de cada ciclo;
- confirmação física dos contratos;
- criação de fixtures sanitizadas ou sintéticas;
- dez ciclos consecutivos de estabilidade.

## Fora do escopo funcional

A fase não deve:

- salvar dispositivo como dispositivo principal;
- criar gerenciamento definitivo do dispositivo;
- fazer auto-connect;
- conectar ao abrir o aplicativo;
- fazer reconexão infinita;
- executar BLE em background;
- criar foreground service;
- sincronizar automaticamente;
- persistir passos;
- persistir resumo diário;
- persistir medições de BPM;
- escrever no Health Connect;
- implementar D1;
- implementar D3;
- implementar D4;
- importar atividades;
- implementar tela final de Saúde;
- implementar tela final de Dispositivo;
- modificar o schema Room;
- suportar outros wearables.

## Manifesto e permissões

A Fase 05 deve adicionar somente as permissões necessárias para scan e conexão BLE.

### Android 12 ou superior

Declarar e solicitar em runtime:

```text
android.permission.BLUETOOTH_SCAN
android.permission.BLUETOOTH_CONNECT
```

Não declarar `BLUETOOTH_ADVERTISE`, pois o app não precisa se tornar detectável.

Como o Champion Companion não usa resultados BLE para determinar localização física:

```text
BLUETOOTH_SCAN
→ android:usesPermissionFlags="neverForLocation"
```

### Android 11 ou inferior

Manter compatibilidade com:

```text
android.permission.BLUETOOTH
android.permission.BLUETOOTH_ADMIN
android.permission.ACCESS_FINE_LOCATION
```

As permissões legadas devem usar `android:maxSdkVersion="30"` quando aplicável.

Não solicitar:

- localização em background;
- foreground service;
- notificações;
- acesso à localização no Android 12+;
- permissões não utilizadas.

### Declaração de hardware

Avaliar:

```text
android.hardware.bluetooth_le
```

O app é dedicado a um relógio BLE, portanto a spec deve decidir explicitamente se o recurso deve ser declarado como obrigatório para distribuição.

### Solicitação em runtime

A solicitação deve:

- acontecer somente após ação explícita do usuário;
- usar APIs modernas de Activity Result;
- representar permissões ausentes no estado da tela;
- explicar por que são necessárias;
- permitir tentar novamente;
- não entrar em loop;
- não iniciar scan antes da concessão;
- não interpretar negação como erro interno.

A camada BLE continua responsável por verificar os pré-requisitos. A UI técnica apenas solicita as permissões que o núcleo indicar como ausentes.

## Tela técnica

Criar uma interface técnica temporária dentro da área de dispositivo.

Ela deve ser destinada à validação da Fase 05 e não representar a UX definitiva da Fase 06.

Preferencialmente, a tela deve ficar disponível somente na variante debug.

A tela deve apresentar:

- status das permissões;
- status do Bluetooth;
- estado do scan;
- candidato encontrado;
- estado da conexão;
- capabilities disponíveis;
- estado do transporte;
- estado do handshake;
- disponibilidade de FEA1;
- disponibilidade de F002;
- último valor técnico de passos recebido;
- estado da sessão D0;
- BPM D0 recente;
- quantidade de pacotes F002;
- BPM estimado por F002;
- valor final selecionado;
- duração da sessão;
- erro sanitizado;
- ações permitidas no estado atual.

Ações esperadas:

```text
Conceder permissões
Iniciar scan
Cancelar scan
Conectar
Executar handshake
Aguardar passos
Iniciar BPM
Parar BPM
Desconectar
Reiniciar ciclo
Exportar relatório sanitizado
```

Ações inválidas devem ficar desabilitadas ou retornar erro de estado seguro.

Não exibir:

- endereço MAC;
- payload hexadecimal;
- bytes brutos;
- senha do handshake;
- stack trace;
- identificadores persistentes.

Valores de passos e BPM podem ser mostrados localmente na tela durante o teste, mas não devem entrar no Logcat nem no relatório exportado.

## Coordenador da prova de conceito

Criar uma camada de orquestração técnica sobre o núcleo BLE.

Responsabilidades:

- controlar o fluxo da PoC;
- expor estado observável;
- validar pré-condições;
- executar ações na ordem correta;
- gerenciar handshake;
- gerenciar subscription FEA1;
- gerenciar sessão D0/F002;
- limpar estado ao desconectar;
- gerar evidência sanitizada;
- impedir comandos concorrentes incompatíveis.

Essa camada não deve:

- acessar `BluetoothGatt`;
- acessar DAOs;
- acessar Room;
- acessar Health Connect;
- conter componentes Compose;
- manter referência de Activity ou Context quando não for necessário.

## Estado da prova de conceito

Separar o estado de transporte do estado da PoC.

Planejar estados suficientes para representar:

```text
Idle
PermissionsRequired
ReadyToScan
Scanning
DeviceFound
Connecting
TransportReady
Handshaking
HandshakeConfirmed
WaitingForSteps
StepsObserved
StartingHeartRate
MeasuringHeartRate
StoppingHeartRate
CycleCompleted
Disconnecting
Failed
```

Os nomes podem ser ajustados, mas as transições devem ser explícitas.

Evitar booleanos concorrentes como:

```text
isConnected
isHandshakeOk
isMeasuring
hasSteps
hasBpm
hasError
```

O estado de handshake deve ser invalidado quando:

- a sessão GATT for encerrada;
- ocorrer disconnect inesperado;
- uma nova sessão for criada;
- o transporte falhar;
- o cleanup for executado.

## Handshake A1

### Canal

Escrever em:

```text
F008 write
f0080003-0451-4000-b000-000000000000
```

Receber confirmação em:

```text
F008 notify
f0080002-0451-4000-b000-000000000000
```

Usar write with response.

### Ordem

O handshake somente pode ser iniciado depois de:

1. conexão;
2. discovery;
3. resolução de capabilities;
4. F008 notify configurada;
5. `TransportReady`.

### Payload

O comando possui 20 bytes:

```text
Offset  Conteúdo
0       0xA1
1       senha low byte
2       senha high byte
3       0x00
4       ano high byte
5       ano low byte
6       mês
7       dia
8       hora
9       minuto
10      segundo
11      0x01
12      0x01
13      timezone em blocos de 15 minutos
14      0x00
15-19   0x00
```

Regras:

- senha inicial fixa: `0000`;
- senha interpretada em decimal;
- ano em big-endian;
- data e horário locais;
- timezone baseado no offset vigente no instante do handshake;
- offset convertido em blocos de 15 minutos;
- resultado limitado ao byte assinado esperado pelo protocolo;
- payload copiado defensivamente;
- senha nunca registrada.

### Confirmação

Sucesso somente quando chegar um pacote F008 que satisfaça:

```text
tamanho >= 20
bytes[0] == 0xA1
bytes[19] == 0x01
```

Sucesso da escrita GATT não significa sucesso do handshake.

Pacotes A1 que não atendam à confirmação devem ser classificados como rejeitados, desconhecidos ou malformados sem promover o estado para confirmado.

### Timeout e retry

O handshake deve possuir timeout explícito.

Para medir a estabilidade real:

- não esconder falhas com retry ilimitado;
- não executar reconexão automática;
- preferir nenhuma repetição automática durante o Gate A;
- oferecer nova tentativa iniciada pelo usuário;
- registrar separadamente qualquer retry que venha a ser aprovado.

Um ciclo somente conta como sucesso quando conclui sem retry oculto.

## FEA1 e passos

### Capability

FEA1 usa:

```text
Service FEE7
0000fee7-0000-1000-8000-00805f9b34fb

Characteristic FEA1 notify
0000fea1-0000-1000-8000-00805f9b34fb
```

Na Fase 04, FEA1 é capability opcional e não bloqueia `TransportReady`.

Na Fase 05, sua presença é obrigatória para concluir o ciclo completo da PoC.

Se estiver ausente:

- conexão pode continuar tecnicamente pronta;
- a etapa de passos deve falhar com erro específico;
- o ciclo não conta como sucesso;
- registrar a capability ausente como evidência física.

### Subscription

A subscription deve usar o `ChampionNotificationHub`.

Não configurar o CCCD diretamente pela feature ou coordenador.

### Pacote

Pacote inicialmente aceito:

```text
01 XX XX XX
```

Regras:

- exatamente 4 bytes;
- primeiro byte `0x01`;
- passos em 24 bits little-endian:

```text
steps = byte1 | byte2 << 8 | byte3 << 16
```

Nesta fase:

- o valor permanece somente em memória;
- não criar ou atualizar `DailySummary`;
- não calcular distância;
- não calcular calorias;
- não sincronizar dados.

### Validação física

Confirmar:

- se FEA1 emite espontaneamente;
- em que momento após conexão ou handshake;
- se exige interação no relógio;
- frequência aproximada de atualização;
- comportamento com valor inalterado;
- formato real de quatro bytes.

Atualizar o documento de protocolo com a classificação confirmada fisicamente.

## D0 e F002

### Capabilities

D0 escreve em F008.

F002 usa:

```text
Service:
f0020001-0451-4000-b000-000000000000

Notify:
f0020002-0451-4000-b000-000000000000
```

F002 notify deve ser configurada antes de enviar D0 start.

Se F002 não estiver disponível:

- relatar capability ausente;
- ainda observar eventual resposta D0 em F008;
- o ciclo completo do Gate A não deve ser considerado aprovado sem resolver a lacuna.

### Start

Enviar:

```text
D0 01
```

Regras:

- somente após A1 confirmado;
- write with response;
- sem retry automático cego;
- limpar todos os valores da sessão anterior;
- registrar instante inicial;
- iniciar buffer F002 limitado;
- iniciar observação D0 na F008.

### Stop

Enviar:

```text
D0 00
```

O stop deve ser tentado em:

- parada solicitada pelo usuário;
- fim do tempo da sessão;
- cancelamento;
- erro recuperável;
- encerramento normal.

Quando o transporte ainda estiver disponível, o stop deve ser enviado antes do disconnect.

A falha de stop não pode:

- manter a UI eternamente em estado de parada;
- impedir cleanup;
- deixar fila presa;
- causar retry infinito.

### Resposta D0

Um pacote F008 com código `0xD0` pode conter BPM em tempo real.

Regra inicial:

- ignorar o byte de comando;
- localizar o primeiro valor subsequente entre 35 e 220;
- aceitar somente valor recebido após o início da sessão atual;
- registrar timestamp monotônico ou instante correspondente;
- nunca reutilizar BPM de sessão anterior.

### F002

Decodificar o stream em pares:

```text
sample = lowByte | highByte << 8
```

Regras:

- `uint16` little-endian;
- byte final ímpar é ignorado;
- amostras ficam somente em memória;
- buffer possui limite explícito;
- bytes recebidos são copiados defensivamente;
- nenhum sinal bruto é enviado ao Logcat;
- nenhum sinal bruto é persistido automaticamente;
- buffer é eliminado ao finalizar ou cancelar a sessão.

## Estimador técnico F002

A PoC deve portar ou reimplementar de forma testável o estimador necessário para confirmar que o sinal F002 pode produzir BPM.

Requisitos conceituais da baseline:

- usar apenas amostras positivas;
- calcular sample rate a partir da duração;
- descartar o início instável do sinal;
- remover baseline;
- suavizar;
- detectar picos;
- aplicar distância mínima entre picos;
- filtrar intervalos fisiologicamente plausíveis;
- exigir quantidade mínima de intervalos;
- usar mediana dos intervalos;
- rejeitar BPM fora de 35 a 220;
- produzir metadados técnicos de qualidade sem registrar o sinal bruto.

A spec deve definir os cálculos precisamente, incluindo:

- número mínimo de amostras;
- janela de baseline;
- suavização;
- threshold;
- distância mínima;
- intervalos aceitos;
- quantidade mínima de picos;
- score de qualidade;
- comportamento com dados insuficientes.

A implementação deve possuir fixtures sintéticas determinísticas.

## Valor final da sessão

Separar:

```text
BPM D0 ao vivo
BPM estimado por F002
BPM final da sessão
```

Regra inicial para a PoC:

```text
1. usar D0 válido recebido durante a sessão atual;
2. caso ausente, usar estimativa F002 válida;
3. caso ambos ausentes, resultado indisponível.
```

Não utilizar:

- valor D0 anterior ao início;
- valor de sessão encerrada;
- valor fora de 35 a 220;
- estimativa inválida;
- valor inventado;
- último valor global sem timestamp.

O valor final não deve ser persistido nesta fase.

## Duração da medição

A baseline funcional usa 60 segundos para a medição manual.

Para o Gate A, planejar:

- pelo menos um ciclo completo de 60 segundos para comparação funcional;
- duração técnica configurável em debug para repetições;
- duração reduzida nunca inferior ao mínimo necessário para produzir uma estimativa confiável;
- registro da duração usada em cada ciclo;
- mesma configuração nas dez execuções consecutivas do gate final.

A redução do tempo não pode mascarar falhas de buffer, timeout ou stop.

## Estado em memória e cleanup

Ao iniciar um ciclo:

- criar ID efêmero;
- limpar passos anteriores;
- limpar D0 anterior;
- limpar timestamp D0 anterior;
- limpar amostras F002;
- zerar contadores;
- limpar erro técnico anterior;
- invalidar resultado final anterior.

Ao desconectar:

- cancelar jobs;
- parar coleta;
- tentar D0 stop quando aplicável e seguro;
- fechar subscriptions lógicas;
- limpar buffer;
- invalidar handshake;
- invalidar session ID;
- usar cleanup do `ChampionConnectionManager`;
- retornar a estado estável.

Nenhum callback tardio pode atualizar a nova sessão.

## Persistência

Não persistir automaticamente:

- dispositivo;
- endereço BLE;
- passos;
- BPM;
- sinal F002;
- payload A1;
- estado do transporte;
- estado da PoC;
- relatório com dados de saúde.

A Fase 05 pode registrar somente evidências técnicas sanitizadas.

Não usar Room para a prova de conceito.

## Relatório sanitizado

Cada execução deve produzir uma evidência técnica em memória e permitir exportação explícita de um relatório sanitizado.

Pode incluir:

- ID efêmero do ciclo;
- versão do app;
- versão do Android;
- modelo geral do aparelho;
- início e fim;
- duração;
- transições de estado;
- permissões concedidas;
- scan concluído;
- conexão concluída;
- discovery concluído;
- capabilities presentes como booleanos;
- subscription concluída;
- handshake confirmado;
- quantidade de pacotes FEA1;
- presença de passos, sem valor;
- quantidade de pacotes D0;
- presença de BPM D0, sem valor;
- quantidade de pacotes F002;
- estimativa disponível, sem valor;
- qualidade categórica;
- stop enviado;
- disconnect concluído;
- durações das etapas;
- categorias de erro.

Não incluir:

- MAC;
- identificador persistente;
- nome personalizado;
- payload;
- hexadecimal;
- bytes;
- senha;
- passos exatos;
- BPM exato;
- sinal F002;
- dados pessoais;
- stack trace.

Usar diretório privado ou cache privado e compartilhamento explícito seguro.

Não criar logs públicos permanentes.

## Fixtures

A captura física deve ser usada para confirmar estruturas, mas dados reais de saúde não devem ser commitados.

Fixtures versionadas devem ser:

- sintéticas;
- sanitizadas;
- mínimas;
- documentadas;
- suficientes para reproduzir o formato;
- livres de MAC, senha, data pessoal, passos reais e BPM real.

O payload A1 de saída deve usar data, hora e senha sintéticas nos testes.

## Testes automatizados

Planejar testes JVM para:

- builder A1;
- endianess do ano;
- senha;
- timezone positivo;
- timezone negativo;
- arredondamento em blocos de 15 minutos;
- matcher de confirmação A1;
- rejeição A1 incompleta;
- parser FEA1;
- passos 24-bit;
- parser D0;
- rejeição de BPM inválido;
- parser F002 little-endian;
- byte ímpar;
- buffer limitado;
- reset entre sessões;
- timestamp de D0;
- estimador com sinal sintético;
- dados insuficientes;
- qualidade do sinal;
- seleção de BPM final;
- stop em finalização;
- stop após erro;
- cleanup;
- state machine da PoC;
- callback tardio;
- relatório sanitizado.

Planejar testes de integração com fakes para:

- fluxo completo de sucesso;
- falha de permissão;
- scan expirado;
- capability FEA1 ausente;
- capability F002 ausente;
- timeout A1;
- A1 rejeitado;
- disconnect durante handshake;
- disconnect durante D0;
- nenhum BPM disponível;
- falha de D0 stop;
- repetição de ciclo sem estado antigo.

Usar testes instrumentados quando necessário para:

- solicitação de permissões;
- UI Compose;
- compartilhamento do relatório;
- integração real com framework Android.

## Validação física

Usar:

```text
Aparelho: Galaxy S22
Relógio: Champion C001
Build: debug
Conexão: ADB
```

### Primeira validação exploratória

Antes do gate final:

1. confirmar nome e `localName` observados no scan;
2. confirmar services e characteristics reais;
3. confirmar properties;
4. confirmar F008 notify;
5. confirmar A1;
6. confirmar FEA1;
7. confirmar D0 start;
8. confirmar resposta D0;
9. confirmar F002;
10. confirmar D0 stop;
11. confirmar disconnect;
12. atualizar documentos de protocolo.

Resultados divergentes do MotionLog devem ser registrados sem forçar o comportamento antigo.

### Casos negativos físicos

Executar pelo menos:

- permissão negada;
- Bluetooth desligado;
- cancelamento de scan;
- relógio fora de alcance;
- disconnect durante BPM;
- nova conexão após disconnect;
- duas execuções seguidas sem reiniciar o app.

### Gate final

Executar dez vezes consecutivas:

1. iniciar com estado limpo;
2. conceder ou verificar permissões;
3. encontrar Champion C001;
4. conectar;
5. atingir `TransportReady`;
6. confirmar A1;
7. receber FEA1;
8. iniciar D0;
9. receber BPM por D0 ou estimativa válida F002;
10. enviar D0 stop;
11. finalizar sessão;
12. desconectar;
13. confirmar estado `Disconnected`;
14. iniciar o ciclo seguinte sem reiniciar o app.

Para aprovação:

- dez ciclos consecutivos completos;
- nenhuma operação GATT concorrente;
- nenhuma fila presa;
- nenhum timeout residual;
- nenhuma resposta de sessão antiga;
- nenhum crash;
- nenhum ANR;
- nenhum restart obrigatório;
- nenhum retry infinito;
- nenhum dado de saúde no Logcat;
- cleanup confirmado em todos os ciclos;
- relatório sanitizado produzido para cada ciclo.

Se um ciclo falhar, registrar a causa, corrigir e reiniciar a sequência de dez ciclos.

## Atualização da documentação

Após validação física, atualizar os documentos relevantes para registrar:

- valores confirmados fisicamente;
- divergências;
- propriedades GATT observadas;
- comportamento real do A1;
- comportamento real do FEA1;
- comportamento real do D0;
- comportamento real do F002;
- timeout eficaz;
- necessidade ou não de retry;
- comportamento do stop;
- comportamento do disconnect;
- fixtures criadas;
- limitações conhecidas.

Não reescrever silenciosamente evidências anteriores. Manter indicação clara do que era inferido e do que passou a ser confirmado fisicamente.

## Critérios de aceite

A Fase 05 será considerada concluída quando:

- permissões BLE estiverem declaradas corretamente;
- solicitação runtime funcionar no Galaxy S22;
- scan for iniciado somente pelo usuário;
- Champion C001 for encontrado;
- conexão atingir `TransportReady`;
- F008 notify estiver ativa;
- handshake A1 usar a fila existente;
- A1 for confirmado por resposta do relógio;
- write success não for confundido com handshake success;
- FEA1 for recebida e interpretada;
- passos permanecerem apenas em memória;
- F002 estiver assinada antes do D0 start;
- D0 start e stop passarem pela fila;
- BPM D0 for aceito somente na sessão atual;
- F002 for decodificado em memória;
- estimador possuir testes sintéticos;
- valor final não reutilizar dados antigos;
- nenhum dado da PoC for enviado ao Room ou Health Connect;
- relatório sanitizado não contiver dados sensíveis;
- documentos de protocolo refletirem evidência física;
- casos negativos forem exercitados;
- dez ciclos consecutivos forem concluídos;
- cleanup funcionar entre ciclos;
- o Gate A for formalmente aprovado.
