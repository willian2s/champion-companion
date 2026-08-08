# Champion Companion — Roadmap completo do aplicativo Android nativo

## Registro atual de encerramento da Fase 013

- **Status:** `closed`.
- **Resultado:** `partially_delivered`.
- **Motivo:** `product_reorientation`.
- Novos treinos iniciados pelo celular foram descontinuados como direção de
  produto. GPS, rota, `ExerciseRoute` e BLE live em sessão phone foram removidos
  da direção futura e não devem ser expandidos.
- A desativação operacional dos pontos de entrada phone foi implementada em
  `013-17` e revisada.
- Migrations, schemas, sessões, dados, contratos, infraestrutura e evidências
  existentes permanecem preservados até decisão explícita de remoção.
- A direção futura prioriza importação D3/D4, derivação de métricas, revisão e
  sincronização com Health Connect de treinos iniciados pelo relógio.
- A Fase 014 está `completed`, com `014-01` a `014-17` concluídas e progresso
  `17/17`. `014-17` encerrou o follow-up funcional e `014-16` concluiu o
  fechamento documental. A fase preserva os limites físicos registrados nas
  evidências e não reabre a Fase 013.
  WorkManager permanece somente como detalhe técnico de trabalho finito e
  adiável, não como objetivo funcional genérico nem owner de sessão ativa.
- Critérios finais do roadmap não exigem novos treinos phone, GPS, rota ou
  mapa. O histórico abaixo permanece preservado e não constitui backlog ativo.

## 1. Visão geral

O **Champion Companion** será um novo aplicativo Android nativo, desenvolvido em Kotlin e Jetpack Compose, dedicado exclusivamente à integração com o relógio Champion C001.

O aplicativo será uma reimplementação das funcionalidades atualmente existentes no MotionLog mobile, utilizando como baseline funcional a branch do repositório MotionLog:

```text
feat/015-watch-workouts-d3-d4
```

Essa branch representa o estado funcional atualmente utilizado e será considerada a fonte de verdade para:

- comportamento das telas;
- regras de negócio;
- estrutura dos dados;
- conexão BLE;
- handshake com o relógio;
- leitura de passos;
- medição de frequência cardíaca;
- histórico automático de BPM;
- importação de treinos por D3 e D4;
- exercícios iniciados pelo celular;
- calibração de atividades de esteira;
- sincronização com o Health Connect;
- deduplicação;
- revisão das atividades.

O objetivo inicial não é adicionar novas funcionalidades, mas atingir paridade funcional com o aplicativo atual utilizando diretamente as APIs nativas do Android.

---

# 2. Projetos envolvidos

## 2.1 MotionLog mobile atual

Repositório:

```text
willian2s/motionlog-app
```

Baseline funcional:

```text
feat/015-watch-workouts-d3-d4
```

Responsabilidades durante a implementação:

- continuar sendo o aplicativo utilizado diariamente;
- servir como referência funcional;
- fornecer os parsers e regras já validados;
- permitir comparações com a versão nativa;
- permanecer estável durante a implementação;
- receber somente correções importantes.

O MotionLog mobile não deverá receber novas funcionalidades significativas após o início do Champion Companion, salvo quando forem necessárias para corrigir problemas do aplicativo em uso.

---

## 2.2 Champion Companion

Novo repositório sugerido:

```text
willian2s/champion-companion
```

Nome interno do projeto:

```text
ChampionCompanion
```

Nome exibido ao usuário:

```text
Champion Companion
```

Responsabilidades:

- implementação Android nativa;
- persistência local;
- comunicação BLE com o Champion C001;
- interpretação do protocolo;
- importação de dados do relógio;
- revisão de atividades;
- calibração de esteira;
- integração com o Health Connect;
- exercícios iniciados pelo celular;
- diagnóstico da comunicação;
- manutenção futura do aplicativo.

---

# 3. Objetivo do produto

O Champion Companion será uma ponte entre o Champion C001 e os aplicativos integrados ao Health Connect.

Fluxo principal:

```text
Champion C001
      ↓ BLE
Champion Companion
      ↓ validação e armazenamento local
Revisão da atividade
      ↓ sincronização
Health Connect
      ↓
Aplicativos terceiros
```

O aplicativo também permitirá:

- visualizar dados do relógio;
- analisar passos;
- analisar frequência cardíaca;
- consultar atividades;
- revisar dados antes da sincronização;
- corrigir tipos de exercício;
- calibrar atividades de esteira;
- executar medições manuais;
- registrar exercícios pelo celular;
- diagnosticar problemas do protocolo.

O Champion Companion não precisa ser o histórico definitivo de todos os dados de saúde. Seu banco funcionará principalmente como:

- armazenamento operacional;
- cache local;
- histórico recente;
- fonte de revisão;
- mecanismo de deduplicação;
- fila para o Health Connect.

---

# 4. Escopo de dispositivos

A primeira versão será desenvolvida exclusivamente para:

```text
Champion C001
```

Não existe compromisso inicial de suporte a outros relógios.

Mesmo sendo específico para um único dispositivo, o código deverá separar:

- transporte BLE;
- fila GATT;
- comandos do protocolo;
- parsing dos pacotes;
- regras de domínio;
- interface.

Essa separação deverá facilitar a manutenção caso ocorram mudanças de firmware ou variações do dispositivo.

---

# 5. Princípios do projeto

## 5.1 Android nativo

O aplicativo será implementado diretamente com as APIs Android.

Não haverá:

- React Native;
- Expo;
- bridge JavaScript;
- dependência de wrappers BLE;
- dependência de wrappers do Health Connect.

---

## 5.2 Local-first

O banco local será a fonte operacional do aplicativo.

O Health Connect será um destino de sincronização.

O histórico local deverá continuar acessível sem internet.

---

## 5.3 Sincronização controlada

O aplicativo não deverá iniciar scans, importações ou sincronizações automaticamente ao abrir.

Exceção permitida: tentativa controlada de auto-conexão do dispositivo já associado, desde que:

- não dispare prompt automático de permissão;
- não importe D1, D3 ou D4 automaticamente;
- não entre em loop de retry;
- não mantenha BLE permanente.

As ações deverão ser iniciadas pelo usuário, especialmente:

- sincronização de passos;
- sincronização de BPM;
- importação D1;
- importação D3/D4;
- envio de atividades ao Health Connect.

---

## 5.4 BPM completo

Quando o usuário solicitar a sincronização de frequência cardíaca:

- todos os registros válidos encontrados deverão ser importados;
- todos os registros válidos ainda não sincronizados deverão ser enviados ao Health Connect;
- não haverá seleção manual registro por registro;
- falhas parciais deverão permanecer pendentes.

---

## 5.5 Revisão antes do envio de atividades

Toda atividade importada pelo relógio deverá ser criada inicialmente como pendente.

O usuário deverá poder:

- revisar os dados;
- alterar o tipo;
- alterar título e observações;
- calibrar atividades de esteira;
- corrigir distância quando permitido;
- confirmar a atividade;
- sincronizar;
- descartar atividades inválidas.

---

## 5.6 Localização e rota sob autorização controlada — política histórica da Fase 013

Esta seção preserva a política autorizada durante a execução histórica da Fase
013. Após o encerramento da direção phone, ela não é backlog ativo nem autoriza
novos gates ou expansão de implementação.

A política padrão continua sendo não coletar nem persistir localização. A
exceção histórica autorizada pela Fase 13 era estreita:

- somente caminhada ou corrida iniciada explicitamente pelo usuário;
- somente durante sessão ativa, entre início e fim ou descarte;
- somente com localização precisa e foreground service visível;
- rota persistida localmente para recuperação, revisão e sincronização da mesma
  sessão, sem servidor, exportação, backup ou device transfer;
- treino livre e esteira não coletam nem persistem rota.

O aplicativo nunca deverá usar rastreamento passivo, iniciar sessão por boot ou
persistir localização fora dessa sessão explícita. Coordenadas, altitude, rota,
BPM, payload BLE, identificadores pessoais, título e observações não podem
aparecer em logs diagnósticos.

