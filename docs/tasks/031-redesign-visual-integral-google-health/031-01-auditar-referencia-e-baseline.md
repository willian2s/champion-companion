# 031-01 — Auditar referência e baseline

- **Ticker:** `031`
- **Número:** `01`
- **Status:** pending

## Objetivo

Registrar versão, estrutura e padrões visuais observáveis de
`com.fitbit.FitbitMobile` que orientarão redesign integral, sem persistir dados
pessoais ou evidência bruta não sanitizada.

## Requisitos e aceite

- Confirmar pacote, versão, data, Activity e disponibilidade da referência.
- Mapear Home, Saúde, detalhes, Exercícios, calendário, Perfil, Dispositivo e
  Configurações, incluindo barra inferior, header, filtros, gráficos e entradas.
- Comparar referência com superfícies atuais usando labels genéricos, bounds e
  estrutura.
- Versionar somente relatório e capturas sanitizadas ou fontes públicas sem
  dados pessoais; estados não observados ficam `N/A`.

## Escopo

Inclui inspeção documental, pública e física autorizada, matriz de mapeamento e
baseline visual. Exclui alteração de código, importação, medição, sync,
captura de dados pessoais e incorporação de assets proprietários.

## Dependências

- `AGENTS.md`, baseline, Spec 024, ADR-004 e ADR-028.
- Acesso opcional ao aparelho com `com.fitbit.FitbitMobile`.

## Arquivos e símbolos prováveis

- `docs/tasks/031-redesign-visual-integral-google-health/`
- `docs/specs/031-redesign-visual-integral-google-health.md`
- `app/.../ChampionNavigation.kt`, telas de `feature/*` para matriz atual.

## Passos

1. Registrar pacote/versão/data e fonte pública usada.
2. Inspecionar somente estrutura visual e fluxos não sensíveis.
3. Sanitizar capturas, remover dumps brutos e documentar lacunas.
4. Criar matriz referência → superfície/estado/componente local.

## Validação e comandos

```bash
git diff --check
adb devices -l
adb shell pm path com.fitbit.FitbitMobile
adb shell uiautomator dump --compressed /sdcard/window.xml
```

Não executar testes automatizados. Não versionar XML, screenshot ou Logcat sem
sanitização.

## Definição de pronto

Baseline versionado, sanitizado, reproduzível e aprovado em revisão independente;
toda superfície tem fonte, observação ou `N/A`; nenhum dado pessoal permanece.
Estado inicial do aparelho é registrado e restaurado quando a inspeção física o
alterar; indisponibilidade fica `N/A`.

## Riscos

Versão/região/conta podem alterar UI. Ausência do pacote bloqueia equivalência
física e deve ser registrada, não mascarada por material público.
