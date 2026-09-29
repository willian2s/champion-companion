# 037-06 — Validar fisicamente e fechar

- **Ticker:** `037`
- **Número:** `06`
- **Status inicial:** `pending`
- **Status atual:** `completed`

## Objetivo e resultado esperado

Validar tendência sobre banco e corridas reais existentes, fechar cálculos e
preservação de fatos, comprovar ausência de persistência/efeitos remotos e
atualizar Spec, ADR, overview e esta subtarefa somente com evidência ou `N/A`.

## Requisitos cobertos

- todos os critérios de aceite da Spec 037;
- população confirmada e janela bounded;
- pace independente, provenance e elegibilidade;
- média FC ponderada e coverage;
- ausência/parcialidade/completude;
- ordem, empate temporal e múltiplas corridas;
- Room v20, snapshots, derivação, calibração, splits e Health Connect;
- screenshot sanitizada e fechamento documental.

## Escopo incluído

- `git diff --check`;
- validação física read-only sobre banco existente;
- composição sanitizada de métodos, versões e tipos confirmados;
- cálculos independentes de pace, FC e coverage;
- comparação antes/depois de fatos persistidos e fila remota;
- reopen/recompose e confirmação de analytics efêmero;
- screenshot sem dados sensíveis;
- status/progresso documental coerentes.

## Escopo excluído

- criar/fabricar corrida, sample, fixture ou estado;
- migration, Room, tabela, coluna, cache ou backfill;
- reprocessar, recalibrar ou editar snapshots;
- alterar Health Connect, parser, BLE, workflow ou calibração;
- criar testes automatizados;
- documentar activity ID, MAC, payload BLE, localização ou BPM individual.

## Dependências

- `037-02`, `037-03`, `037-04` e `037-05` concluídas;
- banco real usado nas validações 034/035/036;
- instalação e artefato Release disponíveis para validação física;
- evidência sanitizada e procedimento local-first existente.

## Arquivos e símbolos prováveis

- `docs/integration/037-06-physical-validation.md`, se evidência for criada;
- `ChampionDatabase`, `ChampionDaos`, `RoomRepositories` apenas para inspeção;
- tabelas `workout_activities`, `workout_heart_rate_samples`,
  `workout_derivation_snapshots`, `workout_derived_intervals`,
  `workout_splits`, calibrações e `health_sync_operations` somente em dump
  sanitizado;
- Spec, ADR, overview e subtarefas 037.

## Passos de implementação/validação

1. Confirmar Room `user_version = 20` e ausência de migration/schema analítico.
2. Inspecionar população real somente por agregados sanitizados: origem,
   `confirmedType`, descarte, métodos, versões, distância/duração canônicas e
   cobertura de FC.
3. Confirmar que `suggestedType` não entrou e `TREADMILL` ficou fora.
4. Recalcular pace independentemente com os dois escalares do snapshot ativo.
5. Comparar labels de provenance com o snapshot ativo.
6. Recalcular média FC ponderada por sample-and-hold e coverage.
7. Exercitar, se naturais, sem FC, FC parcial, FC completa, pace inelegível,
   múltiplas corridas no dia e empate de instante; senão registrar `N/A`.
8. Confirmar ordem temporal e pontos distintos sem agregação diária.
9. Comparar assinaturas/contagens antes/depois para activities, snapshots,
   derivação, derived intervals, splits, calibrações e Health Connect.
10. Abrir, fechar e reabrir tela; confirmar que analytics não são persistidos.
11. Capturar screenshot sanitizada.
12. Atualizar documentação, marcar somente subtarefa concluída e alinhar
    overview/progresso.

O smoke Release é obrigatório: instalar incrementalmente sem limpar,
desinstalar, reimportar ou rederivar. Instalação Debug pode complementar, mas
não substitui a confirmação Release da leitura bounded, devido à regressão
histórica corrigida pela `035-08`. Se Release ou banco real não estiverem
disponíveis, a subtarefa permanece `blocked`, o bloqueio é registrado em
`Observações` do overview e não há fechamento.

## Testes e comandos de validação

Não criar nem executar testes automatizados novos. Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

O build completo só se aplica à execução futura de código. Nesta primeira
execução documental, apenas `git diff --check` é necessário.

## Definição de pronto

- cada critério exercitável tem evidência independente ou `N/A` justificado;
- nenhum dado foi fabricado;
- Room v20 permanece intacto;
- pace, FC, coverage e provenance fecham;
- falhas e ausências permanecem distinguíveis;
- nenhum efeito remoto ou persistência analítica foi causado;
- screenshot sanitizada existe quando a validação física ocorrer;
- Spec, ADR, overview e subtarefa têm status/progresso coerentes.

## Riscos e cuidados

- não atribuir ruído de inicialização automaticamente à feature;
- considerar o ruído de inicialização observado em `035-08` e `036-06` ao
  atribuir mudanças em `health_sync_operations`, snapshots e derivados;
- preservar risco herdado da query sem índice temporal composto;
- não confundir coverage com percentual de zona;
- restaurar qualquer configuração temporária antes do fechamento;
- registrar `N/A` em vez de criar dados;
- não prometer imutabilidade absoluta se causalidade de operação preexistente
  não puder ser isolada.

