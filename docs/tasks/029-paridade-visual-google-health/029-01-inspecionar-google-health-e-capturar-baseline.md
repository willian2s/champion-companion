# 029-01 — Inspecionar Google Health e capturar baseline

- **Ticker:** `029`
- **Número:** `01`
- **Status:** `completed`

## Objetivo e resultado esperado

Inspecionar o aplicativo instalado de referência e o Champion Companion com o
aparelho ADB conectado. Resultado: matriz verificável de telas, cards, detalhes,
filtros, períodos, estados e evidências visuais sanitizadas, sem alteração de
código ou dados do usuário.

## Requisitos e critérios de aceite cobertos

- R1, R2 e R3 da Spec 029; R8 permanece pendente até smoke físico completo.
- Critérios 1, 2, 3 e 10.
- Confirmar `com.fitbit.FitbitMobile`, versão, Activity e configuração física.
- Abrir todos os cards alcançáveis antes de registrar `N/A`.
- Percorrer todos os filtros, incluindo frequência cardíaca diária, períodos
  sem dados, abas Semana/Mês, calendário mensal e os sete filtros de workflow.

## Escopo incluído

- confirmar dispositivo, pacotes, versão e Activity via ADB;
- observar hierarquia, bounds, composição, densidade, ordem, labels genéricos,
  gestos e estados sem registrar valores pessoais;
- abrir telas e cards já existentes, sem login ou troca de conta;
- capturar screenshots temporários para comparação e sanitizá-los antes de
  qualquer persistência;
- registrar matriz por superfície, estado, filtro e cobertura.

## Escopo excluído

- alteração de produção, domínio ou persistência;
- login, troca de conta, importação, sync, medição, exclusão ou reset;
- leitura de banco, Health Connect readback, payload BLE ou logcat amplo;
- screenshot bruto no repositório;
- uso de sites, Google Fit ou Health Connect como referência visual.

## Dependências

- aparelho desbloqueado e ADB autorizado;
- `com.fitbit.FitbitMobile` instalado;
- variante debug do Champion instalada;
- Spec 029 e acesso para criar evidência em sua pasta.

## Arquivos e símbolos prováveis

- `docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`;
- screenshots sanitizados nomeados por superfície dentro da pasta da task;
- matriz de cobertura no mesmo documento;
- `MainActivity`, `ChampionNavigation`, `HomeScreen`, `HealthScreen`,
  `CalendarScreen` e `ExercisesScreen` como pontos de navegação.

## Passos de implementação

1. Executar `adb devices -l` e confirmar Galaxy S22 autorizado.
2. Identificar pacote, versão, `versionCode`, Activity, resolução, density,
   font scale, rotação, modo noturno e configuração regional observável.
3. Abrir a referência e registrar somente estrutura, contagens, bounds e
   comportamento visual genérico.
4. Percorrer todos os cards e detalhes alcançáveis; registrar cada resultado ou
   `N/A` com motivo.
5. Passar por todos os filtros: períodos de frequência cardíaca, Semana/Mês,
   calendário de atividades e filtros de workflow.
6. Repetir a cobertura estrutural no Champion para formar comparação por tela.
7. Remover valores, textos pessoais, títulos, BPM, peso, horários, IDs e
   metadados; revisar pixels e arquivos antes de versionar.
8. Registrar limitações concretas, inclusive estados não alcançados.

## Testes e comandos de validação

```bash
adb devices -l
adb shell pm list packages com.fitbit.FitbitMobile
adb shell pm list packages br.com.mwconsulting.championcompanion.dev
adb shell cmd package resolve-activity --brief com.fitbit.FitbitMobile
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
git diff --check
```

Não executar testes automatizados. Dumps e screenshots de referência devem ser
temporários e removidos após sanitização.

## Definição de pronto

- referência instalada e configuração física registradas;
- matriz cobre todas as superfícies listadas na Spec 029;
- cada card e filtro foi aberto ou possui `N/A` justificado;
- caso cardíaco sem dados e caso exercícios Semana/Mês estão documentados;
- evidências versionadas não contêm dados pessoais nem screenshot bruto;
- nenhum código, dado, integração ou configuração de produção foi alterado.

## Riscos e cuidados

- ADB bloqueado impede aceite físico; não substituir por suposição.
- UI pode depender de versão, região, conta e conteúdo atual.
- Não disparar ações que alterem dados para fabricar estados.
- Revisar pixels, EXIF e nomes de arquivos antes de persistir evidências.
- A inspeção deve respeitar a privacidade mesmo com autorização do usuário para
  dados não sanitizados.

## Execução da subtarefa

- **Status:** `in_progress` em 2026-09-01; ADB TLS foi reconectado e o smoke
  físico avançou, mas R8 ainda não possui cobertura completa tela a tela.
- **Arquivos alterados:**
  `docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`;
  este arquivo; `029-00-overview.md`; `docs/specs/029-paridade-visual-google-health.md`.
