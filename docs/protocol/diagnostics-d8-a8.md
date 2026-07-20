# Diagnostics D8/A8 - Champion C001

## Objetivo

Separar comandos diagnosticos/fallback D8 e A8 do fluxo principal, evitando tratar inferencias como fatos.

## Fontes

- `../motionlog-app/src/lib/ble/champion-commands.ts`
- `../motionlog-app/src/lib/ble/champion-client.ts`
- `../motionlog-app/docs/integrations/champion-c001-ble-protocol.html`
- `../motionlog-app/docs/specs/003-ble-foreground.md`
- Busca por `D8`, `A8`, `SPORT_CURRENT`, `0xd8`, `0xa8` em `../motionlog-app/src`, `../motionlog-app/docs` e modulo Android background.

## A8

| Campo | Valor |
| --- | --- |
| Nome no codigo | `SPORT_CURRENT` |
| Payload | `A8 00` |
| Characteristic provavel | F008 write |
| Rotulo no prototipo | `A8 esporte atual` |
| Parser no app atual | nao encontrado |
| Fluxo funcional no app atual | nao encontrado |
| Classificacao | `Confirmado pelo codigo` como constante; resposta e utilidade `Ainda nao confirmado` |

Evidencias:

- `champion-commands.ts` define `SPORT_CURRENT: [0xa8, 0x00]`.
- Prototipo HTML contem botao/rotulo de leitura de esporte atual.
- Nao ha chamada encontrada para `SPORT_CURRENT` no app runtime.
- Nao ha parser especifico para resposta A8.

Uso recomendado no Champion Companion:

- Manter A8 como diagnostico ou fallback ate validacao fisica.
- Nao usar A8 como fonte primaria de atividades enquanto D3/D4 forem o fluxo confirmado pelo codigo.
- Registrar raw hex se executado em modo diagnostico futuro.

## D8

| Campo | Valor |
| --- | --- |
| Payload | nao encontrado |
| Characteristic | nao confirmada |
| Parser | nao encontrado |
| Fluxo funcional | nao encontrado |
| Classificacao | `Ainda nao confirmado` |

Resultado da analise:

- Nenhuma constante D8 foi encontrada em `src/lib/ble`.
- Nenhum parser D8 foi encontrado.
- Nenhuma chamada D8 foi encontrada no modulo Android background.
- Nenhuma evidencia de payload D8 foi localizada nos docs MotionLog lidos nesta task.

Uso recomendado no Champion Companion:

- Nao implementar D8 no fluxo principal.
- Reservar item em tela/log de diagnostico apenas se surgir evidencia fisica ou documental posterior.
- Nao documentar payload D8 como fato ate captura ou codigo aparecer.

## Relacao Com FEA2/FEC8

FEA2 e FEC8 aparecem como indications opcionais no prototipo e spec MotionLog. Nao ha ligacao confirmada entre D8/A8 e estas characteristics.

Classificacao: `Ainda nao confirmado`.

## Lacunas Para `000-14`

- Confirmar se D8 existe no Champion C001 e qual payload aciona resposta.
- Capturar resposta A8 real com dispositivo fisico.
- Decidir se modo diagnostico futuro deve permitir envio manual de A8/D8 com aviso e log raw.
- Confirmar se A8 tem valor para recuperar atividade atual ou apenas estado temporario nao necessario ao MVP.
