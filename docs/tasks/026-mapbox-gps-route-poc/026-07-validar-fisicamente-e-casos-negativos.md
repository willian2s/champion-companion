# 026-07 — Validar fisicamente e executar casos negativos

- **Ticker:** `026`
- **Número:** `07`
- **Status:** pending

## Objetivo

Executar o gate físico da PoC em aparelho Android real, cobrindo o cenário principal e casos negativos sem expor localização pessoal nas evidências versionadas.

## Resultado esperado

Há evidência sanitizada suficiente para afirmar se mapa, GPS, filtragem, rota crescente, interação da câmera, stop e reset funcionam em uso real.

## Dependências

- `026-06` concluída;
- build debug instalável;
- credenciais Mapbox locais válidas;
- aparelho real disponível;
- ambiente externo adequado para obter GPS.

## Pré-condições

```text
aparelho Android real conectado por ADB
localização do sistema habilitada
build debug instalado
acesso à internet para mapa-base Mapbox
permissão precisa disponível para concessão
```

## Cenário principal obrigatório

1. abrir Activity da PoC;
2. conceder localização precisa;
3. aguardar readiness `READY`;
4. iniciar captura;
5. percorrer trajeto externo real de aproximadamente 500 m ou mais;
6. confirmar progressão da posição;
7. confirmar crescimento da rota;
8. confirmar atualização das métricas técnicas;
9. executar pan/zoom manual;
10. confirmar suspensão do follow;
11. usar `Recentralizar`;
12. continuar percurso;
13. parar captura;
14. confirmar que novos fixes não alteram a rota;
15. confirmar enquadramento da rota final;
16. limpar;
17. iniciar segundo ciclo sem reiniciar o aplicativo.

## Casos negativos obrigatórios

- permissão negada;
- localização aproximada;
- GPS desligado;
- GPS procurando fix;
- qualidade que cai para fraca/perdida durante tracking;
- parar antes do segundo ponto;
- limpar após parada;
- dois ciclos consecutivos;
- ausência/erro de configuração de token em ambiente controlado, quando viável sem comprometer credenciais.

## Observações de qualidade

Registrar de forma categórica, sem coordenadas:

- estabilidade do mapa;
- percepção de fluidez da linha;
- ocorrência de saltos visuais;
- quantidade agregada de pontos aceitos/rejeitados quando útil;
- qualidade GPS observada por categoria;
- comportamento da câmera;
- comportamento após stop/reset;
- crashes/ANRs.

## Evidência e privacidade

Proibido versionar:

- screenshot de rota real reconhecível;
- localização residencial;
- coordenadas;
- GeoJSON;
- bounds;
- gravação de tela com trajeto pessoal;
- tokens.

Screenshot exigido por mudança visual deve usar estado neutro/sanitizado sem localização pessoal visível.

A validação real pode ser registrada textualmente com resultado categórico e sem dados geográficos.

## Validação técnica

```bash
git diff --check
./gradlew lint assembleDebug assembleRelease
./gradlew installDebug
```

Também inspecionar Logcat durante o ciclo para confirmar ausência de coordenadas e tokens.

## Critérios do gate

Gate pode seguir para fechamento somente se:

- mapa carrega de forma estável;
- posição e rota usam mesma fonte;
- linha cresce em deslocamento real;
- somente pontos aceitos entram na rota;
- pan/zoom e recentralização funcionam;
- stop congela rota;
- fit final funciona;
- reset isola ciclo seguinte;
- não há crash ou ANR bloqueador;
- não há localização ou token no Logcat;
- release continua sem entrypoint da PoC.

## Definição de pronto

- cenário principal executado integralmente ou bloqueio real documentado;
- casos negativos executados ou marcados `N/A` com justificativa factual;
- evidência sanitizada registrada;
- nenhuma ausência foi convertida em sucesso;
- achados e ajustes necessários foram documentados para `026-08`.
