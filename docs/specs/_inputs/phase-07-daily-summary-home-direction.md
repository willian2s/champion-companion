# Direção da Home, passos e resumo diário da Fase 07

## Contexto

A Fase 07 cria a primeira experiência diária útil do Champion Companion.

As fases anteriores já entregaram:

- aplicativo Android nativo em Kotlin e Jetpack Compose;
- design system Material 3;
- Room e DataStore;
- modelos de domínio;
- repositories;
- núcleo BLE nativo;
- conexão com o Champion C001;
- handshake A1;
- subscription FEA1;
- parser técnico FEA1;
- gerenciamento de dispositivo;
- associação persistente;
- conexão manual;
- auto-conexão controlada do dispositivo associado;
- estado real da conexão;
- diagnóstico seguro;
- Home com cabeçalho, estado do relógio e acesso ao perfil.

A Home atual ainda não apresenta métricas diárias persistidas.

A Fase 07 deve transformar uma leitura FEA1 solicitada pelo usuário em um resumo diário local e apresentá-lo na Home.

## Objetivo

Permitir ao usuário:

- visualizar os passos do dia;
- visualizar o progresso em relação à meta diária;
- visualizar distância estimada quando calculável;
- visualizar calorias estimadas quando calculáveis;
- visualizar a origem dos dados;
- visualizar o horário da última sincronização;
- visualizar o estado do relógio;
- atualizar os dados por pull-to-refresh;
- acompanhar visualmente a sincronização;
- repetir a operação após falha;
- continuar usando a Home mesmo quando a sincronização falhar.

## Princípio central

Conexão não significa sincronização.

A ADR vigente permite auto-conexão controlada do dispositivo associado. Isso não autoriza persistência automática de pacotes FEA1.

O dispositivo pode estar conectado, mas a leitura e persistência de passos devem ocorrer somente após ação explícita do usuário.

Nesta fase, a ação explícita principal será:

```text
pull-to-refresh na Home
```

Não persistir passos automaticamente apenas porque:

- o aplicativo abriu;
- a Home abriu;
- o relógio conectou;
- o handshake terminou;
- o usuário retornou ao aplicativo;
- FEA1 emitiu espontaneamente;
- uma sessão BLE foi restaurada;
- a auto-conexão foi concluída.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento;
3. ADRs vigentes;
4. spec da Fase 07;
5. implementação e documentos concluídos da Fase 06;
6. implementação e documentos concluídos da Fase 05;
7. implementação e documentos concluídos da Fase 04;
8. modelos de domínio e persistência da Fase 03;
9. documentos de protocolo;
10. roadmap;
11. MotionLog apenas como referência funcional.

## Prioridade de desenvolvimento

A prioridade da Fase 07 é entregar a funcionalidade.

Não criar task exclusiva de testes.

Não exigir:

- meta de cobertura;
- campanha ampla de testes Compose;
- expansão geral dos fakes BLE;
- testes instrumentados para cada estado visual;
- regressão completa das fases anteriores;
- correção ampla da suíte antiga;
- validação automatizada de todos os cenários de timezone.

Continuam obrigatórios:

- `assembleDebug`;
- `assembleRelease`;
- `installDebug`;
- validação manual no Galaxy S22;
- correção de regressões introduzidas diretamente pela Fase 07.

Testes pequenos podem ser adicionados junto das tasks de implementação quando protegerem regras críticas, como:

- não reduzir passos do mesmo dia;
- não somar acumulados FEA1 como deltas;
- separar resumos por data;
- aceitar somente pacote FEA1 fresco;
- calcular corretamente o progresso da meta;
- manter compatibilidade das preferências.

## Escopo

A Fase 07 inclui:

- promoção do parser FEA1 para componente reutilizável;
- pull-to-refresh como interação principal;
- sincronização manual de passos;
- indicador do gesto de atualização;
- barra linear indeterminada durante a sincronização;
- espera por pacote FEA1 fresco;
- persistência do resumo diário;
- consolidação atômica das leituras;
- proteção contra redução acidental;
- tratamento da mudança de dia;
- distância estimada quando houver dados suficientes;
- calorias estimadas quando houver dados suficientes;
- meta diária de passos;
- Home funcional;
- origem do resumo;
- última atualização;
- loading;
- retry;
- estados vazios;
- erros seguros;
- atividades pendentes já persistidas, quando disponíveis.

