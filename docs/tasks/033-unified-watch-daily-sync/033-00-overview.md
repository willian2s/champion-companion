# 033 — Overview: sincronização unificada e dados diários granulares

- **Spec:** `docs/specs/033-unified-watch-daily-sync.md`
- **ADR:** `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`
- **Status:** `in_progress`
- **Progresso:** `5/9`
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
       ✓             |
       +------+------+
              v
033-07 Produto/UI e remoção de entrypoints BLE
              |
033-08 Read models, legacy e compatibilidade
              |
033-09 Validação física e fechamento
```

## Checklist

- [x] `033-01-auditar-e-congelar-d1-v2-e-cutover.md`
- [x] `033-02-implementar-parser-collector-d1-v2.md`
- [x] `033-03-persistir-fatos-diarios-granulares.md`
- [x] `033-04-compor-dados-diarios-fea1-d1-d4.md`
- [x] `033-05-unificar-watch-sync-na-home.md`
- [ ] `033-06-integrar-health-connect-granular-e-cutover.md`
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
- cutover HC ocorre por dia local inteiro e não pode assumir `D1 + D4` como
  fontes mutuamente exclusivas;
- step length sai da UI, mas contratos legacy/fallback permanecem;
- medição manual de FC e importação D1 saem da Saúde como ações de produto;
- importação D3/D4 sai de Exercícios como ação de produto;
- semântica Health Connect de calorias permanece bloqueada até evidência.

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

A 033-06 absorve formalmente essa correção. Não será criada task separada.

Também fica proibido fabricar:

```text
FEA1 - ΣD1
```

como bucket, intervalo ou `StepsRecord`. FEA1 não informa a distribuição temporal
da cauda; somente novo D1 real pode materializá-la.

Evidência física sanitizada:

- `docs/integration/033-03-d1-v2-physical-validation.md`;
- `docs/integration/033-05-unified-watch-sync-physical-validation.md`.

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

## Próxima execução

A 033-06 deve, além do cutover/granular Health Connect:

- refinar a classificação `WatchDailyStepCoverage`/equivalente;
- distinguir `COMPLETE`, cauda recente, incompletude estrutural e divergência;
- nunca sintetizar residual FEA1-D1;
- preservar IDs/payloads já congelados quando a cauda for materializada em D1
  posterior;
- falhar fechado para projeções que dependam de reconciliação ainda não provada.

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
