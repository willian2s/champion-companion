# Roadmap pós-037 — Evolução das análises de corrida

## Contexto

A Spec 037 consolidou uma visão factual da relação entre ritmo e frequência cardíaca em corridas confirmadas do relógio.

A validação física mostrou um ponto importante para a evolução do produto:

```text
21 corridas RUNNING
5 nativas comparáveis
16 fallback por comprimento de passo
```

Antes de ampliar a quantidade de análises, o próximo ciclo deve melhorar a qualidade e a cobertura dos dados comparáveis e, em seguida, evoluir para volume, recordes pessoais e cadência.

Este roadmap não reabre as Fases 17–25, encerradas anteriormente. Os itens abaixo são specs independentes de evolução do produto.

---

## Visão geral

| Spec | Tema | Pergunta principal | Dependência | Prioridade |
|---|---|---|---|---|
| 038 | Recuperação de comparabilidade histórica | Quanto do histórico antigo pode usar a pipeline nativa atual com segurança? | 037 | Alta |
| 039 | Volume e consistência de corrida | Quanto e com que frequência estou correndo ao longo do tempo? | 038 recomendada | Alta |
| 040 | Recordes pessoais e melhores esforços | Quais são minhas melhores marcas comparáveis? | 038 | Média/Alta |
| 041 | Tendência de cadência | Como minha cadência varia entre corridas e ao longo do tempo? | 038 | Média |

---

# Spec 038 — Recuperação de comparabilidade histórica de corridas

## Objetivo

Entender e, quando tecnicamente seguro, reduzir a quantidade de corridas antigas que permanecem apenas como fallback por comprimento de passo.

A Spec começa como auditoria, não como reprocessamento.

## Motivação

A validação física da 037 encontrou:

```text
corridas confirmadas RUNNING: 21
nativas comparáveis:          5
fallback:                     16
```

As próximas análises ficariam limitadas se esse histórico não fosse compreendido antes.

## Perguntas a responder

- Quais parser versions existem nas corridas fallback?
- Quais corridas ainda possuem header D4 e fatos brutos suficientes para reconstrução nativa?
- Quais podem ser reproduzidas deterministicamente usando a pipeline atual?
- Quais devem permanecer fallback?
- O reprocessamento altera apenas:
  - snapshot;
  - derived intervals;
  - splits;
- Ou pode interferir também em workflow e projeções Health Connect?
- Como garantir zero novos efeitos remotos?
- Como tratar atividades já `SYNCED`?
- O reprocessamento deve ser:
  - por atividade;
  - em lote explícito;
  - manutenção técnica;
  - ou não disponibilizado ao usuário?

## Regra central

Nenhuma corrida deve ser promovida apenas para aumentar a quantidade de pontos dos gráficos.

A promoção só pode ocorrer quando os fatos persistidos comprovarem que a derivação nativa atual é reproduzível sem inventar dados.

## Gate de decisão

```text
GO
muitas corridas podem ser recuperadas com segurança
→ implementar reprocessamento explícito

NO-GO
dados históricos não suportam derivação nativa confiável
→ preservar fallback e encerrar a spec documentalmente
```

## Fora de escopo

- backfill automático durante inicialização;
- alteração silenciosa de histórico;
- edição de fatos raw D4;
- reimportação obrigatória do relógio;
- novos efeitos Health Connect;
- reconstrução heurística de dados ausentes.

## Resultado esperado

Maximizar a população histórica comparável sem comprometer provenance, integridade local ou sincronizações já realizadas.

---

# Spec 039 — Volume e consistência de corrida

## Objetivo

Responder de forma factual:

> Quanto e com que frequência estou correndo?

## Contexto

Após as Specs 034–037, o produto já responde:

```text
034 → zonas + carga individual
035 → carga ao longo do tempo
036 → distribuição de intensidade
037 → ritmo × FC ao longo do tempo
```

Ainda falta uma leitura clara de volume de treino.

## Métricas candidatas

Por período:

- número de corridas;
- duração total;
- distância total comparável;
- distância média por corrida;
- duração média;
- dias com corrida;
- distribuição semanal.

Exemplo:

```text
Semana 1   3 corridas   18,4 km   1h52
Semana 2   2 corridas   11,2 km   1h08
Semana 3   4 corridas   24,7 km   2h25
```

## Provenance

Duração e quantidade de corridas podem utilizar toda a população válida.

Distância deve respeitar provenance e comparabilidade.

A 039-01 deve congelar a política exata para:

- distâncias nativas;
- distâncias calibradas;
- fallbacks;
- somas agregadas.

Fallback pode participar de contagens e duração mesmo quando não participar da distância comparável.

## Janela temporal

Avaliar durante o planejamento:

- 28 dias;
- 56 dias;
- 12 semanas.

Direção inicial preferida:

```text
12 semanas
```

por oferecer contexto suficiente para uma leitura semanal sem histórico ilimitado.

## Não objetivos

- score de consistência;
- classificação de semana boa/ruim;
- streak como métrica principal;
- meta arbitrária;
- recomendação de aumento/redução de volume;
- avaliação fisiológica.

## Resultado esperado

Uma visão longitudinal simples e factual de frequência, duração e volume de corrida.

---

# Spec 040 — Recordes pessoais e melhores esforços

## Objetivo

Responder:

> Quais são minhas melhores marcas observadas nas corridas comparáveis?