Continuam proibidos em todos os fluxos não autorizados:

- mapas e tiles;
- GPX;
- TCX;
- FIT;
- envio para servidor;
- exportação automática;
- `ExerciseRoute` em atividades da Fase 12, D3/D4 ou fases sem autorização.

---

## 5.7 BLE previsível

Todas as operações GATT deverão passar por uma fila serial.

Não deverão existir:

- escritas simultâneas;
- leituras simultâneas;
- comandos concorrentes;
- operations sem timeout;
- reconexões infinitas;
- acesso direto ao GATT pelas telas.

---

## 5.8 Sem migração do MotionLog

O Champion Companion será um aplicativo independente.

Não será necessário transferir dados do MotionLog.

A perda do histórico local anterior é aceitável porque os dados relevantes já estão disponíveis em aplicativos terceiros.

---

## 5.9 Escopo controlado

A primeira versão deverá priorizar:

- BLE;
- passos;
- BPM;
- D1;
- D3;
- D4;
- revisão;
- calibração;
- Health Connect;
- exercícios;
- estabilidade.

Novas ideias deverão permanecer em backlog até a conclusão da paridade funcional.

---

# 6. Stack tecnológica

## Linguagem e interface

```text
Kotlin
Jetpack Compose
Material 3
Navigation Compose
```

## Arquitetura

```text
ViewModel
StateFlow
Coroutines
Use Cases
Repositories
Dependency Injection
```

## Persistência

```text
Room
DataStore
```

## Android

```text
Android Bluetooth LE APIs
Health Connect SDK
Android Location APIs
Foreground Services
WorkManager
Notification APIs
```

## Injeção de dependência

```text
Hilt
```

## Serialização

```text
Kotlinx Serialization
```

## Testes

```text
JUnit
Kotlin Test
Room in-memory tests
Compose UI tests
Android instrumentation tests
```

---

# 7. Arquitetura inicial

O projeto deverá começar com um único módulo Gradle.

A modularização física deverá ocorrer somente quando houver uma necessidade concreta.

Estrutura sugerida:

```text
app/
  core/
    bluetooth/
    database/
    healthconnect/
    location/
    notifications/
    preferences/
    logging/
    model/
    common/

  feature/
    onboarding/
    home/
    heart/
    calendar/
    exercises/
    activitydetail/
    device/
    profile/
    settings/
    backup/
    diagnostics/
```

Fluxo:

```text
Compose Screen
      ↓
ViewModel
      ↓
Use Case
      ↓
Repository
      ├── Room
      ├── Bluetooth
      ├── Health Connect
      ├── Location
      └── Backup
```

As telas não deverão acessar diretamente:

- DAOs;
- `BluetoothGatt`;
- `HealthConnectClient`;
- APIs de localização;
- arquivos;
- DataStore.

---

# 8. Namespace e identificadores

Namespace Kotlin sugerido:

```text
br.com.mwconsulting.championcompanion
```

Application ID de desenvolvimento:

```text
br.com.mwconsulting.championcompanion.dev
```

Application ID de produção:

```text
br.com.mwconsulting.championcompanion
```

O identificador de desenvolvimento deverá permitir instalação paralela com o MotionLog.

---

# 9. Estados principais

## 9.1 Estado da atividade

```kotlin
enum class ActivityStatus {
    IMPORTING,
    PENDING_CALIBRATION,
    PENDING_REVIEW,
    READY_TO_SYNC,
    SYNCING,
    SYNCED,
    SYNC_ERROR,
    DISCARDED
}
```

---

## 9.2 Origem da atividade

```kotlin
enum class ActivitySource {
    WATCH_D3_D4,
    PHONE_GPS,
    PHONE_TREADMILL,
    PHONE_FREE_WORKOUT,
    MANUAL
}
```

O histórico D1 será tratado como frequência cardíaca, não como atividade física.

---

## 9.3 Estado da conexão

```kotlin
sealed interface DeviceConnectionState {
    data object Disconnected : DeviceConnectionState
    data object Scanning : DeviceConnectionState

    data class Connecting(
        val deviceName: String
    ) : DeviceConnectionState

    data object DiscoveringServices : DeviceConnectionState
    data object ConfiguringNotifications : DeviceConnectionState
    data object Handshaking : DeviceConnectionState
    data object Connected : DeviceConnectionState
    data object Disconnecting : DeviceConnectionState

    data class Failed(
        val reason: String
    ) : DeviceConnectionState
}
```

---

## 9.4 Estado do Health Connect

```kotlin
enum class HealthConnectSyncStatus {
    NOT_REQUESTED,
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}
```

---

## 9.5 Estado de importação

```kotlin
enum class WatchImportStatus {
    PENDING,
    READING_INDEX,
    READING_BLOCKS,
    PARSING,
    COMPLETED,
    PARTIALLY_COMPLETED,
    FAILED,
    CANCELLED
}
```

---

## 9.6 Estado de exercício ativo

```kotlin
enum class ActiveWorkoutState {
    PREPARING,
    ACTIVE,
    PAUSED,
    FINISHING,
    FINISHED,
    FAILED
}
```

---

# 10. Estratégia de repositório e branches

## Repositório

```text
willian2s/champion-companion
```

## Branch principal

```text
main
```

## Branches existentes verificadas

```text
009-watch-heart-rate-history-d1
feat/001-project-bootstrap
feat/002-design-system
feat/003-room-foundation
feat/004-native-ble-core
feat/005-native-ble-poc
feat/006-device-management
feat/007-daily-summary
feat/008-heart-rate
feat/010-watch-workouts-d3-d4
feat/011-activity-review-calibration
feat/012-health-connect
feat/013-phone-workouts
feat/014-watch-workout-derived-metrics
fix/015-11-sync-integrity
```

`feat/014-watch-workout-derived-metrics` é a branch de execução atual da Fase
014. A branch `feat/015-watch-workouts-d3-d4` citada neste roadmap pertence ao
repositório MotionLog e continua sendo somente baseline externa.

## Padrões de branches futuras sugeridos

Usar somente como convenção de nome quando trabalho futuro for aprovado; não são
branches existentes nem reservam tickers de Specs:

```text
feat/<fase>-<slug>
fix/<ticker>-<slug>
```

Não sugerir branch para backup Android, device transfer ou exportação de rota:
`android:allowBackup="false"` e as regras de privacidade atuais permanecem.

## Documentação

```text
docs/
  architecture/
  protocol/
  specs/
  tasks/
  decisions/
  baseline/
```

---

# Fase 0 — Baseline funcional e documentação essencial

## Objetivo

Usar a branch funcional atual como especificação executável sem bloquear o projeto pela captura antecipada de pacotes reais.

Baseline:

```text
feat/015-watch-workouts-d3-d4
```

## Estratégia

O MotionLog já funciona no dispositivo físico.

Não será obrigatório capturar previamente todas as amostras de D1, D3, D4, F002 e demais comandos.

As capturas serão feitas progressivamente:

```text
Implementar comando ou parser
        ↓
Executar no dispositivo físico
        ↓
Comparar com o MotionLog
        ↓
Registrar pacotes necessários
        ↓
Transformar em fixture de referencia
```

## Tarefas

### Referência funcional

- atualizar a branch local;
- registrar o commit funcional;
- confirmar a instalação;
- confirmar os fluxos principais;
- gerar um APK da baseline;
- registrar screenshots;
- documentar os fluxos usados diariamente.

### Inventário funcional

Documentar:

- telas;
- navegação;
- estados vazios;
- estados de erro;
- estados de loading;
- ações manuais;
- ações automáticas;
- permissões;
- comportamento em segundo plano.

### Inventário BLE

Documentar pelo código atual:

- UUIDs;
- services;
- characteristics;
- fila;
- handshake;
- timeouts;
- retries;
- D0;
- D1;
- D3;
- D4;
- D8;
- A1;
- A8;
- F002;
- FEA1.

### Banco atual

Documentar apenas como referência conceitual:

- tabelas;
- entidades;
- relacionamentos;
- regras de deduplicação;
- dados importantes;
- dados desnecessários.

