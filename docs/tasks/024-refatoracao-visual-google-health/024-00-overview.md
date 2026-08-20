# 024 — Refatoração visual inspirada no Google Health

- **Status geral:** in_progress
- **Spec:** [024-refatoracao-visual-google-health.md](../../specs/024-refatoracao-visual-google-health.md)
- **Progresso:** 18/19 subtarefas concluídas

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
- [x] [024-10-refatorar-distancia.md](024-10-refatorar-distancia.md)
- [x] [024-11-refatorar-calendario.md](024-11-refatorar-calendario.md)
- [x] [024-12-refatorar-detalhe-diario.md](024-12-refatorar-detalhe-diario.md)
- [x] [024-13-refatorar-lista-exercicios.md](024-13-refatorar-lista-exercicios.md)
- [x] [024-14-refatorar-detalhe-exercicio.md](024-14-refatorar-detalhe-exercicio.md)
- [x] [024-15-refatorar-dispositivo.md](024-15-refatorar-dispositivo.md)
- [x] [024-16-refatorar-perfil.md](024-16-refatorar-perfil.md)
- [x] [024-17-refatorar-configuracoes.md](024-17-refatorar-configuracoes.md)
- [x] [024-19-colorir-selecao-navegacao.md](024-19-colorir-selecao-navegacao.md)
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
- `024-19` implementou seleção cromática nominal no shell, sem reabrir o piloto
  Saúde `024-05`.
- `024-19` concluída: indicador, ícone e rótulo selecionados usam roles nominais
  `primary`/`primaryContainer` sem alterar máquina de navegação; smoke ADB,
  builds, instalação, captura sanitizada e revisão independente passaram.
- Fallback físico sem dynamic color ficou `N/A`: aparelho disponível é API 36 e
  o usuário dispensou emulador; paletas nominais foram inspecionadas estaticamente
  sem declarar sucesso físico.
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
- `024-10` concluída após conexão do Galaxy S22 `SM-S901E`: header nominal, valor
  diário estimado, origem, seletor 7D/30D/90D, gráfico com grade e lacunas
  preservadas. Smoke físico cobriu portrait escuro, tema claro, fonte `1,3x`,
  landscape, rolagem, retorno e captura sanitizada; loading, erro e vazio ficaram
  `N/A` sem fabricação.
- `024-11` concluída após decisão explícita do usuário por preservar dimensões
  originais: Calendário recebeu header nominal, controles mensais com ícones,
  grade de sete colunas/42 células, espaçamento entre datas, marcadores
  simplificados, seleção/hoje separados, CTA priorizado e legenda compacta.
  Bordas e margens foram alinhadas às demais superfícies. Exceção R5 foi
  registrada na spec para o viewport portrait de 360 dp. Smoke cobriu mês
  anterior/próximo, `Hoje`, seleção, abertura do dia, retorno, fonte `1,3x`,
  tema claro, landscape e restauração; captura sanitizada válida foi registrada.
   Contraste dos pontos foi corrigido pela variante nominal adequada ao fundo da
   célula, com nova validação e captura sanitizada.
- `024-12` concluída com panorama diário antes de métricas detalhadas, header
  nominal, acentos por métrica, problemas por fonte e fila informativa. Atalhos
  de Saúde/Exercícios, retorno contextual, estados seguros e status por
  atividade foram preservados. Evidência sanitizada registrada em
  `024-12-captura-real-sanitizada-detalhe-diario.png`.
- `024-12` passou em `git diff --check`, compilação Kotlin, lint, builds
  debug/release, instalação e smoke ADB. Foram exercitados dia completo,
  rolagem, Passos → retorno, fonte `1,3x`, landscape e temas claro/escuro; o
  aparelho foi restaurado para fonte `1,0x`, portrait e tema escuro. Loading,
  erro, data inválida, ausência e problemas parciais ficaram `N/A` sem
  fabricação. Nenhum teste automatizado foi criado ou executado.
- `024-13` concluída com header nominal, CTA explícita, progresso/retry, filtros,
  cards compactos e Health Connect secundário. Bloco `Pendências e fluxo` foi
  removido por solicitação do usuário; lista recebeu paginação de cinco itens,
  âncora de título ao trocar página, `DirectionsBike` para bicicleta e título
  `Todos os treinos`. Contratos, cancelamento, deduplicação, detalhe e sync foram
  preservados. `git diff --check`, compilação Kotlin, lint, builds debug/release,
  instalação e smoke ADB passaram com fonte `1,3x`, portrait escuro, landscape
  claro e restauração do aparelho. Evidência sanitizada registrada em
  `024-13-captura-real-sanitizada-exercicios.png`; nenhuma importação foi
  iniciada. Revisão independente final aprovada sem bloqueadores.
