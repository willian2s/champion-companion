# Task 012A-02 - Simplificar resumo e realocar feedback D1

## Status

pending

## Objetivo

Transformar o card `Resumo atual e ações` em `Resumo atual`, removendo CTAs fixos redundantes e preservando o fluxo D1 com ações contextuais fora do resumo.

## Resultado esperado

- o overview deixa de exibir botões fixos;
- o título do card vira `Resumo atual`;
- a medição manual continua acessível apenas em `HeartRateSessionSurface`;
- progresso, metadata, cancelamento, retry e dismiss do D1 continuam acessíveis em superfície contextual.

## Requisitos cobertos

- remover os botões fixos do overview;
- eliminar a redundância do CTA de medição;
- ajustar textos para orientar pull-to-refresh;
- preservar controle operacional do D1.

## Escopo incluído

- `feature/health/HealthMetricDetailScreens.kt`

## Escopo excluído

- alterações em histórico, gráfico, persistência, Health Connect ou roadmap;
- criação de testes novos.

## Dependências

- `012A-01`.

## Arquivos e símbolos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/feature/health/HealthMetricDetailScreens.kt`
- `HeartRateOverviewCard`
- `HeartRateD1ImportPanel`
- `HeartRateSessionSurface`

## Passos de implementação

1. Renomear o card para `Resumo atual`.
2. Remover a fileira fixa de CTAs do `HeartRateOverviewCard`.
3. Atualizar textos do estado sem medições para orientar “puxe para atualizar”.
4. Manter a medição manual apenas na superfície dedicada.
5. Garantir que o painel D1 continue exibindo:
   - progresso;
   - metadata da última importação;
   - cancelamento em andamento;
   - retry em falha recuperável;
   - dismiss em estados terminais.
6. Confirmar que dados já persistidos, gráfico e histórico permanecem visíveis durante o fluxo D1.

## Testes e comandos de validação

Não criar testes novos.

Validação manual esperada nesta subtarefa:

- overview sem botões fixos;
- card renomeado para `Resumo atual`;
- textos sem referência a botão removido;
- medição manual acessível somente na superfície própria;
- painel D1 continua utilizável em sucesso, erro, parcial e cancelamento.

## Definição de pronto

- UI sem redundância fixa no overview;
- D1 continua controlável por ações contextuais;
- nenhum texto da tela referencia botão inexistente;
- subtarefa e overview atualizados com status e evidências.

## Riscos e cuidados

- não perder cancelamento explícito do D1;
- não esconder feedback importante do D1 após remover a fileira fixa de ações;
- não mover a ação de medição para lugar novo.
