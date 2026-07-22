# ADR-007 — Permitir auto-conexão controlada do dispositivo associado na Fase 06

## Status

Aceita

## Contexto

Roadmap inicial e spec da Fase 06 proibiam auto-connect ao abrir app ou tela. Na prática, comportamento do MotionLog já reconecta dispositivo conhecido ao abrir app e isto reduz atrito para uso diário. Ao mesmo tempo, Champion Companion ainda não terá foreground service, WorkManager BLE, D1 automático nem background permanente nesta fase.

## Decisão

Fase 06 passa a permitir auto-conexão limitada do dispositivo associado primário.

Regras obrigatórias:

- tentativa automática só vale para dispositivo já associado;
- tentativa automática só ocorre ao abrir app ou entrar na tela de dispositivo com feature ativa;
- tentativa automática exige permissões e Bluetooth já disponíveis;
- tentativa automática não abre prompt automático de permissão runtime;
- tentativa automática é finita e única por entrada visível da feature;
- falha encerra em estado seguro com retry manual;
- app não pode conectar outro Champion automaticamente;
- app não pode entrar em reconexão infinita nem manter BLE permanente.

## Consequências

### Positivas

- reduz ação manual repetitiva para dispositivo já conhecido;
- aproxima experiência do MotionLog sem exigir background BLE completo;
- reaproveita pipeline de conexão lembrada e handshake já previsto na Fase 06.

### Negativas

- lifecycle da feature fica mais sensível a loops de recomposição ou retorno de tela;
- exige cuidado extra para não violar regra de permissão somente por ação do usuário;
- aumenta necessidade de validação manual para cenários de falha curta, Bluetooth desligado e retorno ao app.

## Alternativas consideradas

- manter conexão sempre manual até Fase 14;
- empurrar auto-connect para foreground service/background completo;
- repetir scan amplo ao abrir app até encontrar qualquer Champion.

Alternativas foram rejeitadas por pior UX imediata, excesso de atraso para paridade com MotionLog, ou comportamento agressivo incompatível com limites de privacidade, bateria e previsibilidade do projeto.
