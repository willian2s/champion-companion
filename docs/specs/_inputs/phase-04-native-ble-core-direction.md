# Direção do núcleo BLE nativo da Fase 04

## Contexto

A Fase 04 estabelece o núcleo BLE nativo do Champion Companion.

O Champion Companion é um aplicativo Android nativo escrito em Kotlin e Jetpack Compose, dedicado exclusivamente ao relógio Champion C001.

As Fases 01, 02 e 03 já estabeleceram:

- fundação Android;
- navegação;
- injeção de dependência com Hilt;
- Coroutines e `StateFlow`;
- design system Material 3;
- modelos de domínio;
- Room V1;
- DataStore;
- repositories e transações;
- modelos diagnósticos;
- regras de privacidade.

A Fase 04 deve introduzir a infraestrutura de transporte BLE sem implementar ainda os fluxos funcionais do relógio.

## Objetivo

Criar uma camada BLE nativa, previsível, testável e independente da interface, responsável por:

- verificar pré-requisitos do Bluetooth;
- localizar dispositivos Champion C001;
- abrir e encerrar conexões GATT;
- descobrir services e characteristics;
- manter um catálogo tipado de canais;
- serializar todas as operações GATT;
- configurar notifications e indications;
- expor notificações como streams tipados;
- enviar comandos por uma fronteira controlada;
- encaminhar respostas para consumidores do protocolo;
- controlar timeouts, cancelamentos e retries;
- representar estados e erros de transporte;
- liberar corretamente todos os recursos.

## Limite entre as Fases 04 e 05

A Fase 04 entrega o núcleo BLE reutilizável.

A Fase 05 será responsável por validar fisicamente o fluxo:

```text
Scan
  ↓
Conexão
  ↓
Discovery
  ↓
Notifications
  ↓
Handshake A1
  ↓
Confirmação no Champion C001
```

Portanto, a Fase 04 pode preparar contratos, comandos genéricos, canais e mecanismos necessários ao handshake, mas não deve considerar o A1 funcionalmente validado.

A Fase 04 não deve:

- executar automaticamente o handshake A1;
- implementar uma experiência completa de pareamento;
- validar o protocolo no relógio físico como critério principal;
- implementar D0, D1, D3 ou D4;
- importar dados;
- persistir resultados BLE no Room;
- sincronizar com o Health Connect.

## Fonte de verdade

A implementação deve respeitar, nesta ordem:

1. `AGENTS.md`;
2. `docs/core/current-project-baseline.md`;
3. ADRs vigentes;
4. este documento de input;
5. spec da Fase 04;
6. documentos de protocolo;
7. roadmap;
8. comportamento do MotionLog apenas como referência funcional.

Informações classificadas como não confirmadas não devem ser promovidas automaticamente a requisitos definitivos.

## Princípios obrigatórios

### BLE nativo

Utilizar diretamente as APIs Android BLE.

Não utilizar:

- React Native;
- Expo;
- bibliotecas BLE multiplataforma;
- wrappers externos de GATT;
- bridge JavaScript;
- código copiado mecanicamente do MotionLog.

### Fronteira única de GATT

Toda interação com `BluetoothGatt` deve permanecer dentro da implementação BLE.

Não expor `BluetoothGatt`, `BluetoothGattCharacteristic`, `BluetoothGattService`, `ScanResult` ou outros tipos Android BLE para:

- features;
- ViewModels;
- use cases;
- domínio;
- repositories de negócio;
- componentes Compose.

Apenas a camada Android concreta pode depender diretamente desses tipos.

### Uma única operação por vez

Todas as operações GATT devem passar por uma fila serial única por sessão de conexão.

Não podem existir simultaneamente:

- duas escritas;
- duas leituras;
- escrita e descriptor write concorrentes;
- discovery concorrente com outro comando;
- mudança de MTU concorrente com outra operação;
- múltiplas configurações do mesmo CCCD;
- comandos enviados fora da fila.

### Sem comportamento automático ao abrir

