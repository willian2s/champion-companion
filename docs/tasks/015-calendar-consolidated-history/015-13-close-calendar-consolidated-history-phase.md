# 015-13 — Fechar Fase 15 de calendário e histórico consolidado

- **Status:** pending
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

Inicialmente nenhum; registrar documentos de fechamento alterados.

## Decisões e desvios

Registrar decisões finais, desvios aceitos e justificativas.

## Screenshot

Não criar screenshot novo salvo se mudança visual durante fechamento exigir.

## Riscos residuais

Divergências documentais herdadas, ADR-013 ausente ou evidência física
incompleta impedem marcar fase concluída até serem explicitamente registradas.

## Atualização do overview

Marcar somente o item 015-13 e atualizar progresso final após aceite de 015-12.

## Regra de testes

Não criar, alterar ou executar testes automatizados.

## Execução independente

Executar em sessão nova usando somente documentos e dependências listados. Não
depender de conversa anterior nem avançar para a próxima subtarefa.
