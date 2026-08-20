# 026-08 — Fechar PoC e registrar decisão

- **Ticker:** `026`
- **Número:** `08`
- **Status:** pending

## Objetivo

Consolidar evidências da Spec 026, classificar o resultado do gate e registrar a decisão sem promover automaticamente a PoC para feature de produção.

## Resultado esperado

Spec, overview, task final, baseline e ADR relevante deixam explícito um dos resultados:

- `go`;
- `go_with_adjustments`;
- `no_go`.

## Dependências

- `026-07` concluída ou bloqueada com evidência factual suficiente para decisão.

## Escopo incluído

- consolidar resultados físicos;
- registrar limitações e riscos residuais;
- atualizar status da spec e overview;
- atualizar ADR experimental com resultado, quando apropriado;
- atualizar baseline factual;
- definir recomendação de próximo passo;
- decidir preservação ou remoção posterior da PoC.

## Escopo excluído

- implementar feature definitiva;
- liberar `PhoneWorkoutPolicy`;
- adicionar foreground service;
- adicionar Room/persistência;
- integrar Health Connect;
- alterar detalhe real de atividade;
- criar automaticamente nova fase.

## Classificação

### `go`

Usar quando:

- mapa é estável;
- rota cresce corretamente;
- interação é fluida;
- GPS + filtro produzem trajetória visualmente coerente;
- arquitetura experimental não apresenta bloqueio relevante.

### `go_with_adjustments`

Usar quando o conceito está validado, mas há ajustes necessários de câmera, estilo, frequência de atualização, filtro ou integração Mapbox antes da efetivação.

### `no_go`

Usar quando existe incompatibilidade, desempenho inadequado, problema de custo/licença/credencial, risco de privacidade não aceitável ou acoplamento arquitetural que torne a abordagem inadequada.

## Decisão posterior permitida

Se `go` ou `go_with_adjustments`, recomendar nova spec independente para avaliar feature real, incluindo explicitamente:

- substituição ou manutenção do `PhoneWorkoutPolicy`;
- recuperação seletiva do legado da Fase 013;
- foreground service;
- tela apagada/background;
- persistência Room de rota;
- recuperação de sessão;
- mapa na sessão ativa;
- mapa no detalhe da atividade;
- Mapbox como provedor definitivo ou nova comparação;
- custos e credenciais;
- privacidade;
- Health Connect `ExerciseRoute`;
- integração opcional com Champion C001.

Nada disso é implementado nesta task.

## Arquivos prováveis

- `docs/specs/026-mapbox-gps-route-poc.md`;
- `docs/tasks/026-mapbox-gps-route-poc/026-00-overview.md`;
- este arquivo;
- `docs/baseline/current-baseline.md`;
- ADR criada em `026-01`;
- evidência sanitizada de `026-07`.

## Passos de implementação

1. Revisar todos os critérios de aceite da spec.
2. Consolidar comandos e evidências da validação física.
3. Marcar cada item como aprovado, falho ou `N/A` justificado.
4. Classificar gate.
5. Atualizar ADR e baseline com resultado factual.
6. Atualizar checklist `026-00` e status da spec.
7. Registrar riscos residuais e recomendação de próximo trabalho.
8. Verificar que nenhuma linguagem autoriza implicitamente feature de produção.

## Validação

Se a task alterar apenas documentação:

```bash
git diff --check
```

Se houver ajuste final de código ou Gradle incorporado ao fechamento, repetir:

```bash
./gradlew lint assembleDebug assembleRelease
```

## Definição de pronto

- gate possui classificação explícita;
- spec e overview refletem estado real;
- evidências são sanitizadas;
- Fase 013 continua com estado histórico correto;
- feature de produção não foi autorizada implicitamente;
- próximo passo está claro e independente desta conversa;
- riscos e limitações remanescentes estão registrados.
