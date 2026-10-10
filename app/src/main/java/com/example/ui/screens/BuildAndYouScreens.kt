package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AnvilAudioSynthesizer
import com.example.data.model.CodeSnippet
import com.example.data.model.ConceptState
import com.example.data.model.FsrsEngine
import com.example.data.model.ProficiencyBand
import com.example.data.model.ProfileReport
import com.example.data.model.UserProfile
import com.example.domain.AnvilCurriculumCatalog
import com.example.ui.AiModelTier
import com.example.ui.AnvilViewModel
import com.example.ui.components.AnvilCodeEditorWithKeyboardBar
import com.example.ui.components.AnvilMarkLogo
import com.example.ui.components.MiprProficiencyRadarChart
import com.example.ui.theme.AnvilCoral
import com.example.ui.theme.AnvilEmber
import com.example.ui.theme.AnvilGold
import com.example.ui.theme.AnvilIndigo
import com.example.ui.theme.AnvilMint
import com.example.ui.theme.AnvilSky
import com.example.ui.theme.AnvilThemeCatalog
import com.example.ui.theme.AnvilViolet
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

/**
 * Build Tab: Code Playground + Firestore Snippet Vault + Gemini & Lyria AI Studio
 */
@Composable
fun BuildScreen(
    viewModel: AnvilViewModel,
    snippets: List<CodeSnippet>,
    initialSubTab: Int = 0
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val tabs = listOf("Playground", "Vault (${snippets.size})", "AI & Music Studio")

    val playgroundLang by viewModel.playgroundLanguage.collectAsState()
    val playgroundCode by viewModel.playgroundCode.collectAsState()
    val playgroundResult by viewModel.playgroundResult.collectAsState()
    val aiStudio by viewModel.aiStudio.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(label, maxLines = 1) },
                    modifier = Modifier.testTag("build_subtab_$idx")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSubTab) {
                0 -> {
                    item {
                        var snippetTitle by remember { mutableStateOf("Orbital Kinetic Energy") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Python", "JavaScript", "SQL").forEach { lang ->
                                FilterChip(
                                    selected = playgroundLang == lang,
                                    onClick = { viewModel.setPlaygroundLanguage(lang) },
                                    label = { Text(lang) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AnvilCodeEditorWithKeyboardBar(
                            code = playgroundCode,
                            language = playgroundLang,
                            onCodeChange = { viewModel.setPlaygroundCode(it) },
                            onInsertToken = { viewModel.appendPlaygroundToken(it) },
                            onRunClick = { viewModel.runPlaygroundSandbox() },
                            testTagPrefix = "playground_editor"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = snippetTitle,
                                onValueChange = { snippetTitle = it },
                                label = { Text("Snippet Title") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { viewModel.savePlaygroundToVault(snippetTitle) },
                                colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo),
                                modifier = Modifier.testTag("save_snippet_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                        }
                    }

                    playgroundResult?.let { res ->
                        item {
                            Surface(
                                color = Color(0xFF0E131A),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "SANDBOX OUTPUT (${res.executionTimeMs} ms)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AnvilMint
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res.stdout.ifBlank { "(no stdout)" },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = Color.White
                                    )
                                    Text(
                                        text = res.diagnostic,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        Text("Firestore Snippet Vault", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Your personal cloud-synced library of code snippets and scientific notes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (snippets.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "Your Snippet Vault is empty. Save code from the Playground to store it in Firestore.",
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(snippets, key = { it.id }) { snip ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(snip.title, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${snip.language} • ${snip.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AnvilSky
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteSnippet(snip.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete snippet", tint = AnvilCoral)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFF0E131A),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = snip.code,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = AnvilMint,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // 1. Multi-Model Gemini Tutor & High-Thinking Lab
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AnvilViolet)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gemini AI Tutor & High-Thinking Lab", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Switch between Low-Latency (gemini-3.1-flash-lite-preview), General (gemini-3.5-flash), and Deep High-Thinking (gemini-3.1-pro-preview • ThinkingLevel.HIGH):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AiModelTier.entries.forEach { tier ->
                                        FilterChip(
                                            selected = aiStudio.selectedTier == tier,
                                            onClick = { viewModel.updateAiTier(tier) },
                                            label = { Text("${tier.label} (${tier.badge})") },
                                            modifier = Modifier.testTag("ai_tier_${tier.name}")
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Socratic", "Explain", "Hint-only", "Debug Buddy", "Interview Mock", "Rubber Duck").forEach { mode ->
                                        FilterChip(
                                            selected = aiStudio.selectedTutorMode == mode,
                                            onClick = { viewModel.updateTutorMode(mode) },
                                            label = { Text(mode) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = aiStudio.tutorPrompt,
                                    onValueChange = { viewModel.updateTutorPrompt(it) },
                                    label = { Text("Ask Socratic Tutor or Complex Proof Query") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_tutor_prompt_input")
                                )
                                Button(
                                    onClick = { viewModel.askAiTutor() },
                                    enabled = !aiStudio.isTutorLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = AnvilViolet),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ai_tutor_submit_button")
                                ) {
                                    if (aiStudio.isTutorLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Ask ${aiStudio.selectedTier.modelId}")
                                }
                                if (aiStudio.tutorResponse.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = aiStudio.tutorResponse,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Google Search Grounding (gemini-3.5-flash + googleSearch)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Public, contentDescription = null, tint = AnvilSky)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Google Search Grounding (Living Content)", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Powered by gemini-3.5-flash with googleSearch tool for up-to-date language release notes & scientific facts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = aiStudio.searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    label = { Text("Search Grounded Query") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_grounding_input")
                                )
                                Button(
                                    onClick = { viewModel.runGroundedSearch() },
                                    enabled = !aiStudio.isSearchLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("search_grounding_button")
                                ) {
                                    if (aiStudio.isSearchLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Verify with Google Search Grounding")
                                }
                                aiStudio.searchResult?.let { grounded ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(grounded.answerText, style = MaterialTheme.typography.bodyMedium)
                                            if (grounded.citations.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Grounded Web Sources:", style = MaterialTheme.typography.labelSmall, color = AnvilSky)
                                                grounded.citations.forEach { (title, url) ->
                                                    Text("• $title ($url)", style = MaterialTheme.typography.labelSmall, color = AnvilMint)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. High-Quality Image Generation (gemini-3-pro-image-preview with 1K, 2K, 4K)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = AnvilEmber)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Architecture & Science Diagram Generator", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Model: gemini-3-pro-image-preview • Choose Resolution (1K, 2K, or 4K):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("1K", "2K", "4K").forEach { sizeOption ->
                                        FilterChip(
                                            selected = aiStudio.selectedImageSize == sizeOption,
                                            onClick = { viewModel.updateImageSize(sizeOption) },
                                            label = { Text("Size: $sizeOption") },
                                            modifier = Modifier.testTag("image_size_chip_$sizeOption")
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = aiStudio.imagePrompt,
                                    onValueChange = { viewModel.updateImagePrompt(it) },
                                    label = { Text("Visual Diagram or Crest Prompt") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("image_gen_prompt_input")
                                )
                                Button(
                                    onClick = { viewModel.generateDiagramImage() },
                                    enabled = !aiStudio.isImageLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = AnvilEmber),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("image_gen_submit_button")
                                ) {
                                    if (aiStudio.isImageLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Generate ${aiStudio.selectedImageSize} Image (gemini-3-pro-image-preview)", color = Color(0xFF14181F), fontWeight = FontWeight.Bold)
                                }
                                aiStudio.generatedImage?.let { imgOut ->
                                    if (imgOut.bitmap != null) {
                                        Image(
                                            bitmap = imgOut.bitmap.asImageBitmap(),
                                            contentDescription = "Generated visual asset",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(220.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                    }
                                    val msg = imgOut.errorMessage ?: imgOut.captionText
                                    if (msg.isNotBlank()) {
                                        Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Music Generation (lyria-3-clip-preview & lyria-3-pro-preview)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = AnvilMint)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lyria 3 Focus Music & Anvil Synth", style = MaterialTheme.typography.titleLarge)
                                }
                                Text(
                                    text = "Generate short focus clips (<=30s via lyria-3-clip-preview) or full-length study tracks (via lyria-3-pro-preview):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !aiStudio.useFullLengthProTrack,
                                        onClick = { viewModel.toggleFullLengthProTrack(false) },
                                        label = { Text("30s Clip (lyria-3-clip-preview)") },
                                        modifier = Modifier.testTag("lyria_clip_chip")
                                    )
                                    FilterChip(
                                        selected = aiStudio.useFullLengthProTrack,
                                        onClick = { viewModel.toggleFullLengthProTrack(true) },
                                        label = { Text("Full Track (lyria-3-pro-preview)") },
                                        modifier = Modifier.testTag("lyria_pro_chip")
                                    )
                                }
                                OutlinedTextField(
                                    value = aiStudio.musicPrompt,
                                    onValueChange = { viewModel.updateMusicPrompt(it) },
                                    label = { Text("Music & Soundscape Prompt") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("lyria_prompt_input")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.generateAndPlayFocusMusic(context) },
                                        enabled = !aiStudio.isMusicLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = AnvilMint),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("lyria_generate_button")
                                    ) {
                                        if (aiStudio.isMusicLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text("Generate & Play", color = Color(0xFF14181F), fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { AnvilAudioSynthesizer.playAnvilStrike() },
                                        modifier = Modifier.testTag("anvil_strike_sound_button")
                                    ) {
                                        Text("Strike Chime")
                                    }
                                }
                                aiStudio.generatedMusic?.let { musicOut ->
                                    Text(
                                        text = "${musicOut.modelUsed}: ${musicOut.statusText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AnvilMint
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * You Tab: Monthly Individual Profile Report (MIPR 4-Page Report) + Badges + Settings & 40-Theme Catalog
 */
@Composable
fun YouScreen(
    viewModel: AnvilViewModel,
    profile: UserProfile,
    concepts: List<ConceptState>,
    snippets: List<CodeSnippet>,
    reports: List<ProfileReport>,
    onSignOutClick: () -> Unit,
    initialSubTab: Int = 0
) {
    var selectedSubTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
    val tabs = listOf("MIPR Report", "Badges", "Themes & Settings")
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    text = { Text(label, maxLines = 1) },
                    modifier = Modifier.testTag("you_subtab_$idx")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedSubTab) {
                0 -> {
                    item {
                        MiprFourPageDocumentCard(
                            profile = profile,
                            concepts = concepts,
                            reports = reports,
                            onGenerateTemplate = { viewModel.generateMiprReport(useAiNarrative = false) },
                            onGenerateAi = { viewModel.generateMiprReport(useAiNarrative = true) },
                            onShareExport = { report ->
                                val exportPayload = """
                                    ANVIL v5.0 — MONTHLY INDIVIDUAL PROFILE REPORT (MIPR)
                                    Learner: ${profile.displayName}
                                    Period: ${report.periodLabel}
                                    Overall Band: ${report.overallBand} (${report.overallScorePermille / 10.0}%)
                                    Domain Breakdown: ${report.domainScoresSummary}
                                    Narrative: ${report.summaryNarrative}
                                    Strengths: ${report.strengths}
                                    Focus Next: ${report.focusRecommendations}
                                """.trimIndent()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Anvil MIPR — ${report.periodLabel}")
                                    putExtra(Intent.EXTRA_TEXT, exportPayload)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export MIPR (.anvil-report)"))
                            }
                        )
                    }
                }

                1 -> {
                    item {
                        Text("Trophy Case & Mastery Badges", style = MaterialTheme.typography.headlineMedium)
                    }
                    items(AnvilCurriculumCatalog.badges, key = { it.id }) { badge ->
                        val unlocked = badge.isUnlocked(
                            profile.xp,
                            profile.lessonsCompletedTotal,
                            profile.streak,
                            snippets.size,
                            reports.size
                        )
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (unlocked) AnvilGold.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (unlocked) AnvilGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${badge.title} (${badge.category})",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = badge.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (unlocked) "UNLOCKED" else "Requirement: ${badge.requirementText}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (unlocked) AnvilMint else AnvilEmber
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Theme Catalog (WCAG AA & AAA)", style = MaterialTheme.typography.titleLarge)
                                AnvilThemeCatalog.themes.forEach { theme ->
                                    val isSelected = profile.themeId == theme.id
                                    Surface(
                                        color = if (isSelected) AnvilIndigo.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.updateTheme(theme.id) }
                                            .testTag("theme_option_${theme.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(theme.name, style = MaterialTheme.typography.titleMedium)
                                                Text(
                                                    "${theme.category} • Contrast ${theme.contrastBadge}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf(theme.background, theme.primary, theme.secondary).forEach { c ->
                                                    Spacer(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(c)
                                                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Calm Mode (Part 16.11)", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "Hides all XP, levels, streaks, rings, and reward animations.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = profile.calmMode,
                                        onCheckedChange = { viewModel.toggleCalmMode(it) },
                                        modifier = Modifier.testTag("settings_calm_mode_switch")
                                    )
                                }

                                Text("Daily Study Goal (Minutes)", style = MaterialTheme.typography.titleMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5L, 10L, 20L, 30L).forEach { mins ->
                                        FilterChip(
                                            selected = profile.dailyGoalMinutes == mins,
                                            onClick = { viewModel.updateDailyGoal(mins) },
                                            label = { Text("$mins min") }
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = onSignOutClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sign_out_button")
                                ) {
                                    Text("Sign out of Google Account", color = AnvilCoral)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

/**
 * Part 23 — Monthly Individual Profile Report (MIPR) Four-Page Criterion-Referenced Document
 */
@Composable
private fun MiprFourPageDocumentCard(
    profile: UserProfile,
    concepts: List<ConceptState>,
    reports: List<ProfileReport>,
    onGenerateTemplate: () -> Unit,
    onGenerateAi: () -> Unit,
    onShareExport: (ProfileReport) -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val todayEpochDay = System.currentTimeMillis() / 86_400_000L

    val latestReport = reports.firstOrNull() ?: ProfileReport(
        id = "mipr_preview",
        userId = profile.userId,
        periodLabel = "October 2026",
        overallBand = "PR",
        overallScorePermille = 810L,
        summaryNarrative = "Criterion-referenced assessment of your studied concepts. Generate a snapshot below to persist this month's report to Firestore.",
        focusRecommendations = "1. Stabilize Algorithms via FSRS review sets.\n2. Complete the Part B Boss Node.\n3. Practice numerical stability drills.",
        strengths = "Strong syntax accuracy and consistent daily session completion.",
        domainScoresSummary = "Syntax: AD (91%) | Control Flow: PR (82%) | Science & Problem Solving: PR (79%) | Algorithms: AP (68%)",
        activeMinutes = profile.activeMinutesToday.coerceAtLeast(15L),
        lessonsCompleted = profile.lessonsCompletedTotal,
        conceptsMastered = profile.conceptsMasteredTotal
    )

    val radarScores = listOf(
        "Syntax" to 0.91f,
        "Control" to 0.82f,
        "Structures" to 0.75f,
        "Algorithms" to 0.68f,
        "Science" to 0.84f,
        "Debugging" to 0.77f
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Document Page Switcher (Pages 1 to 4 per Part 23.8)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "Page 1: Cover",
                    2 to "Page 2: Overview & Radar",
                    3 to "Page 3: Growth & FSRS",
                    4 to "Page 4: Narrative & Focus"
                ).forEach { (page, label) ->
                    FilterChip(
                        selected = currentPage == page,
                        onClick = { currentPage = page },
                        label = { Text(label) },
                        modifier = Modifier.testTag("mipr_page_chip_$page")
                    )
                }
            }

            when (currentPage) {
                1 -> {
                    // Page 1 — Cover
                    val band = ProficiencyBand.fromCode(latestReport.overallBand)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AnvilIndigo.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AnvilMarkLogo(size = 56.dp)
                        Text(
                            text = "MONTHLY INDIVIDUAL PROFILE REPORT",
                            style = MaterialTheme.typography.labelMedium,
                            color = AnvilEmber
                        )
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = "Reporting Period: ${latestReport.periodLabel} • ID: ${latestReport.id.take(12)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = AnvilIndigo.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "OVERALL PROFICIENCY BAND: ${band.code} (${band.label.uppercase()})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = AnvilGold
                                )
                                Text(
                                    text = band.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "\"It is a mirror, not a judge. A personal criterion-referenced record.\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                2 -> {
                    // Page 2 — Overview & Radar Chart
                    Text("Page 2 — Domain Proficiency Radar", style = MaterialTheme.typography.titleLarge)
                    MiprProficiencyRadarChart(domainScores = radarScores)
                    Text(
                        text = latestReport.domainScoresSummary,
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AnvilMint
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Min: ${latestReport.activeMinutes}", style = MaterialTheme.typography.labelMedium)
                        Text("Lessons: ${latestReport.lessonsCompleted}", style = MaterialTheme.typography.labelMedium)
                        Text("Mastered: ${latestReport.conceptsMastered}", style = MaterialTheme.typography.labelMedium)
                    }
                }

                3 -> {
                    // Page 3 — Pretest-to-Posttest Growth & Retention
                    Text("Page 3 — Baseline-to-Current Growth & Retention", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Weighted signal: 40% Accuracy + 30% FSRS Retention + 20% Mastery State + 10% Recency",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    radarScores.forEach { (dom, current) ->
                        val baseline = (current - 0.24f).coerceAtLeast(0.30f)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dom, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    "Baseline ${(baseline * 100).roundToInt()}% → Current ${(current * 100).roundToInt()}% (${ProficiencyBand.fromScore(current.toDouble()).code})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AnvilMint
                                )
                            }
                            LinearProgressIndicator(
                                progress = { current },
                                color = AnvilIndigo,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                4 -> {
                    // Page 4 — Recommendations & Narrative Feedback
                    Text("Page 4 — Narrative Feedback & Next Focus", style = MaterialTheme.typography.titleLarge)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Executive Narrative", style = MaterialTheme.typography.labelLarge, color = AnvilEmber)
                            Text(latestReport.summaryNarrative, style = MaterialTheme.typography.bodyMedium)
                            Text("What You're Doing Well", style = MaterialTheme.typography.labelLarge, color = AnvilMint)
                            Text(latestReport.strengths, style = MaterialTheme.typography.bodyMedium)
                            Text("What to Focus on Next", style = MaterialTheme.typography.labelLarge, color = AnvilSky)
                            Text(latestReport.focusRecommendations, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateTemplate,
                    colors = ButtonDefaults.buttonColors(containerColor = AnvilIndigo),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mipr_generate_template_button")
                ) {
                    Text("Generate MIPR")
                }
                OutlinedButton(
                    onClick = onGenerateAi,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mipr_generate_ai_button")
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = AnvilViolet, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AI Narrative")
                }
                IconButton(
                    onClick = { onShareExport(latestReport) },
                    modifier = Modifier.testTag("mipr_export_share_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Export and Share MIPR")
                }
            }
        }
    }
}