Não será necessário criar compatibilidade com o schema antigo.

### Health Connect

Documentar:

- registros utilizados;
- campos enviados;
- unidades;
- IDs;
- regras de atualização;
- permissões.

## Capturas progressivas

A pasta será criada conforme necessário:

```text
docs/protocol/sample-packets/
```

As capturas serão usadas para:

- validar parsers;
- reproduzir erros;
- criar fixtures e roteiros de validacao;
- comparar resultados.

## Entregáveis

```text
docs/baseline/current-baseline.md
docs/baseline/feature-inventory.md
docs/baseline/feature-parity-checklist.md
docs/baseline/database-reference.md
docs/baseline/health-connect-mapping.md

docs/protocol/champion-c001.md
docs/protocol/ble-command-mapping.md
docs/protocol/d1-history.md
docs/protocol/d3-d4-workouts.md
```

## Critérios de aceite

- commit funcional registrado;
- APK funcional disponível;
- principais fluxos documentados;
- comandos BLE catalogados;
- projeto nativo pode começar sem todas as amostras;
- fixtures serão adicionadas progressivamente.

---

# Fase 1 — Bootstrap do Champion Companion

## Objetivo

Criar a fundação Android.

## Tarefas

- criar repositório;
- criar projeto;
- configurar Kotlin;
- configurar Compose;
- configurar Material 3;
- configurar Navigation;
- configurar Hilt;
- configurar Coroutines;
- configurar StateFlow;
- configurar Kotlinx Serialization;
- configurar logging;
- configurar tratamento de erros;
- configurar lint;
- configurar debug e release;
- configurar versionamento;
- configurar CI;
- criar README;
- criar `AGENTS.md`;
- definir convenções de commit.

## Telas iniciais

- Home;
- Saúde;
- Calendário;
- Exercícios;
- Dispositivo;
- Perfil;
- Configurações.

## Critérios de aceite

- projeto compila;
- instala no Galaxy S22;
- navegação funciona;
- telas abrem;
- tema básico está configurado;
- builds são gerados;
- erros são registrados.

---

# Fase 2 — Design system e identidade visual

## Objetivo

Criar uma fundação visual consistente.

## Tarefas

- definir cores;
- definir tipografia;
- definir espaçamentos;
- definir raios;
- definir elevação;
- definir cards;
- definir botões;
- definir barras de progresso;
- definir gráficos;
- definir chips;
- definir estados;
- definir indicadores BLE;
- definir indicadores do Health Connect;
- criar dark mode;
- avaliar light mode;
- criar ícone;
- criar splash;
- criar ícones de notificação.

## Componentes

```text
HealthCard
MetricCard
ConnectionStatus
SyncStatusBadge
ActivityStatusBadge
PrimaryActionButton
EmptyState
ErrorState
LoadingState
HealthChart
PermissionBanner
PendingReviewBanner
CalibrationBanner
```

## Critérios de aceite

- componentes são compartilhados;
- cores não são duplicadas nas features;
- estados não dependem apenas de cor;
- componentes possuem previews;
- componentes são acessíveis.

---

# Fase 3 — Modelos de domínio e banco local

## Objetivo

Criar a persistência própria do Champion Companion.

## Entidades

```text
UserProfileEntity
DeviceEntity
DailySummaryEntity
HeartRateMeasurementEntity
WeightMeasurementEntity
ExerciseSessionEntity
ExerciseHeartRateSampleEntity
ExerciseIntervalEntity
WatchWorkoutImportEntity
WatchWorkoutRawPacketEntity
HealthConnectSyncItemEntity
```

Configurações simples deverão usar DataStore.

## Atividade

Uma atividade deverá armazenar:

- ID;
- ID estável;
- origem;
- tipo sugerido;
- tipo confirmado;
- status;
- data;
- horário;
- duração;
- passos;
- distância estimada;
- distância original;
- distância calibrada;
- fator de calibração;
- data da calibração;
- calorias;
- BPM médio;
- BPM mínimo;
- BPM máximo;
- observações;
- versão do parser;
- status do Health Connect;
- data da importação;
- data da sincronização.

## Amostras de BPM

Cada amostra deverá armazenar:

- atividade ou medição;
- timestamp;
- BPM;
- índice ou intervalo;
- origem.

## Tarefas

- configurar Room;
- criar entities;
- criar relations;
- criar DAOs;
- criar converters;
- criar índices;
- criar repositories;
- criar migrations próprias;
- configurar DataStore;
- criar dados debug;
- criar política de limpeza;
- criar deduplicação;
- criar transações.

## Dados proibidos

- latitude;
- longitude;
- rota;
- polyline;
- coordenadas brutas.

## Critérios de aceite

- banco criado;
- dados persistem;
- transações são atômicas;
- consultas usam índices;
- telas não acessam Room;
- migrations próprias possuem validacao reproduzivel.

---

# Fase 4 — Núcleo BLE nativo

## Objetivo

Criar uma camada BLE independente da interface.

## Componentes

```text
ChampionBleScanner
ChampionBleClient
ChampionGattQueue
ChampionGattOperation
ChampionProtocol
ChampionCommand
ChampionPacket
ChampionPacketParser
ChampionConnectionManager
ChampionProtocolLogger
```

## Scan

- solicitar permissões;
- verificar Bluetooth;
- iniciar scan;
- filtrar Champion C001;
- parar ao encontrar;
- tratar timeout;
- impedir scans simultâneos.

## Conexão

- conectar;
- descobrir services;
- localizar characteristics;
- configurar MTU quando necessário;
- habilitar notifications;
- habilitar indications;
- detectar desconexão;
- fechar GATT.

## Fila GATT

Operações:

- read;
- write;
- descriptor write;
- notifications;
- MTU;
- comandos.

Cada operação terá:

- ID;
- timeout;
- resultado;
- erro;
- retry limitado.

## Logging

Registrar em debug:

- timestamp;
- direção;
- characteristic;
- payload hexadecimal;
- comando;
- estado;
- duração;
- erro.

## Critérios de aceite

- nenhuma operação concorrente;
- conexão encerra corretamente;
- timeout libera fila;
- cliente reutilizável;
- pacotes registrados no debug.

---

# Fase 5 — Prova de conceito BLE

## Objetivo

Validar o fluxo mínimo no dispositivo físico.

## Fluxo

```text
Scan
  ↓
Conexão
  ↓
Descoberta
  ↓
Notifications
  ↓
Handshake A1
  ↓
Passos FEA1
  ↓
BPM D0/F002
```

## Tarefas

- implementar A1;
- validar respostas;
- ler passos;
- iniciar BPM;
- receber F002;
- identificar valor final;
- parar BPM;
- tratar desconexão;
- repetir ciclos de validacao;
- criar tela técnica;
- exportar logs.

## Critérios de aceite

No Galaxy S22, executar dez vezes:

1. encontrar;
2. conectar;
3. handshake;
4. passos;
5. iniciar BPM;
6. receber BPM;
7. finalizar;
8. desconectar.

## Gate A

O projeto só avança após estabilidade desse fluxo.

---

# Fase 6 — Gerenciamento do dispositivo

## Objetivo

Transformar a prova de conceito em uma feature utilizável.

## Tarefas

- listar dispositivos;
- salvar Champion C001;
- exibir nome;
- exibir endereço;
- exibir conexão;
- exibir última conexão;
- exibir última sincronização;
- tentar auto-conectar dispositivo associado ao abrir app ou tela quando pré-requisitos já estiverem satisfeitos;
- conectar;
- desconectar;
- esquecer;
- tentar novamente;
- exibir erros;
- abrir diagnóstico;
- exportar logs.

## Critérios de aceite

- dispositivo lembrado;
- auto-conexão do associado é limitada e previsível;
- conexão não é agressiva;
- usuário controla scan e pode intervir na conexão;
- falhas não bloqueiam;
- UI reflete estado real.

---

# Fase 7 — Passos, Home e resumo diário

## Objetivo

Criar a primeira experiência diária.

## Tarefas

