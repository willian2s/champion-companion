# 014-14 — Reconciliar agregado e filhos legados no Health Connect

- **Ticker:** `014`
- **Número:** `14`
- **Status:** completed

## Objetivo e resultado esperado

Remover agregado legado próprio conhecido, liberar os filhos granulares
preparados em `014-13`, retomar falhas e remover filhos próprios obsoletos sem
leitura ampla ou delete temporal.

Resultado: uma única projeção de distância por atividade, sem dupla contagem e
com checkpoints retomáveis.

## Dependências

- `014-13` concluída.

## Arquivos permitidos

- coordinator/gateway Health Connect para delete por ID exato;
- repository/DAO/mappers de projeção e operações;
- contratos de ação, checkpoint e tombstone;
- `docs/baseline/health-connect-mapping.md` e overview.

## Arquivos proibidos

- parser, derivação, perfil e UI não necessária;
- Gradle, manifesto, migrations e schemas;
- leitura de records de terceiros, delete por janela, rota e Fase 013.

## Escopo

- identidade agregada legada conhecida;
- delete-before-insert com checkpoint;
- falhas entre etapas;
- liberação dos filhos preparados;
- remoção de filhos próprios obsoletos;
- atividade sem série substituta.

## Regras

1. Resolver identidade legada somente por dados locais determinísticos.
2. Persistir intenção/checkpoint antes do efeito remoto.
3. Remover agregado conhecido por `clientRecordId` exato.
4. Persistir sucesso da remoção antes de liberar filhos de distância/velocidade.
5. Retomar inserts com os IDs, versões e payloads já congelados.
6. Comparar manifesto anterior/novo e remover filhos próprios obsoletos.
7. Sem ID conhecido, falhar fechado.
8. Não remover agregado sem série substituta elegível.
9. Não apagar por tempo, tipo amplo ou origem presumida.

## Fora do escopo

- descobrir IDs por leitura remota;
- apagar records de outros apps;
- substituir agregado quando a série for inelegível ou implausível bloqueada;
- background/WorkManager;
- testes automatizados.

## Critérios de aceite

- Agregado e filhos não coexistem após conclusão.
- Falha após delete retoma inserts sem novo ID, versão ou payload.
- Falha de marcação local após sucesso remoto permanece idempotente.
- ID desconhecido não dispara delete nem insert concorrente.
- Atividade sem série não perde agregado automaticamente.
- Filhos obsoletos próprios são removidos somente por identidade exata.
- Debug e release não cruzam identidades por `applicationId`.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- estados/checkpoints sanitizados;
- provider antes/depois;
- matriz de falhas e retries;
- verificação de ausência de dupla distância;
- builds.

## Definição de pronto

Reconciliação exata e retomável validada, sem dupla contagem, com item `014-14`
marcado.

## Rollback

Parar o reconciliador e preservar checkpoints. Não recriar agregado nem apagar
filhos automaticamente; retomada exige fluxo explícito.

## Implementação

- O gateway agora remove somente por classe de record, `clientRecordId` exato e
  lista vazia de versões; não há leitura remota nem deleção por janela.
- O repository inspeciona a identidade legada local, cria tombstone `DELETE`
  idempotente e mantém estado `UNKNOWN_ID` fail-closed.
- O coordinator grava checkpoint antes do efeito remoto, executa o delete legado
  antes dos filhos de distância/velocidade, grava o checkpoint posterior e só
  então libera inserts granulares.
- O manifesto anterior é comparado ao manifesto atual; filhos obsoletos viram
  operações `DELETE` com identidade, versão e payload já congelados.
- Atividade sem série substituta elegível não remove agregado legado. Identidade
  desconhecida não inicia delete nem inserts concorrentes.

## Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/core/database/ChampionDaos.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectGateway.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/healthconnect/HealthConnectSyncCoordinator.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HealthProjectionContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/HeartRateAndSync.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/core/model/RepositoryContracts.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/DatabaseMappers.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- este arquivo e o overview da task.

## Decisões e desvios

- Nenhuma migration, schema, Gradle, parser, derivação, UI ou código phone foi
  alterado.
- O fluxo global prepara reconciliações locais antes de processar a fila; ID
  desconhecido ou filho não reconstituível bloqueia o agregado inteiro.
- Tombstone legado pendente sem série de distância substituta bloqueia todo o
  agregado; nenhum record remoto é processado nesse estado.
- Operações legadas presas em `SYNCING` retornam a `FAILED` na recuperação,
  preservando ID, versão e checkpoint para retomada.
- A chamada Health Connect informa `clientRecordId` somente em
  `clientRecordIdsList`, mantendo `recordIdsList` vazio; sync global inicia a
  atividade antes dos efeitos remotos e repara workflow já sincronizado para
  permitir conclusão após retry.
- `DELETE` usa mesma identidade natural da projeção e incrementa versão somente
  ao transformar operação legada ou filho obsoleto; retry idêntico reutiliza
  identidade, versão e payload congelados.
- Falha ao marcar sucesso local após efeito remoto mantém operação falha e
  checkpoint retomável; nova tentativa repete delete exato de forma idempotente.

## Comandos executados e resultados

- `./gradlew app:compileDebugKotlin` — PASS após correção das referências do
  coordinator;
- `./gradlew lint assembleDebug assembleRelease` — primeira execução falhou por
  corrida conhecida do lint/Hilt em fontes geradas; validações isoladas
  subsequentes passaram;
- `./gradlew lint` — PASS após execução isolada;
- `./gradlew assembleDebug assembleRelease` — PASS;
- `./gradlew installDebug` — PASS, APK instalado no dispositivo `SM-S901E`;
- `git diff --check` — PASS;
- `git status --short` — diff local revisado;
- testes automatizados — não criados nem executados, conforme escopo.

## Evidências

- Query DAO libera somente tombstones `DELETE` do legado do relógio, mantendo
  inserts legados bloqueados.
- Ordenação do coordinator coloca delete agregado, deletes de filhos e depois
  inserts; o gate de distância/velocidade permanece fechado até checkpoint de
  reconciliação.
- Gateway seleciona classe Health Connect pelo tipo local e envia somente ID
  conhecido.
- Builds debug/release, lint, instalação e `git diff --check` passaram.

## Riscos residuais

- Provider Health Connect real não foi exercitado para delete exato, delete
  idempotente, falha entre efeito remoto e marcação local ou ausência de record;
  validação física permanece em `014-15`.
- Revisão independente anterior encontrou bloqueios no fail-closed global; a
  correção foi validada por revisão independente final, sem blockers funcionais.
- Não foram criados cenários automatizados para matriz de falhas, conforme fora
  do escopo explícito.
