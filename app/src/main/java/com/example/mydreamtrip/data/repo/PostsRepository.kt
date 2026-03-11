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

    fun startSyncExplorePosts() {
        db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val entities = snapshot.documents.map { doc -> doc.toPostEntity() }

                ioScope.launch {
                    // Full replace keeps local cache exactly equal to Firestore,
                    // so deleted posts disappear from Explore/Profile immediately.
                    dao.clearAll()
                    if (entities.isNotEmpty()) {
                        dao.upsertAll(entities)
                    }
                }
            }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPostEntity(): PostEntity {
        val createdAtMillis =
            getTimestamp("createdAt")?.toDate()?.time ?: 0L

        return PostEntity(
            id = id,
            title = getString("title") ?: "",
            location = getString("location") ?: "",
            ratingText = getString("ratingText") ?: "⭐ 0.0 (0)",
            author = getString("author") ?: "Guest",
            authorUid = getString("authorUid")?.takeIf { it.isNotBlank() },
            authorPhotoUrl = getString("authorPhotoUrl")?.takeIf { it.isNotBlank() },
            localImageUri = getString("localImageUri")?.takeIf { it.isNotBlank() },
            wikiTitle = getString("wikiTitle")?.takeIf { it.isNotBlank() },
            wikiExtract = getString("wikiExtract")?.takeIf { it.isNotBlank() },
            wikiUrl = getString("wikiUrl")?.takeIf { it.isNotBlank() },
            wikiImageUrl = getString("wikiImageUrl")?.takeIf { it.isNotBlank() },
            createdAt = createdAtMillis
        )
    }

    private fun PostEntity.toDestination(): Destination {
        return Destination(
            id = id,
            title = title,
            location = location,
            ratingText = ratingText,
            author = author,
            authorUid = authorUid ?: "",
            authorPhotoUrl = authorPhotoUrl ?: "",
            imageRes = android.R.drawable.ic_menu_gallery,
            localImageUri = localImageUri,

            //  now available in UI
            wikiTitle = wikiTitle ?: "",
            wikiExtract = wikiExtract ?: "",
            wikiUrl = wikiUrl ?: "",
            wikiImageUrl = wikiImageUrl ?: ""
        )
    }
}
