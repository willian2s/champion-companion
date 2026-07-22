# Direção da frequência cardíaca manual da Fase 08

## Contexto

A Fase 08 transforma a medição técnica D0/F002 validada na Fase 05 em uma funcionalidade de produto utilizável.

As fases anteriores já entregaram:

- aplicativo Android nativo;
- design system Material 3;
- Room e DataStore;
- modelos de domínio para frequência cardíaca;
- persistência de medições;
- núcleo BLE nativo;
- fila GATT serial;
- conexão com o Champion C001;
- handshake A1;
- subscription F002;
- comando D0 start;
- comando D0 stop;
- parser de BPM D0;
- parser de amostras F002;
- buffer F002 limitado;
- estimador técnico F002;
- política técnica de seleção do valor final;
- gerenciamento do dispositivo;
- associação persistente;
- conexão controlada;
- Home funcional com resumo diário.

A tela Saúde ainda não oferece um fluxo final de medição manual.

## Objetivo

Permitir ao usuário:

- iniciar uma medição manual de frequência cardíaca;
- conectar ao relógio quando necessário;
- acompanhar o BPM durante a medição;
- acompanhar o tempo restante;
- cancelar;
- concluir automaticamente;
- receber um resultado final válido;
- salvar o resultado localmente;
- visualizar a última medição;
- visualizar medições manuais recentes;
- tentar novamente após uma falha;
- visualizar o último BPM na Home.

## Princípio central

A medição somente pode ser iniciada por ação explícita do usuário.

Não iniciar medição porque:

- o aplicativo abriu;
- a tela Saúde abriu;
- o relógio conectou;
- a auto-conexão foi concluída;
- a Home foi atualizada;
- F002 começou a emitir;
- algum pacote D0 foi observado;
- o aplicativo retornou ao foreground.

Fluxo principal:

```text
Usuário toca Medir frequência cardíaca
        ↓
Verificar dispositivo associado
        ↓
Verificar permissões e Bluetooth
        ↓
Usar sessão conectada ou conectar de forma controlada
        ↓
Garantir transporte e handshake
        ↓
Garantir subscription F002
        ↓
Iniciar observação D0 e F002
        ↓
Enviar D0 01
        ↓
Medir durante 60 segundos
        ↓
Selecionar resultado final
        ↓
Enviar D0 00
        ↓
Limpar recursos
        ↓
Persistir somente o resultado final válido
        ↓
Exibir conclusão
```

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento;
3. ADRs vigentes;
4. spec da Fase 08;
5. implementação e documentos concluídos da Fase 07;
6. implementação e documentos concluídos da Fase 06;
7. implementação e documentos concluídos da Fase 05;
8. núcleo BLE da Fase 04;
9. domínio e persistência da Fase 03;
10. documentos de protocolo;
11. roadmap;
12. MotionLog somente como referência funcional.

## Prioridade de desenvolvimento

A prioridade é entregar a medição funcional.

Não criar task exclusiva de testes automatizados.

Não exigir:

- meta de cobertura;
- campanha ampla de testes Compose;
- testes instrumentados para cada estado;
- expansão geral dos fakes BLE;
- correção completa da suíte antiga;
- regressão completa das fases anteriores;
- validação automatizada de todos os sinais F002 possíveis.

Continuam obrigatórios:

- `assembleDebug`;
- `assembleRelease`;
- `installDebug`;
- validação manual no Galaxy S22;
- correção das regressões introduzidas pela Fase 08.

Testes pequenos devem acompanhar as tasks responsáveis quando protegerem regras críticas, como:

- comandos D0;
- parser D0;
- parser F002;
- resultado final;
- cancelamento sem persistência;
- deduplicação por sessão;
- bloqueio de simultaneidade.

## Escopo

A Fase 08 inclui:

- promoção dos componentes D0/F002 da PoC;
- coordinator de medição manual;
- estado explícito da sessão;
- duração de 60 segundos;
- progresso temporal;
- BPM em tempo real;
- estimativa F002;
- política de resultado final;
- cancelamento;
- finalização automática;
- D0 stop;
- cleanup;
- persistência do resultado final;
- última medição;
- lista simples de medições manuais recentes;
- tela Saúde funcional;
- último BPM na Home;
- erros seguros;
- retry;
- validação física.