A existência do núcleo BLE não autoriza:

- scan automático ao iniciar o aplicativo;
- conexão automática ao abrir;
- handshake automático ao abrir;
- importação automática de D1;
- importação automática de D3/D4;
- reconexão infinita;
- serviço BLE permanente.

Ações funcionais continuarão sendo iniciadas pelo usuário nas fases correspondentes.

## Arquitetura esperada

A direção conceitual deve ser:

```text
Feature / Use Case
        ↓
Contrato BLE do aplicativo
        ↓
ChampionConnectionManager
        ↓
ChampionBleClient
        ↓
ChampionGattQueue
        ↓
Android BluetoothGatt
```

Para dados recebidos:

```text
BluetoothGattCallback
        ↓
Bridge de callbacks
        ↓
Eventos internos da sessão
        ↓
Router de characteristics
        ↓
ChampionPacket
        ↓
Consumidores do protocolo
```

O callback Android não deve executar regras de negócio diretamente.

## Componentes conceituais

Os nomes definitivos podem ser ajustados pela spec quando houver justificativa, mas as responsabilidades abaixo devem permanecer separadas.

### `ChampionBleScanner`

Responsável por:

- verificar se existe scan em andamento;
- iniciar scan;
- aplicar filtro de dispositivo;
- emitir dispositivos descobertos de forma segura;
- interromper scan ao encontrar candidato adequado;
- interromper scan por timeout;
- interromper scan por cancelamento;
- impedir scans simultâneos;
- garantir cleanup após falha.

Não deve conectar diretamente.

### `ChampionBleClient`

Responsável pelo acesso concreto ao Android BLE:

- criar uma sessão GATT;
- iniciar conexão;
- receber callbacks;
- descobrir services;
- configurar canais;
- executar operações fornecidas pela fila;
- desconectar;
- fechar o GATT;
- invalidar sessões encerradas.

Não deve implementar regras de D0, D1, D3 ou D4.

### `ChampionGattQueue`

Responsável por:

- manter no máximo uma operação ativa;
- ordenar operações pendentes;
- iniciar a próxima operação somente após conclusão da anterior;
- aplicar timeout por operação;
- cancelar operação ativa quando necessário;
- liberar a fila em timeout;
- falhar operações pendentes durante disconnect;
- ignorar callbacks pertencentes a sessões antigas;
- impedir conclusão duplicada;
- aplicar retry somente quando explicitamente permitido;
- produzir resultados tipados.

A fila deve ser determinística e testável sem dispositivo físico.

### `ChampionGattOperation`

Deve representar operações como:

- descoberta de services;
- read characteristic;
- write characteristic;
- descriptor write;
- configuração de notification;
- configuração de indication;
- solicitação de MTU;
- disconnect controlado.

Cada operação deve poder declarar:

- identificador;
- tipo;
- alvo;
- payload, quando aplicável;
- timeout;
- política de retry;
- condição esperada de conclusão;
- quantidade de tentativas;
- sessão à qual pertence.

### `ChampionConnectionManager`

Responsável por orquestrar:

- scan;
- seleção do dispositivo;
- conexão;
- discovery;
- resolução dos canais obrigatórios;
- configuração de notifications;
- estado da sessão;
- disconnect;
- cleanup.

Não deve conter parsers de protocolo.

### `ChampionProtocol`

Responsável por definições estáveis do Champion C001, como:

- UUIDs;
- papéis dos services;
- papéis das characteristics;
- canais de escrita;
- canais de notification;
- CCCD;
- códigos de comando conhecidos;
- classificação de pacotes por canal e byte de comando.

Não deve misturar protocolo com APIs Compose ou Room.

### `ChampionCommand`

Deve representar um comando imutável destinado ao relógio.

Pode conter:

- código;
- payload defensivamente copiado;
- canal de escrita;
- tipo de escrita;
- matcher genérico de resposta;
- timeout sugerido;
- política explícita de retry.

