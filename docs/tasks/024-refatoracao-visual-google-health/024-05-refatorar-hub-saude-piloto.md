# 024-05 — Refatorar hub Saúde como piloto

- **Ticker:** `024`
- **Número:** `05`
- **Status:** pending

## Objetivo e resultado esperado

Aplicar nova linguagem visual ao hub Saúde e obter aceite do usuário antes das
demais telas. Resultado: piloto com resumo claro, cards de métricas compactos e
navegação para detalhes, sem iniciar BLE.

## Requisitos e critérios cobertos

- R1, R4, R5, R6, R7, R8 e R9.
- Critérios 5–12 da spec aplicáveis ao piloto.

## Escopo incluído

- header, hierarquia, cards de frequência cardíaca, peso, passos e distância;
- status secundário Health Connect;
- estados com e sem dados;
- navegação e acessibilidade de cada card;
- aceite visual documentado.

## Escopo excluído

- telas de detalhe;
- medição, importação ou sincronização nova;
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
5. Validar Health Connect como feedback secundário.
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
- Health Connect permanece secundário e write-only;
- layout adaptativo e acessível;
- capturas e aceite do usuário registrados;
- comandos aprovados e overview atualizado.

## Riscos e cuidados

- piloto rejeitado exige ajuste antes das próximas telas;
- metadados não podem competir com valor principal;
- semântica mesclada não pode ocultar labels acionáveis;
- não transformar card em gatilho de medição ou refresh.

## Execução registrada

### Status

`pending`; piloto visual implementado e ajustado após feedback do usuário.
Estratégia cromática transversal foi aprovada; falta aplicar sua direção ao
piloto, concluir validações físicas e obter aceite visual final.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthScreen.kt`;
- `024-05-refatorar-hub-saude-piloto.md`.

### Decisões e desvios

- Removidos badges dos quatro cards de Saúde conforme feedback do usuário; estado
  vazio permanece explícito no valor e no texto de suporte.
- Corrigida duplicação textual de origem no card Peso (`Origem: entrada manual`).
- Mantidos header editorial, hierarquia de métricas, origem, período, setas
  acionáveis, callbacks dos quatro detalhes e status secundário Health Connect.
- Não aplicada alteração global de tema ou paleta: R2 e ADR-023 exigem adoção
  opt-in por superfície, sem redesenhar telas ainda não migradas implicitamente.
- Nenhuma alteração em `HealthViewModel`, repositories, rotas, BLE, persistência
  ou sincronização.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` antes do ajuste visual |
| `./gradlew app:lintDebug assembleDebug` | Passou; `BUILD SUCCESSFUL` após remoção dos badges |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Activity aberta |
| Smoke ADB Saúde portrait escuro | Passou; header e três primeiros cards visíveis, sem badges |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Captura física temporária em portrait escuro confirmou cards sem badge e origem
  sem duplicação; arquivo bruto não foi persistido por conter dados pessoais.
- Landscape, fonte `1,3x`, tema claro, quatro detalhes, retorno e captura
  sanitizada versionada permanecem pendentes até aceite visual.

### Riscos residuais

- Aceite visual do usuário ainda não registrado; subtarefa não pode ser marcada
  como concluída nem liberar subtarefas seguintes.
- Estados sem dados e card Health Connect não foram capturados em evidência
  sanitizada versionada nesta iteração.