## Fora do escopo

A Fase 07 não deve:

- implementar sincronização automática ao abrir;
- persistir todo pacote FEA1 espontâneo;
- implementar BLE permanente;
- implementar background BLE;
- implementar foreground service;
- implementar WorkManager para sincronização;
- implementar Health Connect;
- implementar D1;
- implementar D3;
- implementar D4;
- implementar medição de BPM de produção;
- importar atividades do relógio;
- implementar GPS;
- implementar perfil completo;
- implementar edição completa da meta;
- implementar histórico mensal;
- implementar calendário;
- suportar outros relógios;
- apagar dados anteriores;
- criar campanha ampla de testes;
- criar botão permanente `Sincronizar agora` como ação principal da Home.

## Arquitetura esperada

```text
HomeScreen
    ├── Pull-to-refresh
    ├── Indicador do gesto
    ├── Barra linear indeterminada
    └── Renderização do resumo
             ↓
HomeViewModel
    ├── HomeUiState
    ├── refresh()
    ├── DailySummaryRepository
    ├── DeviceRepository
    ├── AppPreferencesRepository
    ├── dados pessoais mínimos, quando disponíveis
    └── DailyStepsSyncCoordinator
              ├── DeviceConnectionBootstrap
              ├── ChampionConnectionManager
              ├── ChampionNotificationHub
              ├── parser FEA1
              ├── DailySummaryRepository
              └── DeviceRepository
```

Regras:

- `HomeScreen` somente renderiza estado e emite intents;
- `HomeViewModel` não acessa `BluetoothGatt`;
- o Composable não controla subscriptions;
- o Composable não aguarda pacotes BLE;
- o Composable não acessa Room ou DAO;
- `DailyStepsSyncCoordinator` não depende de Compose;
- o parser FEA1 deve ser reutilizável fora da PoC;
- o repository deve possuir uma operação própria de consolidação;
- regras de merge não devem ficar apenas no ViewModel;
- dependências devem ser fornecidas por Hilt.

## Pull-to-refresh

A Home deve usar pull-to-refresh como interação principal para sincronizar e atualizar o resumo diário.

O usuário inicia a operação puxando o conteúdo da Home para baixo a partir do topo.

Não utilizar um botão permanente `Sincronizar agora` no estado normal da Home.

Fluxo:

```text
Usuário puxa a Home para baixo
        ↓
HomeViewModel recebe Refresh
        ↓
Verificar dispositivo associado
        ↓
Verificar permissões e Bluetooth
        ↓
Usar sessão existente ou conectar de forma controlada
        ↓
Garantir transporte funcional e handshake
        ↓
Garantir FEA1 subscribed
        ↓
Registrar início da sincronização
        ↓
Aguardar pacote FEA1 fresco
        ↓
Consolidar e persistir resumo
        ↓
Atualizar metadados de importação
        ↓
Atualizar Home
        ↓
Encerrar estado de refresh
```

O gesto de pull-to-refresh é uma ação explícita do usuário.

Uma ação de acessibilidade equivalente ou um botão de retry em estado de erro pode chamar o mesmo pipeline.

## Conteúdo rolável

A Home deve permitir o gesto mesmo quando houver pouco ou nenhum conteúdo.

O layout deve permanecer rolável nos estados:

- sem resumo;
- sem dispositivo;
- erro;
- Bluetooth desligado;
- permissão ausente;
- resumo com poucos cards.

O estado vazio deve apresentar uma instrução curta, por exemplo:

```text
Puxe para baixo para sincronizar
```

## Indicadores da sincronização

Durante toda a operação, a Home deve mostrar:

1. indicador associado ao pull-to-refresh;
2. barra linear indeterminada abaixo do cabeçalho.

A barra deve permanecer visível durante:

- preparação;
- verificação de pré-requisitos;
- conexão, quando necessária;
- discovery;
- handshake;
- espera pelo pacote FEA1;
- consolidação;
- persistência;
- atualização dos metadados.

