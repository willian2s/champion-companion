# 024 — Refatoração visual inspirada no Google Health

- **Status geral:** in_progress
- **Spec:** [024-refatoracao-visual-google-health.md](../../specs/024-refatoracao-visual-google-health.md)
- **Progresso:** 9/19 subtarefas concluídas

## Objetivo

Refatorar incrementalmente a interface do Champion Companion, tela por tela,
usando Google Health/Fitbit atual como guia de estilo, design, hierarquia e
usabilidade, sem alterar contratos funcionais nem copiar marca ou ativos.

## Checklist

- [x] [024-01-registrar-baseline-e-direcao-visual.md](024-01-registrar-baseline-e-direcao-visual.md)
- [x] [024-02-evoluir-fundacao-visual.md](024-02-evoluir-fundacao-visual.md)
- [x] [024-03-refatorar-shell-de-navegacao.md](024-03-refatorar-shell-de-navegacao.md)
- [x] [024-04-definir-estrategia-cromatica-transversal.md](024-04-definir-estrategia-cromatica-transversal.md)
- [x] [024-05-refatorar-hub-saude-piloto.md](024-05-refatorar-hub-saude-piloto.md)
- [x] [024-06-refatorar-home.md](024-06-refatorar-home.md)
- [x] [024-07-refatorar-frequencia-cardiaca.md](024-07-refatorar-frequencia-cardiaca.md)
- [x] [024-08-refatorar-peso.md](024-08-refatorar-peso.md)
- [x] [024-09-refatorar-passos.md](024-09-refatorar-passos.md)
- [ ] [024-10-refatorar-distancia.md](024-10-refatorar-distancia.md)
- [ ] [024-11-refatorar-calendario.md](024-11-refatorar-calendario.md)
- [ ] [024-12-refatorar-detalhe-diario.md](024-12-refatorar-detalhe-diario.md)
- [ ] [024-13-refatorar-lista-exercicios.md](024-13-refatorar-lista-exercicios.md)
- [ ] [024-14-refatorar-detalhe-exercicio.md](024-14-refatorar-detalhe-exercicio.md)
- [ ] [024-15-refatorar-dispositivo.md](024-15-refatorar-dispositivo.md)
- [ ] [024-16-refatorar-perfil.md](024-16-refatorar-perfil.md)
- [ ] [024-17-refatorar-configuracoes.md](024-17-refatorar-configuracoes.md)
- [ ] [024-19-colorir-selecao-navegacao.md](024-19-colorir-selecao-navegacao.md)
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
- `024-05` concluída: piloto Saúde aceito visualmente pelo usuário após aplicação
  do branding, redução de textos, diferenciação de estados e remoção do card
  Health Connect. Progresso era `5/18` antes da criação da correção `024-19`.
- Plano normalizado: `024-04` agora define estratégia cromática transversal;
  piloto Saúde passou a `024-05`; subtarefas seguintes foram deslocadas até
  `024-18`, e `024-19` foi inserida antes da consolidação final. Progresso inicial
  era `4/18` após aprovação da estratégia.
- Revisão independente de `024-04` passou após materialização da matriz de roles,
  dependência direta do piloto e validação da renumeração documental.
- `024-05` usa header azul-petróleo do Champion, acentos primário/secundário/
  terciário, `Métricas locais` adaptada por contraste e textos técnicos reduzidos.
- Card Health Connect foi removido do hub conforme decisão do usuário; status de
  integração deve ser consultado em Configurações. Nenhuma alteração funcional
  foi feita em Health Connect, ViewModel, rotas ou persistência.
- Evidência sanitizada atualizada em `024-05-captura-real-sanitizada-health-portrait.png`;
  subtarefa concluída e checklist marcado.
- Revisão independente inicial encontrou contraste claro, distinção de estados do
  peso e necessidade de repetir smoke após ajustes; correções foram aplicadas e
  novas validações físicas passaram. Revisão final encontrou inconsistência na
  semântica da distância; correção aplicada e revisão final passou sem achados
  importantes. Aceite visual do usuário registrado.
- Nova solicitação de cor para item selecionado na barra de navegação pertence ao
  shell compartilhado, fora de `024-05`; requer planejamento de correção próprio
  antes de implementação e não reabre subtarefa aceita.
- Planejamento criado em `024-19-colorir-selecao-navegacao.md` para corrigir
  seleção cromática do shell; implementação ainda não iniciada.
