# Direção do gerenciamento de dispositivo da Fase 06

## Contexto

A Fase 06 transforma a prova de conceito BLE validada na Fase 05 em uma feature de dispositivo utilizável no Champion Companion.

As fases anteriores já entregaram:

- projeto Android nativo;
- design system Material 3;
- domínio e persistência com Room e DataStore;
- núcleo BLE nativo;
- scanner;
- fila GATT serial;
- discovery;
- capability registry;
- notifications;
- command dispatch;
- handshake A1;
- leitura técnica de passos FEA1;
- sessão técnica D0/F002;
- tela técnica da PoC;
- validação física no Galaxy S22 com Champion C001.

A implementação da Fase 05 deve ser reutilizada. Não criar um segundo fluxo BLE ou duplicar scanner, conexão, handshake ou subscriptions.

## Objetivo

Criar uma feature de gerenciamento do Champion C001 que permita ao usuário:

- conceder as permissões necessárias;
- procurar dispositivos manualmente;
- visualizar dispositivos encontrados;
- selecionar um Champion C001;
- conectar;
- concluir o handshake necessário;
- lembrar o dispositivo selecionado;
- visualizar o dispositivo lembrado;
- visualizar estado real da conexão;
- visualizar a última conexão bem-sucedida;
- visualizar a última sincronização, quando existir;
- desconectar;
- tentar novamente;
- esquecer o dispositivo;
- acessar diagnóstico seguro;
- exportar relatório sanitizado.

## Prioridade de desenvolvimento

Nesta fase, a prioridade é entregar o fluxo funcional.

Testes automatizados extensos não são gate da Fase 06.

A cobertura abrangente será tratada principalmente na Fase 21 — Testes e qualidade.

A Fase 06 não deve criar:

- task exclusiva de testes;
- meta de cobertura;
- matriz extensa de testes de interface;
- expansão ampla de fakes;
- testes instrumentados para cada estado visual;
- regressão completa de todas as fases anteriores.

Continuam obrigatórios:

- código compilável;
- `assembleDebug`;
- `assembleRelease`;
- instalação debug no Galaxy S22;
- validação manual dos fluxos críticos;
- correção de falhas introduzidas diretamente pela Fase 06.

Testes existentes não devem ser removidos ou desativados.

Um teste automatizado novo pode ser criado quando:

- proteger uma regra crítica pequena;
- reproduzir um bug encontrado durante a fase;
- for simples e rápido;
- evitar risco claro de perda de dispositivo ou estado inválido.

Falhas automatizadas anteriores e não relacionadas devem ser documentadas como dívida técnica, sem bloquear a fase.

## Fonte de verdade

Respeitar, nesta ordem:

1. `AGENTS.md`;
2. este documento de direção;
3. ADRs vigentes;
4. spec da Fase 06;
5. implementação e documentos concluídos da Fase 05;
6. implementação e documentos concluídos da Fase 04;
7. documentos de protocolo;
8. roadmap;
9. baseline do MotionLog apenas como referência funcional.

O arquivo `docs/core/current-project-baseline.md` pode estar desatualizado em relação à Fase 05.

Durante o planejamento:

- não alterá-lo;
- conferir o estado real do código;
- considerar os documentos concluídos da Fase 05;
- registrar uma task de atualização da baseline no encerramento da Fase 06.

## Limites da fase

A Fase 06 inclui:

- tela utilizável de dispositivo;
- ViewModel ou coordinator de feature;
- injeção dos componentes BLE existentes;
- solicitação controlada de permissões;
- scan manual;
- lista de dispositivos encontrados;
- seleção de dispositivo;
- conexão manual;
- handshake após ação explícita de conexão;
- associação persistente de um Champion C001;
- estado de conexão;
- última conexão;
- última sincronização, quando disponível;
- disconnect;
- retry;
- esquecimento;
- diagnóstico seguro;
- exportação sanitizada;
- estados de loading, vazio e erro.

A Fase 06 não inclui:

