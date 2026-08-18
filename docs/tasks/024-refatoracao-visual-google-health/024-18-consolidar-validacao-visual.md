# 024-18 — Consolidar validação visual

- **Ticker:** `024`
- **Número:** `18`
- **Status:** pending

## Objetivo e resultado esperado

Consolidar componentes após todas as telas, remover somente legado visual sem
consumidores e executar smoke integrado. Resultado: linguagem consistente,
documentação fechada e evidência física completa.

## Requisitos e critérios cobertos

- R1–R10; todos os critérios de aceite da spec.

## Escopo incluído

- inventário final de componentes e consumidores;
- promoção de duplicações comprovadas e remoção segura de legado visual;
- revisão integrada de navegação, telas, acessibilidade e privacidade;
- evidência final, spec, ADR e overview.

## Escopo excluído

- refatoração funcional, nova feature, novo teste ou limpeza de legado phone;
- alteração de dados, dependências, arquitetura ou roadmap.

## Dependências

- `024-01` a `024-17` concluídas;
- aceite individual das treze superfícies;
- dispositivo ADB disponível.

## Arquivos e símbolos prováveis

- `core/designsystem/*` tocado pela task;
- telas migradas somente para adoção final estritamente necessária;
- `docs/specs/024-refatoracao-visual-google-health.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`;
- ADR visual criada em `024-01`;
- evidência final na pasta da task.

## Passos de implementação

1. Listar componentes novos/antigos e todos os consumidores.
2. Promover somente duplicações com dois consumidores reais e API simples.
3. Remover somente símbolos visuais sem uso comprovado.
4. Revisar diff total contra contratos, ADRs e limites de marca/privacidade.
5. Executar smoke ADB de shell e treze superfícies.
6. Validar amostra proporcional em claro/escuro, dynamic/fallback, fonte `1,3x`
   e landscape.
7. Restaurar configurações do aparelho e sanitizar evidências.
8. Atualizar spec, ADR e overview com resultados e riscos residuais.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke final: Início; Saúde e quatro detalhes; Calendário e dia; Exercícios e
detalhe; Dispositivo; Perfil; Configurações; menu `Mais`; back/restauração.

## Definição de pronto

- treze superfícies e shell possuem evidência sanitizada;
- navegação, callbacks e contratos permanecem equivalentes;
- componentes sem uso foram removidos somente com prova;
- lint, builds, instalação e smoke aprovados;
- configurações do aparelho restauradas;
- spec/ADR/overview refletem fechamento e progresso `18/18`.

## Riscos e cuidados

- consolidação pode introduzir regressão transversal: manter diff mínimo;
- não tratar estado não observado como aprovado;
- não executar suites automatizadas por conveniência;
- nenhum fechamento sem ADB físico e aceite das telas.
