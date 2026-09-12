# 033-03 — Persistir fatos diários granulares D1

- **Status:** pending
- **Depende de:** 033-01
- **Pode rodar em paralelo com:** 033-02

## Objetivo

Evoluir Room de v17 para v18 e persistir D1 como fatos diários completos e
idempotentes.

## Escopo

- entidade/tabela D1 granular;
- chave única estável por dispositivo + data + posição;
- DAO e repository para insert/update/dedup;
- BPM nullable e raw adicionais;
- metadata de importação/component success sem reutilizar `lastImportedAt` de
  forma ambígua;
- migration `17 -> 18` aditiva;
- schema exportado v18;
- adapters de domínio/read path necessários.

## Guard rails

- não backfillar fatos inexistentes dos registros D1 BPM-only antigos;
- migration não cria snapshot, fila remota ou efeito Health Connect;
- preservar `heart_rate_measurements` históricos;
- preservar schema/contratos da 032.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.
