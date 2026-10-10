package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AnvilAudioSynthesizer
import com.example.data.ai.GeminiStudioClient
import com.example.data.ai.GeneratedImageOutput
import com.example.data.ai.GeneratedMusicOutput
import com.example.data.ai.GroundedSearchResponse
import com.example.data.model.CodeSnippet
import com.example.data.model.ConceptState
import com.example.data.model.FsrsGrade
import com.example.data.model.MistakeEntry
import com.example.data.model.ProfileReport
import com.example.data.model.UserProfile
import com.example.data.repository.AnvilRepository
import com.example.domain.AnvilCurriculumCatalog
import com.example.domain.CodeSandboxRunner
import com.example.domain.CourseTrack
import com.example.domain.SandboxResult
import com.example.domain.SkillNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class PrimaryTab(val label: String, val testTag: String) {
    HOME("Home", "nav_tab_home"),
    LEARN("Learn", "nav_tab_learn"),
    PRACTICE("Practice", "nav_tab_practice"),
    BUILD("Build", "nav_tab_build"),
    YOU("You", "nav_tab_you")
}

enum class AiModelTier(val label: String, val modelId: String, val badge: String) {
    FAST_LITE("Low-Latency Bolt", GeminiStudioClient.MODEL_FAST_LITE, "Fast • Flash-Lite"),
    GENERAL_FLASH("General Socratic", GeminiStudioClient.MODEL_GENERAL_FLASH, "Balanced • 3.5 Flash"),
    HIGH_THINKING_PRO("High Thinking Pro", GeminiStudioClient.MODEL_PRO_HIGH_THINKING, "ThinkingLevel.HIGH • 3.1 Pro")
}

data class ActiveLessonSession(
    val node: SkillNode,
    val exerciseIndex: Int = 0,
    val editorCode: String = node.exercises.firstOrNull()?.starterCode ?: "",
    val selectedOption: String = "",
    val unlockedHintLevel: Int = 0,
    val lowLatencyAiHint: String? = null,
    val isHintLoading: Boolean = false,
    val lastEvaluation: SandboxResult? = null,
    val isCompleted: Boolean = false,
    val earnedXpInSession: Long = 0L
)

data class AiStudioUiState(
    val selectedTier: AiModelTier = AiModelTier.HIGH_THINKING_PRO,
    val selectedTutorMode: String = "Socratic",
    val tutorPrompt: String = "Explain why symplectic integrators like Velocity Verlet conserve energy in orbital mechanics while Euler spirals outward.",
    val tutorResponse: String = "",
    val isTutorLoading: Boolean = false,
    val searchQuery: String = "What are the newest performance features in Python 3.13 and Kotlin 2.2?",
    val searchResult: GroundedSearchResponse? = null,
    val isSearchLoading: Boolean = false,
    val imagePrompt: String = "Architectural blueprint diagram of a binary search tree and CPU cache lines in glowing indigo and amber on dark ink background",
    val selectedImageSize: String = "1K", // "1K", "2K", or "4K"
    val selectedAspectRatio: String = "1:1",
    val generatedImage: GeneratedImageOutput? = null,
    val isImageLoading: Boolean = false,
    val musicPrompt: String = "Ambient minimalist synth and warm analog Rhodes focus track for deep algorithmic programming, 90 BPM",
    val useFullLengthProTrack: Boolean = false,
    val generatedMusic: GeneratedMusicOutput? = null,
    val isMusicLoading: Boolean = false
)

