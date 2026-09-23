# 037-06 — Validar fisicamente e fechar

- **Ticker:** `037`
- **Número:** `06`
- **Status inicial:** `pending`

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
