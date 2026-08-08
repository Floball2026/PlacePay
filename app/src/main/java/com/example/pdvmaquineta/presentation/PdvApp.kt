package com.example.pdvmaquineta.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.pdvmaquineta.domain.model.SessionState
import com.example.pdvmaquineta.presentation.cash.CashApp
import com.example.pdvmaquineta.presentation.cash.CashViewModel
import com.example.pdvmaquineta.presentation.customer.CustomerViewModel
import com.example.pdvmaquineta.presentation.loyalty.LoyaltyConfigViewModel
import com.example.pdvmaquineta.presentation.lock.LockScreen
import com.example.pdvmaquineta.presentation.login.LoginScreen
import com.example.pdvmaquineta.presentation.product.ProductViewModel
import com.example.pdvmaquineta.presentation.reports.ReportsViewModel
import com.example.pdvmaquineta.presentation.sale.SaleHistoryViewModel
import com.example.pdvmaquineta.presentation.sale.SaleViewModel
import com.example.pdvmaquineta.presentation.session.ChangePinScreen
import com.example.pdvmaquineta.presentation.session.SessionViewModel
import com.example.pdvmaquineta.presentation.terminal.TerminalConfigViewModel
import com.example.pdvmaquineta.presentation.settings.BusinessConfigViewModel
import com.example.pdvmaquineta.presentation.user.UserViewModel

@Composable
fun PdvApp(
    sessionViewModel: SessionViewModel,
    cashViewModel: CashViewModel,
    saleViewModel: SaleViewModel,
    productViewModel: ProductViewModel,
    userViewModel: UserViewModel,
    customerViewModel: CustomerViewModel,
    loyaltyConfigViewModel: LoyaltyConfigViewModel,
    saleHistoryViewModel: SaleHistoryViewModel,
    terminalConfigViewModel: TerminalConfigViewModel,
    businessConfigViewModel: BusinessConfigViewModel,
    reportsViewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val sessionState by sessionViewModel.sessionState.collectAsState()
    val unlockMethod by sessionViewModel.unlockMethod.collectAsState()

    when (val state = sessionState) {
        is SessionState.LoggedOut -> LoginScreen(
            uiState = sessionViewModel.loginUiState,
            loginMethod = sessionViewModel.loginMethod,
            onUsernameChange = sessionViewModel::onUsernameChange,
            onLoginWithPassword = sessionViewModel::login,
            onLoginWithPin = sessionViewModel::loginWithPin,
            modifier = modifier
        )

        is SessionState.Locked -> LockScreen(
            userDisplayName = state.user.displayName,
            unlockMethod = unlockMethod,
            uiState = sessionViewModel.unlockUiState,
            onUnlockWithPassword = sessionViewModel::unlock,
            onUnlockWithPin = sessionViewModel::unlockWithPin,
            onLogout = sessionViewModel::logout,
            modifier = modifier
        )

        is SessionState.Active -> if (state.user.mustChangePin) {
            ChangePinScreen(
                uiState = sessionViewModel.changePinUiState,
                onConfirm = sessionViewModel::confirmNewPin,
                onLogout = sessionViewModel::logout,
                modifier = modifier
            )
        } else {
            CashApp(
                user = state.user,
                viewModel = cashViewModel,
                saleViewModel = saleViewModel,
                productViewModel = productViewModel,
                userViewModel = userViewModel,
                customerViewModel = customerViewModel,
                loyaltyConfigViewModel = loyaltyConfigViewModel,
                saleHistoryViewModel = saleHistoryViewModel,
                terminalConfigViewModel = terminalConfigViewModel,
                businessConfigViewModel = businessConfigViewModel,
                reportsViewModel = reportsViewModel,
                onLogout = sessionViewModel::logout,
                modifier = modifier
            )
        }
    }
}
