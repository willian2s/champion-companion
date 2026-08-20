# 024-18 — Consolidar validação visual

- **Ticker:** `024`
- **Número:** `18`
- **Status:** completed

## Objetivo e resultado esperado

Consolidar componentes após todas as telas, remover somente legado visual sem
consumidores e executar smoke integrado. Resultado: linguagem consistente,
documentação fechada e evidência física completa.

## Requisitos e critérios cobertos

- R1–R10; todos os critérios de aceite da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

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
- `024-19` concluída;
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
- spec/ADR/overview refletem fechamento e progresso `19/19`.

## Riscos e cuidados

- consolidação pode introduzir regressão transversal: manter diff mínimo;
- não tratar estado não observado como aprovado;
- não executar suites automatizadas por conveniência;
- nenhum fechamento sem ADB físico e aceite das telas.

## Execução registrada

### Status

`completed`; consolidação visual, validação integrada e fechamento documental
concluídos sem alteração funcional.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/designsystem/ChampionTokens.kt`;
- `docs/tasks/024-refatoracao-visual-google-health/evidencia-final-validacao-024-18.md`;
- `docs/specs/024-refatoracao-visual-google-health.md`;
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-18-consolidar-validacao-visual.md`.

### Decisões e desvios

- Inventário confirmou uso real múltiplo de `HealthCard`, `MetricCard`, estados
  de feedback, `PrimaryActionButton` e `SyncStatusBadge`; nenhuma promoção nova
  foi necessária.
- Headers e cards locais permaneceram locais por falta de equivalência simples
  comprovada em duas superfícies.
- Removidos somente `LocalChampionSpacing` e `LocalChampionElevation`, sem
  consumidores em código de produção, previews ou testes.
- `HealthChart`, `CalibrationBanner`, `ConnectionStatus` e
  `ActivityStatusBadge` foram preservados por cobertura de previews/testes;
  removê-los exigiria alterar testes, proibido nesta subtarefa.
- Nenhum arquivo de feature, navegação, tema raiz, domínio, persistência, BLE,
  Health Connect ou legado phone foi alterado.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| Auditoria de consumidores reais | Passou; inventário registrado na evidência final |
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Activity aberta |
| Smoke ADB integrado | Passou; shell, treze superfícies, detalhes, menu `Mais`, back e restauração |
| Smoke final claro, fonte `1,3x`, landscape e portrait escuro restaurado | Passou; PNGs transitórios confirmaram renderização em `2340x1080` e `1080x2340`, sem persistência |
| Fallback físico sem dynamic color inferior à API 31 | `N/A`; aparelho API 36 e sem emulador |
| Testes automatizados | Não executados, conforme R8 |

### Evidências

- `evidencia-final-validacao-024-18.md` consolida inventário, smoke, privacidade
  e riscos residuais.
- Capturas individuais sanitizadas `024-03`, `024-05` a `024-17` e `024-19`
  permanecem como evidência física das superfícies e shell.
- Nenhum dump ou screenshot bruto foi versionado.

### Riscos residuais

- Fallback físico inferior à API 31 não foi observado.
- Estados transitórios e operações com efeitos não foram disparados e permanecem
  `N/A`.
- Componentes sem consumidor de superfície permanecem por cobertura de
  previews/testes; nova remoção exigiria escopo próprio para atualizar contratos.
- Revisão independente final: `PASS`; nenhum bloqueador ou achado importante.
