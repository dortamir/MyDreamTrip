package com.example.mydreamtrip.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostsDao {

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE author = :author ORDER BY createdAt DESC")
    fun pagingByAuthor(author: String): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE author = :author ORDER BY createdAt DESC")
    fun observeByAuthor(author: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM posts")
    suspend fun clearAll()
}
