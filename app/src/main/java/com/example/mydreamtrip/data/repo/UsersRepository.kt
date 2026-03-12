package com.example.mydreamtrip.data.repo

import android.content.Context
import com.example.mydreamtrip.data.local.AppDatabase
import com.example.mydreamtrip.data.local.UserEntity
import kotlinx.coroutines.flow.Flow

class UsersRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).usersDao()

    suspend fun saveUser(user: UserEntity) {
        dao.upsert(user)
    }

    suspend fun getCachedUser(uid: String): UserEntity? {
        return dao.getByUid(uid)
    }

    fun observeUser(uid: String): Flow<UserEntity?> {
        return dao.observeByUid(uid)
    }
}
