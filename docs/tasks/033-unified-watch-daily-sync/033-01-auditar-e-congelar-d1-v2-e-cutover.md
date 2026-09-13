# 033-01 — Auditar e congelar D1 v2, watch sync e cutover

- **Status:** completed
- **Depende de:** nenhuma
- **Libera:** 033-02 e 033-03
- **Auditoria:** `docs/integration/033-01-d1-v2-watch-sync-and-health-cutover-audit.md`
- **Correção física posterior:** `docs/integration/033-03-d1-v2-physical-validation.md`

## Objetivo

Reauditar a `main` pós-032 e congelar contratos antes de alterar runtime, schema
ou Health Connect.

## Entregáveis

- [x] confirmar baseline pós-032 e Room v17;
- [x] auditar parser/collector D1 atual;
- [x] congelar layout alvo `d1-v2` e unidades conhecidas;
- [x] congelar BPM nullable e faixa 30..200;
- [x] mapear ownership atual de FEA1, D1 e D3/D4;
- [x] congelar autoridade FEA1/D1/D4 e hipótese inicial de overlap;
- [x] auditar triggers automáticos Health Connect;
- [x] congelar cutover diário inteiro sem backfill;
- [x] mapear UI legacy de medição manual, D1, import D3/D4 e step length;
- [x] registrar bloqueio semântico de calorias;
- [x] criar ADR-030 e Spec 033.

## Conclusões originais

`033-02` e `033-03` foram liberadas em paralelo. Nenhuma alteração de runtime foi
feita nesta task.

A hipótese inicial de overlap adotada em 033-01 era deliberadamente provisória:
D1 seria usado fora de workout e buckets intersectantes poderiam ser suprimidos
por inteiro.

## Correção posterior à validação física

Após 033-02/033-03, dois dumps sanitizados demonstraram que D1 já pode carregar
os mesmos fatos do workout D4 e que mero overlap também pode incluir conteúdo
externo ao workout. Portanto, a hipótese inicial de supressão global foi
**superada** e não deve orientar 033-04.

Contrato vigente:

```text
FEA1 = total diário autoritativo de passos
D1   = linha granular diária, inclusive podendo conter fatos de workout
D4   = fonte detalhada/canônica do workout
```

Composição e Health Connect passam a decidir autoridade por métrica/cobertura,
sem `D1 + D4` ingênuo e sem supressão de bucket por mera interseção.

Fontes vigentes:

- `docs/specs/033-unified-watch-daily-sync.md`;
- `docs/decisions/adr-030-unified-watch-sync-and-granular-daily-data.md`;
- `docs/integration/033-03-d1-v2-physical-validation.md`.

## Validação

Task documental; build não é necessário. O conector GitHub não fornece working
tree local para executar `git diff --check`. Executar localmente como preflight:

```bash
git checkout feat/033-unified-watch-daily-sync
git pull
git diff --check main...HEAD
```
