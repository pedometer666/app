package com.pedometer.app.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException

data class PremiumUiState(
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val billingReady: Boolean = false,
    val productAvailable: Boolean = false,
    val price: String? = null,
    val message: String? = null
)

private val Context.premiumDataStore by preferencesDataStore(name = "premium_entitlement")

class PremiumBillingRepository(context: Context) : PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val entitlementStore = appContext.premiumDataStore
    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    @Volatile
    private var productDetails: ProductDetails? = null
    @Volatile
    private var selectedOfferToken: String? = null
    @Volatile
    private var connectionInProgress = false
    private val billingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    @Synchronized
    fun connect() {
        if (billingClient.isReady) {
            refreshPurchases()
            return
        }
        if (connectionInProgress) return
        connectionInProgress = true
        scope.launch {
            try {
                val savedEntitlement = entitlementStore.data.first()[PREMIUM_ENTITLED] ?: false
                _uiState.value = _uiState.value.copy(isPremium = savedEntitlement)
                if (billingClient.isReady) {
                    connectionInProgress = false
                    refreshPurchases()
                    return@launch
                }
                startBillingConnection()
            } catch (exception: IOException) {
                connectionInProgress = false
                Log.e(TAG, "Could not read the saved Premium entitlement", exception)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Could not load Premium status. Please reopen the app and try again."
                )
            }
        }
    }

    private fun startBillingConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionInProgress = false
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = result.debugMessage
                    )
                    return
                }
                _uiState.value = _uiState.value.copy(billingReady = true, message = null)
                queryProduct()
                refreshPurchases()
            }

            override fun onBillingServiceDisconnected() {
                connectionInProgress = false
                _uiState.value = _uiState.value.copy(
                    billingReady = false,
                    isLoading = false,
                    message = "Google Play billing disconnected. Try again when online."
                )
            }
        })
    }

    fun purchase(activity: Activity) {
        val details = productDetails
        if (!billingClient.isReady || details == null) {
            _uiState.value = _uiState.value.copy(
                message = "Premium is not available from Google Play right now. Please try again."
            )
            connect()
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        selectedOfferToken?.let(productParams::setOfferToken)
        val params = productParams.build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _uiState.value = _uiState.value.copy(message = result.debugMessage)
        }
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        refreshPurchases()
    }

    suspend fun hasStoredEntitlement(): Boolean =
        entitlementStore.data.first()[PREMIUM_ENTITLED] ?: false

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                processPurchases(purchases.orEmpty())
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _uiState.value = _uiState.value.copy(message = null, isLoading = false)
            }
            else -> {
                _uiState.value = _uiState.value.copy(
                    message = result.debugMessage,
                    isLoading = false
                )
            }
        }
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList.firstOrNull()
                val offer = productDetails?.oneTimePurchaseOfferDetails
                    ?: productDetails?.oneTimePurchaseOfferDetailsList?.firstOrNull()
                selectedOfferToken = offer?.offerToken
                _uiState.value = _uiState.value.copy(
                    productAvailable = productDetails != null && offer != null,
                    price = offer?.formattedPrice,
                    message = if (productDetails == null || offer == null) {
                        "The Premium product is not available in Google Play yet."
                    } else {
                        null
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(message = result.debugMessage)
            }
        }
    }

    private fun refreshPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = result.debugMessage
                )
                return@queryPurchasesAsync
            }
            processPurchases(purchases)
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchase = purchases.firstOrNull {
            PREMIUM_PRODUCT_ID in it.products &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (premiumPurchase == null) {
            val isPending = purchases.any {
                PREMIUM_PRODUCT_ID in it.products &&
                    it.purchaseState == Purchase.PurchaseState.PENDING
            }
            if (isPending) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Your purchase is pending with Google Play."
                )
            } else {
                persistEntitlement(false)
            }
            return
        }

        if (premiumPurchase.isAcknowledged) {
            persistEntitlement(true)
        } else {
            val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(premiumPurchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    persistEntitlement(true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Purchase found, but Google Play could not confirm it yet: ${result.debugMessage}"
                    )
                }
            }
        }
    }

    private fun persistEntitlement(entitled: Boolean) {
        scope.launch {
            try {
                entitlementStore.edit { preferences ->
                    preferences[PREMIUM_ENTITLED] = entitled
                }
                _uiState.value = _uiState.value.copy(
                    isPremium = entitled,
                    isLoading = false,
                    message = if (entitled) null else _uiState.value.message
                )
            } catch (exception: IOException) {
                Log.e(TAG, "Could not save Premium entitlement", exception)
                _uiState.value = _uiState.value.copy(
                    isPremium = entitled,
                    isLoading = false,
                    message = "Could not save Premium status on this device. Please reopen the app and restore purchases."
                )
            }
        }
    }

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_unlock"
        private const val TAG = "PremiumBilling"
        private val PREMIUM_ENTITLED = booleanPreferencesKey("premium_entitled")
    }
}
