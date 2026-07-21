# Task 004-09 — Implementar Multiplexação De Notification e Indication

## Status

Pendente.

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
