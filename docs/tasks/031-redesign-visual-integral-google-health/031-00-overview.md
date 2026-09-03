# 031 — Redesign visual integral baseado no Google Health/Fitbit

- **Status geral:** pending
- **Spec:** [031-redesign-visual-integral-google-health.md](../../specs/031-redesign-visual-integral-google-health.md)
- **Progresso:** 1/11 subtarefas concluídas

## Objetivo

Reconstruir apresentação visual do Champion Companion com fidelidade máxima à
interface observada de `com.fitbit.FitbitMobile`, mantendo features, dados,
contratos, privacidade e identidade não proprietária do app. Barra inferior terá
somente Home e Saúde; rotas restantes serão contextuais.

## Checklist

- [x] [031-01-auditar-referencia-e-baseline.md](031-01-auditar-referencia-e-baseline.md)
- [ ] [031-02-registrar-supersessao-e-decisao.md](031-02-registrar-supersessao-e-decisao.md)
- [ ] [031-03-evoluir-fundacao-visual.md](031-03-evoluir-fundacao-visual.md)
- [ ] [031-04-reorganizar-shell-e-navegacao.md](031-04-reorganizar-shell-e-navegacao.md)
- [ ] [031-05-redesenhar-home.md](031-05-redesenhar-home.md)
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
  Progresso atual: `1/11`.
