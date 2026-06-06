package com.example.nerdvault.api

import com.example.nerdvault.data.*
import com.example.nerdvault.domain.*
import kotlinx.coroutines.flow.first
import java.util.UUID
import java.security.MessageDigest
import kotlin.math.absoluteValue

/**
 * REST Simulation Response returned by the simulated API Gateway.
 */
data class ApiResponse(
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, String>,
    val bodyJson: String,
    val endpoint: String,
    val executionTimeMs: Long
)

/**
 * Unified Backend Engine representing NerdVault's microservices inside a single cohesive system.
 */
class NerdVaultApiEngine(
    private val db: NerdVaultRoomDatabase
) {
    private val userMatcher = "UserEngine"
    private val pricingRules = configureNerdVaultMarketplace {
        platformFee(2.5) // NerdVault fee
        defaultRoyalty(5.0) // Creator royalty
        tokenRate("GOLD", 1.0)
        tokenRate("VBUCKS", 0.75)
        tokenRate("GUILD_COINS", 1.5)
    }

    // Hash generator for audit logging
    private fun signPayload(data: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(data.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            "sig_null_dev_key_" + UUID.randomUUID().toString().take(6)
        }
    }

    /**
     * Publishes and logs a domain event in compliance with Event Sourcing & Auditable Sourced Event requirements.
     */
    private suspend fun publishEvent(
        type: EventType,
        actor: String,
        summary: String,
        payload: String
    ): AuditEventEntity {
        val eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").take(14)
        val timestamp = System.currentTimeMillis()
        val signature = signPayload("$eventId:$type:$timestamp:$actor:$payload")
        
        val eventEntity = AuditEventEntity(
            eventId = eventId,
            type = type.name,
            timestamp = timestamp,
            actorAddress = actor,
            summary = summary,
            payloadJson = payload,
            signature = signature
        )
        db.auditDao().insertAuditLog(eventEntity)
        return eventEntity
    }

    // --- Core Wallet Operations ---

    suspend fun createWallet(username: String, email: String): ApiResponse {
        val start = System.currentTimeMillis()
        val userId = "usr_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val walletAddress = "0x" + UUID.randomUUID().toString().replace("-", "").take(40).lowercase()
        
        val newUser = UserEntity(
            id = userId,
            username = username,
            email = email,
            walletAddress = walletAddress,
            creditsBalance = 1000.0 // Starting bonus
        )
        db.userDao().insertUser(newUser)

        // Mint initial welcome token pass
        val initialAssetId = "nft_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val initialAsset = AssetEntity(
            id = initialAssetId,
            name = "NerdVault Origin Founder Passport",
            description = "Unlocks elite platform benefits and early access.",
            type = AssetType.PASSPORT_TICKET.name,
            balance = 1.0,
            valueInCredits = 250.0,
            imageResUrl = "",
            metadataJson = "{\"founderTier\":\"Gold\",\"issueDate\":\"2026-06-06\"}",
            creatorId = "nerdvault_hq",
            isTransferable = true,
            ownerAddress = walletAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"$walletAddress\"}"
        )
        db.assetDao().insertAsset(initialAsset)

        publishEvent(
            type = EventType.ASSET_MINTED,
            actor = walletAddress,
            summary = "Created wallet for user $username. Minted Origin Passport.",
            payload = "{\"userId\":\"$userId\",\"address\":\"$walletAddress\"}"
        )

        val latency = System.currentTimeMillis() - start
        return ApiResponse(
            statusCode = 201,
            statusMessage = "Created",
            headers = mapOf("Content-Type" to "application/json", "X-Service" to "IdentityWalletEngine"),
            bodyJson = """
                {
                  "status": "success",
                  "userId": "$userId",
                  "walletAddress": "$walletAddress",
                  "creditsBalance": 1000.0,
                  "createdAssets": ["$initialAssetId"]
                }
            """.trimIndent(),
            endpoint = "/wallet/create",
            executionTimeMs = latency
        )
    }

    suspend fun getWallet(address: String): ApiResponse {
        val start = System.currentTimeMillis()
        val user = db.userDao().getPrimaryUserDirect()
        val latency = System.currentTimeMillis() - start
        
        if (user == null || user.walletAddress != address) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Wallet or user with address $address not found\"}", "/wallet/$address", latency)
        }

        return ApiResponse(
            statusCode = 200,
            statusMessage = "OK",
            headers = mapOf("Content-Type" to "application/json"),
            bodyJson = """
                {
                  "userId": "${user.id}",
                  "username": "${user.username}",
                  "email": "${user.email}",
                  "walletAddress": "${user.walletAddress}",
                  "currencies": {
                    "credits": ${user.creditsBalance},
                    "gold": ${user.gamePointsGold},
                    "vBucks": ${user.gamePointsVBucks},
                    "guildCoins": ${user.gamePointsGuild}
                  }
                }
            """.trimIndent(),
            endpoint = "/wallet/$address",
            executionTimeMs = latency
        )
    }

    suspend fun transferAsset(assetId: String, recipientAddress: String): ApiResponse {
        val start = System.currentTimeMillis()
        val asset = db.assetDao().getAssetById(assetId)
        val user = db.userDao().getPrimaryUserDirect()
        
        val latency = System.currentTimeMillis() - start
        if (asset == null || user == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Asset or primary user not found\"}", "/wallet/transfer", latency)
        }
        
        val senderAddress = asset.ownerAddress
        if (senderAddress == recipientAddress) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Cannot transfer to yourself\"}", "/wallet/transfer", latency)
        }

        // Complete Transfer
        val updatedAsset = asset.copy(
            ownerAddress = recipientAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"$recipientAddress\"}"
        )
        db.assetDao().insertAsset(updatedAsset)

        publishEvent(
            type = EventType.ASSET_TRANSFERRED,
            actor = senderAddress,
            summary = "Transferred asset [${asset.name}] to $recipientAddress.",
            payload = "{\"assetId\":\"$assetId\",\"sender\":\"$senderAddress\",\"recipient\":\"$recipientAddress\"}"
        )

        return ApiResponse(
            200, "OK", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "txHash": "${UUID.randomUUID().toString().replace("-","").take(24)}",
                  "assetId": "$assetId",
                  "sender": "$senderAddress",
                  "recipient": "$recipientAddress"
                }
            """.trimIndent(),
            "/wallet/transfer",
            System.currentTimeMillis() - start
        )
    }

    suspend fun mintAsset(name: String, description: String, typeString: String, value: Double, owner: String, metadata: Map<String, String>): ApiResponse {
        val start = System.currentTimeMillis()
        val assetId = "nft_" + UUID.randomUUID().toString().replace("-", "").take(8)
        
        val newAsset = AssetEntity(
            id = assetId,
            name = name,
            description = description,
            type = typeString,
            balance = 1.0,
            valueInCredits = value,
            imageResUrl = "",
            metadataJson = metadata.entries.joinToString(",", "{", "}") { "\"${it.key}\":\"${it.value}\"" },
            creatorId = "nerdvault_creator_platform",
            isTransferable = true,
            ownerAddress = owner,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"$owner\"}"
        )
        db.assetDao().insertAsset(newAsset)

        publishEvent(
            type = EventType.ASSET_MINTED,
            actor = owner,
            summary = "Minted brand new asset: [${name}] to wallet $owner.",
            payload = "{\"assetId\":\"$assetId\",\"balance\":1.0,\"valueCredits\":$value}"
        )

        return ApiResponse(
            201, "Created", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "assetId": "$assetId",
                  "ownerAddress": "$owner",
                  "name": "$name"
                }
            """.trimIndent(),
            "/assets/mint",
            System.currentTimeMillis() - start
        )
    }

    // --- Marketplace Operations ---

    suspend fun listAssetMarketplace(assetId: String, price: Double): ApiResponse {
        val start = System.currentTimeMillis()
        val asset = db.assetDao().getAssetById(assetId)
        val latency = System.currentTimeMillis() - start
        if (asset == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Asset $assetId not found\"}", "/market/list", latency)
        }

        // Update state in database
        val listedStateJson = "{\"ownerAddress\":\"${asset.ownerAddress}\",\"listPrice\":$price}"
        val updatedAsset = asset.copy(
            stateType = "LISTED",
            stateJson = listedStateJson
        )
        db.assetDao().insertAsset(updatedAsset)

        // Create Marketplace listing
        val listingId = "lst_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val listing = MarketListingEntity(
            id = listingId,
            assetId = asset.id,
            assetName = asset.name,
            assetType = asset.type,
            sellerAddress = asset.ownerAddress,
            sellPrice = price,
            isAuction = false,
            highestBid = 0.0,
            highestBidderAddress = null,
            expiresAt = 0L,
            isCompleted = false
        )
        db.marketDao().insertListing(listing)

        publishEvent(
            type = EventType.ASSET_TRANSFERRED, // Transfer to marketplace catalog escrow
            actor = asset.ownerAddress,
            summary = "Listed asset [${asset.name}] for $price CREDITS.",
            payload = "{\"listingId\":\"$listingId\",\"assetId\":\"$assetId\",\"price\":$price}"
        )

        return ApiResponse(
            200, "OK", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "listingId": "$listingId",
                  "assetId": "$assetId",
                  "priceInCredits": $price
                }
            """.trimIndent(),
            "/market/list",
            System.currentTimeMillis() - start
        )
    }

    suspend fun buyProduct(listingId: String): ApiResponse {
        val start = System.currentTimeMillis()
        val listing = db.marketDao().getListingById(listingId)
        val buyer = db.userDao().getPrimaryUserDirect()
        
        val latency = System.currentTimeMillis() - start
        if (listing == null || buyer == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Listing or buyer not found\"}", "/market/buy", latency)
        }

        if (buyer.walletAddress == listing.sellerAddress) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Cannot purchase your own asset listing\"}", "/market/buy", latency)
        }

        val price = listing.sellPrice
        if (buyer.creditsBalance < price) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Insufficient balances. Needed: $price CREDITS, Had: ${buyer.creditsBalance}\"}", "/market/buy", latency)
        }

        val asset = db.assetDao().getAssetById(listing.assetId)
        if (asset == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Underlying asset not found\"}", "/market/buy", latency)
        }

        // Deduct/Add balances
        val updatedBuyer = buyer.copy(creditsBalance = buyer.creditsBalance - price, xp = buyer.xp + 50)
        db.userDao().insertUser(updatedBuyer)

        // Complete Transfer ownership
        val updatedAsset = asset.copy(
            ownerAddress = buyer.walletAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"${buyer.walletAddress}\"}"
        )
        db.assetDao().insertAsset(updatedAsset)

        // Close catalog listing
        val updatedListing = listing.copy(isCompleted = true)
        db.marketDao().insertListing(updatedListing)

        // Creator Royalties and fees allocation
        val royaltyPercentage = pricingRules.creatorRoyaltyPercentageDefault
        val platformFeePercentage = pricingRules.platformFeePercentage
        
        val royaltyCredits = price * (royaltyPercentage / 100.0)
        val platformCredits = price * (platformFeePercentage / 100.0)
        val sellerProceeds = price - royaltyCredits - platformCredits

        // If listing asset has a creator registered, credit their database profile
        if (!asset.creatorId.isNullOrEmpty()) {
            val cr = db.creatorDao().getCreatorById(asset.creatorId)
            if (cr != null) {
                db.creatorDao().updateCreator(cr.copy(accumulatedEarnings = cr.accumulatedEarnings + royaltyCredits))
                publishEvent(
                    type = EventType.CREATOR_REVENUE_SHARED,
                    actor = buyer.walletAddress,
                    summary = "Shared royalty $royaltyCredits CREDITS to Creator [${cr.name}]",
                    payload = "{\"creatorId\":\"${cr.id}\",\"credits\":$royaltyCredits}"
                )
            }
        }

        publishEvent(
            type = EventType.ORDER_COMPLETED,
            actor = buyer.walletAddress,
            summary = "Market purchase: [${asset.name}] completed.",
            payload = "{\"listingId\":\"$listingId\",\"sellerAddress\":\"${listing.sellerAddress}\",\"buyerAddress\":\"${buyer.walletAddress}\",\"price\":$price,\"sellerShare\":$sellerProceeds,\"royalty\":$royaltyCredits,\"platformFee\":$platformCredits}"
        )

        return ApiResponse(
            200, "OK", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "receipt": {
                    "assetId": "${asset.id}",
                    "assetName": "${asset.name}",
                    "totalPaid": $price,
                    "platformFeePaid": $platformCredits,
                    "royaltyPaid": $royaltyCredits,
                    "recipientProceeds": $sellerProceeds
                  }
                }
            """.trimIndent(),
            "/market/buy",
            System.currentTimeMillis() - start
        )
    }

    // --- Merch Commerce Operations ---

    suspend fun checkoutMerch(itemId: String): ApiResponse {
        val start = System.currentTimeMillis()
        val item = db.merchDao().getMerchItemById(itemId)
        val user = db.userDao().getPrimaryUserDirect()
        val latency = System.currentTimeMillis() - start
        if (item == null || user == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Merch item or profile user not found\"}", "/store/checkout", latency)
        }

        if (item.stockCount <= 0) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Item ${item.name} is out of stock!\"}", "/store/checkout", latency)
        }

        if (user.creditsBalance < item.price) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Insufficient credits balance. Needed: ${item.price}, Had: ${user.creditsBalance}\"}", "/store/checkout", latency)
        }

        // Deduct stock, deduct balance
        db.merchDao().updateMerchItem(item.copy(stockCount = item.stockCount - 1))
        db.userDao().insertUser(user.copy(creditsBalance = user.creditsBalance - item.price, xp = user.xp + 100))

        // Mint dynamic physical merch voucher/NFT onto wallet as receipt
        val receiptId = "nft_vouch_" + UUID.randomUUID().toString().replace("-", "").take(6)
        val voucher = AssetEntity(
            id = receiptId,
            name = "Physical Redeemable: ${item.name}",
            description = "Physical cargo voucher receipt. Claimable by shipping engine.",
            type = AssetType.MERCH.name,
            balance = 1.0,
            valueInCredits = item.price,
            imageResUrl = item.imageUrl,
            metadataJson = "{\"orderId\":\"ord_${UUID.randomUUID().toString().take(8)}\",\"category\":\"${item.category}\"}",
            creatorId = "nerdvault_warehouse",
            isTransferable = false,
            ownerAddress = user.walletAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"${user.walletAddress}\"}"
        )
        db.assetDao().insertAsset(voucher)

        publishEvent(
            type = EventType.ORDER_COMPLETED,
            actor = user.walletAddress,
            summary = "Checked out merch catalog item: [${item.name}]. Voucher asset generated.",
            payload = "{\"id\":\"${item.id}\",\"price\":${item.price},\"voucherAssetId\":\"$receiptId\"}"
        )

        return ApiResponse(
            200, "OK", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "orderId": "ord_${UUID.randomUUID().toString().take(12)}",
                  "itemPurchased": "${item.name}",
                  "costCredits": ${item.price},
                  "generatedVoucherId": "$receiptId"
                }
            """.trimIndent(),
            "/store/checkout",
            System.currentTimeMillis() - start
        )
    }

    // --- Ticket & Pass Operations ---

    suspend fun mintTicket(eventId: String): ApiResponse {
        val start = System.currentTimeMillis()
        val event = db.eventPassDao().getEventPassById(eventId)
        val user = db.userDao().getPrimaryUserDirect()
        val latency = System.currentTimeMillis() - start
        if (event == null || user == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Event or user profile not found\"}", "/tickets/mint", latency)
        }

        if (event.ticketsSoldCount >= event.totalTickets) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Event is sold out!\"}", "/tickets/mint", latency)
        }

        if (user.creditsBalance < event.ticketPrice) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Insufficient balances to buy ticket for ${event.eventName}\"}", "/tickets/mint", latency)
        }

        // Deduct credits, update sold count
        db.userDao().insertUser(user.copy(creditsBalance = user.creditsBalance - event.ticketPrice, xp = user.xp + 75))
        db.eventPassDao().updateEventPass(event.copy(ticketsSoldCount = event.ticketsSoldCount + 1))

        // Mint Ticket Asset onto wallet
        val ticketId = "pass_tkt_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val ticketAsset = AssetEntity(
            id = ticketId,
            name = "Ticket: ${event.eventName}",
            description = "Official entry pass to the event. Scan the QR code to validate.",
            type = AssetType.PASSPORT_TICKET.name,
            balance = 1.0,
            valueInCredits = event.ticketPrice,
            imageResUrl = event.imageUrl,
            metadataJson = "{\"eventId\":\"${event.id}\",\"location\":\"${event.location}\",\"eventDate\":\"${event.dateString}\"}",
            creatorId = "nerdvault_box_office",
            isTransferable = true,
            ownerAddress = user.walletAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"${user.walletAddress}\"}"
        )
        db.assetDao().insertAsset(ticketAsset)

        publishEvent(
            type = EventType.ASSET_MINTED,
            actor = user.walletAddress,
            summary = "Purchased entry ticket [Ticket: ${event.eventName}]. Ticket Asset mint: $ticketId.",
            payload = "{\"eventId\":\"$eventId\",\"ticketId\":\"$ticketId\",\"price\":${event.ticketPrice}}"
        )

        return ApiResponse(
            201, "Created", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "ticketAssetId": "$ticketId",
                  "eventName": "${event.eventName}",
                  "eventDate": "${event.dateString}",
                  "pricePaidCredits": ${event.ticketPrice}
                }
            """.trimIndent(),
            "/tickets/mint",
            System.currentTimeMillis() - start
        )
    }

    suspend fun validateTicket(ticketId: String): ApiResponse {
        val start = System.currentTimeMillis()
        val asset = db.assetDao().getAssetById(ticketId)
        val latency = System.currentTimeMillis() - start
        if (asset == null || asset.type != AssetType.PASSPORT_TICKET.name) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Valid entry pass with ID $ticketId not found\"}", "/tickets/validate", latency)
        }

        if (asset.stateType == "REDEEMED") {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"This ticket has already been validated and redeemed at [${asset.stateJson}]\"}", "/tickets/validate", latency)
        }

        // Complete Ticket redemption state machine transition: OWNED -> REDEEMED
        val updatedAsset = asset.copy(
            stateType = "REDEEMED",
            stateJson = "{\"redeemedAt\":${System.currentTimeMillis()},\"validatedBy\":\"NERDVAULT_GATE_ACCESS_A\"}"
        )
        db.assetDao().insertAsset(updatedAsset)

        publishEvent(
            type = EventType.TICKET_REDEEMED,
            actor = asset.ownerAddress,
            summary = "Redeemed ticket entry: [${asset.name}]. Welcome to the venue!",
            payload = "{\"ticketId\":\"$ticketId\",\"redeemedTimestamp\":${System.currentTimeMillis()}}"
        )

        return ApiResponse(
            200, "OK", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "redeemed": true,
                  "message": "Access GRANTED for entry ticket: [${asset.name}]",
                  "redemptionLogId": "gate_log_${UUID.randomUUID().toString().take(10)}"
                }
            """.trimIndent(),
            "/tickets/validate",
            System.currentTimeMillis() - start
        )
    }

    // --- Creator Economy Operations ---

    suspend fun subscribeToCreator(creatorId: String): ApiResponse {
        val start = System.currentTimeMillis()
        val creator = db.creatorDao().getCreatorById(creatorId)
        val user = db.userDao().getPrimaryUserDirect()
        val latency = System.currentTimeMillis() - start
        
        if (creator == null || user == null) {
            return ApiResponse(404, "Not Found", emptyMap(), "{\"error\":\"Creator $creatorId or user profile not found\"}", "/fan/subscription", latency)
        }

        val price = 150.0 // Creator sub standard credit cost
        if (user.creditsBalance < price) {
            return ApiResponse(400, "Bad Request", emptyMap(), "{\"error\":\"Insufficient balances. Subscribing costs 150 Credits, but wallet had: ${user.creditsBalance}\"}", "/fan/subscription", latency)
        }

        // Deduct credits from user
        db.userDao().insertUser(user.copy(creditsBalance = user.creditsBalance - price, xp = user.xp + 150))

        // Split revenue: 85% to Creator, 15% to Platform commission as requested
        val creatorCut = price * 0.85
        val platformCut = price * 0.15

        db.creatorDao().updateCreator(creator.copy(
            totalSubscribers = creator.totalSubscribers + 1,
            accumulatedEarnings = creator.accumulatedEarnings + creatorCut
        ))

        // Mint membership card asset onto wallet
        val subAssetId = "sub_p_card_" + UUID.randomUUID().toString().replace("-", "").take(8)
        val membershipCard = AssetEntity(
            id = subAssetId,
            name = "Creator Pass: ${creator.name}",
            description = "Golden pass tier unlocking premium creator channels, early streams, and badges.",
            type = AssetType.CREATOR_MEMBERSHIP.name,
            balance = 1.0,
            valueInCredits = price,
            imageResUrl = creator.avatarUrl,
            metadataJson = "{\"creatorId\":\"$creatorId\",\"fanBadgeTier\":\"Gold\",\"activeSince\":\"2026-06-06\"}",
            creatorId = creatorId,
            isTransferable = false,
            ownerAddress = user.walletAddress,
            stateType = "OWNED",
            stateJson = "{\"ownerAddress\":\"${user.walletAddress}\"}"
        )
        db.assetDao().insertAsset(membershipCard)

        publishEvent(
            type = EventType.CREATOR_REVENUE_SHARED,
            actor = user.walletAddress,
            summary = "Subscribed to Creator [${creator.name}]. Divided 85/15 revenue ratio.",
            payload = "{\"creatorId\":\"$creatorId\",\"costCredits\":$price,\"creatorShare\":$creatorCut,\"systemCommission\":$platformCut,\"fandomName\":\"${creator.name}\"}"
        )

        return ApiResponse(
            201, "Created", mapOf("Content-Type" to "application/json"),
            """
                {
                  "status": "success",
                  "subscribed": true,
                  "fandomPassportId": "$subAssetId",
                  "creatorDetails": {
                    "fandomName": "${creator.name}",
                    "revenueAllocatedToCreator": $creatorCut,
                    "platformTaxCollected": $platformCut
                  }
                }
            """.trimIndent(),
            "/fan/subscription",
            System.currentTimeMillis() - start
        )
    }

    // --- Dynamic Recommendations ---

    suspend fun getRecommendations(): List<Asset> {
        val user = db.userDao().getPrimaryUserDirect() ?: return emptyList()
        val allAssets = db.assetDao().getAllAssetsFlow().first()
        
        // High quality local recommendations engine matching user stats/curating
        // Checks how many credits they have. If high credits -> recommend merch. If low -> suggestions to earn.
        val recs = mutableListOf<Asset>()
        
        // Generate virtual recommendations dynamically from our local assets
        if (user.creditsBalance > 500) {
            recs.add(
                Asset(
                    id = "rec_merch",
                    name = "Custom NerdVault Retro Artisan Keycap",
                    description = "Limited run mechanical keycap. Exclusive drop.",
                    type = AssetType.MERCH,
                    balance = 1.0,
                    valueInCredits = 350.0,
                    creatorId = "nerdvault_hq"
                )
            )
        }
        
        recs.add(
            Asset(
                id = "rec_pass",
                name = "Esports Grand Finals Access VIP Ticket",
                description = "Secure prime seats for Season 1 Playoffs championship match.",
                type = AssetType.PASSPORT_TICKET,
                balance = 1.0,
                valueInCredits = 180.0,
                creatorId = "nerdvault_arena"
            )
        )

        recs.add(
            Asset(
                id = "rec_creator",
                name = "Viper streamer Club Pass Edition",
                description = "Elevate to Golden tier on Apex Legends creator ecosystem.",
                type = AssetType.CREATOR_MEMBERSHIP,
                balance = 1.0,
                valueInCredits = 150.0,
                creatorId = "cre_viper_ape"
            )
        )
        
        return recs
    }

    // --- API Gate Dispatcher ---

    suspend fun handleRequest(route: String, method: String, body: String): ApiResponse {
        val start = System.currentTimeMillis()
        return try {
            when {
                // Route: POST /wallet/create
                route == "/wallet/create" && method == "POST" -> {
                    // Extract placeholder inputs or fallback to defaults
                    val username = extractJsonStringField(body, "username") ?: "NerdWielder"
                    val email = extractJsonStringField(body, "email") ?: "user@ahyx.org"
                    createWallet(username, email)
                }

                // Route: POST /wallet/transfer
                route == "/wallet/transfer" && method == "POST" -> {
                    val assetId = extractJsonStringField(body, "assetId") ?: ""
                    val recipient = extractJsonStringField(body, "recipient") ?: ""
                    transferAsset(assetId, recipient)
                }

                // Route: POST /assets/mint
                route == "/assets/mint" && method == "POST" -> {
                    val name = extractJsonStringField(body, "name") ?: "New Collectible"
                    val desc = extractJsonStringField(body, "description") ?: "Imported digital collectible NFT."
                    val assetTypeSec = extractJsonStringField(body, "type") ?: "COSMETIC"
                    val valCred = extractJsonDoubleField(body, "value") ?: 50.0
                    val owner = extractJsonStringField(body, "owner") ?: db.userDao().getPrimaryUserDirect()?.walletAddress ?: "0x_anonymous"
                    mintAsset(name, desc, assetTypeSec, valCred, owner, mapOf("source" to "api_mint"))
                }

                // Route: POST /market/list
                route == "/market/list" && method == "POST" -> {
                    val assetId = extractJsonStringField(body, "assetId") ?: ""
                    val price = extractJsonDoubleField(body, "price") ?: 100.0
                    listAssetMarketplace(assetId, price)
                }

                // Route: POST /market/buy
                route == "/market/buy" && method == "POST" -> {
                    val listingId = extractJsonStringField(body, "listingId") ?: ""
                    buyProduct(listingId)
                }

                // Route: POST /store/checkout
                route == "/store/checkout" && method == "POST" -> {
                    val itemId = extractJsonStringField(body, "itemId") ?: ""
                    checkoutMerch(itemId)
                }

                // Route: POST /tickets/mint
                route == "/tickets/mint" && method == "POST" -> {
                    val eventId = extractJsonStringField(body, "eventId") ?: ""
                    mintTicket(eventId)
                }

                // Route: POST /tickets/validate
                route == "/tickets/validate" && method == "POST" -> {
                    val ticketId = extractJsonStringField(body, "ticketId") ?: ""
                    validateTicket(ticketId)
                }

                // Route: POST /fan/subscription
                route == "/fan/subscription" && method == "POST" -> {
                    val creatorId = extractJsonStringField(body, "creatorId") ?: ""
                    subscribeToCreator(creatorId)
                }

                // Route: GET /wallet/{id}
                route.startsWith("/wallet/") && method == "GET" -> {
                    val walletIdSec = route.substringAfter("/wallet/")
                    getWallet(walletIdSec)
                }

                else -> {
                    ApiResponse(
                        statusCode = 404,
                        statusMessage = "Not Found",
                        headers = mapOf("X-System" to "GatewayRouter"),
                        bodyJson = "{\"error\":\"Route $method $route is unrecognized\"}",
                        endpoint = route,
                        executionTimeMs = System.currentTimeMillis() - start
                    )
                }
            }
        } catch (e: Exception) {
            ApiResponse(
                statusCode = 500,
                statusMessage = "Internal Server Error",
                headers = mapOf("X-System" to "GatewayRouter"),
                bodyJson = "{\"error\":\"${e.message}\"}",
                endpoint = route,
                executionTimeMs = System.currentTimeMillis() - start
            )
        }
    }

    private fun extractJsonStringField(json: String, field: String): String? {
        val pattern = "\"$field\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
    }

    private fun extractJsonDoubleField(json: String, field: String): Double? {
        val pattern = "\"$field\"\\s*:\\s*([0-9.]+)".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