- importar passos;
- salvar resumo;
- estimar distância;
- estimar calorias;
- mostrar origem;
- mostrar última atualização;
- criar sincronização manual;
- tratar mudança de dia;
- consolidar leituras;
- impedir redução acidental;
- atualizar manualmente.

## Home

Exibir:

- passos;
- meta;
- distância estimada;
- calorias estimadas;
- último BPM;
- último treino;
- atividades pendentes;
- estado do relógio;
- última sincronização.

## Critérios de aceite

- dados persistem;
- Home usa banco;
- não duplica;
- estimativas são identificadas.

---

# Fase 8 — Frequência cardíaca manual

## Objetivo

Portar a medição manual e iniciar a área Saúde como hub de métricas persistidas.

## Tarefas

- transformar Saúde em hub inicial de métricas;
- criar navegação de Saúde para detalhes de frequência cardíaca, peso, passos e distância;
- criar cards de resumo com valor atual, unidade, data/hora, origem/estimativa e estado sem dados;
- mover ownership visual da medição manual para tela específica de frequência cardíaca;
- criar fluxo;
- conectar;
- iniciar D0;
- interpretar F002;
- mostrar BPM em tempo real;
- exibir estado;
- cancelar;
- finalizar;
- usar valor final;
- salvar;
- tratar erro;
- tratar desconexão;
- impedir simultaneidade;
- criar shells das telas de peso, passos e distância usando dados já persistidos;
- reservar seções próprias de gráfico e histórico sem implementação funcional completa.

## Interface

Saúde passa a exibir:

- card de última frequência cardíaca;
- card de último peso;
- card de passos;
- card de distância;
- navegação para tela específica de cada métrica.

Tela de frequência cardíaca passa a concentrar:

- valor atual;
- ação de medir frequência cardíaca;
- feedback da sessão manual;
- histórico manual simples já suportado;
- slots futuros para gráfico, histórico completo e ação de buscar dados do relógio.

Peso, passos e distância nesta fase exibem apenas shells iniciais com valor atual e estados vazios seguros para gráfico/histórico.

## Critérios de aceite

- medição funciona;
- resultado persiste;
- cancelada não conclui;
- stream é encerrado;
- não existem duas medições;
- Saúde funciona como hub inicial sem iniciar BLE pela rota raiz;
- frequência cardíaca possui tela específica como owner visual da medição manual;
- peso, passos e distância possuem telas shell sem dados simulados;
- gráficos e históricos completos ainda não são exigidos nesta fase.

---

# Fase 9 — Histórico D1 local-first e fila pendente de BPM

## Objetivo

Importar o histórico automático do relógio, persistir todos os registros válidos localmente, consolidar histórico de BPM e deixá-los prontos em fila pendente para sincronização real posterior.

## Fluxo

```text
Usuário toca em Buscar dados do relógio
        ↓
Consulta D1
        ↓
Validação
        ↓
Deduplicação
        ↓
Persistência local
        ↓
Fila `PENDING`
        ↓
Histórico e gráfico local
```

## Tarefas

- integrar ação `Buscar dados do relógio` na tela específica de frequência cardíaca;
- portar D1;
- portar parser;
- interpretar timestamps;
- tratar timezone;
- validar BPM;
- ignorar inválidos;
- deduplicar;
- importar por período;
- registrar última consulta;
- mostrar progresso;
- permitir cancelamento;
- tratar timeout;
- permitir retry;
- manter falhas pendentes;
- criar ou reparar operação `PENDING` por medição D1 elegível;
- consolidar medições manuais e automáticas de BPM;
- fornecer agregações para frequência cardíaca;
- registrar última importação D1 local.

## Registros válidos

Deverão possuir:

- timestamp interpretável;
- timestamp dentro do período;
- BPM dentro dos limites aceitos;
- estrutura reconhecida;
- ausência de duplicata local.

## Fronteira com Health Connect

Fase 09 não executa escrita real no Health Connect.

Fase 09 deve:

- persistir todos os registros D1 válidos;
- garantir fila `PENDING` por registro elegível;
- preservar ausência de seleção individual;
- manter dados locais utilizáveis mesmo sem provider.

Fase 09 não deve implementar:

- disponibilidade do provider;
- permissões;
- escrita real;
- lotes reais;
- contadores de enviados e erros do provider.

## Falhas

- timeout com dados preserva resultado parcial local;
- timeout vazio falha sem persistência;
- falhas locais preservam pendências já criadas quando aplicável;
- retry não repete persistência nem cria duplicatas locais.

## Interface

- integrar ação `Buscar dados do relógio` à tela específica de frequência cardíaca;
- histórico diário;
- semanal;
- mensal;
- média;
- mínimo;
- máximo;
- gráfico;
- última importação;
- quantidade importada;
- estado local da importação D1.

## Regra

D1 não será consultado ao abrir o app.

Health Connect real permanece fora desta fase.

## Critérios de aceite

- não duplica;
- timestamps preservados;
- inválidos registrados em log;
- registros válidos ficam persistidos localmente;
- fila `PENDING` fica pronta para processamento futuro;
- falhas locais são retomáveis;
- sem seleção individual;
- nenhuma escrita real no Health Connect ocorre.

---

# Fase 10 — Importação de treinos D3/D4

## Objetivo

Portar o fluxo funcional de treinos do relógio.

## Fluxo

```text
Usuário solicita importação
        ↓
D3
        ↓
Blocos e CRCs
        ↓
D4
        ↓
Remontagem
        ↓
Parser
        ↓
Atividade pendente
```

## D3

- portar parser;
- interpretar blocos;
- interpretar CRCs;
- tratar little-endian;
- identificar válidos;
- identificar vazios;
- detectar inconsistências.

## D4

- solicitar sequencialmente;
- controlar ordem;
- timeout;
- retry;
- detectar duplicatas;
- detectar ausência;
- remontar payload;
- validar tamanho;
- validar CRC quando possível;
- permitir retomada.

## Parser

Extrair quando disponível:

- data;
- hora;
- duração;
- passos;
- distância;
- distância por intervalo;
- calorias;
- BPM por intervalo;
- BPM médio;
- BPM mínimo;
- BPM máximo;
- tipo;
- amostras;
- agregados.

## Calorias

Quando confirmado:

```text
calories = caloriesRaw / 1000
```

## Distância

- preservar total;
- armazenar intervalos;
- não substituir agregado sem validação.

## Sugestão de tipo

```text
menos de 1.000 passos → treino livre
1.000 passos ou mais → corrida
```

A sugestão será editável.

## Deduplicação

Usar:

- origem;
- horário;
- duração;
- CRC;
- bloco;
- contador;
- hash.

## Pacotes brutos

Armazenamento temporário para:

- diagnóstico;
- correção;
- auditoria.

Deverá haver limpeza automática.

## D8 e A8

Permanecem como diagnóstico ou fallback.

## Critérios de aceite

- treino real importado;
- treino longo importado;
- blocos remontados;
- sem duplicação;
- dados preservados;
- atividade pendente;
- sem envio automático.

## Gate B

D3/D4 deverão funcionar com os mesmos treinos da baseline.

---

# Fase 11 — Revisão e calibração das atividades

**Status:** concluída em 28/07/2026. Fase termina em `READY_TO_SYNC`; sincronização Health Connect pertence à Fase 12.

## Objetivo

Permitir revisão completa local e calibrar, quando necessário, até `READY_TO_SYNC`.

## Home

- banner de pendências;
- quantidade;
- acesso direto;
- destaque de calibração.

## Lista

Filtros:

- todas;
- aguardando calibração;
- pendentes;
- prontas;
- sincronizadas;
- com erro;
- descartadas.

## Detalhes

Exibir:

- tipo;
- tipo sugerido;
- origem;
- data;
- duração;
- distância;
- ritmo;
- velocidade;
- passos;
- calorias;
- BPM médio;
- BPM mínimo;
- BPM máximo;
- gráfico;
- intervalos;
- calibração.

## Edição

Permitir:

- tipo;
- título;
- observações;
- classificação.

## Calibração de esteira

