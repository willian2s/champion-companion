# Task 003-07 — Modelar Sessões do Aplicativo

## Status

Concluída.

## Dependência

- `003-02` concluída.

## Objetivo

Modelar ciclo persistente de caminhada, corrida, treino livre e esteira iniciado pelo aplicativo.

## Pode modificar

- Modelos/regras de sessão em `core/model/**`.
- Testes JVM correspondentes.
- Este arquivo.

## Passos

1. Criar sessão com UUIDv7 antes de iniciar, tipo, criação, início/fim, duração ativa/pausada, agregados de passos, distância, calorias e BPM.
2. Implementar `AppWorkoutSessionState`: `CREATED`, `ACTIVE`, `PAUSED`, `AWAITING_RECOVERY`, `FINISHED`, `DISCARDED`.
3. Implementar criar, iniciar, pausar, retomar, interrupção, recuperar, finalizar e descartar; impedir transições terminal/inválidas.
4. Finalização gera vínculo para WorkoutActivity sem persistir coordenadas, rota ou amostras GPS.
5. Testar recuperação após interrupção, duração acumulada, finalização e descarte.

## Aceite

- Usuário pode retomar, finalizar com agregados persistidos ou descartar sessão interrompida.
- Sessão não depende de serviço, GPS, BLE ou Health Connect.
