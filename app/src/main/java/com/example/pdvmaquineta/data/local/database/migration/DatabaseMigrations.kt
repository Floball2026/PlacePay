package com.example.pdvmaquineta.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Fase 1 -> Fase 2: adiciona as tabelas de caixa (cash_sessions, cash_movements).
// Não muda em nada `users`/`audit_log` — só cria as tabelas novas. O SQL abaixo
// é exatamente o que o Room gera para essas entidades hoje (ver
// app/schemas/.../2.json), para o hash de identidade bater após a migração.
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_sessions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, " +
                "`openingBalanceCents` INTEGER NOT NULL, " +
                "`openedAt` INTEGER NOT NULL, " +
                "`closedAt` INTEGER, " +
                "`expectedCashCents` INTEGER, " +
                "`informedCashCents` INTEGER, " +
                "`divergenceCents` INTEGER, " +
                "`status` TEXT NOT NULL)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cash_movements` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, " +
                "`reason` TEXT NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, " +
                "`authorizedByUsername` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_movements_cashSessionId` " +
                "ON `cash_movements` (`cashSessionId`)"
        )
    }
}

// Fase 2 -> Fase 3: adiciona produtos, vendas e itens de venda. SQL exato
// gerado pelo Room para essas entidades (ver app/schemas/.../3.json).
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`priceCents` INTEGER NOT NULL, " +
                "`category` TEXT, " +
                "`active` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_products_active` ON `products` (`active`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sales` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`cashSessionId` INTEGER NOT NULL, " +
                "`operatorId` INTEGER NOT NULL, " +
                "`operatorUsername` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`discountPercent` INTEGER NOT NULL, " +
                "`cancellationReason` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`cashSessionId`) REFERENCES `cash_sessions`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_cashSessionId` ON `sales` (`cashSessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sales_status` ON `sales` (`status`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, " +
                "`productId` INTEGER NOT NULL, " +
                "`productName` TEXT NOT NULL, " +
                "`unitPriceCents` INTEGER NOT NULL, " +
                "`quantity` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`productId`) REFERENCES `products`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE NO ACTION )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_saleId` ON `sale_items` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_items_productId` ON `sale_items` (`productId`)")
    }
}

// Fase 3 -> Fase 4: adiciona a tabela de pagamentos. SQL exato gerado pelo
// Room (ver app/schemas/.../4.json).
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `payments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`saleId` INTEGER NOT NULL, " +
                "`method` TEXT NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, " +
                "`receivedCents` INTEGER, " +
                "`changeCents` INTEGER, " +
                "`status` TEXT NOT NULL, " +
                "`transactionId` TEXT, " +
                "`declineReason` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_payments_saleId` ON `payments` (`saleId`)")
    }
}

// Fase 4 -> Fase 5a: adiciona os campos de administração de usuários em
// `users` (PIN próprio de cada usuário, troca forçada, autoria de criação).
// SQL exato gerado pelo Room (ver app/schemas/.../5.json) — ALTER TABLE em
// vez de recriar a tabela, já que só estamos adicionando colunas.
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `pinSalt` TEXT")
        db.execSQL("ALTER TABLE `users` ADD COLUMN `mustChangePin` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `users` ADD COLUMN `createdByUserId` INTEGER")
        db.execSQL("ALTER TABLE `users` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `users` SET `updatedAt` = `createdAt`")
    }
}

// Fase 5a -> Fase 5: adiciona clientes e fidelidade. `sales` ganha duas
// colunas novas via ALTER TABLE (customerId sem FK, de propósito — ver nota
// no domínio; loyaltyDiscountCents); as três tabelas novas usam o SQL exato
// gerado pelo Room (ver app/schemas/.../6.json).
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `customerId` INTEGER")
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `loyaltyDiscountCents` INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `customers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`phone` TEXT NOT NULL, " +
                "`document` TEXT, " +
                "`active` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_configs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`mode` TEXT NOT NULL, " +
                "`pointsPerCurrencyUnit` REAL, " +
                "`pointValueInCents` INTEGER, " +
                "`visitsRequired` INTEGER, " +
                "`discountPercentOnReward` INTEGER, " +
                "`activatedAt` INTEGER NOT NULL, " +
                "`deactivatedAt` INTEGER, " +
                "`changedByUserId` INTEGER)"
        )

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `loyalty_transactions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`customerId` INTEGER NOT NULL, " +
                "`saleId` INTEGER, " +
                "`mode` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`points` INTEGER, " +
                "`amountCents` INTEGER, " +
                "`timestamp` INTEGER NOT NULL, " +
                "FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_customerId` " +
                "ON `loyalty_transactions` (`customerId`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_loyalty_transactions_saleId` " +
                "ON `loyalty_transactions` (`saleId`)"
        )
    }
}

