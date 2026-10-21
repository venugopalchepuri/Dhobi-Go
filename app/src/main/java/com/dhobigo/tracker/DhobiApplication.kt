package com.dhobigo.tracker

import android.app.Application
import com.dhobigo.tracker.data.auth.AuthRepository
import com.dhobigo.tracker.data.auth.SessionManager
import com.dhobigo.tracker.data.local.database.DhobiDatabase
import com.dhobigo.tracker.data.ocr.TextRecognitionManager
import com.dhobigo.tracker.data.repository.LaundryRepository
import com.dhobigo.tracker.data.storage.ImageStorageManager

class DhobiApplication : Application() {

    lateinit var database: DhobiDatabase
        private set

    lateinit var imageStorageManager: ImageStorageManager
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var repository: LaundryRepository
        private set

    lateinit var textRecognitionManager: TextRecognitionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = DhobiDatabase.getInstance(this)
        imageStorageManager = ImageStorageManager(this)
        sessionManager = SessionManager(this, database.userDao())
        authRepository = AuthRepository(
            userDao = database.userDao(),
            recordDao = database.laundryRecordDao(),
            itemDao = database.laundryItemDao(),
            sessionManager = sessionManager,
            imageStorageManager = imageStorageManager
        )
        repository = LaundryRepository(
            context = this,
            database = database,
            imageStorageManager = imageStorageManager,
            sessionManager = sessionManager
        )
        textRecognitionManager = TextRecognitionManager(this)
    }

    companion object {
        lateinit var instance: DhobiApplication
            private set
    }
}
