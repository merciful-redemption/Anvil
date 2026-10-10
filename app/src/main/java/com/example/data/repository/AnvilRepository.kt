package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CodeSnippet
import com.example.data.model.ConceptState
import com.example.data.model.FsrsEngine
import com.example.data.model.FsrsGrade
import com.example.data.model.GamificationRules
import com.example.data.model.MistakeEntry
import com.example.data.model.ProficiencyBand
import com.example.data.model.ProfileReport
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.math.roundToInt

class AnvilRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(defaultDisplayName: String = "Apprentice Smith"): Result<UserProfile> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                existingSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid, displayName = defaultDisplayName)
            } else {
                val initial = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = defaultDisplayName.take(80).ifBlank { "Apprentice Smith" }
                )
                docRef.set(initial.toCreateMap(uid)).await()
                initial
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateUserPreferences(
        displayName: String? = null,
        dailyGoalMinutes: Long? = null,
        activeCourseId: String? = null,
        themeId: String? = null,
        calmMode: Boolean? = null,
        onboardingCompleted: Boolean? = null,
        restDaysCsv: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val created = UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = displayName ?: "Apprentice Smith",
                    dailyGoalMinutes = dailyGoalMinutes ?: 10L,
                    activeCourseId = activeCourseId ?: "python-core",
                    themeId = themeId ?: "anvil-dark",
                    calmMode = calmMode ?: false,
                    onboardingCompleted = onboardingCompleted ?: true,
                    restDaysCsv = restDaysCsv ?: "SUN"
                )
                docRef.set(created.toCreateMap(uid)).await()
            } else {
                val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
                displayName?.let { updates["displayName"] = it.take(80).ifBlank { "Apprentice Smith" } }
                dailyGoalMinutes?.let { updates["dailyGoalMinutes"] = it.coerceIn(1L, 480L) }
                activeCourseId?.let { updates["activeCourseId"] = it.take(64) }
                themeId?.let { updates["themeId"] = it.take(64) }
                calmMode?.let { updates["calmMode"] = it }
                onboardingCompleted?.let { updates["onboardingCompleted"] = it }
                restDaysCsv?.let { updates["restDaysCsv"] = it.take(64) }
                docRef.update(updates).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeConceptStates(): Flow<List<ConceptState>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/concept_states"
        emitAll(
            db.collection("users").document(uid).collection("concept_states")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ConceptState::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun recordConceptReview(
        conceptId: String,
        courseId: String,
        domain: String,
        title: String,
        grade: FsrsGrade,
        xpDelta: Long,
        activeMinutesDelta: Long = 2L,
        lessonCompleted: Boolean = false,
        todayEpochDay: Long = System.currentTimeMillis() / 86_400_000L
    ): Result<ConceptState> = runCatching {
        val uid = requireUserId()
        val conceptRef = db.collection("users").document(uid).collection("concept_states").document(conceptId)
        try {
            val snap = conceptRef.get().await()
            val existing = if (snap.exists()) {
                snap.toObject(ConceptState::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null

            val updated = FsrsEngine.updateConceptAfterReview(
                existing = existing,
                userId = uid,
                conceptId = conceptId,
                courseId = courseId,
                domain = domain,
                title = title,
                grade = grade,
                todayEpochDay = todayEpochDay
            )

            if (existing == null) {
                conceptRef.set(updated.toCreateMap(uid)).await()
            } else {
                conceptRef.update(
                    mapOf(
                        "courseId" to updated.courseId,
                        "domain" to updated.domain,
                        "title" to updated.title,
                        "stability" to updated.stability,
                        "difficulty" to updated.difficulty,
                        "reps" to updated.reps,
                        "lapses" to updated.lapses,
                        "accuracyPermille" to updated.accuracyPermille,
                        "masteryState" to updated.masteryState,
                        "lastReviewEpochDay" to updated.lastReviewEpochDay,
                        "dueEpochDay" to updated.dueEpochDay,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Also update learner XP, level, active minutes, and streak on UserProfile
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val profile = userSnap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid)
                val newXp = (profile.xp + xpDelta.coerceAtLeast(0L)).coerceAtMost(100000000L)
                val newLevel = GamificationRules.computeLevelForXp(newXp)
                val newStreak = if (profile.streak == 0L) 1L else profile.streak
                val newMinutes = (profile.activeMinutesToday + activeMinutesDelta).coerceAtMost(1440L)
                val newLessons = if (lessonCompleted) profile.lessonsCompletedTotal + 1L else profile.lessonsCompletedTotal
                val newMastered = if (updated.masteryState in listOf("Proficient", "Mastered") &&
                    existing?.masteryState !in listOf("Proficient", "Mastered")
                ) {
                    profile.conceptsMasteredTotal + 1L
                } else {
                    profile.conceptsMasteredTotal
                }
                userRef.update(
                    mapOf(
                        "xp" to newXp,
                        "level" to newLevel,
                        "streak" to newStreak,
                        "activeMinutesToday" to newMinutes,
                        "lessonsCompletedTotal" to newLessons,
                        "conceptsMasteredTotal" to newMastered,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            updated
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, conceptRef.path)
            throw e
        }
    }

    fun observeSnippets(): Flow<List<CodeSnippet>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/snippets"
        emitAll(
            db.collection("users").document(uid).collection("snippets")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(CodeSnippet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createSnippet(
        title: String,
        language: String,
        code: String,
        category: String = "Playground",
        isFavorite: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val snippetId = "snip_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("snippets").document(snippetId)
        val snippet = CodeSnippet(
            id = snippetId,
            userId = uid,
            title = title,
            language = language,
            code = code,
            category = category,
            isFavorite = isFavorite
        )
        try {
            docRef.set(snippet.toCreateMap(uid)).await()
            snippetId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getSnippetById(ownerUid: String, snippetId: String): Result<CodeSnippet?> = runCatching {
        val docRef = db.collection("users").document(ownerUid).collection("snippets").document(snippetId)
        try {
            val snap = docRef.get().await()
            snap.toObject(CodeSnippet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun deleteSnippet(snippetId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("snippets").document(snippetId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    fun observeMistakes(): Flow<List<MistakeEntry>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/mistakes"
        emitAll(
            db.collection("users").document(uid).collection("mistakes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(MistakeEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun logMistake(
        exerciseId: String,
        conceptId: String,
        courseId: String,
        promptSummary: String,
        userAnswer: String,
        correctAnswer: String,
        explanation: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val mistakeId = "mst_${exerciseId.replace(Regex("[^a-zA-Z0-9_-]"), "_")}".take(64)
        val docRef = db.collection("users").document(uid).collection("mistakes").document(mistakeId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val entry = MistakeEntry(
                    id = mistakeId,
                    userId = uid,
                    exerciseId = exerciseId,
                    conceptId = conceptId,
                    courseId = courseId,
                    promptSummary = promptSummary,
                    userAnswer = userAnswer,
                    correctAnswer = correctAnswer,
                    explanation = explanation,
                    retryCount = 0L,
                    resolved = false
                )
                docRef.set(entry.toCreateMap(uid)).await()
            } else {
                docRef.update(
                    mapOf(
                        "userAnswer" to userAnswer.take(5000),
                        "resolved" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            mistakeId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun recordMistakeRetrySuccess(mistakeId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("mistakes").document(mistakeId)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val currentRetries = snap.getLong("retryCount") ?: 0L
                val nextRetries = currentRetries + 1L
                docRef.update(
                    mapOf(
                        "retryCount" to nextRetries,
                        "resolved" to (nextRetries >= 2L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeReports(): Flow<List<ProfileReport>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/reports"
        emitAll(
            db.collection("users").document(uid).collection("reports")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ProfileReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun generateMonthlyReport(
        periodLabel: String,
        concepts: List<ConceptState>,
        profile: UserProfile?,
        customAiNarrative: String? = null,
        todayEpochDay: Long = System.currentTimeMillis() / 86_400_000L
    ): Result<ProfileReport> = runCatching {
        val uid = requireUserId()
        val reportId = "mipr_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("users").document(uid).collection("reports").document(reportId)

        val domainGroups = concepts.groupBy { it.domain }
        val domainScores = if (domainGroups.isNotEmpty()) {
            domainGroups.mapValues { (_, list) ->
                list.map { FsrsEngine.computeConceptProficiencyScore(it, todayEpochDay) }.average()
            }
        } else {
            mapOf(
                "Syntax and Semantics" to 0.78,
                "Control Flow" to 0.74,
                "Data Structures" to 0.68,
                "Algorithms" to 0.64,
                "Functions and Modularity" to 0.76,
                "Testing and Debugging" to 0.70,
                "Problem Solving" to 0.72
            )
        }

        val overallScore = domainScores.values.average().coerceIn(0.0, 1.0)
        val overallBand = ProficiencyBand.fromScore(overallScore)
        val overallPermille = (overallScore * 1000.0).roundToInt().toLong().coerceIn(0L, 1000L)

        val sortedDomains = domainScores.entries.sortedByDescending { it.value }
        val topStrengths = sortedDomains.take(2).joinToString("; ") { (dom, sc) ->
            "$dom (${ProficiencyBand.fromScore(sc).code} — ${(sc * 100).roundToInt()}%)"
        }
        val focusAreas = sortedDomains.takeLast(2).reversed().joinToString("; ") { (dom, sc) ->
            "Stabilize $dom (${ProficiencyBand.fromScore(sc).code} — ${(sc * 100).roundToInt()}%) via FSRS review sets and Practice Gyms"
        }
        val domainSummary = sortedDomains.joinToString(" | ") { (dom, sc) ->
            "$dom: ${ProficiencyBand.fromScore(sc).code} (${(sc * 100).roundToInt()}%)"
        }

        val narrative = customAiNarrative?.takeIf { it.isNotBlank() }
            ?: "During $periodLabel, you achieved an overall ${overallBand.label} (${overallBand.code}) profile with a ${(overallScore * 100).roundToInt()}% composite index across ${concepts.size.coerceAtLeast(1)} tracked concepts. Your FSRS-5 stability shows reliable retention in your strongest domains while highlighting clear next steps for deliberate practice."

        val report = ProfileReport(
            id = reportId,
            userId = uid,
            periodLabel = periodLabel,
            overallBand = overallBand.code,
            overallScorePermille = overallPermille,
            summaryNarrative = narrative,
            focusRecommendations = focusAreas.ifBlank { "Complete 3 daily FSRS review blocks and 1 Boss node." },
            strengths = topStrengths.ifBlank { "Consistent lesson completion and syntax accuracy." },
            domainScoresSummary = domainSummary.ifBlank { "Syntax and Semantics: PR (78%)" },
            activeMinutes = profile?.activeMinutesToday?.coerceAtLeast(15L) ?: 15L,
            lessonsCompleted = profile?.lessonsCompletedTotal ?: 1L,
            conceptsMastered = profile?.conceptsMasteredTotal ?: concepts.count { it.masteryState in listOf("Proficient", "Mastered") }.toLong()
        )

        try {
            docRef.set(report.toCreateMap(uid)).await()
            report
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
