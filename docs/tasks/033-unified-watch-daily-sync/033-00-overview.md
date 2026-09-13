# 033 — Overview: sincronização unificada e dados diários granulares

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADR:** `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`
- **Status:** `in_progress`
- **Progresso:** `6/9`
- **Branch de integração:** `feat/033-unified-watch-daily-sync`

## Objetivo

Unificar FEA1, D1 e D3/D4 em um único watch sync iniciado pela Home, promover D1
completo para fatos diários granulares, compor fontes por finalidade/métrica sem
dupla contagem e fazer cutover seguro da projeção Health Connect diária sem
reescrever histórico.

## Sequência

```text
033-01 Auditoria e contratos                         ✓
   |\
   | +--> 033-02 Parser/collector D1 v2             ✓
   | +--> 033-03 Persistência D1 / Room v18         ✓
   |          |
   +----------+
              v
033-04 Composição diária FEA1 + D1 + D4             ✓
              |
       +------+------+
       |             |
       v             v
033-05 Watch sync   033-06 HC cutover/granular
        ✓             ✓*
       +------+------+
              v
033-07 Produto/UI e remoção de entrypoints BLE
              |
033-08 Read models, legacy e compatibilidade
              |
033-09 Validação física e fechamento
```

`033-06` está implementada, mas recebeu correções pós-validação física em
instalação limpa. Essas correções ainda exigem novo checkpoint físico antes da
033-07.

## Checklist

- [x] `033-01-auditar-e-congelar-d1-v2-e-cutover.md`
- [x] `033-02-implementar-parser-collector-d1-v2.md`
- [x] `033-03-persistir-fatos-diarios-granulares.md`
- [x] `033-04-compor-dados-diarios-fea1-d1-d4.md`
- [x] `033-05-unificar-watch-sync-na-home.md`
- [x] `033-06-integrar-health-connect-granular-e-cutover.md` — implementação + correção pós-validação; revalidação pendente
- [ ] `033-07-remover-entrypoints-ble-e-calibracao-passo-da-ui.md`
- [ ] `033-08-integrar-read-models-legacy-e-compatibilidade.md`
- [ ] `033-09-validar-fisicamente-e-fechar.md`

## Decisões congeladas

- Home pull-to-refresh é o único entrypoint de produto para leitura do relógio;
- FEA1 = total diário autoritativo de passos/validação;
- D1 = linha granular diária do relógio e pode conter fatos ocorridos durante
  workouts;
- D4 = fonte detalhada/canônica do workout;
- D1 v2 preserva passos, distância, calorias, sportValue e raw adicionais mesmo
  com BPM inválido;
- BPM D1 v2 válido em 30..200;
- a regra antiga "bucket D1 intersecta D4 => suprimir bucket inteiro" foi
  rejeitada após validação física;
- passos diários usam FEA1 como total e D1 como distribuição; D4 não é somado
  novamente ao agregado diário;
- distância/calorias diárias usam D1 nativo quando elegível; D4 permanece no
  contexto do workout e só substitui contribuição diária com prova de cobertura;
- FC combinada pode preferir D4 durante workout quando houver cobertura D4 válida;
- sync do relógio não executa Health Connect automaticamente;
- a data de cutover HC pode ser persistida, mas **não pode desligar o caminho
  remoto legacy de STEPS enquanto D1 granular continuar sem substituto remoto
  comprovadamente seguro**;
- step length sai da UI, mas contratos legacy/fallback permanecem;
- medição manual de FC e importação D1 saem da Saúde como ações de produto;
- importação D3/D4 sai de Exercícios como ação de produto;
- semântica Health Connect de calorias permanece bloqueada até evidência;
- `Configurações -> Sincronizar agora` inclui atividades Watch `READY_TO_SYNC`,
  preservando review-before-sync e sem tocar `PENDING_REVIEW`/`PENDING_CALIBRATION`.

## Correção física pós-033-05: cauda recente FEA1 × D1

Refreshes consecutivos da Home confirmaram que FEA1, D1 e D3/D4 são executados em
um único fluxo, D1/D4 permanecem idempotentes e o watch sync não escreve
Health Connect automaticamente.

No dump final foi observado:

```text
FEA1.steps = 4323
Σ D1.steps = 4226
D1         = 183 posições completas, sem lacunas
último D1  = 15:10
FEA1       = ~15:12:47
```

A diferença de `97` passos ocorreu com D1 estruturalmente completo. Portanto:

