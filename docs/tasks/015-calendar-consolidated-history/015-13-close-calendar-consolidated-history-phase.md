# 015-13 — Fechar Fase 15 de calendário e histórico consolidado

- **Status:** concluída em 29/07/2026.
- **Objetivo:** reconciliar documentação e registrar fechamento da fase.
- **Resultado esperado:** spec, overview, baseline, roadmap e ADR-013 existentes,
  evidências coerentes com a implementação realmente entregue.

## Documentos obrigatórios

Spec 015, overview, todas tasks 015, evidências de 015-12, baseline, roadmap,
ADR-013 criada por 015-02 e ADRs 009/011/012.

## Dependências e entradas

- 015-12 concluída.
- Todas as tasks 015 anteriores aceitas.

## Arquivos permitidos

Documentação da Fase 15, overview, baseline e roadmap para reconciliação. ADR-013
é somente entrada de leitura/verificação.

## Arquivos proibidos

Novas funcionalidades, refactors, migrations, Room, navigation, testes, BLE,
Health Connect e alteração de Fases 13/14 além do registro aprovado.

## Escopo

Reconciliar status, progresso, arquivos alterados, decisões, desvios, comandos,
resultados, screenshots, riscos residuais, dívida e critérios. Confirmar que
Fase 12 está concluída, Fases 13/14 adiadas e Fase 15 fechada somente com
evidência suficiente.

## Fora do escopo

Implementação tardia, correção oportunista, nova migration, execução de testes,
novas decisões não rastreadas ou avanço automático para outra fase.

## Passos

1. Revisar checklist e estados das tasks.
2. Conferir matriz de rastreabilidade.
3. Reconciliar baseline e roadmap já atualizado por 015-02.
4. Verificar ADR-013 aceita; ausência bloqueia fechamento e não autoriza criação
   ou alteração nesta task.
5. Consolidar evidências de build/instalação/manual/screenshots.
6. Registrar riscos aceitos e dívida.
7. Marcar somente task 015-13 e fase quando critérios forem atendidos.

## Critérios de aceite

- Overview reflete progresso real.
- Spec e implementação não divergem em escopo.
- Fases 13/14 continuam adiadas.
- Nenhuma migration foi criada sem decisão explícita.
- Evidências manuais estão vinculadas.
- Riscos residuais estão registrados.

## Validação manual

Revisão documental final e conferência das screenshots/evidências de 015-12.
Screenshot adicional: somente se houver mudança visual durante fechamento.

## Comandos de build

Não executar build novamente salvo necessidade de evidência documental. Não
executar testes.

## Evidências e atualização

Registrar diff documental, checklist final, rastreabilidade, decisões e riscos.
Atualizar overview apenas após fechamento completo.

## Validação final

Confirmar ausência de código novo nesta task, ausência de BLE/Health Connect,
ausência de testes e ausência de commit/push. Não criar, alterar ou executar
testes automatizados.

## Contexto

O fechamento precisa refletir somente o que foi realmente entregue e validado,
sem avançar automaticamente para outra subtarefa ou fase.

## Arquivos alterados

- `docs/specs/015-calendar-consolidated-history.md`;
- `docs/tasks/015-calendar-consolidated-history/015-00-overview.md`;
- `docs/baseline/current-baseline.md`;
- `docs/roadmap/champion-companion-roadmap.md`;
- este arquivo.

ADR-013 não foi alterada; permaneceu entrada de leitura conforme escopo.

## Decisões e desvios

- O fechamento usa como evidência manual autoritativa a validação de `015-12`,
  concluída no Galaxy S22 `SM-S901E` e aceita pelo usuário com riscos residuais
  explícitos. Não foram fabricadas distância positiva, zero real, estados de
  sync, peso, atividade pendente ou indisponibilidade forçada do Health Connect.
- A correção posterior da spec `015A` foi considerada no fechamento. Ela foi
  concluída sem reabrir a Fase 15, sem alterar a sequência normal e sem retirar
  aprovações anteriores.
- Spec, overview, baseline e roadmap foram atualizados para refletir Fase 15
  concluída; Fases 13 e 14 continuam adiadas e nenhuma fase seguinte foi
  iniciada automaticamente.
- ADR-013 está presente e com status `Aceita`. Sua redação histórica não foi
  alterada porque era entrada de leitura obrigatória e a task proíbe sua edição.
