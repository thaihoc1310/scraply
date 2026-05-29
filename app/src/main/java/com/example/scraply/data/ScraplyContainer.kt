package com.example.scraply.data

import android.content.Context
import com.example.scraply.data.auth.AuthRepository
import com.example.scraply.data.local.ScraplyDatabase
import com.example.scraply.data.preferences.AppPreferencesRepository
import com.example.scraply.data.remote.FirestoreSyncRepository
import com.example.scraply.data.remote.NotificationsRepository
import com.example.scraply.data.remote.SocialRepository
import com.example.scraply.data.remote.StorageRepository
import com.example.scraply.data.repository.CollectionRepository
import com.example.scraply.data.repository.ProjectRepository
import com.example.scraply.data.repository.StampRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class ScraplyContainer(context: Context) {
    val appContext: Context = context.applicationContext

    val appPreferencesRepository = AppPreferencesRepository(appContext)

    private val db: ScraplyDatabase = ScraplyDatabase.get(appContext)

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Firebase services. FirebaseApp.initializeApp is a no-op if the google-services plugin already
    // initialized it; the try/catch guards against a placeholder google-services.json at runtime so
    // non-Firebase flows keep working in the offline-first scenarios spelled out in REQUIREMENTS §9.
    private val firebaseAvailable: Boolean = runCatching {
        FirebaseApp.initializeApp(appContext)
        FirebaseApp.getInstance()
        true
    }.getOrDefault(false)

    val authRepository: AuthRepository? = if (firebaseAvailable) AuthRepository(appContext) else null
    val storageRepository: StorageRepository? = if (firebaseAvailable) StorageRepository(appContext) else null
    val firestoreSyncRepository: FirestoreSyncRepository? =
        if (firebaseAvailable && storageRepository != null)
            FirestoreSyncRepository(appContext, db, storageRepository)
        else null
    val notificationsRepository: NotificationsRepository? =
        if (firebaseAvailable) NotificationsRepository() else null
    val socialRepository: SocialRepository? =
        if (firebaseAvailable && storageRepository != null && notificationsRepository != null)
            SocialRepository(appContext, storageRepository, notificationsRepository)
        else null

    val collectionRepository: CollectionRepository = CollectionRepository(
        collectionDao = db.collectionDao(),
        collectionStampDao = db.collectionStampDao(),
        sync = firestoreSyncRepository,
        currentUid = { authRepository?.currentUserId },
        appScope = appScope,
    )
    val stampRepository: StampRepository = StampRepository(
        stampDao = db.stampDao(),
        collectionStampDao = db.collectionStampDao(),
        collectionRepository = collectionRepository,
        sync = firestoreSyncRepository,
        currentUid = { authRepository?.currentUserId },
        appScope = appScope,
    )
    val projectRepository: ProjectRepository = ProjectRepository(
        projectDao = db.projectDao(),
        sync = firestoreSyncRepository,
        currentUid = { authRepository?.currentUserId },
        appScope = appScope,
    )

    init {
        appScope.launch { collectionRepository.ensureDefaultCollection() }
    }
}
