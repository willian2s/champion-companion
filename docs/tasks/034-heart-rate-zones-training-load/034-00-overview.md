# 034 — Zonas de frequência cardíaca e carga cardiovascular

- **Status geral:** in_progress
- **Spec:** [034-heart-rate-zones-training-load.md](../../specs/034-heart-rate-zones-training-load.md)
- **Progresso:** 5/6 subtarefas concluídas

## Objetivo

Adicionar FC máxima pessoal, zonas de frequência cardíaca, cobertura temporal e
carga cardiovascular local aos detalhes de workouts existentes, reutilizando a
série `WorkoutHeartRateSample` persistida e preservando BLE, parser, workflow e
Health Connect.

## Checklist

- [x] [034-01-auditar-timeline-fc-e-congelar-contrato.md](034-01-auditar-timeline-fc-e-congelar-contrato.md)
- [x] [034-02-implementar-dominio-zonas-e-carga.md](034-02-implementar-dominio-zonas-e-carga.md)
- [x] [034-03-persistir-fc-maxima-pessoal.md](034-03-persistir-fc-maxima-pessoal.md)
- [x] [034-04-integrar-read-model-no-detalhe.md](034-04-integrar-read-model-no-detalhe.md)
- [x] [034-05-renderizar-zonas-e-carga.md](034-05-renderizar-zonas-e-carga.md)
- [ ] [034-06-validar-fisicamente-e-fechar.md](034-06-validar-fisicamente-e-fechar.md)

## Observações

- Room atual: v20 após migration aditiva e nullable v19 -> v20.
- Sample-and-hold de ADR-018 é única política temporal autorizada.
- Zonas e carga são read model não persistido e dependem da FC máxima atual.
- Health Connect não recebe métrica nova e não deve ganhar operações.
- A validação deve usar workout existente; não exigir treino novo.
- A branch alvo já existe sobre a base local de `main`; documentação não inclui
  commit nesta fase de planejamento.
- `034-01` concluída em 2026-09-16: janela canônica, sample-and-hold,
  persistência, Perfil, detalhe de Exercícios, Room v19 e fronteiras remotas
  foram auditados. `034-02` e `034-03` podem avançar em paralelo; nenhuma outra
  subtarefa foi iniciada.
- `034-02` concluída em 2026-09-16: domínio puro de zonas, cobertura e carga
  implementado; helper sample-and-hold compartilhado com splits; lint e builds
  debug/release passaram. Validação física e cálculo independente permanecem em
  `034-06`.
- `034-03` concluída em 2026-09-16: FC máxima pessoal persistida como coluna
  nullable no Room v20, com migration aditiva v19 -> v20, round-trip de perfil,
  preservação em altura/passada e editor local no Perfil. Lint e builds
  debug/release passaram; validação física permanece em `034-06`. `034-05` e
  `034-06` não foram iniciadas.
- `034-04` concluída em 2026-09-16: read model efêmero integrado ao contrato de
  detalhe e recomposto por atividade selecionada ou snapshot pessoal atual,
  restrito a workouts do relógio e sem efeitos em workflow, snapshots ou Health
  Connect. Compilação, lint e builds debug/release passaram; renderização visual
  permanece em `034-05` e validação física em `034-06`.
- `034-05` concluída em 2026-09-16: seção visual de zonas, carga cardiovascular,
  cobertura e estados seguros integrada entre Frequência cardíaca e Cadência,
  com ordem explícita Z5..Z1 + Abaixo de Z1 e semantics acessíveis. Compilação,
  lint, builds debug/release e revisão independente passaram; validação física,
  screenshot e fechamento permanecem em `034-06`.
