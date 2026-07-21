# ADR-004 — Restringir logs diagnósticos e bloquear backup automático

## Status

Aceita

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
  banco, dados, logging e testes.

## Decisão

Adotar política operacional de privacidade por padrão:

- logs diagnósticos estruturados existem apenas em builds debug;
- release não deve emitir logs diagnósticos com detalhes operacionais;
- payload BLE, dados de saúde, tokens, credenciais, identificadores pessoais,
  coordenadas, rotas e equivalentes não podem aparecer em logs diagnósticos;
- backup automático Android e device transfer permanecerão desabilitados para
  dados do app;
- escopos de domínio, persistência, logging e testes continuarão sem dados de
  localização/rota até decisão futura explícita.

## Consequências

### Positivas

- reduz chance de exposição acidental de dados sensíveis;
- cria comportamento consistente entre manifesto, regras XML e testes;
- simplifica revisão de conformidade de privacidade nesta fase.

### Negativas

- diagnóstico em release fica mais limitado;
- futuras features com localização/rotas exigirão decisão explícita e ajuste dos guard rails;
- suporte de campo depende mais de reprodução em debug e testes.

## Alternativas consideradas

- permitir logs diagnósticos em release com redaction parcial;
- manter backup Android habilitado e filtrar apenas parte dos dados;
- aceitar coordenadas/rotas no modelo local antes de decisão específica.
