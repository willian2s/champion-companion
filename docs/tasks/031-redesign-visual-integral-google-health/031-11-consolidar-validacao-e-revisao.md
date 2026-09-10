# 031-11 — Consolidar validação e revisão final

- **Ticker:** `031`
- **Número:** `11`
- **Status:** completed

## Objetivo

Consolidar todas as superfícies, remover apenas legado visual sem uso e produzir
prova final de navegação, comportamento preservado, privacidade e fidelidade.

## Requisitos e aceite

- Todas as subtarefas anteriores têm revisão independente aprovada.
- Barra tem somente Home/Saúde; toda rota continua alcançável.
- Features e contratos funcionais permanecem equivalentes.
- Evidências são sanitizadas e não existem dumps brutos versionados.
- Diff final não contém logo, fonte, asset ou feature proprietária.
- Se a exceção efêmera da Spec 031 tiver sido usada, inventário sanitizado lista
  cada sessão, confirma descarte individual e confirma ausência final de
  temporários brutos.

## Escopo

Inclui inventário final, limpeza comprovada, smoke integrado, documentação e
revisão. Exclui nova superfície, nova métrica, refactor não relacionado e
execução de ações reais só para produzir screenshot.

## Dependências

- `031-01` a `031-10` concluídas.
- ADR-028, Spec 031 e todos os contratos funcionais revisados.

## Arquivos e símbolos prováveis

- Toda documentação em `docs/tasks/031-redesign-visual-integral-google-health/`.
- Primitives sem consumidores e superfícies migradas identificadas por diff.
- `ChampionNavigation.kt` e `AppDestination.kt` para auditoria final.

## Passos

1. Executar inventário de consumidores antes de apagar legado.
2. Repetir smoke integrado de Home, Saúde e rotas contextuais.
3. Conferir acessibilidade, temas, landscape e fonte ampliada.
4. Se aplicável, conferir inventário das sessões excepcionais, descarte de cada
   temporário e ausência final no aparelho autorizado, sem registrar conteúdo
   bruto.
5. Atualizar spec, overview e subtarefas com evidências e riscos.
6. Solicitar revisão independente; não iniciar próxima task automaticamente.

