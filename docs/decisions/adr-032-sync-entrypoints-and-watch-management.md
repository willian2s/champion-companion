# ADR-032 — Entrypoints de sincronização e gerenciamento do relógio

- **Status:** accepted
- **Data:** 2026-09-14
- **Relacionado:** ADR-030, ADR-031; Spec 033

## Contexto

A Spec 033 separou arquiteturalmente duas operações distintas:

```text
sincronizar relógio != sincronizar Health Connect
```

O pull-to-refresh da Home passou a buscar FEA1, D1 e D3/D4 em um único fluxo
local-first. A escrita no Health Connect continuou explícita em
`Configurações -> Sincronizar agora` e no sync individual de workout.

Após a validação física da 033-06, o caminho técnico estava correto, mas a Home
ainda exibia um atalho de relógio no cabeçalho. Isso dava destaque a gerenciamento
BLE, enquanto a ação remota mais recorrente — publicar a fila local no Health
Connect — exigia navegar até Configurações. A tela Saúde também mantinha um
atalho visual de dispositivo.

## Decisão

1. **Pull-to-refresh da Home continua sendo o único gesto normal de leitura do
   relógio.** Ele atualiza dados locais e nunca executa escrita automática no
   Health Connect.
2. **O cabeçalho da Home passa a expor uma ação explícita de sincronização com o
   Health Connect**, visualmente representada por ícone de atualização/sync.
3. A ação da Home **reutiliza exatamente o mesmo fluxo global de
   `SettingsIntent.SyncNow`** já validado. Não existe um segundo coordinator,
   fila ou política de projeção.
4. A Home mostra estado de sincronização foreground e feedback terminal curto,
   sem ler records de volta do provider.
5. **Configurações passa a ser o entrypoint canônico de gerenciamento do relógio**
   para associação, conexão e manutenção do Champion C001.
6. O atalho de gerenciamento de relógio sai do cabeçalho da Saúde. A Home pode
   preservar CTA contextual de recuperação quando nenhum dispositivo estiver
   associado, porque esse caso é configuração necessária para o próprio resumo.
7. Perfil continua sendo a rota normal para abrir Configurações; a mudança não
   adiciona novo destino primário nem altera o back stack manual Navigation3.

## Contrato de produto

```text
Home pull-to-refresh
  -> WatchSyncCoordinator
  -> FEA1 + D1 + D3/D4
  -> persistência/read models/fila local
  -> sem efeito remoto HC

Home botão Sync
  -> mesmo sync global de Configurações
  -> fila local HC
  -> Watch READY_TO_SYNC elegível
  -> efeito remoto explícito

Configurações -> Gerenciar relógio
  -> DeviceScreen existente
  -> associação/conexão/manutenção BLE
```

## Consequências positivas

- separa visualmente coleta do relógio de publicação no Health Connect;
- reduz o caminho do sync remoto recorrente para um toque na Home;
- remove gerenciamento BLE de superfícies de leitura como Saúde;
- mantém uma única implementação de sync global, já validada fisicamente;
- concentra manutenção do dispositivo em Configurações sem alterar o domínio BLE.

## Riscos e mitigação

- **Ação remota mais acessível:** continua explícita, foreground e protegida pelos
  mesmos guards de permissão, review-before-sync, créditos e fila persistida.
- **Confusão entre dois ícones de atualização:** a semântica acessível diferencia
  `Atualizar dados do relógio` no pull-to-refresh de `Sincronizar com Health
  Connect` no cabeçalho.
- **Falhas silenciosas:** a Home exibe progresso durante o gesto e feedback
  terminal por snackbar usando a mensagem do fluxo global existente.

## Evidência de fechamento — 2026-09-15

No dispositivo real, Home exibiu coleta separada de `Sincronizar com Health
Connect`; Configurações exibiu `Gerenciar relógio` e `Sincronizar agora`; Saúde
não exibiu CTA de relógio/BLE. Dois refreshes confirmaram preservação local em
partial success. O sync explícito foi acionado separadamente, sem leitura do
provider. Ver `docs/integration/033-09-final-physical-validation.md`.

## Alternativas rejeitadas

### Manter sync Health Connect somente em Configurações

Rejeitada porque transforma uma ação operacional recorrente em fluxo de navegação
administrativa, apesar de o pipeline global já ser seguro e explícito.

### Criar um segundo sync coordinator exclusivo da Home

Rejeitada porque duplicaria política de fila, batching, READY_TO_SYNC e tratamento
de falhas, abrindo risco de divergência em relação a Configurações.

### Manter relógio como ação principal do cabeçalho da Home

Rejeitada porque associação/conexão é configuração eventual, enquanto a Home já
faz a leitura normal do relógio por pull-to-refresh.
