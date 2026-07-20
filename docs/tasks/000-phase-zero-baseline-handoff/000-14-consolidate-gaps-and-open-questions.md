# Task 000-14 - Consolidar Lacunas E Questoes Abertas

## Objetivo

Centralizar ambiguidades, contradicoes, duplicidades, dependencias ausentes e requisitos dificeis ou impossiveis de executar.

## Contexto

Duvidas nao podem ficar apenas em respostas de sessao ou comentarios soltos. Devem virar documento persistido.

## Documentos Obrigatorios Para Leitura

- `docs/tasks/000-00-overview.md`
- `docs/specs/000-phase-zero-baseline-handoff.md`
- todos os artefatos criados em `docs/baseline/`
- todos os artefatos criados em `docs/protocol/`

## Dependencias

- `000-03` a `000-13`

## Arquivos Ou Areas De Entrada

- Artefatos de baseline/protocolo
- Notas de lacunas registradas nas tasks anteriores

## Arquivos Exatos De Saida

- `docs/baseline/gaps-and-open-questions.md`
- Atualizacao do checklist em `docs/tasks/000-00-overview.md`

## Escopo

Para cada lacuna:

- assunto;
- impacto;
- evidencia encontrada;
- motivo da duvida;
- fase em que precisa ser validada;
- necessidade ou nao de dispositivo fisico;
- sugestao de validacao.

## Fora Do Escopo

- Resolver lacunas por decisao nova.
- Executar captura fisica.
- Implementar codigo.

## Regras De Seguranca

- `motionlog-app` e somente leitura.
- Roadmap e ADR nao podem ser alterados.
- Requisitos originais nao podem ser alterados.
- Nao deve haver commit ou push.
- Codigo Android ainda nao deve ser implementado.
- Inferencias nao podem ser apresentadas como fatos.
- Duvidas devem ser persistidas em documentos, nao apenas na resposta da sessao.

## Passos De Execucao

1. Ler todos os documentos produzidos.
2. Coletar lacunas internas.
3. Deduplicar sem remover informacao relevante.
4. Classificar por impacto e fase de validacao.
5. Marcar necessidade de dispositivo fisico.
6. Registrar sugestao de validacao.
7. Atualizar checklist do overview.

## Criterios De Aceite

- Lacunas centralizadas.
- Cada lacuna tem evidencia e impacto.
- Nenhuma decisao nova foi tomada.

## Validacao Final

- Confirmar que duvidas nao ficaram apenas na resposta da sessao.
- Confirmar que `motionlog-app` nao foi alterado.
