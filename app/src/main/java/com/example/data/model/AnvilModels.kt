package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Apprentice Smith",
    val xp: Long = 0L,
    val level: Long = 1L,
    val streak: Long = 0L,
    val streakFreezes: Long = 2L,
    val dailyGoalMinutes: Long = 10L,
    val activeMinutesToday: Long = 0L,
    val lessonsCompletedTotal: Long = 0L,
    val conceptsMasteredTotal: Long = 0L,
    val activeCourseId: String = "python-core",
    val themeId: String = "anvil-dark",
    val calmMode: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val restDaysCsv: String = "SUN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "displayName" to displayName.take(80).ifBlank { "Apprentice Smith" },
        "xp" to xp.coerceIn(0L, 100000000L),
        "level" to level.coerceIn(1L, 1000L),
        "streak" to streak.coerceIn(0L, 36500L),
        "streakFreezes" to streakFreezes.coerceIn(0L, 10L),
        "dailyGoalMinutes" to dailyGoalMinutes.coerceIn(1L, 480L),
        "activeMinutesToday" to activeMinutesToday.coerceIn(0L, 1440L),
        "lessonsCompletedTotal" to lessonsCompletedTotal.coerceIn(0L, 100000L),
        "conceptsMasteredTotal" to conceptsMasteredTotal.coerceIn(0L, 100000L),
        "activeCourseId" to activeCourseId.take(64).ifBlank { "python-core" },
        "themeId" to themeId.take(64).ifBlank { "anvil-dark" },
        "calmMode" to calmMode,
        "onboardingCompleted" to onboardingCompleted,
        "restDaysCsv" to restDaysCsv.take(64),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ConceptState(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conceptId: String = "",
    val courseId: String = "python-core",
    val domain: String = "Syntax and Semantics",
    val title: String = "Variables & Bindings",
    val stability: Double = 2.5,
    val difficulty: Double = 3.5,
    val reps: Long = 1L,
    val lapses: Long = 0L,
    val accuracyPermille: Long = 850L,
    val masteryState: String = "Practicing",
    val lastReviewEpochDay: Long = 20736L,
    val dueEpochDay: Long = 20739L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "conceptId" to conceptId.take(128).ifBlank { "concept_default" },
        "courseId" to courseId.take(64).ifBlank { "python-core" },
        "domain" to domain.take(100).ifBlank { "Syntax and Semantics" },
        "title" to title.take(160).ifBlank { "Core Concept" },
        "stability" to stability.coerceIn(0.1, 36500.0),
        "difficulty" to difficulty.coerceIn(1.0, 10.0),
        "reps" to reps.coerceIn(0L, 100000L),
        "lapses" to lapses.coerceIn(0L, 100000L),
        "accuracyPermille" to accuracyPermille.coerceIn(0L, 1000L),
        "masteryState" to masteryState.take(40).ifBlank { "Practicing" },
        "lastReviewEpochDay" to lastReviewEpochDay.coerceIn(0L, 1000000L),
        "dueEpochDay" to dueEpochDay.coerceIn(0L, 1000000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CodeSnippet(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val language: String = "Python",
    val code: String = "",
    val category: String = "Playground",
    val isFavorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "title" to title.take(120).ifBlank { "Untitled Snippet" },
        "language" to language.take(40).ifBlank { "Python" },
        "code" to code.take(20000).ifBlank { "# Empty snippet" },
        "category" to category.take(40).ifBlank { "Playground" },
        "isFavorite" to isFavorite,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class MistakeEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val exerciseId: String = "",
    val conceptId: String = "",
    val courseId: String = "python-core",
    val promptSummary: String = "",
    val userAnswer: String = "",
    val correctAnswer: String = "",
    val explanation: String = "",
    val retryCount: Long = 0L,
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "exerciseId" to exerciseId.take(128).ifBlank { "ex_default" },
        "conceptId" to conceptId.take(128).ifBlank { "concept_default" },
        "courseId" to courseId.take(64).ifBlank { "python-core" },
        "promptSummary" to promptSummary.take(500).ifBlank { "Exercise prompt" },
        "userAnswer" to userAnswer.take(5000),
        "correctAnswer" to correctAnswer.take(5000).ifBlank { "Expected solution" },
        "explanation" to explanation.take(2000).ifBlank { "Review concept rules." },
        "retryCount" to retryCount.coerceIn(0L, 1000L),
        "resolved" to resolved,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class ProfileReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val periodLabel: String = "October 2026",
    val overallBand: String = "PR",
    val overallScorePermille: Long = 800L,
    val summaryNarrative: String = "",
    val focusRecommendations: String = "",
    val strengths: String = "",
    val domainScoresSummary: String = "",
    val activeMinutes: Long = 0L,
    val lessonsCompleted: Long = 0L,
    val conceptsMastered: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "periodLabel" to periodLabel.take(80).ifBlank { "Current Period" },
        "overallBand" to if (overallBand in listOf("AD", "PR", "AP", "DE", "BE")) overallBand else "AP",
        "overallScorePermille" to overallScorePermille.coerceIn(0L, 1000L),
        "summaryNarrative" to summaryNarrative.take(4000).ifBlank { "Consistent progress recorded." },
        "focusRecommendations" to focusRecommendations.take(4000).ifBlank { "Continue daily FSRS reviews." },
        "strengths" to strengths.take(4000).ifBlank { "Strong foundational accuracy." },
        "domainScoresSummary" to domainScoresSummary.take(4000).ifBlank { "Syntax: PR (80%)" },
        "activeMinutes" to activeMinutes.coerceIn(0L, 100000L),
        "lessonsCompleted" to lessonsCompleted.coerceIn(0L, 100000L),
        "conceptsMastered" to conceptsMastered.coerceIn(0L, 100000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

enum class ProficiencyBand(
    val code: String,
    val label: String,
    val description: String
) {
    ADVANCED(
        "AD",
        "Advanced",
        "Learner exceeds expectations: deep conceptual understanding, transfer skills, reliable retention at long intervals."
    ),
    PROFICIENT(
        "PR",
        "Proficient",
        "Learner has established fundamental understanding and can apply it via performance tasks."
    ),
    APPROACHING(
        "AP",
        "Approaching Proficiency",
        "Learner has developed basic fundamental knowledge and skills but requires stabilization."
    ),
    DEVELOPING(
        "DE",
        "Developing",
        "Learner possesses minimal curriculum knowledge and requires targeted practice."
    ),
    BEGINNING(
        "BE",
        "Beginning",
        "Learner struggles systematically and has not yet acquired foundational knowledge."
    );

    companion object {
        fun fromScore(score: Double): ProficiencyBand = when {
            score >= 0.90 -> ADVANCED
            score >= 0.75 -> PROFICIENT
            score >= 0.60 -> APPROACHING
            score >= 0.40 -> DEVELOPING
            else -> BEGINNING
        }

        fun fromCode(code: String): ProficiencyBand =
            entries.firstOrNull { it.code == code } ?: APPROACHING
    }
}

enum class FsrsGrade(val value: Int, val label: String) {
    AGAIN(1, "Again"),
    HARD(2, "Hard"),
    GOOD(3, "Good"),
    EASY(4, "Easy")
}

object FsrsEngine {
    private const val DECAY = -0.5
    private const val FACTOR = 19.0 / 81.0

    /**
     * Retrievability R(t, S) = (1 + FACTOR * t / S) ^ DECAY
     */
    fun retrievability(elapsedDays: Double, stability: Double): Double {
        val safeS = max(0.1, stability)
        val safeT = max(0.0, elapsedDays)
        return (1.0 + FACTOR * safeT / safeS).pow(DECAY).coerceIn(0.0, 1.0)
    }

    /**
     * Next interval I(r, S) = (S / FACTOR) * (r^(1/DECAY) - 1), clamped to [1, 365]
     */
    fun nextIntervalDays(targetRetention: Double = 0.9, stability: Double): Long {
        val safeS = max(0.1, stability)
        val r = targetRetention.coerceIn(0.70, 0.97)
        val raw = (safeS / FACTOR) * (r.pow(1.0 / DECAY) - 1.0)
        return raw.roundToInt().toLong().coerceIn(1L, 365L)
    }

    fun updateConceptAfterReview(
        existing: ConceptState?,
        userId: String,
        conceptId: String,
        courseId: String,
        domain: String,
        title: String,
        grade: FsrsGrade,
        todayEpochDay: Long
    ): ConceptState {
        val prevS = existing?.stability ?: 1.8
        val prevD = existing?.difficulty ?: 4.5
        val prevReps = existing?.reps ?: 0L
        val prevLapses = existing?.lapses ?: 0L
        val prevAcc = (existing?.accuracyPermille ?: 750L).toDouble() / 1000.0

        val newD = (prevD - 0.45 * (grade.value - 3)).coerceIn(1.0, 10.0)
        val newS = when (grade) {
            FsrsGrade.AGAIN -> max(0.5, prevS * 0.45)
            FsrsGrade.HARD -> max(1.0, prevS * 1.25)
            FsrsGrade.GOOD -> max(1.5, prevS * (1.85 + (10.0 - newD) * 0.08))
            FsrsGrade.EASY -> max(2.5, prevS * (2.55 + (10.0 - newD) * 0.12))
        }.coerceIn(0.5, 365.0)

        val attemptScore = when (grade) {
            FsrsGrade.AGAIN -> 0.0
            FsrsGrade.HARD -> 0.65
            FsrsGrade.GOOD -> 0.92
            FsrsGrade.EASY -> 1.0
        }
        val newAcc = if (prevReps == 0L) attemptScore else (prevAcc * 0.75 + attemptScore * 0.25)
        val newAccPermille = (newAcc * 1000.0).roundToInt().toLong().coerceIn(0L, 1000L)
        val newReps = prevReps + 1L
        val newLapses = if (grade == FsrsGrade.AGAIN) prevLapses + 1L else prevLapses
        val interval = nextIntervalDays(0.90, newS)
        val mastery = when {
            grade == FsrsGrade.AGAIN && prevReps >= 2 -> "Regressed"
            newS >= 21.0 && newAccPermille >= 880L -> "Mastered"
            newS >= 7.0 && newAccPermille >= 750L -> "Proficient"
            newReps >= 2L -> "Practicing"
            else -> "Introduced"
        }

        return ConceptState(
            id = conceptId,
            userId = userId,
            conceptId = conceptId,
            courseId = courseId,
            domain = domain,
            title = title,
            stability = newS,
            difficulty = newD,
            reps = newReps,
            lapses = newLapses,
            accuracyPermille = newAccPermille,
            masteryState = mastery,
            lastReviewEpochDay = todayEpochDay,
            dueEpochDay = todayEpochDay + interval,
            createdAt = existing?.createdAt
        )
    }

    /**
     * Part 23.5 Band assignment algorithm:
     * 40% Accuracy + 30% FSRS Retention + 20% Mastery State + 10% Recency
     */
    fun computeConceptProficiencyScore(concept: ConceptState, todayEpochDay: Long): Double {
        val accuracySignal = (concept.accuracyPermille.toDouble() / 1000.0).coerceIn(0.0, 1.0)
        val elapsed = max(0L, todayEpochDay - concept.lastReviewEpochDay).toDouble()
        val retentionSignal = retrievability(elapsed, concept.stability)
        val masterySignal = when (concept.masteryState) {
            "Mastered" -> 1.0
            "Proficient" -> 0.85
            "Refreshing" -> 0.75
            "Practicing" -> 0.62
            "Introduced" -> 0.45
            "Regressed" -> 0.35
            "Decaying" -> 0.30
            else -> 0.20
        }
        val recencySignal = max(0.0, 1.0 - min(30.0, elapsed) / 30.0)
        return (0.40 * accuracySignal + 0.30 * retentionSignal + 0.20 * masterySignal + 0.10 * recencySignal)
            .coerceIn(0.0, 1.0)
    }
}

object GamificationRules {
    /**
     * Part 16.3: XP_to_reach(level) = round(60 * (level - 1)^1.7)
     */
    fun xpRequiredForLevel(level: Long): Long {
        if (level <= 1L) return 0L
        return (60.0 * (level - 1L).toDouble().pow(1.7)).roundToInt().toLong()
    }

    fun computeLevelForXp(xp: Long): Long {
        var lvl = 1L
        while (lvl < 100L && xp >= xpRequiredForLevel(lvl + 1L)) {
            lvl++
        }
        return lvl
    }

    fun wrightRankForLevel(level: Long): String = when {
        level >= 30L -> "Grand Wright"
        level >= 22L -> "Master Wright"
        level >= 15L -> "Adept"
        level >= 10L -> "Artisan"
        level >= 6L -> "Journeyman"
        level >= 3L -> "Tinkerer"
        else -> "Apprentice"
    }
}