A existência de um código catalogado não significa que seu fluxo funcional já esteja implementado.

### `ChampionPacket`

Deve representar dados recebidos de forma imutável, incluindo apenas metadados necessários, como:

- sessão;
- canal de origem;
- characteristic;
- direção;
- timestamp monotônico ou instante de recepção;
- bytes defensivamente copiados;
- código inicial quando identificável.

Não expor internamente um `ByteArray` mutável compartilhado.

### `ChampionPacketParser`

Nesta fase, deve limitar-se a parsing estrutural ou classificação de envelope, como:

- canal de origem;
- código inicial;
- tamanho;
- correspondência com comando aguardado;
- pacote desconhecido;
- pacote malformado no nível de transporte.

Não implementar parsing profundo de:

- BPM D0/F002;
- histórico D1;
- CRCs D3;
- blocos D4;
- passos FEA1 como regra de produto;
- calibração;
- atividades.

Esses parsers pertencem às fases específicas.

### `ChampionProtocolLogger`

Deve produzir logs estruturados e seguros.

Pode registrar em debug:

- ID da operação;
- ID efêmero da sessão;
- direção;
- alias da characteristic;
- tipo da operação;
- código do comando;
- quantidade de bytes;
- início;
- duração;
- resultado;
- status GATT sanitizado;
- categoria de erro.

Não deve registrar no Logcat:

- payload hexadecimal completo;
- bytes brutos;
- endereço MAC;
- identificador persistente do dispositivo;
- dados de saúde;
- BPM;
- passos;
- credenciais;
- senha do handshake;
- stack trace apresentado à interface.

Uma futura captura diagnóstica privada poderá receber uma cópia dos bytes através de contrato explícito, sem transformar o Logcat em armazenamento de payloads.

## Catálogo BLE confirmado

Centralizar os UUIDs e não duplicá-los entre features.

### Service F008

```text
Service:
f0080001-0451-4000-b000-000000000000

Notify:
f0080002-0451-4000-b000-000000000000

Write:
f0080003-0451-4000-b000-000000000000
```

Uso conhecido:

- comandos A1;
- D0;
- D1;
- D3;
- D4;
- comandos diagnósticos catalogados;
- respostas principais do protocolo.

A escrita deve ser modelada como write with response quando suportada e exigida pelo canal.

### Service F002

```text
Service:
f0020001-0451-4000-b000-000000000000

Notify:
f0020002-0451-4000-b000-000000000000

Write catalogada:
f0020003-0451-4000-b000-000000000000
```

F002 notify é um canal confirmado no código de referência.

F002 write não possui uso funcional confirmado e não deve ser habilitada como requisito de produto nesta fase.

### Service FEE7

```text
Service:
0000fee7-0000-1000-8000-00805f9b34fb

FEA1 notify:
0000fea1-0000-1000-8000-00805f9b34fb

FEA2:
0000fea2-0000-1000-8000-00805f9b34fb

FEC8:
0000fec8-0000-1000-8000-00805f9b34fb
```

FEA1 é um canal confirmado.

FEA2 e FEC8 ainda não foram confirmadas como subscriptions necessárias no aplicativo funcional atual.

A Fase 04 deve:

- permitir representar capabilities opcionais;
- detectar propriedades presentes no dispositivo;
- não falhar a conexão somente porque FEA2 ou FEC8 estão ausentes;
- não assinar FEA2 ou FEC8 por padrão sem decisão explícita;
- manter essas characteristics fora do caminho crítico até validação física.

### CCCD

Centralizar:

```text
00002902-0000-1000-8000-00805f9b34fb
```

A configuração deve respeitar se a characteristic suporta notification ou indication.

Não assumir que chamar apenas `setCharacteristicNotification` conclui a configuração. A escrita do descriptor precisa passar pela fila e aguardar callback correspondente.

## Identificação do dispositivo

O filtro de scan deve considerar a evidência atual:

```text
name ou localName começa com "champion"
comparação case-insensitive
```

