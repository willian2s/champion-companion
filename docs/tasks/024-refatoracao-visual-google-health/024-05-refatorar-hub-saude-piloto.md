# 024-05 — Refatorar hub Saúde como piloto

- **Ticker:** `024`
- **Número:** `05`
- **Status:** completed

## Objetivo e resultado esperado

Aplicar nova linguagem visual ao hub Saúde e obter aceite do usuário antes das
demais telas. Resultado: piloto com resumo claro, cards de métricas compactos e
navegação para detalhes, sem iniciar BLE.

## Requisitos e critérios cobertos

- R1, R4, R5, R6, R7, R8 e R9.
- Critérios 5–12 da spec aplicáveis ao piloto.

## Escopo incluído

- header, hierarquia, cards de frequência cardíaca, peso, passos e distância;
- hierarquia cromática do branding do Champion;
- estados com e sem dados;
- navegação e acessibilidade de cada card;
- aceite visual documentado.

## Escopo excluído

- telas de detalhe;
- medição, importação ou sincronização nova;
- status de integração Health Connect no hub; consulta permanece em Configurações;
- mudança de `HealthViewModel`, repositories ou rotas.

## Dependências

- `024-03` concluída;
- `024-04` concluída e estratégia cromática aprovada;
- dados persistidos disponíveis ou estados vazios reais;
- aceite desta subtarefa bloqueia `024-06` em diante.

## Arquivos e símbolos prováveis

- `feature/health/HealthScreen.kt`;
- `HealthScreen`, `HealthHubHeader`, `HealthMetricSummaryCard` e
  `HealthConnectHubStatusCard`;
- componentes opt-in do design system.

## Passos de implementação

1. Priorizar título, contexto curto e métricas locais.
2. Compactar metadados técnicos sem ocultar origem ou ausência.
3. Diferenciar visualmente valor, suporte, origem e estado.
4. Preservar callbacks dos quatro cards e ausência de BLE no hub.
5. Não duplicar status Health Connect no hub; manter Configurações como referência.
6. Exercitar estados reais disponíveis e marcar demais como `N/A`.
7. Apresentar capturas sanitizadas ao usuário e registrar aceite ou ajustes.
8. Atualizar overview somente após aceite.

## Testes e comandos de validação

Não executar testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: Saúde → quatro detalhes → voltar; portrait, fonte `1,3x`, landscape e
temas representativos; nenhuma ação BLE ao tocar no hub.

## Definição de pronto

- hub segue direção aprovada e preserva quatro destinos;
- valores ausentes não são inventados;
- Health Connect não é duplicado no hub e permanece write-only;
- layout adaptativo e acessível;
- capturas e aceite do usuário registrados;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- piloto rejeitado exige ajuste antes das próximas telas;
- metadados não podem competir com valor principal;
- semântica mesclada não pode ocultar labels acionáveis;
- loading e indisponibilidade não podem aparecer como ausência de dados;
- não transformar card em gatilho de medição ou refresh.

## Execução registrada

### Status

`completed`; aceite visual do hub registrado pelo usuário após os ajustes de
branding, hierarquia, textos e remoção do card Health Connect.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`;
- `024-05-captura-real-sanitizada-health-portrait.png`;
- `024-05-refatorar-hub-saude-piloto.md`.

### Decisões e desvios

- Removidos badges dos quatro cards de Saúde conforme feedback do usuário; estado
  vazio permanece explícito no valor e no texto de suporte.
- Corrigida duplicação textual de origem no card Peso (`Origem: entrada manual`).
- Header passou a usar azul-petróleo primário do Champion com texto branco; cards
  usam acentos primário, secundário e terciário em bordas, valores e setas.
- `Métricas locais` permanece branco conforme ajuste visual solicitado.
- Em tema claro, `Métricas locais` usa `onBackground` para preservar contraste;
  em tema escuro permanece branca.
- Removidos textos técnicos do hub: importação D1, contadores e último envio.
  Estados vazios foram reduzidos a mensagens curtas sem inventar dados.
- Peso diferencia carregamento, indisponibilidade e ausência real em valor, apoio
  e semântica acessível.
- Distância mantém qualificador visível `Estimativa local` e alinha valor, apoio
  e estado acessível quando resumo existe sem estimativa.
- Removido card Health Connect do hub; status de integração permanece concentrado
  em Configurações, sem alterar o contrato write-only.
- Mantidos header editorial, hierarquia de métricas, origem, período, setas
  acionáveis e callbacks dos quatro detalhes.
- Adoção cromática permanece limitada ao piloto; tema raiz, dynamic color dos
  demais consumidores e superfícies não migradas não foram alterados.
- O piloto usa fallback cromático nominal do Champion nos acentos para preservar
  identidade em dynamic color acromático; exceção fica restrita a esta superfície.
- Solicitação posterior de cor para item selecionado na barra de navegação não foi
  implementada nesta subtarefa: pertence ao shell compartilhado e requer task de
  correção própria, sem reabrir o piloto aceito.
- Nenhuma alteração em `HealthViewModel`, repositories, rotas, BLE, persistência
  ou sincronização.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew app:lintDebug assembleDebug` | Primeira tentativa falhou por referência intermediária a `brandPrimary`; corrigida e rerun passou |
| `./gradlew app:lintDebug assembleDebug` | Primeira tentativa do ajuste de distância falhou por smart cast em propriedade delegada; variável local aplicada e rerun passou |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` após remoção do card Health Connect |
| `./gradlew lint assembleDebug assembleRelease` | Passou novamente após correção de semântica da distância; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; Galaxy S22 autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Activity aberta |
| Smoke ADB Saúde portrait escuro | Passou; header azul-petróleo, `Métricas locais` branco, acentos coloridos, quatro cards e ausência de card Health Connect |
| Smoke ADB anterior de quatro detalhes | Passou; frequência cardíaca, peso, passos e distância abriram e retornaram; callbacks não foram alterados |
| Smoke ADB anterior landscape, fonte `1,3x` e tema claro | Passou; conteúdo rolável e ações acessíveis; configurações restauradas |
| Smoke ADB final portrait escuro | Passou; seção branca em fundo escuro, estados e ausência de Health Connect confirmados |
| Smoke ADB final portrait claro, fonte `1,3x` | Passou; seção escura com contraste, header colorido e cards roláveis |
| Smoke ADB final landscape, fonte `1,3x` | Passou após scroll; card e seta acionável acessíveis; notificação transitória não foi persistida |
| Smoke ADB final distância + hierarquia acessível | Passou; UI Automator expôs `Disponível` e `Estimativa local` no mesmo card |
| Revisão independente final | PASS; nenhum bloqueador ou achado importante |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Captura física temporária em portrait escuro confirmou cards sem badge e origem
  sem duplicação; arquivo bruto não foi persistido por conter dados pessoais.
- `024-05-captura-real-sanitizada-health-portrait.png` registra header azul,
  hierarquia, acentos, cards e navegação sem métricas pessoais.
- Landscape, fonte `1,3x`, tema claro, quatro detalhes e retornos foram exercitados
  após o último ajuste; remoção de Health Connect não altera callbacks ou rotas.
- Revisão independente final passou; aceite visual do usuário foi registrado.

### Riscos residuais

- Aceite visual do hub registrado; subtarefa liberada para próximas etapas.
- Estados sem dados reais e tema dinâmico não foram capturados em evidência
  sanitizada; foram preservados como `N/A`. Tema claro teve smoke físico, mas sem
  captura versionada por conter dados pessoais.
- Cor do item selecionado na barra de navegação permanece fora deste escopo e
  aguarda planejamento de correção do shell.
