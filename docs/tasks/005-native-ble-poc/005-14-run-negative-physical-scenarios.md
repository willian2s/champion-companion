# Task 005-14 — Executar Cenários Negativos Físicos

## Status

Planejada.

## Objetivo

Validar comportamento físico da PoC em cenários negativos antes do gate final de estabilidade.

## Contexto

Fluxo feliz não basta. Fase 05 precisa confirmar que permissões, Bluetooth, cancelamento, alcance e reconnect curto não deixam fila presa, estado antigo ou necessidade de reiniciar app.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/tasks/005-native-ble-poc/005-13-resolve-physical-findings-and-update-protocol-docs.md`

## Dependências

- `005-13` concluída.

## Pré-Condições

- Galaxy S22 conectado por ADB;
- Champion C001 disponível e carregado;
- build debug instalado;
- Bluetooth habilitado.

## Entradas

- tela técnica pronta;
- relatório sanitizado.

## Arquivos Exatos Permitidos

- `docs/tasks/005-native-ble-poc/005-14-run-negative-physical-scenarios.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- concluir task com fakes;
- alterar roadmap e ADRs.

## Escopo

- permissões negadas;
- Bluetooth desligado;
- cancelamento de scan;
- relógio fora de alcance;
- disconnect durante BPM;
- nova conexão após disconnect;
- duas execuções seguidas sem reiniciar app.

## Fora Do Escopo

- gate de dez ciclos consecutivos.

## Passos

1. Executar cada cenário negativo previsto na spec.
2. Registrar comportamento observado, relatório sanitizado e necessidade de correção.
3. Confirmar ausência de fila presa, timeout residual, callback antigo e crash.
4. Atualizar `Status` e overview.

## Regras De Segurança

- se hardware não estiver disponível, registrar bloqueio;
- não mascarar falha com limpeza manual fora do fluxo da PoC.

## Critérios De Aceite

- cenários negativos executados e documentados;
- app retorna a estado seguro após cada cenário;
- novo ciclo pode começar sem reiniciar app quando aplicável.

## Testes

- validação física manual.

## Comandos De Validação

```bash
adb devices
git status --short
```

## Evidências Esperadas

- relatório sanitizado por cenário relevante;
- lista de falhas remanescentes ou confirmação de estado seguro.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- confirmar explicitamente se task ficou concluída ou bloqueada por hardware.
