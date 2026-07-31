# 017-02 — Alinhar tela Dispositivo

- **Ticker:** `017`
- **Número:** `02`
- **Status:** concluído

## Objetivo e resultado esperado

Alinhar margem, ritmo vertical, tokens de spacing e cabeçalho de Dispositivo ao
padrão predominante do app. Resultado esperado: tela visualmente coerente sem
alteração de qualquer estado, ação ou fronteira BLE.

## Requisitos e critérios cobertos

- R1, R2, R3, R5, R7, R8, R9 e R10 da spec 017.
- Critérios de aceite 1 a 3 e 6, 8, 9 e 10.

## Escopo incluído

- margem externa e arrangement principal de `DeviceManagementScreen`;
- card de header com título e texto de apoio existentes;
- heading semântico único;
- substituição dos literais `8.dp`, `12.dp` e `16.dp` que representem spacing
  nos layouts tocados pelos equivalentes de `ChampionSpacing`;
- ajustes visuais mínimos para fonte aumentada e landscape;
- screenshots e registro de validação desta subtarefa.

## Escopo excluído

- estado, intents, events, lifecycle, permissões, scan e conexão;
- associação, auto-connect, retry, disconnect, esquecimento e substituição;
- diagnóstico, sanitização, exportação, PoC debug e conteúdo dos diálogos;
- mudança de copy funcional ou identificação segura;
- domínio, banco, BLE core, navegação, Gradle e testes automatizados.

## Dependências

- Spec e overview 017 aprovados.
- Nenhuma dependência técnica em `017-01`; ordem após ela é apenas operacional.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`:
  `DeviceManagementScreen`, novo ou ajustado header local,
  `AssociatedDeviceSection`, `ScanResultsSection` e `ActionSection` somente onde
  houver spacing literal no layout tocado.
- Referências sem edição prevista: `ChampionSpacing`, `ChampionShapeLarge`,
  `HealthHubHeader` e `ExercisesHeader`.
- Este arquivo e `017-00-overview.md` para status e evidências.

## Passos de implementação

1. Revalidar padrão visual vigente e mapear quais `dp` de `DeviceScreen.kt`
   representam spacing; separar dimensões de componente que devem permanecer.
2. Aplicar `ChampionSpacing.extraLarge` à margem externa e
   `ChampionSpacing.large` ao ritmo principal.
3. Encapsular `Dispositivo` e texto de apoio existente em card de header com
   container, padding, tipografia, peso e semântica coerentes com padrão real.
4. Substituir somente literais de spacing tocados por `ChampionSpacing.small`,
   `medium` ou `large`, conforme equivalência existente.
5. Conferir FlowRows, listas de candidatos e linhas de ações com largura menor,
   fonte aumentada e landscape; fazer apenas ajustes adaptativos estritamente
   visuais quando necessários.
6. Revisar diff para confirmar que callbacks, branches, intents, events,
   lifecycle, permissões e helpers de diagnóstico permaneceram inalterados.
7. Executar validação, capturar evidências e marcar somente `017-02` no overview
   após conclusão real.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

Executar após implementação:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke manual obrigatório:

- abrir Dispositivo e confirmar header e margem;
- percorrer estados disponíveis sem disparar ação não solicitada;
- verificar estado vazio ou associado, resumo, ações e PoC debug quando visível;
- confirmar que botões permanecem acessíveis e textos não se sobrepõem;
- repetir com fonte aumentada e landscape;
- confirmar que nenhum prompt, scan, conexão ou exportação passou a iniciar por
  efeito do layout.

## Evidências esperadas

- screenshot portrait da tela Dispositivo em estado representativo;
- screenshot com fonte aumentada;
- screenshot landscape;
- registro dos estados manualmente observados;
- resultados dos comandos e confirmação de diff exclusivamente visual.

## Definição de pronto

- Dispositivo atende margem, ritmo, tokens, card, tipografia e semântica da spec.
- Estados e ações existentes permanecem equivalentes.
- Lint, builds e instalação passam.
- Screenshots sanitizados e smoke manual estão registrados.
- Nenhum teste automatizado é tocado ou executado.
- Status muda para concluído e somente item `017-02` é marcado no overview.

## Riscos e cuidados

- `DeviceScreen.kt` concentra código sensível; não aproveitar mudança visual para
  corrigir lifecycle, BLE, diagnóstico ou português legado.
- Literais podem representar tamanho, não spacing; não substituir cegamente.
- Linhas com dois botões podem ficar estreitas em fonte aumentada; preservar
  ações, rótulos e alvos de toque sem criar comportamento novo.
- Screenshots não devem expor identificador completo, relatório ou dado pessoal.
- Não reabrir Fase 006 nem alterar dívida já aceita.

## Registro de execução

### Status

Concluído. Subtarefa limitada à tela Dispositivo; `017-03` não foi iniciada.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/device/DeviceScreen.kt`;
- `017-02-dispositivo-portrait.png`;
- `017-02-dispositivo-fonte-1-3x.png`;
- `017-02-dispositivo-landscape.png`.