- sincronização de passos;
- persistência de passos;
- resumo diário;
- distância e calorias;
- medição final de BPM na tela Saúde;
- D1;
- D3;
- D4;
- Health Connect;
- auto-connect ao abrir;
- conexão em background;
- foreground service;
- reconexão infinita;
- suporte a outros relógios;
- tela completa de diagnóstico avançado;
- exclusão de histórico de saúde;
- mudança ampla do schema sem necessidade funcional.

## Controle do usuário

O usuário deve controlar quando o BLE é usado.

Não iniciar automaticamente ao abrir o aplicativo:

- scan;
- conexão;
- handshake;
- leitura FEA1;
- D0;
- D1;
- D3;
- D4;
- sincronização.

A ação manual `Conectar` pode executar internamente:

```text
resolver dispositivo lembrado
    ↓
conectar transporte
    ↓
discovery
    ↓
TransportReady
    ↓
handshake A1
    ↓
Connected
```

O usuário não precisa executar o handshake por um botão separado na feature final.

Sucesso de conexão funcional exige:

- transporte pronto;
- handshake confirmado ou estado operacional explicitamente aceito pela implementação validada na Fase 05.

Não apresentar apenas `TransportReady` como conexão totalmente funcional.

## Dispositivos encontrados

Os resultados de scan são efêmeros.

A lista deve mostrar somente candidatos compatíveis com o filtro Champion existente.

Cada item pode apresentar:

- nome;
- estado selecionado;
- intensidade aproximada do sinal, quando disponível e confiável;
- identificador local reduzido ou mascarado;
- ação para conectar.

Não persistir todos os resultados de scan.

Não exibir no fluxo principal:

- payload;
- bytes;
- UUIDs;
- detalhes GATT;
- endereço completo sem necessidade;
- dados técnicos da PoC.

O scan deve:

- ser iniciado manualmente;
- exibir loading;
- poder ser cancelado;
- terminar em sucesso, timeout, erro ou cancelamento;
- impedir scans simultâneos;
- permitir nova tentativa.

## Dispositivo lembrado

O aplicativo deve manter no máximo um Champion C001 associado como dispositivo principal nesta fase.

A associação deve persistir após:

- fechamento do aplicativo;
- recriação da Activity;
- reinicialização do aparelho.

Dados persistidos podem incluir:

- ID interno;
- identificador da plataforma;
- nome;
- modelo;
- firmware, quando conhecido;
- data da associação;
- última conexão bem-sucedida;
- última importação ou sincronização conhecida.

Não persistir:

- estado `Connecting`;
- estado `Connected`;
- estado de scan;
- estado de handshake;
- sessão GATT;
- capabilities efêmeras;
- erro transitório;
- payloads da conexão.

## Momento da associação

O dispositivo deve ser lembrado somente depois de uma conexão funcional bem-sucedida.

Fluxo recomendado:

```text
selecionar candidato
    ↓
conectar
    ↓
atingir TransportReady
    ↓
concluir handshake
    ↓
persistir associação
    ↓
mostrar dispositivo conectado
```

Uma tentativa que falhe antes da conexão funcional não deve substituir o dispositivo já associado.

## Semântica de associação

A implementação deve diferenciar:

- dispositivo descoberto;
- dispositivo selecionado;
- dispositivo associado;
- dispositivo conectado;
- dispositivo desconectado;
- dispositivo esquecido.

Não representar todos esses estados com um único booleano.

A estratégia de persistência deve permitir:

- observar o dispositivo associado;
- associar ou substituir o dispositivo;
- atualizar a última conexão;
- recuperar a associação após reinício;
- esquecer a associação;
- preservar dados históricos futuros.

Esquecer o relógio não deve apagar automaticamente:

- resumos diários;
- medições cardíacas;
- atividades;
- histórico de sincronização;
- demais dados de saúde.

Caso a estrutura atual não permita remover somente a associação sem remover dados históricos, o planejamento deve propor uma evolução mínima do modelo.

Alternativas aceitáveis para avaliação:

- estado de associação na entidade do dispositivo;
- referência do dispositivo associado em DataStore;
- entidade própria de associação.

A decisão deve considerar simplicidade, integridade futura e impacto no Room.

Se exigir decisão arquitetural duradoura, recomendar ADR.

## Repository de dispositivo

O contrato atual deve ser ampliado de forma orientada ao domínio.

