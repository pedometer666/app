package com.pedometer.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pedometer.app.R
import com.pedometer.app.data.billing.PremiumUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSheet(
    uiState: PremiumUiState,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(R.string.premium_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.premium_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.premium_feature_background))
            Text(stringResource(R.string.premium_feature_homebase))
            Text(stringResource(R.string.premium_feature_export))
            Text(stringResource(R.string.premium_feature_no_ads))
            Spacer(Modifier.height(16.dp))

            if (uiState.isPremium) {
                Text(
                    text = stringResource(R.string.premium_active),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Button(
                    onClick = onPurchase,
                    enabled = !uiState.isLoading && uiState.billingReady &&
                        uiState.productAvailable,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when {
                            uiState.isLoading -> stringResource(R.string.premium_loading)
                            uiState.price != null ->
                                stringResource(R.string.premium_buy_price, uiState.price)
                            else -> stringResource(R.string.premium_buy)
                        }
                    )
                }
                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.premium_restore))
                }
            }

            uiState.message?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.premium_lifetime_notice),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
