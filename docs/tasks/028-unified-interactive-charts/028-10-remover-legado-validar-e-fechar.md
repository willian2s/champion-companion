# 028-10 — Remover legado, validar e fechar

- **Ticker:** `028`
- **Número:** `10`
- **Status:** `pending`
- **Depends on:** `028-09`

## Objetivo

Consolidar a Spec 028 após todas as migrações, remover somente o código legado de gráficos que ficou comprovadamente sem uso e registrar a validação final integrada.

## Escopo

- inventariar novamente consumidores de `Canvas` usados para visualização de dados;
- remover implementações antigas substituídas de passos, distância, BPM e detalhe de treino;
- remover helpers de normalização/desenho sem consumidores;
- remover `HealthChart` legado somente se nenhum consumidor legítimo permanecer;
- confirmar que features migradas não importam Vico diretamente;
- revisar dependências e imports sem uso;
- atualizar Spec 028, overview e ADR caso o resultado final tenha ajustes factuais;
- registrar evidência final das superfícies migradas.

## Guard rails

- não remover Canvas decorativo ou infraestrutura não relacionada apenas por busca textual;
- não fazer refatorações funcionais fora da Spec 028;
- não alterar Room, BLE, Health Connect ou métricas;
- não alterar a granularidade dos dados;
- não criar testes automatizados sem pedido explícito.

## Checklist final funcional

Confirmar:

- histórico de passos;
- histórico de distância;
- histórico de frequência cardíaca;
- BPM no detalhe do treino;
- distância canônica no detalhe do treino;
- cadência D4 no detalhe do treino;
- marker/tooltip comum;
- eixos e unidades reais;
- gaps preservados;
- tema claro/escuro;
- rolagem vertical convivendo com os gestos;
- scroll/zoom apenas onde explicitamente habilitados.

## Critérios de aceite

1. Nenhum gráfico migrado mantém engine Canvas paralela.
2. Nenhuma feature importa Vico diretamente.
3. O boundary `core/designsystem/chart` concentra renderer, interação e tema.
4. Código legado sem consumidores é removido de forma comprovada.
5. Não há mudança de domínio, persistência ou sincronização decorrente da consolidação.
6. A Spec 028 e o overview refletem o estado final real.
7. Validação física integrada não encontra regressão bloqueante nas superfícies migradas.

## Validação final

Executar:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Em aparelho, repetir smoke sanitizado das superfícies migradas e registrar screenshots representativos.

## Fechamento

Ao concluir, atualizar:

- `docs/specs/028-unified-interactive-charts.md` para `completed`;
- `docs/tasks/028-unified-interactive-charts/028-00-overview.md` para `10/10`;
- este arquivo com resultado final, limitações e evidências;
- ADR-027 somente se a decisão arquitetural efetiva tiver mudado.