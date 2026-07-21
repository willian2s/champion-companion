# Task 004-09 — Implementar Multiplexação De Notification e Indication

## Status

Concluida.

Resumo: implementados `ChampionSubscriptionRegistry`, `ChampionNotificationHub` e `AndroidCccdConfigurator` para fan-out lógico por channel alias, habilitação física única por characteristic e write de CCCD sempre via fila. F008, F002 e FEA1 agora podem ser tratados como streams compartilhados por sessão sem duplicar listener GATT ou reescrever CCCD indevidamente.

## Objetivo

Criar subscriptions físicas únicas por characteristic, com write de CCCD via fila e fan-out para múltiplos consumidores lógicos.

## Contexto

Fase 04 precisa preparar streams compartilhados de F008, F002 e FEA1 sem listener GATT por feature e sem duplicar CCCD.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-08-implement-service-discovery-and-capability-registry.md`
- `docs/protocol/champion-c001.md`

## Dependências

- `004-06`, `004-07` e `004-08` concluídas.

## Entradas

- capabilities da sessão;
- queue e callback bridge.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionSubscriptionRegistry.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidCccdConfigurator.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `AndroidManifest.xml`

## Escopo

- habilitar notifications/indications via fila;
- multiplexar consumidores;
- encerrar streams em disconnect;
- não duplicar subscription física.

## Fora Do Escopo

- parser funcional dos payloads;
- UI de conexão;
- assinatura padrão de FEA2/FEC8.

## Regras De Segurança

- descriptor write sempre pela fila;
- sem subscription física duplicada;
- canal opcional não bloqueia transporte sem justificativa.

## Passos De Implementação

1. Modelar registro de subscribers lógicos por channel alias.
2. Implementar write de CCCD com await de callback correspondente.
3. Garantir apenas uma habilitação física por characteristic.
4. Fechar observers no disconnect e ignorar eventos tardios.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- F008 notify obrigatório pode ser configurado via fila;
- F002 e FEA1 são opcionais e multiplexados;
- remover consumidor não duplica/desabilita subscription indevidamente.

## Testes

- JVM para ref-count lógico, callback duplicado, disconnect e streams fechados.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Notification*"
```

## Validação Final

- confirmar que CCCD não é escrita fora da fila.

## Atualização Documental Esperada

- `Status` deste arquivo concluído;
- registrar se multiplexação exige ADR.

## Implementacao Registrada

Arquivos criados:

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionNotificationHub.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionSubscriptionRegistry.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidCccdConfigurator.kt`

Conclusoes:

- `ChampionSubscriptionRegistry` passou a manter ref-count logico por `ChampionChannelAlias`, estado de habilitacao fisica e lista de subscribers por canal;
- `ChampionNotificationHub` coleta eventos de sessao, faz fan-out de `CharacteristicChanged` para multiplos consumidores e fecha observers em disconnect/close;
- primeira assinatura logica de um canal dispara habilitacao fisica unica;
- remocao de consumidor reduz ref-count logico, mas nao desabilita subscription fisica durante vida da sessao;
- `AndroidCccdConfigurator` encapsula habilitacao de notification/indication com duas etapas enfileiradas: configuracao local do canal e `WRITE_DESCRIPTOR` do CCCD;
- payloads de CCCD seguem padrao seguro: `01 00` para notify e `02 00` para indicate.

Regras preservadas:

- write de CCCD sempre ocorre via `ChampionGattQueue`;
- nenhuma subscription fisica duplicada e criada por feature separada;
- FEA2/FEC8 continuam fora de assinatura padrao e nao bloqueiam transporte;
- eventos tardios apos close/disconnect deixam de ser emitidos pelo hub fechado.

Limite atual:

- multiplexacao foi implementada como infraestrutura reutilizavel, sem ainda acoplar assinatura automatica de canais ao connection manager final;
- callback de descriptor continua resolvido pela infraestrutura de client/queue ja existente.

Validacoes executadas:

- `./gradlew app:compileDebugKotlin`: sucesso;
- `./gradlew app:testDebugUnitTest --tests "*Notification*"`: falha por ausencia de testes com esse filtro nesta fase;
- CCCD nao e escrita fora do configurador via fila nos arquivos criados nesta task.

ADR futura:

- multiplexacao de notifications continua sendo candidata a ADR futura, conforme spec da fase, mas sem formalizacao nesta task.