Não exigir service UUID no filtro inicial, porque o código de referência não depende desse filtro durante scan foreground.

A implementação deve:

- normalizar nome com segurança;
- considerar `name` e `localName`;
- evitar exceção com nomes ausentes;
- rejeitar dispositivos claramente incompatíveis;
- impedir duplicação excessiva de resultados;
- permitir refinamento futuro após teste físico.

O filtro não deve aceitar qualquer dispositivo apenas porque contém a palavra em posição arbitrária.

## Pré-requisitos e permissões

A camada BLE deve representar de forma tipada:

- dispositivo sem suporte a BLE;
- Bluetooth desligado;
- permissão de scan ausente;
- permissão de conexão ausente;
- permissão legada aplicável ausente;
- localização exigida pela combinação de versão e estratégia;
- operação indisponível pelo estado atual.

O núcleo pode:

- declarar permissões necessárias;
- verificar pré-requisitos;
- informar o que está faltando;
- impedir operação sem autorização.

O núcleo não deve:

- exibir dialogs Compose;
- decidir textos da interface;
- solicitar permissões silenciosamente;
- iniciar scan antes da concessão;
- transformar negação em loop de solicitação.

A experiência visual de solicitação pode ser conectada posteriormente pela feature de dispositivo ou pela prova de conceito.

## Estado da conexão

Evitar múltiplos booleanos concorrentes.

Planejar um estado explícito semelhante a:

```text
Disconnected
Scanning
Connecting
DiscoveringServices
ResolvingChannels
ConfiguringSubscriptions
TransportReady
Disconnecting
Failed
```

O nome definitivo pode ser ajustado.

`TransportReady` significa somente:

- GATT conectado;
- services descobertos;
- canais obrigatórios resolvidos;
- subscriptions principais configuradas.

Não significa:

- handshake confirmado;
- dispositivo autenticado;
- D1 disponível;
- sincronização concluída.

O estado de handshake deverá ser separado ou introduzido pela Fase 05.

## Sessões e callbacks antigos

Cada conexão deve possuir uma identidade efêmera ou geração.

Callbacks de uma conexão encerrada não podem:

- concluir operação da nova conexão;
- alterar estado atual;
- emitir pacote para consumidor atual;
- reabrir fila encerrada;
- disparar retry.

No disconnect:

- marcar a sessão como encerrada;
- parar scan;
- cancelar timeout ativo;
- falhar ou cancelar operações pendentes;
- limpar referências;
- desconectar defensivamente;
- chamar `close()` exatamente de forma controlada;
- impedir uso posterior do GATT encerrado.

## Discovery e registro de canais

Após conexão:

1. descobrir services;
2. validar services obrigatórios;
3. registrar characteristics encontradas;
4. validar propriedades necessárias;
5. diferenciar canais obrigatórios e opcionais;
6. configurar subscriptions obrigatórias;
7. emitir `TransportReady`.

Canais obrigatórios para o caminho inicial:

- F008 write;
- F008 notify.

Canais necessários para as funcionalidades futuras, mas que podem ser avaliados durante preparação:

- F002 notify;
- FEA1 notify.

A spec deve definir claramente quais canais impedem `TransportReady` e quais produzem capability reduzida.

Não espalhar chamadas como `getService()` e `getCharacteristic()` em diversos componentes.

Criar um registro ou snapshot imutável de capabilities resolvidas para a sessão.

## Notifications e indications

Manter uma única subscription física por characteristic e permitir múltiplos consumidores lógicos.

A infraestrutura deve:

- configurar CCCD pela fila;
- aguardar callback da escrita do descriptor;
- distinguir notification de indication;
- emitir pacotes recebidos;
- evitar criar subscriptions físicas duplicadas;
- remover consumidores sem necessariamente desabilitar o canal compartilhado;
- encerrar streams no disconnect;
- proteger contra emissão após fechamento da sessão.

Não usar um listener GATT independente para cada feature.

## Roteamento de respostas

