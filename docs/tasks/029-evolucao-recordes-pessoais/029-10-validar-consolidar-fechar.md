# 029-10 — Validar, consolidar e fechar

- **Ticker:** `029`
- **Número:** `10`
- **Status:** `pending`
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

## Checklist funcional

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

## Fechamento

Ao concluir:

- marcar `docs/specs/029-evolucao-recordes-pessoais.md` como `completed`;
- atualizar `029-00-overview.md` para `10/10`;
- registrar evidências e riscos residuais neste arquivo e nas tasks correspondentes.
