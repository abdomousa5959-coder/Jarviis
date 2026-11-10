package com.openjarvis.automation

import android.content.Context
import androidx.room.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val scheduleType: String,
    val scheduleHour: Int = 0,
    val scheduleMinute: Int = 0,
    val scheduleDayOfWeek: Int = 0,
    val scheduleIntervalMs: Long = 0,
    val scheduleAtMs: Long = 0,
    val enabled: Boolean = true,
    val lastRun: Long? = null,
    val lastResult: String? = null,
    val runCount: Int = 0
)

fun AutomationEntity.toAutomation(): AutomationManager.Automation {
    val schedule = when (scheduleType) {
        "daily" -> AutomationManager.AutomationSchedule.Daily(scheduleHour, scheduleMinute)
        "weekly" -> AutomationManager.AutomationSchedule.Weekly(scheduleDayOfWeek, scheduleHour, scheduleMinute)
        "interval" -> AutomationManager.AutomationSchedule.Interval(scheduleIntervalMs)
        "once" -> AutomationManager.AutomationSchedule.Once(scheduleAtMs)
        else -> AutomationManager.AutomationSchedule.Interval(60 * 60 * 1000L)
    }
    return AutomationManager.Automation(id, name, command, schedule, enabled, lastRun, lastResult, runCount)
}

fun AutomationManager.Automation.toEntity(): AutomationEntity {
    val type: String
    var hour = 0
    var minute = 0
    var day = 0
    var interval = 0L
    var at = 0L
    when (val value = schedule) {
        is AutomationManager.AutomationSchedule.Daily -> { type = "daily"; hour = value.hour; minute = value.minute }
        is AutomationManager.AutomationSchedule.Weekly -> { type = "weekly"; day = value.dayOfWeek; hour = value.hour; minute = value.minute }
        is AutomationManager.AutomationSchedule.Interval -> { type = "interval"; interval = value.intervalMs }
        is AutomationManager.AutomationSchedule.Once -> { type = "once"; at = value.atMs }
    }
    return AutomationEntity(id, name, command, type, hour, minute, day, interval, at, enabled, lastRun, lastResult, runCount)
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY name")
    suspend fun getAll(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationEntity)

    @Update
    suspend fun update(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [AutomationEntity::class], version = 2, exportSchema = false)
abstract class AutomationDB : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile private var INSTANCE: AutomationDB? = null

        fun getInstance(context: Context): AutomationDB {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDB::class.java,
                    "automations.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("automation_id") ?: return Result.failure()
        return try {
            val dao = AutomationDB.getInstance(applicationContext).automationDao()
            val entity = dao.getById(id) ?: return Result.failure()
            val automation = entity.toAutomation()
            val updated = automation.copy(
                lastRun = System.currentTimeMillis(),
                lastResult = "success",
                runCount = automation.runCount + 1
            )
            dao.update(updated.toEntity())
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