A camada deve permitir que comandos futuros aguardem respostas sem cada feature alterar diretamente o callback Android.

O roteamento pode considerar:

- sessão;
- characteristic;
- código inicial;
- matcher fornecido pelo comando;
- janela temporal;
- cancelamento;
- timeout.

Não implementar nesta fase:

- acumulador multipart de D1;
- acumulador multipart de D3;
- acumulador multipart de D4;
- condição final específica desses protocolos;
- persistência do resultado.

Pacotes não consumidos por um comando podem permanecer disponíveis para streams observadores, conforme decisão da spec.

## Timeouts

Toda operação enfileirada deve possuir timeout explícito.

A spec deve diferenciar pelo menos:

- timeout de scan;
- timeout de conexão;
- timeout de discovery;
- timeout de descriptor write;
- timeout de characteristic read;
- timeout de characteristic write;
- timeout de MTU;
- timeout de disconnect controlado;
- timeout de espera por resposta de protocolo.

Não usar um único timeout global para tudo.

Os valores finais devem ser centralizados e ajustáveis.

Valores do MotionLog podem servir como referência, não como verdade automática.

## Retry

Retries devem ser:

- limitados;
- explícitos;
- observáveis;
- configurados por tipo de operação;
- cancelados ao desconectar;
- proibidos após encerramento da sessão.

Não aplicar retry cego a comandos que possam gerar efeitos duplicados.

A política padrão para comandos de protocolo deve ser sem retry automático, salvo decisão explícita baseada em idempotência conhecida.

Setup de conexão pode usar retry limitado quando seguro e documentado.

Reconexão infinita é proibida.

## Concorrência

A implementação deve definir uma única autoridade de mutação para a sessão BLE.

Pode utilizar, conforme decisão da spec:

- actor baseado em Channel;
- dispatcher dedicado;
- mutex com fila explícita;
- state machine serializada.

Independentemente da técnica escolhida:

- callbacks Android devem ser convertidos em eventos;
- estado não deve ser alterado concorrentemente por múltiplas threads;
- cada operação deve concluir apenas uma vez;
- timeout e callback não podem vencer simultaneamente sem resolução determinística;
- cancelamento deve liberar recursos;
- a fila deve continuar após falha recuperável;
- disconnect deve ter prioridade de encerramento.

## Erros

Criar modelo de erro seguro e tipado.

Considerar categorias como:

- BLE não suportado;
- Bluetooth desligado;
- permissão ausente;
- scan já em andamento;
- scan falhou;
- scan expirou;
- dispositivo não encontrado;
- conexão falhou;
- desconectado inesperadamente;
- discovery falhou;
- service obrigatório ausente;
- characteristic obrigatória ausente;
- operação rejeitada;
- callback com status de falha;
- timeout;
- fila encerrada;
- operação cancelada;
- subscription falhou;
- payload estruturalmente inválido;
- erro interno inesperado.

Status técnicos podem ser preservados para diagnóstico seguro, mas mensagens internas e stack traces não devem chegar diretamente à UI.

## Integração com domínio e persistência

A Fase 04 não deve persistir automaticamente:

- dispositivo encontrado;
- conexão;
- pacote;
- comando;
- payload;
- estado efêmero;
- erro de transporte.

O núcleo BLE pode expor contratos para camadas futuras reagirem aos eventos.

Não acessar diretamente:

- DAOs;
- `ChampionDatabase`;
- `RoomRepositories`;
- Health Connect;
- DataStore de preferências de produto.

Uma integração futura poderá persistir dados do dispositivo por meio dos repositories já existentes.

## Diagnóstico

A Fase 03 já criou modelos para payloads diagnósticos.

A Fase 04 pode preparar uma interface opcional, por exemplo um sink ou observer, capaz de receber cópia privada de pacotes para uso futuro.

Essa interface:

- deve ser desacoplada de Room;
- não deve estar ativa por padrão;
- não deve enviar bytes ao Logcat;
- não deve reter bytes indefinidamente em memória;
- deve copiar o conteúdo;
- deve permitir que uma fase posterior decida sobre persistência e retenção.

