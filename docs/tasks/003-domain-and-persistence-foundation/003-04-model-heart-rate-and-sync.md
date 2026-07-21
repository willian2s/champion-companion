# Task 003-04 — Modelar BPM e Sincronização Futura

## Status

Concluída.

## Dependência

- `003-02` concluída.

## Objetivo

Modelar HeartRateMeasurement e HealthSyncOperation para D1 futuro, manual, stream, sessão e atividade importada.

## Pode modificar

- Modelos e regras de BPM/sync em `core/model/**`.
- Testes JVM destes agregados.
- Este arquivo.

## Passos

1. Criar origens de BPM, timestamp, BPM, validade, device, importação, versão de parser e vínculo opcional com sessão/atividade.
2. Definir deduplicação: D1 por `dispositivo + origem + instante resolvido/chave de registro`; manual/stream por ID estável da sessão, sem colapsar medições intencionais distintas.
3. Modelar `SyncOperationState` (`PENDING`, `SYNCING`, `SYNCED`, `FAILED`), tentativa, última tentativa e erro seguro; sync por agregado + tipo de record, sem FK polimórfica falsa.
4. Criar transições para operação individual D1: sucesso marca somente registro afetado; falha parcial preserva pendência somente dos afetados; sem seleção manual.
5. Testar estados, deduplicação e cenário de falha parcial.

## Aceite

- BPM válido pendente pode ser consultado individualmente no futuro.
- Sync não usa Health Connect nem expõe erros técnicos ao domínio/UI.
