package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConceptState
import com.example.data.model.FsrsEngine
import com.example.data.model.FsrsGrade
import com.example.data.model.MistakeEntry
import com.example.data.model.UserProfile
import com.example.domain.AnvilCurriculumCatalog
import com.example.ui.ActiveLessonSession
import com.example.ui.AnvilViewModel
import com.example.ui.components.AnvilCodeEditorWithKeyboardBar
import com.example.ui.theme.AnvilCoral
import com.example.ui.theme.AnvilEmber
import com.example.ui.theme.AnvilGold
import com.example.ui.theme.AnvilIndigo
import com.example.ui.theme.AnvilMint
import com.example.ui.theme.AnvilSky
import com.example.ui.theme.AnvilViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Learn Tab: Winding Skill Graph (DAG) + Focus Mode Lesson Screen (Parts 5.12, 8.7, 10.2)
 */
@Composable
fun LearnScreen(
    viewModel: AnvilViewModel,
    profile: UserProfile,
    concepts: List<ConceptState>,
    activeSession: ActiveLessonSession?
) {
    if (activeSession != null) {
        BackHandler { viewModel.closeLessonSession() }
        FocusModeLessonOverlay(
            session = activeSession,
            calmMode = profile.calmMode,
            onClose = { viewModel.closeLessonSession() },
            onCodeChange = { viewModel.updateLessonEditorCode(it) },
            onInsertToken = { viewModel.insertTokenInLessonEditor(it) },
            onSelectOption = { viewModel.selectLessonOption(it) },
            onUnlockHint = { viewModel.unlockNextHint() },
            onRequestAiHint = { viewModel.requestLowLatencyExerciseAiHint() },
            onCheckAnswer = { viewModel.evaluateCurrentLessonExercise() },
            onNextExercise = { viewModel.advanceLessonExercise() }
        )
        return
    }

    val currentCourse = viewModel.currentCourse(profile)
    val masteredConceptIds = concepts.map { it.conceptId }.toSet()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Text(
                text = "CURRICULUM TRACKS (TIER 1–3 & SCIENCE)",
                style = MaterialTheme.typography.labelMedium,
                color = AnvilEmber
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnvilCurriculumCatalog.courses.forEach { track ->
                    FilterChip(
                        selected = track.id == currentCourse.id,
                        onClick = { viewModel.updateCourse(track.id) },
                        label = { Text("${track.badgeMonogram} • ${track.title}") },
                        modifier = Modifier.testTag("course_chip_${track.id}")
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = currentCourse.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentCourse.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(currentCourse.nodes, key = { it.id }) { node ->
            val isCompleted = node.conceptId in masteredConceptIds
            val isBoss = node.type == "Boss"

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBoss) AnvilViolet.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("skill_node_${node.id}")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> AnvilMint
                                            isBoss -> AnvilViolet
                                            else -> AnvilIndigo
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isCompleted -> Icons.Default.CheckCircle
                                        isBoss -> Icons.Default.Shield
                                        else -> Icons.Default.PlayArrow
                                    },
                                    contentDescription = node.type,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${node.moduleId} • ${node.type.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnvilSky
                                )
                                Text(
                                    text = node.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${node.estMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = node.objective,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Domain: ${node.domain}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AnvilEmber
                        )
                        Button(
                            onClick = { viewModel.startLessonNode(node) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBoss) AnvilViolet else AnvilIndigo
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("start_node_${node.id}")
                        ) {
                            Text(if (isBoss) "Attempt Boss / Test-Out" else if (isCompleted) "Practice Again" else "Start Lesson")
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun FocusModeLessonOverlay(
    session: ActiveLessonSession,
    calmMode: Boolean,
    onClose: () -> Unit,
    onCodeChange: (String) -> Unit,
    onInsertToken: (String) -> Unit,
    onSelectOption: (String) -> Unit,
    onUnlockHint: () -> Unit,
    onRequestAiHint: () -> Unit,
    onCheckAnswer: () -> Unit,
    onNextExercise: () -> Unit
) {
    val exercise = session.node.exercises.getOrNull(session.exerciseIndex)
    val totalCount = session.node.exercises.size.coerceAtLeast(1)
    val progress = (session.exerciseIndex + 1).toFloat() / totalCount

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Focus Mode Header (Part 5.12: only progress, close, and hint)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("lesson_close_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Focus Lesson")
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = AnvilIndigo,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
                Text(
                    text = "${session.exerciseIndex + 1}/$totalCount",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (session.isCompleted || exercise == null) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Lesson Complete",
                            tint = AnvilMint,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Strike Complete — ${session.node.title}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        if (!calmMode) {
                            Text(
                                text = "+${session.earnedXpInSession + 100L} XP • Concept scheduled in FSRS-5",
                                style = MaterialTheme.typography.titleMedium,
                                color = AnvilGold
                            )
                        }
                        // Part 8.8 Surprise Card
                        Surface(
                            color = AnvilIndigo.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "SURPRISE CARD",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnvilSky
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.node.surpriseFact,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lesson_finish_button")
                        ) {
                            Text("Return to Skill Path")
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    color = AnvilEmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${exercise.type.uppercase()} • ${exercise.domain}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AnvilEmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.prompt,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (exercise.options.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        exercise.options.forEach { option ->
                            Surface(
                                color = if (session.selectedOption == option) {
                                    AnvilIndigo.copy(alpha = 0.22f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectOption(option) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = session.selectedOption == option,
                                        onClick = { onSelectOption(option) }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = option,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    AnvilCodeEditorWithKeyboardBar(
                        code = session.editorCode,
                        language = exercise.language,
                        onCodeChange = onCodeChange,
                        onInsertToken = onInsertToken,
                        onRunClick = onCheckAnswer,
                        testTagPrefix = "lesson_editor"
                    )
                }
            }

            // Hint Ladder (Part 12.5) + Low-Latency AI Nudge (gemini-3.1-flash-lite-preview)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onUnlockHint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lesson_hint_ladder_button")
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AnvilGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hint Ladder (${session.unlockedHintLevel}/${exercise.hints.size})")
                    }

                    OutlinedButton(
                        onClick = onRequestAiHint,
                        enabled = !session.isHintLoading,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lesson_ai_fast_hint_button")
                    ) {
                        if (session.isHintLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AnvilViolet, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Bolt Nudge")
                    }
                }

                if (session.unlockedHintLevel > 0) {
                    Column(
                        modifier = Modifier.padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        exercise.hints.take(session.unlockedHintLevel).forEach { hintText ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = hintText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                if (!session.lowLatencyAiHint.isNullOrBlank()) {
                    Surface(
                        color = AnvilViolet.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Low-Latency AI Hint (gemini-3.1-flash-lite-preview)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AnvilViolet
                            )
                            Text(
                                text = session.lowLatencyAiHint,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Evaluation Feedback Banner (Part 8.5)
            session.lastEvaluation?.let { eval ->
                item {
                    Surface(
                        color = if (eval.success) AnvilMint.copy(alpha = 0.18f) else AnvilCoral.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (eval.success) "Correct • Tests ${eval.passedTests}/${eval.totalTests}" else "Needs Adjustment • Tests ${eval.passedTests}/${eval.totalTests}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (eval.success) AnvilMint else AnvilCoral
                            )
                            if (eval.stdout.isNotBlank()) {
                                Text(
                                    text = "Stdout: ${eval.stdout}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = eval.diagnostic,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCheckAnswer,
                        colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lesson_check_button")
                    ) {
                        Text("Check / Run")
                    }
                    if (session.lastEvaluation != null) {
                        Button(
                            onClick = onNextExercise,
                            colors = ButtonDefaults.buttonColors(containerColor = AnvilMint),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lesson_continue_button")
                        ) {
                            Text("Continue", color = Color(0xFF14181F), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Practice Hub + Science & Big-O Gym + Interview & Career + Mistake Journal (Parts 13 & 17)
 */
@Composable
fun PracticeScreen(
    viewModel: AnvilViewModel,
    concepts: List<ConceptState>,
    mistakes: List<MistakeEntry>
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("FSRS Queue", "Science & Big-O", "Interview Prep", "Mistakes (${mistakes.count { !it.resolved }})")
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(title, maxLines = 1) },
                    modifier = Modifier.testTag("practice_subtab_$idx")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedSubTab) {
                0 -> {
                    item {
                        Text(
                            text = "FSRS-5 Spaced Repetition Queue",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Formula: R(t, S) = (1 + 19/81 * t / S)^(-0.5). Rate recall to schedule next optimal interval.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (concepts.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No Concept Cards Scheduled Yet",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Complete an exercise in Learn or seed a concept review below to start FSRS-5 tracking.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.gradeFsrsCard(
                                                ConceptState(
                                                    conceptId = "py_variables",
                                                    courseId = "python-core",
                                                    domain = "Syntax and Semantics",
                                                    title = "Variables & Bindings"
                                                ),
                                                FsrsGrade.GOOD
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo)
                                    ) {
                                        Text("Review 'Variables & Bindings' Now")
                                    }
                                }
                            }
                        }
                    } else {
                        items(concepts, key = { it.id.ifBlank { it.conceptId } }) { concept ->
                            val elapsed = (todayEpochDay - concept.lastReviewEpochDay).coerceAtLeast(0L).toDouble()
                            val r = (FsrsEngine.retrievability(elapsed, concept.stability) * 100).roundToInt()
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(concept.title, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${concept.domain} • State: ${concept.masteryState}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AnvilSky
                                            )
                                        }
                                        Text(
                                            text = "R=$r% • S=${(concept.stability * 10).roundToInt() / 10.0}d",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AnvilMint
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FsrsGrade.entries.forEach { grade ->
                                            OutlinedButton(
                                                onClick = { viewModel.gradeFsrsCard(concept, grade) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(grade.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Science & Big-O Complexity Interactive Lab
                    item {
                        ScienceAndBigOLabCard()
                    }
                }

                2 -> {
                    // Interview & Career Hub (Part 17)
                    item {
                        Text(
                            text = "Interview & Career Drills",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Practice algorithmic, systems, and numerical stability drills.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(AnvilCurriculumCatalog.interviewDrills, key = { it.id }) { drill ->
                        var showSolution by remember { mutableStateOf(false) }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${drill.drillType.uppercase()} • ${drill.domain}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AnvilEmber
                                    )
                                    Text(
                                        text = drill.difficulty,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AnvilViolet
                                    )
                                }
                                Text(drill.title, style = MaterialTheme.typography.titleLarge)
                                Text(drill.prompt, style = MaterialTheme.typography.bodyMedium)
                                Surface(
                                    color = Color(0xFF0E131A),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = drill.starterOrSnippet,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = AnvilMint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                OutlinedButton(onClick = { showSolution = !showSolution }) {
                                    Text(if (showSolution) "Hide Key Insight" else "Reveal Key Insight & Solution")
                                }
                                if (showSolution) {
                                    Text(
                                        text = "Key Insight: ${drill.expectedKeyInsight}\n${drill.sampleSolution}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AnvilMint
                                    )
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Mistake Journal (Part 10.9)
                    item {
                        Text(
                            text = "Mistake Journal",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Incorrect or hinted items are logged here and clear after two successful retry sessions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (mistakes.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "Zero unresolved mistakes! Any missed exercises in Learn will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(mistakes, key = { it.id.ifBlank { it.exerciseId } }) { mistake ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (mistake.resolved) "RESOLVED • ${mistake.conceptId}" else "NEEDS RETRY (${mistake.retryCount}/2) • ${mistake.conceptId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mistake.resolved) AnvilMint else AnvilCoral
                                    )
                                    Text(mistake.promptSummary, style = MaterialTheme.typography.titleMedium)
                                    Text("Expected: ${mistake.correctAnswer}", fontFamily = JetBrainsMonoFontFamily, color = AnvilMint)
                                    Text(mistake.explanation, style = MaterialTheme.typography.bodyMedium)
                                    if (!mistake.resolved) {
                                        Button(
                                            onClick = { viewModel.retryMistakeSuccess(mistake) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo)
                                        ) {
                                            Text("Complete Retry Session (+1)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScienceAndBigOLabCard() {
    var inputSizeN by remember { mutableFloatStateOf(64f) }
    var planetMassRatio by remember { mutableFloatStateOf(1.0f) }
    var planetRadiusRatio by remember { mutableFloatStateOf(1.0f) }

    val n = inputSizeN.roundToInt().coerceAtLeast(2)
    val oLogN = (ln(n.toDouble()) / ln(2.0)).roundToInt()
    val oN = n
    val oNLogN = (n * (ln(n.toDouble()) / ln(2.0))).roundToInt()
    val oNSquared = n * n

    val earthEscapeKmS = 11.186
    val escapeVelocity = earthEscapeKmS * sqrt(planetMassRatio.toDouble() / planetRadiusRatio.coerceAtLeast(0.1f).toDouble())

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = AnvilMint)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Orbital Physics & Escape Velocity Simulator", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "v_e = sqrt(2GM / R) • Adjust planetary mass M and radius R (relative to Earth):",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("Mass Ratio (M / M_Earth): ${(planetMassRatio * 10).roundToInt() / 10.0}x")
                Slider(value = planetMassRatio, onValueChange = { planetMassRatio = it }, valueRange = 0.2f..10f)
                Text("Radius Ratio (R / R_Earth): ${(planetRadiusRatio * 10).roundToInt() / 10.0}x")
                Slider(value = planetRadiusRatio, onValueChange = { planetRadiusRatio = it }, valueRange = 0.2f..5f)
                Surface(
                    color = AnvilIndigo.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Computed Escape Velocity: ${(escapeVelocity * 100).roundToInt() / 100.0} km/s",
                        style = MaterialTheme.typography.titleMedium,
                        color = AnvilMint,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Big-O Complexity Lab (N = $n)", style = MaterialTheme.typography.titleLarge)
                Slider(value = inputSizeN, onValueChange = { inputSizeN = it }, valueRange = 4f..256f)
                Text("• O(log N) Binary Search: $oLogN ops", fontFamily = JetBrainsMonoFontFamily, color = AnvilMint)
                Text("• O(N) Linear Scan: $oN ops", fontFamily = JetBrainsMonoFontFamily, color = AnvilSky)
                Text("• O(N log N) Powersort: $oNLogN ops", fontFamily = JetBrainsMonoFontFamily, color = AnvilEmber)
                Text("• O(N²) Quadratic Bubble: $oNSquared ops", fontFamily = JetBrainsMonoFontFamily, color = AnvilCoral)
            }
        }
    }
}