Planejar operações equivalentes a:

- observar dispositivo associado;
- associar dispositivo;
- atualizar última conexão bem-sucedida;
- atualizar metadados conhecidos;
- esquecer associação;
- localizar dispositivo por identificador da plataforma.

Evitar expor DAO para a feature.

Não criar um repository genérico de BLE.

Persistência da associação continua separada da conexão efêmera.

## Conexão do dispositivo lembrado

Quando o usuário tocar em `Conectar`:

1. verificar permissões;
2. verificar Bluetooth;
3. usar o identificador persistido para resolver o dispositivo;
4. conectar usando o núcleo BLE;
5. executar discovery;
6. atingir `TransportReady`;
7. executar A1;
8. atualizar última conexão;
9. refletir estado `Connected`.

Caso não seja possível resolver diretamente o dispositivo:

- executar scan manualmente autorizado pela mesma ação;
- procurar o identificador lembrado;
- não conectar a outro Champion automaticamente;
- informar quando o dispositivo lembrado não for encontrado.

Não realizar loop infinito.

## Seleção de novo dispositivo

Quando nenhum dispositivo estiver associado:

- mostrar estado vazio;
- oferecer ação `Adicionar dispositivo`;
- solicitar permissões quando necessário;
- iniciar scan após ação explícita;
- listar resultados;
- permitir conectar a um candidato;
- associar somente após sucesso.

Quando já existir dispositivo associado:

- a tela principal deve priorizar esse dispositivo;
- procurar outro dispositivo deve ser uma ação secundária explícita;
- substituir associação deve exigir confirmação;
- não substituir silenciosamente.

## Estados da feature

Planejar estado de UI explícito suficiente para representar:

```text
Loading
NoAssociatedDevice
PermissionsRequired
BluetoothDisabled
ReadyToScan
Scanning
ScanResults
Connecting
Connected
Disconnecting
Disconnected
DeviceNotFound
ConnectionFailed
Forgetting
Error
```

Os nomes definitivos podem ser ajustados.

O estado deve combinar corretamente:

- dispositivo persistido;
- ambiente BLE;
- estado do transporte;
- estado do handshake;
- operação iniciada pelo usuário.

Evitar booleanos independentes contraditórios.

## Estado de conexão

A UI deve refletir o estado real do `ChampionConnectionManager`.

Representações principais:

- desconectado;
- procurando;
- conectando;
- preparando conexão;
- conectado;
- desconectando;
- falha.

A UI não deve continuar mostrando conectado quando:

- o GATT desconectar;
- a sessão for fechada;
- o handshake for invalidado;
- ocorrer erro de transporte;
- o Bluetooth for desligado.

A conexão efêmera não deve ser restaurada como conectada após recriação do processo.

## Última conexão

Atualizar `lastSuccessfulConnectionAt` somente quando a conexão funcional estiver pronta.

Não atualizar apenas porque:

- o scan encontrou o relógio;
- `openSession` foi chamado;
- o GATT conectou antes de discovery;
- uma escrita foi aceita;
- o handshake falhou.

Exibir data e hora formatadas de forma local e acessível.

## Última sincronização

Enquanto as fases de sincronização ainda não estiverem implementadas:

- exibir `Nunca sincronizado`;
- ou exibir `Ainda não disponível`;
- não usar a última conexão como se fosse sincronização;
- não inventar timestamp;
- não atualizar import metadata durante conexão simples.

Quando `DeviceImportMetadata.lastImportedAt` tiver valor, a UI pode exibi-lo como última sincronização/importação conhecida.

## Disconnect

A ação `Desconectar` deve:

- ser explícita;
- cancelar operação ativa quando necessário;
- encerrar subscriptions;
- invalidar handshake;
- fechar a sessão;
- atualizar estado da UI;
- preservar o dispositivo associado;
- não apagar dados;
- não iniciar reconexão automática.

Disconnect inesperado deve:

- atualizar estado;
- mostrar erro seguro;
- permitir `Tentar novamente`;
- não bloquear a navegação.

## Retry

`Tentar novamente` deve repetir somente a ação apropriada ao erro atual.

Exemplos:

