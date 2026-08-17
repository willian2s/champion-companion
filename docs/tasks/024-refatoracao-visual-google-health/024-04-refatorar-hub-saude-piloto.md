# 024-04 — Refatorar hub Saúde como piloto

- **Ticker:** `024`
- **Número:** `04`
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
- dados persistidos disponíveis ou estados vazios reais;
- aceite desta subtarefa bloqueia `024-05` em diante.

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
