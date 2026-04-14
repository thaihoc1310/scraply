# Scraply — Functional Requirements Document

> **Version:** 1.0  
> **Last updated:** 2026-04-14  
> **Status:** Draft — awaiting review

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Navigation Structure](#2-navigation-structure)
3. [Stamp Camera](#3-stamp-camera)
4. [Upload Stamp](#4-upload-stamp)
5. [Collections](#5-collections)
6. [Scrapbook Editor](#6-scrapbook-editor)
7. [Calendar](#7-calendar)
8. [Social Network](#8-social-network)
9. [Offline / Online Behavior](#9-offline--online-behavior)
10. [Technical Stack](#10-technical-stack)
11. [Data Model](#11-data-model)

---

## 1. Product Overview

**Scraply** is a creative mobile app for Android that lets users capture photos as decorative "stamps," organize them into collections, compose them into scrapbook pages alongside decorative assets, and share finished scrapbooks on a built-in social feed.

### Core Loop

```
Capture / Upload → Crop to frame → Name & save stamp → Organize in collections
                                                             ↓
                                       Calendar view ← stamps by date
                                                             ↓
                              Scrapbook editor → Compose page → Export / Publish
                                                                      ↓
                                                          Social feed → Interact
```

### Target Platform

| Attribute        | Value                        |
|------------------|------------------------------|
| Platform         | Android                      |
| Language          | Kotlin                       |
| UI Framework     | Jetpack Compose              |
| Minimum SDK      | API 24 (Android 7.0 Nougat)  |
| Backend          | Firebase                     |

---

## 2. Navigation Structure

The app uses a persistent bottom navigation bar with **four tabs**:

| Tab          | Icon     | Destination                        |
|--------------|----------|------------------------------------|
| **Stamp**    | Paw/star | Live camera with frame overlay     |
| **Collection** | Book   | Collections list                   |
| **Editor**   | Easel    | Scrapbook projects list            |
| **Calendar** | Grid     | Monthly calendar view              |

The **social feed** is accessed from a separate entry point (e.g., a profile/globe icon in a top bar or a dedicated screen accessible from the Calendar or Collection tab header). It is intentionally not a bottom-nav tab to keep the core creative loop front and center.

### Acceptance Criteria

- AC-2.1: All four tabs are always visible and tappable.
- AC-2.2: The currently selected tab is visually highlighted.
- AC-2.3: Switching tabs preserves each tab's scroll position and state.

---

## 3. Stamp Camera

The **Stamp** tab opens a full-screen camera viewfinder with a decorative frame overlay. The user captures a photo, which is automatically cropped to the frame shape.

### 3.1 Frame Overlay

- A decorative frame is rendered on top of the live camera preview.
- **Launch frame:** postage-stamp with perforated edges (the only frame at v1 launch).
- The architecture must support adding additional frame shapes (circle, heart, polaroid, etc.) in future releases.
- All frames are **free** — no premium gating or in-app purchases anywhere in the app.

### 3.2 Camera Controls

| Control            | Behavior                                                        |
|--------------------|-----------------------------------------------------------------|
| Capture button     | Takes a photo; proceeds to the crop-preview screen.             |
| Pinch-to-zoom      | Adjusts camera zoom level (1.0x default). Zoom level displayed. |
| Camera flip        | Toggles between rear and front camera.                          |

### 3.3 Post-Capture Flow

After the user presses the capture button, the following two-step flow occurs:

**Step 1 — Crop Preview**

- The captured photo is shown cropped within the selected frame shape.
- The user can drag and pinch-to-zoom to reposition the photo within the frame.

**Step 2 — Stamp Details**

- The cropped stamp preview is displayed at the top.
- Two optional text fields appear:
  - **Title** — placeholder: "Give it a title" (optional).
  - **Caption** — placeholder: "What do you see?" (optional).
- Two action buttons:
  - **Retake** — discards the current capture and returns to the camera viewfinder.
  - **Save to Book** — saves the stamp to the "All Stamps" default collection and persists it locally.

### Acceptance Criteria

- AC-3.1: The frame overlay renders correctly on both front and rear cameras at all supported zoom levels.
- AC-3.2: Pinch-to-zoom adjusts the camera zoom smoothly; the current zoom level (e.g., "1.0x", "2.0x") is displayed on screen.
- AC-3.3: After capture, the user can reposition the photo within the frame via drag and pinch.
- AC-3.4: Title and Caption fields are optional; the stamp saves successfully even if both are left blank.
- AC-3.5: "Retake" discards the photo and returns to the live viewfinder without saving anything.
- AC-3.6: "Save to Book" persists the stamp image to local storage and adds a record to the "All Stamps" collection.
- AC-3.7: The stamp's `createdAt` timestamp is recorded at the moment of capture (used by the Calendar feature).

---

## 4. Upload Stamp

An alternate stamp-creation path that uses an existing photo from the device gallery instead of the live camera.

### 4.1 Entry Point

- Accessible from the **Collections** screen via a gallery-import icon (top-left corner).
- Opens a modal/screen titled **"Upload Stamp"**.

### 4.2 Flow

1. A placeholder state is shown: "Pick a photo to cut a stamp."
2. The user taps **"Choose Photo"** to open the device photo picker.
3. The selected photo is displayed full-screen within the frame overlay.
4. The user can **drag** and **pinch-to-zoom** to position the desired area inside the frame.
5. The user taps **"Cut Stamp"** to crop the photo.
6. The same **Stamp Details** form from Section 3.3 Step 2 appears (Title, Caption, Retake → re-pick, Save to Book).

### Acceptance Criteria

- AC-4.1: The "Choose Photo" button launches the system photo picker.
- AC-4.2: The selected image can be repositioned via drag and pinch-to-zoom before cutting.
- AC-4.3: "Cut Stamp" crops the image to the active frame shape and proceeds to the details form.
- AC-4.4: The resulting stamp is indistinguishable from a camera-captured stamp in collections and the editor.
- AC-4.5: A "Close" button dismisses the upload flow without saving.

---

## 5. Collections

The **Collection** tab displays a scrollable list of collection cards. Stamps are organized into a mandatory default collection and unlimited user-created custom collections.

### 5.1 Default Collection — "All Stamps"

- Automatically created and always appears first.
- Every stamp the user creates (camera or upload) is added here automatically.
- Cannot be renamed or deleted.
- Shows: name ("All Stamps"), stamp count, and a horizontal thumbnail preview of recent stamps.
- Labeled with a **"Default"** badge.

### 5.2 Custom Collections

- Created via the **"+"** button in the top-right corner of the Collections screen.
- User provides a name when creating.
- There is **no limit** on the number of custom collections.
- There is **no limit** on the number of stamps per collection.
- A stamp **can belong to multiple collections** simultaneously (many-to-many relationship). It always remains in "All Stamps" regardless.
- Each custom collection card shows:
  - A **"Custom"** badge.
  - Collection name, stamp count, horizontal thumbnail preview.
  - A **"..."** overflow menu with two actions: **Rename** and **Delete**.
  - An expand arrow to open the full collection grid.
- If a collection has no stamps, the card shows a "No stamps yet" placeholder.

### 5.3 Collection Detail View

- Tapping a collection (or its expand arrow) opens a grid view of all stamps in that collection.
- Header shows collection name and a back button.
- Stamps are displayed as frame-shaped thumbnails with their title label beneath.

### 5.4 Stamp Management (within any collection view)

- Tapping a stamp opens a detail/edit view.
- Editable fields: **Title**, **Caption**.
- **Delete** action removes the stamp entirely from all collections and local storage.
- Re-crop is a **stretch goal** for a future release.

### 5.5 Adding Stamps to Collections

- When saving a new stamp ("Save to Book"), the user can optionally choose which additional custom collections to add it to (beyond the automatic "All Stamps" inclusion).
- From a collection detail view, a user can add existing stamps from "All Stamps" into the current custom collection.

### Acceptance Criteria

- AC-5.1: "All Stamps" is always present and cannot be renamed or deleted.
- AC-5.2: New stamps appear in "All Stamps" immediately after saving.
- AC-5.3: Custom collections can be created, renamed, and deleted without limit.
- AC-5.4: Deleting a custom collection does **not** delete the stamps within it; they remain in "All Stamps" and any other collections.
- AC-5.5: A stamp can appear in multiple custom collections simultaneously.
- AC-5.6: Deleting a stamp removes it from all collections.
- AC-5.7: Renaming a stamp or editing its caption updates the stamp everywhere it appears.

---

## 6. Scrapbook Editor

The **Editor** tab is the creative workspace where users compose scrapbook pages by placing stamps and decorative assets on a canvas.

### 6.1 Projects List

- The Editor tab shows a scrollable list of scrapbook project cards.
- No status distinction (no Active/Draft/Archived) — projects are a flat list sorted by last-modified date (newest first).
- Each project card shows:
  - Project name.
  - Stamp count (number of stamp elements placed on the canvas).
  - Thumbnail preview of the canvas.
  - A **"..."** overflow menu: **Rename**, **Delete**.
- **"+"** button (top-right) to create a new project. User provides a name.

### 6.2 Canvas

- Fixed-size canvas (device-screen aspect ratio).
- Background is selectable from the **Backgrounds** panel:

| Background  | Description                              |
|-------------|------------------------------------------|
| Paper       | Subtle paper texture (default selection) |
| Soft Paper  | Lighter, softer paper texture            |
| Plain       | Solid white / off-white                  |
| Grid        | Grid-lined notebook style                |

### 6.3 Placeable Elements

Elements are added to the canvas via the **Edit Assets** panel, which has three tabs: **Backgrounds**, **Assets**, **Text**.

#### 6.3.1 Assets Tab

| Category         | Count (v1) | Description                                                                       |
|------------------|------------|-----------------------------------------------------------------------------------|
| Tape Pack        | 6          | Decorative washi / masking tape strips                                            |
| Sticker Pack     | 7          | Floral and decorative sticker graphics                                            |
| Polaroid Frame   | 1          | Places a stamp inside a Polaroid-style frame; auto-crops to 1:1 with bottom margin |
| Paper Cuts       | 5          | Torn paper, kraft paper, and textured paper scraps                                |

- Tapping an asset adds it to the center of the canvas.
- The Polaroid Frame prompts the user to pick a stamp from their collections first, then places it.

#### 6.3.2 Text Tab

| Feature          | Description                                                     |
|------------------|-----------------------------------------------------------------|
| Add Text Layer   | Creates a new editable text element on the canvas.              |
| Font Presets     | Selectable fonts (v1: Classic Serif, Fz Kingshare). Single-select radio. |
| Text Styles      | Color chips, stroke weight, and shadow presets.                 |
| Captions         | Pre-made reusable text blocks and quote-style templates.        |

#### 6.3.3 Stamps

- Via the toolbar **"add stamp"** button, the user opens a picker showing all their stamps (from any collection).
- Tapping a stamp places it on the canvas.

### 6.4 Element Manipulation

All placed elements (stamps, stickers, tape, paper cuts, text) support the following **finger gestures**:

| Gesture             | Action                                    |
|---------------------|-------------------------------------------|
| Single-finger drag  | Move the element on the canvas.           |
| Two-finger pinch    | Resize the element (scale up/down).       |
| Two-finger rotate   | Rotate the element to any angle.          |
| Tap                 | Select the element (shows selection handles). |
| Long-press or menu  | Bring to front / send to back (z-order).  |

- A selected element shows resize/rotate handles and a delete button.
- Elements have a z-order (layer stack). Users can reorder elements to control which appears on top.

### 6.5 Toolbar

The editor toolbar (top of the canvas screen) provides:

| Button        | Action                                                       |
|---------------|--------------------------------------------------------------|
| Back arrow    | Returns to the project list (auto-saves the current state).  |
| Share/Export  | Opens the export flow (see Section 6.7).                     |
| Add Stamp     | Opens the stamp picker to place a stamp on the canvas.       |
| Adjustments   | Opens the Edit Assets panel (Backgrounds / Assets / Text).   |
| Palette       | Quick-access color picker for selected text elements.        |

### 6.6 Undo

- An **Undo** button is available in the editor.
- Each tap reverses the most recent canvas action (add, move, resize, rotate, delete, reorder).
- Minimum requirement: single-step undo. Multi-step undo stack is a recommended enhancement.

### 6.7 Export

- **Save as image**: renders the canvas to a PNG or JPG file and saves it to the device gallery.
- **Publish to social feed**: exports the image and creates a published scrapbook post (requires online connectivity; see Section 8).
- The project remains editable after export — exporting does not lock or archive it.

### Acceptance Criteria

- AC-6.1: Users can create, rename, and delete scrapbook projects.
- AC-6.2: All four background types render correctly on the canvas.
- AC-6.3: All asset categories (tape, stickers, polaroid, paper cuts) can be placed on the canvas.
- AC-6.4: Text layers can be created with selectable fonts, styles, and colors.
- AC-6.5: All placed elements support move, resize, rotate, and z-order reordering via gestures.
- AC-6.6: Undo reverses the last action correctly.
- AC-6.7: Export produces a correct raster image (PNG/JPG) of the full canvas.
- AC-6.8: The project auto-saves when the user navigates away (back button or tab switch).
- AC-6.9: The Polaroid Frame asset prompts the user to pick a stamp before placement.

---

## 7. Calendar

The **Calendar** tab provides a monthly diary view showing which stamps were captured on which dates.

### 7.1 Layout

- Header displays the current month and year (e.g., "04 2026 April") with left/right arrows to navigate months.
- A **Compact / Full** toggle switches between:
  - **Compact**: smaller cells, minimal stamp thumbnails.
  - **Full**: larger cells with more visible stamp previews.
- The calendar grid shows a standard 7-column (Sun–Sat) layout.

### 7.2 Stamp Thumbnails on Dates

- If one or more stamps were captured on a given date, a small thumbnail (or stacked thumbnails for multiple stamps) appears in that date's cell.
- Dates with no stamps show an empty cell.

### 7.3 Date Detail

- Tapping a date opens a detail view listing all stamps captured on that day.
- Each stamp shows its frame-shaped thumbnail, title, and caption.
- Tapping a stamp from this list opens the stamp detail/edit view (same as Section 5.4).

### Acceptance Criteria

- AC-7.1: The calendar defaults to the current month on open.
- AC-7.2: Left/right arrows navigate to previous/next months correctly.
- AC-7.3: Stamp thumbnails appear on the correct dates based on the stamp's `createdAt` timestamp.
- AC-7.4: Tapping a date with stamps opens a list of those stamps.
- AC-7.5: Compact/Full toggle switches the layout without losing state.
- AC-7.6: The calendar works fully offline using locally stored stamp data.

---

## 8. Social Network

A lightweight social layer where users can publish scrapbooks, discover others' work, and interact.

### 8.1 Authentication

- **Google Sign-In** via Firebase Authentication.
- Users must be signed in to access any social features.
- Sign-in is **not required** for the core creative features (stamp capture, collections, editor, calendar).
- A sign-in prompt appears when the user first tries to access a social feature (feed, profile, publish).

### 8.2 User Profile

| Field              | Editable | Description                                                  |
|--------------------|----------|--------------------------------------------------------------|
| Username           | Yes      | Unique display name.                                         |
| Avatar             | Yes      | Profile picture (uploaded or taken with camera).             |
| Bio                | Yes      | Short text description.                                      |
| Follower count     | No       | Computed from Follow relationships.                          |
| Following count    | No       | Computed from Follow relationships.                          |
| Published gallery  | No       | Grid of scrapbooks the user has published.                   |

- Tapping another user's name/avatar anywhere in the app navigates to their profile.
- A **Follow / Unfollow** button is shown on other users' profiles.

### 8.3 Feed

- The feed shows a scrollable list of published scrapbook posts.
- **Ordering logic**:
  1. Posts from **followed users** appear first, sorted by most recent.
  2. Posts from **non-followed users** appear after, sorted by most recent.
- Each post card displays:
  - The scrapbook image (full-width or large thumbnail).
  - Author's avatar, username.
  - Like count, comment count.
  - Action buttons: Like, Comment, Save, Fork.

### 8.4 Interactions

| Action    | Behavior                                                                                      |
|-----------|-----------------------------------------------------------------------------------------------|
| **Like**  | Toggles a like on the post. Updates the like count in real time.                              |
| **Comment** | Opens a comment thread. Users can post text comments. Comments are listed chronologically.  |
| **Save**  | Bookmarks the post for the current user. Saved posts are accessible from a "Saved" section in the user's profile. |
| **Fork**  | Copies the published scrapbook's full canvas data into the current user's Editor as a new project. The forked project is fully editable. The original post is unaffected. |

### 8.5 Publishing Flow

1. From the Editor's export/share flow, the user selects "Publish to Feed."
2. The canvas is rendered to an image and uploaded to Firebase Storage.
3. A `PublishedScrapbook` document is created in Firestore with the image URL, canvas data (for forking), and metadata.
4. The post appears in the global feed.

### 8.6 Scope Exclusions (v1)

The following are intentionally **out of scope** for the initial release:

- Private accounts.
- Block / report functionality.
- Content moderation system.
- Direct messaging.
- Notifications (push or in-app).
- Hashtags or search-by-tag.

These may be introduced in future versions based on user feedback.

### Acceptance Criteria

- AC-8.1: Google Sign-In flow completes successfully and creates/retrieves a user profile.
- AC-8.2: Users can edit their username, avatar, and bio.
- AC-8.3: The feed loads and displays posts with followed-users-first ordering.
- AC-8.4: Like toggles on/off and the count updates immediately in the UI.
- AC-8.5: Comments can be posted and appear in chronological order.
- AC-8.6: Save bookmarks the post; it appears in the user's saved section.
- AC-8.7: Fork creates a new local project with the full canvas data from the original post.
- AC-8.8: Publishing uploads the image and creates the Firestore document; the post is visible in the feed.

---

## 9. Offline / Online Behavior

Scraply is designed as an **offline-first** app for all creative features, with social features requiring connectivity.

### Offline-Capable (no internet required)

| Feature           | Notes                                                    |
|-------------------|----------------------------------------------------------|
| Stamp Camera      | Capture, crop, and save stamps locally.                  |
| Upload Stamp      | Pick from gallery, crop, and save locally.               |
| Collections       | Create, rename, delete collections; manage stamps.       |
| Scrapbook Editor  | Full canvas editing, all assets bundled with the app.    |
| Calendar          | View stamps by date from local data.                     |
| Export to device   | Save scrapbook as image to device gallery.              |

### Online-Only (requires internet)

| Feature                | Notes                                                |
|------------------------|------------------------------------------------------|
| Google Sign-In         | Authentication requires network.                     |
| Social feed            | Fetching and displaying posts.                       |
| Publish to feed        | Uploading image and creating post.                   |
| Like / Comment / Save  | All social interactions.                             |
| Fork                   | Downloading canvas data from a published scrapbook.  |
| Profile sync           | Updating and viewing user profiles.                  |
| Follow / Unfollow      | Managing social connections.                         |

### Data Sync Strategy

Scraply uses a **dual-storage architecture**: Room (local) for instant offline access, and Firestore (remote) as the persistent source of truth once the user is signed in.

| Scenario | Behavior |
|----------|----------|
| **Not signed in** | All data (stamps, collections, projects) lives in Room only. No cloud backup. |
| **Signed in, online** | Every create/update/delete to stamps, collections, and projects is written to Room first (for instant UI response), then synced to Firestore in the background. |
| **Signed in, offline** | Changes are saved to Room immediately. Pending Firestore writes are queued and automatically flushed when connectivity returns (Firestore offline persistence). |
| **Reinstall / new device** | After signing in with Google, the app pulls all the user's stamps, collections, and projects from Firestore and populates Room. Stamp images are re-downloaded from Firebase Storage. |
| **Sign-in after offline use** | Existing local data is associated with the new user ID and uploaded to Firestore (one-time migration). |

- **Firestore offline persistence** is also enabled for social data, so previously loaded feed posts and profiles remain viewable when connectivity is temporarily lost.

### Acceptance Criteria

- AC-9.1: All offline features work correctly in airplane mode.
- AC-9.2: Attempting a social action while offline shows a clear error message (not a crash).
- AC-9.3: Previously cached feed data is displayed when offline, with a visual indicator that it may be stale.
- AC-9.4: After reinstalling the app and signing in, all stamps, collections, and projects are restored from Firestore.
- AC-9.5: Offline changes sync to Firestore automatically when connectivity is restored.
- AC-9.6: A user who captures stamps before signing in retains all data after signing in (local-to-cloud migration).

---

## 10. Technical Stack

| Layer               | Technology                                                    |
|---------------------|---------------------------------------------------------------|
| Language            | Kotlin                                                        |
| UI Framework        | Jetpack Compose                                               |
| Min SDK             | API 24 (Android 7.0 Nougat)                                  |
| Target SDK          | Latest stable (API 35 at time of writing)                     |
| Authentication      | Firebase Authentication (Google Sign-In provider)             |
| Database (remote)   | Cloud Firestore                                               |
| File storage        | Firebase Storage (stamp images, scrapbook exports, avatars)   |
| Database (local)    | Room (local cache for stamps, collections, projects; synced to Firestore when signed in) |
| Image loading       | Coil (Compose-native, coroutine-based)                        |
| Image processing    | Android `Canvas` / `Bitmap` APIs for frame cropping & export  |
| Camera              | CameraX (Jetpack library for camera access)                   |
| Dependency injection| Hilt                                                          |
| Navigation          | Jetpack Navigation Compose                                    |
| Architecture        | MVVM with Repository pattern                                  |

### Build Configuration

- **Build system**: Gradle with Kotlin DSL (`.kts` files).
- **Version catalog**: `libs.versions.toml` for dependency management.

---

## 11. Data Model

### 11.1 Entity Relationship Overview

```
User ──1:N──> Stamp
User ──1:N──> Collection
User ──1:N──> ScrapbookProject
User ──1:N──> PublishedScrapbook

Collection ──M:N──> Stamp          (via CollectionStamp join)
ScrapbookProject ──1:0..1──> PublishedScrapbook

User ──M:N──> User                 (via Follow join: follower ↔ followee)
User ──M:N──> PublishedScrapbook   (via Like, Save/Bookmark)
User ──1:N──> Comment
PublishedScrapbook ──1:N──> Comment
```

### 11.2 Synced Entities (Room + Firestore)

These entities are stored in **both** Room (local, for offline access) and Firestore (remote, for persistence across devices). Room is the immediate data source for the UI; Firestore is the durable source of truth once signed in.

#### Stamp

| Field       | Type     | Room | Firestore | Notes |
|-------------|----------|------|-----------|-------|
| id          | String   | PK   | Doc ID    | UUID, generated locally |
| imageUri    | String   | Yes  | —         | Local file path (Room only) |
| imageUrl    | String?  | Yes  | Yes       | Firebase Storage URL (null until uploaded) |
| frameType   | String   | Yes  | Yes       | Frame identifier (e.g., "postage_stamp") |
| title       | String?  | Yes  | Yes       | Optional, user-provided |
| caption     | String?  | Yes  | Yes       | Optional, user-provided |
| createdAt   | Long     | Yes  | Timestamp | Epoch millis locally; server timestamp in Firestore |
| userId      | String?  | Yes  | Yes       | Null until user signs in |

Firestore path: `users/{uid}/stamps/{stampId}`

On sync: the cropped image file is uploaded to Firebase Storage, and `imageUrl` is populated. On restore (reinstall), the image is downloaded from `imageUrl` back to local storage and `imageUri` is set.

#### Collection

| Field       | Type     | Room | Firestore | Notes |
|-------------|----------|------|-----------|-------|
| id          | String   | PK   | Doc ID    | UUID |
| name        | String   | Yes  | Yes       | User-provided or "All Stamps" |
| isDefault   | Boolean  | Yes  | Yes       | True only for "All Stamps" |
| createdAt   | Long     | Yes  | Timestamp | Epoch millis / server timestamp |
| userId      | String?  | Yes  | Yes       | Null until user signs in |

Firestore path: `users/{uid}/collections/{collectionId}`

#### CollectionStamp (join table)

| Field        | Type   | Room | Firestore | Notes |
|--------------|--------|------|-----------|-------|
| collectionId | String | FK   | Yes       | → Collection.id |
| stampId      | String | FK   | Yes       | → Stamp.id |

Room composite primary key: `(collectionId, stampId)`.  
Firestore path: `users/{uid}/collections/{collectionId}/stamps/{stampId}`

#### ScrapbookProject

| Field          | Type    | Room | Firestore | Notes |
|----------------|---------|------|-----------|-------|
| id             | String  | PK   | Doc ID    | UUID |
| name           | String  | Yes  | Yes       | User-provided |
| backgroundType | String  | Yes  | Yes       | One of: "paper", "soft_paper", "plain", "grid" |
| canvasJson     | String  | Yes  | Yes       | Serialized JSON of all placed elements |
| createdAt      | Long    | Yes  | Timestamp | Epoch millis / server timestamp |
| updatedAt      | Long    | Yes  | Timestamp | Updated on every save |
| userId         | String? | Yes  | Yes       | Null until user signs in |

Firestore path: `users/{uid}/projects/{projectId}`

### 11.3 Social-Only Entities (Firestore)

These entities exist only in Firestore and are relevant to the social network features. They are not stored in Room (beyond Firestore's built-in offline cache).

#### users/{uid}

| Field          | Type   | Notes                           |
|----------------|--------|---------------------------------|
| username       | String | Unique                          |
| avatarUrl      | String | Firebase Storage URL            |
| bio            | String | Max 150 characters              |
| followerCount  | Number | Denormalized counter            |
| followingCount | Number | Denormalized counter            |

#### published_scrapbooks/{id}

| Field         | Type      | Notes                                        |
|---------------|-----------|----------------------------------------------|
| id            | String    | Auto-generated document ID                   |
| projectId     | String    | Reference to local ScrapbookProject          |
| imageUrl      | String    | Firebase Storage URL of rendered image        |
| canvasJson    | String    | Full canvas state (enables forking)          |
| userId        | String    | Author's UID                                 |
| likeCount     | Number    | Denormalized counter                         |
| commentCount  | Number    | Denormalized counter                         |
| createdAt     | Timestamp | Server timestamp                             |

#### published_scrapbooks/{id}/comments/{commentId}

| Field     | Type      | Notes                  |
|-----------|-----------|------------------------|
| userId    | String    | Commenter's UID        |
| text      | String    | Comment body           |
| createdAt | Timestamp | Server timestamp       |

#### published_scrapbooks/{id}/likes/{uid}

| Field     | Type      | Notes                  |
|-----------|-----------|------------------------|
| createdAt | Timestamp | Server timestamp       |

Document ID is the liker's UID, ensuring one like per user per post.

#### published_scrapbooks/{id}/saves/{uid}

| Field     | Type      | Notes                  |
|-----------|-----------|------------------------|
| createdAt | Timestamp | Server timestamp       |

Document ID is the saver's UID.

#### follows/{followerId_followeeId}

| Field      | Type   | Notes              |
|------------|--------|--------------------|
| followerId | String | UID of follower    |
| followeeId | String | UID of followee    |
| createdAt  | Timestamp | Server timestamp |

Document ID is `{followerId}_{followeeId}` for easy existence checks.

---

## Appendix A: Glossary

| Term              | Definition                                                                 |
|-------------------|----------------------------------------------------------------------------|
| **Stamp**         | A photo cropped into a decorative frame shape.                             |
| **Frame**         | The decorative border/mask applied to a photo during stamp creation.       |
| **Collection**    | A user-defined group of stamps (like a folder or album).                   |
| **Scrapbook**     | A canvas-based composition of stamps, decorative assets, and text.         |
| **Project**       | A scrapbook that is being edited (local, not yet published).               |
| **Published Scrapbook** | A scrapbook that has been exported and shared to the social feed.    |
| **Fork**          | Copying a published scrapbook into one's own editor as a new project.      |
| **Asset**         | A pre-bundled decorative element (tape, sticker, paper cut, etc.).         |

---

## Appendix B: Future Considerations (Out of Scope for v1)

- Additional frame shapes (circle, heart, wave, custom user-drawn).
- Premium / in-app purchase frame packs or asset packs.
- Stamp re-cropping (change frame or reposition after initial save).
- Private accounts and block/report functionality.
- Content moderation and automated review.
- Push notifications (likes, comments, new followers).
- Direct messaging between users.
- Hashtags, search-by-tag, and discover/explore page.
- Multi-page scrapbooks.
- Collaborative editing (multiple users on one scrapbook).
- Export to PDF.
- Animated / video stamps.
