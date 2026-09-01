# 029 — Paridade visual com Google Health instalado

- **Status geral:** in_progress
- **Spec:** [029-paridade-visual-google-health.md](../../specs/029-paridade-visual-google-health.md)
- **Progresso:** 1/8 subtarefas concluídas

## Objetivo

Usar aplicativo instalado `com.fitbit.FitbitMobile` como única referência visual
e alinhar superfícies Champion, gráficos, calendário, abas e filtros com máxima
fidelidade observável, preservando contratos, dados e privacidade.

## Checklist

- [x] [029-01-inspecionar-google-health-e-capturar-baseline.md](029-01-inspecionar-google-health-e-capturar-baseline.md)
- [ ] [029-02-registrar-decisao-visual-e-contrato-de-evidencia.md](029-02-registrar-decisao-visual-e-contrato-de-evidencia.md)
- [ ] [029-03-evoluir-fundacao-visual-e-shell.md](029-03-evoluir-fundacao-visual-e-shell.md)
- [ ] [029-04-alinhar-home-saude-e-metricas.md](029-04-alinhar-home-saude-e-metricas.md)
- [ ] [029-05-alinhar-calendario-e-detalhe-diario.md](029-05-alinhar-calendario-e-detalhe-diario.md)
- [ ] [029-06-alinhar-exercicios-abas-calendario-e-filtros.md](029-06-alinhar-exercicios-abas-calendario-e-filtros.md)
- [ ] [029-07-alinhar-dispositivo-perfil-e-configuracoes.md](029-07-alinhar-dispositivo-perfil-e-configuracoes.md)
- [ ] [029-08-validar-superficies-e-fechar.md](029-08-validar-superficies-e-fechar.md)

## Observações

- ADB foi confirmado e usado no Galaxy S22 `SM-S901E`; novas capturas dependem
  do aparelho permanecer desbloqueado e autorizado.
- Evidências versionadas devem ser sanitizadas; screenshots brutos não podem
  permanecer no repositório nem virar assets do app.
- `028-10` permanece dependência para alterações no renderer de gráficos.
- Primeira subtarefa elegível: 029-01; ADB TLS foi reconectado no Galaxy S22.
- 029-01 está `completed`: superfícies seguras e os sete filtros foram
  percorridos, mas R8 exige smoke físico completo e o paralelo Semana/Mês do
  Champion ainda está ausente no APK instalado.
- Portrait escuro padrão, tema claro e fonte `1.3` foram observados nas
  superfícies principais. Solicitação de landscape carregou a referência, mas o
  Champion permaneceu em portrait. Cobertura tela a tela de variantes, retorno,
  acessibilidade, marker, tooltip e rolagem horizontal permanece pendente;
  checklist continua desmarcado.