## Validação e comandos

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
```

Smoke ADB sanitizado e relatório final. Não criar ou executar testes automatizados.

## Definição de pronto

Spec/overview refletem progresso real, checklist marca somente subtarefas
concluídas, builds/lint passam, evidência permitida está versionada e revisão
independente retorna `PASS` sem bloqueadores. Quando a exceção foi usada,
inventário sanitizado contém confirmação de descarte por sessão e confirmação
final de que nenhum temporário bruto permanece no aparelho autorizado.

## Riscos

Diferenças de versão da referência podem impedir equivalência absoluta. Registrar
limites observados e não declarar sucesso além da evidência disponível.

## Consolidação da execução — 2026-09-10

- **Status da execução:** `completed`; revisão independente final aprovada.
- **Consistência documental:** Spec, ADR, overview e subtarefas usam ticker `031`;
  overview contém uma única seção `Checklist`, com onze itens, e registra `10/11`
  antes desta subtarefa. `031-01` a `031-10` estão com status `completed` e cada
  uma registra revisão independente aprovada, incluindo a aprovação terminal de
  `031-07` e a correção aprovada de `031-10`.
- **Auditoria de navegação:** `primaryItems` contém somente `Home` e `Saúde`.
  Home abre Dispositivo, Perfil, Exercícios e os detalhes de métricas; Saúde abre
  Exercícios e os quatro detalhes; Perfil abre Configurações; Exercícios abre o
  detalhe da atividade e mantém calendário, resumo e lista mensal na aba `Mês`.
  `Calendar`/`CalendarDay` permanecem serializáveis e renderizáveis somente para
  estado legado/restaurado, conforme Spec 031. Rotas de treino phone continuam
  protegidas por `PhoneWorkoutPolicy` e não são declaradas como fluxo de produto.
- **Inventário de legado visual:** `ConnectionStatus`, `SyncStatusBadge` e
  `ActivityStatusBadge` não têm consumidor de produção, mas continuam usados por
  previews e por `ChampionActionAndStatusComponentsTest`; não foram apagados para
  evitar quebra de consumidores existentes. Demais primitives e gráficos têm
  consumidores reais identificados. Nenhuma limpeza adicional segura foi
  necessária.
- **Auditoria de propriedade:** o diff da Spec 031 contém somente Kotlin e
  documentação. Não adiciona logo, fonte externa, imagem, asset ou dependência
  proprietária. A tipografia do redesign usa `FontFamily.Default`; nenhum recurso
  Fitbit/Google foi incorporado.

## Inventário sanitizado das sessões excepcionais

Inventário individual dos registros que mencionam inspeção da referência ou
temporário estrutural durante `031`. Nenhuma linha retém conteúdo bruto, conta,
métrica pessoal, serial ou identificador pessoal. Quando o registro histórico não
reteve o caminho exato, isso é indicado explicitamente, sem inventar um caminho.

| Sessão registrada | Aparelho/escopo sanitizado | Artefato e descarte individual |
|---|---|---|
| `031-01`, 2026-09-03 | `SM-S901E`; referência, pacote, Activity e estrutura, somente leitura | `/sdcard/window.xml`; removido e `test ! -e` confirmado; nenhuma captura bruta criada |
| `031-05`, smoke Home | `SM-S901E`; Home, rolagem, refresh e retornos contextuais | Dump estrutural temporário; caminho não retido no registro histórico; remoção registrada e nenhum arquivo versionado |
| `031-06`, 2026-09-04, métricas | `SM-S901E`; Saúde, FC, Passos e Distância, somente leitura | Dumps/capturas temporários; caminhos não retidos; descarte registrado ao fim da sessão |
| `031-06`, 2026-09-08, Peso | `SM-S901E`; Saúde → Peso, somente leitura | Dump estrutural temporário; caminho não retido; remoção e ausência no aparelho registradas |
| `031-06`, 2026-09-10, validação física posterior | `SM-S901E`; Saúde → Frequência cardíaca, menu e retorno, sem importação D1 | Nenhum dump/screenshot/arquivo temporário bruto criado nesta sessão; descarte não aplicável |
| `031-07`, 2026-09-08, referência Exercícios | `SM-S901E`; filtros e calendário da referência, somente leitura | Dump estrutural temporário; caminho não retido; removido e ausência confirmada |
| `031-07`, 2026-09-09, detalhe da referência | `SM-S901E`; detalhe de atividade da referência, somente leitura | Dump/screenshot temporários de inspeção; caminhos não retidos; removidos, sem versionamento |
| `031-07`, 2026-09-09, detalhe local após reconexão | `SM-S901E`; Exercícios → atividade local, somente leitura | XML estrutural temporário; caminho não retido; removido e ausência confirmada |
| `031-07`, 2026-09-09, gráfico período D | `SM-S901E`; detalhe local, inspeção visual do gráfico | Dump/screenshot temporários; caminhos não retidos; removidos, sem versionamento |
| `031-07`, 2026-09-09, estado final do gráfico | `SM-S901E`; detalhe local e retorno, somente leitura | Dump/screenshot temporários; caminhos não retidos; removidos, sem versionamento |
| `031-07`, 2026-09-09, duração real no gráfico | `SM-S901E`; detalhe local, somente leitura | Dump estrutural temporário; caminho não retido; removido e ausência confirmada |
| `031-07`, 2026-09-10, smoke do botão voltar | `SM-S901E`; abertura de Exercícios e retorno à Home | XML estrutural temporário; caminho não retido; removido após inspeção |
| `031-07`, 2026-09-10, validação física posterior | `SM-S901E`; Exercícios, calendário contextual e retorno | XML estrutural temporário; caminho não retido; removido e ausência confirmada; PoC aberta inadvertidamente foi encerrada sem captura de rota, operação ou persistência |
| `031-07`, 2026-09-10, validação ampliada | `SM-S901E`; fonte ampliada, landscape, temas e TalkBack, sem ação de dados | Nenhum dump, screenshot ou arquivo temporário criado; descarte não aplicável; configurações restauradas |
| `031-07`, 2026-09-10, popup terminal | `SM-S901E`; estados terminais da importação, sem iniciar operação real | Nenhum dump, screenshot ou arquivo temporário bruto criado; descarte não aplicável |
| `031-09`, 2026-09-10 | `SM-S901E`; Dispositivo, estados e retorno, sem operação BLE | Dumps XML temporários; caminho não retido; removidos; nenhum screenshot ou dado bruto versionado |
| `031-11`, 2026-09-10, smoke final | `SM-S901E`; inicialização e navegação básica do Champion | `/sdcard/031-11-smoke.xml`; removido e ausência confirmada; nenhum screenshot criado |

`031-02`, `031-03`, `031-04`, `031-08` e `031-10` não registram uso
da exceção nem criação de material bruto temporário; por isso ficam explicitamente
fora do inventário de sessões excepcionais. Em particular, a validação de
`031-10` não registra dump ou screenshot temporário.

- **Confirmação final de limpeza:** os caminhos conhecidos da execução final e da
  auditoria da referência (`/sdcard/window.xml` e
  `/sdcard/031-11-smoke.xml`) foram conferidos no aparelho autorizado e estão
  ausentes; todos os demais registros acima confirmam descarte individual sem
  retenção do caminho. Não há dump, screenshot ou dado bruto novo no workspace.
- **Ações proibidas não executadas:** nenhuma importação, sincronização, medição,
  exclusão, edição, login, troca de conta, concessão de permissão ou operação BLE
  foi iniciada para produzir evidência desta consolidação.

## Validação integrada e evidências

- **Comandos executados:** `git diff --check`; `./gradlew lint assembleDebug
  assembleRelease --no-daemon`; `./gradlew installDebug --no-daemon`;
  `adb devices -l`; inicialização explícita da `MainActivity` Debug; dump ADB
  temporário com descarte no aparelho; limpeza do Logcat, reinicialização da
  Activity e consulta sanitizada de processo/ausência de `FATAL EXCEPTION`.
- **Resultados:** `git diff --check`, lint, assemble Debug, assemble Release e
  instalação Debug passaram. `adb devices -l` identificou `SM-S901E`; Activity
  iniciou, processo permaneceu ativo e a consulta posterior não encontrou
  `FATAL EXCEPTION`. Dump temporário teve `2515` bytes somente no aparelho e foi
  removido; a ausência foi confirmada.
- **Cobertura integrada:** auditoria estática confirmou barra de duas áreas,
  back stack manual, saver, entradas contextuais, destinos de métricas,
  Exercícios, Perfil, Dispositivo e Configurações. Evidências físicas anteriores
  das subtarefas cobrem rolagem, retorno, fonte ampliada, temas, calendário
  contextual, detalhe de atividade e estados visuais. Nenhuma ação sensível foi
  repetida somente para produzir screenshot.
- **Testes automatizados:** não criados nem executados, conforme `AGENTS.md` e
  escopo da Spec 031.
- **Arquivos alterados nesta subtarefa:** este arquivo, o overview `031-00` e a
  documentação `031-07`, somente para fechamento, inventário e sanitização. Nenhum
  arquivo de produção ou contrato funcional foi alterado.

## Revisão independente

Revisão independente final solicitada após esta consolidação. Resultado: `PASS`,
sem bloqueadores. A revisão confirmou inventário, sanitização, barra de duas áreas,
rotas contextuais, ausência de ativos proprietários e coerência do checklist.

**Aceite final:** subtarefa concluída em 2026-09-10. Nenhuma próxima subtarefa foi
iniciada.

## Riscos residuais da consolidação

- TalkBack completo não foi exercitado nesta sessão final; evidências anteriores
  também têm ressalvas de cobertura.
- Landscape efetivo permaneceu limitado pela orientação do aparelho; temas e
  fonte ampliada têm cobertura distribuída nas subtarefas, não nova matriz final.
- A referência visual varia por versão, região, conta e testes A/B; equivalência
  absoluta não é declarada.
- `CalendarScreen` continua compatível para estado legado/restaurado, sem CTA
  principal, enquanto o calendário vigente fica dentro de Exercícios.
