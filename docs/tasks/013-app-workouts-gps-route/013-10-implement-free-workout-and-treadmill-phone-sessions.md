# 013-10 — Implementar treino livre e esteira iniciados pelo celular

- **Ticker:** `013`
- **Número:** `10`
- **Status:** pending

## Objetivo e resultado esperado

Entregar sessões sem GPS para treino livre e esteira, com countdown, duração, dados opcionais do relógio e encaminhamento correto à revisão/calibração. Resultado: ambos funcionam sem localização e sem relógio.

## Contexto

FGS e sessão existem após `013-08`. Fluxo atual de calibração de atividade deve ser reutilizado; distância real da esteira não pode ser inventada.

## Documentos obrigatórios

Spec 013; specs/ADR 011/012/014; `013-03/08`; implementação existente de revisão/calibração.

## Dependências

Base operacional de `013-08` disponível. Pode iniciar antes do aceite integrado de
`013-08`, em paralelo com `013-09`/`013-11`, com isolamento de hotspots. Não
antecipa o aceite integrado de FGS dinâmico GPS/BLE.

## Arquivos permitidos

`feature/exercises/**`; session service/coordinator para tipos sem GPS; domínio/repository somente APIs aprovadas; resources; task/overview.

## Arquivos e símbolos prováveis

Policies de `WorkoutType.FREE_WORKOUT`/`TREADMILL`, `WorkoutSessionCoordinator`, `ExercisesViewModel`, estado de sessão ativa, `SubmitWorkoutReviewInput` e fluxo `TreadmillCalibration`.

## Arquivos proibidos

Location permissions/runtime, rota, D3/D4 correlação, Health Connect e testes.

## Requisitos cobertos

Livre/esteira sem GPS; ausência de relógio/BPM/passos; esteira pendente de calibração; countdown e tela ativa.

## Escopo

- start sem readiness GPS;
- notificação sem status GPS de erro;
- duração ativa/pausada;
- passos/BPM/calorias opcionais conforme fontes;
- treino livre revisável;
- esteira sem distância real e encaminhamento ao fluxo de calibração.

## Fora do escopo

Captura de rota, estimativa falsa de distância, protocolo BLE live e sync.

## Passos de implementação

1. Separar policy por tipo no coordinator/UI.
2. Garantir zero acesso ao location source para livre/esteira.
3. Mostrar métricas aplicáveis e ausência explícita.
4. Implementar pause/resume/recovery base.
5. Preparar draft final de livre em `PENDING_REVIEW`.
6. Preparar draft final de esteira para revisão e `PENDING_CALIBRATION` após confirmação de tipo.
7. Reutilizar formulário/cálculos atuais de calibração, sem modelo paralelo.
8. Validar sem relógio e atualizar overview.

## Critérios de aceite

- nenhuma permissão/localização é solicitada;
- GPS não aparece como falha;
- sessões continuam com FGS;
- ausência BPM/passos é preservada;
- esteira não possui distância real antes da calibração;
- calibração existente continua fonte da distância final.

## Validação manual

Livre e esteira com countdowns, sem localização, sem relógio, pause/resume, background/recovery, finalização e fluxo de calibração.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Ausência de prompt GPS, estados/metricas, calibração, comandos e screenshots.

## Screenshots

Obrigatórias: preparação e ativa de cada tipo; ausência de dados; entrada de calibração.

## Definição de pronto

Tipos sem GPS funcionam isoladamente e overview está atualizado.

## Riscos e cuidados

Não reutilizar distância GPS/estimativa de passos como real de esteira. Finalização atômica completa pertence a 013-12.

## Rollback

Ocultar tipos na seleção e preservar sessões como descartáveis/recuperáveis.

Não criar, alterar ou executar testes automatizados.