Não representar porcentagem fictícia.

A operação não possui progresso total mensurável, portanto a barra deve ser indeterminada.

## Etapa atual

A Home pode apresentar uma descrição curta da etapa atual:

```text
Preparando sincronização…
Conectando ao relógio…
Aguardando dados do relógio…
Atualizando resumo…
```

Esses textos servem apenas como feedback visual.

A lógica de negócio não deve depender das strings exibidas.

## Estado do refresh

O estado da Home deve possuir informações equivalentes a:

```text
isRefreshing
refreshStage
refreshError
lastSuccessfulRefreshAt
```

Uma possível enumeração:

```text
Preparing
Connecting
WaitingForSteps
Persisting
```

`isRefreshing` deve derivar da operação ativa.

Evitar manter um booleano visual independente que possa divergir da operação real.

## Concorrência

Somente uma sincronização pode existir por vez.

Enquanto `isRefreshing` for verdadeiro:

- ignorar novos gestos;
- impedir criação de outro awaiter FEA1;
- impedir nova conexão concorrente;
- impedir nova operação de persistência;
- manter a operação atual;
- não reiniciar timeout.

O bloqueio deve ficar no ViewModel ou coordinator, não apenas no Composable.

## Encerramento garantido

O estado de refresh deve terminar em todos os caminhos:

- sucesso;
- erro de conexão;
- erro de handshake;
- timeout FEA1;
- payload inválido;
- persistência falha;
- Bluetooth desligado;
- permissão ausente;
- disconnect;
- cancelamento;
- recriação da tela.

A implementação deve usar uma estrutura equivalente a `try/finally` para garantir que:

```text
isRefreshing = false
```

não permaneça preso.

A barra não pode continuar visível indefinidamente após uma falha.

## Manutenção dos dados anteriores

Durante o refresh:

- manter o último resumo válido visível;
- não substituir passos por zero;
- não mostrar loading de tela cheia;
- não remover distância ou calorias anteriores;
- não limpar origem;
- não esconder última atualização;
- manter navegação disponível.

A nova leitura só substitui o estado visível após persistência bem-sucedida.

## Falha e retry

Depois de uma falha:

- esconder a barra;
- manter o último resumo válido;
- mostrar erro seguro;
- permitir novo pull-to-refresh;
- exibir `Tentar novamente` quando melhorar a descoberta da ação;
- fazer o botão chamar exatamente o mesmo fluxo do gesto.

O botão de retry é secundário e deve aparecer somente quando houver erro relevante.

## Reutilização da Fase 05

O parser atual de FEA1 deve ser promovido de algo equivalente a:

```text
DevicePocFea1Steps
```

para um componente reutilizável, por exemplo:

```text
ChampionFea1StepsParser
```

A promoção deve preservar:

- pacote com exatamente quatro bytes;
- header `0x01`;
- total de passos em 24 bits little-endian;
- erro tipado para payload inválido;
- cópia defensiva dos bytes.

Não manter dois parsers FEA1 independentes.

A PoC deve passar a reutilizar o parser promovido quando permanecer no projeto.

## Fluxo de sincronização manual

A sincronização deve:

1. verificar se já existe operação ativa;
2. verificar se existe dispositivo associado;
3. verificar permissões;
4. verificar Bluetooth;
5. usar sessão funcional existente, quando disponível;
6. conectar de forma controlada quando necessário;
7. atingir transporte pronto;
8. concluir ou validar handshake A1;
9. garantir subscription FEA1;
10. registrar o instante de início da espera;
11. aguardar pacote válido e fresco;
12. resolver a data local;
13. calcular estimativas quando possível;
14. consolidar o resumo;
15. atualizar metadados de importação;
16. encerrar o estado de refresh;
17. manter a sessão conectada quando saudável, conforme ownership definido pela Fase 06.

## Pacote fresco

A sincronização não deve reutilizar silenciosamente valor recebido antes da ação do usuário.

Um pacote é elegível quando:

- pertence à sessão atual;
- vem de `FEA1_NOTIFY`;
- passa pelo parser;
- é recebido depois do início da sincronização;
- não pertence a uma tentativa anterior;
- não pertence a sessão BLE encerrada;
- não foi consumido por outro refresh.

