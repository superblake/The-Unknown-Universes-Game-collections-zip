package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import kotlinx.coroutines.flow.Flow

@Dao
interface UniverseDao {

  @Query("SELECT * FROM operator_profiles WHERE userId = :userId LIMIT 1")
  fun observeProfile(userId: String): Flow<OperatorProfile?>

  @Query("SELECT * FROM operator_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getProfileOnce(userId: String): OperatorProfile?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: OperatorProfile)

  @Query("SELECT * FROM armory_items WHERE userId = :userId ORDER BY isCustom ASC, createdAtMillis ASC")
  fun observeArmoryItems(userId: String): Flow<List<ArmoryItem>>

  @Query("SELECT * FROM armory_items WHERE userId = :userId ORDER BY isCustom ASC, createdAtMillis ASC")
  suspend fun getArmoryItemsOnce(userId: String): List<ArmoryItem>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertArmoryItem(item: ArmoryItem)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertArmoryItems(items: List<ArmoryItem>)

  @Query("DELETE FROM armory_items WHERE userId = :userId AND id = :itemId")
  suspend fun deleteArmoryItem(userId: String, itemId: String)

  @Query("DELETE FROM armory_items WHERE userId = :userId")
  suspend fun deleteAllArmoryItemsForUser(userId: String)

  @Query("SELECT * FROM custom_rigs WHERE userId = :userId ORDER BY createdAtMillis ASC")
  fun observeCustomRigs(userId: String): Flow<List<RigAsset>>

  @Query("SELECT * FROM custom_rigs WHERE userId = :userId ORDER BY createdAtMillis ASC")
  suspend fun getRigsOnce(userId: String): List<RigAsset>

  @Query("SELECT * FROM custom_rigs WHERE userId = :userId AND id = :rigId LIMIT 1")
  suspend fun getRigOnce(userId: String, rigId: String): RigAsset?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRig(rig: RigAsset)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRigs(rigs: List<RigAsset>)

  @Query("DELETE FROM custom_rigs WHERE userId = :userId")
  suspend fun deleteAllRigsForUser(userId: String)

  @Query("SELECT * FROM match_logs WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 25")
  fun observeMatchLogs(userId: String): Flow<List<MatchRecord>>

  @Query("SELECT * FROM match_logs WHERE userId = :userId ORDER BY createdAtMillis DESC LIMIT 50")
  suspend fun getMatchLogsOnce(userId: String): List<MatchRecord>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatchLog(record: MatchRecord)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMatchLogs(records: List<MatchRecord>)

  @Query("DELETE FROM match_logs WHERE userId = :userId")
  suspend fun deleteAllMatchLogsForUser(userId: String)

  @Query("SELECT * FROM linked_accounts WHERE userId = :userId ORDER BY linkedAtMillis DESC")
  fun observeLinkedAccounts(userId: String): Flow<List<LinkedAccount>>

  @Query("SELECT * FROM linked_accounts WHERE userId = :userId ORDER BY linkedAtMillis DESC")
  suspend fun getLinkedAccountsOnce(userId: String): List<LinkedAccount>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLinkedAccount(account: LinkedAccount)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLinkedAccounts(accounts: List<LinkedAccount>)

  @Query("DELETE FROM linked_accounts WHERE userId = :userId AND providerId = :providerId")
  suspend fun deleteLinkedAccount(userId: String, providerId: String)
}
