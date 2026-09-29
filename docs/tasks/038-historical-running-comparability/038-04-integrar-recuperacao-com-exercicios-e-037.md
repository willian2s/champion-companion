# 038-04 — Integrar recuperação com Exercícios e 037

- **Ticker:** `038`
- **Número:** `04`
- **Status:** `pending`

## Objetivo

Integrar recuperação autorizada à ação existente de Exercícios sem duplicar CTA
ou pipeline, e confirmar que a Spec 037 incorpora somente snapshot nativo que
satisfaça naturalmente seu contrato atual.

## Resultado esperado

Usuário vê preflight, elegibilidade e motivo seguro antes de qualquer mutação;
ação é explícita e local. Novo snapshot elegível recompõe 037 automaticamente
por observação Room, sem alterar `RunningPerformanceHistoryPolicy` nem incluir
fallback.

Em `NO-GO` ou `INCONCLUSIVO`, documentar que nenhuma UI ou integração adicional
é necessária.

## Requisitos cobertos

- Reuso da ação individual atual de reprocessamento.
- Preflight antes da confirmação.
- Mensagem do que muda localmente e do que não muda no Health Connect.
- Motivo seguro para inelegibilidade/bloqueio.
- 037 preservada: métodos nativos, versão v4, temporalidade e plausibility.

## Escopo incluído

- Auditar `ExercisesViewModel`, `ExercisesScreen` e contratos de estado.
- Reaproveitar `requiresWatchWorkoutReprocessing` se representar a ação.
- Exibir somente classe/motivo que o preflight autorize.
- Confirmar recomposição automática do read model após snapshot novo.
- Documentar N/A se fluxo existente já cobrir a necessidade.

## Escopo excluído

- Novo destino de navegação, CTA duplicado ou batch obrigatório.
- Relaxar filtros de 037, incluir `STEP_LENGTH_FALLBACK` ou alterar janela.
- Atualizar Health Connect, resetar workflow ou mostrar divergência local/remota
  como comparabilidade oficial.
- Alterar parser, Room/schema, BLE, calibração ampla ou analytics persistente.

## Dependências

- `038-02` preflight e `038-03` recuperação aprovados pelo Gate.
- `ExercisesViewModel.kt`, `ExercisesScreen.kt`,
  `RunningPerformanceHistory.kt` e tela de histórico.
- ADR-037 e contrato de reprocessamento atual.

## Arquivos e símbolos prováveis

- `feature/exercises/ExercisesViewModel.kt`:
  `ReprocessSelectedActivity`, estado de reprocessamento e sync separado.
- `feature/exercises/ExercisesScreen.kt`: confirmação, card, bloqueios e HC.
- `core/model/RunningPerformanceHistory.kt` e
  `RunningPerformanceHistoryScreen.kt`.
- `RoomRepositories`/Flow de snapshot ativo observado pelo read model.

## Passos de implementação

1. Verificar se ação atual pode representar a recuperação sem novo CTA.
2. Inserir preflight visual antes de confirmação, com motivo seguro.
3. Declarar explicitamente preservação local/remota e ausência de HC.
4. Manter `SYNCED` bloqueado e sync como gesto separado.
5. Confirmar que snapshot novo elegível aparece em 037 por recomposição, sem
   leitura de splits/derived completos nem nova policy.
6. Validar que fallback continua listável, mas fora dos gráficos.
7. Se nenhuma mudança de UI for necessária, registrar decisão e fechar tarefa
   sem camada nova.

## Testes e comandos de validação

- Revisão de estados UI e chamadas, sem automatizar nova suíte.
- `git diff --check`.
- Validação manual futura no dispositivo real, somente após implementação
  autorizada e sem acionar sync.
- Nenhum teste automatizado criado ou executado nesta execução.

## Definição de pronto

- Ação existente é reutilizada ou a necessidade de nova UI é justificada.
- Preflight/motivo aparecem antes da mutação.
- Texto não promete alteração Health Connect nem reabre `SYNCED`.
- 037 aceita somente snapshot que já satisfaz seu contrato exato.
- Fallback não entra no trend por conveniência.
- NO-GO não cria UI artificial.

## Riscos e cuidados

- `asOf` do ViewModel pode estar congelado até recriação; documentar sem mudar
  janela da 037.
- Não interpretar recomposição de UI como prova de Health Connect.
- Não chamar coordinator HC durante ação de recuperação.
- Não duplicar query completa, N+1 ou pipeline de derivação na tela.