Um cache pode existir para diagnóstico ou exibição técnica, mas não deve concluir uma nova sincronização quando o timestamp for anterior ao início da tentativa atual.

## Timeout

A espera por FEA1 deve possuir timeout explícito e centralizado.

A política deve ser:

- uma tentativa por gesto;
- sem retry automático infinito;
- sem loop de reconexão;
- timeout encerra a tentativa;
- usuário pode tentar novamente;
- cleanup remove awaiters e collectors temporários;
- timeout não exige disconnect de uma sessão que permaneça saudável;
- disconnect durante a espera encerra a tentativa.

O valor inicial deve ser escolhido considerando a evidência física das fases anteriores.

## Cancelamento

Ao cancelar ou abandonar a operação:

- encerrar espera pelo pacote;
- remover observers temporários;
- não persistir valor incompleto;
- não atualizar `lastImportedAt`;
- não deixar `isRefreshing` ativo;
- não criar reconexão automática adicional;
- não deixar operação GATT presa.

Sair da composição não deve causar duplicação de uma operação já gerenciada pelo ViewModel.

## Semântica dos passos

FEA1 representa total acumulado de passos do relógio.

Não interpretar como delta.

Exemplo:

```text
Primeira leitura: 4.000
Segunda leitura: 4.150

Resultado do dia: 4.150
```

Nunca fazer:

```text
4.000 + 4.150 = 8.150
```

## Chave natural do resumo

Usar a chave prevista pelo domínio:

```text
deviceId
origin = WATCH_DAILY_SUMMARY
localDate
```

O `deviceId` deve corresponder ao dispositivo que originou a leitura.

A data deve ser resolvida usando:

- instante de recepção do pacote;
- timezone atual do sistema;
- `LocalDate` correspondente.

Não usar a data UTC diretamente como data diária da interface.

## Consolidação atômica

Adicionar uma operação de repository específica para consolidar leitura do relógio.

Exemplo conceitual:

```text
mergeWatchDailySummary(...)
```

A operação deve ser atômica.

Para a mesma chave natural:

```text
nextSteps = max(existingSteps, incomingSteps)
```

Regras:

- primeira leitura cria o resumo;
- leitura maior atualiza o total;
- leitura igual mantém o total;
- leitura menor não reduz o total;
- leitura menor não é somada;
- leitura menor não cria novo resumo;
- nova data cria resumo independente;
- outro dispositivo cria chave independente;
- outra origem não é misturada automaticamente.

O merge deve ficar na camada de persistência ou domínio.

Não depender apenas de comparação feita pelo ViewModel.

## Leitura menor

Quando o relógio reportar menos passos que o valor persistido no mesmo dia:

- preservar os passos existentes;
- considerar a comunicação válida se o pacote for válido;
- atualizar a última sincronização/importação;
- não mostrar erro fatal;
- não substituir o total por zero;
- não somar;
- não apagar o resumo anterior;
- registrar somente categoria diagnóstica segura.

## Leitura igual

Quando o relógio reportar exatamente o mesmo valor:

- manter o total;
- considerar a sincronização bem-sucedida;
- atualizar a última sincronização;
- não criar duplicata;
- não alterar estimativas sem necessidade;
- não apresentar erro.

## Mudança de dia

A Home deve observar o resumo da data atual.

Ela deve reavaliar a data:

- ao retornar ao foreground;
- ao passar da meia-noite enquanto a tela estiver ativa;
- ao iniciar um novo refresh;
- quando o sistema sinalizar mudança de data relevante.

Regras:

- passos do dia anterior não passam para o novo dia;
- leitura após a meia-noite pertence ao novo dia;
- o resumo do dia anterior não é apagado;
- a nova data começa sem leitura ou com zero visual;
- mudança de timezone afeta novas leituras;
- não migrar automaticamente resumos históricos ao alterar timezone nesta fase.

Não criar serviço em background apenas para detectar meia-noite.

## Timestamps

Diferenciar:

