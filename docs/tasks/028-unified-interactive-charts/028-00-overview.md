# 028 — Sistema unificado de gráficos interativos

- **Status geral:** `in_progress`
- **Spec:** [028-unified-interactive-charts.md](../../specs/028-unified-interactive-charts.md)
- **Progresso:** `1/10` subtarefas concluídas

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
- [ ] `028-02` — adotar Vico e registrar decisão arquitetural
- [ ] `028-03` — criar contratos e tema compartilhados
- [ ] `028-04` — implementar eixos, formatters, marker e interação
- [ ] `028-05` — migrar histórico de passos
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

## Verificação

Para subtarefas com código ou Gradle:

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar ou executar testes automatizados sem solicitação explícita.
