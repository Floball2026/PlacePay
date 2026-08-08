package com.example.pdvmaquineta.presentation.cash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.pdvmaquineta.domain.authorization.Permission
import com.example.pdvmaquineta.domain.model.CashMovementType
import com.example.pdvmaquineta.domain.model.User
import com.example.pdvmaquineta.presentation.customer.CustomerApp
import com.example.pdvmaquineta.presentation.customer.CustomerViewModel
import com.example.pdvmaquineta.presentation.loyalty.LoyaltyConfigScreen
import com.example.pdvmaquineta.presentation.loyalty.LoyaltyConfigViewModel
import com.example.pdvmaquineta.presentation.product.ProductApp
import com.example.pdvmaquineta.presentation.product.ProductViewModel
import com.example.pdvmaquineta.presentation.reports.ReportsScreen
import com.example.pdvmaquineta.presentation.reports.ReportsViewModel
import com.example.pdvmaquineta.presentation.sale.SaleApp
import com.example.pdvmaquineta.presentation.sale.SaleHistoryApp
import com.example.pdvmaquineta.presentation.sale.SaleHistoryViewModel
import com.example.pdvmaquineta.presentation.sale.SaleViewModel
import com.example.pdvmaquineta.presentation.terminal.TerminalConfigScreen
import com.example.pdvmaquineta.presentation.terminal.TerminalConfigViewModel
import com.example.pdvmaquineta.presentation.settings.BusinessConfigScreen
import com.example.pdvmaquineta.presentation.settings.BusinessConfigViewModel
import com.example.pdvmaquineta.presentation.user.UserApp
import com.example.pdvmaquineta.presentation.user.UserViewModel

@Composable
fun CashApp(
    user: User,
    viewModel: CashViewModel,
    saleViewModel: SaleViewModel,
    productViewModel: ProductViewModel,
    userViewModel: UserViewModel,
    customerViewModel: CustomerViewModel,
    loyaltyConfigViewModel: LoyaltyConfigViewModel,
    saleHistoryViewModel: SaleHistoryViewModel,
    terminalConfigViewModel: TerminalConfigViewModel,
    businessConfigViewModel: BusinessConfigViewModel,
    reportsViewModel: ReportsViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by viewModel.openSession.collectAsState()

    if (session == null) {
        CashOpenScreen(
            uiState = viewModel.openCashUiState,
            onOpenCash = viewModel::openCash,
            modifier = modifier
        )
        return
    }

    when (val route = viewModel.route) {
        CashRoute.Dashboard -> CashScreen(
            user = user,
            overview = viewModel.overview,
            onNewSale = { viewModel.navigateTo(CashRoute.Sale) },
            onSupply = { viewModel.navigateTo(CashRoute.Movement(CashMovementType.SUPPLY)) },
            onWithdrawal = { viewModel.navigateTo(CashRoute.Movement(CashMovementType.WITHDRAWAL)) },
            onCloseCash = { viewModel.navigateTo(CashRoute.Close) },
            onManageProducts = { viewModel.navigateTo(CashRoute.Products) },
            onManageUsers = { viewModel.navigateTo(CashRoute.Users) },
            onManageCustomers = { viewModel.navigateTo(CashRoute.Customers) },
            onManageLoyalty = { viewModel.navigateTo(CashRoute.Loyalty) },
            onSaleHistory = { viewModel.navigateTo(CashRoute.SaleHistory) },
            onManageTerminal = { viewModel.navigateTo(CashRoute.Terminal) },
            onManageBusinessConfig = { viewModel.navigateTo(CashRoute.BusinessConfig) },
            onViewReports = { viewModel.navigateTo(CashRoute.Reports) },
            onLogout = onLogout,
            modifier = modifier
        )

        is CashRoute.Movement -> {
            val permission = if (route.type == CashMovementType.WITHDRAWAL) {
                Permission.AUTHORIZE_CASH_WITHDRAWAL
            } else {
                Permission.AUTHORIZE_CASH_SUPPLY
            }
            CashMovementScreen(
                type = route.type,
                uiState = viewModel.movementUiState,
                onSubmit = { amount, reason -> viewModel.registerMovement(route.type, amount, reason) },
                onAuthorize = { username, password ->
                    viewModel.authorizeSupervisor(permission, username, password)
                },
                onDismissAuthorization = viewModel::dismissAuthorizationPrompt,
                onCancel = { viewModel.navigateTo(CashRoute.Dashboard) },
                modifier = modifier
            )
        }

        CashRoute.Close -> CashCloseScreen(
            overview = viewModel.overview,
            uiState = viewModel.closeCashUiState,
            onConfirm = viewModel::closeCash,
            onCancel = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Sale -> SaleApp(
            viewModel = saleViewModel,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Products -> ProductApp(
            viewModel = productViewModel,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Users -> UserApp(
            viewModel = userViewModel,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Customers -> CustomerApp(
            viewModel = customerViewModel,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Loyalty -> {
            val activeConfig by loyaltyConfigViewModel.activeConfig.collectAsState()
            LoyaltyConfigScreen(
                activeConfig = activeConfig,
                uiState = loyaltyConfigViewModel.uiState,
                pendingChange = loyaltyConfigViewModel.pendingChange,
                onRequestChange = loyaltyConfigViewModel::requestChange,
                onConfirmChange = loyaltyConfigViewModel::confirmChange,
                onDismissChangeConfirmation = loyaltyConfigViewModel::dismissChangeConfirmation,
                onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
                modifier = modifier
            )
        }

        CashRoute.SaleHistory -> SaleHistoryApp(
            viewModel = saleHistoryViewModel,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Terminal -> {
            val terminalConfig by terminalConfigViewModel.config.collectAsState()
            TerminalConfigScreen(
                config = terminalConfig,
                uiState = terminalConfigViewModel.uiState,
                onSave = terminalConfigViewModel::save,
                activationState = terminalConfigViewModel.activationState,
                initialBaseUrl = terminalConfigViewModel.currentBaseUrl,
                isActivated = terminalConfigViewModel.isActivated,
                terminalId = terminalConfigViewModel.terminalId,
                onActivate = terminalConfigViewModel::activate,
                isOutdated = terminalConfigViewModel.isOutdated,
                currentVersion = terminalConfigViewModel.currentVersionCode,
                minVersion = terminalConfigViewModel.minAppVersionCode,
                onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
                modifier = modifier
            )
        }

        CashRoute.BusinessConfig -> BusinessConfigScreen(
            config = businessConfigViewModel.config,
            savedMessage = businessConfigViewModel.savedMessage,
            onChange = businessConfigViewModel::onChange,
            onSave = businessConfigViewModel::save,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )

        CashRoute.Reports -> ReportsScreen(
            user = user,
            reportData = reportsViewModel.reportData,
            uiState = reportsViewModel.uiState,
            exportUiState = reportsViewModel.exportUiState,
            onDateFilterChange = reportsViewModel::setDateFilter,
            onExport = reportsViewModel::exportReport,
            onBack = { viewModel.navigateTo(CashRoute.Dashboard) },
            modifier = modifier
        )
    }
}
