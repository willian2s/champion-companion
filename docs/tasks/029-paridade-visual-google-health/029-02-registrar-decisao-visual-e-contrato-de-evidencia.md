# 029-02 — Registrar decisão visual e contrato de evidência

- **Ticker:** `029`
- **Número:** `02`
- **Status:** `completed`
- **Depende de:** `029-01`

## Objetivo e resultado esperado

Transformar o baseline em contrato executável para implementação: registrar
referência instalada normativa, equivalência visual esperada, limites de cópia,
política de screenshots e critérios de `N/A`. Resultado: decisão arquitetural e
matriz de aceite prontos antes de qualquer código de produção.

## Requisitos e critérios de aceite cobertos

- R1, R2, R3 e R4 da Spec 029.
- Critérios 1, 2, 3, 4 e 10.
- Formalizar que sites não participam do aceite visual.
- Formalizar que a expressão pixel a pixel é objetivo de fidelidade, com
  preservação de identidade Champion e evidência sanitizada.

## Escopo incluído

- registrar ADR-028 em `docs/decisions/`;
- revisar matriz produzida em 029-01;
- definir nomenclatura, redaction, retenção e critérios de comparação;
- mapear divergências por superfície e ordem de rollout;
- registrar dependência da Spec 028 para gráficos.

## Escopo excluído

- código de UI, tema ou domínio;
- cópia de marca, fonte, assets, textos proprietários ou screenshots brutos;
- mudança de rotas, Room, DataStore, Health Connect, BLE ou backup;
- novo contrato de dados sem decisão separada.

## Dependências

- baseline e matriz de 029-01;
- ADR-023 e ADR-027;
- Spec 024 e Spec 028;
- decisão arquitetural registrada antes do código.

## Arquivos e símbolos prováveis

- `docs/decisions/adr-028-paridade-visual-com-referencia-instalada.md`;
- `docs/specs/029-paridade-visual-google-health.md`;
- `docs/tasks/029-paridade-visual-google-health/evidencia-baseline-visual-029-01.md`;
- `ChampionNavigation`, `ChampionTimeSeriesChart`,
  `HeartRateHistorySnapshot` e `WorkoutActivityFilter` como contratos de
  referência.

## Passos de implementação

1. Conferir fatos de 029-01 contra versões e decisões existentes.
2. Registrar referência instalada como fonte visual normativa única.
3. Registrar equivalência de hierarquia, ordem, densidade, comportamento,
   acessibilidade e estados como critério verificável.
4. Registrar limites de identidade, privacidade e evidência sanitizada.
5. Registrar preservação de navegação, domínio, persistência e gráficos.
6. Publicar matriz de aceite por superfície e dependências.

## Testes e comandos de validação

```bash
git diff --check
```

Não executar testes automatizados nem Gradle: subtarefa documental.

## Definição de pronto

- ADR-028 existe e referencia ADR-023, ADR-027, Spec 024 e Spec 028;
- direção visual instalada, limites e critérios de aceite estão explícitos;
- matriz não promete estados não alcançados;
- contrato de sanitização impede screenshot bruto e dado pessoal;
- nenhuma decisão contradiz navegação, privacidade ou contratos existentes sem
  decisão adicional.

## Riscos e cuidados

- Não reescrever histórico da Spec 024; registrar evolução como decisão nova.
- Não aceitar literalidade que implique copiar ativos, dados ou marca.
- Se baseline revelar necessidade de novo domínio ou persistência, bloquear a
  implementação e abrir spec própria.

## Execução da subtarefa

- **Status:** `completed` em 2026-09-01.
- **Arquivos alterados:**
  `docs/decisions/adr-028-paridade-visual-com-referencia-instalada.md`;
  este arquivo; `docs/tasks/029-paridade-visual-google-health/029-00-overview.md`.
- **Decisões e desvios:** ADR-028 tornou `com.fitbit.FitbitMobile` a única
  referência visual normativa; sites, Google Fit e Health Connect foram
  excluídos do aceite. “Pixel a pixel” ficou registrado como objetivo de
  fidelidade, preservando identidade Champion. Matriz cobre superfícies,
  critérios, estados alcançados, `N/A`, sanitização, retenção e rollout. A
  ausência observada de Semana/Mês no Champion e a dependência `028-10`
  permanecem explícitas; não houve código, contrato, domínio ou persistência
  alterados.
- **Comandos executados:** `git diff --check`.
- **Resultados e evidências:** comando passou sem saída de erro. ADR-028
  referencia ADR-023, ADR-027, Spec 024, Spec 028 e Spec 029; usa a matriz de
  `evidencia-baseline-visual-029-01.md` como entrada factual. Revisão
  independente aprovou ADR-028, overview e esta subtarefa, sem achados.
- **Riscos residuais:** aceite físico de tema claro, fonte ampliada, landscape,
  acessibilidade, marker, tooltip e rolagem horizontal segue pendente. Estados
  marcados `N/A` não são aceitos por presunção. Variação de conta, versão e
  região continua limitada ao baseline observado.