- permissão ausente → solicitar ou orientar;
- Bluetooth desligado → reavaliar ambiente;
- scan expirado → iniciar novo scan;
- dispositivo lembrado não encontrado → procurar novamente;
- conexão falhou → tentar nova conexão;
- handshake falhou → iniciar nova tentativa controlada;
- disconnect inesperado → conectar novamente.

Não criar retry automático infinito.

## Esquecer dispositivo

A ação `Esquecer dispositivo` deve exigir confirmação.

Ao confirmar:

1. impedir nova operação;
2. desconectar, caso conectado;
3. limpar associação persistida;
4. limpar estado efêmero da feature;
5. retornar ao estado sem dispositivo;
6. preservar dados históricos;
7. permitir adicionar outro Champion C001.

A falha ao desconectar não deve impedir indefinidamente o esquecimento local, desde que o cleanup seja executado de forma segura.

## Permissões e Bluetooth desligado

Reutilizar a modelagem de pré-requisitos criada nas Fases 04 e 05.

A feature deve:

- solicitar permissões somente após ação;
- explicar a necessidade;
- permitir tentar novamente;
- indicar Bluetooth desligado;
- abrir a ação apropriada do sistema quando necessário;
- não criar loop de permissão;
- continuar navegável após negação.

Não adicionar permissões novas sem necessidade.

## Diagnóstico

A tela principal pode oferecer `Diagnóstico`.

O diagnóstico deve reutilizar informações seguras já existentes.

Pode mostrar:

- permissões;
- suporte BLE;
- Bluetooth habilitado;
- estado de transporte;
- capabilities como presentes ou ausentes;
- última categoria de erro;
- timestamps e duração de operações;
- contagem de eventos sanitizados.

Não mostrar:

- payload;
- hexadecimal;
- bytes;
- BPM;
- passos;
- senha;
- endereço completo em relatório;
- dados pessoais;
- stack trace.

A tela técnica detalhada da Fase 05 pode permanecer disponível em debug, mas não deve ser a tela principal de dispositivo.

## Exportação

A exportação deve continuar sanitizada e explícita.

Pode reutilizar o exporter da PoC, ajustando-o para um relatório de diagnóstico da feature.

O relatório deve ser salvo em área privada ou cache privado e compartilhado por mecanismo seguro.

Não exportar dados de saúde.

## Interface

Usar o design system criado na Fase 02.

A tela deve possuir, no mínimo:

### Sem dispositivo

- título;
- explicação curta;
- estado de permissões, quando relevante;
- ação `Adicionar dispositivo`.

### Procurando

- indicador de progresso;
- ação para cancelar;
- resultados conforme chegarem ou ao final.

### Resultados

- lista de dispositivos;
- nome;
- identificador seguro;
- ação para conectar;
- estado vazio quando nenhum for encontrado.

### Dispositivo associado

- nome;
- modelo;
- estado da conexão;
- última conexão;
- última sincronização;
- botão conectar ou desconectar;
- tentar novamente quando necessário;
- menu ou ação para esquecer;
- acesso ao diagnóstico.

### Erro

- mensagem segura;
- ação adequada;
- conteúdo restante da tela navegável.

Reutilizar componentes como:

- `ConnectionStatus`;
- `PrimaryActionButton`;
- `EmptyState`;
- `ErrorState`;
- `LoadingState`;
- `PermissionBanner`.

Não reutilizar a aparência técnica da PoC como interface final.

## Arquitetura da feature

Direção esperada:

```text
DeviceScreen
    ↓
DeviceViewModel
    ↓
casos de uso ou coordinator da feature
    ├── DeviceRepository
    ├── ChampionConnectionManager
    ├── protocolo A1 reutilizável
    └── diagnóstico sanitizado
```

O Composable não deve:

- construir manualmente scanner;
- construir cliente GATT;
- criar `CoroutineScope` de infraestrutura;
- construir connection manager;
- acessar Room;
- acessar DAO;
- acessar `BluetoothGatt`;
- controlar diretamente a fila GATT.

Os componentes devem vir por Hilt.

## Reutilização da PoC

Avaliar o que deve ser promovido para componentes reutilizáveis:

