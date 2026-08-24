# Evidência 027-06 — Health Connect, Health Sync e Strava

- **Data observada:** 24 de agosto de 2026
- **Dispositivo:** Samsung SM-S901E
- **Android:** 16
- **Aplicativo:** Champion Companion Dev, APK debug local instalada via `installDebug`
- **Status:** `go_with_adjustments`; atividade → Daily e Daily → atividade
  reproduzidos

## Atividade antes do Daily

Atividade `WALKING` importada do relógio:

- início observado: `05:44:58`;
- fim observado: `05:51:54`;
- duração: `06:56`;
- passos locais: `16`;
- parser: `VALID`;
- workflow local: `SYNCED`.

Evidência local sanitizada, obtida após o fluxo:

- `STEPS` da atividade: `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT`;
- `STEPS_CADENCE`: `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT`;
- ambas as operações tiveram duas tentativas, compatível com retry após a
  primeira execução física;
- crédito: `ACCEPTED`, depois `CONSUMED`, com `16` passos;
- Daily local posterior: `16` passos;
- checkpoint Daily: total efetivo `16`, janela encerrada às `05:51:54`;
- não foi observado segundo incremento Daily para esses `16` passos.

## Health Connect

Screenshots em `docs/tasks/027-workout-steps-cadence-1hz-timeline/antes-daily/`
mostram, antes do Daily:

- `StepsRecord`: `16 passos`, janela `05:44–05:51`;
- `StepsCadenceRecord`: série exibida como `2,308 passos/min` para a mesma
  janela, com buckets de zero visíveis.

O valor usa vírgula decimal da interface. `2,308 passos/min` é compatível com
`16` passos distribuídos pela janela de aproximadamente `6:56`; não foi
classificado como erro do Health Connect.

## Health Sync → Strava

Atividade apareceu no Strava como `Caminhada matinal`, hoje às `05:45`.
Campos visíveis na tela observada:

- distância: `0,01 km`;
- tempo de movimentação: `6:26`;
- calorias: `2 Kcal`;
- frequência cardíaca média: `111 bpm`;
- frequência cardíaca máxima: `143 bpm`.

Após rolagem da atividade, o Strava exibiu gráfico temporal de cadência:

- gráfico: presente;
- cadência média: `16 ppm`;
- cadência máxima: `16 ppm`;
- gráfico observado: série plana em `16 ppm`.

Passos totais não ficaram visíveis na tela do Strava observada; o total de `16`
passos foi confirmado no Health Connect e no estado local. A cadência exibida
no Strava (`16 ppm`) difere da representação observada no Health Connect
(`2,308 passos/min`); registrar como diferença observada entre destinos, sem
classificar Health Connect como incorreto ou inferir a causa.

## Classificação provisória

`go_with_adjustments`: integridade local/Health Connect do cenário
atividade → Daily permaneceu sem inflação; Strava preservou gráfico e métricas
de cadência, mas exibiu valor diferente (`16 ppm`) e não expôs o total de
passos na tela observada.

## Cenário Daily antes da atividade — reteste

O Daily foi atualizado antes da sincronização da nova atividade e passou de
`16` para `201` passos. A atividade seguinte foi importada e revisada como
`WALKING`, com `132` passos e janela `06:24:59–06:30:38`.

Estado local após a sincronização:

- crédito da atividade: `BLOCKED`;
- motivo: `ALREADY_COVERED`;
- `STEPS` da atividade: `PENDING`;
- `STEPS_CADENCE`: `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT`;
- `EXERCISE_SESSION`, `DISTANCE`, `SPEED`, `HEART_RATE` e calorias:
  `SYNCED`, checkpoint `AFTER_REMOTE_EFFECT`;
- atividade: `SYNCED`.

Health Sync publicou uma atividade correspondente no Strava, com `0,13 km`,
`5:15`, `7 Kcal`, `111 bpm` médio e `152 bpm` máximo. Nas telas observadas
dessa segunda atividade, a atividade apareceu no Strava; cadência foi confirmada
na tela pelo usuário durante a validação. O valor exato não foi preservado no
relatório sanitizado.

O cenário confirmou o bloqueio local de `StepsRecord` por cobertura Daily e a
independência efetiva de `StepsCadenceRecord`: passos ficaram bloqueados sem
crédito, enquanto cadência foi aceita remotamente.

**Resultado do reteste:** passou sem inflação. `StepsRecord` bloqueado,
`StepsCadenceRecord` aceito e demais filhos sincronizados.

## Fechamento físico do Gate A

- atividade → Daily: passou; crédito aceito e consumido uma vez, residual sem
  inflação;
- Daily → atividade: passou; Daily em `201`, atividade em `132`, passos
  bloqueados por `ALREADY_COVERED`, cadência aceita independentemente;
- retry: atividade anterior teve duas tentativas e terminou sincronizada sem
  segundo crédito;
- Health Sync → Strava: atividades apareceram; cadência foi observada no
  destino, com diferença de representação registrada acima;
- mudança de dia: não exercitada.

**Classificação final do Gate A:** `go_with_adjustments`.

## Privacidade

Não preservar endereço, identificadores de conta, IDs remotos, MAC, tokens ou
payloads. Screenshots originais permanecem somente como evidência local do
usuário; qualquer relatório deve omitir dados pessoais e localização.
