# 027-07 — Definir contrato e versionamento da timeline 1 Hz

- **Ticker:** `027`
- **Número:** `07`
- **Status:** `pending`
- **Depends on:** `027-06`

## Objetivo

Formalizar a camada derivada temporal antes de implementar algoritmos de BPM/cadência.

## Pré-condição

Gate A (`027-01` a `027-06`) fechado sem risco de integridade de passos.

## Contrato

Distinguir explicitamente:

```text
SOURCE
DERIVED
RESAMPLED_1HZ
```

SOURCE continua sendo a informação persistida/interpretada do D4.

DERIVED aplica tratamento somente sobre observações suportadas pela fonte.

RESAMPLED_1HZ cria uma grade de um segundo para consumo analítico/visual.

## Definir

- versão do algoritmo;
- parâmetros versionados de BPM;
- parâmetros versionados de cadência;
- política de outlier;
- política de gaps;
- convenção de timestamp;
- arredondamento para BPM inteiro quando necessário;
- representação de ausência em segundos sem suporte;
- regras para início/fim da sessão;
- reprocessamento futuro;
- se a série deve ser persistida ou calculada sob demanda/cacheada.

## Autoridade

A timeline 1 Hz não substitui:

- amostras D4 persistidas;
- intervalos D4;
- totais da atividade;
- estatísticas autoritativas existentes.

Qualquer uso da série interpolada para média/mínimo/máximo exige decisão explícita posterior.

## Gaps

Não interpolar através de bucket estruturalmente ausente.

A política deve preferir índices de origem/continuidade semântica a um timeout arbitrário quando ambos estiverem disponíveis.

## Persistência

Se houver nova entidade/cache:

- migration aditiva;
- vínculo com `activityId` e versão da derivação;
- regeneração determinística;
- exclusão/reprocessamento sem afetar SOURCE.

## Entregável

Atualizar ADR/spec/overview com o contrato final antes de `027-08`.

## Fora de escopo

- implementar EMA/interpolação;
- mudanças Health Connect;
- mudanças visuais.
