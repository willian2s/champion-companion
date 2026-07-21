# Task 003-05 — Modelar Atividades D3/D4 e Revisão

## Status

Concluída.

## Dependência

- `003-02` concluída.

## Objetivo

Modelar WorkoutActivity, WatchWorkoutImport, intervalos e amostras; preservar importação e revisão do usuário.

## Pode modificar

- Modelos e regras de atividade/importação em `core/model/**`.
- Testes JVM destes agregados.
- Este arquivo.

## Passos

1. Criar entrada de parser separada de domínio para D3/D4; não implementar parser/protocolo nem guardar payload binário no domínio.
2. Modelar métricas disponíveis: início, duração, passos, calorias, distâncias original/estimada/calibrada, ritmo, velocidade, BPM agregado/amostras/intervalos, origem, device, parser e diagnóstico vinculado.
3. Criar `ActivityWorkflowState`: `PENDING_REVIEW`, `PENDING_CALIBRATION`, `READY_TO_SYNC`, `SYNCING`, `SYNCED`, `SYNC_FAILED`, `DISCARDED`; implementar transições válidas e descarte terminal.
4. D3/D4 inicia sempre `PENDING_REVIEW`; `PARTIAL`/`INVALID` nunca chegam a `READY_TO_SYNC`; esteira exige calibração.
5. Preservar tipo sugerido e confirmado separadamente. Sugestão: menos de 1.000 passos = treino livre; 1.000 ou mais = corrida. Sugestão não substitui confirmação.
6. Definir chave D3/D4 `dispositivo + origem + CRC`; reimportação atualiza parser sem perder confirmação, revisão, calibração ou descarte.
7. Testar máquina de estados, regra de sugestão, reimportação e campos indisponíveis.

## Aceite

- Booleanos contraditórios não representam workflow.
- Atividade importada preserva valores originais e edições do usuário.
