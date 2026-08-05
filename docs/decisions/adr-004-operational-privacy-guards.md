# ADR-004 — Restringir logs diagnósticos e bloquear backup automático

## Status

Aceita, com supersessão parcial pela ADR-014

## Contexto

Aplicativo lida com dados de saúde, dispositivo e diagnósticos. Vazamento por
logs, backup automático ou sincronização de device transfer aumenta risco
desnecessário.

Implementação atual já impõe guard rails:

- `StructuredLogger` emite somente em build debug;
- schema atual de log contém apenas `event` e `error_code`;
- `AndroidManifest.xml` define `android:allowBackup="false"`;
- `backup_rules.xml` e `data_extraction_rules.xml` excluem root, files,
  database, shared preferences e external;
- `Phase003PrivacyGuardTest` bloqueia termos de localização/rota em domínio,
  banco, dados, logging e testes. A incompatibilidade desse guard com a
  política posterior da Fase 13 é dívida conhecida e não autoriza contorno.

## Decisão

Adotar política operacional de privacidade por padrão:

- logs diagnósticos estruturados existem apenas em builds debug;
- release não deve emitir logs diagnósticos com detalhes operacionais;
- payload BLE, dados de saúde, tokens, credenciais, identificadores pessoais,
  coordenadas, rotas e equivalentes não podem aparecer em logs diagnósticos;
- backup automático Android e device transfer permanecerão desabilitados para
  dados do app;
- logs, diagnósticos, exportações e testes continuarão sem coordenadas, altitude,
  rota ou amostras de localização;
- Fase 12, D3/D4 e fluxos fora da Fase 13 continuarão sem rota;
- a ADR-014 substitui somente a proibição absoluta de persistência para a
  sessão explícita de caminhada/corrida da Fase 13, com GPS preciso, retenção
  local controlada, sem rastreamento passivo, servidor, exportação, backup ou
  device transfer.

## Consequências

### Positivas

- reduz chance de exposição acidental de dados sensíveis;
- cria comportamento consistente entre manifesto, regras XML e testes;
- simplifica revisão de conformidade de privacidade nesta fase.

### Negativas

- diagnóstico em release fica mais limitado;
- futuras features com localização/rotas continuam exigindo decisão explícita;
  a exceção atual é somente a Fase 13 conforme ADR-014;
- suporte de campo depende mais de reprodução em debug e testes.

## Alternativas consideradas

- permitir logs diagnósticos em release com redaction parcial;
- manter backup Android habilitado e filtrar apenas parte dos dados;
- aceitar coordenadas/rotas no modelo local sem escopo de sessão, retenção,
  deleção e sincronização definidos.

## Relação com ADR-014

A ADR-014 é a decisão específica para rota de treino. Ela não libera
coordenadas em logs, diagnósticos, testes, backup, device transfer ou qualquer
fluxo fora de caminhada/corrida iniciada explicitamente pelo usuário. Backup e
device transfer continuam negados no manifesto e nas regras Android.
