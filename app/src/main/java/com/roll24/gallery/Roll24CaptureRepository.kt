package com.roll24.gallery

import android.content.Context
import kotlinx.coroutines.flow.Flow

class Roll24CaptureRepository private constructor(
    private val dao: CaptureDao
) {
    val captures: Flow<List<CaptureRecord>> = dao.observeAll()

    suspend fun upsert(record: CaptureRecord) {
        dao.upsert(record)
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    companion object {
        @Volatile
        private var instance: Roll24CaptureRepository? = null

        fun get(context: Context): Roll24CaptureRepository {
            return instance ?: synchronized(this) {
                instance ?: Roll24CaptureRepository(
                    Roll24Database.get(context).captureDao()
                ).also { instance = it }
            }
        }
    }
}
