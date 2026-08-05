# 013-10 — Implementar treino livre e esteira iniciados pelo celular

- **Ticker:** `013`
- **Número:** `10`
- **Status:** cancelled

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

Implementação estática em `WorkoutShells.kt`: a sessão ativa de treino livre e
esteira agora mostra card acessível sem GPS, mantém distância de esteira
indisponível antes da calibração e renderiza passos, calorias e BPM somente
quando os agregados estão presentes. `NoGpsCard` orienta a calibração da
esteira.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- este arquivo e `013-00-overview.md`

### Decisões e desvios

- Mudança limitada à superfície de Exercícios para evitar conflito com as
  frentes paralelas `013-08`, `013-09` e `013-11`.
- Sessão livre continua sem distância; esteira continua sem distância real até
  calibração. Nenhum valor foi estimado a partir de passos, GPS ou relógio.
- Finalização em atividade revisável e abertura do fluxo de calibração ficam
  para `013-12`, conforme fronteira planejada.

### Comandos, resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: primeira execução excedeu o
  timeout operacional de 120 s; repetição com timeout ampliado concluiu com
  `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; instalado em Galaxy S22.
- Validação manual no Galaxy S22: preparação de treino livre exibiu `Sem GPS`
  sem solicitação de localização; treino livre iniciou, exibiu `Sessão sem GPS`,
  duração e ausência explícita de métricas opcionais; finalização e descarte
  foram confirmados.
- Validação manual: preparação de esteira exibiu `Sem GPS` e orientação para
  informar distância no fluxo de calibração, sem estimativa local.
- Tentativa de iniciar esteira criou sessão `TREADMILL`; após interrupção
  controlada, recuperação exibiu tipo Esteira e ações de retomada, finalização e
  descarte. Card ativo de esteira não foi observado porque o handoff de início
  permaneceu em `START_REQUESTED`, dependência operacional ainda pendente de
  `013-08`.
- Capturas temporárias: `013-10-free-preparation.png`,
  `013-10-free-active.png` e `013-10-treadmill-preparation.png`. Não foram
  adicionadas ao repositório nesta execução.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme regra
  da fase.

## Screenshots

Obrigatórias: preparação e ativa de cada tipo; ausência de dados; entrada de calibração.

## Definição de pronto

Tipos sem GPS funcionam isoladamente e overview está atualizado.

## Riscos e cuidados

Não reutilizar distância GPS/estimativa de passos como real de esteira. Finalização atômica completa pertence a 013-12.

## Bloqueios e riscos residuais

- Bloqueio de aceite: card ativo e pause/resume da esteira ainda dependem de
  handoff funcional de `013-08`; a tentativa física ficou em
  `START_REQUESTED` e foi limpa por recuperação/descarte.
- Diagnóstico independente confirmou que `startForegroundService()` retorna sem
  exceção antes de `startForeground()`; `startForegroundCount=0` prova que o
  início não foi confirmado. A causa provável está no estado compartilhado de
  `WorkoutRecordingService`/`WorkoutSessionOwnershipCoordinator` durante
  teardown (`pendingSessionId` ou owner `TEARING_DOWN`), não na policy de
  `TREADMILL`.
- Correção exige mudança transversal em `013-08` para handoff serializado,
  confirmação real de foreground e retry limitado. Não foi aplicada nesta
  subtarefa para respeitar arquivos e escopo autorizados.
- Fluxo final de atividade, calibração e distância informada permanece em
  `013-12`.
- Screenshots de sessão ativa da esteira e entrada de calibração permanecem
  pendentes.

## Rollback

Ocultar tipos na seleção e preservar sessões como descartáveis/recuperáveis.

Não criar, alterar ou executar testes automatizados.

## Encerramento da Fase 013 — cancelada por reorientação de produto

- **Status atual:** `cancelled`.
- **Motivo:** `product_reorientation`; novos treinos iniciados pelo celular
  foram descontinuados como direção ativa.
- **Resultado aproveitável:** permanecem a política documental de treino livre
  e esteira sem GPS, o card sem localização e a orientação de calibração
  registradas na execução. Isso não representa aceite da sessão completa.
- **Execução e bloqueios preservados:** a tentativa de esteira ficou em
  `START_REQUESTED`, sem card ativo, pause/resume ou screenshots correspondentes;
  o bloqueio de handoff compartilhado de `013-08` continua factual. Nenhuma
  ausência foi convertida em implementação concluída.
- **Evidências e riscos:** registros físicos, capturas temporárias, diagnóstico
  de `startForegroundCount=0` e riscos de finalização/calibração permanecem no
  histórico desta task. Não houve novo gate nesta decisão.
- **Destino:** preservar código, sessões e documentação existentes sem ampliar
  entrada phone; uma redefinição conceitual futura de treinos do relógio pode
  reutilizar somente políticas sem GPS e o fluxo de calibração, sem alegar
  migração funcional já executada.
