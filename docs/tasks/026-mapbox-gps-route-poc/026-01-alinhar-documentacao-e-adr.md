# 026-01 — Alinhar documentação e ADR da PoC

- **Ticker:** `026`
- **Número:** `01`
- **Status:** pending

## Objetivo

Registrar formalmente a exceção que autoriza uma PoC debug-only de GPS + Mapbox sem reabrir a Fase 013 nem alterar a direção vigente de produto.

## Resultado esperado

`AGENTS.md`, baseline e uma nova ADR concordam que:

- a Fase 013 continua encerrada;
- `PhoneWorkoutPolicy` continua bloqueando treino phone;
- a Spec 026 autoriza somente uma PoC experimental isolada;
- promoção para feature definitiva exige nova spec e nova decisão.

## Escopo incluído

- atualizar `AGENTS.md` com exceção explícita da Spec 026;
- atualizar `docs/baseline/current-baseline.md`;
- confirmar o próximo número livre de ADR;
- criar ADR para Mapbox experimental debug-only;
- referenciar Spec 026 nos documentos atualizados.

## Escopo excluído

- código Kotlin;
- Gradle;
- Mapbox SDK;
- permissões;
- GPS runtime;
- navegação;
- Room, Health Connect e BLE.

## Arquivos prováveis

- `AGENTS.md`;
- `docs/baseline/current-baseline.md`;
- `docs/decisions/adr-024-*.md` ou próximo número livre confirmado;
- `docs/specs/026-mapbox-gps-route-poc.md` somente se surgir inconsistência factual.

## Regras

1. Não remover o guard geral contra expansão de treino phone.
2. Inserir exceção estreita, nominativa e vinculada à Spec 026.
3. ADR deve registrar Mapbox como escolha experimental, não definitiva.
4. ADR deve registrar implicações de rede, credenciais, privacidade e custo observado.
5. Nenhum documento deve sugerir nova fase de roadmap.

## Passos de implementação

1. Ler inventário atual de ADRs e confirmar sequência.
2. Atualizar `AGENTS.md` para permitir apenas a PoC debug-only desta spec.
3. Atualizar baseline factual com novo work item experimental.
4. Criar ADR com contexto, decisão, alternativas, consequências e fronteira de validade.
5. Verificar links e coerência com Fase 013, Spec 025 e roadmap completo.

## Validação

```bash
git diff --check
```

Não executar Gradle por se tratar somente de documentação.

## Definição de pronto

- exceção documental explícita existe;
- Fase 013 continua encerrada;
- nenhuma feature phone é reativada documentalmente;
- ADR possui número correto e fronteira debug-only;
- spec, overview e baseline não se contradizem;
- task registra resultado e riscos residuais antes de ser marcada concluída.
