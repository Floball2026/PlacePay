# Roadmap — PDV Maquineta

## Fase 0 — Fundação (concluída)
Clean Architecture (data/domain/presentation), Hilt, Room vazio, tema Compose
para tela pequena/alto contraste, tela placeholder.

## Fase 1 (concluída)
Autenticação, perfis, bloqueio por inatividade, auditoria

Login usuário/senha, `AuthorizationPolicy`/`Permission` (RF-002), bloqueio por
inatividade (60s fixo), auditoria de login/logout/bloqueio. Base reaproveitada
por todas as fases seguintes (gate de supervisor, vocabulário de auditoria).

## Fase 2 (concluída e validada)
Caixa - abertura/fechamento/sangria/suprimento

Implementado: entidades `cash_sessions`/`cash_movements`, gate de autorização de
supervisor para sangria/suprimento (reaproveitável na Fase 3), telas de
abertura/dashboard/movimento/fechamento com divergência calculada. Build limpo
(`app:assembleDebug`), instalação fresca no dispositivo confirmada sem crash
via logcat.

Migração 1→2 testada e validada: o banco subiu de version 1→2 com uma
`Migration` real (`MIGRATION_1_2`, SQL conferido contra `schemas/2.json`),
substituindo o `fallbackToDestructiveMigration` inicial que apagaria dados a
cada mudança de schema. O teste instrumentado `Migration1To2Test` recria via
SQL puro o banco real da Fase 1 (só `users`/`audit_log`, version=1), insere um
usuário, e abre esse banco com o mesmo `Room.databaseBuilder(...)
.addMigrations(MIGRATION_1_2).build()` usado em produção — rodou no
dispositivo físico (`connectedDebugAndroidTest`) em 2026-07-07 com
**0 falhas**: a migração executa sem crash, o usuário pré-existente continua
lá depois, e as tabelas novas (`cash_sessions`/`cash_movements`) ficam
utilizáveis. Upgrades futuros de schema não devem mais apagar dado de
caixa/auditoria.

## Fase 3 (concluída e validada)
Produtos e carrinho de venda

Implementado: entidades `products`/`sales`/`sale_items`; gestão de produtos
restrita a supervisor/admin (`Permission.MANAGE_PRODUCTS`, expandido pra
supervisor nesta fase) com auditoria de criação/alteração de preço/status
(`PRODUCT_CREATED`, `PRODUCT_PRICE_CHANGED`, `PRODUCT_STATUS_CHANGED`); tela
de venda com busca, carrinho, desconto (>10% exige autorização de supervisor,
reaproveitando `AuthorizeWithSupervisorUseCase` da Fase 2), suspender/retomar,
cancelamento com motivo obrigatório e auditoria (`SALE_CANCELLED`), e
finalização (`SALE_FINALIZED`) — a venda migra pra `AWAITING_PAYMENT` e fica
como placeholder até a Fase 4 existir. Paleta Place Pay (navy/dourado)
aplicada. Validado visualmente no dispositivo pelo usuário: cadastro/edição/
ativação de produto e o fluxo completo de venda (carrinho, desconto,
suspender/retomar, cancelar, finalizar).

Migração 2→3 testada e validada: o banco subiu de version 2→3 com
`MIGRATION_2_3` (SQL conferido contra `schemas/3.json`). O teste instrumentado
`Migration2To3Test` recria via SQL puro o banco real da Fase 2 (`users`/
`audit_log`/`cash_sessions`/`cash_movements`, version=2), insere uma sessão de
caixa aberta, e abre esse banco com o mesmo `Room.databaseBuilder(...)
.addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()` usado em produção —
rodou no dispositivo físico (`connectedDebugAndroidTest`) em 2026-07-08 com
**0 falhas**: a migração executa sem crash, o caixa aberto pré-existente
continua lá depois, e as tabelas novas (`products`/`sales`/`sale_items`)
ficam utilizáveis.

## Fase 4 (concluída e validada)
Pagamentos com gateway mock (aguardando doc da Plasyplay)

