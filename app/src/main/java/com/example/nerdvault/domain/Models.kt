package com.example.nerdvault.domain

/**
 * Asset types representing all programmable fandom values.
 */
enum class AssetType {
    TOKEN,
    COSMETIC,
    MERCH,
    PASSPORT_TICKET,
    CREATOR_MEMBERSHIP
}

/**
 * Sealed class for asset state machines as requested:
 * STATE CONTRACT: OWNED -> LISTED -> TRANSFER_PENDING -> REDEEMED / EXPIRED
 */
sealed class AssetState {
    data class Owned(val ownerAddress: String) : AssetState()
    
    data class Listed(val ownerAddress: String, val listingId: String, val listPrice: Double) : AssetState()
    
    data class TransferPending(val sender: String, val recipient: String, val holdingTxId: String) : AssetState()
    
    data class Redeemed(val redeemerAddress: String, val timestamp: Long) : AssetState()
    
    data class Expired(val reason: String) : AssetState()

    val label: String
        get() = when (this) {
            is Owned -> "OWNED"
            is Listed -> "LISTED"
            is TransferPending -> "TRANSFER_PENDING"
            is Redeemed -> "REDEEMED"
            is Expired -> "EXPIRED"
        }
}

/**
 * Unified model for NerdVault Asset (Physical / Digital / Access).
 * Schema: "Asset = anything with value, ownership, and transferability"
 */
data class Asset(
    val id: String,
    val name: String,
    val description: String,
    val type: AssetType,
    val balance: Double = 1.0, // For tokens or quantity
    val valueInCredits: Double = 0.0, // In NerdVault system credits
    val imageResUrl: String = "", // Display placeholder
    val metadata: Map<String, String> = emptyMap(),
    val creatorId: String? = null,
    val isTransferable: Boolean = true
)

/**
 * Audit Events for Event-Sourced transaction logs.
 */
enum class EventType {
    ASSET_MINTED,
    ASSET_TRANSFERRED,
    ORDER_COMPLETED,
    TICKET_REDEEMED,
    TOKEN_EARNED,
    CREATOR_REVENUE_SHARED,
    BID_PLACED
}

data class DomainEvent(
    val eventId: String,
    val type: EventType,
    val timestamp: Long = System.currentTimeMillis(),
    val actorAddress: String,
    val summary: String,
    val payloadJson: String,
    val signature: String // Cryptographically signed log simulator
)

/**
 * Config DSL style marketplace pricing and system structures.
 */
class MarketPricingDsl {
    var platformFeePercentage: Double = 2.5
    var creatorRoyaltyPercentageDefault: Double = 5.0
    var currencyExchangeRates: MutableMap<String, Double> = mutableMapOf(
        "GOLD" to 1.0,
        "VBUCKS" to 0.75,
        "GUILD_COINS" to 1.5,
        "CREDITS" to 1.0
    )
    
    fun platformFee(percentage: Double) {
        platformFeePercentage = percentage
    }
    
    fun defaultRoyalty(percentage: Double) {
        creatorRoyaltyPercentageDefault = percentage
    }
    
    fun tokenRate(symbol: String, rateInCredits: Double) {
        currencyExchangeRates[symbol] = rateInCredits
    }
}

fun configureNerdVaultMarketplace(init: MarketPricingDsl.() -> Unit): MarketPricingDsl {
    val dsl = MarketPricingDsl()
    dsl.init()
    return dsl
}