## Fora do escopo

A Fase 08 não deve:

- implementar D1;
- importar histórico automático;
- sincronizar com Health Connect;
- criar `HealthSyncOperation` para a medição manual;
- persistir o sinal bruto F002;
- persistir todo valor D0 recebido;
- persistir stream contínuo de BPM;
- implementar gráficos diário, semanal ou mensal;
- implementar estatísticas históricas extensas;
- implementar BPM durante exercícios;
- implementar D3;
- implementar D4;
- implementar foreground service;
- manter medição em background;
- suportar outros relógios;
- iniciar medição automaticamente;
- criar campanha ampla de testes.

## Arquitetura esperada

```text
HealthScreen
    ↓
HealthViewModel
    ├── HeartRateRepository
    ├── DeviceRepository
    └── ManualHeartRateCoordinator
              ├── ForegroundBleOperationGate
              ├── DeviceConnectionBootstrap
              ├── ChampionConnectionManager
              ├── ChampionNotificationHub
              ├── ChampionD0Protocol
              ├── ChampionF002Parser
              ├── ChampionF002BpmEstimator
              └── MonotonicClock
```

Para persistência:

```text
ManualHeartRateCoordinator
        ↓
ManualHeartRateResult
        ↓
HealthViewModel
        ↓
HeartRateRepository
        ↓
Room
```

Regras:

- `HealthScreen` somente renderiza estado e envia intents;
- `HealthViewModel` controla a operação de produto;
- o Composable não acessa BLE;
- o Composable não acessa Room;
- o Composable não mantém buffer F002;
- o coordinator não depende de Compose;
- somente a camada BLE concreta acessa `BluetoothGatt`;
- comandos continuam passando pela fila GATT;
- dependencies devem vir por Hilt.

## Reutilização da PoC

Promover os componentes técnicos atualmente ligados à PoC para componentes reutilizáveis.

Exemplos conceituais:

```text
DevicePocHeartRateSession
    → ChampionManualHeartRateProtocol
```

```text
DevicePocF002BpmEstimator
    → ChampionF002BpmEstimator
```

A nomenclatura definitiva pode ser ajustada.

Devem ser promovidos:

- criação do comando `D0 01`;
- criação do comando `D0 00`;
- parsing de BPM D0;
- parsing de amostras F002;
- buffer limitado;
- inspeção do sinal;
- estimativa de BPM;
- classificação de qualidade;
- seleção do resultado final.

A PoC debug deve reutilizar os componentes promovidos enquanto permanecer no projeto.

Não manter implementações distintas para produto e PoC.

## Contrato D0

### Start

Enviar:

```text
D0 01
```

Canal:

```text
F008 write
```

Regras:

- write with response;
- sem retry automático cego;
- somente depois de transporte funcional e handshake;
- somente depois de F002 estar subscribed;
- limpar todos os dados da sessão anterior antes do start;
- registrar o instante monotônico e o instante civil do início.

### Stop

Enviar:

```text
D0 00
```

O stop deve ser tentado em:

- conclusão normal;
- cancelamento;
- timeout;
- erro recuperável;
- saída controlada da tela;
- app indo para background;
- disconnect solicitado.

Quando o transporte ainda estiver disponível, o stop deve ser tentado antes do cleanup.

A falha no stop não pode:

- manter a tela presa;
- impedir cleanup;
- causar retry infinito;
- persistir resultado incompleto;
- manter a sessão como ativa.

## Subscription F002

F002 deve estar subscribed antes de `D0 01`.

Usar:

- `ChampionNotificationHub`;
- sessão atual;
- uma única subscription física;
- collector lógico exclusivo da medição;
- identidade da sessão BLE;
- identidade da medição manual.

Não configurar CCCD diretamente na feature.

A ausência de F002 deve impedir o início da medição de produto nesta fase.

## BPM D0 em tempo real

Aceitar somente pacotes:

- do canal F008 notify;
- com código `0xD0`;
- pertencentes à sessão BLE atual;
- observados depois do início da medição;
- com valor dentro do intervalo validado pelo protocolo manual.

Intervalo inicial:

```text
35..220 BPM
```