O usuário deverá informar a distância real exibida pela esteira.

Exemplo:

```text
Distância estimada: 4,72 km
Distância da esteira: 5,00 km
```

Recalcular:

- distância final;
- ritmo médio;
- velocidade média;
- métricas derivadas;
- fator de calibração.

## Dados de calibração

Armazenar:

- distância estimada;
- distância informada;
- fator;
- data da calibração;
- indicador de ajuste manual.

## Fluxo

```text
PENDING_REVIEW
        ↓
Confirmar tipo esteira
        ↓
PENDING_CALIBRATION
        ↓
Usuário informa distância
        ↓
Recalcular
        ↓
READY_TO_SYNC
```

## Ações

- calibrar;
- editar;
- salvar;
- marcar revisada;
- descartar;
- diagnosticar.

## Fronteira da fase

Saída da Fase 11:

```text
READY_TO_SYNC
```

Atividades prontas continuam locais até uma fase posterior de sincronização.

## Critérios de aceite

- pendências visíveis;
- tipo editável;
- esteira calibrável;
- ritmo recalculado;
- revisão não esteira produz `READY_TO_SYNC`;
- revisão de esteira produz `PENDING_CALIBRATION` e depois `READY_TO_SYNC`;
- sem envio antes da confirmação;
- falhas não apagam dados.

---

# Fase 12 — Health Connect nativo

Status de fechamento: concluída em 28/07/2026. Gate C foi validado manualmente
no Galaxy S22 com Health Connect real, após correção da identidade de sync por variante.

**Refinamento pós-fechamento (Spec 016):** sincronização de passos migrou de
operações cumulativas para incrementos imutáveis, com checkpoint transacional,
retry determinístico e compatibilidade com legado. Ver
`docs/specs/016-health-connect-incremental-daily-steps.md`.

**Refinamento visual independente (Spec 017):** margens e cabeçalhos de
Calendário, detalhe diário, Dispositivo, Selecionar treino e Preparar serão
alinhados ao padrão visual existente, sem reabrir Fases 006, 013 ou 015 e sem
alterar contratos funcionais. Ver
`docs/specs/017-alinhamento-visual-margens-cabecalhos.md` e o overview em
`docs/tasks/017-alinhamento-visual-margens-cabecalhos/017-00-overview.md`.

## Objetivo

Implementar integração nativa write-only com Health Connect, consumindo backlog local persistido e mantendo Room como fonte operacional e visual primária.

Fluxo principal da fase:

```text
Persistência local
        ↓
Fila persistida
        ↓
Disponibilidade e permissão
        ↓
Mapeamento Health Connect
        ↓
Upsert idempotente
        ↓
SYNCED ou FAILED
```

## Tipos

- passos;
- BPM;
- sessão;
- distância;
- calorias;
- peso;
- altura.

## Não enviar na Fase 12

- rota;
- latitude;
- longitude;
- polyline;
- GPX;
- TCX;
- FIT.

## Princípios

- write-only;
- local-first;
- sem leitura de dados de outros apps;
- sem `WorkManager`;
- sem foreground service;
- sem sync ao abrir o app;
- sem `Steps` separado para atividade;
- sem rota na Fase 12; rota somente conforme política controlada da Fase 13.

## Tarefas

- detectar disponibilidade;
- solicitar permissões de escrita por capacidade;
- mostrar permissões faltantes;
- criar repository;
- criar mapeadores;
- criar IDs estáveis;
- criar versões;
- insert;
- update idempotente por `clientRecordVersion`;
- fila;
- retry foreground;
- erros;
- rationale de privacidade;
- manage access;
- data de sincronização;
- processar todas operações D1 `PENDING` acumuladas desde a Fase 09;
- escrever todas amostras D1 válidas ainda não sincronizadas;
- tentar sync automático de passos após refresh persistido;
- tentar sync automático de BPM manual após persistência;
- tentar backlog D1 após importação persistida;
- sincronizar atividade somente por ação explícita;
- mapear atividade em records granulares independentes;
- sincronizar peso e altura somente quando houver dados locais elegíveis;
- manter falhas parciais como pendentes ou failed sem desfazer dados locais.

## Entrada da fase

- atividades D3/D4 revisadas em `READY_TO_SYNC`;
- backlog D1 `PENDING` acumulado desde a Fase 09;
- medições manuais válidas já persistidas;
- resumos diários válidos já persistidos;
- peso e altura locais elegíveis.

## IDs

Cada registro terá:

```text
clientRecordId
clientRecordVersion
```

Formato determinístico esperado:

```text
<applicationId>:<aggregateType>:<aggregateId>:<recordType>
```

## BPM

Enviar amostras com timestamps.

Não enviar apenas média quando houver amostras.

Para D1, fila local da Fase 09 e fonte de verdade para backlog de escrita.

Para medição manual, usar dado local persistido como origem de uma operação por medição.

## Sessão

Enviar:

- início;
- fim;
- tipo confirmado;
- título;
- notas.

Complementos da atividade são granulares e independentes:

- distância final;
- calorias;
- BPM.

Passos da atividade não geram `StepsRecord` separado.

## Esteira

Usar a distância calibrada.

## Permissões

- passos exigem apenas escrita de passos;
- BPM manual e D1 exigem apenas escrita de frequência cardíaca;
- sessão de exercício exige apenas escrita de exercício;
- distância, calorias e BPM da atividade exigem permissões próprias e independentes;
- peso e altura só são pedidos quando houver dado local elegível.

## Triggers

```text
Passos:
persistir -> liberar BLE -> tentar sync

BPM manual:
persistir -> liberar BLE -> tentar sync

D1:
persistir lote -> liberar BLE -> processar backlog

Atividade:
READY_TO_SYNC -> ação explícita -> sync
```

Quando `healthSyncEnabled` estiver desativado:

```text
persistir -> manter PENDING -> não tentar
```

## Falha parcial

- sessão já sincronizada não deve ser apagada quando complemento falhar;
- retry processa apenas pendentes ou falhos;
- atividade só vai para `SYNCED` quando todas as operações elegíveis concluírem.

## Fora do escopo

- leitura do Health Connect;
- importação de dados de outros apps;
- `ChangeLogs`;
- `WorkManager`;
- retry em background;
- foreground service;
- sincronização periódica;
- sincronização ao abrir o app.

## Critérios de aceite

- atividade revisada é enviada;
- atividade `READY_TO_SYNC` é consumida sem reabrir revisão local;
- backlog D1 pendente da Fase 09 é processado integralmente;
- reenvio não duplica;
- atualização funciona;
- erros aparecem;
- passos sincronizam sem duplicação por resumo diário;
- BPM manual sincroniza sem rebaixar sucesso local;
- atividade não cria `Steps` separado;
- falha parcial é retomável por record granular;
- peso e altura sincronizam somente quando elegíveis;
- nenhuma rota;
- sincronização controlada;
- integração permanece write-only;
- integração Health Connect não substitui Room/repositories locais como fonte visual primária da área Saúde.

## Gate C

Fluxo diário completo:

```text
Champion C001
      ↓
Champion Companion
      ↓
Revisão
      ↓
Health Connect
```

---

# Fase 13 — Exercícios iniciados pelo Champion Companion

**Status:** `closed`, sem renumeração. **Outcome:** `partially_delivered`.
**Motivo:** `product_reorientation`.

Novos treinos iniciados pelo celular não continuam como direção de produto. A
desativação operacional dos pontos de entrada phone foi implementada em
`013-17` e revisada. Migrations, schemas,
sessões, dados, infraestrutura e evidências existentes devem ser preservados.

## Direção atual de encerramento

- `013-01..07` e `013-09` permanecem `completed`.
- `013-08`, `013-10`, `013-11` e `013-12..16` permanecem `cancelled`, com
  históricos, riscos e evidências preservados; cancelamento não é aceite.
- `013-17` está `completed`; a desativação operacional foi revisada e a Fase 013
  está fechada como `partially_delivered`.
- A Fase 013 não recebe novos gates físicos phone. A futura direção é a
  importação, derivação, revisão e sincronização de treinos iniciados pelo
  relógio.

