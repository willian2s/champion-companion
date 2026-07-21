# Task 005-10 — Criar Relatório Sanitizado E Fixtures Da PoC

## Status

Concluída em 2026-07-21.

## Objetivo

Criar evidência por ciclo, exportação sanitizada e fixtures sintéticas/sanitizadas para testes e comparação segura.

## Contexto

PoC precisa provar sequência, duração, capabilities, limpeza e resultado de cada ciclo sem vazar payload, bytes, MAC, passos exatos ou BPM exato.

## Documentos Obrigatórios

- `docs/tasks/005-native-ble-poc/005-00-overview.md`
- `docs/specs/005-native-ble-poc.md`
- `docs/decisions/adr-004-operational-privacy-guards.md`

## Dependências

- `005-08` concluída.

## Pré-Condições

- estado da PoC e sessão técnica já expõem eventos/contagens necessários.

## Entradas

- estados da PoC;
- eventos técnicos da sessão.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/**`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/**`, se contrato técnico comum for necessário
- `app/src/test/java/br/com/mwconsulting/championcompanion/**`
- `app/src/androidTest/**`, se compartilhamento seguro exigir
- `docs/tasks/005-native-ble-poc/005-10-create-sanitized-cycle-report-and-fixtures.md`
- `docs/tasks/005-native-ble-poc/005-00-overview.md`, somente para checklist

## Arquivos Proibidos

- `core/database/**`
- `data/**`
- logs públicos permanentes fora de cache/privado

## Escopo

- modelo de evidência em memória;
- export seguro e sanitizado;
- fixtures sintéticas para A1, FEA1, D0 e F002;
- testes de sanitização.

## Fora Do Escopo

- persistência em Room;
- retenção longa;
- captura de payload real bruto versionado.

## Passos

1. Definir modelo de evidência sanitizada por ciclo.
2. Implementar montagem de relatório com contagens, estados, durações e categorias de erro.
3. Implementar export em diretório privado/cache privado e compartilhamento explícito seguro.
4. Criar fixtures sintéticas ou sanitizadas sem dados reais de saúde.
5. Cobrir sanitização e estrutura com testes.
6. Atualizar `Status` e overview.

## Regras De Segurança

- não incluir MAC, payload, hex, bytes, senha, passos exatos, BPM exato ou stack trace;
- não gravar sinal F002 bruto;
- não criar log público permanente.

## Critérios De Aceite

- relatório prova sequência e resultado do ciclo sem vazar dados sensíveis;
- export só ocorre por ação explícita;
- fixtures versionadas são sintéticas ou sanitizadas.

## Testes

- JVM: sanitização, campos obrigatórios e proibidos.
- instrumentado: compartilhamento/export seguro, se necessário.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Report*"
./gradlew app:connectedDebugAndroidTest
```

## Evidências Esperadas

- `DevicePocCycleReport` produz JSON versionado com ciclo, horários, duração,
  sequência de estados, capabilities, contagens e categorias de erro;
- relatório não inclui identificador de transporte, payload, bytes, hex,
  stack trace, passos exatos ou BPM exato;
- `DevicePocReportExporter` grava somente em `cacheDir/poc-reports/` após ação
  explícita de exportação na tela debug;
- fixtures sintéticas `A1`, `FEA1`, `D0` e `F002` documentam apenas observação e
  contagem, sem captura de dispositivo ou dado de saúde.

## Atualização Documental

- atualizar `Status`;
- marcar checklist de `005-00`.

## Validação Final

- testes JVM de relatório cobrem campos obrigatórios, termos proibidos e
  cobertura das quatro fixtures sintéticas;
- nenhuma fixture ou relatório contém payload ou dado real de saúde.
