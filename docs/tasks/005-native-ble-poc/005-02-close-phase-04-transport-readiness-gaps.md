# Task 005-02 — Fechar Gaps De `TransportReady` Físico Herdados Da Fase 04

## Status

Planejada.

## Objetivo

Fechar gaps mínimos do core para permitir caminho real até `TransportReady` físico e envio seguro de comandos reutilizáveis na PoC.

## Contexto

Hoje o Hilt injeta `ChampionCapabilityResolver` stub e o caminho concreto ainda não fecha snapshot completo de discovery nem dispatch genérico de `ChampionCommand`. Sem isso, A1 e D0/F002 ficariam bloqueados ou espalhados em camada errada.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/tasks/005-native-ble-poc/005-01-audit-phase-04-contracts-and-physical-gaps.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/004-native-ble-core.md`

## Dependências

- `005-01` concluída.

## Pré-Condições

- auditoria dos gaps concluída.

## Entradas

- contratos BLE atuais;
- Hilt atual;
- wrappers Android atuais.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleModule.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionConnectionManager.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportSession.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBleContracts.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionTransportStreams.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/**`
- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `docs/tasks/005-native-ble-poc/005-02-close-phase-04-transport-readiness-gaps.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `app/src/main/AndroidManifest.xml`
- `feature/device/**`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- resolver snapshot concreto de discovery/capabilities;
- fechar wiring Hilt necessário;
- expor dispatch mínimo de `ChampionCommand` reaproveitável pela PoC;
- preservar semântica de `TransportReady` como transporte puro.

## Fora Do Escopo

- permissões de manifesto;
- A1 funcional completo;
- FEA1/D0/F002 funcionais;
- UI técnica.

## Passos

1. Implementar `ChampionCapabilityResolver` concreto usando snapshot real de discovery.
2. Garantir que wrappers Android exponham dados mínimos necessários sem vazar `BluetoothGatt`.
3. Criar adaptador mínimo de dispatch de `ChampionCommand` para fila + await estruturado.
4. Validar que `TransportReady` continua sem significado de handshake.
5. Adicionar ou ajustar testes JVM/integration do core para esses gaps.
6. Atualizar overview e `Status`.

## Regras De Segurança

- não mudar semântica de `ChampionBleState.TransportReady`;
- não mover lógica para UI;
- não introduzir scanner paralelo;
- não logar payload.

## Critérios De Aceite

- app consegue atingir `TransportReady` físico sem stub de capabilities;
- caminho de comando reutilizável existe acima da fila existente;
- mudanças são mínimas e cobertas por teste;
- nenhuma feature recebe `BluetoothGatt`.

## Testes

- JVM para resolver de capabilities e command dispatch.
- integração com fakes para caminho `connect -> discovery -> capabilities -> TransportReady`.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Ble*"
./gradlew app:testDebugUnitTest --tests "*ConnectionManager*"
```

## Evidências Esperadas

- testes cobrindo resolver concreto e dispatch de comando.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar sem alterações em Manifest, Compose e roadmap.
