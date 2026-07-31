# 017-03 — Alinhar Selecionar treino e Preparar

- **Ticker:** `017`
- **Número:** `03`
- **Status:** pending

## Objetivo e resultado esperado

Alinhar cabeçalho e hierarquia visual de Selecionar treino e Preparar ao padrão
de Exercícios e das telas de detalhe. Resultado esperado: header independente,
margem e ritmo oficiais, conteúdo fora do card de cabeçalho e retorno preservado,
sem alterar preparação, countdown ou início de sessão.

## Requisitos e critérios cobertos

- R1, R2, R3, R6, R7, R8, R9 e R10 da spec 017.
- Critérios de aceite 1 a 3 e 7 a 10.

## Escopo incluído

- `WorkoutTypeSelectionShell`;
- `WorkoutPreparationShell` para caminhada, corrida, treino livre e esteira;
- layout local usado por essas duas superfícies;
- header em card com título, descrição e heading semântico único;
- posição visual coerente da ação `Voltar`, mantendo callbacks atuais;
- margem `ChampionSpacing.extraLarge` e ritmo
  `ChampionSpacing.large`;
- fonte aumentada, landscape, screenshots e evidências.

## Escopo excluído

- `ActiveWorkoutShell`, `WorkoutRecoveryShell` e
  `WorkoutFinishConfirmationShell`;
- estado, intents, ViewModel, countdown, GPS, permissões e notificação;
- foreground service, sessão, rota, BLE, Health Connect e correlação;
- textos de requisito, tipos de treino, regras de habilitação e callbacks;
- `ExercisesScreen` e `ExercisesHeader`, usados somente como referência;
- tarefas `013-08` a `013-15` e testes automatizados.

## Dependências

- Spec e overview 017 aprovados.
- Nenhuma dependência técnica em `017-01` ou `017-02`; ordem é operacional.
- Deve respeitar trabalho concorrente válido da Fase 13 em `WorkoutShells.kt`.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/exercises/WorkoutShells.kt`:
  `WorkoutTypeSelectionShell`, `WorkoutPreparationShell`, `WorkoutShellCard` ou
  novo layout local restrito a seleção/preparação.
- Referências sem edição prevista:
  `ExercisesScreen.kt::ExercisesHeader`, `MetricDetailHeader`,
  `ChampionSpacing` e `ChampionShapeLarge`.
- Este arquivo e `017-00-overview.md` para status e evidências.

## Passos de implementação

1. Revalidar diff e código vigente de `WorkoutShells.kt`, separando alterações
   concorrentes da Fase 13 do escopo visual 017.
2. Definir composição local para seleção e preparação com margem externa e ritmo
   oficiais, sem modificar shells ativo, recuperação ou confirmação.
3. Separar título e descrição em header card equivalente a `ExercisesHeader`;
   renderizar cards de tipo e preparação como conteúdo irmão, não dentro do card
   de header.
4. Manter `Voltar` disponível em posição coerente com navegação contextual das
   telas de detalhe, usando exatamente callbacks atuais e executando
   `ExitWorkoutPreparation` no mesmo momento em que ocorre hoje.
5. Preservar tipos, cards, permissões, GPS, notificação, relógio opcional,
   countdown, notices, condições de habilitação e texto de segurança.
6. Adicionar heading semântico único sem mesclar indevidamente cards acionáveis
   ou gerar anúncio duplicado do conteúdo dinâmico.
7. Validar seleção e preparação de tipo com e sem GPS, voltar antes de zero,
   fonte aumentada e landscape.
8. Revisar diff para comprovar ausência de mudança em sessão ativa, recuperação,
   serviço e estado.
9. Registrar evidências e marcar somente `017-03` no overview após conclusão
   real.

## Testes e comandos de validação

Não criar, alterar ou executar testes automatizados.

Executar após implementação:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Smoke manual obrigatório:

- abrir Exercícios e entrar em Selecionar treino;
- abrir preparação de caminhada ou corrida e uma preparação sem GPS;
- confirmar título, descrição, conteúdo, ação Voltar e ordem de foco;
- voltar antes do countdown e confirmar ausência de nova sessão, atividade, rota
  ou serviço;
- sem iniciar operação nova, conferir estados de countdown/GPS disponíveis;
- repetir superfícies com fonte aumentada e landscape;
- fazer smoke estático ou visual de treino ativo/recuperação para confirmar que
  ficaram fora do diff comportamental, quando houver sessão segura disponível.

## Evidências esperadas

- screenshot portrait de Selecionar treino;
- screenshot portrait de Preparar em estado representativo;
- screenshot de preparação sem GPS ou com GPS, conforme estado disponível;
- screenshots de fonte aumentada e landscape cobrindo ambas as superfícies;
- resultados dos comandos, roteiro de retorno e confirmação de ausência de
  sessão antes do fluxo atual.

## Definição de pronto

- Duas superfícies atendem margem, ritmo, header, tipografia e semântica da spec.
- Conteúdo não permanece agrupado dentro do card de header.
- Voltar, preparação e countdown permanecem funcionalmente equivalentes.
- Shells ativo, recuperação e confirmação não recebem mudança não intencional.
- Lint, builds, instalação e screenshots estão registrados.
- Nenhum teste automatizado é tocado ou executado.
- Status muda para concluído e somente item `017-03` é marcado no overview.

## Riscos e cuidados

- `WorkoutShellCard` é compartilhado; evitar alteração global que redesenhe
  superfícies fora do pedido.
- `ExitWorkoutPreparation` precisa ocorrer antes da navegação de retorno como no
  fluxo atual; mudança visual não pode alterar ordem funcional.
- Conteúdo de preparação é longo; preservar scroll, alvos de toque e visibilidade
  do CTA em landscape e fonte aumentada.
- Semântica do header não deve absorver cards de GPS, countdown ou tipos de
  treino.
- Não resolver bloqueios, requisitos ou validações pendentes de `013-08` a
  `013-15` nesta subtarefa.