Implementado: `PaymentGateway`/`MockPaymentGateway` (dinheiro, cartão crédito/
débito, Pix, com estados aprovado/recusado/timeout — convenção de teste ,13/
,66 isolada dentro do mock, sem vazar pro domínio/UI, pra trocar pela Plasyplay
depois sem reescrever nada fora de `data/payment`); entidade `payments` (uma
linha por tentativa, pronta pra pagamento misto no futuro); venda migra de
`AWAITING_PAYMENT` pra `COMPLETED` só quando aprovada, com `ReopenSaleUseCase`
pro "voltar ao carrinho"; auditoria `PAYMENT_CONFIRMED`/`PAYMENT_DECLINED`;
telas de escolha de forma de pagamento, processamento e comprovante (sem
impressão/envio, isso é Fase 6). Fechamento de caixa (Fase 2) conectado às
vendas reais: totais por forma de pagamento e vendas em dinheiro somadas ao
saldo esperado, substituindo o placeholder "indisponível". Validado
visualmente no dispositivo pelo usuário: os três cenários (aprovado, recusado
,13, timeout ,66 com o atraso simulado), comprovante e fechamento de caixa com
valores reais.

Migração 3→4 testada e validada: o banco subiu de version 3→4 com
`MIGRATION_3_4` (SQL conferido contra `schemas/4.json`). O teste instrumentado
`Migration3To4Test` recria via SQL puro o banco real da Fase 3 (`users`/
`audit_log`/`cash_sessions`/`cash_movements`/`products`/`sales`/`sale_items`,
version=3), com um caixa aberto e uma venda já registrados, e abre esse banco
com o mesmo `Room.databaseBuilder(...)
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()` usado em
produção — rodou no dispositivo físico (`connectedDebugAndroidTest`) em
2026-07-08 com **0 falhas**: a migração executa sem crash, o caixa e a venda
pré-existentes continuam lá depois, e a tabela nova (`payments`) fica
utilizável.

## Fase 5a (concluída e validada)
Administração de usuários

Implementado: `UserEntity` ganhou `pinSalt`, `mustChangePin`, `createdByUserId`,
`updatedAt`. Login híbrido por decisão deliberada (Opção B): contas legadas
(criadas por senha, sem PIN) continuam autenticando por senha normalmente;
usuários novos, criados pela tela de administração, autenticam só por PIN de
6 dígitos — a tela de login detecta reativamente (via `hasPinSet`) qual
teclado mostrar a partir do username digitado, sem exigir migração das contas
antigas. `mustChangePin` intercepta o app logo após um login por PIN
aprovado, forçando a tela "Defina seu novo PIN" antes de liberar qualquer
outra tela (usado tanto na criação de usuário quanto em "forçar troca de PIN"
pelo admin). Administração completa: criar (nome/usuário/perfil/PIN),
editar (nome/perfil, sem tocar em usuário/PIN), ativar/desativar e forçar
troca de PIN, todos com auditoria (`USER_CREATED`, `USER_ROLE_CHANGED`,
`USER_ACTIVATED`, `USER_DEACTIVATED`, `USER_DEACTIVATION_BLOCKED`,
`USER_PIN_RESET_FORCED`, `USER_PIN_CHANGED`) e proteção contra zerar os
admins ativos do sistema (bloqueia tanto rebaixar quanto desativar o último
admin ativo, com mensagem clara). Como efeito colateral necessário de
introduzir PIN, o desbloqueio por inatividade (`LockScreen`) também passou a
aceitar PIN, evitando travar usuários PIN-only.

