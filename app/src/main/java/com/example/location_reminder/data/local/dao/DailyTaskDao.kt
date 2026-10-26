package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.location_reminder.data.local.entity.DailyTaskCompletionEntity
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.DailyTaskWithCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTaskDao {

    @Query(
        """
        SELECT dt.id AS taskId, dt.title AS title, dt.createdAt AS createdAt,
               COALESCE(dtc.isCompleted, 0) AS isCompleted,
               dtc.completedAt AS completedAt
        FROM daily_tasks dt
        LEFT JOIN daily_task_completions dtc ON dt.id = dtc.taskId AND dtc.date = :date
        WHERE dt.isDeleted = 0
        ORDER BY dt.createdAt ASC
        """
    )
    fun getDailyTasksWithCompletion(date: String): Flow<List<DailyTaskWithCompletion>>

    @Query("SELECT * FROM daily_tasks WHERE isDeleted = 0 ORDER BY createdAt ASC")
    fun getActiveDailyTasks(): Flow<List<DailyTaskEntity>>

    @Query("SELECT * FROM daily_tasks ORDER BY createdAt ASC")
    fun getAllDailyTasks(): Flow<List<DailyTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DailyTaskEntity): Long

    @Update
    suspend fun updateTask(task: DailyTaskEntity)

    @Delete
    suspend fun deleteTask(task: DailyTaskEntity)

    @Query("DELETE FROM daily_tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)

    @Query("UPDATE daily_tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :taskId")
    suspend fun softDeleteTaskById(taskId: Long, deletedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompletion(completion: DailyTaskCompletionEntity): Long

    @Query("DELETE FROM daily_task_completions WHERE taskId = :taskId AND date = :date")
    suspend fun deleteCompletion(taskId: Long, date: String)

    @Query("SELECT * FROM daily_task_completions WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getCompletion(taskId: Long, date: String): DailyTaskCompletionEntity?

    @Query("SELECT * FROM daily_task_completions WHERE date IN (:dates)")
    fun getCompletionsForDates(dates: List<String>): Flow<List<DailyTaskCompletionEntity>>

    @androidx.room.Transaction
    suspend fun updateCompletionsForDate(date: String, updates: Map<Long, Boolean>) {
        for ((taskId, isCompleted) in updates) {
            if (isCompleted) {
                val existing = getCompletion(taskId, date)
                val completion = DailyTaskCompletionEntity(
                    id = existing?.id ?: 0L,
                    taskId = taskId,
                    date = date,
                    isCompleted = true,
                    completedAt = existing?.completedAt ?: System.currentTimeMillis()
                )
                insertOrUpdateCompletion(completion)
            } else {
                deleteCompletion(taskId, date)
            }
        }
    }
}