As capturas foram sanitizadas antes do registro: conteúdo do dispositivo
associado foi coberto, metadados removidos quando aplicável e nenhum relatório,
identificador completo ou payload BLE foi incluído.

### Decisões e desvios

- A margem da página passou a usar `ChampionSpacing.extraLarge` e o ritmo
  principal passou a usar `ChampionSpacing.large`.
- O cabeçalho textual foi encapsulado em `DeviceHeader`, com `Card`,
  `ChampionShapeLarge`, `surfaceContainerLowest`, padding `large`,
  `headlineSmall`, peso seminegrito, texto de apoio `bodyMedium` e um heading.
- Literais de spacing tocados foram substituídos por `ChampionSpacing.small` e
  `ChampionSpacing.medium`; dimensões de componentes não foram convertidas.
- Branches de estado, intents, callbacks, lifecycle, permissões, scan,
  conexão, associação, diagnóstico, exportação, diálogos e PoC debug não foram
  alterados.
- Não houve desvio funcional. Nenhum teste automatizado foi criado, alterado ou
  executado.

### Comandos executados e resultados

- `git diff --check` — passou antes da validação Gradle.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`;
  relatório lint sem erros.
- `./gradlew installDebug` — `BUILD SUCCESSFUL`; instalado em `SM-S901E - 16`.
- `adb devices` — dispositivo físico disponível para smoke visual.

### Evidências visuais e smoke manual

- Portrait: header em card, margem externa, resumo de conexão e estado associado
  conectado observados; textos e ações permaneceram acessíveis.
- Fonte aumentada: `font_scale=1.3`; título e texto de apoio fizeram wrap sem
  corte. A navegação inferior manteve rótulos visíveis, com quebra em duas
  linhas já registrada no pacote.
- Landscape: header, texto de apoio e resumo de conexão permaneceram visíveis;
  status de conexão não foi ocultado.
- O fluxo disponível no aparelho foi inspecionado sem iniciar busca, conexão,
  exportação ou diagnóstico. Nenhum prompt novo de permissão, scan, conexão ou
  exportação apareceu.
- Branches de vazio, busca e candidatos foram preservadas por inspeção do diff;
  não foram disparadas para evitar ação BLE não solicitada durante smoke.

### Riscos residuais

- Estados de busca e vazio não foram acionados manualmente nesta validação, pois
  isso iniciaria fluxo BLE; permanecem sem alteração estrutural no diff.
- Em fonte `1,3x`, rótulos da navegação inferior quebram em duas linhas, sem
  corte e fora do escopo desta subtarefa.

### Revisão independente

Aprovada com ressalvas não bloqueantes. Revisão confirmou ticker, checklist,
escopo visual, tokens, header, preservação de BLE/estado/callbacks, screenshots
sanitizados e comandos registrados.