## Critérios finais de encerramento

- documentação e matriz de status refletem `9 completed`, `8 cancelled` e
  `0 in_progress`;
- dados, migrations, schemas e evidências permanecem preservados;
- não se exige novo treino phone, GPS, rota, `ExerciseRoute` ou mapa;
- a operação de desativação dos pontos de entrada phone está implementada e
  revisada;
- a Fase 014 é redefinida para treinos do relógio, com WorkManager somente para
  tarefas finitas e adiáveis.

## Histórico do escopo original

## Objetivo histórico

Portar os exercícios do aplicativo.

## Tipos

- caminhada;
- corrida;
- treino livre;
- esteira.

## Comum

- iniciar;
- persistir sessão;
- controlar duração;
- pausar;
- retomar;
- finalizar;
- descartar;
- recuperar;
- coletar BPM;
- salvar amostras;
- iniciar countdown persistido e cancelável;
- criar atividade pendente.

## Caminhada e corrida

- solicitar localização;
- iniciar foreground service mínimo ao fim do countdown, enquanto o app estiver
  visível;
- receber localizações;
- filtrar pontos;
- persistir pontos aceitos incrementalmente, com segmentos;
- calcular distância a partir dos pontos persistidos;
- calcular velocidade;
- calcular ritmo;
- remover rota e amostras transitórias quando houver descarte antes da
  atividade;
- sincronizar a rota na mesma `ExerciseSessionRecord`, quando autorizada.

## Treino livre

- funcionar sem GPS;
- registrar duração;
- BPM;
- calorias estimadas;
- edição posterior.

## Esteira

- funcionar sem GPS;
- registrar duração;
- registrar passos;
- registrar BPM;
- estimar distância quando possível;
- deixar claro que é estimativa;
- finalizar sem distância real;
- criar `PENDING_CALIBRATION`;
- permitir informar distância da esteira;
- recalcular ritmo;
- recalcular velocidade;
- enviar valor calibrado.

## Fluxo de esteira

```text
Iniciar
   ↓
Registrar tempo, passos e BPM
   ↓
Finalizar
   ↓
Informar distância da esteira
   ↓
Recalcular
   ↓
Revisar
   ↓
Health Connect
```

## Critérios históricos (não vigentes)

- caminhada e corrida calculam distância;
- rota somente é persistida em sessão explícita de caminhada/corrida;
- treino livre sem localização;
- esteira sem GPS;
- esteira calibrável;
- sessão recuperável;
- atividade revisável.

---

# Fase 14 — Refinamento, derivação e sincronização dos treinos do relógio

**Status:** `completed`. **Progresso:** `17/17`.

A Fase 014 não é uma fase genérica de WorkManager. Seu objetivo funcional é
refinar importação, derivação, revisão e sincronização de treinos iniciados pelo
relógio. O foreground service mínimo da sessão phone permanece apenas como
infraestrutura histórica preservada da Fase 013.

Spec e planejamento executável:

- `docs/specs/014-watch-workout-derived-metrics.md`;
- `docs/tasks/014-watch-workout-derived-metrics/014-00-overview.md`;
- `docs/decisions/adr-018-canonical-workout-time-series-and-health-projection.md`.

## Objetivo atual

Priorizar o fluxo:

```text
Champion C001 inicia e encerra
        ↓
Importação D3/D4 explícita
        ↓
Derivação de métricas por passos e `stepLengthMeters`
        ↓
Revisão local
        ↓
Sincronização Health Connect granular
```

O relógio é owner temporal do treino. GPS, rota, mapa e `ExerciseRoute` não são
obrigatórios. A distância original do relógio é diagnóstica; a distância
canônica deriva exclusivamente da série de passos válidos e
`stepLengthMeters`, conforme ADR-017 e ADR-018.

## Estado das tasks

- `014-01..13`: `completed` — auditoria, contratos, derivação, consumidores e
  writer granular das projeções Health Connect.
- `014-14`: `completed` — reconciliação do agregado legado, tombstones,
  checkpoints, retries e remoção exata de filhos obsoletos.
- `014-15`: `completed` — validação física; Gates A e F passaram somente nos
  cenários exercitados, e Gates B, C, D, E e G ficaram `N/A` com justificativa.
- `014-17`: `completed` — follow-up funcional de autorização, resultado por filho,
  falha parcial e retry, sem leitura ampla do Health Connect.
- `014-16`: `completed` — fechamento documental factual da fase.
- `021-01..04`: `completed` — duração D4 autoritativa, timeline normalizada,
  candidate semântico hidratado e validação do banco preservado/calendário.

Gate F não representa cobertura física universal: a observação manual do provider
permanece limitada aos cenários exercitados e o aplicativo continua write-only.
Fixtures físicos não executados, midpoint, delete-before-insert e cobertura de
firmwares permanecem riscos residuais documentados. Parser efetivo novo é
`d4-v3`, derivação `watch-d3-d4-v2`, e `MIGRATION_8_9`/Room v9,
`MIGRATION_9_10`/schema v10 e `MIGRATION_10_11`/schema v11 permanecem no
histórico aditivo.

Relatório de `014-01`:

```text
docs/integration/014-01-d4-layout-and-consumer-audit.md
```

A auditoria confirmou a ponte controlada `d4-v1 steps → rawMotionValue`, a
necessidade de índices contínuos, a prioridade legada de distância, o bloqueio
atual de esteira e a cardinalidade atual da fila Health Connect.

## WorkManager — detalhe técnico finito e adiável

WorkManager pode ser usado somente, quando houver requisito futuro explícito,
para tarefas finitas e adiáveis como:

- reprocessamento;
- reenvio autorizado;
- limpeza;
- backup;
- restauração;
- tarefas adiáveis.

O WorkManager não controla captura GPS, sessão ativa, foreground service,
importação interativa, revisão ou sync imediato. Fica reservado para trabalho
adiável posterior, com retry limitado e consentimento operacional apropriado.

## Não implementar

- BLE permanente;
- sincronização agressiva;
- BPM contínuo;
- D1 ao abrir;
- importação silenciosa;
- retry infinito.

## Critérios de aceite da direção redefinida

- importação D3/D4 explícita preserva início e fim definidos pelo relógio;
- `d4-v1` e `d4-v2` preservam proveniência legada e novas importações usam
  `d4-v3`;
- índices da série V1 são contínuos, únicos e iniciam em zero;
- derivação de distância usa a soma exata dos intervalos canônicos;
- distância original do relógio permanece diagnóstica e não vira fallback;
- revisão local precede sincronização Health Connect granular;
- falhas parciais permanecem rastreáveis e recuperáveis;
- tarefas adiáveis, quando aprovadas, são finitas e não controlam sessão ativa;
- sem GPS, rota, mapa ou `ExerciseRoute` obrigatórios;
- nenhum critério exige novos treinos iniciados pelo celular.

## Dependências planejadas

```text
014-01
   ↓
014-02
   ↓
014-03
   ├── 014-04 → 014-05 ─┐
   └── 014-06 ──────────┤
                        ↓
                      014-07
                        ↓
                      014-08
                     ┌──┴──┐
                     ↓     ↓
                   014-09  014-11
                     ↓
                   014-10
                     ↓
                   014-12
                     ↓
                   014-13
                     ↓
                   014-14
                     ↓
                   014-15
                     ↓
                   014-16
```

`014-15` depende também da conclusão de `014-11`. `014-16` é exclusivamente
documental e não pode conter correções funcionais.

Nenhuma task da Fase 014 autoriza GPS, rota, mapa, treino phone, BLE live,
importação silenciosa, TCX, FIT, GPX ou remoção de legado da Fase 013.

---

# Fase 15 — Calendário e histórico consolidado

**Status:** concluída em 29/07/2026.

A fase usa somente dados locais e não depende funcionalmente das Fases 13 e 14.

## Objetivo

Centralizar dados por dia.

## Tarefas