// Fase 5 -> Fase 6: adiciona a preferência de tom de tema por usuário. SQL
// exato gerado pelo Room (ver app/schemas/.../7.json) — ALTER TABLE, já que
// só estamos adicionando uma coluna.
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `themeTone` TEXT NOT NULL DEFAULT 'NAVY_DARK'")
    }
}

// Fase 6 -> Fase 6 (comprovantes, parte não-fiscal): adiciona a configuração
// de terminal (tabela nova, linha única) e o snapshot de terminal/loja em
// `sales`. SQL exato gerado pelo Room (ver app/schemas/.../8.json).
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `terminalNameSnapshot` TEXT")
        db.execSQL("ALTER TABLE `sales` ADD COLUMN `storeNameSnapshot` TEXT")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `terminal_config` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`terminalName` TEXT NOT NULL, " +
                "`storeName` TEXT NOT NULL, " +
                "`environment` TEXT NOT NULL, " +
                "`printerType` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "`updatedByUserId` INTEGER)"
        )
    }
}

// Fase 6 -> Fase 7a (estoque): adiciona controle de estoque em `products`.
// SQL exato gerado pelo Room (ver app/schemas/.../9.json) — ALTER TABLE, já
// que só estamos adicionando colunas.
val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `stockQuantity` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `minStockAlert` INTEGER")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `allowSaleWithoutStock` INTEGER NOT NULL DEFAULT 0")
    }
}

// Fase 7d (leitura de código de barras, RF-009/010/011): adiciona o código
// de barras opcional em `products`. SQL exato gerado pelo Room (ver
// app/schemas/.../10.json) — ALTER TABLE pra coluna, mais um índice único que
// permite múltiplos produtos com barcode NULL (SQLite não considera NULL
// duplicado em índice único) mas barra duplicidade entre os que têm valor.
val MIGRATION_9_10: Migration = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `barcode` TEXT")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)")
    }
}

// Fase 8 (imagem de produto): adiciona o nome do arquivo de imagem opcional
// em `products`. ALTER TABLE simples (coluna nova, nullable).
val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `imagePath` TEXT")
    }
}

// Integração SaaS (Carga PLU): guarda o id do produto no servidor e a URL da
// imagem remota, para o sync incremental por remoteId.
val MIGRATION_11_12: Migration = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `products` ADD COLUMN `remoteId` TEXT")
        db.execSQL("ALTER TABLE `products` ADD COLUMN `imageUrl` TEXT")
    }
}


// Envio de vendas (fila offline): cria a tabela de outbox que guarda cada
// venda concluida pronta pra enviar ao SaaS, com UUID de idempotencia.
val MIGRATION_12_13: Migration = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `sale_outbox` (" +
                "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                "`saleId` INTEGER NOT NULL, " +
                "`transactionUuid` TEXT NOT NULL, " +
                "`payload` TEXT NOT NULL, " +
                "`status` TEXT NOT NULL, " +
                "`attempts` INTEGER NOT NULL, " +
                "`lastError` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sale_outbox_saleId` ON `sale_outbox` (`saleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_outbox_status` ON `sale_outbox` (`status`)")
    }
}


// Sync de operadores: guarda o id do operador no servidor pra dar upsert por
// remoteId (fallback por username).
val MIGRATION_13_14: Migration = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `remoteId` TEXT")
    }
}


// Sync de clientes: guarda o id do cliente no servidor (upsert por remoteId,
// fallback por telefone/documento) e relaxa o indice de telefone para
// nao-unico (o SaaS permite cliente sem telefone; multiplos "" quebrariam o
// indice unico antigo).
val MIGRATION_14_15: Migration = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `customers` ADD COLUMN `remoteId` TEXT")
        db.execSQL("DROP INDEX IF EXISTS `index_customers_phone`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_phone` ON `customers` (`phone`)")
    }
}
