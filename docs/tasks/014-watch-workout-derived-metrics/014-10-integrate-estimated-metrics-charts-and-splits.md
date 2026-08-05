# 014-10 — Integrar métricas, gráficos e splits à UI

- **Ticker:** `014`
- **Número:** `10`
- **Status:** pending

## Objetivo e resultado esperado

Substituir consumidores visuais de atividades `WATCH_D3_D4` pela série canônica,
exibindo proveniência, plausibilidade, gráficos e splits com estados ausentes.

Resultado: detalhe, resumo e calendário usam a mesma distância canônica e nunca
recorrem à distância original, calibrada histórica ou estimativa agregada.

## Dependências

- `014-09` concluída.

## Arquivos permitidos

- `feature/exercises/ExercisesScreen.kt` e `ExercisesViewModel.kt`;
- read models/mappers de exercícios e calendário;
- componentes visuais existentes necessários;
- recursos de texto;
- documentação/screenshot e overview.

## Arquivos proibidos

- parser, perfil, schema/migration e Health Connect writer;
- Gradle e manifesto;
- entradas ou telas phone descontinuadas.

## Escopo

- distância, ritmo, velocidade, acumulado, gráficos, splits e BPM/km;
- rótulos **Distância estimada** e **Ritmo estimado** quando aplicáveis;
- origem `stepLengthSource` e versão da derivação em contexto apropriado;
- estados sem comprimento do passo, série inelegível ou plausibilidade bloqueada;
- orientação para configurar perfil ou reprocessar atividade;
- workflow `TREADMILL` de relógio sem exigir calibração histórica;
- acessibilidade e screenshots.

A distância original/calibrada pode aparecer somente em diagnóstico claramente
separado do valor principal.

## Fora do escopo

- mapa, rota ou GPS;
- edição da fórmula;
- reprocessamento operacional, tratado em `014-11`;
- sincronização Health Connect;
- refatoração visual não relacionada;
- testes automatizados.

## Passos de implementação

1. Adaptar read models para a série canônica.
2. Remover fallbacks visuais para original, calibrada e estimativa agregada.
3. Exibir ausência com motivo e ação apropriada.
4. Integrar acumulado e métricas temporais sem suavização.
5. Exibir splits e BPM temporal com formatos locais.
6. Exibir plausibilidade sem alterar fatos.
7. Ajustar a apresentação/workflow de `TREADMILL` para origem relógio.
8. Revisar resumo, detalhe, calendário e acessibilidade.
9. Registrar screenshots e atualizar overview.

## Critérios de aceite

- Todas as superfícies usam a mesma distância canônica.
- O total exibido termina exatamente no acumulado da série.
- Original, calibrada histórica e estimativa agregada não aparecem como fallback.
- Ausência não vira zero, salvo intervalo real com zero passos.
- Splits/BPM correspondem ao read model de `014-09`.
- Série implausível é claramente identificada e não apresentada como confiável.
- `TREADMILL` de relógio não pede calibração incompatível.
- Conteúdo possui descrições acessíveis e screenshots.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
git diff --check
git status --short
```

Não criar nem executar testes automatizados.

## Evidências

- screenshots dos estados principais;
- matriz superfície → fonte canônica;
- casos sem perfil, inelegível, implausível, zero e esteira;
- builds e roteiro manual.

## Definição de pronto

Consumidores visuais estão alinhados à série canônica, screenshots registradas e
item `014-10` marcado.

## Rollback

Ocultar superfícies novas e retornar ausência para métrica canônica. Nunca
restaurar distância original ou calibrada como fallback funcional.
