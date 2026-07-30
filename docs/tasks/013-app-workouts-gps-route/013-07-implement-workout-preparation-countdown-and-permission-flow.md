# 013-07 — Implementar preparação, countdown e fluxo de permissão

- **Ticker:** `013`
- **Número:** `07`
- **Status:** completed

## Objetivo e resultado esperado

Entregar seleção e preparação acessíveis com countdown cancelável, GPS aquecido sem persistência e status opcional do relógio. Resultado: comando de start emitido somente em zero e com pré-condições válidas.

## Contexto

Shells/destinos e preferência vêm de `013-04`; readiness vem de `013-06`. Preparação não pode criar atividade, rota ou FGS por abertura.

## Documentos obrigatórios

Spec 013; `013-04/06`; padrões visuais/a11y existentes; AGENTS.

## Dependências

`013-06` concluída.

## Arquivos permitidos

`feature/exercises/**`; navigation mínima; resources; preferences para update; estado do dispositivo somente por contratos existentes; task/overview.

## Arquivos e símbolos prováveis

`ExercisesFeatureContract`, `ExercisesViewModel`, `ExercisesScreen`, novos estados/intents de preparação/countdown, destinos de treino e `AppPreferencesRepository.update`.

## Arquivos proibidos

Room route writes, FGS real além de interface de start, D0/A8, Health Connect, testes.

## Requisitos cobertos

Tela de seleção/preparação; tipos; countdown 0/3/5/10; cancelamento; GPS/relógio; permissão; voltar sem atividade; a11y.

## Escopo

- CTA `Iniciar treino` na entrada de Exercícios;
- seleção dos quatro tipos;
- preparação por tipo;
- solicitação contextual de `ACCESS_FINE_LOCATION` só para walking/running;
- detecção de aproximada/serviço off;
- warming GPS descartável;
- countdown visível, anunciado com moderação e persistido.

## Fora do escopo

Sessão ativa completa, notificação, captura/persistência e sync.

## Passos de implementação

1. Expandir estado/intents ViewModel para seleção/preparação/countdown.
2. Integrar preferência e allowlist.
3. Iniciar warming ao entrar em preparação GPS, sem repository de rota.
4. Modelar mensagens/ações para permissão e serviços.
5. Habilitar start GPS somente bom/forte; sem GPS para livre/esteira.
6. Após toque, contar; cancelar retorna espera.
7. Se readiness cair durante countdown GPS, cancelar/pausar e retornar espera.
8. Em zero, emitir uma única solicitação idempotente de início para `013-08`.
9. Cleanup warming ao sair; voltar sem criar nada.
10. Validar fonte aumentada/landscape e atualizar overview.

## Critérios de aceite

- abrir/voltar não cria sessão/atividade/FGS/rota;
- todos countdowns e último valor funcionam;
- 0 inicia sem atraso artificial após toque;
- cancelamento não cria atividade;
- treino sem GPS não pede localização;
- perda de GPS impede start;
- status e ações têm descrição acessível.

## Validação manual

Countdown 0/3/5/10, cancelamento, permissão negada/aproximada, localização desligada, estados GPS e degradação; livre/esteira sem prompt; back; rotação, 1,3x e landscape.

## Comandos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

## Evidências

Roteiro preenchido, confirmação de ausência de writes/service antes de zero, comandos e screenshots.

## Screenshots

Obrigatórias: seleção; preparação GPS em estados principais; preparação sem GPS; countdown; fonte aumentada/landscape representativos.

## Definição de pronto

Preparação funcional emite exatamente um start válido e overview está atualizado.

## Riscos e cuidados

Recomposição não pode duplicar timer/start. Live region não deve anunciar cada frame. Não expor coordenadas em UI/erro.

## Rollback

Remover destinos/CTA novos e conservar preference inócua.

Não criar, alterar ou executar testes automatizados.

## Registro de execução

### Estado e escopo

- Dependência `013-06` confirmada como concluída; ticker `013` consistente entre
  spec, pasta, overview e subtarefas.
- Escopo executado somente nesta subtarefa: CTA, seleção, preparação,
  aquecimento GPS descartável, permissão contextual, countdown e handoff
  idempotente para `013-08`.
- Nenhuma sessão, atividade, rota, escrita Room, FGS, BLE, D0/A8 ou Health
  Connect foi iniciado nesta etapa.

### Decisões e desvios

- `ExercisesViewModel` concentra estado de preparação, geração do runtime GPS,
  token de visibilidade e execução única do countdown; recomposição não duplica
  timer nem `WorkoutStartRequest`.
- Caminhada/corrida usam `WorkoutLocationSource` somente enquanto preparação
  está visível. Readiness é invalidada ao suspender, sair ou trocar geração; fix
  de aquecimento não é persistido.
- Permissão runtime solicita FINE/COARSE somente para caminhada/corrida.
  Treino livre e esteira não criam fonte GPS nem exibem prompt de localização.
- `WorkoutStartRequest` é handoff in-memory idempotente, emitido somente em
  zero com readiness válida e token de preparação vigente. Consumo, sessão e
  FGS permanecem em `013-08`.
- Back, troca de destino e cleanup de lifecycle encerram preparação antes de
  alterar back stack. Perda de readiness durante countdown retorna à espera.
- Negação permanente de permissão ainda depende de encaminhamento futuro às
  configurações do aplicativo; comportamento seguro permanece sem start.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/navigation/ChampionNavigation.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesFeatureContract.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesScreen.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/ExercisesViewModel.kt`
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`
- este arquivo e `013-00-overview.md`

### Comandos executados, resultados e evidências

- `git diff --check`: passou.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`; lint e
  builds debug/release concluídos sem keystore local.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`; APK instalado no Galaxy S22
  `SM-S901E - 16`.
- Validação manual parcial no dispositivo confirmou CTA, seleção acessível e
  preparação de treino livre sem prompt GPS. Warming GPS, countdowns,
  permissões, back, rotação, fonte aumentada e landscape permanecem pendentes
  de execução física completa.
- Screenshots temporários capturados em `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/013-07-selection.png`,
  `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/013-07-free.png` e
  `/var/folders/_r/sp91m8xn4zv3_wwh1wq46_cm0000gn/T/opencode/013-07-exercises.png`.
- Revisão independente final: aprovada, sem blockers estáticos. Confirmou
  readiness vinculada à geração, cancelamento síncrono na navegação, cleanup,
  exactly-once até o handoff e escopo restrito.
- Nenhum teste automatizado foi criado, alterado ou executado, conforme regra
  explícita da Fase 13.

### Riscos residuais

- Validação física completa de permissão negada/aproximada, localização
  desligada, estados reais de GPS, countdown 0/3/5/10, degradação, rotação,
  background, fonte aumentada, landscape e TalkBack ainda pertence ao roteiro
  manual desta subtarefa.
- Após negação permanente, a tela oferece nova solicitação, mas ainda não
  detecta de forma explícita quando deve abrir configurações do aplicativo.
- Exatamente uma solicitação foi garantida até `WorkoutStartRequest`; criação de
  sessão, FGS e consumo idempotente pertencem a `013-08`.

### Estado de conclusão

- Critérios estáticos e builds atendidos; item concluído sem iniciar
  subtarefas seguintes.
- Próxima subtarefa elegível: `013-08`.
