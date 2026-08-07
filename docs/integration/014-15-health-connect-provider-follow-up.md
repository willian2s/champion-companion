# Follow-up — divergência do provider Health Connect em 014-15

- **Origem:** validação física de `014-15`
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16, API 36
- **Aplicativo:** `1.0-dev`, working tree pós-`014-17`, sem commit criado;
  build instalado via `./gradlew installDebug`
- **Estado:** bloqueador funcional para conclusão do Gate F

## Fato observado

Após sincronização explícita de atividade do relógio, o Health Connect real não
exibiu os dados esperados de distância e velocidade. A permissão de velocidade
apareceu como não permitida.

A tela do Champion informou sucesso local e passou a atividade para `SYNCED`, mas
essa mensagem não prova existência dos records no provider: a integração é
write-only e não faz leitura posterior de confirmação.

## Impacto

Gate F fica `FAIL` bloqueador. Não é permitido marcar `014-15` como concluída.
Não foi alterado código de produção nesta validação.

## Follow-up funcional necessário

1. Reproduzir e registrar estado de `WRITE_DISTANCE` e `WRITE_SPEED` no provider
   real.
2. Confirmar se `SpeedRecord` é aceito pela versão do Health Connect instalada.
3. Diferenciar na UI permissão ausente, falha de distância, falha de velocidade
   e sucesso somente local da fila.
4. Evitar declarar projeção remota concluída sem evidência operacional por filho.
5. Revalidar distância, velocidade, retry e falha parcial após correção aprovada.

## Escopo preservado

Este follow-up não autoriza leitura ampla do Health Connect, alteração de
permissões, mudança de mapper, nova migration ou correção funcional dentro da
task `014-15`.

## Revalidação física após 014-17

- Galaxy S22 `SM-S901E`, Android 16, package debug
  `br.com.mwconsulting.championcompanion.dev`.
- Com `WRITE_DISTANCE` e `WRITE_SPEED` concedidas, uma atividade elegível foi
  sincronizada e o provider exibiu distância granular `3,463 km` e velocidade
  `7,245 km/h` da fonte `Champion Companion Dev`.
- Revogação isolada de `WRITE_SPEED` foi observada no provider; Settings exibiu
  `Velocidade — Pendente`. Reautorização pelo fluxo `Conectar` restaurou acesso.
- Falha parcial e retry idempotente permanecem sem evidência: não havia segunda
  atividade `WATCH_D3_D4` elegível não sincronizada. A corrida restante tinha
  duração zero e falhou fechada na materialização local.
- O limite write-only permanece preservado; observação manual do provider não
  foi implementada como leitura no app.

## Falha parcial e retry físico

- Dois treinos novos foram importados; o de 15 minutos foi aceito como
  `Corrida`, e o de 20 minutos como `Treino livre`.
- Com `WRITE_SPEED` revogada, a corrida exibiu `Falha parcial por capacidade`,
  com um filho de velocidade pendente e recuperável.
- Após restauração da permissão, retry explícito concluiu como `Sincronizada`.
- Observação manual do provider mostrou distância acumulada `3,517 km` e uma
  única velocidade nova `0,202 km/h` no intervalo `12:33–12:49`; nenhum
  duplicado observado.
- Evidências: `014-17-health-connect-distance-after-retry.png` e
  `014-17-health-connect-speed-after-retry.png`.

## Revogação isolada de distância e retry

- `WRITE_DISTANCE` foi revogada no mesmo package debug. O treino livre de 20
  minutos exibiu falha parcial nomeada no filho `Distância`, com três operações
  recuperáveis.
- Após reautorização, retry terminou `Sincronizada`. O provider exibiu
  `3,570 km` acumulados, com acréscimo novo de `0,053 km` e sem duplicação
  observada.
- Evidência: `014-17-health-connect-distance-after-distance-retry.png`.