- handshake A1;
- coordinator de bootstrap;
- estado de protocolo;
- report exporter;
- erros seguros;
- permissões;
- componentes visuais úteis.

Não transportar para a feature final:

- state machine inteira do ciclo de BPM;
- passos e BPM técnicos na tela principal;
- botões manuais de cada etapa;
- contadores internos;
- detalhes de protocolo;
- duração e dados do Gate A.

A PoC debug pode permanecer acessível separadamente.

## Ciclo de vida

A feature deve sobreviver corretamente a recomposição.

Não manter infraestrutura BLE com `remember` dentro do Composable.

Planejar ownership adequado para:

- `ChampionConnectionManager`;
- ViewModel;
- sessão ativa;
- coordinator;
- observers.

Ao sair da tela:

- não iniciar disconnect automaticamente apenas por recomposição;
- não manter referências de Activity;
- não duplicar collectors;
- não duplicar scan;
- não duplicar conexão.

O comportamento ao sair da tela durante conexão deve ser explícito.

Como background BLE ainda está fora de escopo, uma sessão não deve depender de serviço permanente.

## Segurança e privacidade

Não registrar em Logcat:

- MAC;
- identificador persistente;
- payload;
- bytes;
- dados de saúde;
- senha;
- stack trace destinado à UI.

Identificadores apresentados na UI devem ser locais e minimizados.

Não enviar dados para servidor.

Não alterar as regras de backup.

## Estratégia de testes reduzida

Não criar task exclusiva de testes.

A validação obrigatória da fase é:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew installDebug
```

Quando útil, executar também:

```bash
./gradlew lint
./gradlew testDebugUnitTest
```

Porém:

- falhas conhecidas e não relacionadas não bloqueiam a fase;
- documentar resultado;
- não iniciar uma campanha de correção da suíte;
- corrigir somente regressões causadas pela Fase 06;
- não aumentar cobertura por obrigação.

Validação manual mínima no Galaxy S22:

1. abrir tela Dispositivo;
2. conceder ou verificar permissões;
3. iniciar scan;
4. encontrar Champion C001;
5. selecionar e conectar;
6. confirmar estado conectado;
7. fechar e abrir novamente a tela;
8. confirmar que o dispositivo permanece lembrado;
9. desconectar;
10. conectar novamente;
11. desligar Bluetooth e verificar estado;
12. ligar Bluetooth e tentar novamente;
13. esquecer dispositivo;
14. confirmar retorno ao estado vazio;
15. associar novamente;
16. abrir diagnóstico;
17. exportar relatório sanitizado.

## Dívida técnica

A spec e o overview devem possuir seção de dívida técnica adiada.

Registrar pelo menos:

- falhas automatizadas anteriores ainda abertas;
- testes desejáveis não implementados;
- cenários que deverão ser cobertos na Fase 21;
- riscos aceitos temporariamente;
- validações feitas somente de forma manual.

A dívida técnica não deve virar uma lista de tasks obrigatórias da Fase 06.

## Critérios de aceite

A Fase 06 será considerada concluída quando:

- a tela release não for mais placeholder;
- a feature usar Hilt e ViewModel;
- o Composable não construir a infraestrutura BLE;
- o usuário puder iniciar scan;
- dispositivos Champion forem listados;
- um Champion C001 puder ser selecionado;
- conexão e handshake ocorrerem após ação explícita;
- o dispositivo for lembrado após sucesso;
- associação sobreviver à reinicialização;
- estado da conexão refletir o transporte real;
- última conexão for atualizada corretamente;
- última sincronização não for inventada;
- o usuário puder desconectar;
- o usuário puder tentar novamente;
- o usuário puder esquecer o dispositivo;
- esquecer preservar dados históricos;
- falhas não bloquearem navegação;
- diagnóstico seguro estiver acessível;
- exportação continuar sanitizada;
- não houver auto-connect ao abrir;
- não houver reconexão infinita;
- não houver implementação de D1, D3, D4 ou Health Connect;
- debug e release compilarem;
- o fluxo crítico for validado manualmente no Galaxy S22;
- dívidas de testes forem registradas para a Fase 21.
