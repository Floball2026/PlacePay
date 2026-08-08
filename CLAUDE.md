# PDV Android para Maquineta

## Contexto
App de Ponto de Venda (PDV) para maquinetas Android. Escopo: vendas, pagamentos,
caixa, produtos, clientes, comprovantes, relatórios e integrações (ERP e adquirente).

## Perfis de usuário
- Operador de Caixa: vender, consultar produtos, receber pagamento, emitir comprovante, fechar caixa.
- Supervisor: autorizar descontos/cancelamentos/sangrias/suprimentos, ver relatórios operacionais.
- Administrador: configurar usuários, produtos, terminais, formas de pagamento, integrações, relatórios completos.

## Stack
- Kotlin + Jetpack Compose (Material 3)
- Arquitetura: Clean Architecture (data/domain/presentation) + MVVM
- Room para persistência local (essencial: app precisa funcionar offline)
- Retrofit/OkHttp para APIs externas (ERP, adquirente)
- Hilt para injeção de dependência
- WorkManager para fila de sincronização offline→online
- CameraX + ML Kit para leitura de código de barras
- AndroidX Biometric para login por biometria

## Convenções
- Todo dado sensível (token, senha, credenciais) fica em armazenamento criptografado — nunca em SharedPreferences puro ou hardcoded.
- Toda ação sensível (desconto, cancelamento, sangria, suprimento, fechamento de caixa) deve gerar registro de auditoria (usuário, data/hora, motivo, valor).
- Integrações de pagamento e impressão devem ser feitas atrás de uma interface (ex: PaymentGateway, ReceiptPrinter) com implementação mock, para permitir plugar o SDK real da adquirente depois sem reescrever a camada de domínio.
- O app deve assumir que pode ficar sem internet a qualquer momento durante uma venda: toda escrita crítica (venda, pagamento, caixa) precisa ser gravada localmente primeiro e sincronizada depois.
- UI pensada para maquineta: telas pequenas, botões grandes, alta legibilidade.

## Não fazer
- Não implementar chamada de SDK de pagamento real sem confirmação da adquirente escolhida — usar mock.
- Não deixar nenhuma tela crítica (venda, pagamento) travar caso a API externa esteja fora do ar.
- Não misturar lógica de permissão de perfil (operador/supervisor/admin) espalhada pela UI — centralizar em uma camada de autorização.