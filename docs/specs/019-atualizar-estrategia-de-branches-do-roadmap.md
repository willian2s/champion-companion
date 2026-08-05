# 019 — Atualizar estratégia de branches do roadmap

- **Ticker:** `019`
- **Status:** concluída
- **Contexto:** roadmap contém nomes históricos e branches que não existem no
  repositório atual.

## Objetivo

Alinhar seção de branches do roadmap com branches locais/remotas verificadas,
separando baseline externa, branches existentes e sugestões futuras. Remover
sugestões que representam fases já concluídas ou conflitam com regras atuais de
privacidade e backup.

## Requisitos

1. Manter `main` como branch principal do repositório Champion Companion.
2. Registrar nomes reais de branches locais/remotas relevantes, incluindo a
   exceção histórica `009-watch-heart-rate-history-d1` e
   `fix/015-11-sync-integrity`.
3. Não apresentar como existente branch que não está em `git branch --all`.
4. Separar branches existentes de padrões de nomenclatura sugeridos para
   trabalho futuro.
5. Representar Specs 015, 015A, 016, 017 e 018 sem inventar branches dedicadas;
   mudanças dessas tasks estão na linha de integração atual quando não há branch
   publicada específica.
6. Manter referências `feat/015-watch-workouts-d3-d4` somente quando indicarem
   baseline externa do MotionLog, não como branch deste repositório.
7. Não sugerir branch para backup Android, device transfer ou exportação de
   rota, proibidos pelas regras atuais do projeto.
8. Preservar português brasileiro, estrutura do roadmap e demais fases.

## Critérios de aceite

- Seção de estratégia identifica branch principal e integração atual.
- Lista de branches existentes corresponde à saída verificada de Git.
- Padrões sugeridos não são apresentados como branches existentes nem reservam
  tickers de Specs.
- Nenhuma ocorrência da baseline externa é alterada indevidamente.
- `git diff --check` passa.
- Spec, overview e subtarefa usam ticker `019`; overview possui um único
  checklist com um item.

## Escopo

Incluído: `docs/roadmap/champion-companion-roadmap.md`, esta spec, task 019 e
overview, além de baseline se uma referência factual precisar de alinhamento.

Excluído: criação/renomeação de branches Git, código Android, commits, push,
alteração de fases ou mudança da política de backup.

## Abordagem

- usar `git branch --all --no-color` como inventário factual;
- manter baseline MotionLog explicitamente separada;
- reorganizar somente seção de estratégia de branches;
- registrar decisão documental, sem ADR arquitetural.

## Validação

```bash
git branch --all --no-color
git diff --check
```

Não há mudança de código; não executar build, lint ou testes automatizados.
