# Task 004-14 — Adicionar Testes Do Núcleo BLE

## Status

Pendente.

## Objetivo

Cobrir núcleo BLE com testes JVM prioritários e instrumentados mínimos quando Android BLE for realmente indispensável.

## Contexto

Fase 04 depende fortemente de concorrência, timeout e cleanup. Sem testes, regressões são prováveis e difíceis de diagnosticar.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/tasks/004-native-ble-core/004-06-implement-gatt-operation-model-and-serial-queue.md`
- `docs/tasks/004-native-ble-core/004-11-implement-connection-manager-and-cleanup.md`

## Dependências

- `004-13` concluída.

## Entradas

- componentes BLE implementados;
- fakes criados em `004-13`.

## Arquivos Exatos Permitidos

- `app/src/test/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`
- `app/src/androidTest/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, somente se indispensável
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `feature/**`
- `data/**`
- `docs/specs/004-native-ble-core.md`

## Escopo

- testes JVM prioritários da fila, estados, filtro, capabilities, routing, logs e cleanup;
- instrumentados mínimos para wrappers Android, se necessários.

## Fora Do Escopo

- teste físico com Champion C001;
- testes de A1, D0, D1, D3 ou D4 funcionais.

## Regras De Segurança

- priorizar JVM sempre que possível;
- não introduzir testes que exijam scan ou conexão reais nesta fase;
- não usar bytes sensíveis em logs de teste.

## Passos De Implementação

1. Adicionar testes JVM para todos cenários obrigatórios listados na spec.
2. Adicionar instrumentados apenas para wrappers Android sem alternativa razoável.
3. Garantir que testes cubram timeout, cancelamento, retry, sessão antiga, callback duplicado e logging seguro.
4. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- cobertura JVM existe para filtro, fila, timeout, cancelamento, retry, stale callback, disconnect, capabilities, matcher, cópia defensiva, logs e estados;
- instrumentados são mínimos e justificados.

## Testes

- próprios desta task.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Ble*"
./gradlew app:connectedDebugAndroidTest
```

## Validação Final

- confirmar que nenhum teste depende de Champion C001 físico.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.