- `collectedAt`: instante do pacote FEA1 válido;
- `updatedAt`: instante em que a consolidação foi persistida;
- `DeviceImportMetadata.lastImportedAt`: última sincronização manual válida concluída;
- `lastSuccessfulConnectionAt`: última conexão funcional.

A Home deve usar o timestamp apropriado para mostrar:

```text
Atualizado agora
Atualizado às 09:42
Nunca sincronizado
```

Não usar `lastSuccessfulConnectionAt` como se fosse última sincronização.

## Atualização dos metadados do dispositivo

Após uma leitura FEA1 válida e consolidação local concluída:

- atualizar `DeviceImportMetadata.lastImportedAt`;
- atualizar versão do parser quando aplicável;
- não atualizar somente por conectar;
- não atualizar em timeout;
- não atualizar em payload inválido;
- não atualizar quando a persistência falhar;
- não confundir importação local com Health Connect.

O contrato `DeviceRepository` pode receber uma operação equivalente a:

```text
updateImportMetadata(...)
```

## Distância estimada

Usar esta prioridade:

1. passada manual válida, quando disponível;
2. passada estimada pela altura, quando disponível;
3. distância indisponível quando faltarem dados.

Fórmula:

```text
distanceMeters = steps × strideLengthMeters
```

Não inventar altura ou passada.

Quando não houver dados suficientes:

- permitir `estimatedDistanceMeters = null`;
- mostrar `—` ou mensagem curta;
- indicar que o valor depende do perfil;
- não usar valor padrão oculto.

A Fase 16 poderá personalizar a passada e recalcular estimativas.

## Calorias estimadas

Calorias exigem peso e distância válidos.

Fórmula conhecida:

```text
distanceKm = steps × strideLengthMeters / 1000
estimatedCalories = weightKg × distanceKm × 0.55
```

Regras:

- peso deve ser positivo;
- passada deve ser válida;
- distância deve ser válida;
- retornar `null` quando faltarem dados;
- mostrar que o resultado é estimado;
- não inventar peso;
- não mostrar zero como cálculo real quando os dados estiverem ausentes.

## Dados pessoais mínimos

A implementação deve auditar contratos existentes para:

- perfil atual;
- altura;
- última medição de peso;
- futura passada manual.

Não antecipar toda a Fase 16.

Adicionar somente consultas mínimas necessárias.

Caso os dados ainda não estejam disponíveis, distância e calorias permanecem `null`, e a Home representa corretamente a ausência.

## Meta diária

A Home deve possuir uma meta diária de passos.

Valor padrão:

```text
8.000 passos
```

Adicionar à `AppPreferences` algo equivalente a:

```text
dailyStepsGoal
```

Regras:

- padrão de 8.000;
- valor positivo;
- armazenado como preferência;
- compatibilidade com preferências existentes;
- passos reais não são limitados pela meta;
- edição completa pode permanecer para a Fase 16.

## Progresso da meta

Calcular:

```text
progress = steps / dailyStepsGoal
```

Regras:

- evitar divisão por zero;
- progresso visual pode ser limitado a `1f`;
- texto pode mostrar valores acima da meta;
- exemplo: `9.200 de 8.000 passos`;
- estado sem resumo usa zero visual;
- progresso não é persistido;
- o valor é derivado.

## Home

A Home deve substituir o estado `Painel em preparação`.

Estrutura recomendada:

```text
Cabeçalho
  - Hoje ou data
  - estado do relógio
  - acesso ao perfil

Barra de sincronização
  - visível somente durante refresh
  - indeterminada
  - etapa atual opcional

Resumo principal
  - passos
  - progresso da meta
  - instrução de pull-to-refresh no estado vazio

Métricas estimadas
  - distância
  - calorias
  - indicação Estimado

Sincronização
  - origem
  - última atualização
  - estado de erro ou sucesso
  - retry quando necessário

Demais seções
  - atividades pendentes já persistidas
  - estados neutros para métricas futuras
```

## Estado da Home

Planejar estado observável capaz de representar:

```text
Loading
Ready
Refreshing
Empty
RefreshFailed
DeviceRequired
PermissionsRequired
BluetoothDisabled
```

O estado pode ser uma data class composta, mas não deve permitir combinações contraditórias.

