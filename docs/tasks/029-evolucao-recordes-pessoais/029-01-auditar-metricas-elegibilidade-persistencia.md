# 029-01 — Auditar métricas, elegibilidade e pontos de persistência

- **Ticker:** `029`
- **Número:** `01`
- **Status:** `pending`

## Objetivo

Mapear os fatos disponíveis, as fórmulas agregáveis e todos os pontos que podem tornar uma atividade elegível ou alterar sua métrica final antes de implementar evolução e recordes.

## Escopo

Auditar pelo menos:

- `WorkoutActivity` e snapshots derivados;
- workflows `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCED`, `SYNC_FAILED`, `DISCARDED` e validade `PARTIAL`;
- distância final por `RUNNING`, `WALKING`, `TREADMILL`, `CYCLING` e demais tipos;
- passos elegíveis por tipo;
- duração oficial;
- BPM e cadência D4 disponíveis para tendências;
- operações de revisão, distância manual de bicicleta, calibração e rederivação pré-sync;
- repositories/DAOs já capazes de fornecer histórico sem nova tabela;
- ponto exato em que a UI pode obter snapshot anterior e snapshot novo persistido.

## Entregáveis

- matriz `métrica × tipo × origem × elegibilidade`;
- tabela de workflows incluídos/excluídos;
- fórmulas aceitas para agregação;
- lista de mutações que podem disparar avaliação pós-save;
- riscos de re-save, mudança de tipo, atividade antiga e rederivação;
- decisão sobre navegação inicial da superfície Evolução.

## Guard rails

- não mudar código funcional nesta task salvo correção documental estritamente necessária;
- não assumir que distância derivada por passos vale para `CYCLING`;
- não assumir que toda métrica numérica pode virar recorde;
- não criar testes automatizados.

## Critérios de aceite

1. Cada métrica planejada possui fonte de verdade identificada.
2. Distância final por modalidade está documentada.
3. Estados elegíveis para evolução e recordes estão definidos.
4. Todos os pontos de persistência que podem alterar recordes estão identificados.
5. Fórmulas agregadas não dependem de média ingênua de médias.
6. O fluxo pós-save pode ser implementado sem consultar estado transitório da UI como fonte de verdade.
