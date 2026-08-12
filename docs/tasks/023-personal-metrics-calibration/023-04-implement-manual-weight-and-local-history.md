# 023-04 — Implementar peso manual e histórico local

- **Status:** `completed`
- **Depends on:** `023-03`

## Objetivo

Concluir a parte de peso que hoje existe apenas como persistência/leitura parcial,
permitindo registrar novas medições e consultar histórico local sem criar uma
segunda infraestrutura de Health Connect.

## Persistência e domínio

Reutilizar:

- `WeightMeasurementEntity`;
- `WeightMeasurement`;
- `WeightUnit`;
- `PersonalMetricsRepository`;
- fila Health Connect já existente para `WeightRecord`.

Adicionar somente as APIs ausentes para:

- salvar medição manual;
- observar/listar histórico de peso em ordem temporal;
- manter último peso derivado do histórico persistido;
- resolver conflitos/erros de storage com estado UI seguro.

V1 da Fase 16 usa quilogramas na UI. O enum existente não obriga criar preferência
de unidade.

## UI

A tela Saúde > Peso deve deixar de ser placeholder e oferecer:

- último peso;
- CTA para adicionar nova medição;
- entrada decimal em kg;
- data/hora atual como padrão;
- lista cronológica de medições;
- origem manual;
- estado vazio;
- feedback de persistência/erro.

Gráfico é opcional. Não criar gráfico se isso aumentar escopo sem necessidade.

## Health Connect

Após persistência local bem-sucedida:

- reutilizar a preparação/enqueue de `WeightRecord` existente;
- metadata continua manual;
- falha remota não remove a medição local;
- retry continua pelo mecanismo foreground existente;
- não realizar readback.

## Fora do escopo

- meta de peso;
- IMC;
- composição corporal;
- edição retroativa complexa;
- importação de balança;
- leitura Health Connect.

## Checklist

- [x] API de save manual criada
- [x] histórico observável criado
- [x] último peso deriva de persistência real
- [x] UI permite registrar kg
- [x] histórico aparece após restart — implementação persistente concluída; validação física permanece pendente em `023-07`
- [x] falha HC preserva sucesso local
- [x] fila existente é reutilizada
- [x] tela não promete gráfico ausente
- [x] spec/overview atualizados

## Evidências

- `RoomRepositories` valida valor positivo/finito, resolve perfil atual, cria
  `WeightMeasurement` em `KILOGRAMS`/`MANUAL_ENTRY`, persiste medição e garante
  operação `HealthSyncAggregate.WeightMeasurement` na transação local.
- `observeWeightHistory` acompanha perfil atual e retorna histórico descendente;
  `listWeightHistory` fornece leitura pontual equivalente.
- `HealthViewModel` observa histórico, combina estado, trata intents do editor,
  salva localmente e aciona `triggerHealthConnectSyncIfEnabled` após sucesso.
- `WeightDetailScreen` oferece formulário kg, CTA, estados vazio/erro/sucesso e
  histórico cronológico, sem gráfico ou placeholder.
- Revisão independente corrigida: `saveManualWeightMeasurement` lança exceção
  dedicada quando o enqueue falha, permitindo rollback da medição na transação;
  falha remota posterior não remove persistência local. Histórico converte
  `POUNDS` para kg antes de renderizar, e `HealthViewModel` protege o reset de
  `isSavingWeight` com `finally`, preservando sucesso local se o trigger de
  Health Connect falhar.
- Validações: `./gradlew app:compileDebugKotlin` passou; `./gradlew app:lintDebug`
  passou; `git diff --check` passou.

## Pendências e riscos

- Persistência após restart e Health Connect indisponível ainda exigem validação
  física da `023-07`; nenhuma suite de testes foi criada ou executada.

## Aceite

Registrar pelo menos duas medições, reiniciar o app e confirmar ordem/histórico.
Se Health Connect estiver indisponível, os dois registros locais continuam
visíveis.
