# Task 012-13 - Executar Gate C físico de Health Connect

## Status

Planejada.

## Objetivo

Executar a validação física obrigatória da Fase 12 no Galaxy S22 com Health Connect real, cobrindo disponibilidade, permissões, passos, BPM, D1, atividades, falha parcial, retry, recuperação e privacidade.

## Contexto

O design da fase depende de comportamento real de provider, permissões, `clientRecordId`, `clientRecordVersion` e falha parcial. Essa validação não pode ser substituída por leitura de código.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- tasks `012-03` a `012-12`

## Dependências

- `012-12`.

## Entradas

- build debug instalável;
- provider Health Connect real no Galaxy S22;
- backlog local e UI da fase já concluídos.

## Arquivos permitidos

- `docs/specs/012-native-health-connect.md`
- `docs/tasks/012-native-health-connect/012-00-overview.md`
- `docs/tasks/012-native-health-connect/012-13-run-physical-health-connect-gate-c.md`

## Arquivos proibidos

- `app/src/test/**`
- `app/src/androidTest/**`
- criação de novo código funcional sem task anterior específica

## Escopo

- disponibilidade;
- permissões parciais;
- passos;
- update sem duplicação;
- BPM manual;
- backlog D1;
- atividade explícita;
- atividade calibrada;
- ausência de `Steps` duplicado;
- ausência de rota;
- falha parcial;
- retry;
- preferência pausada;
- recuperação após processo encerrado;
- peso e altura quando elegíveis;
- privacidade.

## Fora do escopo

- testes automatizados;
- leitura de dados de terceiros;
- `WorkManager`.

## Passos

1. Executar cenário de disponibilidade e provider.
2. Executar cenário de permissões parciais.
3. Executar passos com update idempotente.
4. Executar BPM manual.
5. Executar backlog D1.
6. Executar atividade explícita e calibrada.
7. Executar falha parcial e retry.
8. Executar preferência pausada.
9. Executar recuperação após morte de processo.
10. Executar cenários de peso e altura, quando elegíveis.
11. Registrar privacidade e screenshots.

## Critérios de aceite

- todos os cenários do Gate C ficam registrados com resultado obtido;
- duplicação indevida não ocorre;
- ausência de rota e ausência de `Steps` de atividade ficam confirmadas;
- falha parcial é retomável sem apagar sessão sincronizada;
- privacidade operacional permanece preservada.

## Validação manual

- validação física integral no dispositivo real.

## Comandos de build

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

- checklist preenchido do Gate C;
- screenshots das telas alteradas;
- registro textual dos resultados por cenário.

## Screenshot quando visual

- screenshot obrigatório.

## Atualização do overview

- marcar `012-13` como concluída;
- registrar resultado do Gate C e correções de follow-up, se existirem.

## Validação final

- Não criar, alterar ou executar testes automatizados.
- Task executável em sessão nova, sem depender de contexto anterior.
