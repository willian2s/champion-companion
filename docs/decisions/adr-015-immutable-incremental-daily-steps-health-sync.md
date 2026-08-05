# ADR-015 — Passos diários incrementais e imutáveis no Health Connect

## Status

Aceita e revisada. Task 016 concluída.
**Correção pós-conclusão** (`387945a`): elegibilidade de passos liberada para
operações sem `dailyStepsPayload` (legado cumulativo).

## Contexto

`DailySummary` é agregado local por dispositivo, origem e data. O sync anterior
reutiliza um `clientRecordId` cumulativo e atualiza o mesmo `StepsRecord` a cada
leitura. A experiência experimental precisa testar registros incrementais sem
alterar o armazenamento local.

## Decisão

Para `DailySummary + STEPS`, cada aumento aceito gera operação própria na fila,
com payload congelado, intervalo contíguo e identidade determinística distinta.
O mapper transforma esse payload em `StepsRecord`; retry nunca reconstrói o
registro a partir do resumo atual.

Checkpoint, criação da operação e aceitação do maior resumo devem ser
transacionais. Igualdade, redução, zero e janela inválida não avançam estado.
Cada data local possui sequência independente.

Operações cumulativas legadas não coexistem com incrementos no mesmo dia. A
ponte deve preservar identidade legada, não ler nem excluir dados remotos e não
converter históricos antigos automaticamente.

## Consequências

- `DailySummary`, UI, histórico, distância e calorias locais permanecem
  cumulativos.
- A fila pode crescer por leitura positiva; compactação fica fora do escopo.
- Payload técnico exige evolução persistida e migration compatível.
- Provider pode ter comportamento diferente para intervalos incrementais; gate
  físico continua necessário.

## Fora do escopo

BPM, exercícios, peso, altura, parser BLE, leitura/limpeza remota, rota,
`WorkManager`, nova entidade de domínio de deltas e alteração de `DailySummary`.

## Relação com ADR-012

Esta decisão substitui somente a política de passos diários que previa um
`StepsRecord` cumulativo por resumo. Write-only, local-first, granularidade de
atividades, demais records e ausência de limpeza remota permanecem vigentes.