Deve conter, quando aplicável:

- data atual;
- resumo;
- meta;
- progresso;
- dispositivo associado;
- estado da conexão;
- operação de refresh;
- estágio;
- erro seguro;
- última atualização;
- atividades pendentes.

## Sem dispositivo associado

Quando não houver Champion C001 associado:

- mostrar resumo já persistido, se existir;
- informar que não há relógio associado;
- permitir pull-to-refresh;
- ao tentar atualizar, direcionar para ação apropriada;
- oferecer acesso à tela Dispositivo;
- não iniciar scan diretamente na Home sem fluxo explícito;
- não solicitar permissão ao abrir;
- não apagar resumo existente.

## Dispositivo desconectado

Quando houver dispositivo associado, mas desconectado:

- permitir pull-to-refresh;
- o refresh pode iniciar conexão controlada;
- não exigir visita prévia à tela Dispositivo;
- mostrar estágio `Conectando`;
- falha deve permitir retry;
- não manter reconexão infinita;
- manter dados anteriores visíveis.

## Permissões

Quando o usuário iniciar pull-to-refresh e faltar permissão:

- não iniciar scan antes da concessão;
- interromper a operação BLE;
- encerrar a barra quando o fluxo de tratamento terminar;
- mostrar explicação;
- permitir conceder permissão;
- preservar o resumo;
- não entrar em loop.

Como o gesto foi iniciado explicitamente pelo usuário, a UI pode apresentar a solicitação apropriada, seguindo a política já criada nas fases anteriores.

## Bluetooth desligado

Quando o usuário iniciar pull-to-refresh e o Bluetooth estiver desligado:

- interromper a sincronização;
- esconder a barra ao finalizar o tratamento;
- mostrar estado apropriado;
- permitir abrir ação do sistema;
- preservar os dados;
- permitir novo pull-to-refresh depois de habilitado;
- não iniciar loop automático.

## Origem dos dados

Exibir origem amigável:

```text
Champion C001
```

ou:

```text
Relógio
```

Internamente manter:

```text
DataOrigin.WATCH_DAILY_SUMMARY
```

Não exibir nomes internos de enum.

## Métricas futuras

O roadmap menciona último BPM e último treino.

Não implementar novos fluxos para obtê-los nesta fase.

Enquanto as fases responsáveis não estiverem concluídas:

- não inventar BPM;
- não usar valor técnico da PoC como medição de produção;
- não inventar treino;
- omitir a seção ou mostrar `Sem dados`;
- não bloquear o fechamento da Fase 07.

Atividades pendentes já persistidas podem ser observadas sem implementar D3 ou D4.

## Persistência

A estrutura atual de `DailySummary` já suporta:

- passos;
- distância estimada;
- calorias estimadas;
- data;
- dispositivo;
- origem;
- timestamps;
- validade.

Preferir não alterar o schema Room.

Alterar schema somente se uma necessidade funcional real não puder ser atendida.

A meta pertence ao DataStore.

A última importação pertence aos metadados do dispositivo.

## Health Connect

Nesta fase:

```text
FEA1
  → consolidação local
  → Room
  → Home
```

Não:

```text
FEA1
  → Room
  → Health Connect automático
```

Não criar operações do Health Connect ao salvar o resumo.

## Logging e diagnóstico

Pode registrar metadados seguros:

- início;
- fim;
- duração;
- etapa;
- quantidade de pacotes observados;
- pacote válido ou inválido;
- leitura maior, igual ou menor;
- resultado da consolidação;
- categoria de erro.

Não registrar:

- número exato de passos;
- payload;
- bytes;
- hexadecimal;
- MAC;
- identificador persistente;
- peso;
- altura;
- calorias;
- distância;
- dados pessoais;
- stack trace destinado à UI.

## Privacidade

Passos e estimativas permanecem no armazenamento local.

Não enviar para servidor.

Não alterar regras de backup.

Não criar arquivos públicos com dados.

Não incluir resumo diário no relatório de diagnóstico sanitizado.

## Acessibilidade

A atualização deve anunciar estados equivalentes a:

```text
Sincronizando dados do relógio
Dados atualizados
Não foi possível atualizar os dados
```

