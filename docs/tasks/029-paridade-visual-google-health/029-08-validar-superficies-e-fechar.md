# 029-08 — Validar superfícies e fechar

- **Ticker:** `029`
- **Número:** `08`
- **Status:** `pending`
- **Depende de:** `029-04`, `029-05`, `029-06` e `029-07`

## Objetivo e resultado esperado

Consolidar rollout visual, repetir smoke integrado e fechar a documentação com
matriz, screenshots sanitizados, limitações e resultados reais.

## Requisitos e critérios de aceite cobertos

- R1 a R9 da Spec 029.
- Todos os critérios de aceite, com foco em não regressão, evidência e
  preservação de contratos.

## Escopo incluído

- revisão de diff e busca de renderer/import proibido;
- smoke integrado de todas as superfícies, cards, períodos, abas e filtros;
- validação de tema, orientação, fonte, acessibilidade e gestos;
- auditoria de screenshots, nomes, metadados e dados sensíveis;
- atualização da Spec 029, overview, decisão arquitetural e evidências com fatos
  finais.

## Escopo excluído

- novas funcionalidades, refatorações não relacionadas ou mudança de domínio;
- criação de testes automatizados;
- persistência de screenshot bruto;
- alterar histórico de specs concluídas sem correção factual necessária.

## Dependências

- todas as subtarefas anteriores concluídas;
- ADB autorizado para aceite físico;
- build e lint disponíveis;
- matriz 029-01 e decisão de 029-02.

## Arquivos e símbolos prováveis

- `docs/specs/029-paridade-visual-google-health.md`;
- `docs/tasks/029-paridade-visual-google-health/029-00-overview.md`;
- `docs/decisions/adr-028-paridade-visual-com-referencia-instalada.md`;
- evidências sanitizadas da pasta da task;
- `core/designsystem/chart`, `ChampionNavigation` e contratos de cada feature.

## Passos de implementação

1. Auditar checklist contra arquivos e matriz, sem marcar cobertura presumida.
2. Executar `git diff --check` e build completo.
3. Instalar debug e repetir smoke de todas as superfícies e combinações
   alcançadas.
4. Validar cards, filtros, tabs Semana/Mês, calendário mensal, gráficos,
   marker/tooltip, gaps, vazio, rolagem e retorno.
5. Revisar acessibilidade, tema, orientação e fonte ampliada.
6. Sanitizar e auditar evidências; remover temporários.
7. Registrar riscos residuais, `N/A`, desvios e resultado final.
8. Atualizar overview somente após cada subtarefa estar realmente concluída.

## Testes e comandos de validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados. Registrar smoke ADB sanitizado e
resultado de cada superfície.

## Definição de pronto

- todos os itens anteriores têm evidência ou limitação explícita;
- overview reflete progresso real e pode ser marcado `8/8` somente com todas as
  subtarefas concluídas;
- spec e decisão refletem implementação real;
- nenhum screenshot bruto, dado de saúde ou identificador permanece;
- build, lint e smoke integrado passam sem regressão bloqueante;
- status final e riscos residuais estão documentados.

## Riscos e cuidados

- ADB indisponível mantém task não concluída, mesmo com build verde.
- Não ocultar falha de filtro, gesto, acessibilidade ou estado vazio.
- Não marcar subtarefa como concluída por compilação sem evidência de escopo.
- Revalidar que nenhuma tela passou a depender de rede, Health Connect readback
  ou dados sintéticos.
