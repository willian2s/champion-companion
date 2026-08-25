# 028-07 — Migrar histórico de frequência cardíaca

- **Ticker:** `028`
- **Número:** `07`
- **Status:** `pending`
- **Depends on:** `028-06`

## Objetivo

Migrar o histórico consolidado de frequência cardíaca para o sistema compartilhado de gráficos, com eixos reais e interação padronizada.

## Escopo

- adaptar as amostras consolidadas já disponíveis na tela de saúde para contratos Champion de série temporal;
- substituir o `HealthChart` manual onde ele for o renderer vigente do histórico de BPM;
- exibir eixo temporal coerente com os dados reais;
- exibir eixo Y em `bpm`;
- permitir tap/drag com marker e tooltip contendo horário/período e BPM do ponto selecionado;
- preservar manual + D1 + amostras reais de atividades conforme o read model atual;
- preservar gaps e ausência de amostras.

## Guard rails

- não reamostrar BPM;
- não suavizar nem interpolar pontos;
- não criar timeline 1 Hz;
- não mudar o agrupamento ou consolidação já feito pelo read model;
- scroll/zoom somente se a densidade real da série justificar;
- remover `HealthChart` apenas quando não houver mais consumidor que dependa dele.

## Critérios de aceite

1. Histórico de BPM usa o renderer compartilhado.
2. Eixo Y apresenta BPM real, não escala normalizada sem labels.
3. Eixo X representa os tempos/buckets reais disponíveis.
4. Marker/tooltip seleciona pontos de forma previsível.
5. Gaps continuam ausências.
6. Acessibilidade resume período, quantidade de dados e lacunas relevantes.
7. A feature não importa Vico diretamente.

## Validação física

Validar:

- série curta e série mais densa quando disponíveis;
- toque e drag;
- rolagem da tela durante interação;
- tema claro/escuro;
- fonte ampliada;
- estado vazio.

Registrar screenshot sanitizado sem expor dados pessoais além do necessário à evidência.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.