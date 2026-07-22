# ADR-008 — Manter sessão BLE enquanto app permanecer aberto

## Status

Aceita

## Contexto

Fase 06 colocou `DeviceManagementViewModel` como fronteira de estado da feature de dispositivo e reutilizou `ChampionConnectionManager` como owner da sessão BLE ativa. Na prática, `DeviceScreen` ainda enviava `ExitVisibleScreen` no `onDispose`, e `DeviceManagementViewModel.onFeatureExit()` executava `connectionManager.disconnect(CLEANUP)` ao sair da tela.

Com navegação top-level atual, abrir `Perfil`, `Configurações` ou qualquer outra tela desmonta `DeviceScreen`. Isso fazia a conexão BLE cair mesmo com app ainda aberto e sem ação explícita do usuário para desconectar.

Esse comportamento conflita com dois objetivos já aceitos no projeto:

- auto-conexão controlada do dispositivo associado para reduzir atrito diário, conforme ADR-007;
- reuso de sessão saudável entre telas e fluxos de sync manual da Home na Fase 07.

## Decisão

Enquanto app permanecer aberto em foreground, sessão BLE saudável não pertence à tela de dispositivo. Ela pertence ao escopo do app/ViewModel e deve continuar ativa independentemente da tela visível.

Regras obrigatórias:

- trocar de tela não pode disparar disconnect implícito de sessão saudável;
- abrir `Perfil`, `Configurações`, `Home` ou outra tela não pode derrubar conexão já estabelecida;
- conexão em andamento e handshake/preparação em andamento também devem sobreviver à troca de tela;
- somente operações estritamente transitórias da tela podem ser canceladas ao sair dela, como scan manual ou request de permissão;
- disconnect continua reservado para ação explícita do usuário, falha real de transporte/protocolo, esquecimento do dispositivo ou cleanup de processo/app;
- fluxos futuros de sync manual podem assumir reuso de `ChampionConnectionManager.currentSession()` sem exigir volta prévia à tela de dispositivo.

## Consequências

### Positivas

- experiência diária fica consistente: conexão não depende da tela atual;
- Home, Perfil e Configurações podem coexistir com sessão BLE ativa;
- Fase 07 ganha base correta para refresh manual reaproveitar sessão saudável;
- reduz reconnects desnecessários e churn de lifecycle entre telas top-level.

### Negativas

- ciclo de vida da sessão fica menos óbvio para quem olhar só `DeviceScreen`;
- feature precisa distinguir melhor operação transitória de tela versus sessão de app;
- bugs de sessão presa ou reconnect indevido passam a exigir atenção maior no escopo do ViewModel/app, não só do Composable.

## Alternativas consideradas

- manter disconnect no `onDispose` da tela de dispositivo;
- desconectar sempre que usuário sair da feature de dispositivo;
- mover manutenção da sessão apenas para future foreground service/background BLE.

Alternativas foram rejeitadas por quebrarem continuidade entre telas, aumentarem atrito para uso diário e conflitariarem com reuso de sessão já necessário para Home com sync manual.
