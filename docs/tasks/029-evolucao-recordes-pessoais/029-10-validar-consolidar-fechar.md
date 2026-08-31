# 029-10 — Validar, consolidar e fechar

- **Ticker:** `029`
- **Número:** `10`
- **Status:** `completed`
- **Depends on:** `029-09`

## Objetivo

Consolidar a Spec 029 após implementação completa, revisar fórmulas/eligibilidade, validar as superfícies e registrar limitações reais.

## Escopo

- revisar contratos de evolução e recordes;
- confirmar que não houve materialização Room desnecessária;
- confirmar que nenhuma feature importa Vico diretamente;
- revisar filtros 7/30/90 e comparação de períodos;
- revisar recordes atuais e idempotência pós-save;
- verificar comportamento de atividade antiga, mudança de tipo, calibração de esteira e distância manual de bicicleta quando houver fixtures reais;
- remover helpers/estado obsoleto introduzidos durante rollout, somente quando comprovadamente sem consumidor;
- atualizar spec, overview e documentação factual final.

## Pontos funcionais revisados

Confirmar:

- volume e dias ativos;
- tendências implementadas;
- comparação período atual × anterior;
- filtros por esporte;
- recordes por modalidade;
- maior distância com valor final persistido;
- maior duração;
- passos somente em modalidade elegível;
- feedback pós-save não bloqueante;
- múltiplos recordes agrupados;
- re-save sem repetição;
- navegação para Evolução/atividade quando disponível;
- tema claro/escuro, fonte ampliada e rolagem.

## Critérios de aceite

1. Spec e overview refletem o estado final real.
2. Fórmulas implementadas estão documentadas e reconciliam com os fatos locais.
3. Nenhum recorde usa estado transitório de formulário.
4. Save funcional não depende do sucesso do evaluator.
5. Recordistas atuais continuam deriváveis após descarte/alteração elegível.
6. Nenhum score fisiológico ou métrica fora do escopo foi introduzido.
7. Nenhum fluxo phone/GPS/Health Connect read foi reaberto.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
```

Não criar nem executar testes automatizados sem solicitação explícita.

## Validação física

Executar smoke sanitizado da tela Evolução e do fluxo pós-save. Registrar somente os cenários efetivamente exercitados; fixture indisponível permanece `N/A`, nunca sucesso presumido.

## Registro de execução

### Arquivos alterados

- `app/src/main/java/br/com/mwconsulting/championcompanion/data/LocalEvolutionReadModel.kt`;
- `docs/specs/029-evolucao-recordes-pessoais.md`;
- `docs/tasks/029-evolucao-recordes-pessoais/029-00-overview.md`;
- `docs/tasks/029-evolucao-recordes-pessoais/029-09-integrar-recorde-pos-save.md`;
- este arquivo.

### Decisões e desvios

- O filtro `CYCLING` revelou que a projeção calculava fatos de cadência D4 e os
  enviava ao contrato de bicicleta, que corretamente rejeita passos/cadência
  nessa modalidade. O read model agora omite esses fatos antes de montar o
  snapshot. Nenhuma outra regra foi refatorada.
- Não foram removidos helpers ou estado: inspeção não encontrou consumidor
  comprovadamente obsoleto.
- Nenhuma entidade, DAO, migration, schema, dependência, teste automatizado ou
  superfície fora do escopo foi alterada.
- Após feedback físico de usabilidade, o banner de conquista passou a rolar
  automaticamente para o topo quando emitido, preservando visibilidade após
  save/calibração sem mudar contrato de persistência.

### Comandos executados e resultados

- `git diff --check` — passou antes e depois da correção;
- `./gradlew lint assembleDebug assembleRelease` — `BUILD SUCCESSFUL` após a
  correção;
- `./gradlew installDebug` — `BUILD SUCCESSFUL`, instalado em `SM-S901E`;
- `adb devices` — dispositivo `SM-S901E` disponível;
- `adb shell uiautomator dump` — smoke sanitizado executado sem crash observado;
- testes automatizados — não criados nem executados, conforme spec.

### Evidências físicas

- Exercícios → Evolução abriu no aparelho debug.
- Períodos 7, 30 e 90 dias renderizaram; 7 dias mostrou 3 atividades e 8 km,
  30 dias mostrou resumo consolidado e 90 dias mostrou séries com lacunas.
- Filtro Corrida renderizou resumo; filtro Bicicleta renderizou 2 atividades,
  3,5 km, 40 min 17 s e `Não aplicável` para passos, sem crash após a correção.
- Rolagem expôs tendências, lacunas, ausência e inaplicabilidade sem fabricar
  zero.
- Uma atividade de corrida aguardando revisão foi aberta e salva; estado mudou
  para `Pronta para sincronizar`, sem bloqueio de persistência.
- Uma segunda atividade foi revisada como corrida, calibrada com distância
  externa de `20,00 km` e produziu recorde de distância com diferença de
  `10,00 km`; banner apareceu automaticamente no topo, sem swipe manual.
- Re-save idempotente, múltiplos recordes, mudança de tipo, calibração de
  esteira, distância manual de bicicleta no pós-save, abertura de atividade
  recordista, tema claro e fonte ampliada: `N/A` nesta sessão.

### Riscos residuais

- Histórico all-history continua falhando fechado quando fatos locais inválidos
  impedem mapeamento integral; comportamento é deliberado e já documentado em
  `029-07`.
- Cenários de conquista e idempotência dependem de fixtures controladas que não
  foram preparados no aparelho; conquista de distância foi exercitada, mas
  re-save idempotente não foi marcado como sucesso presumido.
- Validação visual clara/fonte ampliada e tooltip permanece pendente de sessão
  física específica.

## Fechamento

Ao concluir:

- marcar `docs/specs/029-evolucao-recordes-pessoais.md` como `completed`;
- atualizar `029-00-overview.md` para `10/10`;
- registrar evidências e riscos residuais neste arquivo e nas tasks correspondentes.
