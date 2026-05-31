package com.example.scraply.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scraply.R
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.example.scraply.ui.calendar.CalendarDateDetailScreen
import com.example.scraply.ui.calendar.CalendarScreen
import com.example.scraply.ui.calendar.CalendarViewModel
import com.example.scraply.ui.collection.CollectionDetailScreen
import com.example.scraply.ui.collection.CollectionsScreen
import com.example.scraply.ui.collection.CollectionsViewModel
import com.example.scraply.ui.collection.StampDetailScreen
import com.example.scraply.ui.collection.UploadStampScreen
import com.example.scraply.ui.editor.EditorProjectsScreen
import com.example.scraply.ui.editor.EditorViewModel
import com.example.scraply.ui.editor.ScrapbookEditorScreen
import com.example.scraply.ui.notifications.NotificationsScreen
import com.example.scraply.ui.social.CommentsScreen
import com.example.scraply.ui.social.FeedPostScreen
import com.example.scraply.ui.social.FeedScreen
import com.example.scraply.ui.social.ProfilePostsFeedScreen
import com.example.scraply.ui.social.ProfileScreen
import com.example.scraply.ui.stamp.StampCameraScreen
import com.example.scraply.ui.stamp.StampCropScreen
import com.example.scraply.ui.stamp.StampDetailsScreen
import com.example.scraply.ui.stamp.StampCaptureViewModel
import com.example.scraply.ui.settings.SettingsScreen
import com.example.scraply.ui.settings.RecentLikesScreen
import com.example.scraply.ui.settings.RecentCommentsScreen
import com.example.scraply.ui.settings.AboutScreen
import com.example.scraply.ui.social.EditProfileScreen
import com.example.scraply.ui.vm.scraplyViewModel
import kotlinx.coroutines.delay

