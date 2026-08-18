# 024-04 — Definir estratégia cromática transversal

- **Ticker:** `024`
- **Número:** `04`
- **Status:** completed

## Objetivo e resultado esperado

Definir como aumentar variedade cromática no app inteiro, usando Google
Health/Fitbit somente como referência de hierarquia e uso de cor, sem copiar
marca, paleta ou layout. Resultado: estratégia aprovada para adoção opt-in,
tela por tela, antes da retomada do piloto Saúde.

## Requisitos e critérios cobertos

- R1, R2, R4, R5, R7, R8 e R9.
- Critérios 1, 3, 5, 7, 8, 11 e 12 da spec.

## Escopo incluído

- inventário de roles Material 3 e tokens semânticos disponíveis;
- definição de superfícies, containers e acentos por intenção visual;
- regras para estados, origem, suporte, erro e sincronização sem depender só de
  cor;
- preservação de dynamic color e fallback azul-petróleo;
- ordem de adoção por superfície e gate de revisão visual.

## Escopo excluído

- alteração global imediata de `ChampionColorScheme` ou `ChampionTheme`;
- redesign de qualquer tela;
- nova dependência, fonte, ativo ou paleta Google/Fitbit;
- alteração de contratos funcionais, navegação, dados, BLE ou Health Connect.

## Dependências

- `024-03` concluída;
- Spec 024 e ADR-023 revisadas;
- aceite explícito da estratégia antes de retomar `024-05`.

## Arquivos e símbolos prováveis

- `docs/specs/024-refatoracao-visual-google-health.md`;
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`;
- possível matriz documental de roles e tokens, sem código de produção.

## Passos de implementação

1. Mapear cores e superfícies existentes sem alterar consumidores.
2. Separar direção cromática global de mutação global de tema.
3. Definir uso opt-in de `primary`, `secondary`, `tertiary` e containers por
   intenção, preservando identidade Champion.
4. Definir sinais textuais e icônicos complementares à cor.
5. Atualizar spec, ADR e ordem do overview.
6. Registrar decisão, riscos, dependências e gate para `024-05`.

## Testes e comandos de validação

Não executar testes automatizados nem alterar código de produção.

```bash
git diff --check
```

Validação: ticker, links, checklist único, rastreabilidade e consistência entre
spec, ADR, overview e subtarefas.

## Definição de pronto

- estratégia cromática transversal documentada;
- adoção opt-in por superfície preservada;
- dynamic color, fallback e identidade Champion preservados;
- cópia de marca e paleta Google/Fitbit explicitamente proibidas;
- `024-05` desbloqueada somente após decisão e aceite registrados.

## Estratégia aprovada

| Intenção | Role/container preferencial | Regra de uso |
|---|---|---|
| ação primária e seleção ativa | `primary`, `onPrimary`, `primaryContainer`, `onPrimaryContainer` | reservar para ação principal, seleção e foco; não pintar toda superfície |
| apoio, progresso e agrupamento | `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer` | destacar suporte e contexto sem competir com valor principal |
| acento de métrica ou seção | `tertiary`, `onTertiary`, `tertiaryContainer`, `onTertiaryContainer` | usar por categoria visual quando houver ganho de leitura; nunca como sinal único |
| erro, falha ou estado destrutivo | `error`, `onError`, `errorContainer`, `onErrorContainer` | acompanhar label, ícone e ação textual; não usar para ausência comum |
| base e hierarquia de superfície | `background`, `surface`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer` | criar ritmo tonal entre página, resumo, card e suporte, sem mudar tema global |

Regras transversais:

- valor principal permanece com contraste e tipografia prioritários;
- suporte, origem, unidade e período usam `onSurfaceVariant` ou equivalente
  legível, sem competir com valor;
- estados `Disponível`, `Sem dados`, `Pendente`, `Falha` e `Sincronizado`
  continuam textuais e, quando aplicável, acompanhados por ícone;
- origem nunca é comunicada somente por cor;
- cada tela escolhe apenas roles necessários e mantém componentes antigos fora do
  ramo migrado;
- dynamic color permanece padrão; fallback azul-petróleo permanece identidade
  própria em aparelhos sem dynamic color;
- nenhuma combinação reproduz as quatro cores ou mapeamentos proprietários do
  Google/Fitbit.

## Riscos e cuidados

- alterar tema raiz redesenha superfícies não migradas implicitamente;
- dynamic color pode ocultar mudanças no fallback em aparelhos compatíveis;
- excesso de cores pode competir com valor principal;
- inconsistência temporária entre telas migradas e antigas permanece aceitável.

## Execução registrada

### Status

`completed`; estratégia aprovada pelo usuário para adoção opt-in por superfície.

### Arquivos alterados

- `docs/specs/024-refatoracao-visual-google-health.md`;
- `docs/decisions/adr-023-refatoracao-visual-incremental-inspirada-no-google-health.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-00-overview.md`;
- `docs/tasks/024-refatoracao-visual-google-health/024-04-definir-estrategia-cromatica-transversal.md`;
- subtarefas `024-05` a `024-18`, renumeradas para inserir este gate documental.

### Decisões e desvios

- Direção cromática passa a abranger app inteiro como resultado final, sem
  mutação global imediata de `ChampionTheme` ou `ChampionColorScheme`.
- Adoção será opt-in por superfície, com roles `primary`, `secondary`,
  `tertiary` e containers Material 3 conforme intenção visual.
- Dynamic color, fallback azul-petróleo, contraste e sinais textuais/icônicos
  foram preservados.
- Google Health/Fitbit continua referência estrutural; não serão copiados marca,
  paleta, fonte, ativos ou layout.
- Planejamento foi normalizado para manter formato `<ticker>-NN-<subtask>.md`:
  este gate tornou-se `024-04`, piloto Saúde tornou-se `024-05` e consolidação
  tornou-se `024-18`.

### Comandos executados e resultados

| Comando/verificação | Resultado |
|---|---|
| `git diff --check` | Passou; sem erro |
| Auditoria de ticker, números, links e checklist | Passou; 18 subtarefas, uma seção `## Checklist`, progresso `4/18` |

Não foram criados, alterados ou executados testes automatizados nem código de
produção nesta subtarefa.

### Evidências

- Aprovação explícita do usuário registrada na conversa para estratégia opt-in
  por tela.
- Spec, ADR, overview e subtarefas renumeradas refletem nova ordem e gate.

### Riscos residuais

- `024-05` permanece bloqueada até aplicar estratégia no piloto e obter aceite
  visual da superfície.
- Dynamic color pode exigir validação física em cada superfície para confirmar
  contraste e variedade cromática observada.

## Revisão independente

`PASS`; revisão confirmou matriz cromática materializada, dependência direta de
`024-04` em `024-05`, renumeração sem lacunas, checklist único e progresso `4/18`.
