# 024-16 — Refatorar Perfil

- **Ticker:** `024`
- **Número:** `16`
- **Status:** completed

## Objetivo e resultado esperado

Refatorar Perfil para organizar métricas pessoais, passo base, perfis aprendidos
e histórico de calibração. Resultado: edição e provenance compreensíveis sem
reescrever snapshots ou misturar RUNNING/WALKING.

## Requisitos e critérios cobertos

- R4–R9; critérios 5, 7–12 da spec.
- Regra cromática comum: quando presentes, header azul-petróleo nominal do
  Champion com conteúdo branco; cards, indicadores e progresso com acentos
  nominais `primary`/`secondary`/`tertiary` da paleta clara/escura conforme o
  tema, seguindo o piloto `024-05`, sem alterar o tema raiz.

## Escopo incluído

- altura, meta diária e comprimento de passo;
- sugestão por altura e confirmação;
- perfis aprendidos RUNNING/WALKING;
- histórico, correção/remoção de calibração, mensagens e dialogs;
- abertura de atividade relacionada.

## Escopo excluído

- novos campos pessoais, unidade imperial ou regra de calibração;
- alteração de repository, snapshots, reprocessamento ou sync.

## Dependências

- `024-15` concluída.

## Arquivos e símbolos prováveis

- `feature/profile/ProfileScreen.kt`;
- `ProfileScreen`, editores, cards de perfil e histórico;
- `ProfileViewModel` somente como referência.

## Passos de implementação

1. Priorizar métricas editáveis e passo base atual.
2. Separar perfis aprendidos por modalidade com origem/data.
3. Organizar histórico e ações de correção/remoção sem perder contexto.
4. Preservar sugestões, confirmações, mensagens e abertura de atividade.
5. Validar cancelamento de edição e dialogs sem persistir dado artificial.
6. Conferir fonte ampliada, teclado, landscape e conteúdo longo.
7. Registrar evidências e overview.

## Testes e comandos de validação

Sem testes automatizados.

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
adb devices -l
adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity
```

Smoke: abrir/fechar editores, cancelar dialogs, abrir atividade, voltar; não
salvar valor apenas para screenshot.

## Definição de pronto

- métricas, passo base, perfis e histórico têm hierarquia clara;
- RUNNING/WALKING permanecem independentes;
- snapshots e dados históricos não são reescritos;
- adaptação/teclado validados;
- comandos, capturas sanitizadas e overview registrados.

## Riscos e cuidados

- dados antropométricos e calibrações são pessoais;
- remover calibração pode ser destrutivo;
- copy de origem não pode prometer reprocessamento automático;
- não alterar regras da Spec 023.

## Registro de execução

### Status

`completed`; Perfil reorganizado com foco em métricas editáveis e comprimento do
passo. Valor atual, sugestão por altura e edição manual ficaram no mesmo contexto;
perfis aprendidos e histórico de calibração ficaram disponíveis em modais próprios,
sem misturar RUNNING/WALKING nem alterar contratos funcionais.

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileScreen.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/profile/ProfileViewModel.kt`;
- `app/src/main/res/values/strings.xml`;
- `024-16-captura-real-sanitizada-perfil.png`;
- `024-00-overview.md`.

### Decisões e desvios

- A tela principal agora prioriza header, métricas editáveis e uma única seção de
  comprimento do passo. Valor atual, sugestão por altura e edição manual são
  subseções relacionadas, separadas por divisores no mesmo card.
- Perfis aprendidos e histórico aparecem em um card-resumo com abertura modal
  separada, evitando sobrecarga da tela principal.
- O modal de perfis mantém corrida e caminhada independentes, com quantidade de
  amostras, distância de referência, origem e data de atualização.
- O modal de histórico mantém correção, remoção confirmada e abertura da
  atividade relacionada. Cancelamentos não persistem valores artificiais.
- `formatStepLengthMeters` limita apresentação a quatro casas decimais, com
  arredondamento `HALF_UP`; campos editáveis usam formatter separado sem perda
  de precisão, portanto persistência e cálculos não foram alterados.
- Após feedback visual, a composição foi ajustada para não repartir o contexto
  do comprimento do passo em cards desconexos.
- Revisão independente inicial encontrou risco real de arredondamento contaminar
  campo editável e unidade duplicada no resumo de distância. Formatter editável
  separado e texto corrigido foram aplicados antes da validação final.
- Nenhuma alteração em repository, Room, snapshots, sync, navegação ou tema raiz.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou |
| `./gradlew app:compileDebugKotlin` | Passou |
| `./gradlew lint assembleDebug assembleRelease` | Passou; `BUILD SUCCESSFUL` |
| `./gradlew installDebug` | Passou; instalado no Galaxy S22 `SM-S901E` |
| `adb devices -l` | Passou; dispositivo autorizado |
| `adb shell am start -n br.com.mwconsulting.championcompanion.dev/br.com.mwconsulting.championcompanion.MainActivity` | Passou |
| Smoke ADB modal de perfis | Passou; abertura, conteúdo independente e fechamento |
| Smoke ADB modal de histórico | Passou; ações de atividade, correção, remoção e fechamento visíveis |
| Smoke ADB cancelamento de sugestão | Passou; dialog aberto e cancelado sem salvar |
| Smoke ADB abertura de atividade | Passou; atividade relacionada aberta e retorno executado |
| Verificação de precisão visual | Passou; máximo observado de 4 casas decimais |
| Smoke ADB fonte `1,3x`, landscape e tema claro/escuro | Passou; labels essenciais permaneceram acessíveis; aparelho restaurado para fonte `1,0x`, portrait e tema escuro |
| Smoke ADB final após correção de precisão | Passou; modal de perfis, modal de histórico, máximo visual de 4 casas e fonte/landscape/temas revalidados |
| Captura sanitizada | Passou; somente header e descrição genérica preservados em `024-16-captura-real-sanitizada-perfil.png` |

Não foram criados, alterados ou executados testes automatizados.

### Evidências

- Card principal expõe `Detalhes do perfil` com duas ações separadas para
  `Ver perfis aprendidos` e `Ver histórico de calibrações`.
- Card principal expõe uma única seção `Comprimento do passo`, contendo valor
  salvo, sugestão e editor manual no mesmo contexto visual.
- Modal de perfis exibiu `Corrida` e `Caminhada` separadamente.
- Modal de histórico exibiu ações `Abrir atividade`, `Corrigir` e `Remover`,
  sem executar remoção ou correção no aparelho.
- Verificação estrutural confirmou máximo de quatro casas decimais nos valores
  de comprimento de passo visíveis.
- Verificação de fluxo confirmou que limite visual não reutiliza formatter
  arredondado para preencher campos editáveis.
- Captura sanitizada registra header visual da superfície sem métricas pessoais.

### Riscos residuais

- Estados indisponíveis, loading, erro e histórico vazio não foram fabricados;
  permanecem `N/A` nesta validação física.
- Dados existentes no aparelho não foram alterados; remoção e correção reais
  permanecem não exercitadas por serem operações destrutivas/imutáveis.
- Revisão independente final após correção aprovada sem bloqueadores; confirmou
  hierarquia, modais, precisão, unidade única e checklist SDD.
