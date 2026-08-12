# 023-03 — Completar Perfil com altura e meta diária de passos

- **Status:** `completed`
- **Depends on:** `023-02`

## Objetivo

Transformar a tela Perfil em ponto de gestão dos dados básicos realmente usados
pelos cálculos, sem adicionar campos pessoais sem consumidor funcional.

## Altura

Adicionar edição explícita de altura:

- entrada em centímetros;
- valor opcional, positivo e finito;
- persistir no `UserProfile` existente;
- recalcular `heightSuggestionMeters` após persistência;
- não aceitar automaticamente a sugestão;
- não substituir `stepLengthProfile` atual;
- preservar `updatedAt` coerente;
- acionar o fluxo local-first de Height/Health Connect já existente somente após
  sucesso local e conforme configuração/permissões atuais.

A task deve adicionar API de repository específica para atualizar altura em vez
de reconstruir perfil na ViewModel.

## Meta diária de passos

Adicionar edição da meta já persistida em DataStore:

- valor inteiro positivo;
- usar o sanitizer/policy existente onde aplicável;
- Profile mostra a meta atual;
- salvar por `AppPreferencesRepository`/contrato existente, sem acesso direto ao
  DataStore pela UI;
- Home, Saúde e Calendário devem continuar consumindo a mesma fonte e refletir a
  nova meta naturalmente.

Não mover a meta para Room.

## Perfil base

Preservar os cards atuais:

- comprimento de passo base;
- origem `MANUAL`/`HEIGHT_SUGGESTION_ACCEPTED`;
- edição manual;
- sugestão baseada em altura.

Preparar espaço de UI/estado para os perfis aprendidos que serão conectados em
`023-05/023-06`, sem inventar valores quando não houver calibração.

## Fora do escopo

- peso (023-04);
- calibração de atividade (023-05);
- meta de peso;
- nome/nascimento/sexo;
- unidade imperial;
- zonas de FC.

## Checklist

- [x] altura editável
- [x] validação de altura segura
- [x] sugestão recalculada sem aceite automático
- [x] step length base preservado
- [x] meta diária editável
- [x] meta continua em DataStore
- [x] consumers existentes refletem nova meta
- [x] enqueue de Height reutiliza fluxo existente
- [x] UI sem novos dados pessoais desnecessários
- [x] spec/overview atualizados

## Evidências

- `PersonalMetricsRepository.updateHeight` adiciona API específica para altura;
  `RoomRepositories` preserva perfil base, ID e `updatedAt` monotônico.
- Altura aceita valor positivo/finito em centímetros ou vazio para remoção; a
  sugestão é recalculada após persistência e nunca substitui passo base aceito.
- Meta é lida e salva por `AppPreferencesRepository`; DataStore permanece fonte
  única consumida por Home, Saúde e Calendário.
- Enqueue de altura reutiliza `ensureReconciledOperation` e
  `HealthConnectSyncCoordinator`. Ao remover altura, operação existente usa a
  mesma identidade como `DELETE`; sem identidade remota existente não é criado
  tombstone cego.
- Mutation de altura e preparação da operação Health Connect são serializadas
  na transação local; operações `SYNCING` bloqueiam alteração para evitar envio
  de versão obsoleta. O coordinator usa início condicional pela versão atual.
- Perfil expõe perfis aprendidos por `observeLearnedStepLengthProfiles`, com
  estados distintos de carregamento, indisponibilidade e vazio para
  corrida/caminhada, sem valores artificiais.
- Cards de passo base, origem, edição manual e sugestão por altura foram
  preservados. Nenhum dado pessoal fora do escopo foi adicionado.
- Validação: `./gradlew app:compileDebugKotlin --no-daemon` passou após
  `./gradlew --stop`; `./gradlew app:lintDebug --no-daemon` passou;
  `./gradlew lint assembleDebug assembleRelease --no-daemon` passou;
  `git diff --check` passou.
- Uma tentativa inicial de compilação falhou por cache incremental do daemon
  Kotlin já registrado; o daemon foi parado e a compilação seguinte passou.
- Nenhum teste automatizado novo foi criado ou executado, conforme guard rail.

## Decisões e desvios

- A remoção de altura não cria operação remota sem identidade própria. Quando há
  operação existente, prepara remoção exata usando contrato DELETE já vigente;
  falhas remotas não desfazem o valor local removido.
- A fila continua sendo preparada localmente; configuração, permissões e escrita
  Health Connect permanecem responsabilidade do coordinator existente.
- Falha de leitura dos perfis aprendidos aparece como indisponibilidade na UI,
  não como ausência de calibração.

## Riscos residuais

- Persistência após reinício e escrita/remoção real no Health Connect ainda
  dependem de validação física em dispositivo, prevista para `023-07`.
- Permanecem warnings de APIs/deprecações no build; nenhum bloqueou compilação ou
  lint.

## Aceite

Alterar altura e meta de passos, reiniciar o app e confirmar persistência. Alterar
altura não pode mudar silenciosamente o comprimento do passo já aceito.
