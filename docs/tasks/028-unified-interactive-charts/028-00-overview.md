# 028 — Sistema unificado de gráficos interativos

- **Status geral:** `in_progress`
- **Spec:** [028-unified-interactive-charts.md](../../specs/028-unified-interactive-charts.md)
- **Progresso:** `5/10` subtarefas concluídas

## Objetivo

Substituir motores locais de gráficos desenhados manualmente por uma infraestrutura compartilhada, interativa e consistente, preservando os dados e sua granularidade real.

## Guard rails

- features não importam Vico diretamente;
- novos gráficos de dados passam por `core/designsystem/chart`;
- não criar timeline 1 Hz, suavização ou interpolação para fins visuais;
- gaps continuam ausência de dado;
- não alterar Room, BLE ou Health Connect por necessidade de apresentação;
- não criar nem executar testes automatizados sem pedido explícito;
- scroll/zoom são opt-in por caso de uso;
- mudanças visuais exigem validação física/screenshot quando a task correspondente pedir aceite.

## Checklist

- [x] `028-01` — auditar todos os gráficos atuais
- [x] `028-02` — adotar Vico e registrar decisão arquitetural
- [x] `028-03` — criar contratos e tema compartilhados
- [x] `028-04` — implementar eixos, formatters, marker e interação
- [x] `028-05` — migrar histórico de passos
- [ ] `028-06` — migrar histórico de distância
- [ ] `028-07` — migrar histórico de frequência cardíaca
- [ ] `028-08` — migrar gráficos do detalhe do treino
- [ ] `028-09` — adicionar gráfico de cadência D4 ao detalhe do treino
- [ ] `028-10` — remover legado, validar e fechar

## Dependências

```text
028-01
  ↓
028-02
  ↓
028-03
  ↓
028-04
  ↓
028-05 → 028-06 → 028-07 → 028-08 → 028-09
                                      ↓
                                    028-10
```

As migrações de tela são deliberadamente sequenciais para permitir revisão visual isolada e remoção segura do legado somente após substituição comprovada.

## Estado atual

- `028-01` confirmou cinco implementações manuais relevantes e duplicação de engine Canvas entre superfícies.
- `028-02` adotou Vico `3.2.3` com módulos Compose + Material 3 e registrou ADR-027.
- `028-03` criou a boundary neutra e `028-04` entregou renderer Vico com eixos,
  formatters, marker, tooltip, gaps, acessibilidade e interação opt-in.
- `StepsHistoryChart` foi migrado para `ChampionBarChart`, preservando pontos,
  lacunas, meta, acessibilidade e lista do histórico. Eixo Y mostra valores sem
  unidade redundante; eixo X usa datas compactas e tema compartilhado mantém
  barras/marker coloridos. Períodos 30/90 habilitam scroll horizontal; 7 dias
  permanece sem scroll/zoom.
- GitHub Actions run `32845432177` passou `./gradlew lint assembleDebug assembleRelease` após correção de uma edição acidental do ID do plugin Hilt durante a integração inicial.

## Observações

- Nenhum bloqueio ativo em `028-05`.
- Validação física registrou 7 dias, gaps, tap/drag, tooltip, 30/90 dias, scroll horizontal e rolagem vertical no `SM-S901E`; tema claro e fonte ampliada permanecem riscos residuais documentados na subtarefa.
- Próxima subtarefa elegível: `028-06`.

## Verificação

Para subtarefas com código ou Gradle:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar ou executar testes automatizados sem solicitação explícita.
