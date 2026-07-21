# Task 005-01 — Auditar Contratos Da Fase 04 E Lacunas Físicas

## Status

Planejada.

## Objetivo

Confirmar pontos de reutilização obrigatória da Fase 04, lacunas reais para `TransportReady` físico e pontos de extensão mínimos antes de implementar a PoC.

## Contexto

Fase 05 deve reutilizar o núcleo BLE existente. Auditoria inicial precisa evitar que permissões, A1 e D0/F002 sejam construídos sobre core incompleto ou com atalhos fora das fronteiras.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/handshake-a1.md`
- `docs/protocol/heart-rate-d0-f002.md`

## Dependências

- `005-00` lida.

## Pré-Condições

- nenhuma.

## Entradas

- estado atual de `core/bluetooth/**` e `feature/device/**`.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-01-audit-phase-04-contracts-and-physical-gaps.md`
- `docs/specs/005-native-ble-poc.md`, somente se ajuste factual for necessário

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-05-native-ble-poc-direction.md`
- qualquer arquivo Kotlin nesta task

## Escopo

- auditar contratos e bindings existentes;
- listar gaps estruturais reais do core para PoC física;
- confirmar dependências de tasks seguintes;
- registrar decisões que podem exigir ADR futura.

## Fora Do Escopo

- implementar código;
- alterar manifesto;
- criar ADR.

## Passos

1. Revisar `ChampionConnectionManager`, `ChampionTransportSession`, `ChampionGattQueue`, `ChampionCommand`, `ChampionPacket`, `ChampionPacketRouter`, `ChampionNotificationHub`, `ChampionCapabilityRegistry`, `ChampionBleState`, `ChampionBleError` e `ChampionProtocolLogger`.
2. Confirmar bindings Hilt e fakes existentes.
3. Registrar gaps que impedem `TransportReady` físico ou command path da PoC.
4. Mapear onde Fase 05 pode estender o core sem quebrar fronteiras.
5. Atualizar `Status` e resumo.

## Regras De Segurança

- não promover inferência física a requisito confirmado;
- não sugerir stack paralelo BLE;
- não alterar código nesta task.

## Critérios De Aceite

- contratos e reuso obrigatório ficam listados sem ambiguidade;
- gaps estruturais do core ficam identificados antes de tasks funcionais;
- extensões permitidas ficam delimitadas.

## Testes

- não aplicável; auditoria documental e de código.

## Comandos De Validação

- nenhum obrigatório.

## Evidências Esperadas

- lista de gaps reais, como resolver de capabilities, discovery snapshot e dispatch de comandos.

## Atualização Documental

- concluir `Status` com resumo factual.

## Validação Final

- confirmar ausência de alteração em Kotlin, Manifest e roadmap.