- Não houve mudança de código, migration, schema, BLE, Health Connect,
  permissões, workflow, screenshot ou teste automatizado nesta subtarefa.
- A ausência de nova screenshot é coerente: fechamento foi exclusivamente
  documental e não introduziu mudança visual.

## Comandos executados

- `git status --short`;
- `git diff --check`;
- inspeção documental dos tickers, status, checklist único, rastreabilidade,
  ADR-013, evidências de `015-12` e dependências `015-01` a `015-14`.

Não foram executados build, lint ou testes automatizados. Builds, instalação e
validação física permanecem evidenciados em `015-11` e `015-12`, conforme escopo
que proíbe execução adicional nesta subtarefa.

## Resultados e evidências

- Spec 015 passou a registrar status concluído, sem alterar requisitos de
  calendário, histórico local, Fases 13/14 ou proibições.
- Overview usa ticker `015`, possui uma única seção `## Checklist`, contém
  exatamente um item por subtarefa e registra `14/14` itens concluídos.
- O item `015-13` foi marcado `[x]` somente após aceite de `015-12` e fechamento
  desta task; os demais itens já estavam marcados conforme seus aceites
  registrados. Nenhum item adicional foi criado.
- Baseline registra calendário, detalhe diário, históricos de passos/distância e
  histórico dedicado de frequência cardíaca como escopo factual entregue.
- Roadmap registra Fase 12 concluída, Fases 13/14 adiadas e Fase 15 concluída,
  sem iniciar Fase 16.
- ADR-013 foi localizada, confirmada como `Aceita` e mantida intacta.
- `015-12` fornece evidência de `./gradlew lint assembleDebug assembleRelease`
  e `./gradlew installDebug` com `BUILD SUCCESSFUL`, instalação no Galaxy S22,
  39 cenários e quatorze screenshots. Os resultados aceitos incluem calendário,
  detalhe diário, históricos, navegação, operação offline, ausência de prompts e
  ausência de operações BLE/Health Connect.
- `015A-01` e `015A-02` estão concluídas; a correção de integridade semântica da
  fila foi incorporada como evidência posterior, sem reabrir `015-11` ou Fase 15.
- `git diff --check`: sem erros. Não há código novo nesta subtarefa.

## Resultados da revisão independente

- A primeira revisão independente reprovou o fechamento por referências
  documentais antigas sobre o status da Fase 12, a exposição visual ainda futura
  e o planejamento futuro de ADR/roadmap.
- As referências foram reconciliadas nesta task: `012-00` e `012-13` agora são
  tratados como concluídos, a exposição visual é registrada como entregue e a
  Definition of Done da spec registra ADR-013 aceita e roadmap atualizado.
- A revisão também apontou risco de exposição em screenshots com métricas e
  horários reais; evidências aprovadas foram mantidas e o risco foi registrado
  para futuras capturas.
- Nenhuma correção de código foi necessária. A revisão independente final foi
  solicitada novamente após as correções.

## Screenshot

Não criar screenshot novo salvo se mudança visual durante fechamento exigir.

## Riscos residuais

- Health Connect indisponível, permissões específicas, distância positiva, zero
  real, estados de sync diferentes de `Sincronizado`, degradação parcial,
  erro/retry, aggregate ausente e operação órfã não foram reproduzidos com os
  dados locais disponíveis; foram aceitos explicitamente pelo usuário em
  `015-12`.
- Peso com medidas reais e atividade pendente não estavam disponíveis no aparelho;
  ausência foi aceita sem fabricar valores.
- TalkBack falado não foi ativado; acessibilidade foi conferida pela árvore UI
  Automator e descrições semânticas.
- Diagnóstico pré-existente `ExercisesDbStatus` registra contagens de atividades
  no Logcat; desvio foi aceito pelo usuário e não alterado por proibição de código.
- Screenshots de validação permanecem evidências aprovadas e contêm métricas e
  horários reais; futuras evidências devem ser sanitizadas ou ter acesso restrito.
- ADR-013 mantém sua redação histórica de Fase 15 como próxima fase; está aceita,
  foi usada somente como entrada e não pode ser editada nesta task. Estado atual
  está reconciliado na spec, overview, baseline e roadmap.

## Atualização do overview

Item `015-13` marcado `[x]`; progresso recalculado para `14/14`. Todos os itens
estão concluídos, Fase 15 foi fechada, Fases 13 e 14 continuam adiadas e nenhuma
subtarefa ou fase seguinte foi iniciada automaticamente.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