- calendário mensal;
- dias com passos;
- dias com BPM;
- dias com exercícios;
- resumo diário;
- meta de passos;
- exercícios;
- BPM;
- peso;
- sincronizações;
- navegação;
- estados vazios;
- gráficos de passos;
- histórico de passos;
- gráficos de distância;
- histórico de distância;
- navegação temporal das métricas diárias.

Entrega validada documentalmente com dados locais, sem acesso a BLE ou Health
Connect. A validação offline, builds, instalação e screenshots estão registradas
em `docs/tasks/015-calendar-consolidated-history/015-12-run-manual-offline-calendar-validation.md`.
Riscos residuais aceitos permanecem registrados no overview da Fase 15. A spec
015A corrigiu a integridade semântica da fila sem reabrir esta fase.

## Critérios de aceite

- calendário consistente;
- detalhe não consulta BLE;
- funciona offline;
- pendências destacadas.

---

# Fase 16 — Perfil e cálculos pessoais

## Objetivo

Manter dados usados nas estimativas.

## Dados

- nome;
- nascimento;
- sexo quando necessário;
- altura;
- peso;
- meta de peso;
- meta de passos;
- passada;
- unidades;
- zonas de BPM.

## Tarefas

- formulário;
- validação;
- persistência;
- histórico de peso;
- recalcular estimativas;
- sincronização manual;
- identificar estimativas;
- personalizar passada;
- edição e gerenciamento de peso na tela específica da métrica e/ou fluxo equivalente de perfil.

## Critérios de aceite

- cálculos atualizados;
- histórico preservado;
- dados sincronizáveis;
- estimativas identificadas.

---

# Fase 17 — Backup e restauração

## Objetivo

Proteger apenas os dados do Champion Companion.

## Escopo

- perfil;
- dispositivos;
- resumos;
- BPM;
- peso;
- exercícios;
- amostras;
- intervalos;
- calibrações;
- estados de sincronização;
- configurações.

## Formato

- versão do app;
- versão do schema;
- data;
- ID;
- checksum;
- dados serializados.

## Tarefas

- exportação local;
- importação local;
- checksum;
- validação de versão;
- preview;
- backup pré-restauração;
- impedir sobrescrita;
- conflito;
- nuvem opt-in;
- excluir logs sensíveis.

## Limites

Não precisa ser compatível com:

- MotionLog;
- exportações antigas;
- banco antigo.

## Critérios de aceite

- cria backup;
- restaura;
- rejeita incompatíveis;
- cria segurança;
- valida registros.

---

# Fase 18 — Transição sem migração

## Objetivo

Adotar o Champion Companion como aplicativo independente.

## Estratégia definida

Usar novo:

- nome;
- projeto;
- application ID;
- banco;
- ciclo de dados.

## Sem transferência

Não importar:

- passos antigos;
- BPM antigo;
- peso antigo;
- exercícios antigos;
- atividades;
- configurações;
- IDs do Health Connect;
- banco SQLite;
- backups do MotionLog.

## Primeira execução

- criar banco vazio;
- solicitar permissões;
- selecionar Champion C001;
- registrar início de uso;
- começar novos dados;
- não procurar MotionLog;
- não importar arquivos antigos.

## Histórico ainda no relógio

Dados disponíveis no Champion C001 poderão ser importados por:

- D1;
- D3;
- D4.

Seguirão:

- validação;
- deduplicação interna;
- revisão;
- sincronização.

Não haverá deduplicação cruzada com o MotionLog.

## Tarefas

- definir application ID;
- permitir instalação paralela;
- criar onboarding;
- criar banco limpo;
- remover dependência de schema antigo;
- documentar ausência de migração;
- validar uso paralelo;
- permitir remoção futura do MotionLog.

## Critérios de aceite

- instala ao lado;
- banco vazio;
- não acessa MotionLog;
- sem transferência;
- novos dados sincronizam;
- histórico antigo não é requisito;
- MotionLog pode ser removido.

---

# Fase 19 — Diagnóstico e observabilidade

## Objetivo

Facilitar investigação.

## Tarefas

- tela de diagnóstico;
- versão do app;
- versão do parser;
- dispositivo;
- estado BLE;
- último erro;
- última sincronização;
- fila GATT;
- copiar relatório;
- exportar logs;
- exportar D3/D4;
- mascarar dados;
- retenção;
- reduzir logs em produção.

## Critérios de aceite

- BLE investigável;
- exportação controlada;
- limpeza;
- proteção de dados.

---

# Fase 20 — Segurança e privacidade

## Objetivo

Proteger dados de saúde.

## Tarefas

- revisar permissões;
- solicitar somente necessárias;
- explicar permissões;
- não armazenar localização fora da sessão explícita de caminhada/corrida
  autorizada pela Fase 13;
- não enviar para servidores;
- proteger backups;
- evitar dados pessoais em logs;
- usar armazenamento privado;
- documentar retenção;
- apagar todos os dados;
- desconectar Health Connect;
- política de privacidade;
- inventário de dados.

## Critérios de aceite

- conjunto mínimo;
- negar Health Connect mantém histórico;
- negar localização afeta apenas GPS;
- usuário apaga dados;
- nenhuma rota fora da política controlada da Fase 13.

---

# Fase 21 — Testes e qualidade

## Objetivo

Criar segurança para substituir o app atual.

## Testes unitários

- D1;
- D3;
- D4;
- little-endian;
- calorias;
- distância;
- BPM;
- deduplicação;
- sugestão de tipo;
- calibração;
- Health Connect;
- estados;
- pace;
- distância;
- retries.

## Banco

- inserção;
- atualização;
- relações;
- transações;
- migrations próprias;
- deduplicação;
- limpeza;
- calibração.

## Interface

- Home;
- pendências;
- calibração;
- revisão;
- erros;
- permissões;
- estados vazios;
- sincronização.

## Validacao Android Dependente De Framework

- Health Connect;
- foreground service;
- recuperação;
- localização;
- notificações;
- backup.

## BLE falso

Criar:

```text
FakeChampionBleClient
RecordedChampionPacketSource
```

Simular:

- timeout;
- desconexão;
- duplicata;
- bloco ausente;
- ordem incorreta;
- payload inválido;
- CRC inválido;
- sessão completa.

## Critérios de aceite

- parsers com fixtures reais;
- migrations próprias validadas;
- fluxos críticos cobertos;
- BLE reproduzível;
- sem bug crítico conhecido.

---

# Fase 22 — Validação em dispositivos reais

## Objetivo

Validar diferenças Android.

## Principal

```text
Samsung Galaxy S22
```

## Adicionais

Quando possível:

- outro Samsung;
- Motorola ou Xiaomi;
- Android anterior.

## BLE

- scan;
- conexão;
- handshake;
- passos;
- BPM;
- D1;
- D3;
- D4;
- reconexão;
- Bluetooth desligado;
- fora de alcance;
- tela apagada;
- segundo plano;
- interrupção.

## Exercícios

- caminhada;
- corrida;
- esteira;
- treino livre;
- pausa;
- retomada;
- processo encerrado;
- reinício.

## Health Connect

- aceitar;
- negar;
- revogar;
- enviar;
- atualizar;
- retry;
- deduplicação.

## Critérios de aceite

- fluxo no S22;
- sem falhas graves em outro fabricante;
- sessões não perdidas;
- bateria documentada.

---

# Fase 23 — Paridade funcional

## Objetivo

Comparar com a baseline.

## Dispositivo

- scan;
- conexão;
- handshake;
- desconexão;
- passos;
- BPM;
- D1;
- D3/D4.

## Saúde

- histórico;
- métricas;
- gráficos;
- peso;
- perfil.

## Exercícios

- caminhada;
- corrida;
- treino livre;
- esteira;
- calibração;
- revisão;
- histórico.

## Health Connect

- passos;
- BPM completo;
- exercícios;
- peso;
- altura;
- sincronização;
- erros.

## Dados

- banco próprio;
- backup;
- restauração;
- deduplicação.

## Critérios de aceite

- equivalência das funcionalidades essenciais;
- diferenças documentadas;
- sem regressão crítica;
- nenhum fluxo essencial ausente.