Não implementar tela de diagnóstico nesta fase.

## Injeção de dependência

Preparar bindings Hilt para:

- scanner;
- cliente BLE;
- fila ou factory de fila;
- connection manager;
- relógio monotônico ou scheduler usado em timeouts;
- dispatcher BLE;
- logger seguro;
- implementations de plataforma;
- fakes usados em testes quando aplicável.

Evitar singleton global de `BluetoothGatt` acessível livremente.

O connection manager pode ser scoped conforme o ciclo de vida definido na spec.

## Testabilidade

A implementação deve permitir testes sem Champion C001 físico.

Criar abstrações estreitas para o código Android difícil de simular, sem reproduzir toda a API Bluetooth em interfaces próprias.

Planejar testes JVM para:

- filtro de nome;
- catálogo UUID;
- estados da conexão;
- fila FIFO;
- uma operação ativa;
- timeout;
- callback antes do timeout;
- timeout antes do callback;
- conclusão duplicada;
- cancelamento;
- disconnect com fila pendente;
- callback de sessão antiga;
- retry permitido;
- retry proibido;
- response matcher;
- cópia defensiva de bytes;
- sanitização de logs;
- capabilities obrigatórias e opcionais.

Planejar testes instrumentados somente quando dependerem realmente do framework Android.

A prova de conceito com Galaxy S22 e Champion C001 pertence à Fase 05.

## Segurança e privacidade

Não registrar ou expor:

- endereço MAC;
- ID persistente;
- payload hexadecimal;
- bytes brutos;
- dados de saúde;
- senha;
- coordenadas;
- tokens;
- dados pessoais.

Não incluir dados BLE sensíveis em mensagens de erro destinadas à interface.

Não habilitar backup de capturas BLE.

Não criar arquivos públicos de captura.

## Fora do escopo

A Fase 04 não deve:

- implementar handshake A1 completo;
- validar A1 no dispositivo;
- implementar D0;
- interpretar F002 como BPM;
- implementar D1;
- implementar D3;
- implementar D4;
- interpretar FEA1 como resumo diário;
- importar atividades;
- sincronizar BPM;
- sincronizar Health Connect;
- criar tela funcional de conexão;
- implementar pareamento completo;
- implementar foreground service;
- implementar background BLE;
- implementar auto-connect;
- implementar reconexão infinita;
- implementar GPS;
- alterar schema Room;
- persistir payloads automaticamente;
- suportar outros wearables;
- assinar FEA2 ou FEC8 como requisito obrigatório sem validação.

## Critérios de aceite

A Fase 04 será considerada concluída quando:

- existir uma fronteira única para APIs Android BLE;
- nenhuma tela ou domínio acessar `BluetoothGatt`;
- scan possuir timeout, cancelamento e exclusão mútua;
- o filtro localizar candidatos Champion sem depender obrigatoriamente de service UUID;
- existir catálogo central de services e characteristics;
- existir uma fila GATT serial;
- houver no máximo uma operação ativa;
- toda operação possuir timeout;
- timeout liberar a fila;
- disconnect cancelar a sessão e limpar recursos;
- callbacks antigos não afetarem nova sessão;
- discovery produzir capabilities tipadas;
- subscriptions forem configuradas por descriptor write enfileirado;
- houver uma única subscription física por characteristic;
- packets forem expostos por uma fronteira segura;
- bytes forem defensivamente copiados;
- retries forem limitados e explícitos;
- não existir reconexão infinita;
- não existir scan ou conexão automática ao abrir;
- logs não contiverem bytes brutos ou identificadores sensíveis;
- testes JVM cobrirem fila, estado, timeout, retry e cleanup;
- não houver implementação funcional de A1, D0, D1, D3 ou D4;
- não houver dependência de Room, Health Connect ou UI dentro do núcleo BLE;
- o projeto continuar compilando e instalando no Galaxy S22.
