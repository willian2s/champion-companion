# 024-06 — Refatorar Home

- **Ticker:** `024`
- **Número:** `06`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar Home como visão diária prioritária, com progresso, métricas e
pendências legíveis. Resultado: resumo útil em primeira dobra, sem reduzir
clareza de refresh, relógio ou atividades pendentes.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- top bar, status do relógio, progresso de passos, BPM, distância, calorias;
- refresh, feedback Health Connect e banners de revisão/calibração;
- estados loading, vazio, erro, permissões e Bluetooth;
- atalhos para Dispositivo, Perfil, BPM e Exercícios.

## Escopo excluído

- política de estimativas, FEA1, coordinator, ViewModels ou novos dados;
- refresh automático, nova ação BLE ou mudança de meta.

## Dependências

- `024-05` aceita.

## Arquivos e símbolos prováveis

- `feature/home/HomeScreen.kt`;
- `HomeScreen`, `HomeTopBar`, `HomeSurface`, `HomeSummaryContent`,
  `HomeRefreshStatus` e mapeamentos visuais locais;
- componentes do design system.

## Passos de implementação

1. Aplicar hierarquia do piloto à visão diária.
2. Destacar progresso/meta e último dado relevante antes de metadados.
3. Remover cores locais tocadas em favor de tokens semânticos adequados.
4. Preservar pull-to-refresh, ação acessível, banners e callbacks.
5. Exercitar estados reais sem inventar métricas.
6. Validar ciclo de vida e saída da tela sem mudança funcional.
7. Registrar capturas sanitizadas, resultados e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: refresh seguro, atalhos, retorno, rolagem, portrait, fonte `1,3x`,
landscape e temas representativos.

## Definição de pronto

- primeira dobra prioriza resumo diário e estado do relógio;
- refresh e pendências continuam explícitos;
- estados sem dados permanecem seguros;
- callbacks e lifecycle equivalentes;
- comandos e capturas registrados; overview atualizado.

## Riscos e cuidados

- não disparar BLE apenas para screenshot;
- banners não podem ocultar resumo ou ação essencial;
- semântica do pull-to-refresh deve permanecer;
- dados pessoais precisam ser ocultados nas evidências.

## Execução registrada

### Status

`completed`; refatoração visual, alinhamento cromático, validação física e
captura sanitizada concluídos.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/home/HomeScreen.kt`;
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`;
- `docs/specs/024-refatoracao-visual-google-health.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`;
- subtarefas pendentes `024-06` a `024-19`, somente para registrar a regra
  cromática comum solicitada;
- `docs/tasks/024-refatoracao-visual-google-health/024-06-captura-real-sanitizada-home-portrait.png`.

### Causa do escalonamento e decisões

- O diff visual recebido usava `MaterialTheme.colorScheme.primaryContainer` no
  header e roles dinâmicas diretamente nos acentos. Isso não reproduzia o piloto
  Saúde aceito, que fixa o header no azul-petróleo nominal com conteúdo branco e
  seleciona acentos nominais claros/escuros conforme o tema.
- Home passou a usar o mesmo padrão do piloto: header azul-petróleo, conteúdo
  branco, cards neutros com bordas/valores coloridos e progresso com acentos
  `primary`/`secondary`/`tertiary` da paleta Champion correspondente ao tema.
- Card de revisão pendente passou a usar base neutra, borda e título com acento
  `secondary` ou `tertiary` conforme prioridade, sem ocultar a ação para
  Exercícios.
- Badge cromático sobre o ícone do relógio foi removido conforme solicitação do
  usuário. Status permanece explícito no texto `Relógio: ...` e na descrição
  acessível do atalho Dispositivo.
- O título `Hoje` usa composição sobreposta no header para permanecer
  geometricamente centralizado entre as margens da superfície.
- Tema raiz, dynamic color, dados, contratos, callbacks, lifecycle, BLE,
  persistência, Health Connect e navegação não foram alterados.
- A regra visual foi promovida para a spec e repetida somente nas subtarefas
  pendentes, sem reabrir `024-01` a `024-05` nem alterar requisitos funcionais.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| `./gradlew lint assembleDebug assembleRelease` | Passou após centralização do título e remoção do badge; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; APK instalado no dispositivo SM-S901E Android 16 |
| `adb devices -l` | Passou; dispositivo autorizado e operacional |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou; Home iniciou |
| Smoke portrait | Passou; resumo, progresso, revisão pendente, rolagem e primeira dobra verificados |
| Smoke de atalhos e retorno | Passou; revisão pendente abriu Exercícios; Perfil, Dispositivo e BPM abriram e retornaram |
| Smoke da barra inferior | Passou; Saúde abriu pela barra e Início retornou à Home |
| Centralização semântica | Passou; bounds de `Hoje` `[466,225][615,321]`, centro `540,5`, coincidente com superfície `[120,960]` |
| Smoke landscape/fonte `1,3x` | Passou; labels estruturais acessíveis, sem corte observado |
| Smoke temas claro/escuro | Passou; ambos renderizaram labels estruturais; aparelho restaurado para tema escuro |
| Pull-to-refresh | Gesto executado; estado transitório não foi capturado, sem disparar BLE adicional para screenshot; callback e ação acessível preservados estaticamente |
| Auditoria do checklist e da regra cromática | Passou; overview mantém uma única seção `## Checklist`, 19 itens e regra consta em `024-06` a `024-19` |

Não foram criados, alterados ou executados testes automatizados.

### Evidências e pendências

- Comparação estática com `HealthScreen.kt` confirmou uso equivalente de
  `LightColorScheme.primary`, conteúdo branco e seleção de
  `LightColorScheme`/`DarkColorScheme` para acentos.
- Evidência sanitizada: `024-06-captura-real-sanitizada-home-portrait.png`.
  Barra de status foi recortada; métricas e dados livres foram cobertos; chunks
  PNG ficaram restritos a `IHDR`, `IDAT` e `IEND`; header, hierarquia e cores
  permanecem verificáveis. Card de revisão foi exercitado antes de seu estado
  deixar de aparecer no dispositivo, após abertura de Exercícios.
- Configurações do aparelho restauradas após smoke: fonte `1,0x`, portrait,
  rotação automática desativada e tema escuro.
- Estados raros de erro, permissões, Bluetooth desligado e loading completo
  ficaram `N/A`; não foram fabricados dados nem disparada operação BLE apenas
  para evidência.
- Não há risco residual funcional conhecido no escopo. Revisão visual física
  independente registrada antes do fechamento.
