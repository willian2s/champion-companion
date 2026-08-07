# Follow-up — divergência do provider Health Connect em 014-15

- **Origem:** validação física de `014-15`
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16, API 36
- **Aplicativo:** `1.0-dev`, working tree pós-`014-17`, sem commit criado;
  build instalado via `./gradlew installDebug`
- **Estado:** resolvido para os cenários físicos exercitados; limites residuais
  permanecem documentados em `014-15`

## Fato observado

Após sincronização explícita de atividade do relógio, o Health Connect real não
exibiu os dados esperados de distância e velocidade. A permissão de velocidade
apareceu como não permitida.

A tela do Champion informou sucesso local e passou a atividade para `SYNCED`, mas
essa mensagem não prova existência dos records no provider: a integração é
write-only e não faz leitura posterior de confirmação.

## Impacto histórico

Gate F ficou `FAIL` bloqueador na tentativa inicial. Não foi alterado código de
produção naquela validação. O bloqueio foi resolvido pelo follow-up `014-17` e
pela revalidação física posterior.

## Follow-up funcional registrado

1. Estado de `WRITE_DISTANCE` e `WRITE_SPEED` foi reproduzido e registrado no
   provider real para o package debug correto.
2. A versão instalada do Health Connect exibiu `DistanceRecord` e `SpeedRecord`.
3. A UI diferenciou permissão ausente, falha de distância, falha de velocidade e
   sucesso somente local da fila.
4. A projeção remota continua sem confirmação por leitura no aplicativo; a
   observação manual do provider ficou restrita à evidência física.
5. Distância, velocidade, retry e falha parcial foram revalidados nos cenários
   de ambas as permissões, revogação isolada de `WRITE_SPEED` e revogação
   isolada de `WRITE_DISTANCE`.

## Escopo preservado

Este follow-up não autoriza leitura ampla do Health Connect, alteração de
permissões, mudança de mapper, nova migration ou correção funcional dentro da
task `014-15`.

## Revalidação física após 014-17

- Galaxy S22 `SM-S901E`, Android 16, package debug
  `br.com.mwconsulting.championcompanion.dev`.
- Com `WRITE_DISTANCE` e `WRITE_SPEED` concedidas, uma atividade elegível foi
  sincronizada e o provider exibiu um `DistanceRecord` granular de `3,463 km` e
  um `SpeedRecord` de `7,245 km/h` da fonte `Champion Companion Dev`.
- Revogação isolada de `WRITE_SPEED` foi observada no provider; Settings exibiu
  `Velocidade — Pendente`. Reautorização pelo fluxo `Conectar` restaurou acesso.
- A primeira tentativa não tinha segunda atividade `WATCH_D3_D4` elegível não
  sincronizada; a corrida restante tinha duração zero e falhou fechada na
  materialização local. Essa limitação histórica foi superada pela matriz física
  posterior com dois treinos novos.
- O limite write-only permanece preservado; observação manual do provider não
  foi implementada como leitura no app.

## Falha parcial e retry físico

- Dois treinos novos foram importados; o de 15 minutos foi aceito como
  `Corrida`, e o de 20 minutos como `Treino livre`.
- Com `WRITE_SPEED` revogada, a corrida exibiu `Falha parcial por capacidade`,
  com falha nomeada no filho de velocidade, pendente e recuperável.
- Após restauração da permissão, retry explícito concluiu como `Sincronizada`.
- Observação manual do provider mostrou `DistanceRecord` acumulado de `3,517 km`
  e um único `SpeedRecord` novo de `0,202 km/h` no intervalo `12:33–12:49`; não
  foi observada duplicação.
- Evidências: `014-17-health-connect-distance-after-retry.png` e
  `014-17-health-connect-speed-after-retry.png`.

## Revogação isolada de distância e retry

- `WRITE_DISTANCE` foi revogada no mesmo package debug. O treino livre de 20
  minutos exibiu falha parcial nomeada no filho `Distância`, com três operações
  recuperáveis.
- Após reautorização, retry terminou `Sincronizada`. O provider exibiu
  `DistanceRecord` acumulado de `3,570 km`, com acréscimo novo de `0,053 km` e
  sem duplicação observada.
- Evidência: `014-17-health-connect-distance-after-distance-retry.png`.

## Resultado consolidado

- Ambas as permissões foram concedidas e o provider exibiu `DistanceRecord` e
  `SpeedRecord`.
- Revogação isolada de cada permissão produziu falha parcial nomeada e retry
  idempotente após reautorização, sem duplicação observada.
- Gate F de `014-15` passa para os cenários exercitados. A integração continua
  write-only; o aplicativo não executou `readRecords`, `ChangeLogs` ou leitura
  ampla para confirmar records.
