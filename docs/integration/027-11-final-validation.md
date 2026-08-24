# Evidência 027-11 — Validação final da Spec 027

- **Data observada:** 24 de agosto de 2026
- **Dispositivo:** Samsung SM-S901E
- **Android:** 16 / SDK 36
- **Aplicativo:** Champion Companion Dev, APK debug local instalada via
  `installDebug`
- **Status:** validação física concluída; fechamento da Spec bloqueado por
  achados de implementação no Gate B

## Evidência herdada do Gate A

`docs/integration/027-06-health-sync-strava-validation.md` permanece fonte da
validação física já concluída:

- atividade → Daily passou sem inflação, com crédito aceito e consumido uma vez;
- Daily → atividade bloqueou `StepsRecord` por `ALREADY_COVERED` e aceitou
  `StepsCadenceRecord` independentemente;
- retry não criou segundo crédito;
- Health Sync publicou as atividades no Strava e exibiu cadência;
- diferença de representação entre Health Connect e Strava foi registrada sem
  inferência;
- mudança de dia não foi exercitada.

Classificação do Gate A: `go_with_adjustments`.

## Tentativa inicial com as duas atividades do relógio

Fluxo explícito executado no app:

1. APK debug instalada no dispositivo;
2. `MainActivity` aberta;
3. Exercícios aberto;
4. ação **Buscar treinos no relógio** executada.

Resultado observado após aproximadamente 35 segundos:

- erro UI: `Não foi possível importar os treinos. Operação Bluetooth expirou.`;
- resumo UI: `Inseridos 0, duplicados 0, inválidos 0, ausentes 0`;
- nenhuma das duas atividades foi importada;
- nenhuma atividade foi sincronizada com Health Connect;
- nenhuma atividade foi enviada ao Health Sync/Strava nesta tentativa;
- atividades existentes não foram descartadas.

Após a tentativa, foi confirmado que o relógio estava descarregado. O timeout
Bluetooth é compatível com essa condição física.

O Health Connect não foi aberto nem usado nesta tentativa. Não houve readback,
consulta ao banco local, captura de payload ou coleta de identificadores.

## Reteste após recarga

Com o relógio ligado, o fluxo foi repetido sem nova busca posterior. A importação
concluiu com:

- inseridos: `2`;
- duplicados: `0`;
- inválidos: `0`;
- ausentes: `0`.

As duas atividades foram revisadas e sincronizadas individualmente:

| Atividade | Dados observados | Estado final |
| --- | --- | --- |
| Longa | `32:17`, `4.605` passos; intervalos em aproximadamente `137/161/162/131/54 spm` | `SYNCED` |
| Curta | `05:30`, `639` passos; cadência visível de `116 passos/min` | `SYNCED` |

O app exibiu `Status: Sincronizada` e `Escritas locais aceitas` para a longa.
Health Connect não solicitou autorização adicional e não foi lido; `SYNCED`
registra aceite local da escrita pelo fluxo explícito do app. Não foi executado
Health Sync/Strava para estas duas atividades, pois esse caminho já está
documentado no fechamento do Gate A em `027-06`.

## Classificação por área

| Área | Resultado | Evidência |
| --- | --- | --- |
| Integridade de passos | `go_with_adjustments`; duas atividades importadas e sincronizadas sem duplicados locais | `027-06` e reteste após recarga |
| Health Connect | duas escritas `SYNCED` localmente; sem readback | `027-06` e estados UI do reteste |
| Health Sync/Strava | Gate A aceito com diferença de representação | `027-06`; não repetido para estas duas atividades |
| Timeline local 1 Hz | validada localmente até `027-10` | detalhe local, gaps/provenance e screenshot sanitizado |

## Limitações e risco residual

Mudança de dia/reset não foi exercitada. Health Connect continua write-only:
`SYNCED` é aceite local da escrita, não readback do provider. O valor exato de
cadência da atividade longa foi observado somente nos intervalos exibidos.

A revisão independente final identificou três lacunas no contrato/implementação
da timeline 1 Hz, fora da correção operacional desta subtarefa:

- `deriveCadence1Hz()` falha a série inteira quando há intervalo estrutural
  inválido, em vez de preservar buckets ausentes e segmentos separados;
- `deriveBpm1Hz()` não exige `ParserValidity.VALID`;
- parâmetros BPM aceitam `fallbackGapSeconds > 90`, contrariando o limite
  versionado do contrato.

Esses achados bloqueiam o fechamento integral do Gate B e exigem correção
planejada sem alterar a evidência física do Gate A.

Não houve alteração de código, schema, permissões ou histórico remoto.