```text
FEA1 > ΣD1
+ D1 completo até totalPositions recebido
+ último bucket recente
=> RECENT_TAIL / LAGGING
=> não é divergência estrutural
```

Também fica proibido fabricar `FEA1 - ΣD1` como bucket, intervalo ou
`StepsRecord`. FEA1 não informa a distribuição temporal da cauda; somente novo
D1 real pode materializá-la.

## Resultado físico antes da 033-04

Dois dumps sucessivos confirmaram:

- D1 v2/Room v18 em dispositivo real;
- posições contínuas e reimportação idempotente;
- `Σ D1.steps == FEA1.steps` em duas leituras;
- crescimento incremental D1 igual ao crescimento FEA1;
- workouts D4 preservados durante reimportação D1;
- corrida em que D1 já continha os mesmos passos/distância do D4;
- outro workout em que overlap temporal não autorizava descartar o bucket D1.

## Resultado físico da 033-05

Refreshes físicos sucessivos confirmaram:

- Home atualiza FEA1 + D1 + D3/D4 em um único gesto;
- D1 não duplica samples em refresh idempotente;
- D3/D4 não recria workouts válidos já persistidos;
- um workout novo foi descoberto pelo fluxo unificado sem usar Saúde/Exercícios;
- fila Health Connect permaneceu sem efeito remoto automático;
- FEA1 pode avançar à frente do último bucket D1 sem lacuna estrutural.

Partial success forçado por falha de componente continua reservado para 033-09.

## Resultado e correção pós-validação da 033-06

A implementação inicial entregou Room v19, cutover persistido, `RECENT_TAIL`,
proibição de residual sintético e separação explícita entre watch sync e HC.
Validação física em instalação limpa revelou três gaps:

1. cutover no próprio dia desligava `DailySummary STEPS` enquanto D1 granular
   permanecia `deferred`, deixando o dia sem StepsRecord;
2. `Settings -> Sincronizar agora` não criava a primeira projeção de uma atividade
   Watch `READY_TO_SYNC`;
3. backlog antigo podia exigir vários cliques porque cada gesto processava somente
   um lote de 100 operações.

Correções aplicadas:

- a data de cutover solicitada continua persistida, mas o **cutover efetivo de
  STEPS fica adiado** enquanto a projeção D1 granular remota não estiver pronta;
- nesse período, FEA1/DailySummary continua usando o caminho incremental das
  Specs 016/027, incluindo créditos/ledgers de `WorkoutSteps`;
- `Sincronizar agora` drena a fila em passes finitos enquanto houver progresso e,
  em seguida, chama o sync explícito de cada Watch `READY_TO_SYNC`;
- `PENDING_REVIEW`, `PENDING_CALIBRATION`, `DISCARDED` e `SYNCED` não entram na
  criação inicial pelo gesto global;
- distância diária e calorias diárias continuam deliberadamente `deferred` por
  risco de sobreposição/semântica não comprovada.

A ordenação causal interna da fila não foi invertida por recência/estado. O gesto
global drena lotes sucessivos, evitando o starvation físico observado sem quebrar
cadeias incrementais de passos.

## Evidências físicas sanitizadas

- `docs/integration/033-03-d1-v2-physical-validation.md`;
- `docs/integration/033-05-unified-watch-sync-physical-validation.md`;
- `docs/integration/033-06-health-connect-clean-install-validation.md`.

## Observações

- D1 granular Health Connect continua fail-closed até prova de intervalo temporal
  e contabilidade segura com `WorkoutSteps`;
- a data de cutover persistida funciona como intenção/auditoria; sua aplicação a
  STEPS está adiada enquanto não houver substituto remoto seguro;
- a correção pós-validação da 033-06 ainda precisa de um novo dump físico antes de
  liberar a 033-07.

## Guard rails

- não criar testes automatizados sem pedido explícito;
- não introduzir GPS/rota/mapa de produção;
- não remover dados históricos de medição manual, D1, step length ou workout;
- não alterar atividades `SYNCED` silenciosamente;
- não criar efeitos Health Connect em migration/watch sync;
- não fabricar fatos diários antigos que nunca foram persistidos;
- não usar header total D4 como fonte de comportamento;
- não somar D1 + D4 ingenuamente nem suprimir D1 apenas por overlap;
- não fabricar bucket residual a partir de FEA1-D1;
- cada task atualiza este overview.
