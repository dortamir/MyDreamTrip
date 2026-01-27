package com.example.mydreamtrip.data.repo

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.mydreamtrip.data.local.AppDatabase
import com.example.mydreamtrip.data.local.PostEntity
import com.example.mydreamtrip.data.local.PostsDao
import com.example.mydreamtrip.model.Destination
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PostsRepository(context: Context) {

    private val dao: PostsDao = AppDatabase.getInstance(context).postsDao()
    private val db = FirebaseFirestore.getInstance()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun observeExplore(): Flow<List<Destination>> {
        return dao.observeAll().map { list -> list.map { it.toDestination() } }
    }

    fun observeMyPosts(author: String): Flow<List<Destination>> {
        return dao.observeByAuthor(author).map { list -> list.map { it.toDestination() } }
    }

    fun explorePaging(): Flow<PagingData<Destination>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.pagingAll() }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDestination() }
        }
    }

    fun myPostsPaging(author: String): Flow<PagingData<Destination>> {
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.pagingByAuthor(author) }
        ).flow.map { pagingData ->
            pagingData.map { entity -> entity.toDestination() }
        }
    }

    fun startSyncExplorePosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val entities = snapshot.documents.map { doc ->
                    val createdAtMillis =
                        doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L

                    PostEntity(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        location = doc.getString("location") ?: "",
                        ratingText = doc.getString("ratingText") ?: "⭐ 0.0 (0)",
                        author = doc.getString("author") ?: "Guest",
                        localImageUri = doc.getString("localImageUri")?.takeIf { it.isNotBlank() },
                        createdAt = createdAtMillis
                    )
                }

                ioScope.launch {
                    dao.upsertAll(entities)
                }
            }
    }

    private fun PostEntity.toDestination(): Destination {
        return Destination(
            id = id,
            title = title,
            location = location,
            ratingText = ratingText,
            author = author,
            imageRes = android.R.drawable.ic_menu_gallery,
            localImageUri = localImageUri
        )
    }
}
