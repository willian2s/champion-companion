# Follow-up — divergência do provider Health Connect em 014-15

- **Origem:** validação física de `014-15`
- **Dispositivo:** Galaxy S22 `SM-S901E`, Android 16, API 36
- **Aplicativo:** `1.0-dev`, commit `0286c79`
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
