# 014-15 — Executar validação física completa

- **Ticker:** `014`
- **Número:** `15`
- **Status:** completed

## Objetivo e resultado esperado

Executar gate físico completo com Champion C001, Galaxy S22 e Health Connect
real, sem corrigir produção dentro da própria task.

Resultado: evidência sanitizada da cadeia importação → derivação → revisão → UI →
reprocessamento → projeção Health Connect, com gates `PASS`, `FAIL` ou `N/A`
justificado.

## Dependências

- `014-01` a `014-14` concluídas.
- O ramo `014-11` de reprocessamento precisa estar validado.

## Arquivos permitidos

- `docs/integration/014-*` para evidências sanitizadas;
- screenshots no local adotado pelo repositório;
- correções documentais estritamente factuais;
- overview para registrar o resultado da task.

## Arquivos proibidos

- código de produção, Gradle, manifesto, migrations e schemas;
- correções funcionais escondidas no gate;
- reabertura da Fase 013;
- payload BLE, IDs pessoais ou valores de saúde identificáveis em evidência
  pública.

## Gates

### A — Importação e parser

- importação D3/D4 permanece explícita;
- novas importações usam `d4-v2`;
- sequência de referência soma `3.737` passos;
- raw permanece preservado;
- distância/calorias temporais raw não viram métricas.

### B — Índices, duração e plausibilidade

- índices contínuos elegíveis funcionam;
- lacuna/duplicata falha fechada;
- `1.637 s` produz último intervalo de 17 s;
- plausibilidade é registrada sem redistribuir passos.

### C — Perfil e derivação

- valor manual e sugestão aceita funcionam;
- sugestão não aceita não persiste;
- distância total é exatamente a soma dos intervalos;
- edição do perfil não muda snapshot antigo;
- ausência de perfil mantém canônico ausente.

### D — Métricas, UI e esteira

- acumulado termina na distância canônica;
- cadência, velocidade e ritmo usam a mesma série;
- splits e BPM temporal correspondem à linha do tempo;
- original/calibrada não aparecem como fallback;
- `TREADMILL` de relógio não exige calibração histórica incompatível.

### E — Reprocessamento

- legado `d4-v1` recuperável reprocessa explicitamente sem BLE;
- revisão, `recordedAt`, título, notas e calibração histórica permanecem;
- repetição idempotente;
- nenhuma sincronização automática.

### F — Health Connect

- uma operação existe por filho;
- `DistanceRecord` usa intervalos positivos;
- `SpeedRecord` usa midpoint;
- retry não duplica;
- agregado legado e filhos não coexistem após reconciliação;
- falha parcial e delete-before-insert retomam pelos mesmos IDs/versões/payloads.

### G — Regressão

- importação D3/D4, revisão, calendário, BPM, dispositivo e sync explícito
  continuam funcionais;
- nenhum fluxo phone, GPS, rota, mapa ou BLE live é reativado.

## Passos de implementação

1. Registrar versões sem identificadores sensíveis.
2. Executar cada gate e guardar evidência mínima.
3. Usar somente dados necessários e sanitizados.
4. Registrar divergências e riscos residuais.
5. Abrir follow-up para qualquer correção funcional.
6. Manter a fase `in_progress` se houver gate bloqueador.
7. Atualizar overview após revisão independente.

## Comandos permitidos

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Critérios de aceite

- Todos os gates obrigatórios possuem evidência suficiente.
- Gates indisponíveis recebem `N/A` apenas com justificativa factual aceita.
- Qualquer `FAIL` bloqueador impede conclusão da task.
- Nenhuma correção de produção está misturada à validação.
- Evidências não expõem dados sensíveis.

## Definição de pronto

Gate físico revisado e item `014-15` marcado. A fase ainda não é encerrada nesta
task; o fechamento documental pertence a `014-16`.

## Rollback

Reverter somente afirmações documentais incorretas. Evidências válidas permanecem
históricas.

## Registro de execução

### Arquivos alterados nesta consolidação documental

- `docs/integration/014-15-physical-validation.md`;
- `docs/integration/014-15-health-connect-provider-follow-up.md`;
- este arquivo;
- `014-00-overview.md`.

As evidências visuais previamente registradas permanecem em
`docs/integration/014-15-physical-validation-evidence/`, incluindo
`import-summary.png`, `health-connect-summary.png`,
`reprocess-confirmation.png`, `reprocess-blocked.png` e os registros da
revalidação após `014-17`.

Nenhum arquivo de produção, Gradle, manifesto, migration ou schema foi alterado.

### Decisões e desvios

- O gate foi executado no Galaxy S22 conectado ao Champion C001, usando APK
  `debug` instalado localmente.
- Dados exibidos na evidência foram reduzidos a contagens, estados e métricas
  necessárias; BPM e identificadores foram omitidos da documentação textual.
- A tentativa inicial permaneceu bloqueada porque a sessão não ofereceu fixture
  controlada para lacuna/duplicata/duração residual/plausibilidade nem atividade
  legada não sincronizada recuperável. Esse registro é histórico; não representa
  bloqueio ativo após a revalidação.
- Reprocessamento de atividade já sincronizada foi confirmado até a mensagem de
  bloqueio, sem criar operação Health Connect ou alterar snapshot.
- Acesso inicial posterior ao Health Connect real não encontrou os records
  esperados de distância e velocidade; velocidade estava sem permissão. O
  sucesso exibido pelo aplicativo comprovava somente estado local da fila. Esse
  fato abriu o follow-up funcional em
  `docs/integration/014-15-health-connect-provider-follow-up.md` e permanece
  registrado como histórico da causa do escalonamento.