O domínio genérico pode aceitar intervalo mais amplo, mas o fluxo manual deve usar o intervalo confirmado pelo protocolo.

Cada BPM D0 válido pode atualizar a tela.

Esses valores permanecem em memória e não são persistidos individualmente.

## F002

Decodificar em pares little-endian:

```text
sample = lowByte | highByte << 8
```

Regras:

- ignorar byte final ímpar;
- copiar bytes defensivamente;
- manter somente amostras positivas na etapa apropriada do estimador;
- limitar o buffer;
- descartar buffer ao finalizar;
- não registrar amostras no Logcat;
- não salvar amostras no Room;
- não exportar o sinal.

## Duração

A duração de produto será:

```text
60 segundos
```

A duração deve ser centralizada em configuração ou constante de domínio da feature.

Usar relógio monotônico para:

- tempo decorrido;
- tempo restante;
- progresso;
- timeout da sessão.

Usar relógio civil para:

- timestamps persistidos;
- exibição;
- `measuredAt`.

Não calcular a duração usando somente `Instant.now()`.

## Progresso

Como a medição possui duração conhecida, utilizar progresso determinado.

Calcular:

```text
progress = elapsedDuration / targetDuration
```

Regras:

- limitar progresso visual entre 0 e 1;
- mostrar tempo restante;
- não depender de ticks exatos de um segundo;
- recalcular pelo relógio monotônico;
- não persistir progresso;
- não continuar contando depois de concluir ou cancelar.

## Política do valor exibido

Durante a medição:

1. mostrar BPM D0 válido mais recente;
2. quando não houver D0 válido, permitir estimativa F002 suficientemente confiável;
3. indicar de forma discreta quando o valor exibido for estimado;
4. mostrar estado aguardando leitura antes do primeiro valor válido.

Não exibir:

- amostras F002;
- números técnicos do estimador;
- quantidade de picos;
- sample rate;
- bytes;
- payload.

Esses dados podem permanecer no diagnóstico debug sanitizado.

## Política do resultado final

A política inicial deve preservar o comportamento validado na Fase 05:

```text
1. último D0 válido da sessão atual;
2. caso ausente, estimativa F002 válida;
3. caso ambos ausentes, resultado indisponível.
```

Nunca utilizar:

- D0 de sessão anterior;
- valor recebido antes do start;
- último valor global;
- estimativa inválida;
- BPM fora do intervalo;
- valor inventado.

O planejamento deve auditar o MotionLog para confirmar o significado de “calcular média quando necessário”.

Caso o MotionLog use média ou mediana como parte real do resultado final:

- documentar a evidência;
- definir exatamente o conjunto de valores;
- definir o período;
- definir filtros;
- definir arredondamento;
- adicionar teste pequeno;
- atualizar o documento de protocolo.

Na ausência dessa evidência, preservar a política da Fase 05 e não introduzir média nova.

## Qualidade do sinal

A qualidade calculada pelo estimador pode ser usada durante a sessão para feedback:

```text
Boa
Regular
Baixa
```

Ela deve permanecer efêmera nesta fase.

Não alterar o schema apenas para persistir qualidade.

A UI pode orientar:

- manter o braço parado;
- ajustar o relógio;
- aguardar o fim da medição.

Qualidade baixa não deve automaticamente produzir um resultado inválido quando houver D0 válido aceito pela política final.

## Estado da sessão

Planejar estado explícito semelhante a:

```text
Idle
Preparing
Connecting
Starting
Measuring
Stopping
Completed
Cancelled
Failed
```

Dados associados podem incluir:

- session ID da medição;
- session ID BLE;
- instante inicial;
- duração;
- progresso;
- BPM atual;
- origem atual do BPM;
- qualidade do sinal;
- resultado final;
- erro seguro.

Evitar booleanos independentes como:

```text
isMeasuring
isConnected
isStopping
hasResult
hasError
```

## State machine

```text
Idle
  ↓ Start
Preparing
  ↓
Connecting
  ↓
Starting
  ↓ D0 01 enviado
Measuring
  ├── Cancel → Stopping → Cancelled
  ├── Erro → Stopping/Cleanup → Failed
  └── 60 segundos → Stopping → Completed
```

Somente `Completed` com resultado final válido pode persistir uma medição.

