# 033-01 — Auditar e congelar D1 v2, watch sync e cutover

- **Status:** completed
- **Depende de:** nenhuma
- **Libera:** 033-02 e 033-03
- **Auditoria:** `docs/integration/033-01-d1-v2-watch-sync-and-health-cutover-audit.md`

## Objetivo

Reauditar a `main` pós-032 e congelar contratos antes de alterar runtime, schema
ou Health Connect.

## Entregáveis

- [x] confirmar baseline pós-032 e Room v17;
- [x] auditar parser/collector D1 atual;
- [x] congelar layout alvo `d1-v2` e unidades conhecidas;
- [x] congelar BPM nullable e faixa 30..200;
- [x] mapear ownership atual de FEA1, D1 e D3/D4;
- [x] congelar autoridade FEA1/D1/D4 e overlap inicial;
- [x] auditar triggers automáticos Health Connect;
- [x] congelar cutover diário inteiro sem backfill;
- [x] mapear UI legacy de medição manual, D1, import D3/D4 e step length;
- [x] registrar bloqueio semântico de calorias;
- [x] criar ADR-030 e Spec 033.

## Conclusões

`033-02` e `033-03` estão liberadas em paralelo. Nenhuma alteração de runtime foi
feita nesta task.

## Validação

Task documental; build não é necessário. O conector GitHub não fornece working
tree local para executar `git diff --check`. Executar localmente como preflight:

```bash
git checkout feat/033-unified-watch-daily-sync
git pull
git diff --check main...HEAD
```