class AnvilViewModel(
    private val repository: AnvilRepository,
    val currentUserId: String
) : ViewModel() {

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile()
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            UiState.Success(profile ?: UserProfile(id = currentUserId, userId = currentUserId))
        }
        .catch { error ->
            Log.w(TAG, "Error observing profile", error)
            emit(UiState.Error(error.message ?: "Failed to load learner profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val conceptStates: StateFlow<UiState<List<ConceptState>>> = repository.observeConceptStates()
        .map<List<ConceptState>, UiState<List<ConceptState>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing concepts", error)
            emit(UiState.Error(error.message ?: "Failed to load FSRS concept cards"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val snippetsState: StateFlow<UiState<List<CodeSnippet>>> = repository.observeSnippets()
        .map<List<CodeSnippet>, UiState<List<CodeSnippet>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { error ->
            Log.w(TAG, "Error observing snippets", error)
            emit(UiState.Error(error.message ?: "Failed to load snippets"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val mistakesState: StateFlow<UiState<List<MistakeEntry>>> = repository.observeMistakes()
        .map<List<MistakeEntry>, UiState<List<MistakeEntry>>> { list ->
            UiState.Success(list.sortedBy { it.resolved })
        }
        .catch { error ->
            Log.w(TAG, "Error observing mistakes", error)
            emit(UiState.Error(error.message ?: "Failed to load Mistake Journal"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val reportsState: StateFlow<UiState<List<ProfileReport>>> = repository.observeReports()
        .map<List<ProfileReport>, UiState<List<ProfileReport>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { error ->
            Log.w(TAG, "Error observing reports", error)
            emit(UiState.Error(error.message ?: "Failed to load MIPR reports"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _selectedTab = MutableStateFlow(PrimaryTab.HOME)
    val selectedTab: StateFlow<PrimaryTab> = _selectedTab.asStateFlow()

    private val _activeLessonSession = MutableStateFlow<ActiveLessonSession?>(null)
    val activeLessonSession: StateFlow<ActiveLessonSession?> = _activeLessonSession.asStateFlow()

    private val _playgroundLanguage = MutableStateFlow("Python")
    val playgroundLanguage: StateFlow<String> = _playgroundLanguage.asStateFlow()

    private val _playgroundCode = MutableStateFlow(
        "# Anvil v5.0 Scientific & Algorithmic Sandbox\nmass = 6\nvelocity = 10\nenergy = 0.5 * mass * (velocity ** 2)\nprint(int(energy))"
    )
    val playgroundCode: StateFlow<String> = _playgroundCode.asStateFlow()

    private val _playgroundResult = MutableStateFlow<SandboxResult?>(null)
    val playgroundResult: StateFlow<SandboxResult?> = _playgroundResult.asStateFlow()

    private val _aiStudio = MutableStateFlow(AiStudioUiState())
    val aiStudio: StateFlow<AiStudioUiState> = _aiStudio.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _searchDialogOpen = MutableStateFlow(false)
    val searchDialogOpen: StateFlow<Boolean> = _searchDialogOpen.asStateFlow()

    private val _localSearchQuery = MutableStateFlow("")
    val localSearchQuery: StateFlow<String> = _localSearchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureUserProfile()
        }
    }

    fun selectTab(tab: PrimaryTab) {
        _selectedTab.value = tab
    }

    fun setSearchDialogOpen(open: Boolean) {
        _searchDialogOpen.value = open
    }

    fun setLocalSearchQuery(q: String) {
        _localSearchQuery.value = q
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun currentCourse(profile: UserProfile?): CourseTrack {
        val courseId = profile?.activeCourseId ?: "python-core"
        return AnvilCurriculumCatalog.courses.firstOrNull { it.id == courseId }
            ?: AnvilCurriculumCatalog.courses.first()
    }

    fun completeOnboarding(
        displayName: String,
        courseId: String,
        dailyGoalMinutes: Long,
        themeId: String,
        calmMode: Boolean
    ) {
        viewModelScope.launch {
            val res = repository.updateUserPreferences(
                displayName = displayName,
                activeCourseId = courseId,
                dailyGoalMinutes = dailyGoalMinutes,
                themeId = themeId,
                calmMode = calmMode,
                onboardingCompleted = true
            )
            if (res.isSuccess) {
                _statusBanner.value = "Workshop configured. Ready to strike."
            }
        }
    }

    fun updateCourse(courseId: String) {
        viewModelScope.launch {
            repository.updateUserPreferences(activeCourseId = courseId)
        }
    }

    fun updateTheme(themeId: String) {
        viewModelScope.launch {
            repository.updateUserPreferences(themeId = themeId)
        }
    }

    fun toggleCalmMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateUserPreferences(calmMode = enabled)
            _statusBanner.value = if (enabled) {
                "Calm Mode enabled: XP, streaks, and celebrations hidden."
            } else {
                "Calm Mode disabled."
            }
        }
    }

    fun updateDailyGoal(minutes: Long) {
        viewModelScope.launch {
            repository.updateUserPreferences(dailyGoalMinutes = minutes)
        }
    }

    // --- Focus Mode Lesson & Exercise Flow ---

    fun startLessonNode(node: SkillNode) {
        val firstEx = node.exercises.firstOrNull() ?: return
        _activeLessonSession.value = ActiveLessonSession(
            node = node,
            exerciseIndex = 0,
            editorCode = firstEx.starterCode,
            selectedOption = firstEx.options.firstOrNull() ?: ""
        )
    }

    fun closeLessonSession() {
        _activeLessonSession.value = null
    }

    fun updateLessonEditorCode(newCode: String) {
        _activeLessonSession.update { session ->
            session?.copy(editorCode = newCode)
        }
    }

    fun insertTokenInLessonEditor(token: String) {
        _activeLessonSession.update { session ->
            session?.copy(editorCode = session.editorCode + token)
        }
    }

    fun selectLessonOption(option: String) {
        _activeLessonSession.update { session ->
            session?.copy(selectedOption = option)
        }
    }

    fun unlockNextHint() {
        _activeLessonSession.update { session ->
            if (session == null) return@update null
            val maxHints = session.node.exercises.getOrNull(session.exerciseIndex)?.hints?.size ?: 0
            session.copy(unlockedHintLevel = (session.unlockedHintLevel + 1).coerceAtMost(maxHints))
        }
    }

    fun requestLowLatencyExerciseAiHint() {
        val session = _activeLessonSession.value ?: return
        val ex = session.node.exercises.getOrNull(session.exerciseIndex) ?: return
        _activeLessonSession.update { it?.copy(isHintLoading = true) }
        viewModelScope.launch {
            val prompt = "Exercise (${ex.language}): ${ex.prompt}\nLearner's current draft:\n${session.editorCode}\nGive a 2-sentence Socratic nudge without revealing the full answer."
            val reply = GeminiStudioClient.generateFastLowLatencyResponse(prompt, "Hint-only")
            _activeLessonSession.update { it?.copy(lowLatencyAiHint = reply, isHintLoading = false) }
        }
    }

    fun evaluateCurrentLessonExercise() {
        val session = _activeLessonSession.value ?: return
        val ex = session.node.exercises.getOrNull(session.exerciseIndex) ?: return

        val evalResult = if (ex.options.isNotEmpty()) {
            val isCorrect = session.selectedOption.trim().equals(ex.expectedOutput.trim(), ignoreCase = true)
            SandboxResult(
                success = isCorrect,
                stdout = session.selectedOption,
                passedTests = if (isCorrect) 1 else 0,
                totalTests = 1,
                executionTimeMs = 2L,
                diagnostic = if (isCorrect) ex.explanation else "Selected '${session.selectedOption}', expected '${ex.expectedOutput}'. ${ex.explanation}"
            )
        } else {
            CodeSandboxRunner.executeCode(
                language = ex.language,
                code = session.editorCode,
                expectedOutput = ex.expectedOutput,
                requiredConstructs = ex.requiredConstructs
            )
        }

        _activeLessonSession.update { it?.copy(lastEvaluation = evalResult) }

        viewModelScope.launch {
            if (evalResult.success) {
                AnvilAudioSynthesizer.playAnvilStrike()
                val hintPenalty = if (session.unlockedHintLevel > 0 || session.lowLatencyAiHint != null) 0.5 else 1.0
                val earnedXp = (ex.xpValue * hintPenalty).toLong().coerceAtLeast(5L)
                val grade = if (session.unlockedHintLevel > 0) FsrsGrade.HARD else FsrsGrade.GOOD
                val isLastExercise = session.exerciseIndex >= session.node.exercises.lastIndex

                repository.recordConceptReview(
                    conceptId = ex.conceptId,
                    courseId = session.node.courseId,
                    domain = ex.domain,
                    title = session.node.title,
                    grade = grade,
                    xpDelta = earnedXp + if (isLastExercise) 100L else 0L,
                    activeMinutesDelta = 3L,
                    lessonCompleted = isLastExercise
                )
                _activeLessonSession.update { s ->
                    s?.copy(earnedXpInSession = s.earnedXpInSession + earnedXp)
                }
            } else {
                // Save to Mistake Journal per Part 10.9
                repository.logMistake(
                    exerciseId = ex.id,
                    conceptId = ex.conceptId,
                    courseId = session.node.courseId,
                    promptSummary = ex.prompt,
                    userAnswer = if (ex.options.isNotEmpty()) session.selectedOption else session.editorCode,
                    correctAnswer = ex.expectedOutput,
                    explanation = ex.explanation
                )
            }
        }
    }

    fun advanceLessonExercise() {
        val session = _activeLessonSession.value ?: return
        val nextIndex = session.exerciseIndex + 1
        if (nextIndex >= session.node.exercises.size) {
            _activeLessonSession.value = session.copy(isCompleted = true)
        } else {
            val nextEx = session.node.exercises[nextIndex]
            _activeLessonSession.value = session.copy(
                exerciseIndex = nextIndex,
                editorCode = nextEx.starterCode,
                selectedOption = nextEx.options.firstOrNull() ?: "",
                unlockedHintLevel = 0,
                lowLatencyAiHint = null,
                lastEvaluation = null
            )
        }
    }

    // --- FSRS Direct Concept Review & Mistake Retry ---

    fun gradeFsrsCard(concept: ConceptState, grade: FsrsGrade) {
        viewModelScope.launch {
            repository.recordConceptReview(
                conceptId = concept.conceptId,
                courseId = concept.courseId,
                domain = concept.domain,
                title = concept.title,
                grade = grade,
                xpDelta = 10L,
                activeMinutesDelta = 2L,
                lessonCompleted = false
            )
            _statusBanner.value = "FSRS-5 scheduled '${concept.title}' (${grade.label})."
        }
    }

    fun retryMistakeSuccess(mistake: MistakeEntry) {
        viewModelScope.launch {
            repository.recordMistakeRetrySuccess(mistake.id)
            _statusBanner.value = "Mistake retry recorded for '${mistake.conceptId}'."
        }
    }

    // --- Playground & Snippet Vault ---

    fun setPlaygroundLanguage(lang: String) {
        _playgroundLanguage.value = lang
        _playgroundCode.value = when (lang) {
            "Python" -> "# Python 3.13 Sandbox\nmass = 6\nvelocity = 10\nenergy = 0.5 * mass * (velocity ** 2)\nprint(int(energy))"
            "JavaScript" -> "// JavaScript Sandbox\nconst nums = [1, 2, 3, 4];\nconst sum = nums.map(x => x * 2).reduce((a, b) => a + b, 0);\nconsole.log(sum);"
            "SQL" -> "SELECT symbol FROM elements WHERE atomic_number <= 3 ORDER BY atomic_number;"
            else -> "print(42)"
        }
        _playgroundResult.value = null
    }

    fun setPlaygroundCode(code: String) {
        _playgroundCode.value = code
    }

    fun appendPlaygroundToken(token: String) {
        _playgroundCode.value += token
    }

    fun runPlaygroundSandbox() {
        val result = CodeSandboxRunner.executeCode(
            language = _playgroundLanguage.value,
            code = _playgroundCode.value
        )
        _playgroundResult.value = result
        if (result.success) {
            AnvilAudioSynthesizer.playAnvilStrike()
        }
    }

    fun savePlaygroundToVault(title: String, category: String = "Playground") {
        viewModelScope.launch {
            val res = repository.createSnippet(
                title = title.ifBlank { "${_playgroundLanguage.value} Study Snippet" },
                language = _playgroundLanguage.value,
                code = _playgroundCode.value,
                category = category,
                isFavorite = true
            )
            if (res.isSuccess) {
                _statusBanner.value = "Saved snippet to Firestore Snippet Vault."
            }
        }
    }

    fun deleteSnippet(snippetId: String) {
        viewModelScope.launch {
            repository.deleteSnippet(snippetId)
        }
    }

    // --- Gemini AI Studio & Lyria 3 Actions ---

    fun updateAiTier(tier: AiModelTier) {
        _aiStudio.update { it.copy(selectedTier = tier) }
    }

    fun updateTutorMode(mode: String) {
        _aiStudio.update { it.copy(selectedTutorMode = mode) }
    }

    fun updateTutorPrompt(prompt: String) {
        _aiStudio.update { it.copy(tutorPrompt = prompt) }
    }

    fun askAiTutor() {
        val state = _aiStudio.value
        if (state.tutorPrompt.isBlank()) return
        _aiStudio.update { it.copy(isTutorLoading = true) }
        viewModelScope.launch {
            val reply = when (state.selectedTier) {
                AiModelTier.FAST_LITE ->
                    GeminiStudioClient.generateFastLowLatencyResponse(state.tutorPrompt, state.selectedTutorMode)
                AiModelTier.GENERAL_FLASH ->
                    GeminiStudioClient.generateGeneralTutorResponse(state.tutorPrompt, state.selectedTutorMode)
                AiModelTier.HIGH_THINKING_PRO ->
                    GeminiStudioClient.generateHighThinkingResponse(
                        "[Tutor Mode: ${state.selectedTutorMode}]\n${state.tutorPrompt}"
                    )
            }
            _aiStudio.update { it.copy(tutorResponse = reply, isTutorLoading = false) }
        }
    }

    fun updateSearchQuery(query: String) {
        _aiStudio.update { it.copy(searchQuery = query) }
    }

    fun runGroundedSearch() {
        val query = _aiStudio.value.searchQuery
        if (query.isBlank()) return
        _aiStudio.update { it.copy(isSearchLoading = true) }
        viewModelScope.launch {
            val response = GeminiStudioClient.searchGroundedQuery(query)
            _aiStudio.update { it.copy(searchResult = response, isSearchLoading = false) }
        }
    }

    fun updateImagePrompt(prompt: String) {
        _aiStudio.update { it.copy(imagePrompt = prompt) }
    }

    fun updateImageSize(size: String) {
        _aiStudio.update { it.copy(selectedImageSize = size) }
    }

    fun updateImageAspectRatio(ratio: String) {
        _aiStudio.update { it.copy(selectedAspectRatio = ratio) }
    }

    fun generateDiagramImage() {
        val state = _aiStudio.value
        if (state.imagePrompt.isBlank()) return
        _aiStudio.update { it.copy(isImageLoading = true) }
        viewModelScope.launch {
            val output = GeminiStudioClient.generateHighQualityImage(
                prompt = state.imagePrompt,
                imageSize = state.selectedImageSize,
                aspectRatio = state.selectedAspectRatio
            )
            _aiStudio.update { it.copy(generatedImage = output, isImageLoading = false) }
        }
    }

    fun updateMusicPrompt(prompt: String) {
        _aiStudio.update { it.copy(musicPrompt = prompt) }
    }

    fun toggleFullLengthProTrack(usePro: Boolean) {
        _aiStudio.update { it.copy(useFullLengthProTrack = usePro) }
    }

    fun generateAndPlayFocusMusic(context: Context) {
        val state = _aiStudio.value
        _aiStudio.update { it.copy(isMusicLoading = true) }
        viewModelScope.launch {
            val result = GeminiStudioClient.generateFocusMusic(
                prompt = state.musicPrompt,
                useFullLengthTrack = state.useFullLengthProTrack
            )
            _aiStudio.update { it.copy(generatedMusic = result, isMusicLoading = false) }
            AnvilAudioSynthesizer.playAudioBytesOrFallback(
                context = context,
                audioBytes = result.audioBytes,
                durationSeconds = if (state.useFullLengthProTrack) 6 else 3
            )
        }
    }

    // --- Monthly Individual Profile Report (MIPR) Generation ---

    fun generateMiprReport(useAiNarrative: Boolean = false) {
        viewModelScope.launch {
            val concepts = (conceptStates.value as? UiState.Success)?.data ?: emptyList()
            val profile = (profileState.value as? UiState.Success)?.data
            val customNarrative = if (useAiNarrative && GeminiStudioClient.isApiKeyConfigured()) {
                val conceptSummary = concepts.joinToString { "${it.title} (${it.domain}, accuracy=${it.accuracyPermille / 10}%, S=${it.stability}d)" }
                    .ifBlank { "Initial baseline assessment window" }
                GeminiStudioClient.generateHighThinkingResponse(
                    "Write a concise, non-comparative, evidence-based 3-sentence Monthly Individual Profile Report (MIPR) narrative summary for learner '${profile?.displayName ?: "Learner"}' with concepts: $conceptSummary."
                )
            } else null

            val res = repository.generateMonthlyReport(
                periodLabel = "October 2026",
                concepts = concepts,
                profile = profile,
                customAiNarrative = customNarrative
            )
            if (res.isSuccess) {
                AnvilAudioSynthesizer.playAnvilStrike()
                _statusBanner.value = "Monthly Individual Profile Report (MIPR) generated."
            }
        }
    }

    private companion object {
        const val TAG = "AnvilViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
