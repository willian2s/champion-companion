# 024 — Refatoração visual inspirada no Google Health

- **Status geral:** in_progress
- **Spec:** [024-refatoracao-visual-google-health.md](../../specs/024-refatoracao-visual-google-health.md)
- **Progresso:** 4/18 subtarefas concluídas

## Objetivo

Refatorar incrementalmente a interface do Champion Companion, tela por tela,
usando Google Health/Fitbit atual como guia de estilo, design, hierarquia e
usabilidade, sem alterar contratos funcionais nem copiar marca ou ativos.

## Checklist

- [x] [024-01-registrar-baseline-e-direcao-visual.md](024-01-registrar-baseline-e-direcao-visual.md)
- [x] [024-02-evoluir-fundacao-visual.md](024-02-evoluir-fundacao-visual.md)
- [x] [024-03-refatorar-shell-de-navegacao.md](024-03-refatorar-shell-de-navegacao.md)
- [x] [024-04-definir-estrategia-cromatica-transversal.md](024-04-definir-estrategia-cromatica-transversal.md)
- [ ] [024-05-refatorar-hub-saude-piloto.md](024-05-refatorar-hub-saude-piloto.md)
- [ ] [024-06-refatorar-home.md](024-06-refatorar-home.md)
- [ ] [024-07-refatorar-frequencia-cardiaca.md](024-07-refatorar-frequencia-cardiaca.md)
- [ ] [024-08-refatorar-peso.md](024-08-refatorar-peso.md)
- [ ] [024-09-refatorar-passos.md](024-09-refatorar-passos.md)
- [ ] [024-10-refatorar-distancia.md](024-10-refatorar-distancia.md)
- [ ] [024-11-refatorar-calendario.md](024-11-refatorar-calendario.md)
- [ ] [024-12-refatorar-detalhe-diario.md](024-12-refatorar-detalhe-diario.md)
- [ ] [024-13-refatorar-lista-exercicios.md](024-13-refatorar-lista-exercicios.md)
- [ ] [024-14-refatorar-detalhe-exercicio.md](024-14-refatorar-detalhe-exercicio.md)
- [ ] [024-15-refatorar-dispositivo.md](024-15-refatorar-dispositivo.md)
- [ ] [024-16-refatorar-perfil.md](024-16-refatorar-perfil.md)
- [ ] [024-17-refatorar-configuracoes.md](024-17-refatorar-configuracoes.md)
- [ ] [024-18-consolidar-validacao-visual.md](024-18-consolidar-validacao-visual.md)

## Observações

- Google Health correto usa pacote `com.fitbit.FitbitMobile`; Google Fit e
  Health Connect não são referência visual.
- `024-05` é piloto e exige aceite visual antes das demais telas.
- ADB foi autorizado pelo usuário e esteve operacional nesta execução; nova
  indisponibilidade bloqueia aceite físico das subtarefas correspondentes.
- ADR duradoura criada em `024-01`:
  `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`.
- Nenhum teste automatizado será criado, alterado ou executado.
- `024-01` concluída com baseline físico sanitizado, matriz visual e ADR-023.
- Reexecução de `024-01` em 2026-08-17 passou: pacotes, versões, `Activity`
  resolvida de cada pacote, configuração física e hierarquias sanitizadas foram
  confirmados; nenhum bloqueio novo.
- `024-02` concluída após resolução do conflito entre plano e R2: APIs sem
  consumidor real e capturas do protótipo foram removidas; primitives, tema e
  componentes vigentes com uso real permanecem como fundação mínima.
- Spec 024 e ADR-023 não receberam exceção. Componentes internos de superfície
  só podem ser promovidos após dois usos reais; `024-03` pode compartilhar apenas
  invariantes estritos do shell.
- Resolução de `024-02` passou em `git diff --check`, lint, builds debug/release,
  instalação e smoke ADB de Saúde, Início e menu `Mais`; testes automatizados não
  foram executados.
- Detalhe diário e estados raros ficaram `N/A` em `024-01`; landscape, fonte
  `1,3x`, dynamic/fallback e temas devem ser cobertos nas subtarefas de
  superfície que renderizarem a composição real.
- `024-03` concluída com shell tonal, cinco entradas preservadas, menu `Mais`,
  bounds mínimos, smoke ADB de rotas/detalhe/retorno e validação de fonte `1,3x`,
  landscape e temas claro/escuro; evidências sanitizadas portrait/landscape
  reais e vetoriais versionadas na pasta da task.
- Durante `024-03`, primeiro script de smoke precisou ser corrigido para usar
  bounds dos itens do `DropdownMenu`; nenhum bloqueio funcional foi identificado.
- Revisão independente de `024-03` passou após inclusão das capturas reais
  sanitizadas; riscos residuais documentados não bloqueiam aceite.
- `024-05` está `pending`: piloto Saúde implementado, badges dos cards removidos
  após feedback do usuário e build/instalação/smoke portrait validados. Estratégia
  cromática opt-in foi aprovada; falta aplicá-la ao piloto, concluir validações e
  registrar aceite visual. Checklist permanece desmarcado.
- Plano normalizado: `024-04` agora define estratégia cromática transversal;
  piloto Saúde passou a `024-05`; subtarefas seguintes foram deslocadas até
  `024-18`. Progresso passa a `4/18` após aprovação da estratégia.
- Revisão independente de `024-04` passou após materialização da matriz de roles,
  dependência direta do piloto e validação da renumeração documental.
