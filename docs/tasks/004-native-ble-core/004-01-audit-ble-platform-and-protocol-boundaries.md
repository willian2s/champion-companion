# Task 004-01 — Auditar Fronteiras BLE, Plataforma e Protocolo

## Status

Pendente.

## Objetivo

Confirmar limites arquiteturais, lacunas físicas e contratos herdados da baseline antes de criar código BLE.

## Contexto

Fase 04 não pode copiar comportamento MotionLog cegamente. Precisa separar transporte, protocolo, domínio e UI com base em documentos persistidos.

## Documentos Obrigatórios

- `docs/tasks/004-native-ble-core/004-00-overview.md`
- `docs/specs/004-native-ble-core.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/protocol/champion-c001.md`
- `docs/protocol/ble-transport.md`
- `docs/protocol/ble-command-mapping.md`
- `docs/protocol/handshake-a1.md`
- `docs/baseline/gaps-and-open-questions.md`
- `docs/baseline/source-map.md`

## Dependências

- `004-00` lida.

## Entradas

- documentação acima;
- estado atual de `app/src/main/java/br/com/mwconsulting/championcompanion`.

## Arquivos Exatos Permitidos

- `docs/tasks/004-native-ble-core/004-01-audit-ble-platform-and-protocol-boundaries.md`
- `docs/specs/004-native-ble-core.md`, somente se auditoria exigir ajuste factual.

## Arquivos Que Não Podem Ser Modificados

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- qualquer arquivo Kotlin nesta task.

## Escopo

- confirmar fronteiras permitidas;
- listar lacunas físicas ainda abertas;
- confirmar canais obrigatórios e opcionais;
- confirmar proibições de logging e acesso a Room/Health Connect.

## Fora Do Escopo

- criar código BLE;
- adicionar permissões;
- alterar manifest;
- criar ADR automaticamente.

## Regras De Segurança

- não promover informação `Ainda não confirmado` a requisito obrigatório;
- não planejar auto-connect, retry infinito ou background BLE;
- não planejar logs com payload.

## Passos De Implementação

1. Revisar documentos autoritativos e confirmar ordem de precedência.
2. Registrar fronteiras entre Android BLE, transporte, protocolo, domínio e UI.
3. Consolidar lacunas físicas que permanecem para Fase 05.
4. Registrar se alguma decisão precisa ADR futura.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- fronteiras arquiteturais documentadas sem ambiguidade;
- lacunas físicas listadas por impacto;
- canais obrigatórios/opcionais confirmados;
- nenhuma decisão especulativa promovida a fato.

## Testes

- não aplicável; auditoria documental.

## Comandos De Validação

- nenhum obrigatório.

## Validação Final

- confirmar que nenhum arquivo Kotlin, Gradle ou Manifest foi alterado.

## Atualização Documental Esperada

- `Status` deste arquivo concluído com resumo das conclusões.
