# 013-17 — Fechar Fase 13 de exercícios phone com GPS e rota

- **Ticker:** `013`
- **Número:** `17`
- **Status:** pending

## Objetivo e resultado esperado

Reconciliar documentação com estado entregue e encerrar Fase 13 sem iniciar Fase 14. Resultado: spec, overview, roadmap, baseline, ADR e protocolos refletem evidências/limites reais.

## Contexto

Fechamento depende do gate físico aceito. Fase 12 deve permanecer concluída sem rota; Fase 14 continua posterior com WorkManager/tarefas adiáveis.

## Documentos obrigatórios

Spec/overview e todas tasks 013; evidências 013-16; roadmap; baselines; ADR-004/012/014; protocolos afetados; spec/overview 015.

## Dependências

`013-16` concluída e aceita.

## Arquivos permitidos

Documentação autoritativa, tasks/overview 013, ADRs/protocolos para fechamento factual.

## Arquivos e símbolos prováveis

Status da spec/overview, Fases 12-14 do roadmap, baseline, G-13/G-14, mapeamento Health Connect, ADR-004/012/014 e protocolos D0/F002/D3/D4 quando afetados.

## Arquivos proibidos

Código, Gradle, manifesto, schemas e testes.

## Requisitos cobertos

Fechamento; fronteiras; migration/ADR; riscos aceitos; evidências; status/progresso; nenhum WorkManager/teste.

## Escopo

- atualizar status da spec/overview e todos links;
- reconciliar baseline/roadmap sem renumerar;
- registrar v6, FGS, rota, correlação e HC reais;
- registrar limitações do BPM esportivo e riscos residuais;
- confirmar dívida do guard antigo conforme decisão e sem alegar teste alterado;
- preservar F14.

## Fora do escopo

Implementação corretiva, F14, WorkManager, novos features, testes.

## Passos de implementação

1. Conferir 17 tasks, statuses, evidências e overview.
2. Atualizar spec somente com fatos entregues.
3. Atualizar baseline/feature inventory/gaps/HC mapping.
4. Atualizar roadmap: F13 concluída e F14 posterior.
5. Confirmar ADR-014/ADR-004 coerentes.
6. Atualizar protocolos só com observações reais.
7. Registrar migration v6/schema e estratégia de rollback residual.
8. Registrar riscos aceitos e débitos, sem esconder não reproduzidos.
9. Marcar `013-17` e progresso `17/17` somente após aceite.
10. Mostrar `git status`; não commit/push.

## Critérios de aceite

- status/progresso/checklist correspondem;
- F12 não é reescrita como route-capable;
- F14 mantém WorkManager/tarefas adiáveis;
- baseline descreve somente entrega real;
- ADR/migration/riscos/evidências são rastreáveis;
- nenhum teste automatizado é alegado;
- nenhum commit/push.

## Validação manual

Revisão documental de links, nomes, ticker, estados, matriz de rastreabilidade e `git status`.

## Comandos

Nenhum Gradle adicional se evidências válidas de 013-16 existirem. Não executar testes.

## Evidências

Diff documental, verificação de links/checklist, resumo de riscos e `git status` final.

## Screenshots

Referenciar screenshots da 013-16; não criar evidência nova sem execução.

## Definição de pronto

Fase 13 fechada documentalmente, overview 17/17 e Fase 14 intacta.

## Riscos e cuidados

Não converter risco aceito em sucesso. Não apagar dívida do guard/protocolo. Não iniciar F14.

## Rollback

Reverter somente fechamento documental se evidência for invalidada; reabrir status das tasks afetadas honestamente.

Não criar, alterar ou executar testes automatizados.
