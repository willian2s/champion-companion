# 026-01 — Alinhar documentação e ADR da PoC

- **Ticker:** `026`
- **Número:** `01`
- **Status:** completed

## Objetivo

Registrar formalmente a exceção debug-only que autoriza a PoC GPS + mapa sem reabrir a Fase 013 e documentar MapLibre + OpenFreeMap como stack cartográfica experimental.

## Resultado esperado

Fontes autoritativas deixam claro que:

- Fase 013 continua `closed/partially_delivered`;
- treinos iniciados pelo celular continuam descontinuados;
- a Spec 026 autoriza somente uma PoC isolada;
- MapLibre Compose é o renderizador experimental;
- OpenFreeMap é o provedor inicial do mapa-base;
- nenhuma decisão de produção é tomada nesta task.

## Requisitos e critérios cobertos

- Spec 026: critérios 1 a 4;
- exceção explícita aos guards vigentes somente para `src/debug`;
- decisão cartográfica registrada em ADR;
- histórico da Fase 013 preservado.

## Escopo incluído

- atualizar `AGENTS.md`;
- atualizar `docs/baseline/current-baseline.md`;
- criar ADR-024 ou próximo número livre confirmado;
- referenciar Spec 026 nas fontes operacionais necessárias;
- registrar que a escolha anterior de Mapbox foi abandonada antes da implementação por exigir configuração de billing/forma de pagamento incompatível com a premissa da PoC.

## Escopo excluído

- código Android;
- Gradle;
- manifesto;
- dependências;
- navegação;
- Room;
- Health Connect;
- BLE;
- implementação de mapa ou GPS.

## Dependências

- Spec `026-maplibre-openfreemap-gps-route-poc.md`;
- `AGENTS.md`;
- baseline atual;
- ADR-014 histórica;
- ADR-023 atual como último número observado no planejamento inicial; confirmar novamente antes de criar ADR-024.

## Arquivos prováveis

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/decisions/adr-024-maplibre-openfreemap-gps-poc.md` ou próximo número livre;
- spec e overview 026 para atualização de status/evidência ao concluir.

## Passos de implementação

1. Revalidar baseline e sequência atual de ADRs.
2. Registrar em `AGENTS.md` uma exceção estrita para a Spec 026, sem remover os guards gerais da Fase 013.
3. Atualizar baseline com a existência da PoC planejada e sua natureza debug-only/efêmera.
4. Criar ADR registrando MapLibre Compose + OpenFreeMap como decisão experimental.
5. Documentar ausência de cadastro, API key e pagamento como premissa observada, além da ausência de SLA.
6. Registrar obrigação de atribuição cartográfica.
7. Registrar que a aprovação da PoC não autoriza feature de produção.
8. Atualizar overview somente com evidência real da execução.

## Validação

```bash
git diff --check
```

Como esta task é documental, não executar Gradle apenas para produzi-la.

## Definition of Done

- guards gerais continuam válidos fora da Spec 026;
- exceção debug-only está explícita e não ambígua;
- ADR cartográfica experimental existe;
- MapLibre + OpenFreeMap substituem Mapbox na direção vigente da Spec 026;
- nenhuma credencial ou billing é introduzido;
- diff documental passa em `git diff --check`;
- overview registra resultado real da task.

## Riscos e cuidados

- não reescrever o encerramento histórico da Fase 013;
- não transformar a PoC em backlog de produto;
- não remover `PhoneWorkoutPolicy`;
- não afirmar estabilidade ou SLA do OpenFreeMap;
- não registrar como permanente uma condição gratuita que deve ser revalidada no futuro.

## Execução

- **Status:** completed
- **Arquivos alterados:** `AGENTS.md`, `docs/baseline/current-baseline.md`,
  `docs/decisions/adr-024-maplibre-openfreemap-gps-poc.md`,
  `docs/tasks/026-maplibre-openfreemap-gps-route-poc/026-00-overview.md` e este
  arquivo.
- **Decisões e desvios:** confirmado ADR-024 como próximo número livre. Registrada
  exceção exclusiva da Spec 026 no source set `debug`, sem reabrir a Fase 013;
  MapLibre Compose `0.13.0` + OpenFreeMap/Positron foram formalizados como stack
  experimental. Mapbox foi abandonado antes da implementação por exigir billing/
  forma de pagamento incompatível com a premissa da PoC. Nenhum desvio de escopo.
- **Comandos executados:** `git status --short`; `git diff --check`; `git diff
  --no-index --check /dev/null
  docs/decisions/adr-024-maplibre-openfreemap-gps-poc.md`.
- **Resultados e evidências:** ticker `026` confirmado na spec, overview e oito
  subtarefas; overview mantém uma única seção `## Checklist` com um item por
  subtarefa. `git diff --check` e a checagem explícita do ADR ainda não rastreado
  concluídos sem saída/erros. Nenhum comando Gradle executado, conforme validação
  documental da task.
- **Riscos residuais:** OpenFreeMap continua sem SLA; termos, disponibilidade e
  política gratuita precisam ser revalidados em `026-02`. ADR-024 não autoriza
  feature de produção nem altera os guards gerais.
