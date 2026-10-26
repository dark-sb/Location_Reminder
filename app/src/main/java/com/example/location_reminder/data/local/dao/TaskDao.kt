package com.example.location_reminder.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.location_reminder.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE reminderId = :reminderId ORDER BY createdAt ASC")
    fun getTasksForReminder(reminderId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE reminderId = :reminderId ORDER BY createdAt ASC")
    suspend fun getTasksForReminderOnce(reminderId: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE reminderId = :reminderId")
    suspend fun deleteTasksForReminder(reminderId: Long)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompleted(id: Long, isCompleted: Boolean)
}
