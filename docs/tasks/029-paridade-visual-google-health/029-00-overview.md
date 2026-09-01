# 029 — Paridade visual com Google Health instalado

- **Status geral:** pending
- **Spec:** [029-paridade-visual-google-health.md](../../specs/029-paridade-visual-google-health.md)
- **Progresso:** 0/8 subtarefas concluídas

## Objetivo

Usar aplicativo instalado `com.fitbit.FitbitMobile` como única referência visual
e alinhar superfícies Champion, gráficos, calendário, abas e filtros com máxima
fidelidade observável, preservando contratos, dados e privacidade.

## Checklist

- [ ] [029-01-inspecionar-google-health-e-capturar-baseline.md](029-01-inspecionar-google-health-e-capturar-baseline.md)
- [ ] [029-02-registrar-decisao-visual-e-contrato-de-evidencia.md](029-02-registrar-decisao-visual-e-contrato-de-evidencia.md)
- [ ] [029-03-evoluir-fundacao-visual-e-shell.md](029-03-evoluir-fundacao-visual-e-shell.md)
- [ ] [029-04-alinhar-home-saude-e-metricas.md](029-04-alinhar-home-saude-e-metricas.md)
- [ ] [029-05-alinhar-calendario-e-detalhe-diario.md](029-05-alinhar-calendario-e-detalhe-diario.md)
- [ ] [029-06-alinhar-exercicios-abas-calendario-e-filtros.md](029-06-alinhar-exercicios-abas-calendario-e-filtros.md)
- [ ] [029-07-alinhar-dispositivo-perfil-e-configuracoes.md](029-07-alinhar-dispositivo-perfil-e-configuracoes.md)
- [ ] [029-08-validar-superficies-e-fechar.md](029-08-validar-superficies-e-fechar.md)

## Observações

- ADB foi confirmado pelo usuário no Galaxy S22 `SM-S901E`; captura nova ainda
  depende de autorização operacional para execução de `adb`.
- Evidências versionadas devem ser sanitizadas; screenshots brutos não podem
  permanecer no repositório nem virar assets do app.
- `028-10` permanece dependência para alterações no renderer de gráficos.
- Primeira subtarefa elegível: 029-01, condicionada ao acesso ADB.
