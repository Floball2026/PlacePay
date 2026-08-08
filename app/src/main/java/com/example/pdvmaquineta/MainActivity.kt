package com.example.pdvmaquineta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.pdvmaquineta.presentation.PdvApp
import com.example.pdvmaquineta.presentation.cash.CashViewModel
import com.example.pdvmaquineta.presentation.customer.CustomerViewModel
import com.example.pdvmaquineta.presentation.loyalty.LoyaltyConfigViewModel
import com.example.pdvmaquineta.presentation.product.ProductViewModel
import com.example.pdvmaquineta.presentation.reports.ReportsViewModel
import com.example.pdvmaquineta.presentation.sale.SaleHistoryViewModel
import com.example.pdvmaquineta.presentation.sale.SaleViewModel
import com.example.pdvmaquineta.presentation.session.InactivityAwareContent
import com.example.pdvmaquineta.presentation.session.SessionViewModel
import com.example.pdvmaquineta.presentation.terminal.TerminalConfigViewModel
import com.example.pdvmaquineta.presentation.settings.BusinessConfigViewModel
import com.example.pdvmaquineta.presentation.theme.PDVMaquinetaTheme
import com.example.pdvmaquineta.presentation.theme.PdvDimens
import com.example.pdvmaquineta.presentation.user.UserViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val sessionViewModel: SessionViewModel by viewModels()
    private val cashViewModel: CashViewModel by viewModels()
    private val saleViewModel: SaleViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()
    private val customerViewModel: CustomerViewModel by viewModels()
    private val loyaltyConfigViewModel: LoyaltyConfigViewModel by viewModels()
    private val saleHistoryViewModel: SaleHistoryViewModel by viewModels()
    private val terminalConfigViewModel: TerminalConfigViewModel by viewModels()
    private val businessConfigViewModel: BusinessConfigViewModel by viewModels()
    private val reportsViewModel: ReportsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeTone by sessionViewModel.themeTone.collectAsState()
            PDVMaquinetaTheme(tone = themeTone) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        InactivityAwareContent(onInactive = sessionViewModel::lock) {
                            PdvApp(
                                sessionViewModel = sessionViewModel,
                                cashViewModel = cashViewModel,
                                saleViewModel = saleViewModel,
                                productViewModel = productViewModel,
                                userViewModel = userViewModel,
                                customerViewModel = customerViewModel,
                                loyaltyConfigViewModel = loyaltyConfigViewModel,
                                saleHistoryViewModel = saleHistoryViewModel,
                                terminalConfigViewModel = terminalConfigViewModel,
                                businessConfigViewModel = businessConfigViewModel,
                                reportsViewModel = reportsViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        // Ícone discreto, sempre visível, sobreposto a
                        // qualquer tela (login, bloqueio, caixa, venda...) —
                        // cicla o tom de azul a cada toque, sem diálogo.
                        // Antes de uma sessão ativa, o toque não tem efeito
                        // (nada pra salvar preferência).
                        IconButton(
                            onClick = sessionViewModel::cycleThemeTone,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(innerPadding)
                                .padding(PdvDimens.SpacingSmall)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = "Trocar tom do tema",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
