# 031 — Redesign visual integral baseado no Google Health/Fitbit

- **Status geral:** in_progress
- **Spec:** [031-redesign-visual-integral-google-health.md](../../specs/031-redesign-visual-integral-google-health.md)
- **Progresso:** 5/11 subtarefas concluídas

## Objetivo

Reconstruir apresentação visual do Champion Companion com fidelidade máxima à
interface observada de `com.fitbit.FitbitMobile`, mantendo features, dados,
contratos, privacidade e identidade não proprietária do app. Barra inferior terá
somente Home e Saúde; rotas restantes serão contextuais.

## Checklist

- [x] [031-01-auditar-referencia-e-baseline.md](031-01-auditar-referencia-e-baseline.md)
- [x] [031-02-registrar-supersessao-e-decisao.md](031-02-registrar-supersessao-e-decisao.md)
- [x] [031-03-evoluir-fundacao-visual.md](031-03-evoluir-fundacao-visual.md)
- [x] [031-04-reorganizar-shell-e-navegacao.md](031-04-reorganizar-shell-e-navegacao.md)
- [x] [031-05-redesenhar-home.md](031-05-redesenhar-home.md)
- [ ] [031-06-redesenhar-saude-e-metricas.md](031-06-redesenhar-saude-e-metricas.md)
- [ ] [031-07-redesenhar-exercicios-e-calendario.md](031-07-redesenhar-exercicios-e-calendario.md)
- [ ] [031-08-redesenhar-perfil.md](031-08-redesenhar-perfil.md)
- [ ] [031-09-redesenhar-dispositivo.md](031-09-redesenhar-dispositivo.md)
- [ ] [031-10-redesenhar-configuracoes.md](031-10-redesenhar-configuracoes.md)
- [ ] [031-11-consolidar-validacao-e-revisao.md](031-11-consolidar-validacao-e-revisao.md)

## Observações

- Ticker `031` escolhido porque auditoria encontrou `029` e `030` reservados em
  trabalhos/branches existentes; nenhum arquivo `031` foi sobrescrito.
- Spec 024 e ADR-023 permanecem como histórico supersedido.
- Evidência sem sanitização não será versionada; regra de privacidade prevalece.
  Exceção temporária permite somente inspeção local no aparelho autorizado,
  durante a sessão, sem envio ao contexto, cloud ou logs.
- Material não sanitizado, quando indispensável para o layout, deve ser apagado
  ao fim de cada sessão. `031-11` deve confirmar limpeza de todas as sessões;
  ausência de confirmação bloqueia encerramento e exige rollback ao baseline
  sanitizado.
- A revisão hierárquica considerou a exceção compatível apenas por ser local,
  efêmera e sem transporte de dados; `AGENTS.md` e ADR-004 continuam
  prevalecendo. Conflito impeditivo deve ficar registrado em `Observações`.
- Primeira subtarefa elegível: `031-01`.
- `031-01` foi concluída após revisão independente aprovada com ressalvas não
  bloqueantes. Reexecução ADB sanitizada em `SM-S901E` confirmou pacote, versão,
  Activity e estrutura parcial; superfícies não alcançadas permanecem `N/A`;
  estado da Activity retornou à Home e nenhuma configuração exigiu restauração.
  Após a conclusão de `031-01`, o progresso registrado era `1/11`.
- `031-02` foi concluída em escopo documental: Spec 024 e ADR-023 permanecem
  preservadas como histórico, com supersessão limitada à direção visual e à
  organização/apresentação da navegação. ADR-028 foi confirmada como `aceita`,
  substituta de ADR-023 e vinculada à Spec 031. A primeira revisão independente
  identificou marcação antecipada de conclusão; após correção, a segunda revisão
  foi aprovada em 2026-09-03. `031-03` foi concluída após implementação
  aditiva e opt-in da fundação visual, com revisão independente aprovada em
  2026-09-03. `031-04` foi concluída após reorganização do shell, rotas
  contextuais, preservação do saver/back stack e revisão independente aprovada.
  Smoke ADB sanitizado confirmou os fluxos principais e restaurou configurações
  do aparelho. Progresso atual: `4/11`.
- `031-05` teve ajuste visual implementado em `HomeScreen.kt` e foi concluída
  após revisão independente aprovada. O dashboard usa a geometria sanitizada
  observada, sem alegar paridade de conteúdo além da evidência disponível.
- `031-05` foi refinada conforme inspeção estrutural da Home de referência: card
  maior de Passos e linhas laterais para Exercícios, Distância e Freq. cardíaca.
  Calorias foi substituída por frequência cardíaca e a fonte de dados foi
  removida por solicitação explícita.
  Contagem semanal de exercícios com meta 4 ficou bloqueada por ausência de
  contrato e por estar fora do escopo visual; nenhuma contagem semanal sintética
  foi criada.
  Cards permanecem clicáveis sem setas visuais, com navegação para os detalhes
  existentes. Link de Calendário foi removido da Home por solicitação explícita;
  pull-to-refresh foi preservado com contêiner rolável compatível com o gesto.
  Estados `Loading`/`Empty` usam a mesma composição com zeros visuais; erros
  acionáveis continuam preservados. Card inferior aparece somente para revisão
  pendente. `031-05` foi concluída após revisão independente aprovada: o
  dashboard mantém zero visual sem resumo, linhas responsivas à escala de fonte,
  retry acionável e card inferior somente para revisão pendente. Smoke final
  usou variante debug `br.com.mwconsulting.championcompanion.dev`; não confundir
  com pacote release sem sufixo. Progresso atual: `5/11`.