- Nova solicitação tornou o padrão cromático do piloto Saúde obrigatório nas
  superfícies pendentes `024-06` a `024-19`: header azul-petróleo nominal com
  conteúdo branco e acentos nominais claros/escuros nos cards, indicadores e
  progresso, sem alteração do tema raiz. Regra também foi registrada no ADR-023.
- `024-06` concluída: Home prioriza resumo/progresso, mantém status textual do
  relógio, refresh, pendências, atalhos e estados seguros; card de revisão usa
  acento secundário/terciário conforme prioridade; badge cromático sobre o ícone
  do relógio foi removido a pedido do usuário; título `Hoje` ficou centralizado.
- `024-06` passou em `git diff --check`, `./gradlew lint assembleDebug
  assembleRelease`, instalação debug e smoke ADB sanitizado. Foram exercitados
  portrait, retorno, rolagem, atalhos para Exercícios/Perfil/Dispositivo/BPM,
  barra inferior Saúde → Início, fonte `1,3x`, landscape e temas claro/escuro;
  configurações do aparelho foram restauradas para fonte `1,0x`, portrait e
  tema escuro.
- Evidência sanitizada registrada em
  `024-06-captura-real-sanitizada-home-portrait.png`; barra de status foi
  recortada, métricas e dados livres foram ocultados e o PNG ficou sem chunks
  temporais. Estados raros e feedback transitório de refresh ficaram `N/A`
  quando não observados sem fabricar dados.
- `024-07` concluída com header nominal azul-petróleo, resumo, medição manual,
  importação D1, tendência e histórico visualmente separados; callbacks,
  intents, confirmação de saída, gráfico, períodos e dados persistidos foram
  preservados.
- `024-07` passou em `git diff --check`, lint, builds debug/release, instalação e
  smoke ADB com dados persistidos existentes; períodos, rolagem, retorno,
  landscape, fonte `1,3x` e temas claro/escuro foram exercitados. Evidência
  sanitizada registrada em `024-07-captura-real-sanitizada-heart-rate-portrait.png`.
- Em `024-07`, medição manual e importação D1 não foram iniciadas para gerar
  evidência; estados em voo, retry, cancelamento, erro e resultado parcial
  ficaram `N/A`.
- Revisão independente inicial de `024-07` encontrou contraste do hero escuro,
  ação destrutiva primária e representação de lacunas; correções foram aplicadas,
  builds/smoke foram repetidos e revisão independente final passou sem
  bloqueadores.
- `024-08` concluída com header nominal, valor atual, editor de quilogramas,
  feedback semântico, histórico separado, tratamento explícito de loading/
  indisponibilidade, fallback seguro entre coletores e `imePadding`; contratos,
  callbacks, ViewModel, persistência e timestamps foram preservados.
- `024-08` passou em `git diff --check`, lint, builds debug/release, instalação e
  smoke ADB sanitizado. Foram exercitados portrait escuro, tema claro, fonte
  `1,3x`, landscape por rotação forçada, rolagem, editor e cancelamento sem
  persistência, inclusive após correção final do fallback; aparelho restaurado
  para fonte `1,0x`, portrait e tema escuro.
- Evidência sanitizada registrada em
  `024-08-captura-real-sanitizada-weight-portrait.png`. Estados de salvamento,
  sucesso, erro, loading e vazio real ficaram `N/A` por não iniciar persistência
  artificial nem fabricar estados transitórios.
- `024-09` concluída com detalhe de passos priorizando valor diário, meta e
  tendência; gráfico recebeu barras arredondadas, grade discreta, linha de meta
  e marca compacta para lacunas, mantendo ausência diferente de zero.
- `024-09` passou em `git diff --check`, lint, builds debug/release, instalação
  debug e smoke ADB sanitizado. Foram exercitados 7D/30D/90D, rolagem, retorno,
  portrait, fonte `1,3x`, landscape forçado e temas claro/escuro; aparelho foi
  restaurado para fonte `1,0x`, portrait, tamanho físico e tema escuro.
- Evidência sanitizada registrada em
  `024-09-captura-real-sanitizada-steps-portrait.png`; nenhum teste automatizado
  foi criado ou executado. Estados loading, erro e vazio não foram fabricados e
  ficaram `N/A`.
- Revisão independente de `024-09` aprovou após correção da evidência sanitizada
  e redução da descrição do gráfico para resumo acessível; nenhum bloqueador
  permaneceu.
