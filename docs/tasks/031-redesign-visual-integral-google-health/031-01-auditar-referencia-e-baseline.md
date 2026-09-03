# 031-01 — Auditar referência e baseline

- **Ticker:** `031`
- **Número:** `01`
- **Status:** completed

## Objetivo

Registrar versão, estrutura e padrões visuais observáveis de
`com.fitbit.FitbitMobile` que orientarão redesign integral, mantendo evidência
versionada sanitizada. A inspeção física pode usar a exceção efêmera local
definida na Spec 031, sem transportar material bruto para fora do aparelho
autorizado.

## Requisitos e aceite

- Confirmar pacote, versão, data, Activity e disponibilidade da referência.
- Mapear Home, Saúde, detalhes, Exercícios, calendário, Perfil, Dispositivo e
  Configurações, incluindo barra inferior, header, filtros, gráficos e entradas.
- Comparar referência com superfícies atuais usando labels genéricos, bounds e
  estrutura.
- Versionar somente relatório, capturas sanitizadas e fontes públicas sem dados
  pessoais; estados não observados ficam `N/A`.
- Se uma sessão autorizada precisar de material não sanitizado, usá-lo somente
  para análise visual/estrutural no aparelho, sem importação, sincronização,
  alteração de dados ou uso no produto, e confirmar descarte ao final da sessão.

## Escopo

Inclui inspeção documental, pública e física autorizada, matriz de mapeamento e
baseline visual. A inspeção física excepcional pode manter material não
sanitizado somente em temporário local do aparelho autorizado durante a sessão.
Exclui alteração de código, importação, medição, sync, alteração de dados,
transporte de material bruto, versionamento de evidência não sanitizada e
incorporação de assets proprietários.

## Dependências

- `AGENTS.md`, baseline, Spec 024, ADR-004 e ADR-028.
- Acesso opcional ao aparelho com `com.fitbit.FitbitMobile`.

## Arquivos e símbolos prováveis

- `docs/tasks/031-redesign-visual-integral-google-health/`
- `docs/specs/031-redesign-visual-integral-google-health.md`
- `app/.../ChampionNavigation.kt`, telas de `feature/*` para matriz atual.

## Passos

1. Registrar pacote/versão/data e fonte pública usada.
2. Inspecionar somente estrutura visual e fluxos não destrutivos.
3. Quando indispensável e sob a exceção da Spec 031, manter material não
   sanitizado apenas no temporário local do aparelho autorizado; não copiá-lo
   para máquina, contexto, cloud ou logs.
4. Sanitizar qualquer evidência derivada, remover temporários brutos ao fim da
   sessão e documentar confirmação de limpeza e lacunas.
5. Criar matriz referência → superfície/estado/componente local.

## Validação e comandos

```bash
git diff --check
adb devices -l
adb shell pm path com.fitbit.FitbitMobile
adb shell uiautomator dump --compressed /sdcard/window.xml
```

Não executar testes automatizados. Não versionar XML, screenshot ou Logcat sem
sanitização. Em sessão excepcional, apagar e conferir os temporários brutos no
aparelho ao encerrar, sem usar cache, backup ou sincronização como retenção.

## Definição de pronto

Baseline versionado, sanitizado, reproduzível e aprovado em revisão independente;
toda superfície tem fonte, observação ou `N/A`; nenhum dado pessoal permanece.
Toda sessão excepcional tem escopo, descarte e restauração auditados. Estado
inicial do aparelho é registrado e restaurado quando a inspeção física o alterar;
indisponibilidade fica `N/A`.

## Riscos

Versão/região/conta podem alterar UI. Ausência do pacote bloqueia equivalência
física e deve ser registrada, não mascarada por material público. Uso local de
material não sanitizado amplia risco de cópia acidental; qualquer dúvida sobre
escopo ou limpeza bloqueia a subtask e exige rollback para baseline sanitizado.

## Exceção operacional e auditoria da execução

- A autorização temporária da Spec/Task 031 aplica-se somente a uma inspeção
  física necessária, em aparelho autorizado, e apenas durante a sessão. Não
  altera `AGENTS.md` ou ADR-004 e não autoriza evidência bruta no repositório.
- Esta execução documental não capturou nem leu dados pessoais reais ou
  screenshot bruto. O XML temporário foi usado somente para agregados, labels
  genéricos e bounds, e removido após a leitura estrutural.