## Arquivos alterados

- `docs/integration/037-06-physical-validation.md` — evidência física
  sanitizada, cálculos independentes, preservação, reabertura e N/A.
- `docs/integration/037-06-running-performance.png` — screenshot sanitizada da
  tela de tendência.
- `docs/specs/037-running-pace-heart-rate-trend.md` — status e fechamento factual.
- `docs/decisions/adr-037-running-pace-heart-rate-trend.md` — aceite e evidência.
- `docs/tasks/037-running-pace-heart-rate-trend/037-00-overview.md` — checklist,
  progresso e observações.
- este arquivo — status, comandos, resultados e riscos.

## Decisões e desvios

- Decisão: aceitar validação do artefato Release instalado incrementalmente no
  dispositivo real, sem wipe, desinstalação, reimportação ou rederivação.
- Decisão: manter fallbacks na lista e fora dos gráficos comparáveis. A população
  física teve 21 corridas; somente 5 pontos nativos comparáveis foram plotados.
  Não há pontos adicionais escondidos à direita; os 16 fallbacks são excluídos
  deliberadamente pelo contrato.
- Decisão: usar cálculo independente read-only sobre cópia do banco físico,
  sem registrar IDs, timestamps completos, BPM individual, payload ou dados
  pessoais.
- Desvio: nenhum funcional. A captura original continha uma janela flutuante do
  dispositivo; screenshot final foi sanitizada por mascaramento antes de ser
  adicionada à evidência.
- N/A natural: sem FC, FC completa, amostra inválida, pace inelegível, snapshot
  ausente, falha estrutural de fonte, múltiplas corridas no mesmo dia e empate
  temporal não existiam no banco. Nenhum dado foi fabricado.

## Comandos executados

- `git diff --check`
- `./gradlew lint assembleDebug assembleRelease`
- `adb devices -l`
- `adb install -r app/build/outputs/apk/release/app-release.apk`
- `adb shell monkey -p br.com.mwconsulting.championcompanion -c android.intent.category.LAUNCHER 1`
- `adb shell uiautomator dump ...` e leitura da árvore de acessibilidade
- `adb exec-out screencap -p`
- cópia read-only do banco físico via `adb exec-out run-as ... cat`
- consultas SQLite read-only e cálculo independente em Python
- reabertura da tela e nova cópia read-only do banco
- sanitização da screenshot com `magick`

## Resultados e evidências

- `git diff --check` — passou.
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL`.
- Instalação Release incremental — sucesso, sem limpeza, desinstalação,
  reimportação ou rederivação.
- Dispositivo real `SM-S901E` acessível via ADB; Exercícios abriu a entrada
  `Tendência de corrida` e a tela recompôs após reabertura.
- Room `user_version = 20`; população física: 48 atividades, 1.470 samples,
  71 snapshots, 2.209 derived intervals, 211 splits, 6 calibrações e 8.275
  operações Health Connect.
- População bounded: 21 corridas confirmadas `RUNNING`; 5 nativas comparáveis
  `watch-d3-d4-v4`; 16 fallbacks listáveis. Todas as 21 tinham escalares
  positivos, snapshot ativo e temporalidade elegível.
- Recalculo independente de pace: 21 linhas fechadas por distância canônica e
  duração coberta. Recalculo independente de FC: 21 médias ponderadas por
  sample-and-hold; 21 coberturas parciais, zero ausências e zero completas.
- Árvore de acessibilidade confirmou período de 56 dias, 21 corridas,
  provenance, dois gráficos independentes e 5 pontos em cada gráfico. Os dados
  adicionais são fallbacks listados, não pontos omitidos à direita.
- Antes/depois da instalação, navegação e reabertura: mesmas contagens e
  assinaturas sanitizadas para activities, samples, snapshots, derived
  intervals, splits, calibrações e `health_sync_operations`.
- Nenhuma tabela de tendência/performance foi criada; analytics permaneceu
  efêmero.
- Screenshot sanitizada: `docs/integration/037-06-running-performance.png`.

## Riscos residuais

- Cenários N/A dependem de banco físico futuro para validação natural; não foram
  fabricados apenas para preencher cobertura.
- Corridas fallback permanecem fora dos gráficos por decisão de comparabilidade;
  sua presença na lista precisa continuar clara para evitar interpretação de
  que foram perdidas.
- O risco herdado de custo da query sem índice temporal composto permanece sem
  mudança de schema.
- A janela flutuante do dispositivo foi mascarada na screenshot; evidência
  textual preserva somente agregados sanitizados.

## Revisão independente

- Agente `review` aprovou o fechamento com correções documentais menores.
- Revisão confirmou ticker, checklist único com 6 itens, progresso `6/6`, links,
  Release incremental, Room v20, cálculos independentes, fallback fora dos
  gráficos, preservação, reabertura, N/A e screenshot sanitizada.
- Correções aplicadas após revisão: remoção de referências históricas a 037-06
  pendente e explicitação dos estados físicos N/A não exercitados.
