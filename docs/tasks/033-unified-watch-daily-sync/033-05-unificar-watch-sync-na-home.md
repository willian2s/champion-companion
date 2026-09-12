# 033-05 — Unificar watch sync na Home

- **Status:** pending
- **Depende de:** 033-02 e 033-03

## Objetivo

Fazer do pull-to-refresh da Home o único entrypoint de leitura de FEA1, D1 e
D3/D4.

## Escopo

- `WatchSyncCoordinator`;
- owner foreground único;
- reuso de conexão/sessão/handshake;
- sequência FEA1 -> D1 -> D3/D4;
- persistência por componente;
- partial success sem rollback global;
- metadata/timestamps por componente;
- estados de Home claros para progresso, partial, retry e erro;
- dedup/skip D3/D4 existentes preservados.

## Guard rails

- não sincronizar Health Connect automaticamente;
- não iniciar watch sync ao abrir app/tela;
- não tornar D1/D4 dependentes de Compose;
- não reintroduzir background polling.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```
