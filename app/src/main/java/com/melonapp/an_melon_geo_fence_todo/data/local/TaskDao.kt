package com.melonapp.an_melon_geo_fence_todo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, rowid DESC")
    fun getAllTasksWithPlaceFlow(): Flow<List<TaskWithPlace>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getActiveTasksWithPlace(): List<TaskWithPlace>

    @Transaction
    @Query("SELECT * FROM tasks WHERE placeId = :placeId AND isCompleted = 0")
    suspend fun getActiveTasksForPlace(placeId: String): List<TaskWithPlace>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskWithPlaceById(id: String): TaskWithPlace?

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, isSynced = :isSynced WHERE id = :taskId")
    suspend fun setTaskCompleted(taskId: String, isCompleted: Boolean, isSynced: Boolean = false)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("SELECT * FROM tasks WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<TaskEntity>

    @Query("UPDATE tasks SET isSynced = :isSynced WHERE id = :taskId")
    suspend fun updateSyncStatus(taskId: String, isSynced: Boolean)
}
