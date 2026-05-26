package com.example.scraply.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.example.scraply.ui.social.ProfileScreen
import com.example.scraply.ui.stamp.StampCameraScreen
import com.example.scraply.ui.stamp.StampCropScreen
import com.example.scraply.ui.stamp.StampDetailsScreen
import com.example.scraply.ui.stamp.StampCaptureViewModel
import com.example.scraply.ui.vm.scraplyViewModel

@Composable
fun ScraplyNavHost(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val currentTab = BottomTab.entries.firstOrNull { tab ->
        currentRoute == tab.route
    }

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
                    onOpenUpload = { navController.navigate(Routes.UPLOAD_STAMP) },
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
                val vm: EditorViewModel = scraplyViewModel()
                ScrapbookEditorScreen(
                    vm = vm,
                    projectId = id,
                    onBack = { navController.popBackStack() },
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
                FeedScreen()
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
        }
    }
}
