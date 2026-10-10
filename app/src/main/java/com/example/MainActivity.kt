package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.UserProfile
import com.example.data.repository.AnvilRepository
import com.example.domain.AnvilCurriculumCatalog
import com.example.ui.AnvilViewModel
import com.example.ui.PrimaryTab
import com.example.ui.UiState
import com.example.ui.components.AnvilCommandBar
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BuildScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.PracticeScreen
import com.example.ui.screens.YouScreen
import com.example.ui.screens.signOutAnvil
import com.example.ui.theme.AnvilTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnvilRootApp()
        }
    }
}

@Composable
fun AnvilRootApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        AnvilTheme(themeId = "anvil-dark") {
            AuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
    } else {
        AuthenticatedAnvilWorkspace(
            currentUserId = user.uid,
            onSignedOut = { currentUser = null }
        )
    }
}

@Composable
fun AuthenticatedAnvilWorkspace(
    currentUserId: String,
    onSignedOut: () -> Unit
) {
    val viewModel: AnvilViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AnvilViewModel(AnvilRepository(db), currentUserId)
            }
        }
    )

    val profileUiState by viewModel.profileState.collectAsStateWithLifecycle()
    val conceptsUiState by viewModel.conceptStates.collectAsStateWithLifecycle()
    val snippetsUiState by viewModel.snippetsState.collectAsStateWithLifecycle()
    val mistakesUiState by viewModel.mistakesState.collectAsStateWithLifecycle()
    val reportsUiState by viewModel.reportsState.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeLessonSession by viewModel.activeLessonSession.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val searchDialogOpen by viewModel.searchDialogOpen.collectAsStateWithLifecycle()
    val localSearchQuery by viewModel.localSearchQuery.collectAsStateWithLifecycle()

    val profile = (profileUiState as? UiState.Success)?.data ?: UserProfile(id = currentUserId, userId = currentUserId)
    val concepts = (conceptsUiState as? UiState.Success)?.data ?: emptyList()
    val snippets = (snippetsUiState as? UiState.Success)?.data ?: emptyList()
    val mistakes = (mistakesUiState as? UiState.Success)?.data ?: emptyList()
    val reports = (reportsUiState as? UiState.Success)?.data ?: emptyList()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var buildSubTabTarget by remember { mutableIntStateOf(0) }
    var youSubTabTarget by remember { mutableIntStateOf(0) }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    if (selectedTab != PrimaryTab.HOME && activeLessonSession == null) {
        BackHandler {
            viewModel.selectTab(PrimaryTab.HOME)
        }
    }

    AnvilTheme(themeId = profile.themeId) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (activeLessonSession == null) {
                        AnvilCommandBar(
                            calmMode = profile.calmMode,
                            onSearchClick = { viewModel.setSearchDialogOpen(true) },
                            onAiTutorClick = {
                                buildSubTabTarget = 2
                                viewModel.selectTab(PrimaryTab.BUILD)
                            },
                            onSettingsClick = {
                                youSubTabTarget = 2
                                viewModel.selectTab(PrimaryTab.YOU)
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!isWideScreen && activeLessonSession == null) {
                        NavigationBar {
                            PrimaryTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = iconForTab(tab),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen && activeLessonSession == null) {
                        NavigationRail {
                            PrimaryTab.entries.forEach { tab ->
                                NavigationRailItem(
                                    selected = selectedTab == tab,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = iconForTab(tab),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            PrimaryTab.HOME -> HomeScreen(
                                viewModel = viewModel,
                                profile = profile,
                                concepts = concepts,
                                reports = reports
                            )
                            PrimaryTab.LEARN -> LearnScreen(
                                viewModel = viewModel,
                                profile = profile,
                                concepts = concepts,
                                activeSession = activeLessonSession
                            )
                            PrimaryTab.PRACTICE -> PracticeScreen(
                                viewModel = viewModel,
                                concepts = concepts,
                                mistakes = mistakes
                            )
                            PrimaryTab.BUILD -> BuildScreen(
                                viewModel = viewModel,
                                snippets = snippets,
                                initialSubTab = buildSubTabTarget
                            )
                            PrimaryTab.YOU -> YouScreen(
                                viewModel = viewModel,
                                profile = profile,
                                concepts = concepts,
                                snippets = snippets,
                                reports = reports,
                                onSignOutClick = {
                                    signOutAnvil(
                                        credentialManager = credentialManager,
                                        onSignOutComplete = onSignedOut,
                                        scope = scope
                                    )
                                },
                                initialSubTab = youSubTabTarget
                            )
                        }
                    }
                }
            }

            if (searchDialogOpen) {
                LocalFullTextSearchDialog(
                    query = localSearchQuery,
                    onQueryChange = { viewModel.setLocalSearchQuery(it) },
                    onDismiss = { viewModel.setSearchDialogOpen(false) },
                    onSelectCourseNode = { courseId, node ->
                        viewModel.setSearchDialogOpen(false)
                        viewModel.updateCourse(courseId)
                        viewModel.selectTab(PrimaryTab.LEARN)
                        viewModel.startLessonNode(node)
                    }
                )
            }
        }
    }
}

private fun iconForTab(tab: PrimaryTab): ImageVector = when (tab) {
    PrimaryTab.HOME -> Icons.Default.Home
    PrimaryTab.LEARN -> Icons.Default.School
    PrimaryTab.PRACTICE -> Icons.Default.FitnessCenter
    PrimaryTab.BUILD -> Icons.Default.Build
    PrimaryTab.YOU -> Icons.Default.Person
}

@Composable
private fun LocalFullTextSearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectCourseNode: (String, com.example.domain.SkillNode) -> Unit
) {
    val allNodes = remember {
        AnvilCurriculumCatalog.courses.flatMap { c -> c.nodes.map { c.id to it } }
    }
    val matches = remember(query) {
        if (query.isBlank()) allNodes
        else allNodes.filter { (_, node) ->
            node.title.contains(query, ignoreCase = true) ||
                node.objective.contains(query, ignoreCase = true) ||
                node.domain.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anvil Full-Text Search") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search lessons, concepts, science, or SQL...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                matches.take(5).forEach { (courseId, node) ->
                    TextButton(
                        onClick = { onSelectCourseNode(courseId, node) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${node.title} (${node.domain})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