@Composable
fun ScraplyNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = BottomTab.entries.firstOrNull { tab ->
        currentRoute == tab.route
    }
    var isFeedPostOpening by remember { mutableStateOf(false) }
    var isProfilePostOpening by remember { mutableStateOf(false) }
    var publishedConfirmationPostId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.FEED) {
            isFeedPostOpening = false
        }
        if (currentRoute == Routes.PROFILE) {
            isProfilePostOpening = false
        }
    }

    LaunchedEffect(publishedConfirmationPostId) {
        if (publishedConfirmationPostId != null) {
            delay(2400)
            publishedConfirmationPostId = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (currentTab != null) {
                    ScraplyBottomBar(
                        current = currentTab,
                        onSelect = { tab ->
                            if (tab.route != currentRoute) {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.STAMP) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.STAMP,
                modifier = Modifier.padding(padding),
            ) {
            composable(Routes.STAMP) {
                val vm: StampCaptureViewModel = scraplyViewModel()
                StampCameraScreen(
                    vm = vm,
                    onCaptured = { uri -> navController.navigate(Routes.stampCrop(uri)) },
                    onOpenUpload = { navController.navigate(Routes.UPLOAD_STAMP) },
                )
            }

            composable(
                Routes.STAMP_CROP,
                arguments = listOf(navArgument("sourceUri") { type = NavType.StringType }),
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("sourceUri").orEmpty()
                StampCropScreen(
                    sourceUriEncoded = uri,
                    onBack = { navController.popBackStack() },
                    onCropped = { stampUri ->
                        navController.navigate(Routes.stampDetails(stampUri)) {
                            popUpTo(Routes.STAMP_CAMERA) { inclusive = false }
                        }
                    },
                )
            }

            composable(
                Routes.STAMP_DETAILS,
                arguments = listOf(navArgument("stampUri") { type = NavType.StringType }),
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("stampUri").orEmpty()
                StampDetailsScreen(
                    stampUriEncoded = uri,
                    onBack = { navController.popBackStack() },
                    onRetake = { navController.popBackStack(Routes.STAMP, inclusive = false) },
                    onSaved = { navController.popBackStack(Routes.STAMP, inclusive = false) },
                )
            }

            composable(Routes.COLLECTIONS) {
                val vm: CollectionsViewModel = scraplyViewModel()
                CollectionsScreen(
                    vm = vm,
                    onOpenCollection = { id -> navController.navigate(Routes.collectionDetail(id)) },
                    onOpenCalendar = { navController.navigate(Routes.CALENDAR) },
                )
            }

            composable(Routes.UPLOAD_STAMP) {
                UploadStampScreen(
                    onClose = { navController.popBackStack() },
                    onCropped = { uri -> navController.navigate(Routes.stampDetails(uri)) },
                )
            }

            composable(
                Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("collectionId").orEmpty()
                val vm: CollectionsViewModel = scraplyViewModel()
                CollectionDetailScreen(
                    vm = vm,
                    collectionId = id,
                    onBack = { navController.popBackStack() },
                    onOpenStamp = { stampId -> navController.navigate(Routes.stampDetail(stampId)) },
                )
            }

            composable(
                Routes.STAMP_DETAIL,
                arguments = listOf(navArgument("stampId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("stampId").orEmpty()
                val vm: CollectionsViewModel = scraplyViewModel()
                StampDetailScreen(
                    vm = vm,
                    stampId = id,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.EDITOR_LIST) {
                val vm: EditorViewModel = scraplyViewModel()
                EditorProjectsScreen(
                    vm = vm,
                    onOpenProject = { id -> navController.navigate(Routes.projectEditor(id)) },
                )
            }

            composable(
                Routes.PROJECT_EDITOR,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("projectId").orEmpty()
                val editorListEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.EDITOR_LIST)
                }
                val vm: EditorViewModel = scraplyViewModel(viewModelStoreOwner = editorListEntry)
                ScrapbookEditorScreen(
                    vm = vm,
                    projectId = id,
                    onBack = { navController.popBackStack() },
                    onPublished = { postId ->
                        publishedConfirmationPostId = postId
                        navController.navigate(Routes.FEED) {
                            popUpTo(Routes.STAMP) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                        navController.navigate(Routes.feedPost(postId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.CALENDAR) {
                val vm: CalendarViewModel = scraplyViewModel()
                CalendarScreen(
                    vm = vm,
                    onOpenDate = { day -> navController.navigate(Routes.calendarDate(day)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                Routes.CALENDAR_DATE,
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType }),
            ) { backStackEntry ->
                val day = backStackEntry.arguments?.getLong("epochDay") ?: 0L
                val vm: CalendarViewModel = scraplyViewModel()
                CalendarDateDetailScreen(
                    vm = vm,
                    epochDay = day,
                    onBack = { navController.popBackStack() },
                    onOpenStamp = { id -> navController.navigate(Routes.stampDetail(id)) },
                )
            }

            composable(Routes.FEED) {
                FeedScreen(
                    onOpenPost = { id ->
                        if (!isFeedPostOpening) {
                            isFeedPostOpening = true
                            navController.navigate(Routes.feedPost(id)) {
                                popUpTo(Routes.FEED) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(
                Routes.FEED_POST,
                arguments = listOf(
                    navArgument("postId") { type = NavType.StringType },
                    navArgument("showComments") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                val showComments = backStackEntry.arguments?.getBoolean("showComments") ?: false
                FeedPostScreen(
                    postId = postId,
                    showComments = showComments,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        if (!navController.popBackStack(Routes.FEED, inclusive = false)) {
                            navController.navigate(Routes.FEED) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(
                Routes.COMMENTS,
                arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                CommentsScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenPost = { id ->
                        if (!isProfilePostOpening) {
                            isProfilePostOpening = true
                            navController.navigate(Routes.profilePosts(id)) {
                                popUpTo(Routes.PROFILE) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(
                Routes.PROFILE_POSTS,
                arguments = listOf(navArgument("postId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId").orEmpty()
                val profileEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.PROFILE)
                }
                val vm: com.example.scraply.ui.social.ProfileViewModel =
                    scraplyViewModel(viewModelStoreOwner = profileEntry)
                ProfilePostsFeedScreen(
                    vm = vm,
                    initialPostId = postId,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        if (!navController.popBackStack(Routes.PROFILE, inclusive = false)) {
                            navController.navigate(Routes.PROFILE) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { id, showComments ->
                        navController.navigate(Routes.feedPost(id, showComments))
                    },
                )
            }

            composable(
                Routes.SETTINGS,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                val profileEntry = remember(it) {
                    navController.getBackStackEntry(Routes.PROFILE)
                }
                val profileVm: com.example.scraply.ui.social.ProfileViewModel =
                    scraplyViewModel(viewModelStoreOwner = profileEntry)
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = {
                        navController.navigate(Routes.EDIT_PROFILE)
                    },
                    onOpenRecentLikes = {
                        navController.navigate(Routes.RECENT_LIKES)
                    },
                    onOpenRecentComments = {
                        navController.navigate(Routes.RECENT_COMMENTS)
                    },
                    onOpenAbout = {
                        navController.navigate(Routes.ABOUT)
                    },
                    onSignOut = {
                        profileVm.signOut()
                    },
                )
            }

            composable(
                Routes.RECENT_LIKES,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                RecentLikesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId ->
                        navController.navigate(Routes.feedPost(postId))
                    }
                )
            }

            composable(
                Routes.RECENT_COMMENTS,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                RecentCommentsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { postId ->
                        navController.navigate(Routes.feedPost(postId))
                    }
                )
            }

            composable(
                Routes.EDIT_PROFILE,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                val profileEntry = remember(it) {
                    navController.getBackStackEntry(Routes.PROFILE)
                }
                val profileVm: com.example.scraply.ui.social.ProfileViewModel =
                    scraplyViewModel(viewModelStoreOwner = profileEntry)
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = profileVm
                )
            }

            composable(
                Routes.ABOUT,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(350)
                    ) + fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(350)
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            }
        }

        if (publishedConfirmationPostId != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                ),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(R.drawable.seal_fill_icon),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.editor_published_toast),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
