# 033-02 — Implementar parser e collector D1 v2

- **Status:** pending
- **Depende de:** 033-01
- **Pode rodar em paralelo com:** 033-03

## Objetivo

Implementar o contrato D1 v2 sem persistência nem UI.

## Escopo

- parser dos campos completos e endianness congelada;
- dayOffset/hour packed;
- BPM nullable com faixa 30..200;
- pacote estrutural válido independente de BPM;
- collector por posição física e total;
- conclusão mesmo quando o último pacote não possui BPM válido;
- progresso, gaps, duplicatas, fora de ordem e timeout parcial;
- modelos internos semanticamente nomeados, sem inventar unidades raw desconhecidas.

## Fora do escopo

- Room;
- Health Connect;
- composição D1/D4;
- UI.

## Validação

```bash
./gradlew lint assembleDebug assembleRelease
git diff --check
```

Não criar nem executar testes automatizados sem autorização explícita.