- **Decisões e desvios:** ADB autorizado no Galaxy S22. Superfícies navegáveis
  seguras foram percorridas nos dois aplicativos e têm paralelo ou `N/A` concreto
  na matriz. A referência possui Semana/Mês; o Champion instalado ainda não
  possui essas abas, portanto fica `N/A` observado, não coberto. Na retomada,
  Home, Saúde, Calendário, Exercícios, Dispositivo, Perfil e Configurações
  carregaram no Champion; os sete filtros de workflow e as três tendências
  cardíacas foram selecionados sem efeitos em dados. Tema claro, fonte ampliada,
  landscape tela a tela, acessibilidade, marker, tooltip e rolagem horizontal
  permanecem incompletos. Na retomada adicional, todas as superfícies de suporte
  carregaram em tema claro e a solicitação de landscape deixou o Champion em
  portrait; nenhum workaround foi aplicado. Screenshots/dumps brutos foram
  temporários e serão
  removidos antes do fechamento documental.

- **Acessibilidade exploratória:** dumps `uiautomator` carregaram em todas as
  superfícies. Foram observados containers clicáveis sem `text` ou
  `content-desc` próprio em Home `6/8`, Saúde `8/8`, Calendário `35/37`,
  Exercícios `6/6`, Mais `3/3`, Dispositivo `8/8`, Perfil `5/5` e Configurações
  `6/8`. Texto descendente não foi convertido em aceite; TalkBack não foi
  habilitado.

- **Comandos adicionais:** navegação por `adb shell input tap`; dumps por
  `adb shell uiautomator dump` e leitura via `adb exec-out`; restauração com
  `adb shell settings put system font_scale 1.0`, `adb shell cmd uimode night
  yes`, `adb shell settings put system accelerometer_rotation 1` e `adb shell
  wm user-rotation free`; validação final com `git diff --check`.
- **Resultados adicionais:** oito superfícies tiveram hierarquia UI carregada;
  orientação voltou a `accelerometer_rotation=1`/`user-rotation=free`; não há
  artefatos brutos na pasta da task.
- **Comandos executados:** `adb devices -l`; `adb shell pm list packages
  com.fitbit.FitbitMobile`; `adb shell pm list packages
  br.com.mwconsulting.championcompanion.dev`; `adb shell cmd package
  resolve-activity --brief com.fitbit.FitbitMobile`; `adb shell dumpsys package`
  para os dois pacotes; `adb shell wm size`; `adb shell wm density`; leitura de
  `font_scale`, modo noturno e locale; `adb shell monkey -p
  com.fitbit.FitbitMobile 1`; `adb shell am start -n
  br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity`;
  `adb shell input tap`; `adb shell input swipe`; `adb shell input keyevent 4`;
  `adb shell uiautomator dump` e `adb shell cat` em arquivos temporários;
  alteração temporária de `font_scale`, `uimode night`,
  `accelerometer_rotation` e `user_rotation`, sempre com restauração;
  `git diff --check`; retomada de `adb devices -l`; navegação segura no Champion;
  seleção dos sete filtros de workflow; seleção de `Hoje`, `7 dias` e `30 dias`;
  toque prolongado e arrasto horizontal no gráfico; nova navegação segura em
  tema claro e fonte `1.3`; solicitação de landscape com rotação `1` no
  Champion; restauração final confirmada
  com `font_scale=1.0`, modo noturno `yes`, `accelerometer_rotation=1` e
  rotação do usuário `free`.
- **Resultados e evidências parciais:** referência `com.fitbit.FitbitMobile`, versão
  `5.06.1.health-mobile-1104101009-966217170`, `versionCode 1104101009`,
  alias `com.fitbit.HealthBrandedAlias`, `HomeActivity`; Champion
  `1.0-dev`, `MainActivity`; Galaxy S22 `SM-S901E`, `1080x2340`, density
  física `480`, override `450`, fonte `1.0`, portrait, tema escuro. Matriz
  comparativa em `evidencia-baseline-visual-029-01.md`; telas seguras têm
  paralelo ou `N/A` concreto. O smoke confirmou carregamento do Home em
  portrait e tema claro/escuro, além do comportamento com fonte `1.3`; no
  landscape solicitado, o Champion permaneceu em portrait. R8 ainda aguarda
  cobertura completa.
- **Riscos residuais:** conteúdo pode variar por conta/versão/região. Tema claro
  e fonte ampliada foram percorridos nas superfícies principais, mas retorno e
  acessibilidade tela a tela permanecem pendentes. Landscape não foi alcançado
  no Champion, que permaneceu portrait; marker, tooltip e rolagem horizontal
  não foram observáveis. A ausência das abas Semana/Mês no Champion segue como
  `N/A` observado. Nenhum risco foi convertido em aceite presumido.

## Limitações atuais

- **Limitação:** ADB está disponível, mas R8 exige cobertura física completa de
  cada superfície e combinação alcançada.
- **Impacto:** esta retomada não permite declarar aceite para retorno e
  acessibilidade tela a tela em tema claro e fonte ampliada; landscape não foi
  alcançado no Champion; marker, tooltip e rolagem horizontal não foram
  observáveis.
- **Próximo passo:** repetir somente cobertura física sanitizada nas variantes e
  interações pendentes, sem executar ações geradoras ou destrutivas; então
  atualizar evidência e fechar esta subtarefa.
