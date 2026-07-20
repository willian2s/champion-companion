# Task 003-06 — Modelar Calibração de Esteira

## Status

Pendente.

## Dependência

- `003-05` concluída.

## Objetivo

Modelar TreadmillCalibration e aplicar revisão auditável sem apagar métricas de origem.

## Pode modificar

- Modelos/regras de calibração em `core/model/**`.
- Testes JVM correspondentes.
- Este arquivo.

## Passos

1. Registrar atividade, revisão, distâncias original, estimada, informada e calibrada, data, origem, fator, motivo de ausência e métricas derivadas/proveniência.
2. Para importação com distância original positiva: fator = informada/original.
3. Para sessão de esteira sem estimativa positiva: aceitar distância calibrada; fator ausente com motivo explícito. Nunca usar `1.0` inventado.
4. Recalcular ritmo/velocidade com proveniência, sem destruir valores importados; Health Connect futuro consumirá somente distância calibrada.
5. Testar fator, divisões inválidas, ausência explícita, preservação de origem e transição para pronta.

## Aceite

- Histórico separa as quatro distâncias.
- Calibração não cria valores ou fatores artificiais.
