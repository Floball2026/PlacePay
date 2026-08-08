package com.example.pdvmaquineta.presentation.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.example.pdvmaquineta.domain.authorization.AuthorizationPolicy
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.ThemeTone
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.domain.model.UserRole
import com.example.pdvmaquineta.domain.usecase.CashOverview
import com.example.pdvmaquineta.domain.format.formatCents
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvButton
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.theme.PdvOutlinedButton

private fun roleLabel(role: UserRole): String = when (role) {
    UserRole.OPERATOR -> "Operador de Caixa"
    UserRole.SUPERVISOR -> "Supervisor"
    UserRole.ADMIN -> "Administrador"
}

@Composable
fun CashScreen(
    user: User,
    overview: CashOverview?,
    onNewSale: () -> Unit,
    onSupply: () -> Unit,
    onWithdrawal: () -> Unit,
    onCloseCash: () -> Unit,
    onManageProducts: () -> Unit,
    onManageUsers: () -> Unit,
    onManageCustomers: () -> Unit,
    onManageLoyalty: () -> Unit,
    onSaleHistory: () -> Unit,
    onManageTerminal: () -> Unit,
    onManageBusinessConfig: () -> Unit,
    onViewReports: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PdvDimens.SpacingLarge, vertical = PdvDimens.SpacingMedium),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(PdvDimens.SpacingSmall))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PdvDimens.SpacingMedium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PDV Maquineta",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Bem-vindo, ${user.displayName}",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = PdvDimens.SpacingSmall)
                )
                val role = roleLabel(user.role)
                if (role != user.displayName) {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        if (overview != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PdvDimens.SpacingMedium),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Caixa aberto", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(PdvDimens.SpacingSmall))
                    Text(
                        text = "Saldo em dinheiro: ${formatCents(overview.expectedCashCents)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Text(
                        text = "Sangrias: ${formatCents(overview.withdrawalTotalCents)}  •  Suprimentos: ${formatCents(overview.supplyTotalCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        // Ação primária destacada: mais alta, com ícone e amarelo (o amarelo
        // fica reservado só para esta tela — os demais botões são neutros).
        PdvButton(
            onClick = onNewSale,
            modifier = Modifier
                .fillMaxWidth()
                .height(PdvDimens.HeroButtonHeight)
        ) {
            Icon(imageVector = Icons.Filled.ShoppingCart, contentDescription = null)
            Spacer(Modifier.width(PdvDimens.SpacingSmall))
            Text("Nova Venda", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(Modifier.height(PdvDimens.SpacingMedium))

        // Demais ações em grid de 2 colunas para caber sem rolar. Visibilidade
        // decidida só pela AuthorizationPolicy (RF-002), nunca por checagem de
        // perfil espalhada na tela.
        val actions = buildList {
            add(CashAction("Suprimento", onSupply))
            add(CashAction("Sangria", onWithdrawal))
            add(CashAction("Clientes", onManageCustomers))
            add(CashAction("Histórico", onSaleHistory))
            if (AuthorizationPolicy.hasPermission(user.role, Permission.MANAGE_PRODUCTS)) {
                add(CashAction("Produtos", onManageProducts))
            }
            if (AuthorizationPolicy.hasPermission(user.role, Permission.MANAGE_USERS)) {
                add(CashAction("Usuários", onManageUsers))
            }
            if (AuthorizationPolicy.hasPermission(user.role, Permission.MANAGE_LOYALTY)) {
                add(CashAction("Fidelidade", onManageLoyalty))
            }
            if (AuthorizationPolicy.hasPermission(user.role, Permission.MANAGE_TERMINALS)) {
                add(CashAction("Terminal", onManageTerminal))
            }
            if (AuthorizationPolicy.hasPermission(user.role, Permission.MANAGE_BUSINESS_CONFIG)) {
                add(CashAction("Configurações", onManageBusinessConfig))
            }
            if (AuthorizationPolicy.hasPermission(user.role, Permission.VIEW_OPERATIONAL_REPORTS)) {
                add(CashAction("Relatórios", onViewReports))
            }
            add(CashAction("Fechar caixa", onCloseCash))
            add(CashAction("Sair", onLogout, Icons.AutoMirrored.Filled.ExitToApp))
        }

        actions.chunked(2).forEach { rowActions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PdvDimens.SpacingMedium)
            ) {
                rowActions.forEach { action ->
                    PdvOutlinedButton(
                        onClick = action.onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(PdvDimens.GridButtonHeight)
                    ) {
                        action.icon?.let {
                            Icon(imageVector = it, contentDescription = null)
                            Spacer(Modifier.width(PdvDimens.SpacingSmall))
                        }
                        Text(
                            action.label,
                            maxLines = 1,
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp)
                        )
                    }
                }
                // Mantém a última coluna alinhada quando o número de ações é ímpar.
                if (rowActions.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(PdvDimens.SpacingSmall))
        }

        Spacer(Modifier.height(PdvDimens.SpacingSmall))
    }
}

// Item de ação do grid do caixa (label + ação, ícone opcional).
private data class CashAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null
)

@Preview(showBackground = true)
@Composable
private fun CashScreenPreview() {
    PDVMaquinetaTheme {
        CashScreen(
            user = User(
                id = 1,
                username = "teste",
                displayName = "Operador de Teste",
                role = UserRole.OPERATOR,
                active = true,
                mustChangePin = false,
                themeTone = ThemeTone.NAVY_DARK
            ),
            overview = null,
            onNewSale = {},
            onSupply = {},
            onWithdrawal = {},
            onCloseCash = {},
            onManageProducts = {},
            onManageUsers = {},
            onManageCustomers = {},
            onManageLoyalty = {},
            onSaleHistory = {},
            onManageTerminal = {},
            onManageBusinessConfig = {},
            onViewReports = {},
            onLogout = {}
        )
    }
}
