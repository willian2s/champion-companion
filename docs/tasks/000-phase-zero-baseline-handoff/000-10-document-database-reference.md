# Task 000-10 - Documentar Referencia De Banco

## Objetivo

Documentar banco SQLite atual do MotionLog apenas como referencia conceitual.

## Contexto

Champion Companion tera banco proprio, iniciara vazio e nao migrara dados do MotionLog.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- `docs/baseline/current-baseline.md`
- `docs/roadmap/champion-companion-roadmap.md`
- `docs/decisions/adr-001-native-rewrite.md`

## Dependencias

- `000-01`
- `000-02`

## Arquivos Ou Areas De Entrada

- Codigo de persistencia, schema, migrations, repositories e queries em `../motionlog-app`

## Arquivos Exatos De Saida

- `docs/baseline/database-reference.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

- Requisitos originais 91-99.
- Tabelas, colunas, tipos, relacionamentos, indices, estados, deduplicacao, transacoes, temporarios, dados necessarios e desnecessarios.
- Destacar ausencia de migracao.

## Fora Do Escopo

- Criar schema Room.
- Criar migration plan.
- Compatibilidade com banco antigo.
- Importador de dados.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Localizar persistencia do MotionLog.
2. Registrar entidades/tabelas e campos evidenciados.
3. Registrar relacionamentos e indices.
4. Registrar regras de deduplicacao e transacoes.
5. Registrar dados temporarios.
6. Registrar dados que nao devem existir no novo modelo, incluindo coordenadas/rotas.
7. Registrar lacunas para `000-14`.
8. Atualizar checklist do overview.

## Criterios De Aceite

- Documento e referencia conceitual, nao plano de migracao.
- Banco proprio e vazio do Champion Companion esta explicito.
- Dados proibidos estao destacados.

## Validacao Final

- Confirmar que nenhum schema novo foi criado.
- Confirmar que `motionlog-app` nao foi alterado.
