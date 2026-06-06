package com.example.nerdvault.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.nerdvault.api.ApiResponse
import com.example.nerdvault.api.NerdVaultApiEngine
import com.example.nerdvault.data.*
import com.example.nerdvault.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class NerdVaultViewModel(application: Application) : AndroidViewModel(application) {

    // --- Database Initialization ---
    val database: NerdVaultRoomDatabase = Room.databaseBuilder(
        application,
        NerdVaultRoomDatabase::class.java,
        "nerdvault_db"
    )
    .fallbackToDestructiveMigration()
    .build()

    val apiEngine = NerdVaultApiEngine(database)

    // --- Exposed Reactive States ---
    val currentUserState: StateFlow<UserEntity?> = database.userDao().getPrimaryUserFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val assetsState: StateFlow<List<AssetEntity>> = database.assetDao().getAllAssetsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeListingsState: StateFlow<List<MarketListingEntity>> = database.marketDao().getActiveListingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val creatorsState: StateFlow<List<CreatorEntity>> = database.creatorDao().getAllCreatorsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val merchCatalogState: StateFlow<List<MerchCatalogEntity>> = database.merchDao().getMerchCatalogFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventPassesState: StateFlow<List<EventPassEntity>> = database.eventPassDao().getAllEventPassesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogState: StateFlow<List<AuditEventEntity>> = database.auditDao().getAuditTrailFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sandbox Console State
    private val _sandboxResponse = MutableStateFlow<ApiResponse?>(null)
    val sandboxResponse: StateFlow<ApiResponse?> = _sandboxResponse.asStateFlow()

    private val _recommendations = MutableStateFlow<List<Asset>>(emptyList())
    val recommendations: StateFlow<List<Asset>> = _recommendations.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    init {
        // Seed default database states on first launch
        viewModelScope.launch(Dispatchers.IO) {
            seedDatabaseIfEmpty()
            refreshRecommendations()
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val userCount = database.userDao().getPrimaryUserDirect()
        if (userCount == null) {
            // 1. Seed primary user
            val walletAddress = "0x89fab02bca476686121b6d1a938c5f3e990bff3a"
            val defaultUser = UserEntity(
                id = "usr_origin_101",
                username = "NerdElite_07",
                email = "shroud_clone@nerdvault.gg",
                walletAddress = walletAddress,
                creditsBalance = 2450.0,
                xp = 6400,
                gamePointsGold = 12500.0,
                gamePointsVBucks = 4800.0,
                gamePointsGuild = 980.0
            )
            database.userDao().insertUser(defaultUser)

            // 2. Seed catalog assets owned by this user
            val startingAssets = listOf(
                AssetEntity(
                    id = "nft_cyb_skn_01",
                    name = "Viper Cyberpunk Neon Skin",
                    description = "Ultra high-rarity digital cosmetic skin. Compatible with Apex Ecosystem.",
                    type = AssetType.COSMETIC.name,
                    balance = 1.0,
                    valueInCredits = 650.0,
                    imageResUrl = "",
                    metadataJson = "{\"rarity\":\"Legendary\",\"itemSlot\":\"Weapon\",\"game\":\"Apex Legends\"}",
                    creatorId = "cre_viper_ape",
                    isTransferable = true,
                    ownerAddress = walletAddress,
                    stateType = "OWNED",
                    stateJson = "{\"ownerAddress\":\"$walletAddress\"}"
                ),
                AssetEntity(
                    id = "nft_guild_crest",
                    name = "Mithril Guild Crest Badge",
                    description = "Guild achievement badge signaling alpha access and passive gold drops.",
                    type = AssetType.COSMETIC.name,
                    balance = 1.0,
                    valueInCredits = 300.0,
                    imageResUrl = "",
                    metadataJson = "{\"guildId\":\"MITH_RAID\",\"rank\":\"Officer\",\"forgedDate\":\"2026-05\"}",
                    creatorId = "nerdvault_hq",
                    isTransferable = true,
                    ownerAddress = walletAddress,
                    stateType = "OWNED",
                    stateJson = "{\"ownerAddress\":\"$walletAddress\"}"
                )
            )
            database.assetDao().insertAssets(startingAssets)

            // 3. Seed Creators
            val starterCreators = listOf(
                CreatorEntity(
                    id = "cre_viper_ape",
                    name = "VesperViper (Apex Streamer)",
                    email = "viper@twitch.tv",
                    walletAddress = "0x44bfbc690cd42ea2d6f21c5c0a32194b1a89c92b",
                    revenueSharePercentage = 85.0,
                    accumulatedEarnings = 12540.0,
                    totalSubscribers = 1042,
                    fandomTags = "Apex Legends,FPS,Tours",
                    avatarUrl = ""
                ),
                CreatorEntity(
                    id = "cre_relic_painter",
                    name = "LoreForge (Figurine Designer)",
                    email = "loreforge@patreon.com",
                    walletAddress = "0x539bbfa602e1c2eae3d6ec7c7c32b5ca8179da2e",
                    revenueSharePercentage = 85.0,
                    accumulatedEarnings = 8420.0,
                    totalSubscribers = 512,
                    fandomTags = "Custom Paintings,RPG,Miniatures",
                    avatarUrl = ""
                )
            )
            database.creatorDao().insertCreators(starterCreators)

            // 4. Seed Merchandise Catalog
            val starterMerch = listOf(
                MerchCatalogEntity(
                    id = "merch_art_keycap",
                    name = "Titan Obsidian Artisan Keycap",
                    description = "Hand-forged premium resin mechanical keycap. Outlined in green carbon.",
                    price = 120.0,
                    stockCount = 24,
                    category = "PERIPHERALS",
                    imageUrl = ""
                ),
                MerchCatalogEntity(
                    id = "merch_retro_hoodie",
                    name = "NerdVault Retro Oversized Hoodie",
                    description = "Double-stitched premium heavyweight cotton. High contrast chest logo.",
                    price = 280.0,
                    stockCount = 150,
                    category = "APPAREL",
                    imageUrl = ""
                ),
                MerchCatalogEntity(
                    id = "merch_cyber_figure",
                    name = "Neon Shogun 1:8 Figurine",
                    description = "Limited Run collectors statue. Hand numbered. Comes with digital token.",
                    price = 950.0,
                    stockCount = 5,
                    category = "COLLECTIBLE",
                    imageUrl = ""
                )
            )
            database.merchDao().insertCatalog(starterMerch)

            // 5. Seed Event Passes (Tickets)
            val starterEvents = listOf(
                EventPassEntity(
                    id = "evt_arena_championship",
                    eventName = "Cyber Arena Pro Invitational",
                    description = "The absolute apex of competitive FPS gaming. Live in Paris.",
                    ticketPrice = 150.0,
                    dateString = "July 12, 2026",
                    location = "Cyber Dome Arena, Paris",
                    totalTickets = 500,
                    ticketsSoldCount = 412,
                    imageUrl = ""
                ),
                EventPassEntity(
                    id = "evt_creator_meetup",
                    eventName = "LoreForge Creator Fanmeet",
                    description = "Private coffee & live designing fan meeting with LoreForge.",
                    ticketPrice = 80.0,
                    dateString = "August 06, 2026",
                    location = "Guild Headquarters, San Francisco",
                    totalTickets = 100,
                    ticketsSoldCount = 68,
                    imageUrl = ""
                )
            )
            database.eventPassDao().insertEventPasses(starterEvents)

            // 6. Seed active peer listings in the peer-to-peer marketplace
            val starterListings = listOf(
                MarketListingEntity(
                    id = "lst_ape_heir",
                    assetId = "nft_placeholder_heir",
                    assetName = "Shadow Shuriken",
                    assetType = "COSMETIC",
                    sellerAddress = "0x123ab45cd67ef89a012bc34de56fg78hi90jk12l",
                    sellPrice = 450.0,
                    isAuction = false
                ),
                MarketListingEntity(
                    id = "lst_wow_sword",
                    assetId = "nft_placeholder_sword",
                    assetName = "Eldritch Nether-Blade",
                    assetType = "COSMETIC",
                    sellerAddress = "0x987zy65xw43vu21ts09rq87po65on43ml21kj09i",
                    sellPrice = 1200.0,
                    isAuction = true,
                    highestBid = 950.0,
                    highestBidderAddress = "0x89fab02bca476686121b6d1a938c5f3e990bff3a", // our user
                    expiresAt = System.currentTimeMillis() + 86400000 * 3 // 3 Days
                )
            )
            database.marketDao().insertListing(starterListings[0])
            database.marketDao().insertListing(starterListings[1])

            // 7. Establish audit trails
            val initialEvent = AuditEventEntity(
                eventId = "evt_genesis",
                type = EventType.TOKEN_EARNED.name,
                timestamp = System.currentTimeMillis() - 120000,
                actorAddress = walletAddress,
                summary = "Genesis event: Seeded default NerdVault eco structures.",
                payloadJson = "{\"status\":\"active\"}",
                signature = "sig_genesis_hash_0xff129da0"
            )
            database.auditDao().insertAuditLog(initialEvent)
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launch(Dispatchers.IO) {
            val recList = apiEngine.getRecommendations()
            _recommendations.value = recList
        }
    }

    // --- Action Methods dispatched to apiEngine ---

    fun executeApiSandboxRequest(route: String, method: String, bodyJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Executing in Sandbox..."
            val result = apiEngine.handleRequest(route, method, bodyJson)
            _sandboxResponse.value = result
            _operationStatus.value = null
            refreshRecommendations()
        }
    }

    fun mintAssetDirectly(name: String, value: Double, type: AssetType) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Minting asset..."
            val user = database.userDao().getPrimaryUserDirect()
            if (user != null) {
                apiEngine.mintAsset(
                    name = name,
                    description = "Custom minted developer asset.",
                    typeString = type.name,
                    value = value,
                    owner = user.walletAddress,
                    metadata = mapOf("dev_mint" to "true")
                )
            }
            _operationStatus.value = null
            refreshRecommendations()
        }
    }

    fun buyMarketplaceItem(listingId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Processing buy order..."
            val apiRes = apiEngine.buyProduct(listingId)
            _operationStatus.value = if (apiRes.statusCode == 200) "Item purchased successfully!" else parseError(apiRes.bodyJson)
            refreshRecommendations()
        }
    }

    fun bidOnAuction(listingId: String, amount: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Submitting bid..."
            val listing = database.marketDao().getListingById(listingId)
            val user = database.userDao().getPrimaryUserDirect()
            if (listing != null && user != null) {
                if (user.creditsBalance < amount) {
                    _operationStatus.value = "Error: Insufficient credits."
                    return@launch
                }
                if (amount <= listing.highestBid && amount <= listing.sellPrice) {
                    _operationStatus.value = "Error: Bid must exceed highest bid of ${listing.highestBid}"
                    return@launch
                }

                // Update bid
                val updatedListing = listing.copy(
                    highestBid = amount,
                    highestBidderAddress = user.walletAddress
                )
                database.marketDao().insertListing(updatedListing)

                // Log Event
                val eventId = "evt_" + UUID.randomUUID().toString().replace("-", "").take(14)
                val signature = "sig_bid_" + UUID.randomUUID().toString().take(6)
                database.auditDao().insertAuditLog(
                    AuditEventEntity(
                        eventId = eventId,
                        type = EventType.BID_PLACED.name,
                        timestamp = System.currentTimeMillis(),
                        actorAddress = user.walletAddress,
                        summary = "Placed bid of $amount Credits on [${listing.assetName}]",
                        payloadJson = "{\"listingId\":\"$listingId\",\"bid\":$amount}",
                        signature = signature
                    )
                )
                _operationStatus.value = "Bid placed successfully!"
            }
            refreshRecommendations()
        }
    }

    fun buyMerchandise(merchId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Checking out hoodie..."
            val apiRes = apiEngine.checkoutMerch(merchId)
            _operationStatus.value = if (apiRes.statusCode == 200) "Merch purchased!" else parseError(apiRes.bodyJson)
            refreshRecommendations()
        }
    }

    fun buyEventPass(passId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Securing tournament pass..."
            val apiRes = apiEngine.mintTicket(passId)
            _operationStatus.value = if (apiRes.statusCode == 201) "Tickets acquired! Check your Wallet." else parseError(apiRes.bodyJson)
            refreshRecommendations()
        }
    }

    fun subscribeCreatorClub(creatorId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Subscribing to creator fanbase..."
            val apiRes = apiEngine.subscribeToCreator(creatorId)
            _operationStatus.value = if (apiRes.statusCode == 201) "Subscribed successfully! Earned 150XP!" else parseError(apiRes.bodyJson)
            refreshRecommendations()
        }
    }

    fun validateAccessTicket(ticketId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _operationStatus.value = "Scannig entry barcode..."
            val apiRes = apiEngine.validateTicket(ticketId)
            _operationStatus.value = if (apiRes.statusCode == 200) "Access GRANTED! Welcome inside." else parseError(apiRes.bodyJson)
            refreshRecommendations()
        }
    }

    fun clearStatus() {
        _operationStatus.value = null
    }

    private fun parseError(json: String): String {
        val pattern = "\"error\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1) ?: "Transaction failed."
    }
}