## Gate D

Champion Companion equivalente à baseline:

```text
feat/015-watch-workouts-d3-d4
```

---

# Fase 24 — Beta e uso paralelo

## Objetivo

Usar os dois aplicativos durante a validação.

## Estratégia

- manter MotionLog;
- instalar Champion Companion;
- usar em sessões reais;
- comparar;
- validar D1;
- validar D3/D4;
- validar Health Connect;
- registrar divergências.

## Comparações

- passos;
- BPM;
- amostras;
- duração;
- distância;
- calorias;
- horários;
- atividades;
- duplicações;
- estabilidade;
- calibração.

## Critérios de aceite

- sem perda;
- sem problema crítico;
- dados equivalentes ou melhores;
- Champion Companion assume uso diário.

---

# Fase 25 — Lançamento

## Objetivo

Tornar o Champion Companion o app principal do Champion C001.

## Tarefas

- definir package final;
- definir assinatura;
- gerar release;
- criar changelog;
- documentação;
- política de privacidade;
- instruções de adoção;
- plano de rollback;
- congelar MotionLog mobile;
- manter APK estável anterior.

## Critérios de aceite

- release assinado;
- banco próprio validado;
- backup funcionando;
- documentação disponível;
- sem bloqueadores;
- uso diário estável.

---

# 11. Marcos principais

## Marco A — Fundação

```text
Kotlin
+ Compose
+ Room
+ Hilt
+ navegação
```

Fases 1 a 3.

---

## Marco B — Viabilidade BLE

```text
Scan
→ conexão
→ handshake
→ passos
→ BPM
```

Fases 4 a 6.

---

## Marco C — Dados diários

```text
Passos
+ BPM manual
+ D1 completo
+ Health Connect
```

Fases 7 a 9.

---

## Marco D — Treinos do relógio

```text
D3
→ D4
→ parser
→ atividade pendente
```

Fase 10.

---

## Marco E — Fluxo completo

```text
Champion C001
→ Champion Companion
→ revisão/calibração
→ Health Connect
```

Fases 11 e 12.

---

## Marco F — Reorientação para treinos iniciados pelo relógio

```text
Champion C001 inicia e encerra
+ importação D3/D4
+ derivação por passos e comprimento pessoal do passo
+ revisão
+ Health Connect granular
```

Fases 13 (encerramento) e 14 (refinamento redefinido).

---

## Marco G — Paridade

```text
Champion Companion equivalente à baseline
```

Fases 15 a 23.

---

## Marco H — Substituição

```text
Champion Companion em produção
MotionLog congelado
```

Fases 24 e 25.

---

# 12. Ordem recomendada

```text
Fase 0  — Baseline
Fase 1  — Bootstrap
Fase 2  — Design system
Fase 3  — Banco
Fase 4  — Core BLE
Fase 5  — Prova de conceito
Fase 6  — Dispositivo
Fase 7  — Passos e Home
Fase 8  — BPM manual
Fase 9  — D1 e BPM completo
Fase 10 — D3/D4
Fase 11 — Revisão e calibração
Fase 12 — Health Connect (concluída)
Fase 13 — Exercícios phone (`closed`, `partially_delivered`)
Fase 14 — Refinamento, derivação e sincronização dos treinos do relógio (`completed`, `17/17`)
Fase 15 — Calendário (concluída)
Fase 16 — Perfil
Fase 17 — Backup próprio
Fase 18 — Transição sem migração
Fase 19 — Diagnóstico
Fase 20 — Privacidade
Fase 21 — Testes
Fase 22 — Dispositivos reais
Fase 23 — Paridade
Fase 24 — Beta
Fase 25 — Lançamento
```

---

# 13. Priorização

Fase 15 está concluída. Fase 13 está fechada como `partially_delivered` após a
descontinuação de novos treinos phone. Fase 14 está `completed`, com `014-01` até
`014-17` concluídas. O fechamento documental foi concluído após a revalidação
física limitada de `014-15`; Gates físicos sem fixtures permanecem `N/A` e Gate F
vale somente para os cenários exercitados. WorkManager permanece somente detalhe
técnico de tarefas finitas e adiáveis. O refinamento visual da Spec 017 é pacote
independente e não constitui nova fase nem altera essa ordem.

Spec 016 (passos incrementais) foi aplicada como refinamento pós-Fase-12 sobre
a sincronização de passos do Health Connect. Nenhuma fase nova foi criada para
isso — o escopo permanece dentro da Fase 12.

## Crítica

- baseline;
- BLE;
- handshake;
- passos;
- BPM;
- D1;
- D3;
- D4;
- revisão;
- calibração;
- Health Connect;
- deduplicação.

## Alta

- importação, derivação, revisão e sincronização de treinos do relógio;
- foreground service;
- recuperação;
- calendário;
- perfil;
- backup.

## Média

- visual;
- refinamento visual da Spec 017;
- diagnóstico;
- gráficos;
- personalização.

## Baixa

- novas métricas;
- automações;
- outros relógios.

---

# 14. Riscos

## Protocolo incompleto

Mitigação:

- documentação;
- logs;
- fixtures progressivas;
- comparação;
- validação isolada.

---

## Diferenças GATT

Mitigação:

- fila;
- timeout;
- retry limitado;
- múltiplos aparelhos;
- logs.

---

## Duplicação histórica

O Champion Companion poderá encontrar no relógio registros já enviados anteriormente pelo MotionLog.

Mitigação:

- mostrar período;
- registrar início de uso;
- deduplicação interna;
- importação manual;
- revisão D3/D4;
- evitar histórico involuntário.

---

## Duplicação no Health Connect

Mitigação:

- IDs estáveis;
- versões;
- status local;
- validacao de reenvio.

---

## Bateria

Mitigação:

- conexão sob demanda;
- encerrar scan;
- encerrar GATT;
- localização limitada;
- sem BPM contínuo;
- WorkManager somente para tarefas adiáveis da Fase 14.

---

## Escopo

Mitigação:

- baseline;
- checklist;
- backlog;
- gates.

---

## Firmware

Mitigação:

- registrar versão;
- versionar parsers;
- fixtures diversas;
- guardar payload inválido temporariamente;
- não assumir tamanhos fixos.

---

# 15. Fora do escopo

- iOS;
- web;
- outros relógios;
- BLE permanente;
- BPM contínuo;
- sincronização automática ao abrir;
- envio automático de atividades;
- rotas fora da política controlada da Fase 13;
- mapas;
- GPX;
- TCX;
- FIT;
- Strava direto;
- Suunto direto;
- Intervals.icu direto;
- conta obrigatória;
- servidor obrigatório;
- migração do MotionLog;
- compatibilidade com banco antigo;
- novas métricas não validadas;
- muitos módulos Gradle.

---

# 16. Definition of Done

O projeto estará concluído quando:

- desenvolvido em Kotlin;
- conectar ao Champion C001;
- handshake previsível;
- importar passos;
- medir BPM;
- importar D1;
- sincronizar todos os BPM válidos;
- importar D3/D4;
- deduplicar internamente;
- criar atividades pendentes;
- permitir corrigir tipo;
- permitir calibrar esteira;
- recalcular distância, ritmo e velocidade;
- sincronizar atividades;
- preservar infraestrutura e dados de treinos phone existentes sem exigir
  novos treinos phone como critério final;
- importar, derivar, revisar e sincronizar treinos iniciados pelo relógio;
- sobreviver a interrupções conforme contratos da direção futura;
- manter rota histórica somente nos escopos já autorizados, sem expandi-la;
- possuir backup próprio;
- iniciar com banco vazio;
- não depender do MotionLog;
- atingir paridade;
- funcionar como fonte confiável para aplicativos terceiros.

Novos treinos iniciados pelo celular, GPS, rota, `ExerciseRoute` e mapa não são
critérios finais de conclusão do roadmap. A desativação operacional dos pontos
de entrada phone foi registrada em `013-17`; o legado permanece preservado.

O MotionLog poderá ser desinstalado depois da validação final, sem necessidade de transferência dos dados locais.
