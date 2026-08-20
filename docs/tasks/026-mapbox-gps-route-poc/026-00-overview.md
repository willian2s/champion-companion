# 026 — Overview: PoC Mapbox com GPS e rota em tempo real

- **Status geral:** pending
- **Spec:** [026-mapbox-gps-route-poc.md](../../specs/026-mapbox-gps-route-poc.md)
- **Progresso:** 0/8 subtarefas concluídas

## Objetivo

Executar uma prova de conceito debug-only para validar Mapbox + GPS nativo + rota em tempo real no Champion Companion, sem reabrir a Fase 013 nem reativar treinos iniciados pelo celular.

## Checklist

- [ ] [026-01-alinhar-documentacao-e-adr.md](026-01-alinhar-documentacao-e-adr.md)
- [ ] [026-02-configurar-mapbox-e-credenciais.md](026-02-configurar-mapbox-e-credenciais.md)
- [ ] [026-03-criar-entrypoint-debug-e-estado.md](026-03-criar-entrypoint-debug-e-estado.md)
- [ ] [026-04-integrar-gps-readiness-e-filtro.md](026-04-integrar-gps-readiness-e-filtro.md)
- [ ] [026-05-renderizar-mapa-posicao-e-rota.md](026-05-renderizar-mapa-posicao-e-rota.md)
- [ ] [026-06-implementar-camera-stop-e-reset.md](026-06-implementar-camera-stop-e-reset.md)
- [ ] [026-07-validar-fisicamente-e-casos-negativos.md](026-07-validar-fisicamente-e-casos-negativos.md)
- [ ] [026-08-fechar-poc-e-registrar-decisao.md](026-08-fechar-poc-e-registrar-decisao.md)

## Ordem de execução

```text
026-01 → 026-02 → 026-03 → 026-04 → 026-05 → 026-06 → 026-07 → 026-08
```

## Guard rails

- Não alterar `PhoneWorkoutPolicy` para liberar treino phone.
- Não usar `WorkoutRecordingService`.
- Não criar rota persistida, migration ou integração Health Connect.
- Não adicionar CTA da PoC à navegação de produto.
- Preferir `src/debug` para entrypoint, Activity e implementação experimental.
- Não versionar tokens Mapbox.
- Não registrar coordenadas, GeoJSON ou bounds no Logcat.
- Não criar, alterar ou executar testes automatizados sem pedido explícito do usuário.
- Mudanças visuais exigem evidência sanitizada, sem revelar localização real.

## Gate final

A PoC só conclui após validação física em aparelho real e classificação explícita:

- `go`;
- `go_with_adjustments`;
- `no_go`.

Build verde ou mapa estático isolado não encerram a spec.

## Validação esperada

Para subtarefas com código ou Gradle:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Quando houver validação no aparelho:

```bash
./gradlew installDebug
```

## Observações

- Fase 013 permanece `closed` / `partially_delivered` / `product_reorientation`.
- Esta spec é um work item independente e não cria nova fase de roadmap.
- ADR nova será criada na `026-01` para registrar a exceção experimental e a fronteira debug-only.
- Mapbox é decisão experimental; promoção para produção exige nova spec.
