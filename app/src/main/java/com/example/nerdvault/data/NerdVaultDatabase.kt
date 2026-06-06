package com.example.nerdvault.data

import androidx.room.*
import com.example.nerdvault.domain.AssetType
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

// --- Room Database Entities ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val walletAddress: String,
    val creditsBalance: Double,
    val xp: Int = 1250,
    val gamePointsGold: Double = 5000.0,
    val gamePointsVBucks: Double = 1200.0,
    val gamePointsGuild: Double = 350.0
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val type: String, // Maps to AssetType.name
    val balance: Double,
    val valueInCredits: Double,
    val imageResUrl: String,
    val metadataJson: String, // Map<String, String> serialized
    val creatorId: String?,
    val isTransferable: Boolean,
    val ownerAddress: String,
    val stateType: String, // OWNED, LISTED, TRANSFER_PENDING, REDEEMED, EXPIRED
    val stateJson: String // String representation of the AssetState details
)

@Entity(tableName = "market_listings")
data class MarketListingEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val assetName: String,
    val assetType: String,
    val sellerAddress: String,
    val sellPrice: Double,
    val isAuction: Boolean = false,
    val highestBid: Double = 0.0,
    val highestBidderAddress: String? = null,
    val expiresAt: Long = 0L,
    val isCompleted: Boolean = false
)

@Entity(tableName = "creators")
data class CreatorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val walletAddress: String,
    val revenueSharePercentage: Double, // % creator gets, remainder to platform
    val accumulatedEarnings: Double = 0.0,
    val totalSubscribers: Int = 0,
    val fandomTags: String, // Comma separated, e.g. "RPG,Action,FPS"
    val avatarUrl: String = ""
)

@Entity(tableName = "merch_catalog")
data class MerchCatalogEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val stockCount: Int,
    val category: String, // APPAREL, COLLECTIBLE, PERIPHERALS, BUNDLE
    val imageUrl: String
)

@Entity(tableName = "event_passes")
data class EventPassEntity(
    @PrimaryKey val id: String,
    val eventName: String,
    val description: String,
    val ticketPrice: Double,
    val dateString: String,
    val location: String,
    val totalTickets: Int,
    val ticketsSoldCount: Int = 0,
    val imageUrl: String
)

@Entity(tableName = "audit_events")
data class AuditEventEntity(
    @PrimaryKey val eventId: String,
    val type: String, // EventType.name
    val timestamp: Long,
    val actorAddress: String,
    val summary: String,
    val payloadJson: String,
    val signature: String
)

// --- Room Type Converters ---

class NerdVaultTypeConverters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val mapAdapter = moshi.adapter<Map<String, String>>(
        Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
    )

    @TypeConverter
    fun stringToMap(value: String?): Map<String, String> {
        if (value.isNullOrEmpty()) return emptyMap()
        return try {
            mapAdapter.fromJson(value) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun mapToString(map: Map<String, String>?): String {
        if (map == null) return "{}"
        return mapAdapter.toJson(map)
    }
}

// --- DAOs (Data Access Objects) ---

@Dao
interface UserCombinedDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getPrimaryUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getPrimaryUserDirect(): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface AssetCombinedDao {
    @Query("SELECT * FROM assets")
    fun getAllAssetsFlow(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE ownerAddress = :walletAddress")
    fun getAssetsByOwnerFlow(walletAddress: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE ownerAddress = :walletAddress AND type = :assetType")
    suspend fun getAssetsByOwnerAndTypeDirect(walletAddress: String, assetType: String): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE id = :assetId")
    suspend fun getAssetById(assetId: String): AssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Query("DELETE FROM assets WHERE id = :assetId")
    suspend fun deleteAssetById(assetId: String)
}

@Dao
interface MarketCombinedDao {
    @Query("SELECT * FROM market_listings WHERE isCompleted = 0")
    fun getActiveListingsFlow(): Flow<List<MarketListingEntity>>

    @Query("SELECT * FROM market_listings WHERE id = :listingId")
    suspend fun getListingById(listingId: String): MarketListingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListing(listing: MarketListingEntity)

    @Update
    suspend fun updateListing(listing: MarketListingEntity)
}

@Dao
interface CreatorCombinedDao {
    @Query("SELECT * FROM creators")
    fun getAllCreatorsFlow(): Flow<List<CreatorEntity>>

    @Query("SELECT * FROM creators WHERE id = :id")
    suspend fun getCreatorById(id: String): CreatorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreator(creator: CreatorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreators(creators: List<CreatorEntity>)

    @Update
    suspend fun updateCreator(creator: CreatorEntity)
}

@Dao
interface MerchCombinedDao {
    @Query("SELECT * FROM merch_catalog")
    fun getMerchCatalogFlow(): Flow<List<MerchCatalogEntity>>

    @Query("SELECT * FROM merch_catalog WHERE id = :id")
    suspend fun getMerchItemById(id: String): MerchCatalogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchItem(item: MerchCatalogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCatalog(catalog: List<MerchCatalogEntity>)

    @Update
    suspend fun updateMerchItem(item: MerchCatalogEntity)
}

@Dao
interface EventPassCombinedDao {
    @Query("SELECT * FROM event_passes")
    fun getAllEventPassesFlow(): Flow<List<EventPassEntity>>

    @Query("SELECT * FROM event_passes WHERE id = :id")
    suspend fun getEventPassById(id: String): EventPassEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventPass(pass: EventPassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventPasses(passes: List<EventPassEntity>)

    @Update
    suspend fun updateEventPass(pass: EventPassEntity)
}

@Dao
interface AuditCombinedDao {
    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC")
    fun getAuditTrailFlow(): Flow<List<AuditEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(event: AuditEventEntity)
}

// --- App Database definition ---

@Database(
    entities = [
        UserEntity::class,
        AssetEntity::class,
        MarketListingEntity::class,
        CreatorEntity::class,
        MerchCatalogEntity::class,
        EventPassEntity::class,
        AuditEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(NerdVaultTypeConverters::class)
abstract class NerdVaultRoomDatabase : RoomDatabase() {
    abstract fun userDao(): UserCombinedDao
    abstract fun assetDao(): AssetCombinedDao
    abstract fun marketDao(): MarketCombinedDao
    abstract fun creatorDao(): CreatorCombinedDao
    abstract fun merchDao(): MerchCombinedDao
    abstract fun eventPassDao(): EventPassCombinedDao
    abstract fun auditDao(): AuditCombinedDao
}