- `024-14` concluída após revisão independente e nova validação física. Header separado e retorno por ícone acessível, badges
  tonais nominais, card `Status e origem` consolidando workflow, tipo, parser,
  origem e derivação, além de seções claras para resumo, tendências,
  detalhamento, revisão e workflow. Callbacks, guards, dialogs, snapshots e
  sync foram preservados.
- `024-14` passou em `git diff --check`, compilação Kotlin, lint, builds
  debug/release, instalação e smoke ADB sanitizado. Evidência registrada em
  `024-14-captura-real-sanitizada-detalhe-exercicio.png`.
- Em `024-14`, estados transitórios de operação ficaram `N/A` sem fabricar dados;
  fonte `1,3x`, portrait, landscape e temas claro/escuro com dynamic colors
  padrão foram repetidos após o ajuste final. Revisão independente final aprovada
  sem bloqueadores. Nenhum bloqueio técnico ativo.
- `024-15` concluída após compactação solicitada pelo usuário: o estado de
  conexão aparece uma única vez no resumo, o card associado não repete status ou
  timestamps, e descoberta, conexão, manutenção e diagnóstico foram separados.
- `024-15` passou em `git diff --check`, compilação Kotlin, lint, builds
  debug/release, instalação e smoke ADB sanitizado. Foram exercitados portrait,
  rolagem, fonte `1,3x`, landscape, temas claro/escuro, abertura/cancelamento de
  diagnóstico e confirmação de esquecer sem confirmar. Evidência registrada em
  `024-15-captura-real-sanitizada-dispositivo.png`; revisão independente final
  aprovada sem bloqueadores.
- `024-16` concluída com foco no passo base e métricas editáveis; perfis aprendidos
  e histórico de calibração foram retirados do fluxo principal e organizados em
  modais separados. RUNNING/WALKING, provenance, correção, remoção confirmada e
  abertura de atividade relacionada foram preservados.
- Após feedback visual, valor atual, sugestão por altura e edição manual de
  comprimento do passo foram reunidos em uma única seção contextual, com
  subseções e divisores; a tela principal deixou de repartir essa informação em
  cards desconexos.
- `024-16` passou em `git diff --check`, compilação Kotlin, lint, builds
  debug/release, instalação e smoke ADB. Foram exercitados modais, cancelamento
  de sugestão, abertura/retorno de atividade, precisão máxima de quatro casas,
  fonte `1,3x`, landscape e temas claro/escuro; aparelho restaurado para fonte
  `1,0x`, portrait e tema escuro. Evidência registrada em
  `024-16-captura-real-sanitizada-perfil.png`.
- Revisão independente inicial de `024-16` encontrou arredondamento visual
  contaminando campo editável e unidade duplicada no resumo de distância.
  Formatter separado para entrada preserva precisão persistida; unidade duplicada
  foi removida; compilação, lint, builds e smoke final foram repetidos.
- Revisão independente final de `024-16` aprovada sem bloqueadores; confirmou
  agrupamento contextual, modais, precisão, unidade única e checklist SDD.
- Nenhum teste automatizado foi criado ou executado em `024-16`; revisão
  independente final concluída.
- `024-17` concluída com header nominal azul-petróleo, hierarquia de integração,
  ações empilhadas, permissões e fila local reorganizadas. Toggle, launcher,
  provider update, gerenciar acesso e sincronização write-only foram preservados.
- `024-17` passou em `git diff --check`, lint, builds debug/release, instalação e
  smoke ADB sanitizado. Foram exercitados abertura, rolagem, fonte `1,3x`,
  landscape e temas claro/escuro; aparelho restaurado para fonte `1,0x`, portrait
  e tema escuro. Evidência registrada em
  `024-17-captura-real-sanitizada-configuracoes.png`.
- Em `024-17`, `Gerenciar acesso` não abriu tela externa no provider disponível;
  callback permaneceu seguro, sem mutação, e retorno real ficou `N/A`. Provider
  update, indisponibilidade, sync remoto e estados transitórios também ficaram
  `N/A` sem fabricação.
- Revisão independente de `024-17` encontrou bloqueio documental e melhoria
  preventiva no filtro de permissões; documentação foi registrada e o filtro foi
  removido antes da validação final. Revisão final permanece solicitada.
