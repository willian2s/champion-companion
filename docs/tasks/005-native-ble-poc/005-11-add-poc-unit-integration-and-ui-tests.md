# Task 005-11 — Adicionar Testes Automatizados Da PoC

## Status

Planejada.

## Objetivo

Cobrir PoC com testes JVM, integração com fakes e instrumentados mínimos antes da primeira validação física.

## Contexto

Gate A exige validação física, mas não dispensa rede de segurança automatizada para builder, parsers, state machine, cleanup, sanitização e regressões básicas do fluxo.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- todas tasks `005-02` a `005-10`

## Dependências

- `005-10` concluída.

## Pré-Condições

- implementação lógica principal da PoC já está pronta.

## Entradas

- builders, parsers, coordenador, tela técnica e relatório.

## Arquivos Exatos Permitidos

- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/**`
- `docs/tasks/005-native-ble-poc/005-11-add-poc-unit-integration-and-ui-tests.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/*.md`

## Escopo

- testes JVM da PoC;
- integração com fakes;
- instrumentados necessários para permissões/UI/export.

## Fora Do Escopo

- validação física;
- commit de dados reais do relógio.

## Passos

1. Adicionar testes JVM listados na spec.
2. Adicionar testes de integração com fakes para sucesso e falhas principais.
3. Adicionar instrumentados mínimos para permission request, tela técnica e export quando necessário.
4. Executar suite relevante.
5. Atualizar `Status` e overview.

## Regras De Segurança

- não substituir validação física por fake;
- não usar fixture com dado real sensível;
- manter logs sanitizados também em testes.

## Critérios De Aceite

- builders, parsers, estimador, cleanup, state machine e relatório têm cobertura;
- cenários de falha principais têm testes de integração;
- UI/permissions/export têm instrumentados só se realmente necessários.

## Testes

- `testDebugUnitTest`
- `connectedDebugAndroidTest`, se houver instrumentados

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest
./gradlew app:connectedDebugAndroidTest
```

## Evidências Esperadas

- suites verdes ou bloqueio reproduzível documentado.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar que bloqueios físicos seguem fora dos testes automatizados.
