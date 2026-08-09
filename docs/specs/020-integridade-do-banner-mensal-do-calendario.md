# 020 — Integridade do banner mensal do Calendário

- **Ticker:** `020`
- **Status:** concluída
- **Contexto:** o Calendário pode exibir `Dados parciais do mês` quando a
  observação da fila Health Connect encontra operações não resolvidas fora dos
  agregados pertencentes ao mês consultado.

## Objetivo

Restringir diagnósticos de integridade da fila Health Connect aos agregados
presentes na janela do Calendário, preservando degradações reais do mês e
ausência legítima de passos ou frequência cardíaca em treinos.

## Comportamento atual encontrado

`LocalCalendarRepository` solicita integridade para o conjunto de agregados do
mês, mas `RoomRepositories` calcula `hasUnresolvedAggregate` e
`hasInvalidOperation` usando todas as operações válidas do banco. Assim, uma
operação inválida ou semanticamente não resolvida de outro período pode
contaminar o snapshot mensal. Operações ausentes por falta de passos ou FC não
devem criar issue por si só; somente operações persistidas e inelegíveis na
janela devem continuar degradando de forma observável.

## Requisitos

1. Calcular flags de integridade somente para operações cujo agregado pertence
   ao conjunto solicitado.
2. Preservar operações válidas do mês e seus estados de sincronização.
3. Manter issue para operação inválida ou agregado não resolvido pertencente ao
   mês consultado.
4. Não transformar ausência de passos ou FC em zero, operação artificial ou
   sucesso de sincronização.
5. Não alterar texto, navegação, writer, importador, contratos de projeção ou
   comportamento remoto do Health Connect.
6. Não expor IDs, métricas, payloads ou timestamps individuais em diagnóstico.

## Critérios de aceite

1. Operação inválida ou não resolvida fora do mês não produz `Dados parciais do
   mês` no mês consultado.
2. Operação inválida ou não resolvida dentro do mês continua produzindo issue
   segura.
3. Treino sem passos ou FC, sem operação correspondente inelegível, permanece
   visível com ausência segura de métricas.
4. Operações válidas continuam aparecendo no resumo diário e no estado da fila.
5. `git diff --check`, lint e builds debug/release passam.
6. Nenhum teste automatizado é criado ou executado, conforme regra do projeto.

## Abordagem

- transportar o agregado recuperável junto ao resultado interno de mapeamento;
- filtrar operações e flags de integridade pelo conjunto de agregados recebido;
- remover comparação global no read model do Calendário;
- validar compilação, lint, builds e diff sem alterar a UI.

## Alternativas descartadas

- esconder ou remover o banner: mascararia degradações reais;
- tratar falta de passos/FC como falha: ausência legítima não é corrupção;
- alterar o texto do banner: não corrige escopo do diagnóstico;
- alterar writer ou apagar operações antigas: fora da causa confirmada e do
  escopo mínimo.

## Arquivos prováveis

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/RoomRepositories.kt`;
- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalCalendarRepository.kt`;
- documentação SDD 020.

## Riscos e mitigação

- Operações malformadas sem agregado recuperável continuam sem associação de
  janela; manter diagnóstico global apenas não é seguro para o Calendário, por
  isso a consulta deve limitar flags a entidades cujo agregado seja recuperável.
- Sem banco físico reproduzível, validar por inspeção de fluxo e gates de
  compilação; não fabricar registros.

## Validação

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar, alterar ou executar testes automatizados.

## Ordem das subtarefas

```text
020-01
```

## Fora do escopo

- UI, texto, navegação e read models visuais;
- importação D3/D4 e persistência de métricas;
- writer, coordinator, provider ou nova sincronização Health Connect;
- migração Room, schema e testes automatizados.
