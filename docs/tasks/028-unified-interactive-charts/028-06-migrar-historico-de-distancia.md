# 028-06 — Migrar histórico de distância

- **Ticker:** `028`
- **Número:** `06`
- **Status:** `pending`
- **Depends on:** `028-05`

## Objetivo

Migrar o histórico de distância para a infraestrutura compartilhada, eliminando a implementação Canvas paralela ao histórico de passos sem alterar a semântica de estimativa já vigente.

## Escopo

- adaptar `CalendarDistanceHistorySnapshot` para contratos Champion de gráfico;
- renderizar barras via componente compartilhado;
- formatar eixo/tooltip em quilômetros ou metros conforme a escala escolhida pela camada de apresentação;
- preservar dias sem estimativa como gaps;
- preservar o texto que deixa claro que ausência não significa zero quilômetros;
- reutilizar marker, tooltip, eixos e tema da infraestrutura comum;
- manter períodos de 7, 30 e 90 dias.

## Guard rails

- não recalcular distância;
- não alterar `estimatedDistanceMeters` nem suas regras de derivação;
- não preencher gaps;
- não criar escala divergente apenas para melhorar aparência;
- remover somente o Canvas local de distância substituído.

## Critérios de aceite

1. `DistanceHistoryChart` deixa de implementar barras diretamente.
2. A feature não importa Vico.
3. Tooltip informa data e distância do bucket selecionado.
4. Dias sem estimativa permanecem identificados como ausência de dado.
5. Eixo Y apresenta valores reais com unidade explícita.
6. 7/30/90 dias permanecem legíveis e consistentes com passos.
7. Tema e interação seguem a mesma linguagem do gráfico migrado na `028-05`.

## Validação física

Validar em aparelho:

- 7, 30 e 90 dias;
- dias com e sem estimativa;
- tap/drag;
- rolagem vertical;
- tema claro/escuro;
- fonte ampliada representativa.

Registrar screenshot sanitizado.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.