## Simultaneidade

Somente uma medição manual pode existir.

A Fase 08 também deve impedir conflito com outros fluxos BLE iniciados pelo usuário, especialmente:

- refresh de passos da Home;
- nova conexão gerenciada pela tela Dispositivo;
- outra medição manual;
- futuros imports.

Planejar ou reutilizar um gate compartilhado de operação foreground, por exemplo:

```text
ForegroundBleOperationGate
```

Operações iniciais:

```text
DAILY_STEPS_REFRESH
MANUAL_HEART_RATE
```

Regras:

- aquisição atômica;
- apenas um owner;
- liberação em `finally`;
- cancelamento libera ownership;
- erro libera ownership;
- operação concorrente recebe erro seguro;
- não substituir silenciosamente a operação ativa.

Se essa decisão tiver impacto arquitetural duradouro e não estiver coberta pelas ADRs, criar ou atualizar ADR durante a task responsável.

## Conexão

Ao iniciar medição:

1. verificar dispositivo associado;
2. verificar permissões;
3. verificar Bluetooth;
4. tentar reutilizar sessão funcional do dispositivo associado;
5. quando necessário, conectar de forma controlada;
6. garantir `TransportReady`;
7. garantir handshake;
8. garantir F002;
9. iniciar medição.

Não:

- conectar outro Champion;
- abrir scan amplo automaticamente;
- entrar em retry infinito;
- solicitar permissão ao abrir a tela;
- iniciar medição apenas porque conectou.

Quando não houver dispositivo associado, direcionar o usuário para a tela Dispositivo.

## Persistência

Persistir somente o resultado final.

Criar uma `HeartRateMeasurement` com:

```text
source = MANUAL_MEASUREMENT
bpm = resultado final
measuredAt = timestamp correspondente ao resultado
validity = VALID
deviceId = dispositivo associado
importedAt = null
appWorkoutSessionId = null
workoutActivityId = null
deduplicationKey = MeasurementSession(sessionId)
```

A versão do pipeline pode ser registrada por `parserVersion`, quando compatível com o modelo atual.

Timestamp recomendado:

- resultado D0: `observedAt` do D0 selecionado;
- resultado F002: instante de encerramento ou cálculo final da sessão.

A decisão deve ser documentada na spec.

## Deduplicação

Uma sessão manual deve gerar no máximo um registro persistido.

A chave deve usar:

```text
HeartRateMeasurementSessionId
```

Regras:

- retry de persistência da mesma sessão não duplica;
- nova medição gera novo session ID;
- cancelamento não persiste;
- falha sem resultado não persiste;
- callback tardio não cria registro;
- restauração do app não reutiliza session ID encerrado.

## Repository

Evoluir `HeartRateRepository` para suportar operações equivalentes a:

```text
saveManualMeasurement(...)
observeLatestValidMeasurement()
observeRecentManualMeasurements(limit)
```

Ou uma API equivalente que preserve intenção de domínio.

Regras:

- feature não acessa DAO;
- persistência idempotente por sessão;
- consultas ordenadas por `measuredAt`;
- somente medições válidas entram na última medição;
- registros D1 futuros devem poder coexistir;
- não quebrar o contrato de importação da Fase 09.

A consulta genérica de último BPM pode considerar todas as fontes válidas, permitindo que a Fase 09 seja integrada sem reescrever a Home.

A lista da tela Saúde nesta fase pode filtrar apenas `MANUAL_MEASUREMENT`.

## Room

O schema atual aparentemente já possui os campos necessários para a medição manual.

Preferir não alterar schema Room.

Adicionar apenas:

- queries;
- métodos DAO;
- contracts;
- mappers;
- operações de repository.

Alterar schema somente se a auditoria demonstrar necessidade funcional inevitável.

Não adicionar campos de:

- sinal bruto;
- qualidade detalhada;
- BPMs intermediários;
- origem D0/F002 final;
- progresso.

## Health Connect

A Fase 08 salva localmente.

Fluxo:

```text
D0/F002
    ↓
Resultado final
    ↓
Room
    ↓
Saúde e Home
```

Não:

```text
D0/F002
    ↓
Room
    ↓
Health Connect
```

Não criar operação pendente do Health Connect para a medição manual nesta fase.