## Base técnica disponível

O app já possui:

- série canônica;
- distância acumulada;
- splits;
- pace;
- snapshots;
- provenance;
- população `RUNNING` confirmada.

## Primeira versão

Exemplos de recordes:

```text
Melhor 1 km
4:52 /km
12 set

Melhor 5 km
25:48
3 set
```

## Decisão importante

Separar explicitamente:

```text
split materializado de 1 km
≠
melhor janela contínua de 1 km dentro da corrida
```

A spec deve decidir se a primeira versão usa:

### Opção A — Splits existentes

Usa os splits oficiais já materializados.

Vantagens:

- infraestrutura existente;
- sem nova análise temporal complexa.

Limitação:

- não representa necessariamente o melhor trecho contínuo da atividade.

### Opção B — Best effort em janela móvel

Calcula o melhor intervalo contínuo sobre a série canônica.

Vantagens:

- mais próximo da definição intuitiva de recorde.

Exige:

- política de interpolação;
- precisão temporal;
- tratamento de provenance;
- desempate determinístico.

## Distâncias candidatas

Começar com conjunto pequeno:

```text
1 km
5 km
10 km
```

Somente quando a atividade possuir distância suficiente e provenance comparável.

Distâncias futuras podem ser adicionadas quando justificadas por dados reais.

## Estabilidade

Empates precisam de política determinística.

Exemplo:

```text
menor duração
→ empate
→ data definida por regra congelada
```

A saída não pode mudar entre recomposições sem alteração dos fatos.

## Não objetivos

- previsão de tempo de prova;
- VO2max;
- equivalência de performance;
- estimativa de potencial;
- ranking externo;
- score de performance.

## Resultado esperado

Uma superfície factual de melhores marcas baseada somente em trechos comparáveis e reproduzíveis.

---

# Spec 041 — Tendência de cadência de corrida

## Objetivo

Responder:

> Como minha cadência observada varia entre corridas e ao longo do tempo?

## Base técnica disponível

Já existem:

- intervalos D4;
- passos por intervalo;
- duração efetiva;
- cadência derivada;
- `StepsCadenceRecord`;
- série canônica;
- splits.

## Métricas candidatas

Por corrida:

- cadência média ponderada;
- coverage;
- mínimo e máximo, caso a semântica seja validada.

Trend principal:

```text
data → cadência média
```

Possível evolução futura:

```text
split → pace + cadência
```

A primeira versão deve permanecer somente longitudinal entre corridas.

## Não objetivos

Não inferir:

- cadência ideal;
- cadência ruim;
- regra de 180 spm;
- eficiência de passada;
- overstriding;
- risco de lesão;
- recomendação técnica.

## Resultado esperado

Uma visão factual da cadência observada ao longo do tempo, preservando provenance e cobertura.

---

# Ordem de execução

Dependências conceituais:

```text
037 — Pace × FC
        │
        ▼
038 — Comparabilidade histórica
        │
        ├─────────────┐
        ▼             ▼
039 — Volume       041 — Cadência
        │
        ▼
040 — Recordes pessoais
```

Ordem de execução recomendada:

```text
038
 ↓
039
 ↓
040
 ↓
041
```

Essa sequência permite usar as descobertas do banco físico de uma spec para refinar a próxima.

---

# Itens deliberadamente fora deste roadmap

## Recovery / readiness

Ainda não existem fatos suficientes para uma análise robusta de recuperação.

Seriam necessários sinais adicionais como:

- HRV;
- frequência cardíaca de repouso;
- sono;
- tendência de recuperação consistente.

## VO2max

Ritmo e frequência cardíaca isolados não sustentam uma estimativa confiável.

## Score de eficiência pace / FC

A Spec 037 corretamente evitou essa inferência.

Ainda não são normalizados:

- percurso;
- inclinação;
- vento;
- temperatura;
- superfície;
- fadiga;
- sono;
- estrutura do treino.

## Predição de prova

Fora de escopo até existir base histórica e modelo explicitamente validado.

## Recomendação de treino

O produto permanece descritivo, não prescritivo.

## Scatter pace × FC

Pode ser reconsiderado futuramente, mas não resolve uma necessidade de produto mais importante que as Specs 038–041.

---

# Princípios transversais

Todas as specs devem preservar:

- local-first;
- ausência explícita;
- provenance;
- comparabilidade;
- leitura bounded;
- analytics efêmero quando possível;
- nenhum score sem base factual;
- nenhuma inferência causal;
- nenhuma recomendação fisiológica;
- nenhuma fabricação de dados;
- nenhuma alteração silenciosa de histórico;
- Health Connect write-only e explícito;
- zero efeitos remotos em telas analíticas.

Sempre preferir:

```text
fato observado
>
inferência frágil
```

e:

```text
N/A
>
fabricar cenário para preencher validação
```

---

# Estado desejado após a Spec 041

A seção Exercícios passa a responder dimensões complementares:

```text
Carga
→ quanto estresse cardiovascular ocorreu

Intensidade
→ onde o tempo foi distribuído nas zonas

Volume
→ quanto e com que frequência houve corrida

Performance observada
→ pace, FC e melhores marcas

Mecânica observável
→ cadência
```

O objetivo é evoluir o Champion Companion como ferramenta pessoal de leitura histórica confiável, baseada exclusivamente nos fatos que o relógio e o app conseguem sustentar.
