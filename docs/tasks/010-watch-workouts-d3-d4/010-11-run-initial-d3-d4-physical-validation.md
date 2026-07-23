# Task 010-11 - Executar validação física inicial D3/D4

## Status

Pendente.

## Objetivo

Executar gate mínimo de build e primeira validação física D3/D4 no Galaxy S22 com Champion C001 para descobrir divergências reais antes do Gate B final.

## Contexto

Layouts D3/D4 ainda dependem de validação real. Esta task não pode ser concluída com fakes, emulador ou leitura de código apenas.

## Documentos obrigatórios

- `AGENTS.md`
- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-10-integrate-progress-cancel-retry-lifecycle-and-safe-errors.md`
- `docs/protocol/workouts-d3-d4.md`

## Dependências

- `010-10`.

## Entradas

- build debug/release compilando;
- tela Exercícios funcional;
- Galaxy S22 e Champion C001 disponíveis;
- treino curto e treino longo ou multipart disponíveis no relógio.

## Arquivos exatos permitidos

- `docs/specs/010-watch-workouts-d3-d4.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-00-overview.md`
- `docs/tasks/010-watch-workouts-d3-d4/010-11-run-initial-d3-d4-physical-validation.md`
- `docs/protocol/workouts-d3-d4.md`

## Arquivos proibidos

- novos arquivos Kotlin, salvo correção pontual de regressão descoberta na validação;
- roadmap;
- ADRs.

## Escopo

- executar `lint`, `assembleDebug`, `assembleRelease` e `installDebug`;
- rodar primeira importação física D3/D4;
- registrar divergências reais de offsets, timeout, multipart, CRC, duração, intervalos e BPM;
- atualizar protocolo apenas com fatos observados.

## Fora do escopo

- fechar fase;
- aceitar Gate B final;
- marcar concluída sem hardware real.

## Passos

1. Executar gate obrigatório de build e instalação.
2. Rodar o roteiro físico inicial da spec.
3. Registrar divergências reais e regressões encontradas.
4. Atualizar `docs/protocol/workouts-d3-d4.md` apenas com evidência confirmada.
5. Manter task bloqueada se hardware ou treinos reais não existirem.

## Critérios de aceite

- gate mínimo executado;
- ausência de auto importação confirmada;
- D3 e D4 observados fisicamente;
- treino curto e treino longo ou multipart exercitados;
- divergências reais registradas;
- protocolo atualizado só com fatos confirmados.

## Testes pequenos aplicáveis

- não criar novos testes nesta task; task é de validação física.

## Validação mínima

- `./gradlew lint assembleDebug assembleRelease`
- `./gradlew installDebug`

## Evidências

- saídas dos comandos obrigatórios;
- observações do smoke físico;
- screenshots e achados do protocolo, quando aplicável.

## Atualização do overview

- marcar `010-11` como concluída ou bloqueada;
- registrar achados físicos e dependência de correção para `010-12`.

## Validação final

- confirmar que task não foi concluída por fakes, emulador ou testes sintéticos.
