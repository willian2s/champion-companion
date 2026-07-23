# Task 010-13 - Fechar fase de importação D3/D4

## Status

Pendente.

## Objetivo

Consolidar encerramento documental da Fase 10 após Gate B: spec, overview, baseline factual, checklist, lacunas e dívida técnica.

## Contexto

Após validação física final, a fase precisa atualizar documentação factual sem reabrir roadmap, fases antigas ou Health Connect.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-12-resolve-physical-findings-and-run-gate-b.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Dependências

- `010-12`.

## Entradas

- Gate B concluído;
- protocolo D3/D4 atualizado;
- riscos residuais conhecidos.

## Arquivos exatos permitidos

- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-13-close-watch-workout-import-phase.md`
- `docs/protocol/workouts-d3-d4.md`
- `docs/baseline/current-baseline.md`
- `docs/baseline/feature-parity-checklist.md`
- `docs/baseline/gaps-and-open-questions.md`

## Arquivos proibidos

- roadmap;
- ADRs, salvo decisão explícita do usuário em nova execução;
- código Kotlin, salvo correção documental mínima de caminho ou nome citados.

## Escopo

- marcar fase como concluída quando Gate B estiver aceito;
- atualizar baseline factual;
- alinhar checklist de paridade e lacunas;
- consolidar dívida técnica e riscos aceitos;
- fechar checklist e instruções do overview.

## Fora do escopo

- nova implementação funcional;
- revisão da atividade;
- Health Connect.

## Passos

1. Atualizar spec e overview com estado final real da fase.
2. Atualizar baseline factual, checklist de paridade e lacunas abertas.
3. Registrar riscos aceitos, dívidas adiadas e fronteira para Fases 11 e 12.
4. Confirmar evidências de build, instalação e Gate B.

## Critérios de aceite

- spec, overview e baseline factual alinhados ao estado final;
- checklist de paridade reflete a entrega D3/D4 até `PENDING_REVIEW`;
- lacunas remanescentes ficam explícitas;
- fronteira para Fases 11 e 12 permanece clara.

## Testes pequenos aplicáveis

- nenhum; task é de fechamento documental.

## Validação mínima

- revisão do diff documental final;
- conferência das evidências citadas.

## Evidências

- diffs documentais finais;
- referências aos comandos obrigatórios e ao Gate B concluído;
- lista de riscos aceitos e dívidas adiadas.

## Atualização do overview

- marcar `010-13` como concluída;
- fechar checklist, comandos, Gate B, dívida e instruções de atualização.

## Validação final

- confirmar que nenhuma fase anterior foi reaberta nem alterada fora do necessário para baseline factual e checklist.
