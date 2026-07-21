# Task 004-03 — Preparar Permissões e Pré-Requisitos De Bluetooth

## Status

Pendente.

## Objetivo

Modelar permissões aplicáveis, checagem de suporte BLE e estados de prontidão sem criar UI de solicitação e sem alterar manifest nesta fase.

## Contexto

Fase 04 precisa impedir operações ilegais antes do scan/conexão, mas não deve assumir experiência visual nem loops de permissão.

## Documentos Obrigatórios

- `docs/specs/004-native-ble-core.md`
- `docs/specs/_inputs/phase-04-native-ble-core-direction.md`
- `docs/protocol/ble-transport.md`
- `docs/core/current-project-baseline.md`

## Dependências

- `004-02` concluída.

## Entradas

- SDK alvo 36;
- `AndroidManifest.xml` atual;
- contratos BLE criados em `004-02`.

## Arquivos Exatos Permitidos

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/ChampionBlePrerequisites.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidBleEnvironment.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/bluetooth/android/AndroidPermissionSnapshot.kt`
- este arquivo

## Arquivos Que Não Podem Ser Modificados

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `feature/**`

## Escopo

- mapear permissões por SDK;
- verificar suporte BLE, adapter e estado ligado/desligado;
- expor resultado tipado e seguro para scanner/manager.

## Fora Do Escopo

- pedir permissões ao usuário;
- adicionar permissões ao manifest;
- criar textos de UI;
- iniciar scan ou conexão.

## Regras De Segurança

- sem side effects de sistema;
- sem dependência de Compose;
- sem loops automáticos de permissão.

## Passos De Implementação

1. Criar abstração estreita para ler suporte BLE, estado do adapter e permissões correntes.
2. Modelar snapshot de permissões exigidas por operação e SDK.
3. Criar avaliador de pré-requisitos reutilizável por scan e conexão.
4. Garantir mensagens/códigos seguros sem detalhe sensível.
5. Atualizar `Status` deste arquivo.

## Critérios De Aceite

- núcleo consegue dizer por que scan/conexão não podem iniciar;
- manifest permanece intacto;
- nenhum componente BLE tenta pedir permissão;
- comportamento pode ser testado em JVM com doubles.

## Testes

- JVM para mapeamento de permissões por SDK e combinações de estados;
- instrumentado só se wrapper Android exigir confirmação real.

## Comandos De Validação

```bash
./gradlew app:testDebugUnitTest --tests "*Prerequisite*"
```

## Validação Final

- confirmar que `AndroidManifest.xml` não mudou.

## Atualização Documental Esperada

- `Status` deste arquivo concluído.
