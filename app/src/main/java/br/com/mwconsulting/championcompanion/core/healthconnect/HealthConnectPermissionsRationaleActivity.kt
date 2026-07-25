package br.com.mwconsulting.championcompanion.core.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.mwconsulting.championcompanion.core.designsystem.ChampionCompanionTheme

class HealthConnectPermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChampionCompanionTheme {
                HealthConnectPermissionsRationaleScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HealthConnectPermissionsRationaleScreen(
    onClose: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Privacidade do Health Connect") })
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RationaleSection(
                    title = "O que o app envia",
                    body = "O Champion Companion pode enviar passos, frequência cardíaca, sessões de exercício, distância, calorias, peso e altura quando esses dados já existirem localmente e a capacidade correspondente estiver autorizada.",
                )
                RationaleSection(
                    title = "Por que envia",
                    body = "A integração existe para publicar no Health Connect os dados coletados e revisados no Champion Companion, sem substituir o banco local como fonte principal da experiência.",
                )
                RationaleSection(
                    title = "O que não envia",
                    body = "Esta fase não envia rota, latitude, longitude, mapas, GPX, TCX, FIT nem outros dados de localização. O app também não importa dados de outros aplicativos pelo Health Connect.",
                )
                RationaleSection(
                    title = "Como pausar e gerenciar",
                    body = "A sincronização pode ser pausada nas Configurações do app. O acesso ao provider também pode ser gerenciado diretamente pela tela de permissões do Health Connect.",
                )
                RationaleSection(
                    title = "Como os dados são tratados",
                    body = "O Champion Companion continua local-first: falhas do provider não apagam dados locais persistidos. Logs diagnósticos não devem expor valores de saúde, credenciais nem informações de rota.",
                )
                Button(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Fechar")
                }
            }
        }
    }
}

@Composable
private fun RationaleSection(
    title: String,
    body: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