Seed automático de admin (`AdminSeedCallback`, `RoomDatabase.Callback.onOpen`):
depois que os botões de debug de criação de usuário de teste foram removidos,
uma investigação no banco do dispositivo revelou zero usuários com role ADMIN
ativo (só o operador de teste histórico existia) — sem isso, ninguém
conseguiria acessar a própria tela de administração para criar um admin de
verdade. A decisão adotada (Opção B entre as alternativas discutidas) foi
seedar automaticamente um usuário `admin` (PIN temporário `000000`,
`mustChangePin=true`) sempre que `onOpen` detectar zero admins ativos no
banco — cobre tanto instalação nova (banco criado direto na versão atual)
quanto upgrade de um banco existente (depois das migrações), e também
restaura o acesso automaticamente se todos os admins forem desativados no
futuro, sem duplicar a linha caso o username `admin` já exista. Auditado como
`USER_CREATED` com `username="system"` e `createdByUserId=null`, deixando
explícito que a criação foi automática, não humana.

Migração 4→5 testada e validada: o banco subiu de version 4→5 com
`MIGRATION_4_5` (`ALTER TABLE` adicionando as colunas novas + `UPDATE users
SET updatedAt = createdAt`, SQL conferido contra `schemas/5.json`) — sem
`fallbackToDestructiveMigration`. O teste instrumentado `Migration4To5Test`
recria via SQL puro o banco real da Fase 4 (schema completo de 8 tabelas,
version=4) com um usuário admin pré-existente, e abre esse banco com o mesmo
`Room.databaseBuilder(...).addMigrations(MIGRATION_1_2, MIGRATION_2_3,
MIGRATION_3_4, MIGRATION_4_5).build()` usado em produção — rodou no
dispositivo físico (`connectedDebugAndroidTest`) em 2026-07-08 com
**0 falhas**: a migração executa sem crash, o usuário pré-existente continua
lá com `updatedAt` preenchido a partir de `createdAt`, e as colunas novas
ficam utilizáveis. Validado visualmente no dispositivo pelo usuário: seed de
admin, login híbrido, troca forçada de PIN, desbloqueio por PIN,
criar/editar/ativar/desativar usuário, e o bloqueio do último admin ativo.

## Fase 5 (concluída e validada)
Clientes e fidelidade

