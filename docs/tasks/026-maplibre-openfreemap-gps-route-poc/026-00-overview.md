# 026 — Overview: PoC MapLibre + OpenFreeMap com GPS e rota

- **Status geral:** pending
- **Spec:** [026-maplibre-openfreemap-gps-route-poc.md](../../specs/026-maplibre-openfreemap-gps-route-poc.md)
- **Progresso:** 2/8 subtarefas concluídas

## Objetivo

Validar fisicamente, em fluxo debug-only e sem reativar treinos iniciados pelo celular, se MapLibre Compose + OpenFreeMap conseguem apresentar posição GPS e uma rota que cresce em tempo real, com câmera utilizável e estado totalmente efêmero.

## Decisão experimental

```text
MapLibre Compose 0.14.0
        +
OpenFreeMap public instance
        +
GPS nativo do Champion Companion
```

Premissas atuais:

- MapLibre Compose vem do Maven Central;
- OpenFreeMap não exige cadastro, API key, token ou pagamento;
- OpenFreeMap não oferece SLA;
- atribuição cartográfica deve permanecer;
- `PhoneWorkoutPolicy` continua bloqueando a antiga feature phone;
- nenhuma rota será persistida nesta PoC.

## Checklist

- [x] [026-01-alinhar-documentacao-e-adr.md](026-01-alinhar-documentacao-e-adr.md)
- [x] [026-02-configurar-maplibre-e-openfreemap.md](026-02-configurar-maplibre-e-openfreemap.md)
- [ ] [026-03-criar-entrypoint-debug-e-estado.md](026-03-criar-entrypoint-debug-e-estado.md)
- [ ] [026-04-integrar-gps-readiness-e-filtro.md](026-04-integrar-gps-readiness-e-filtro.md)
- [ ] [026-05-renderizar-mapa-posicao-e-rota.md](026-05-renderizar-mapa-posicao-e-rota.md)
- [ ] [026-06-implementar-camera-stop-e-reset.md](026-06-implementar-camera-stop-e-reset.md)
- [ ] [026-07-validar-fisicamente-e-casos-negativos.md](026-07-validar-fisicamente-e-casos-negativos.md)
- [ ] [026-08-fechar-poc-e-registrar-decisao.md](026-08-fechar-poc-e-registrar-decisao.md)

## Observações

026-01 concluída. ADR-024 registra MapLibre Compose + OpenFreeMap como stack
experimental `debug-only`; guards da Fase 013 permanecem vigentes fora da
exceção. Não há bloqueios para 026-02. Termos, disponibilidade e ausência de
SLA do OpenFreeMap devem ser revalidados antes da configuração.

026-02 concluída com MapLibre Compose `0.14.0` em `debugImplementation` via
Maven Central. `settings.gradle.kts` não precisou de alteração. Release não
resolve MapLibre; style Positron e atribuição permanecem definidos para a tela
debug das subtarefas seguintes. Renderização física ainda não foi validada.

## Ordem

```text
026-01 → 026-02 → 026-03 → 026-04 → 026-05 → 026-06 → 026-07 → 026-08
```

## Gate

O gate exige validação física de um percurso externo de aproximadamente 500 m ou mais, observando:

- mapa-base carregado;
- atribuição preservada;
- GPS pronto;
- rota crescendo somente com pontos aceitos;
- pan/zoom manual;
- recentralização;
- stop com rota preservada em memória;
- enquadramento da rota final;
- reset;
- segundo ciclo sem contaminação do primeiro.

Resultado final obrigatório:

```text
go
ou
go_with_adjustments
ou
no_go
```

## Guard rails

- não reabrir Fase 013;
- não liberar `PhoneWorkoutPolicy`;
- não usar `WorkoutRecordingService`;
- não adicionar Room/migration;
- não usar Health Connect;
- não usar BLE;
- não adicionar CTA em superfícies de produto;
- não registrar coordenadas;
- não criar credenciais de mapa;
- não remover atribuição do mapa;
- não criar ou alterar testes automatizados sem pedido explícito do usuário.
