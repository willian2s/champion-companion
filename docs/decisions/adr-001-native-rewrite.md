# ADR-001 — Reimplementar o aplicativo em Android nativo

## Status

Aceita

## Contexto

O aplicativo depende fortemente de BLE, Health Connect, serviços em
segundo plano, localização e notificações.

A implementação atual utiliza React Native e Expo, adicionando uma camada
intermediária sobre APIs Android críticas para o funcionamento do produto.

## Decisão

Criar um novo aplicativo chamado Champion Companion utilizando:

- Kotlin
- Jetpack Compose
- Room
- Hilt
- APIs nativas de Bluetooth LE
- Health Connect SDK
- Foreground Services
- WorkManager

O MotionLog será mantido temporariamente como baseline funcional.

## Consequências

### Positivas

- controle direto sobre BluetoothGatt;
- melhor gerenciamento de background;
- integração direta com Health Connect;
- menor dependência de bridges;
- diagnóstico BLE mais detalhado.

### Negativas

- reimplementação completa;
- manutenção temporária de dois aplicativos;
- necessidade de portar parsers e regras;
- Android passa a ser a única plataforma suportada.

## Alternativas consideradas

- continuar com React Native;
- criar somente módulos nativos dentro do MotionLog;
- reescrever apenas a camada BLE.

## Resultado

O Champion Companion será criado em um repositório separado.
