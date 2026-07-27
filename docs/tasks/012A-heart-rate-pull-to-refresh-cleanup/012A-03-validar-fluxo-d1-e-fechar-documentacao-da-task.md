# Task 012A-03 - Validar fluxo D1 e fechar documentação da task

## Status

pending

## Objetivo

Executar validação final da mudança, registrar evidências reais e alinhar a documentação factual da task 012A, incluindo baseline.

## Resultado esperado

- validação técnica registrada;
- baseline factual ajustado para não mencionar o botão removido;
- overview atualizado com progresso final coerente;
- task pronta para handoff sem lacunas documentais.

## Requisitos cobertos

- registrar comandos executados e seus resultados;
- registrar validação manual do fluxo D1 por pull-to-refresh;
- alinhar spec, overview, subtarefas e baseline factual.

## Escopo incluído

- `docs/tasks/012A-heart-rate-pull-to-refresh-cleanup/012A-00-overview.md`
- `docs/tasks/012A-heart-rate-pull-to-refresh-cleanup/012A-01-integrar-pull-to-refresh-d1.md`
- `docs/tasks/012A-heart-rate-pull-to-refresh-cleanup/012A-02-simplificar-resumo-e-realocar-feedback-d1.md`
- `docs/tasks/012A-heart-rate-pull-to-refresh-cleanup/012A-03-validar-fluxo-d1-e-fechar-documentacao-da-task.md`
- `docs/specs/012A-heart-rate-pull-to-refresh-cleanup.md`
- `docs/baseline/current-baseline.md`

## Escopo excluído

- novas mudanças funcionais fora do necessário para corrigir falha encontrada na validação;
- roadmap e ADR.

## Dependências

- `012A-02`.

## Arquivos e símbolos prováveis

- `docs/tasks/012a-heart-rate-pull-to-refresh-cleanup/012A-00-overview.md`
- `docs/specs/012A-heart-rate-pull-to-refresh-cleanup.md`
- `docs/baseline/current-baseline.md`

## Passos de implementação

1. Executar a validação esperada para mudança de código.
2. Registrar resultados reais dos comandos, inclusive falhas se existirem.
3. Registrar validação manual dos estados:
   - overview sem CTAs fixos;
   - card `Resumo atual`;
   - pull-to-refresh iniciando D1;
   - cancelamento, retry e dismiss preservados;
   - medição manual ainda acessível;
   - ausência de auto-BLE em carregamento passivo;
   - acessibilidade do refresh equivalente ao gesto.
4. Atualizar `docs/baseline/current-baseline.md` para refletir a UX factual entregue.
5. Atualizar status da `012A-01`, `012A-02`, `012A-03` e do `00-overview.md`.
6. Consolidar riscos residuais, screenshot futura de PR e necessidade de follow-up, se houver.

## Testes e comandos de validação

```bash
./gradlew lint assembleDebug assembleRelease
```

Comando adicional esperado para gate físico, se o implementador executar validação no dispositivo:

```bash
./gradlew installDebug
```

## Definição de pronto

- resultados de validação documentados sem omissão;
- baseline factual alinhado à UX entregue;
- overview mostra progresso final coerente com as subtarefas;
- spec, overview e subtarefas permanecem alinhados entre si.

## Riscos e cuidados

- não marcar concluído sem evidência real;
- não mascarar regressão de gesto, cancelamento ou refresh passivo;
- não deixar o baseline desatualizado após a mudança funcional.
