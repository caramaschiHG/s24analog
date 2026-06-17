package com.roll24

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roll24.camera.CameraSensorScanner
import com.roll24.camera.CameraSettings
import com.roll24.camera.CaptureJob
import com.roll24.camera.CaptureJobStage
import com.roll24.camera.CaptureOutputUris
import com.roll24.camera.CameraUiState
import com.roll24.camera.GridMode
import com.roll24.camera.Roll24FlashMode
import com.roll24.camera.SavedCapture
import com.roll24.camera.SensorProfile
import com.roll24.camera.TimerMode
import com.roll24.camera.ViewfinderAspect
import com.roll24.film.FilmDevelopmentEngine
import com.roll24.film.FilmLabSettings
import com.roll24.film.FilmProfile
import com.roll24.film.FilmProfileRepository
import com.roll24.gallery.CaptureRecord
import com.roll24.gallery.CaptureStatus
import com.roll24.gallery.Roll24CaptureRepository
import com.roll24.image.BitmapTransforms
import com.roll24.image.CaptureMetadata
import com.roll24.image.ImageSaver
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class Roll24ViewModel : ViewModel() {

    companion object {
        private const val PREFS_NAME = "roll24_prefs"
        private const val KEY_LAST_FILM_PREFIX = "last_film_"
    }

    private val filmEngine = FilmDevelopmentEngine()

    private val _selectedProfile = MutableStateFlow(FilmProfileRepository.getDefaultProfile())
    val selectedProfile: StateFlow<FilmProfile> = _selectedProfile.asStateFlow()

    private val _cameraSettings = MutableStateFlow(CameraSettings())
    val cameraSettings: StateFlow<CameraSettings> = _cameraSettings.asStateFlow()

    private val _filmLabSettings = MutableStateFlow(FilmLabSettings())
    val filmLabSettings: StateFlow<FilmLabSettings> = _filmLabSettings.asStateFlow()

    private val _cameraUiState = MutableStateFlow(CameraUiState())
    val cameraUiState: StateFlow<CameraUiState> = _cameraUiState.asStateFlow()

    private val _galleryCaptures = MutableStateFlow<List<CaptureRecord>>(emptyList())
    val galleryCaptures: StateFlow<List<CaptureRecord>> = _galleryCaptures.asStateFlow()

    private var galleryStarted = false
    private val pendingRecipes = mutableMapOf<String, CaptureRecipeSnapshot>()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun saveSelectedProfileForLens(context: Context, lensLabel: String, profileId: String) {
        prefs(context).edit().putString("$KEY_LAST_FILM_PREFIX$lensLabel", profileId).apply()
    }

    private fun loadSelectedProfileForLens(context: Context, lensLabel: String): FilmProfile? {
        val id = prefs(context).getString("$KEY_LAST_FILM_PREFIX$lensLabel", null) ?: return null
        return FilmProfileRepository.getProfile(id)
    }

    /**
     * Returns a sensible default film profile for the given S24 Ultra lens label.
     *
     * The mapping favours the canonical stocks being added in Waves 4/5, but
     * falls back to existing profiles when a canonical stock is not yet present
     * in the repository.
     */
    private fun defaultProfileForLens(lensLabel: String?): FilmProfile {
        val fallback = FilmProfileRepository.getDefaultProfile()
        if (lensLabel == null) return fallback

        return when (lensLabel) {
            "0.6x" -> FilmProfileRepository.getProfile("gold_200")
                ?: FilmProfileRepository.getProfile("fujicolor_c200")
                ?: FilmProfileRepository.getProfile("warm_gold_200")
            "1x" -> FilmProfileRepository.getProfile("portra_400")
                ?: FilmProfileRepository.getProfile("ektar_100")
                ?: FilmProfileRepository.getProfile("soft_portrait_400")
                ?: FilmProfileRepository.getProfile("s24_1x_clean_negative")
            "3x" -> FilmProfileRepository.getProfile("pro_400h")
                ?: FilmProfileRepository.getProfile("s24_3x_portrait_400")
            "5x" -> FilmProfileRepository.getProfile("vision3_250d")
                ?: FilmProfileRepository.getProfile("s24_5x_chrome_200")
            else -> null
        } ?: fallback
    }

    private fun applyLensProfileSelection(context: Context, lens: SensorProfile?) {
        if (lens == null) return
        val saved = loadSelectedProfileForLens(context, lens.lensLabel)
        _selectedProfile.value = saved ?: defaultProfileForLens(lens.lensLabel)
    }

    fun selectProfile(context: Context, profile: FilmProfile) {
        _selectedProfile.value = profile
        _cameraUiState.value.activeLens?.let { lens ->
            saveSelectedProfileForLens(context, lens.lensLabel, profile.id)
        }
    }

    fun updateAspect(aspect: ViewfinderAspect) {
        _cameraSettings.value = _cameraSettings.value.copy(aspect = aspect)
    }

    fun updateGridMode(gridMode: GridMode) {
        _cameraSettings.value = _cameraSettings.value.copy(gridMode = gridMode)
    }

    fun updateFlashMode(flashMode: Roll24FlashMode) {
        _cameraSettings.value = _cameraSettings.value.copy(flashMode = flashMode)
    }

    fun updateTimerMode(timerMode: TimerMode) {
        _cameraSettings.value = _cameraSettings.value.copy(timerMode = timerMode)
    }

    fun updateExposureCompensation(value: Float) {
        _cameraSettings.value = _cameraSettings.value.copy(exposureCompensation = value.coerceIn(-2f, 2f))
    }

    fun toggleFocusLock() {
        _cameraSettings.value = _cameraSettings.value.copy(
            focusLocked = !_cameraSettings.value.focusLocked
        )
    }

    fun updateFilmLabSettings(settings: FilmLabSettings) {
        _filmLabSettings.value = settings
    }

    fun setCameraReady(isReady: Boolean) {
        _cameraUiState.value = _cameraUiState.value.copy(isCameraReady = isReady)
    }

    fun startGallery(context: Context) {
        if (galleryStarted) return
        galleryStarted = true
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            Roll24CaptureRepository.get(appContext).captures.collect { captures ->
                _galleryCaptures.value = captures
            }
        }
    }

    fun scanSensors(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = runCatching { CameraSensorScanner.scan(appContext) }
                .getOrDefault(emptyList())
            val newlyActive = profiles.firstOrNull()
            _cameraUiState.update { current ->
                current.copy(
                    sensorProfiles = profiles,
                    activeLens = current.activeLens ?: newlyActive
                )
            }
            if (newlyActive != null && _cameraUiState.value.activeLens == null) {
                applyLensProfileSelection(appContext, newlyActive)
            }
        }
    }

    fun selectLens(context: Context, profile: SensorProfile) {
        _cameraUiState.update { it.copy(activeLens = profile) }
        applyLensProfileSelection(context.applicationContext, profile)
    }

    fun beginCaptureFeedback(context: Context): String {
        val appContext = context.applicationContext
        val profile = _selectedProfile.value
        val settings = _cameraSettings.value
        val labSettings = _filmLabSettings.value
        val lens = _cameraUiState.value.activeLens
        val id = ImageSaver.buildUniqueLabel(profile)
        val job = CaptureJob(
            id = id,
            label = id,
            stage = CaptureJobStage.CAPTURING,
            progress = 0.05f
        )
        pendingRecipes[id] = CaptureRecipeSnapshot(profile, settings, labSettings, lens)

        _cameraUiState.update { current ->
            val jobs = (listOf(job) + current.captureJobs).take(8)
            current.copy(
                isCapturing = true,
                shutterFlash = true,
                captureJobs = jobs,
                queueDepth = jobs.count { it.stage == CaptureJobStage.CAPTURING || it.stage == CaptureJobStage.DEVELOPING || it.stage == CaptureJobStage.QUEUED },
                errorMessage = null,
                visibleError = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            Roll24CaptureRepository.get(appContext).upsert(
                CaptureRecord(
                    id = id,
                    createdAt = System.currentTimeMillis(),
                    filmId = profile.id,
                    filmName = profile.name,
                    lensId = lens?.physicalId ?: lens?.cameraId,
                    lensLabel = lens?.lensLabel,
                    aspect = settings.aspect.label,
                    rawUri = null,
                    negativeUri = null,
                    developedUri = null,
                    thumbnailUri = null,
                    galleryUri = null,
                    status = CaptureStatus.CAPTURING,
                    error = null,
                    usedFallback = lens?.supportsRaw != true
                )
            )
        }

        return id
    }

    fun clearShutterFlash() {
        _cameraUiState.value = _cameraUiState.value.copy(shutterFlash = false)
    }

    fun captureFailed(context: Context, jobId: String?, message: String) {
        val appContext = context.applicationContext
        val id = jobId
        if (id == null) {
            _cameraUiState.update {
                it.copy(
                    isCapturing = false,
                    shutterFlash = false,
                    errorMessage = message,
                    visibleError = message
                )
            }
            return
        }

        val failedSnapshot = pendingRecipes[id]
        pendingRecipes.remove(id)
        updateJob(
            jobId = id,
            stage = CaptureJobStage.FAILED,
            progress = 1f,
            error = message
        )
        _cameraUiState.update {
            val jobs = it.captureJobs
            it.copy(
                isCapturing = false,
                isDeveloping = jobs.any { job -> job.stage == CaptureJobStage.DEVELOPING },
                queueDepth = jobs.count { job -> job.stage == CaptureJobStage.CAPTURING || job.stage == CaptureJobStage.DEVELOPING || job.stage == CaptureJobStage.QUEUED },
                shutterFlash = false,
                errorMessage = message,
                visibleError = message
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            Roll24CaptureRepository.get(appContext).upsert(
                CaptureRecord(
                    id = id,
                    createdAt = System.currentTimeMillis(),
                    filmId = failedSnapshot?.profile?.id ?: _selectedProfile.value.id,
                    filmName = failedSnapshot?.profile?.name ?: _selectedProfile.value.name,
                    lensId = failedSnapshot?.lens?.physicalId ?: failedSnapshot?.lens?.cameraId,
                    lensLabel = failedSnapshot?.lens?.lensLabel,
                    aspect = failedSnapshot?.cameraSettings?.aspect?.label ?: _cameraSettings.value.aspect.label,
                    rawUri = null,
                    negativeUri = null,
                    developedUri = null,
                    thumbnailUri = null,
                    galleryUri = null,
                    status = CaptureStatus.FAILED,
                    error = message,
                    usedFallback = failedSnapshot?.lens?.supportsRaw != true
                )
            )
        }
    }

    fun developAndSaveCapture(context: Context, capturedFile: File, jobId: String) {
        val appContext = context.applicationContext
        val snapshot = pendingRecipes[jobId] ?: CaptureRecipeSnapshot(
            profile = _selectedProfile.value,
            cameraSettings = _cameraSettings.value,
            labSettings = _filmLabSettings.value,
            lens = _cameraUiState.value.activeLens
        )
        val profile = snapshot.profile
        val cameraSettings = snapshot.cameraSettings
        val labSettings = snapshot.labSettings

        updateJob(
            jobId = jobId,
            stage = CaptureJobStage.DEVELOPING,
            progress = 0.20f
        )
        _cameraUiState.update { current ->
            current.copy(
                isCapturing = false,
                isDeveloping = true,
                queueDepth = current.captureJobs.count { it.stage == CaptureJobStage.CAPTURING || it.stage == CaptureJobStage.DEVELOPING || it.stage == CaptureJobStage.QUEUED },
                errorMessage = null,
                visibleError = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val decoded = BitmapTransforms.decodeJpegWithExif(capturedFile)
                ?: error("Nao foi possivel abrir a foto capturada")
            val metadata = runCatching {
                CaptureMetadata.fromExifInterface(
                    ExifInterface(capturedFile),
                    decoded.width,
                    decoded.height
                ).also {
                    Log.d("CaptureMetadata", "Extracted metadata: $it")
                }
            }.getOrElse {
                Log.w("CaptureMetadata", "Failed to extract EXIF metadata", it)
                null
            }

            try {
                Roll24CaptureRepository.get(appContext).upsert(
                    CaptureRecord(
                        id = jobId,
                        createdAt = System.currentTimeMillis(),
                        filmId = profile.id,
                        filmName = profile.name,
                        lensId = snapshot.lens?.physicalId ?: snapshot.lens?.cameraId,
                        lensLabel = snapshot.lens?.lensLabel,
                        aspect = cameraSettings.aspect.label,
                        rawUri = null,
                        negativeUri = null,
                        developedUri = null,
                        thumbnailUri = null,
                        galleryUri = null,
                        status = CaptureStatus.DEVELOPING,
                        error = null,
                        usedFallback = snapshot.lens?.supportsRaw != true,
                        metadata = metadata
                    )
                )

                updateJob(jobId, CaptureJobStage.DEVELOPING, 0.42f)
                val cropped = BitmapTransforms.centerCropToAspect(decoded, cameraSettings.aspect.ratio)
                val pair = filmEngine.developPair(
                    cropped,
                    profile,
                    labSettings,
                    sensorProfile = snapshot.lens,
                    captureMetadata = metadata
                )
                updateJob(jobId, CaptureJobStage.DEVELOPING, 0.76f)
                val saved = ImageSaver.saveAnalogCapture(
                    context = appContext,
                    negative = pair.negative,
                    developed = pair.developed,
                    profile = profile,
                    label = jobId,
                    metadata = metadata
                ) ?: error("Nao foi possivel salvar na galeria")

                Roll24CaptureRepository.get(appContext).upsert(
                    CaptureRecord(
                        id = jobId,
                        createdAt = System.currentTimeMillis(),
                        filmId = profile.id,
                        filmName = profile.name,
                        lensId = snapshot.lens?.physicalId ?: snapshot.lens?.cameraId,
                        lensLabel = snapshot.lens?.lensLabel,
                        aspect = cameraSettings.aspect.label,
                        rawUri = saved.rawUri?.toString(),
                        negativeUri = saved.negativeUri?.toString(),
                        developedUri = saved.developedUri?.toString(),
                        thumbnailUri = saved.thumbnailUri?.toString(),
                        galleryUri = saved.galleryUri?.toString(),
                        status = CaptureStatus.SAVED,
                        error = null,
                        usedFallback = snapshot.lens?.supportsRaw != true,
                        metadata = metadata
                    )
                )

                updateJob(
                    jobId = jobId,
                    stage = CaptureJobStage.SAVED,
                    progress = 1f,
                    outputUris = CaptureOutputUris(
                        rawUri = saved.rawUri,
                        negativeUri = saved.negativeUri,
                        developedUri = saved.developedUri,
                        thumbnailUri = saved.thumbnailUri,
                        galleryUri = saved.galleryUri
                    )
                )
                pendingRecipes.remove(jobId)
                _cameraUiState.update { current ->
                    current.copy(
                        isDeveloping = current.captureJobs.any { it.stage == CaptureJobStage.DEVELOPING },
                        queueDepth = current.captureJobs.count { it.stage == CaptureJobStage.CAPTURING || it.stage == CaptureJobStage.DEVELOPING || it.stage == CaptureJobStage.QUEUED },
                        lastSavedId = jobId,
                        lastSavedCapture = SavedCapture(
                            negativeUri = saved.negativeUri,
                            developedUri = saved.developedUri,
                            thumbnailUri = saved.thumbnailUri,
                            galleryUri = saved.galleryUri,
                            label = saved.label
                        )
                    )
                }
            } catch (e: Exception) {
                val message = e.message ?: "Falha ao revelar a foto"
                Roll24CaptureRepository.get(appContext).upsert(
                    CaptureRecord(
                        id = jobId,
                        createdAt = System.currentTimeMillis(),
                        filmId = profile.id,
                        filmName = profile.name,
                        lensId = snapshot.lens?.physicalId ?: snapshot.lens?.cameraId,
                        lensLabel = snapshot.lens?.lensLabel,
                        aspect = cameraSettings.aspect.label,
                        rawUri = null,
                        negativeUri = null,
                        developedUri = null,
                        thumbnailUri = null,
                        galleryUri = null,
                        status = CaptureStatus.FAILED,
                        error = message,
                        usedFallback = snapshot.lens?.supportsRaw != true
                    )
                )
                updateJob(jobId, CaptureJobStage.FAILED, 1f, error = message)
                pendingRecipes.remove(jobId)
                _cameraUiState.update { current ->
                    current.copy(
                        isDeveloping = current.captureJobs.any { it.stage == CaptureJobStage.DEVELOPING },
                        queueDepth = current.captureJobs.count { it.stage == CaptureJobStage.CAPTURING || it.stage == CaptureJobStage.DEVELOPING || it.stage == CaptureJobStage.QUEUED },
                        errorMessage = message,
                        visibleError = message
                    )
                }
            } finally {
                capturedFile.delete()
            }
        }
    }
    
    fun removeCaptureFromLocalGallery(context: Context, record: CaptureRecord) {
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            record.galleryUri?.let { ImageSaver.deleteUri(appContext, Uri.parse(it)) }
            record.developedUri?.let { ImageSaver.deleteUri(appContext, Uri.parse(it)) }
            record.negativeUri?.let { ImageSaver.deleteUri(appContext, Uri.parse(it)) }
            record.thumbnailUri?.let { ImageSaver.deleteUri(appContext, Uri.parse(it)) }
            record.rawUri?.let { ImageSaver.deleteUri(appContext, Uri.parse(it)) }
            Roll24CaptureRepository.get(appContext).delete(record.id)
        }
    }

    private fun updateJob(
        jobId: String,
        stage: CaptureJobStage,
        progress: Float,
        outputUris: CaptureOutputUris = CaptureOutputUris(),
        error: String? = null
    ) {
        _cameraUiState.update { current ->
            val jobs = current.captureJobs.map { job ->
                if (job.id == jobId) {
                    job.copy(
                        stage = stage,
                        progress = progress,
                        outputUris = if (outputUris == CaptureOutputUris()) job.outputUris else outputUris,
                        error = error
                    )
                } else {
                    job
                }
            }
            current.copy(captureJobs = jobs)
        }
    }

    private data class CaptureRecipeSnapshot(
        val profile: FilmProfile,
        val cameraSettings: CameraSettings,
        val labSettings: FilmLabSettings,
        val lens: SensorProfile?
    )
}