- Após `014-17`, ambas as permissões foram concedidas no package debug correto e
  o provider exibiu `DistanceRecord` e `SpeedRecord`. Revogações isoladas de
  `WRITE_SPEED` e `WRITE_DISTANCE` produziram falhas parciais nomeadas; após
  reautorização, os retries foram idempotentes e não houve duplicação observada.
- Gate F foi aprovado somente para esses cenários exercitados. A observação
  manual do provider não foi transformada em leitura pelo aplicativo.

### Comandos executados e interação física

```text
adb devices
./gradlew installDebug
adb shell monkey -p br.com.mwconsulting.championcompanion.dev 1
adb exec-out screencap -p > <evidência sanitizada>
adb shell am force-stop br.com.mwconsulting.championcompanion.dev
./gradlew lint assembleDebug assembleRelease
git diff --check
git status --short
```

Toques e swipes foram interação manual no dispositivo; não constituem teste
automatizado.

Não foram criados nem executados testes automatizados.

### Resultados e evidências

- APK `1.0-dev` instalado com sucesso no Galaxy S22 `SM-S901E`.
- Importação explícita terminou com 3/3 blocos, 2 inseridos, 1 parcial, 0
  inválidos e 1 pulado; uma atividade ficou aguardando revisão.
- Revisão local mostrou derivação canônica, série, métricas, intervalos, BPM e
  splits.
- Na tentativa inicial, sync explícito Health Connect foi marcado como sucesso
  local; resumo posterior mostrou 13 atividades sincronizadas e 0 com erro, mas
  o provider real não exibiu distância/velocidade e velocidade estava sem
  permissão. Esse resultado é histórico e foi superado pela revalidação final.
- Reprocessamento legado exibiu confirmação específica e terminou fechado,
  preservando operação e snapshot quando suporte futuro era necessário.
- Após reinício do aplicativo, dados locais permaneceram disponíveis.
- `./gradlew lint assembleDebug assembleRelease`: `BUILD SUCCESSFUL`.
- `./gradlew installDebug`: `BUILD SUCCESSFUL`.
- `git diff --check`: sem saída, sucesso.

Evidência detalhada: `docs/integration/014-15-physical-validation.md`.

### Resultado consolidado dos gates

- Gate A: `PASS` para importação explícita e cadeia física exercitada; a soma de
  `3.737` passos e campos raw permanecem sustentados pela evidência estática das
  subtarefas anteriores, sem leitura ampla pelo app.
- Gates B, C, D, E e G: `N/A`, pelas justificativas factuais já registradas na
  evidência: ausência de fixtures controladas para índices/duração/plausibilidade,
  sugestão por altura, cenário `TREADMILL`, legado não sincronizado e repetição
  integral da regressão, respectivamente.
- Gate F: `PASS` para os cenários exercitados: permissões concedidas com
  `DistanceRecord`/`SpeedRecord` visíveis no provider; revogação isolada de cada
  permissão com falha parcial nomeada; retry após reautorização sem duplicação
  observada.
- Status final da subtarefa: `completed`. `014-16` continua pendente e não foi
  alterada.

### Riscos residuais

- Falta fixture física para testar falha fechada de índices, duração `1.637 s` e
  plausibilidade.
- Sugestão de comprimento baseada em altura não foi aceita fisicamente.
- Reprocessamento de legado sincronizado não foi concluído fisicamente por exigir
  suporte futuro de projeção; não foi aplicada correção funcional dentro desta
  task.
- O provider foi observado manualmente; o app continua write-only e não declara
  confirmação remota por leitura.
- A matriz física cobre os cenários exercitados de autorização, falha parcial e
  retry, não todos os fixtures sintéticos dos Gates B, C, D, E e G.

### Revalidação final após 014-17

- Nova limpeza de dados, configuração manual de `0,8818 m`, importação explícita
  de 3 treinos e revisão com aceite explícito foram concluídas no Galaxy S22.
- Uma atividade elegível sincronizou com ambas as permissões concedidas. O
  provider real exibiu `3,463 km` em distância granular e `7,245 km/h` em
  velocidade para `Champion Companion Dev`.
- `WRITE_SPEED` foi revogada isoladamente; Settings exibiu `Velocidade —
  Pendente`. A permissão foi restaurada pelo fluxo explícito `Conectar`.
- O bloqueio inicial de ausência de records foi resolvido para o cenário com
  ambas as permissões concedidas. A segunda corrida da tentativa inicial tinha
  duração zero e não materializou derivação após reprocessamento explícito; esse
  limite permanece histórico, sem impedir a matriz física posterior.

### Falha parcial e retry com treino novo

- Dois treinos novos foram importados explicitamente. O treino de 15 minutos foi
  classificado como `Corrida`; o de 20 minutos foi mantido como `Treino livre`.
- Com `WRITE_SPEED` revogada, a corrida produziu `Falha parcial por capacidade`:
  o filho `Velocidade [speed-series]` falhou por permissão pendente e permaneceu
  recuperável; a distância aceita não foi reescrita.
- Após reautorização, `Tentar novamente no Health Connect` levou a atividade a
  `Sincronizada`. O provider exibiu distância acumulada `3,517 km` e um único
  intervalo novo de velocidade `0,202 km/h` para `12:33–12:49`.
- Não foram observadas duplicações no provider durante retry. O app permaneceu
  write-only e informou que o provider não foi lido para confirmar records.
- Com `WRITE_DISTANCE` revogada, o treino livre de 20 minutos exibiu falha
  parcial nomeada para três operações de distância; reautorização e retry
  terminaram `Sincronizada`, com `3,570 km` acumulados no provider e sem
  duplicação observada.
- Gate F passa para os cenários de falha parcial e retry. Os dois treinos antigos
  continuam pendentes de revisão e não integram a matriz física concluída.
