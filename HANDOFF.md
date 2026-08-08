# Handoff — PDV Maquineta (Android)

Guia rápido para quem está entrando no projeto agora.

## O que é

App de Ponto de Venda (PDV) Android para maquinetas: vendas, pagamentos,
caixa, produtos, clientes, comprovantes, relatórios e integrações (ERP e
adquirente). Kotlin + Jetpack Compose, Clean Architecture (data/domain/
presentation) + MVVM, Room, Hilt. Veja `CLAUDE.md` na raiz para convenções e
stack completos antes de mexer em qualquer coisa.

## Contexto do sistema maior

Este repositório é só a **parte Android** de um sistema com três componentes,
em pastas/repositórios separados:

1. **PDVMaquineta** (este repo) — o app da maquineta em si.
2. **PDVMaquinetaBackend** — backend separado (pasta irmã `../PDVMaquinetaBackend`),
   já com seu próprio repositório Git. É quem o app vai sincronizar dados
   quando a Fase 8 (abaixo) avançar.
3. **Painel web** — ainda não iniciado. Vai consumir o mesmo backend.

Não assuma que mudanças de schema/contrato aqui se propagam automaticamente
para o backend — hoje são times/repos desacoplados.

## Onde encontrar mais contexto

- **`CLAUDE.md`** (raiz) — convenções do projeto: perfis de usuário, stack,
  regras de segurança/auditoria, o que não fazer (ex.: não implementar SDK de
  pagamento real sem confirmação da adquirente, não misturar lógica de
  permissão espalhada pela UI).
- **`ROADMAP.md`** (raiz) — histórico completo de fases, o que foi decidido e
  por quê, incluindo pendências conscientes (não são esquecimento). É a fonte
  de verdade sobre o que já foi construído e validado em dispositivo físico.

## Migrações do Room: sempre testar em dispositivo físico

O banco (`PdvDatabase`) já passou por 10 migrações reais (`MIGRATION_1_2` até
`MIGRATION_9_10`, em `data/local/database/migration/DatabaseMigrations.kt`).
**Nunca usar `fallbackToDestructiveMigration`** — cada migração é escrita à
mão e testada com um teste instrumentado dedicado (`Migration<N>To<N+1>Test`
em `androidTest`) que recria o schema anterior via SQL puro, insere dados, e
aplica a migração real usada em produção.

Por quê isso importa tanto aqui: é um app de PDV que fica **offline boa parte
do tempo** (princípio central do projeto — toda escrita crítica de venda/
pagamento/caixa é local primeiro). Se uma migração falhar ou apagar dados,
isso significa perder vendas, sessões de caixa ou histórico de auditoria de
um comércio real, não um dado descartável. Testes de migração em JVM/Robolectric
não pegam certas incompatibilidades de SQLite real do dispositivo — por isso
o padrão do projeto é sempre rodar via `connectedDebugAndroidTest` num
aparelho físico (ou emulador com Play Store/SQLite real) antes de considerar
a migração validada, nunca só compilar.

## Estado atual (resumo — ver `ROADMAP.md` para detalhes)

- **Fases 0–5 e 7a/7b/7c**: concluídas e validadas em dispositivo físico
  (fundação, auth/perfis, caixa, produtos/carrinho, admin de usuários,
  clientes/fidelidade, estoque, relatórios, exportação CSV/PDF/XLSX).
- **Fase 6 (comprovantes) — parcial**: a parte não-fiscal está concluída e
  validada (comprovante não-fiscal, config de terminal/loja). A parte fiscal
  (RF-033/RF-034: NFC-e/SAT/MFE) está **pendente aguardando definição do
  modelo de emissão fiscal e do provedor junto ao diretor/cliente final** —
  é uma pendência consciente registrada no `ROADMAP.md`, não um esquecimento.
  Não implementar emissão fiscal real sem essa definição.
- **Fase 7d (integração com ERP mockada)**: pendente — é o que falta pra
  fechar a Fase 7 por completo.
- **Em andamento agora**: leitura de código de barras (RF-009/010/011) —
  cadastro opcional de barcode em produtos (migração 9→10, índice único),
  componente reutilizável de câmera (CameraX + ML Kit, offline), leitura via
  leitor físico USB/Bluetooth na tela de venda.
- **Fase 8 (modo offline e sincronização com o backend)**: em andamento a
  nível de planejamento/kickoff. Depende do **PDVMaquinetaBackend estar
  terminado** antes de fechar o contrato de sincronização (fila
  offline→online via WorkManager, conforme `CLAUDE.md`) — não faz sentido
  implementar o cliente de sync contra uma API que ainda está mudando.
- **Fase 9 (não funcionais)**: não iniciada.

## Primeiros passos práticos

1. Ler `CLAUDE.md` e a seção correspondente do `ROADMAP.md` antes de tocar em
   qualquer fase.
2. `local.properties` não vai vir no clone (está no `.gitignore`) — criar o
   seu apontando pro seu Android SDK local.
3. Antes de mexer em `ProductEntity`/schema do Room, ler como as migrações
   anteriores foram escritas (`DatabaseMigrations.kt` + os testes
   `Migration*Test.kt` correspondentes) e seguir o mesmo padrão.