- **Descarte confirmado:**
  `adb shell 'test ! -e /sdcard/window.xml && printf "window_xml_absent\\n" || printf "window_xml_present\\n"'`
  retornou `window_xml_absent`; não há screenshot ou outro temporário bruto
  criado por esta execução no aparelho ou no workspace.
- Não houve importação, medição, sincronização, exclusão de dados da referência,
  edição, troca de conta,
  alteração de configuração ou operação BLE para gerar evidência. Não há
  temporário bruto pendente conhecido nesta execução.
- Uma futura sessão que não conseguir confirmar limpeza deve ser interrompida;
  o material não pode orientar layout, a subtask deve permanecer bloqueada e
  qualquer decisão derivada deve voltar ao baseline público ou sanitizado.

## Execução e evidências

- **Arquivos alterados:**
  `evidencia-baseline-visual-031-01.md` e este arquivo; o item correspondente do
  overview foi marcado após revisão independente.
- **Decisões:** a tentativa inicial sem dispositivo fica registrada como falha;
  observação física sanitizada de 2026-08-17 foi substituída pela reexecução
  sanitizada de 2026-09-03 após conexão do `SM-S901E`. Fontes públicas atuais
  confirmam pacote, nome público, disponibilidade da listagem e data de
  atualização, mas não substituem UI instalada. Nenhum asset proprietário foi
  incorporado.
- **Desvios:** a primeira tentativa ADB ocorreu sem dispositivo; a segunda
  confirmou pacote, versão, Activity e hierarquia. A cobertura física ficou
  parcial: Home, Saúde, quatro detalhes de métricas e Dispositivos foram
  inspecionados; superfícies e estados não alcançados permanecem `N/A`. Nenhuma
  captura ou XML novo foi versionado.
- **Comandos executados:**
  `git diff --check`; `adb devices -l`;
  `adb shell pm path com.fitbit.FitbitMobile`;
  `adb shell uiautomator dump --compressed /sdcard/window.xml`;
  `adb shell dumpsys package com.fitbit.FitbitMobile`;
  `adb shell cmd package resolve-activity --brief com.fitbit.FitbitMobile`;
  `adb shell am start -n com.fitbit.FitbitMobile/com.fitbit.HealthBrandedAlias`;
  `adb shell input tap 810 2258`; `adb shell input tap 270 500`;
  `adb shell input keyevent 4`; `adb shell input tap 630 500`;
  `adb shell input keyevent 4`; `adb shell input tap 650 1150`;
  `adb shell input keyevent 4`; `adb shell input tap 650 1670`;
  `adb shell input keyevent 4`; `adb shell input tap 270 2258`;
  `adb shell input tap 80 170`; `adb shell input keyevent 4`;
  `adb shell input tap 1000 170`; `adb shell input keyevent 4`;
  consultas sanitizadas de `wm size`, `wm density`, `font_scale`, modo noturno,
  Activity retomada e orientação; `adb shell test ! -e /sdcard/window.xml`.
- **Resultados/evidências:** primeira execução ADB retornou `no
  devices/emulators found`; após conexão, `git diff --check` passou, `pm path`
  encontrou APK base e splits, `dumpsys package` confirmou
  `5.06.1.health-mobile-1104101009-966217170`, e a Activity resolvida foi
  `com.fitbit.HealthBrandedAlias`; a Activity efetivamente exibida no início e
  no fim foi `com.google.android.apps.fitbit.app.home.impl.HomeActivity`.
  Dumps temporários registraram somente agregados de nós, labels genéricos e
  bounds; Home, Saúde, quatro detalhes de métricas e Dispositivos foram
  acessados. Configuração observada: portrait,
  `1080x2340`, densidade física `480`, override `450`, fonte `1.0`, tema escuro.
  `test ! -e /sdcard/window.xml` confirmou `window_xml_absent`. A Activity final
  foi deixada na Home; nenhuma configuração de aparelho, conta ou dado foi
  alterada. Relatório sanitizado contém a matriz das 13 superfícies, lacunas de
  shell e limites de privacidade.
- **Testes:** não executados, conforme escopo documental e instrução explícita da
  subtarefa.
- **Revisão independente:** aprovada com ressalvas não bloqueantes em
  2026-09-03. Nenhum bloqueador para 031-01; cobertura física restante, estados
  raros e validações de 031-11 permanecem riscos/documentação futura.
- **Riscos residuais:** estados, filtros, gráficos, Configurações, Perfil,
  Exercícios, Calendário, detalhe diário, landscape, fonte ampliada e temas da
  referência não foram revalidados fisicamente; revisão visual dessas áreas
  permanece dependente de aparelho autorizado.
