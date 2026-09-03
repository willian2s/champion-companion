# 031-11 — Consolidar validação e revisão final

- **Ticker:** `031`
- **Número:** `11`
- **Status:** pending

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
