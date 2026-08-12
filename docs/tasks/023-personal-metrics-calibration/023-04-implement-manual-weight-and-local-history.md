# 023-04 — Implementar peso manual e histórico local

- **Status:** `pending`
- **Depends on:** `023-03`

## Objetivo

Concluir a parte de peso que hoje existe apenas como persistência/leitura parcial,
permitindo registrar novas medições e consultar histórico local sem criar uma
segunda infraestrutura de Health Connect.

## Persistência e domínio

Reutilizar:

- `WeightMeasurementEntity`;
- `WeightMeasurement`;
- `WeightUnit`;
- `PersonalMetricsRepository`;
- fila Health Connect já existente para `WeightRecord`.

Adicionar somente as APIs ausentes para:

- salvar medição manual;
- observar/listar histórico de peso em ordem temporal;
- manter último peso derivado do histórico persistido;
- resolver conflitos/erros de storage com estado UI seguro.

V1 da Fase 16 usa quilogramas na UI. O enum existente não obriga criar preferência
de unidade.

## UI

A tela Saúde > Peso deve deixar de ser placeholder e oferecer:

- último peso;
- CTA para adicionar nova medição;
- entrada decimal em kg;
- data/hora atual como padrão;
- lista cronológica de medições;
- origem manual;
- estado vazio;
- feedback de persistência/erro.

Gráfico é opcional. Não criar gráfico se isso aumentar escopo sem necessidade.

## Health Connect

Após persistência local bem-sucedida:

- reutilizar a preparação/enqueue de `WeightRecord` existente;
- metadata continua manual;
- falha remota não remove a medição local;
- retry continua pelo mecanismo foreground existente;
- não realizar readback.

## Fora do escopo

- meta de peso;
- IMC;
- composição corporal;
- edição retroativa complexa;
- importação de balança;
- leitura Health Connect.

## Checklist

- [ ] API de save manual criada
- [ ] histórico observável criado
- [ ] último peso deriva de persistência real
- [ ] UI permite registrar kg
- [ ] histórico aparece após restart
- [ ] falha HC preserva sucesso local
- [ ] fila existente é reutilizada
- [ ] tela não promete gráfico ausente
- [ ] spec/overview atualizados

## Aceite

Registrar pelo menos duas medições, reiniciar o app e confirmar ordem/histórico.
Se Health Connect estiver indisponível, os dois registros locais continuam
visíveis.