# Task 003-03 — Modelar Dispositivo, Perfil e Resumo Diário

## Status

Concluída.

## Dependência

- `003-02` concluída.

## Objetivo

Modelar agregados Device, UserProfile, WeightMeasurement e DailySummary com identidade e deduplicação explícitas.

## Pode modificar

- Modelos e regras destes agregados em `core/model/**`.
- Testes JVM destes agregados.
- Este arquivo.

## Passos

1. Modelar Device persistente: ID interno, identificador permitido pela plataforma único, nome, modelo C001, firmware, associação, última conexão e metadados de importação; excluir estados BLE efêmeros.
2. Modelar perfil e peso somente no escopo da spec; preservar unidades, instante e proveniência sem conta remota/dados clínicos extras.
3. Modelar DailySummary: data local, passos, distância/calorias estimadas, origem, coleta, atualização, validade e dispositivo aplicável.
4. Definir chave natural `dispositivo + origem + data local`, comportamento de atualização e validações de valores.
5. Testar deduplicação conceitual, atualização idempotente, data local e ausência de device quando a origem permitir.

## Aceite

- Nenhum modelo contém estado BLE transitório, Room ou UI.
- Resumo diário atualiza mesma chave natural sem duplicar.