A Fase 12 deverá ampliar a elegibilidade e criar a operação apropriada.

## Tela Saúde

A tela Saúde deve deixar de ser placeholder.

### Estado ocioso

Exibir:

- título;
- última medição;
- horário da última medição;
- ação principal `Medir frequência cardíaca`;
- lista simples de medições manuais recentes;
- estado sem medições;
- orientação quando não houver dispositivo.

### Durante a medição

Exibir:

- BPM atual em destaque;
- estado aguardando leitura quando necessário;
- progresso determinado;
- tempo restante;
- qualidade ou orientação do sinal;
- instrução para manter o braço parado;
- ação `Cancelar`;
- estado da conexão de forma discreta.

### Concluída

Exibir:

- BPM final;
- horário;
- confirmação de salvamento;
- ação `Concluir`;
- ação para iniciar nova medição.

### Falha

Exibir:

- erro seguro;
- último histórico preservado;
- ação `Tentar novamente`;
- acesso à tela Dispositivo quando aplicável.

Não mostrar:

- F002 bruto;
- payload;
- hex;
- bytes;
- senha;
- MAC;
- detalhes GATT;
- stack trace.

## Home

A Home deve passar a mostrar o último BPM válido armazenado.

Exibir:

- valor;
- horário;
- estado `Sem medições` quando não existir;
- indicação local apropriada.

A Home não deve:

- iniciar medição;
- conectar ao relógio para obter BPM;
- reutilizar valor técnico da PoC;
- usar BPM em memória não persistido;
- bloquear o resumo diário quando não houver BPM.

A atualização deve vir da observação do repository.

## Histórico recente

A Fase 08 pode mostrar uma lista simples de medições manuais recentes.

Exemplo:

```text
Hoje, 10:42 — 78 BPM
Ontem, 18:17 — 82 BPM
```

Não implementar nesta fase:

- gráfico;
- média diária;
- mínimo/máximo diário;
- filtros;
- calendário;
- agrupamento semanal;
- agrupamento mensal;
- histórico D1.

Esses recursos pertencem principalmente à Fase 09 e às fases de calendário.

## Cancelamento

Quando o usuário cancelar:

1. alterar estado para `Stopping`;
2. tentar `D0 00`;
3. cancelar timer;
4. cancelar collectors;
5. limpar buffer;
6. liberar gate;
7. não persistir;
8. retornar a estado estável.

Cancelamento nunca deve aparecer como medição concluída.

## Lifecycle

Durante medição:

- manter a tela acordada;
- sobreviver a recomposição;
- evitar collectors duplicados;
- evitar novo start em recriação de Composable;
- manter ownership no ViewModel/coordinator.

Ao pressionar voltar durante medição:

- solicitar confirmação de cancelamento;
- não navegar silenciosamente deixando a medição ativa.

Ao ir para background:

- cancelar a medição;
- tentar D0 stop;
- limpar recursos;
- não persistir resultado parcial.

Sem foreground service, a medição não deve continuar em background.

Em morte do processo:

- não restaurar uma sessão ativa como se continuasse;
- iniciar novamente em estado ocioso;
- preservar somente medições já persistidas.

## Callback tardio

Cada medição deve possuir identidade própria.

Callbacks tardios não podem:

- atualizar nova medição;
- concluir sessão encerrada;
- persistir resultado;
- alterar BPM atual;
- reativar estado;
- adquirir novamente o gate.

Validar:

```text
bleSessionId
manualMeasurementSessionId
sessionStartedAt
```

antes de aceitar eventos.

## Erros

Considerar erros seguros para:

- dispositivo não associado;
- permissão ausente;
- Bluetooth desligado;
- dispositivo não encontrado;
- conexão falhou;
- handshake falhou;
- F002 ausente;
- D0 start falhou;
- disconnect inesperado;
- nenhum BPM válido;
- D0 stop falhou;
- persistência falhou;
- medição já em andamento;
- outra operação BLE em andamento;
- cancelamento.

A falha de D0 stop deve ser registrada, mas não pode impedir cleanup.

## Logging e diagnóstico

Pode registrar:

- início e fim;
- duração;
- transições;
- D0 start enviado;
- D0 stop tentado;
- quantidade de pacotes D0;
- quantidade de pacotes F002;
- disponibilidade de resultado;
- fonte final como categoria;
- qualidade categórica;
- categoria de erro.

Não registrar:

- BPM exato;
- amostras F002;
- payload;
- bytes;
- hexadecimal;
- MAC;
- identificador persistente;
- dados pessoais;
- stack trace destinado à UI.

## Acessibilidade

A tela deve anunciar:

```text
Medição iniciada
Frequência cardíaca detectada
Medição concluída
Medição cancelada
Não foi possível concluir a medição
```

O BPM não deve depender apenas de tamanho ou cor.

O progresso deve possuir descrição de tempo restante.

O botão de cancelamento deve ter área de toque adequada.

## Estratégia reduzida de testes

Não criar task exclusiva de testes.

Testes pequenos recomendados:

- `D0 01`;
- `D0 00`;
- D0 ignora pacote antigo;
- D0 ignora valor inválido;
- F002 little-endian;
- byte ímpar;
- buffer limitado;
- seleção D0;
- fallback F002;
- resultado indisponível;
- cancelamento não persiste;
- sessão concluída persiste uma vez;
- callback tardio é ignorado;
- gate impede simultaneidade;
- repository deduplica por session ID;
- última medição considera somente registro válido.

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

Regressões introduzidas pela Fase 08 devem ser corrigidas.

## Smoke test manual

No Galaxy S22 com Champion C001:

1. abrir Saúde;
2. confirmar estado sem medição, quando aplicável;
3. tocar `Medir frequência cardíaca`;
4. conectar ao relógio quando necessário;
5. confirmar início da medição;
6. confirmar progresso de 60 segundos;
7. confirmar BPM em tempo real;
8. confirmar atualização do tempo restante;
9. aguardar conclusão;
10. confirmar resultado final;
11. confirmar persistência;
12. fechar e reabrir o app;
13. confirmar última medição;
14. confirmar BPM na Home;
15. iniciar segunda medição;
16. confirmar novo registro;
17. cancelar uma medição;
18. confirmar ausência de registro da sessão cancelada;
19. tentar iniciar duas medições;
20. confirmar bloqueio;
21. iniciar refresh de passos e tentar medir simultaneamente;
22. confirmar bloqueio seguro de uma das operações;
23. desconectar durante a medição;
24. confirmar cleanup e ausência de persistência;
25. enviar app para background durante a medição;
26. confirmar cancelamento;
27. desligar Bluetooth;
28. confirmar erro seguro;
29. ligar Bluetooth e tentar novamente;
30. confirmar que nenhum registro foi enviado ao Health Connect;
31. confirmar ausência de BPM e F002 no Logcat.

## Dívida técnica

Registrar para fases futuras:

- sincronização com Health Connect na Fase 12;
- histórico D1 na Fase 09;
- gráficos e agregações na Fase 09;
- BPM durante exercícios nas fases de exercícios;
- foreground service na Fase 14;
- testes Compose amplos na Fase 21;
- persistência de qualidade somente se surgir requisito;
- revisão da política de média caso evidência funcional seja encontrada.

## Critérios de aceite

A Fase 08 será concluída quando:

- Saúde deixar de ser placeholder;
- medição iniciar somente por ação explícita;
- conexão controlada funcionar;
- F002 estiver subscribed antes de D0 start;
- `D0 01` passar pela fila GATT;
- BPM em tempo real for exibido;
- progresso de 60 segundos for exibido;
- somente uma medição puder existir;
- refresh de passos e medição não concorrerem;
- cancelamento enviar stop quando possível;
- cancelamento não persistir;
- conclusão enviar stop quando possível;
- cleanup ocorrer independentemente do stop;
- resultado final usar somente a sessão atual;
- resultado final usar D0 ou fallback F002;
- ausência de resultado não criar medição;
- somente resultado final for persistido;
- sinal F002 não for persistido;
- stream D0 não for persistido;
- deduplicação por sessão funcionar;
- última medição aparecer em Saúde;
- último BPM aparecer na Home;
- não houver Health Connect;
- não houver D1;
- debug e release compilarem;
- fluxo crítico for validado no Galaxy S22;
- dívida de testes ficar registrada.
