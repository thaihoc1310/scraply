package com.example.scraply.ui.navigation

import com.example.scraply.R

object Routes {
    const val STAMP = "stamp"
    const val COLLECTIONS = "collections"
    const val EDITOR_LIST = "editor"
    const val CALENDAR = "calendar"
    const val FEED = "feed"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val RECENT_LIKES = "recent_likes"
    const val RECENT_COMMENTS = "recent_comments"
    const val EDIT_PROFILE = "edit_profile"
    const val ABOUT = "about"
    const val COMMENTS = "feed/comments/{postId}"
    const val FEED_POST = "feed/post/{postId}?showComments={showComments}"
    const val PROFILE_POSTS = "profile/posts/{postId}"

    const val STAMP_CAMERA = "stamp/camera"
    const val STAMP_CROP = "stamp/crop/{sourceUri}"
    const val STAMP_DETAILS = "stamp/details/{stampUri}"
    const val UPLOAD_STAMP = "upload_stamp"
    const val COLLECTION_DETAIL = "collections/{collectionId}"
    const val STAMP_DETAIL = "stamps/{stampId}"
    const val PROJECT_EDITOR = "editor/project/{projectId}"
    const val CALENDAR_DATE = "calendar/date/{epochDay}"

    fun stampCrop(sourceUri: String) = "stamp/crop/${android.net.Uri.encode(sourceUri)}"
    fun stampDetails(stampUri: String) = "stamp/details/${android.net.Uri.encode(stampUri)}"
    fun collectionDetail(id: String) = "collections/$id"
    fun stampDetail(id: String) = "stamps/$id"
    fun projectEditor(id: String) = "editor/project/$id"
    fun calendarDate(epochDay: Long) = "calendar/date/$epochDay"
    fun comments(postId: String) = "feed/comments/${android.net.Uri.encode(postId)}"
    fun feedPost(postId: String, showComments: Boolean = false) =
        "feed/post/${android.net.Uri.encode(postId)}?showComments=$showComments"
    fun profilePosts(postId: String) = "profile/posts/${android.net.Uri.encode(postId)}"
}

enum class BottomTab(val route: String, val label: String, val labelResId: Int) {
    STAMP(Routes.STAMP, "Stamp", R.string.tab_stamp),
    COLLECTIONS(Routes.COLLECTIONS, "Collection", R.string.tab_collection),
    EDITOR(Routes.EDITOR_LIST, "Editor", R.string.tab_editor),
    FEED(Routes.FEED, "Feed", R.string.tab_feed),
    PROFILE(Routes.PROFILE, "Profile", R.string.tab_profile),
}