A barra não deve ser a única indicação de operação ativa.

A instrução de pull-to-refresh deve possuir alternativa acessível.

O botão de retry pode servir como ação explícita acessível em estados de erro.

## Estratégia reduzida de testes

Não criar task exclusiva de testes.

Testes pequenos recomendados:

- parser FEA1 promovido;
- pacote anterior ao refresh não é aceito;
- leitura menor não reduz;
- leitura maior substitui;
- acumulado não é somado;
- nova data cria novo resumo;
- refresh concorrente é ignorado;
- progresso da meta;
- cálculo válido;
- cálculo indisponível sem perfil;
- codec das preferências mantém valor padrão.

Esses testes devem acompanhar as tasks correspondentes.

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

Regressões introduzidas pela Fase 07 devem ser corrigidas.

## Smoke test manual

No Galaxy S22 com Champion C001:

1. abrir a Home;
2. confirmar que não existe botão permanente de sincronização;
3. confirmar instrução de pull-to-refresh no estado vazio;
4. puxar a Home para baixo;
5. confirmar indicador do gesto;
6. confirmar barra linear indeterminada;
7. confirmar dados anteriores visíveis durante a operação;
8. conectar ao relógio, caso necessário;
9. receber FEA1;
10. confirmar passos na Home;
11. confirmar origem;
12. confirmar última atualização;
13. repetir refresh com valor maior;
14. confirmar substituição pelo maior total;
15. repetir com valor igual;
16. confirmar ausência de soma;
17. reproduzir leitura inferior;
18. confirmar ausência de redução;
19. tentar iniciar dois refreshes;
20. confirmar uma única operação;
21. fechar e reabrir o aplicativo;
22. confirmar persistência;
23. sincronizar com relógio inicialmente desconectado;
24. validar conexão controlada;
25. desligar Bluetooth;
26. executar pull-to-refresh;
27. verificar erro seguro;
28. confirmar que a barra foi encerrada;
29. ligar Bluetooth;
30. tentar novamente;
31. negar permissão quando possível;
32. confirmar estado navegável;
33. validar origem e última atualização;
34. validar comportamento de mudança de dia quando possível;
35. confirmar ausência de envio ao Health Connect.

## Dívida técnica

Registrar para fases futuras:

- cobertura ampla da Home na Fase 21;
- testes de pull-to-refresh Compose;
- testes de virada de dia em múltiplos timezones;
- edição da meta na Fase 16;
- personalização da passada na Fase 16;
- recálculo histórico após mudança de perfil;
- Health Connect na Fase 12;
- calendário e histórico na Fase 15;
- BPM real nas fases correspondentes;
- atividades do relógio nas fases D3/D4;
- background somente na Fase 14.

## Critérios de aceite

A Fase 07 será concluída quando:

- a Home exibir passos do dia;
- existir meta padrão de 8.000 passos;
- existir progresso da meta;
- pull-to-refresh for a interação principal;
- não existir botão permanente `Sincronizar agora` no estado normal;
- o conteúdo permanecer visível durante a sincronização;
- a barra linear indeterminada ficar visível durante toda a operação;
- o indicador terminar em todos os caminhos;
- novos gestos não criarem sincronizações concorrentes;
- retry reutilizar o mesmo pipeline;
- auto-conexão isolada não persistir FEA1;
- somente pacote fresco concluir o refresh;
- parser FEA1 estiver promovido e reutilizável;
- FEA1 for tratado como total acumulado;
- leituras não forem somadas;
- leitura menor não reduzir o total;
- leitura igual for tratada como sucesso;
- consolidação for atômica;
- mudança de dia produzir resumo independente;
- origem for exibida;
- última atualização for exibida;
- última importação do dispositivo for atualizada;
- distância e calorias forem exibidas quando calculáveis;
- valores indisponíveis não forem inventados;
- erros permitirem retry;
- Home continuar navegável durante falhas;
- nenhum dado for enviado ao Health Connect;
- não houver D1, D3 ou D4;
- debug e release compilarem;
- o fluxo crítico for validado no Galaxy S22;
- dívida de testes for registrada para a Fase 21.
