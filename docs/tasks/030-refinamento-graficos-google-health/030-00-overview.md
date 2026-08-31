# 030 — Refinamento visual integral inspirado no Google Health

- **Status geral:** `in_progress`
- **Spec:** [030-refinamento-graficos-google-health.md](../../specs/030-refinamento-graficos-google-health.md)
- **Progresso:** 2/6 subtarefas concluídas

## Objetivo

Revisar todas as superfícies produtivas atuais com referência visual do Google
Health/Fitbit: shell, navegação visual, cards, listas, estados, controles,
gráficos, cores, espaçamento, acessibilidade e comportamento de apresentação,
sem alterar dados, domínio ou integração. “Tudo” significa cobertura visual do
Champion atual, não portar funcionalidades inexistentes.

## Checklist

- [x] [030-01-auditar-e-benchmark-visual-integral-google-health.md](030-01-auditar-e-benchmark-visual-integral-google-health.md)
- [x] [030-02-definir-fundacao-visual-e-shell.md](030-02-definir-fundacao-visual-e-shell.md)
- [ ] [030-03-refinar-home-e-saude.md](030-03-refinar-home-e-saude.md)
- [ ] [030-04-refinar-calendario-e-exercicios.md](030-04-refinar-calendario-e-exercicios.md)
- [ ] [030-05-refinar-evolucao-dispositivo-perfil-e-configuracoes.md](030-05-refinar-evolucao-dispositivo-perfil-e-configuracoes.md)
- [ ] [030-06-validar-e-fechar-refinamento-visual.md](030-06-validar-e-fechar-refinamento-visual.md)

## Observações

- Specs 024 e 029 permanecem concluídas (`completed`); o trabalho da Task 028
  foi concluído e esta melhoria permanece independente do fechamento documental
  daquelas specs.
- Referência autorizada: Google Health/Fitbit (`com.fitbit.FitbitMobile`), não
  Google Fit nem Health Connect.
- A interface do Google Health/Fitbit pode ser replicada fielmente nos aspectos
  autorizados. Não copiar código-fonte ou APK, dados pessoais ou credenciais,
  nem reutilizar arquivos ou ativos extraídos do app de referência; a
  implementação deve usar código próprio no Champion Companion.
- Seletor visual de referência: `D`, `S`, `M`, `3M`, `A`; opções sem suporte
  ficam ocultas ou desabilitadas e não geram query nova.
- O plano foi ampliado de gráficos para revisão visual integral de todas as
  superfícies produtivas atuais.
- `030-01` foi concluída após conferência da matriz gráfica preservada e da nova
  matriz objetiva por destino/subsuperfície contra o código atual.
- Divergências visuais e validações físicas não exercitadas permanecem
  registradas em `030-01`; não são requisitos funcionais. A subtarefa `030-02`
  foi concluída com fundação visual opt-in, shell compartilhado e boundary de
  gráficos preservado.
- Validação física de `030-02` cobriu shell, menu Mais, navegação para Dispositivo
  e retorno em portrait/tema escuro. Fonte ampliada, landscape, tema claro,
  estados sem dados e períodos não suportados permanecem `N/A` e devem ser
  exercitados nas superfícies migradas.
- Regressão do botão `Início` foi reproduzida após a primeira implementação e
  corrigida; smoke posterior confirmou retorno de Saúde para Home por toque.