Implementado: entidades `customers`, `loyalty_configs`, `loyalty_transactions`;
`sales` ganhou `customerId` (nullable, sem FK — ver nota abaixo) e
`loyaltyDiscountCents`. Cliente é opcional na venda (decisão de negócio
revertida durante a validação: tornar obrigatório atrapalhava a agilidade do
caixa) — quando associado, via busca por telefone ou mini-cadastro rápido
(nome + telefone) direto na tela de venda, com opção de remover o cliente da
venda a qualquer momento (bloqueado se houver resgate de fidelidade ativo).
`LoyaltyEngine` (mesmo espírito do `PaymentGateway`: abstração trocável) com
duas modalidades configuráveis pelo admin — `PointsPerValueEngine` (pontos por
valor gasto) e `VisitCountDiscountEngine` (desconto por frequência de
visitas) — escolhidas em runtime por uma `LoyaltyEngineFactory` a partir da
config ativa, já que (diferente do gateway de pagamento) o modo muda
dinamicamente. Trocar de modo fecha a config atual (`deactivatedAt`) e
insere uma nova como ativa; o histórico da config anterior fica congelado,
não apagado. Tela de configuração reformulada com campos diretos ("a cada
R$X gastos, ganha Y pontos", "a cada X pontos, resgata R$Y") depois de uma
confusão real de configuração durante os testes (a forma matemática invertida
original — pontos por R$1 — não é como o dono do comércio pensa). Resgate é
parcial automático: consome só os pontos inteiros necessários pra cobrir o
que falta pagar (calculado sobre `netBeforeLoyaltyCents`, depois do desconto
manual, não o subtotal bruto), nunca o saldo inteiro — o resto continua
disponível; o carrinho fica travado enquanto o resgate estiver aplicado
(edição de item/desconto bloqueada), com "Desfazer resgate" liberando e
gravando um estorno (`REDEMPTION_REVERSED`, a transação original nunca é
apagada). Ganho de pontos/visita só é registrado no pagamento aprovado
(`ProcessPaymentUseCase`), nunca na finalização ou em venda pendente/recusada.
Cancelamento de venda (`CancelSaleUseCase`) reverte automaticamente qualquer
resgate de fidelidade vinculado à venda cancelada, reaproveitando
`UndoLoyaltyRedemptionUseCase` com um motivo (`LoyaltyRedemptionReversalReason`)
que diferencia na auditoria estorno manual (botão) de estorno automático por
cancelamento — bug real descoberto durante a validação: duas vendas de teste
canceladas antes dessa correção tinham ficado com pontos consumidos sem
nunca terem sido pagas; corrigido com uma correção retroativa pontual direto
no banco do dispositivo (não migração — é dado, não schema), auditada como
correção retroativa, com saldo dos dois clientes de teste confirmado restaurado
via consulta direta no banco antes de prosseguir. Nota de schema: `sales.customerId`
não tem foreign key — adicionar uma a uma tabela existente exigiria recriar a
tabela inteira no SQLite (ALTER TABLE não suporta), então ficou como coluna
simples, mesmo trade-off já aceito pelas colunas que a Fase 5a adicionou em
`users`.

Migração 5→6 testada e validada: o banco subiu de version 5→6 com
`MIGRATION_5_6` (`ALTER TABLE` em `sales` pras duas colunas novas + `CREATE
TABLE` das três tabelas novas, SQL conferido contra `schemas/6.json`) — sem
`fallbackToDestructiveMigration`. O teste instrumentado `Migration5To6Test`
recria via SQL puro o banco real da Fase 5a (schema completo de 8 tabelas,
version=5) com uma venda já registrada, e abre esse banco com o mesmo
`Room.databaseBuilder(...).addMigrations(MIGRATION_1_2, MIGRATION_2_3,
MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()` usado em produção —
rodou no dispositivo físico (`connectedDebugAndroidTest`) em 2026-07-09 com
**0 falhas**: a migração executa sem crash, a venda pré-existente continua lá
com `customerId=null`/`loyaltyDiscountCents=0` por padrão, e as tabelas novas
ficam utilizáveis (mesmo depois das correções de resgate parcial e reversão
por cancelamento, sem exigir mudança de schema — enums novos como
`REDEMPTION_REVERSED` são armazenados como `TEXT`, não coluna). Validado
visualmente no dispositivo pelo usuário: cliente opcional na venda,
mini-cadastro, remoção de cliente, os dois modos de fidelidade com troca de
config, resgate parcial, trava de carrinho com desfazer resgate, e
cancelamento estornando fidelidade automaticamente.

**Lacunas conscientes em relação ao documento de requisitos (RF-025 a
RF-028)** — registradas como pendência pra uma iteração futura, não
esquecimento:
- Busca de cliente hoje só por telefone; o documento pede também CPF, QR
  Code e código de fidelidade.
- Sem campo de e-mail no cadastro de cliente.
- Fidelidade cobre só pontos por valor e desconto por visita; o documento
  cita também cashback como modalidade.
- Não existe o conceito de "ofertas exibidas antes da conclusão da venda".

## Fase 6 — Comprovantes (parte não-fiscal concluída e validada; parte fiscal pendente)
Comprovantes e emissão fiscal

Implementado (parte não-fiscal): entidade `terminal_config` (linha única,
sempre atualizada em vez de versionada — diferente de `loyalty_configs`,
não há necessidade de histórico de reconfigurações de terminal) com
`terminalName`/`storeName`/`environment`/`printerType`, editável só por admin
(`Permission.MANAGE_TERMINALS`, já existia órfã no enum desde a Fase 0) via
`UpdateTerminalConfigUseCase`, auditado como `TERMINAL_CONFIG_UPDATED`.
`sales` ganhou `terminalNameSnapshot`/`storeNameSnapshot` (nullable), gravados
por `FinalizeSaleUseCase` a partir da config ativa no momento da finalização —
mesmo princípio de snapshot já usado em `SaleItem` (produto/preço) e
`LoyaltyTransaction.mode`: reconfigurar o terminal depois não altera
retroativamente o comprovante de vendas antigas. `ReceiptScreen` completada
com número da venda, data/hora, operador, loja e terminal, além dos botões
"Imprimir" (`ReceiptPrinter`) e "Enviar" (`DigitalReceiptSender`, com seletor
de canal WhatsApp/SMS/E-mail) — ambas interfaces mockadas
(`MockReceiptPrinter`/`MockDigitalReceiptSender`, mesmo padrão trocável do
`PaymentGateway`: sem hardware de impressão nem provedor de envio digital
contratados ainda), auditadas como `RECEIPT_PRINTED`/`RECEIPT_SENT_DIGITALLY`.
Histórico de vendas (RF-032) via `SaleDao.observeCompletedSales()` com filtro
opcional por data e nova `SaleHistoryScreen`, acessível a qualquer perfil
(reimprimir/reenviar comprovante é parte de "emitir comprovante", já coberto
pelo perfil de operador) — tocar numa venda do histórico abre a mesma
`ReceiptScreen` em modo reimpressão, reconstruindo os dados a partir do banco
em vez do fluxo pós-venda em memória. Durante a implementação, uma correção
necessária: `SaleViewModel.finalizeSale()` capturava a venda *antes* de
chamar `FinalizeSaleUseCase`, então o comprovante nunca via o snapshot de
terminal recém-gravado — corrigido para usar a venda retornada pelo caso de
uso.

Migração 7→8 testada e validada: o banco subiu de version 7→8 com
`MIGRATION_7_8` (`ALTER TABLE` em `sales` pras duas colunas novas + `CREATE
TABLE` de `terminal_config`, SQL conferido contra `schemas/8.json`) — sem
`fallbackToDestructiveMigration`. O teste instrumentado `Migration7To8Test`
recria via SQL puro o banco real da Fase 5 (schema completo de 11 tabelas,
version=7) com uma venda concluída já registrada, e abre esse banco com o
mesmo `Room.databaseBuilder(...).addMigrations(MIGRATION_1_2, MIGRATION_2_3,
MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
MIGRATION_7_8).build()` usado em produção — rodou no dispositivo físico
(`connectedDebugAndroidTest`) em 2026-07-10 com **0 falhas**: a migração
executa sem crash, a venda pré-existente continua lá com os snapshots de
terminal/loja nulos (retroativo, sem valor histórico), e a tabela nova
`terminal_config` fica utilizável. Validado visualmente no dispositivo pelo
usuário: configuração de terminal (admin only), comprovante completo,
imprimir/enviar mockados simulando sucesso, histórico com busca por data,
reimpressão a partir do histórico, e snapshot de terminal confirmado não
mudando retroativamente após reconfiguração.

**Pendente — parte fiscal (RF-033/RF-034: NFC-e/SAT/MFE)**: não implementada
nesta etapa, aguardando definição do modelo de emissão fiscal e do provedor
junto ao diretor/cliente final. Registrado como pendência consciente, não
esquecimento — a Fase 6 só pode ser marcada como totalmente concluída depois
dessa definição.

## Fase 7 (em andamento)
Estoque, relatórios, exportação, integrações

### Fase 7a — Estoque (concluída e validada)

Implementado: `products` ganhou `stockQuantity` (Int, default 0),
`minStockAlert` (Int?, nullable — sem alerta configurado quando nulo) e
`allowSaleWithoutStock` (Boolean, default false, configurável por produto).
Baixa automática de estoque (RF-035) via `DecrementStockForSaleUseCase`,
chamada dentro de `ProcessPaymentUseCase` no mesmo branch de pagamento
aprovado onde já roda o ganho de fidelidade — decrementa incondicionalmente
pela quantidade vendida, podendo deixar `stockQuantity` negativo quando o
produto permite venda sem estoque (refletindo estoque "a repor").
`ValidateCartStockUseCase` bloqueia a finalização da venda direto no
carrinho, antes de chegar à tela de pagamento, quando algum item tem
`allowSaleWithoutStock=false` e estoque insuficiente, com mensagem clara.
Consulta e alerta (RF-036): `ProductFormScreen` ganhou os três campos novos
(disponíveis tanto na criação quanto na edição do produto, por decisão
deliberada — evita forçar um cadastro seguido de edição imediata só pra
informar o estoque inicial); `ProductListScreen` mostra o estoque atual de
cada produto e destaca visualmente (badge "Estoque baixo", cor de erro)
quando `stockQuantity <= minStockAlert`. Auditoria `STOCK_ADJUSTED` nova,
disparada só em `UpdateProductUseCase` quando o estoque muda manualmente pela
tela de edição — a baixa automática por venda não gera essa auditoria própria
(já coberta pela auditoria de pagamento/venda).

Bug adormecido descoberto e corrigido durante a validação (não é da Fase 7a,
é da Fase 6): `AdminSeedCallback` fazia um `INSERT INTO users` bruto que não
listava a coluna `themeTone` (adicionada na Fase 6). O valor padrão do Kotlin
(`= "NAVY_DARK"`) não vira uma cláusula `DEFAULT` no `CREATE TABLE` gerado
pelo Room — só a migração escrita à mão tem esse `DEFAULT`, e só serve pra
atualizar linhas já existentes. Numa instalação genuinamente nova, a coluna
fica `NOT NULL` sem valor padrão nenhum, e o `INSERT` do seed de admin
quebrava com `NOT NULL constraint failed: users.themeTone` — mas isso ficou
adormecido desde a Fase 6 porque sempre havia um admin pré-existente no
banco do dispositivo (o seed só roda quando não há nenhum admin ativo).
Corrigido incluindo `themeTone` explicitamente no `INSERT`; confirmado que
não há outro `INSERT` bruto no projeto com o mesmo risco.

Migração 8→9 testada e validada: o banco subiu de version 8→9 com
`MIGRATION_8_9` (`ALTER TABLE` em `products` pras três colunas novas, SQL
conferido contra `schemas/9.json`) — sem `fallbackToDestructiveMigration`. O
teste instrumentado `Migration8To9Test` recria via SQL puro o banco real da
Fase 6 completa (schema completo de 12 tabelas, version=8) com um produto já
cadastrado, e abre esse banco com o mesmo `Room.databaseBuilder(...)
.addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9).build()` usado em
produção — rodou no dispositivo físico (`connectedDebugAndroidTest`) em
2026-07-10 com **0 falhas**: a migração executa sem crash, o produto
pré-existente continua lá com os valores padrão de estoque (sem histórico
anterior à Fase 7a), e as colunas novas ficam utilizáveis. Validado
visualmente no dispositivo pelo usuário: cadastro de produto com estoque
inicial/alerta/toggle, baixa automática na venda, bloqueio de venda sem
estoque com mensagem clara, venda permitida com estoque negativo quando
configurado, alerta visual de estoque baixo na listagem, e auditoria
`STOCK_ADJUSTED` disparando só no ajuste manual.

### Fase 7b — Relatórios (concluída e validada)

Implementado: painel único (`ReportsScreen`/`ReportsViewModel`) combinando
Vendas, Caixa e Produtos, sem migração de schema — só queries agregadas
novas sobre `Sale`/`Payment`/`CashSession`/`CashMovement`/`Product` já
existentes. `GetReportUseCase` monta as três seções: **Vendas** (total
vendido e totais por forma de pagamento via
`PaymentDao.sumApprovedByMethodInRange`, mesmo padrão de agregação já usado
no fechamento de caixa da Fase 2/4; quantidade de vendas concluídas via
`SaleDao.countCompletedInRange`; top 5 produtos mais vendidos via
`SaleItemDao.getTopSellingProducts`, JOIN `sale_items`↔`sales`); **Caixa**
(sessões fechadas no período, totais de abertura/informado no fechamento,
sangrias/suprimentos via `CashMovementDao.sumByTypeInRange`, divergências
identificadas reaproveitando o `divergenceCents` já calculado e persistido no
fechamento da Fase 2); **Produtos** (estoque baixo agora — estado atual,
independente do período, via `ProductDao.findLowStock` — e os mesmos top
produtos da seção Vendas).

Regra de permissão (RF-002) imposta em dois níveis, não só na UI: o próprio
`GetReportUseCase` calcula o intervalo efetivo — quem só tem
`VIEW_OPERATIONAL_REPORTS` (Supervisor) tem qualquer `from` pedido (ou
ausente) sobrescrito para "30 dias atrás" se for mais antigo que isso; quem
tem `VIEW_FULL_REPORTS` (Admin) consulta sem limite, desde o início do
histórico. O período *efetivamente* aplicado (já ajustado) é sempre exibido
na tela ("Período consultado: ..."), nunca o que foi digitado sem ajuste.
Filtro "De"/"Até" estende o parser de data dd/mm/aaaa já usado no histórico
de vendas (Fase 6) de um único dia para um intervalo livre. Botão
"Relatórios" no `CashScreen` gated por `VIEW_OPERATIONAL_REPORTS` (Admin
herda automaticamente por ter todas as permissões; Operador nunca vê).
Botão de exportação só aparece pra quem tem `VIEW_FULL_REPORTS`, sempre
desabilitado com rótulo "Exportar (em breve)" — a exportação real fica pra
Fase 7c.

Validado visualmente no dispositivo pelo usuário: dashboard único, regra de
permissão aplicada corretamente no caso de uso (Supervisor limitado a 30
dias com período efetivo sempre exibido, Admin sem limite), botão de
exportação visível só para Admin e desabilitado, botão "Relatórios"
bloqueado para Operador, e as três seções (Vendas/Caixa/Produtos)
conferidas e corretas.

### Fase 7c — Exportação (concluída e validada)

Implementado: exportação do relatório (`ReportData` da Fase 7b) em CSV, PDF
e Excel a partir do botão "Exportar" (antes desabilitado com "em breve"),
com seletor de formato ao tocar. `org.dhatim:fastexcel` promovida de
`androidTestImplementation` (só o experimento isolado da Fase 7) para
`implementation`, já que passou a ser usada em produção. PDF gerado com
`android.graphics.pdf.PdfDocument` nativo (sem dependência externa), com
paginação automática; CSV é texto puro com seções por cabeçalho
(VENDAS/CAIXA/PRODUTOS); Excel usa uma aba por seção. `ReportExporter`
(interface em `domain`, implementação real em
`data.export.AndroidReportExporter`) — diferente de `PaymentGateway`/
`ReceiptPrinter`/`DigitalReceiptSender`, aqui não há mock: os três formatos
são implementáveis sem depender de SDK/provedor externo ainda não escolhido.
Arquivo salvo em `MediaStore.Downloads` (API 29+, sem precisar de permissão,
visível na pasta Downloads de verdade — resolve o problema de descoberta que
a Fase 6 teve com `Android/data`), com fallback pra `getExternalFilesDir` em
API < 29 exposto via `FileProvider` (novo no manifest); nome do arquivo
inclui o período consultado (`relatorio_aaaa-MM-dd_aaaa-MM-dd.ext`). Botão
"Compartilhar" via `Intent.ACTION_SEND` junto da confirmação de sucesso.
Exportação continua restrita a `VIEW_FULL_REPORTS` (Admin), verificado tanto
na UI quanto em `ExportReportUseCase`; `REPORT_EXPORTED` novo no
`AuditAction`, com formato e período no detalhe.

Correção de arquitetura durante a implementação: `formatCents` vivia em
`presentation.format`, mas o exportador (camada `data`) precisava dela —
violação da direção de dependência da Clean Architecture. Corrigido movendo
`formatCents` pra `domain.format.Money.kt` (formatação pura, sem
Android/Compose) e atualizando os imports em todas as telas que já usavam a
função; `parseToCents` (parsing de input do operador) permanece em
`presentation`, onde é específico de UI.

Sem migração de schema. Verificado antes da validação do usuário com um
teste instrumentado (`AndroidReportExporterTest`) que gera os três formatos
com dados sintéticos e roda no dispositivo físico — passou, confirmando que
PDF/MediaStore/fastexcel funcionam de verdade em produção (não só compilam).
Validado visualmente no dispositivo pelo usuário: os três formatos com dados
batendo com o dashboard da Fase 7b, arquivos salvos em Downloads com nome
refletindo o período real, botão "Compartilhar" funcionando, e exportação
bloqueada para Supervisor.

### Fase 7e — Leitura de código de barras (concluída e validada)

Implementado: cadastro opcional de código de barras em produtos (RF-009),
leitura via câmera (RF-010) e leitura via leitor físico USB/Bluetooth agindo
como teclado (RF-011). `ProductEntity` ganhou `barcode: String?` com índice
único (`Index(value = ["barcode"], unique = true)`) — permite múltiplos
produtos sem código (NULL não conflita em índice único no SQLite) mas barra
duplicidade entre os que têm. Migração 9→10 (`MIGRATION_9_10`, ALTER TABLE +
CREATE UNIQUE INDEX) testada com `Migration9To10Test`, seguindo o mesmo
padrão das anteriores.

Componente reutilizável `BarcodeScannerScreen` (CameraX + ML Kit Barcode
Scanning, `com.google.mlkit:barcode-scanning` — modelo embarcado no APK, sem
rede, alinhado ao offline-first do projeto): preview de câmera com overlay de
mira, cobrindo os formatos comuns de varejo (EAN-13/8, UPC-A/E, Code
128/39/93, ITF, QR Code). Permissão de câmera solicitada em runtime via
`rememberLauncherForActivityResult`, com explicação clara e atalho pras
configurações do app se negada — nunca crasha, mesmo sem câmera disponível no
aparelho (`uses-feature android.hardware.camera.any required="false"` no
manifest, cobrindo o caso de maquineta sem câmera dependendo só do leitor
físico).

`ProductFormScreen` ganhou o campo de código de barras (opcional) e o botão
"Escanear" (abre o scanner como estado local da própria tela, sem trocar de
rota — preserva os outros campos já digitados no formulário). Duplicidade
verificada em `CreateProductUseCase`/`UpdateProductUseCase` via
`ProductRepository.findByBarcode` antes de gravar, retornando
`SaveProductResult.DuplicateBarcode` com o nome do produto conflitante — sem
deixar estourar a constraint do banco sem tratamento.

Na tela de venda, `ProductDao.observeActive` passou a considerar `barcode`
além de nome/categoria na busca; botão "Escanear" adicionado ao lado do campo
de busca; leitor físico USB/Bluetooth coberto tratando o Enter do campo
(`onKeyEvent` da tecla física + `KeyboardActions.onDone` do IME, cobrindo
qualquer leitor) — ao bater com um produto ativo, adiciona direto ao carrinho
sem exigir toque adicional do operador (`SaleViewModel.submitBarcode`).

Validado no dispositivo físico pelo usuário: `Migration9To10Test` passou via
`connectedDebugAndroidTest` (`tests="1" failures="0"`); a migração real
também rodou sem crash sobre o banco de produção do aparelho (que já vinha de
fases anteriores em version=9, não só o teste isolado); leitura por câmera
funcionando de verdade tanto no cadastro de produto quanto na tela de venda.
Leitura via leitor físico USB/Bluetooth foi implementada mas ainda não
confirmada com um leitor real — pendência a validar quando houver um
disponível.

**Pendente — resto da Fase 7**: só falta 7d (Integração com ERP mockada)
para fechar a Fase 7 por completo.

## Fase 8
Modo offline e sincronização

## Fase 9
Não funcionais - segurança, performance, atualização remota, idiomas
