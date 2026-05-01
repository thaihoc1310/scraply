# Quick Onboarding

> **Dành cho:** Dev mới onboard, chỉ biết sơ Kotlin, cần hiểu project trong ≈ 1 tiếng
> **Cập nhật:** 2026-04-29 — đối chiếu docs & code thật

---

## 1. Project Summary

### App này làm gì?
**Scraply** là app sáng tạo trên Android, cho phép:
- Chụp ảnh → crop thành "stamp" (con tem trang trí)
- Sắp xếp stamp vào collection (album)
- Ghép stamp + sticker + text lên canvas tạo scrapbook
- Đăng scrapbook lên social feed, tương tác (like / comment / save / fork)

### Các flow chính của user
1. **Stamp Camera** — Chụp ảnh → crop theo khung → đặt tên → lưu vào collection
2. **Upload Stamp** — Chọn ảnh từ gallery → crop → lưu (giống camera nhưng thay live camera)
3. **Collections** — Quản lý album stamp: tạo, rename, xóa collection; thêm/bỏ stamp
4. **Editor** — Tạo project scrapbook → kéo thả stamp/sticker/text lên canvas → export ảnh / publish lên feed
5. **Calendar** — Xem stamp theo ngày (lịch tháng)
6. **Social** — Feed, Like, Comment, Save, Fork, Follow, Profile (cần sign-in Google)

### Feature/module chính
| Module       | Mô tả ngắn                              |
|--------------|------------------------------------------|
| `stamp`      | Camera + crop + lưu stamp               |
| `collection` | Quản lý collection & stamp detail        |
| `editor`     | Canvas scrapbook, thêm element, export   |
| `calendar`   | Lịch tháng hiển thị stamp theo ngày     |
| `social`     | Feed, Profile, Auth (Google Sign-In)     |
| `notifications` | FCM push notifications               |

---

## 2. Architecture Overview

### Kiến trúc thực tế: **MVVM + Repository Pattern** (không dùng Hilt)

Docs (REQUIREMENTS §10) ghi dùng Hilt cho DI, nhưng code thật **không dùng Hilt**. Thay vào đó, project dùng **manual DI** qua class `ScraplyContainer` và `ScraplyViewModelFactory`.

### Các layer chính

```
┌──────────────────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose Screens)                      │
│  ui/stamp, ui/collection, ui/editor, ui/calendar, ui/social │
├──────────────────────────────────────────────────────────┤
│  ViewModel Layer                                         │
│  StampCaptureVM, CollectionsVM, EditorVM, CalendarVM,    │
│  FeedVM, ProfileVM, NotificationsVM                      │
├──────────────────────────────────────────────────────────┤
│  Repository Layer (data/repository)                      │
│  StampRepo, CollectionRepo, ProjectRepo                  │
├──────────────────────────────────────────────────────────┤
│  Data Sources                                            │
│  Local: Room DB (data/local)                             │
│  Remote: Firestore + Storage (data/remote)               │
│  Auth: Firebase Auth + Credential Manager (data/auth)    │
└──────────────────────────────────────────────────────────┘
```

**Lưu ý:** 
- **Không có UseCase/Domain layer** — ViewModel gọi thẳng Repository.
- DI container: [`ScraplyContainer`](app/src/main/java/com/example/scraply/data/ScraplyContainer.kt) — tạo trong `ScraplyApp.onCreate()`, inject vào ViewModel qua [`ScraplyViewModelFactory`](app/src/main/java/com/example/scraply/ui/vm/ViewModelFactory.kt).
- Firebase repos (`AuthRepository`, `SocialRepository`, `StorageRepository`, `FirestoreSyncRepository`) có thể `null` nếu Firebase chưa config → app vẫn chạy offline.

---

## 3. Folder Structure

```
app/src/main/java/com/example/scraply/
│
├── MainActivity.kt          ← Entry point (Activity duy nhất)
├── ScraplyApp.kt             ← Application class, khởi tạo ScraplyContainer
│
├── data/                     ← Toàn bộ data layer
│   ├── ScraplyContainer.kt   ← Manual DI container (⭐ đọc đầu tiên khi tìm hiểu DI)
│   ├── model/
│   │   ├── Models.kt         ← Domain models: Stamp, StampCollection, ScrapbookProject
│   │   └── CanvasModel.kt    ← CanvasElement, CanvasState (JSON serializable)
│   ├── local/
│   │   ├── Entities.kt       ← Room entities: StampEntity, CollectionEntity, ProjectEntity...
│   │   ├── Daos.kt           ← Room DAOs: StampDao, CollectionDao, ProjectDao...
│   │   └── ScraplyDatabase.kt ← Room DB singleton
│   ├── repository/
│   │   ├── StampRepository.kt      ← CRUD stamp + auto-add vào "All Stamps"
│   │   ├── CollectionRepository.kt ← CRUD collection + stamp-collection M:N
│   │   └── ProjectRepository.kt    ← CRUD scrapbook project + sync Firestore
│   ├── remote/
│   │   ├── FirestoreSyncRepository.kt ← Push/pull data lên Firestore
│   │   ├── SocialRepository.kt       ← Feed, Like, Comment, Follow, Publish
│   │   ├── StorageRepository.kt       ← Upload ảnh lên Firebase Storage
│   │   └── NotificationsRepository.kt ← Tạo notification documents
│   └── auth/
│       └── AuthRepository.kt  ← Google Sign-In, quản lý user profile
│
├── ui/                       ← Toàn bộ UI layer (Compose)
│   ├── navigation/
│   │   ├── Destinations.kt    ← Routes & BottomTab enum
│   │   ├── ScraplyNavHost.kt  ← NavHost chính (⭐ xem toàn bộ routing ở đây)
│   │   └── ScraplyBottomBar.kt ← Bottom nav bar custom
│   ├── stamp/                 ← Camera, crop, stamp details screens
│   ├── collection/            ← Collections list, detail, stamp detail, upload stamp
│   ├── editor/                ← ⭐ EDITOR (focus chính)
│   │   ├── EditorProjectsScreen.kt  ← Danh sách project
│   │   ├── ScrapbookEditorScreen.kt ← Canvas chính (790 dòng — file lớn nhất)
│   │   ├── EditorViewModel.kt       ← Logic editor
│   │   └── EditorAssets.kt           ← Render element + asset options
│   ├── calendar/              ← Calendar screen
│   ├── social/                ← Feed, Profile, AuthGate
│   ├── notifications/         ← Notifications screen
│   ├── common/
│   │   └── CommonUi.kt       ← Shared components: CircleIconButton, PillBadge, ScraplyCard
│   ├── vm/
│   │   └── ViewModelFactory.kt ← ViewModel factory + `scraplyViewModel()` helper
│   └── theme/
│       ├── Theme.kt, Color.kt, Type.kt
│
├── util/
│   ├── ImageUtils.kt          ← Load/crop/save bitmap, export to gallery
│   ├── PostageStampShape.kt   ← Custom Compose shape cho stamp frame
│   └── StampFrameAsset.kt     ← Path builder cho khung stamp
│
└── notifications/
    ├── ScraplyMessagingService.kt  ← FCM service
    ├── ScraplyNotifications.kt     ← Notification channels & display
    └── NotificationTokenManager.kt ← FCM token management
```

### Entry point & chuỗi khởi động
1. `AndroidManifest.xml` → `ScraplyApp` (Application) → `ScraplyContainer` (DI)
2. `MainActivity` → `ScraplyTheme` → `ScraplyNavHost()` (start = `Routes.STAMP`)

---

## 4. Code Flow

### App khởi động
```
ScraplyApp.onCreate()
  └─ ScraplyContainer(this)        // Tạo DB, repos, Firebase repos (nullable)
       └─ ensureDefaultCollection()  // Tạo "All Stamps" nếu chưa có

MainActivity.onCreate()
  └─ setContent { ScraplyTheme { ScraplyNavHost() } }
       └─ NavHost(startDestination = "stamp")
            └─ StampCameraScreen  // Màn đầu tiên user thấy
```

### Một action tiêu biểu: User tạo stamp
```
StampCameraScreen (UI — capture ảnh)
  → navigate("stamp/crop/{uri}")
    → StampCropScreen (UI — user kéo/zoom ảnh trong frame)
      → ImageUtils.renderPostageStamp()  // Crop bitmap
      → ImageUtils.saveStampPng()        // Lưu file PNG
      → navigate("stamp/details/{uri}")
        → StampDetailsScreen (UI — nhập title/caption)
          → StampCaptureViewModel.saveStamp()
            → StampRepository.createStamp()
              → StampDao.upsert()                    // Lưu Room
              → CollectionStampDao.insert()           // Add vào "All Stamps"
              → FirestoreSyncRepository.pushStamp()   // Sync lên Firestore (nếu signed in)
```

### Luồng dữ liệu tổng quát
```
Screen (Compose) → collectAsState() từ ViewModel
ViewModel        → gọi Repository.method()
Repository       → gọi DAO (Room) trước → rồi pushAsync lên Firestore
                    ↑ observe qua Flow
```

- **Room là source of truth cho UI** — luôn write Room trước, UI observe qua `Flow`.
- **Firestore sync là background** — qua `appScope.launch { sync.push... }`.
- Firebase repos nullable → nếu Firebase chưa setup, app vẫn hoạt động offline.

---

## 5. Editor Focus

### Tổng quan Editor feature
Editor gồm **2 màn hình chính** và chia thành **4 file Kotlin**:

| File | Vai trò | Dòng code |
|------|---------|-----------|
| [`EditorProjectsScreen.kt`](app/src/main/java/com/example/scraply/ui/editor/EditorProjectsScreen.kt) | Danh sách project (tạo/rename/delete) | 216 |
| [`ScrapbookEditorScreen.kt`](app/src/main/java/com/example/scraply/ui/editor/ScrapbookEditorScreen.kt) | **Canvas chính** — nơi user kéo thả, chỉnh sửa element | **790** |
| [`EditorViewModel.kt`](app/src/main/java/com/example/scraply/ui/editor/EditorViewModel.kt) | State management, undo, CRUD element, export, publish | 247 |
| [`EditorAssets.kt`](app/src/main/java/com/example/scraply/ui/editor/EditorAssets.kt) | Render visual cho element + danh sách asset options | 218 |

### Model liên quan
| File | Nội dung |
|------|----------|
| [`CanvasModel.kt`](app/src/main/java/com/example/scraply/data/model/CanvasModel.kt) | `CanvasElement` (id, type, x, y, scale, rotation, zIndex, text, font, color, stampId, assetKey) + `CanvasState` (list elements, JSON serialize) |
| [`Models.kt`](app/src/main/java/com/example/scraply/data/model/Models.kt) | `ScrapbookProject` (id, name, backgroundType, canvasJson, timestamps) |
| [`Entities.kt`](app/src/main/java/com/example/scraply/data/local/Entities.kt) | `ProjectEntity` — Room entity tương ứng |

### State trong EditorViewModel
```kotlin
_projects  : StateFlow<List<ProjectCard>>    // Danh sách project
_current   : StateFlow<ScrapbookProject?>    // Project đang mở
_canvas    : StateFlow<CanvasState>          // Trạng thái canvas (list elements)
_stamps    : StateFlow<List<Stamp>>          // Tất cả stamp của user (để pick)
_background: StateFlow<String>               // Loại background ("paper", "grid"...)
_selectedId: StateFlow<String?>              // Element đang được chọn
_publish   : StateFlow<PublishState>         // Trạng thái publish (Idle/Publishing/Error/Success)
undoStack  : ArrayDeque<CanvasState>         // Stack undo (max 40 bước)
```

### Element types trên canvas
```kotlin
enum class CanvasElementType { STAMP, TAPE, STICKER, PAPER_CUT, POLAROID, TEXT }
```
Mỗi element có: `x, y` (normalized 0..1), `scale`, `rotation`, `zIndex`.

### Luồng chỉnh sửa (edit flow)
```
EditorProjectsScreen
  → User tap project card
  → navigate("editor/project/{projectId}")
  → ScrapbookEditorScreen(vm, projectId, onBack)
     ├─ LaunchedEffect → vm.loadProject(id)
     │    → ProjectRepository.getById() → CanvasState.fromJson(canvasJson)
     ├─ Canvas render: canvas.elements.sortedBy(zIndex).forEach { CanvasElementOnBoard }
     ├─ Gesture: detectTransformGestures → vm.updateElement(id) { copy(x,y,scale,rotation) }
     ├─ Select: vm.selectElement(id) → hiện border + action buttons
     ├─ Add element: showAssets=true → EditAssetsSheet (3 tabs: Backgrounds/Assets/Text)
     │    → vm.addElement(type, assetKey, stampId, text)
     ├─ Add stamp: showStampPicker → StampPickerDialog → vm.addElement(STAMP, stampId=...)
     ├─ Undo: vm.undo() → pop undoStack
     └─ DisposableEffect.onDispose → vm.saveCurrent()
```

### Luồng lưu (save flow)
```
vm.saveCurrent()
  → ProjectRepository.updateCanvas(id, backgroundType, canvasJson)
    → ProjectDao.update(entity)               // Room
    → pushAsync → FirestoreSyncRepository.pushProject()  // Firestore
```
**Auto-save:** Khi user nhấn Back hoặc khi `DisposableEffect.onDispose` (navigate away).

### Luồng export
```
Toolbar "Export" button
  → graphicsLayer.toImageBitmap().asAndroidBitmap()  // Capture canvas thành Bitmap
  → vm.exportToGallery(bitmap)
    → ImageUtils.saveToGallery(context, bitmap, name) // Lưu vào MediaStore/Gallery
```

### Luồng publish lên feed
```
Toolbar "Publish" button (chỉ hiện khi vm.canPublish = true, tức Firebase available)
  → graphicsLayer.toImageBitmap().asAndroidBitmap()
  → vm.publishToFeed(bitmap)
    → Check: authRepository != null, socialRepository != null, currentUserId != null
    → Lưu bitmap thành PNG vào cache dir
    → SocialRepository.publishScrapbook(uid, projectId, localImagePath, canvasJson)
      → StorageRepository.uploadPostImage()  // Upload lên Firebase Storage
      → Firestore "published_scrapbooks/{id}" document tạo mới
    → PublishState.Success / Error → Toast
```

### File phải đọc kỹ nhất (nếu sắp sửa Editor)
1. ⭐ **`ScrapbookEditorScreen.kt`** (790 dòng) — file lớn nhất, chứa toàn bộ canvas UI, gesture handling, bottom sheets, dialogs
2. ⭐ **`EditorViewModel.kt`** (247 dòng) — toàn bộ logic state, undo, add/update/delete element, save, export, publish
3. **`CanvasModel.kt`** (41 dòng) — data model cho canvas element, JSON serialize
4. **`EditorAssets.kt`** (218 dòng) — render visual cho mỗi loại element, danh sách asset/background options
5. **`ProjectRepository.kt`** (72 dòng) — CRUD project, sync logic

---

## 6. What to Read in 1 Hour

### 🔴 Critical (25 phút) — Đọc kỹ, hiểu logic
| Thứ tự | File | Lý do |
|--------|------|-------|
| 1 | `docs/REQUIREMENTS.md` §6 (Editor) | Hiểu spec Editor trước khi đọc code |
| 2 | `data/model/CanvasModel.kt` | Model nền tảng — chỉ 41 dòng, đọc 2 phút |
| 3 | `ui/editor/EditorViewModel.kt` | Toàn bộ logic Editor — 247 dòng |
| 4 | `ui/editor/ScrapbookEditorScreen.kt` | Canvas UI chính — scan nhanh cấu trúc, focus vào gesture & bottom sheets |

### 🟡 Important (20 phút) — Đọc nhanh để hiểu context
| Thứ tự | File | Lý do |
|--------|------|-------|
| 5 | `ui/editor/EditorAssets.kt` | Render logic cho element types |
| 6 | `ui/editor/EditorProjectsScreen.kt` | Danh sách project (entry vào Editor) |
| 7 | `data/repository/ProjectRepository.kt` | CRUD + sync logic cho project |
| 8 | `data/ScraplyContainer.kt` | Hiểu DI, biết Editor VM nhận deps nào |
| 9 | `ui/vm/ViewModelFactory.kt` | Hiểu cách ViewModel được tạo |
| 10 | `ui/navigation/ScraplyNavHost.kt` | Hiểu routing — Editor ở đâu trong nav graph |

### 🟢 Skim (15 phút) — Lướt qua để hiểu tổng thể
| Thứ tự | File | Lý do |
|--------|------|-------|
| 11 | `data/model/Models.kt` + `data/local/Entities.kt` | Domain & Room models |
| 12 | `data/local/Daos.kt` | DAO queries |
| 13 | `ui/common/CommonUi.kt` | Shared UI components dùng trong Editor |
| 14 | `util/ImageUtils.kt` | Export/crop logic |
| 15 | `data/remote/SocialRepository.kt` | Hiểu publish flow |
| 16 | `ScraplyApp.kt` + `MainActivity.kt` | Entry points — rất ngắn |
| 17 | `docs/REQUIREMENTS.md` phần còn lại | Hiểu full spec nếu còn thời gian |

---

## 7. Risks / Notes

### Docs vs Code — Những điểm lệch

| Chủ đề | Docs nói | Code thật | Mức ảnh hưởng |
|--------|----------|-----------|---------------|
| **DI** | Hilt (REQUIREMENTS §10) | **Manual DI** qua `ScraplyContainer` + `ScraplyViewModelFactory` — không import Hilt ở bất kỳ đâu | ⚠️ Quan trọng — cần biết khi thêm dependency mới cho Editor |
| **Bottom Nav** | 4 tabs: Stamp, Collection, Editor, Calendar | **5 tabs**: Stamp, Collection, Editor, **Feed**, **Profile** (không có Calendar tab) | ⚠️ Calendar được truy cập qua Collections, không phải bottom tab |
| **Kotlin plugin** | Docs không nói | Code dùng `kotlin.serialization` + `ksp` (cho Room) | Thông tin bổ sung |
| **Firebase Storage** | Stamp images dùng Firebase Storage | Code có `StorageRepository` nhưng **không có `firebase-storage` dependency** trong `build.gradle.kts` — chỉ có `firebase-auth`, `firebase-firestore`, `firebase-messaging` | ⚠️ `StorageRepository` có thể chưa hoạt động hoàn chỉnh — **cần xác nhận với team** |
| **Notifications** | Out of scope cho v1 (REQUIREMENTS §8.6) | **Đã implement**: FCM service, notification channels, `NotificationsRepository`, `NotificationsScreen` | Docs chưa cập nhật |

### Asset render — Placeholder
- File `EditorAssets.kt` line 78 có comment: **"Renders a placeholder visual for each element type. Real assets can replace these later."**
- Hiện tại tape/sticker/paper cut đều render bằng **colored Box/Canvas shapes** thay vì ảnh thật → visual chưa giống mockup

### Điểm cần hỏi team trước khi sửa Editor

1. **Firebase Storage dependency** — `build.gradle.kts` không có `firebase-storage`. Upload ảnh (publish, avatar) có hoạt động không? Hay đang dùng workaround?
2. **Asset images** — Có kế hoạch thay placeholder bằng ảnh thật (PNG/SVG) không? Nếu có, ảnh lưu ở đâu (`res/drawable` hay `assets/`)?
3. **Undo chỉ track `CanvasState`** — Background change gọi `pushUndo()` nhưng push `_canvas.value` (không bao gồm background). Undo background có đúng ý design không?
4. **Canvas export dùng `graphicsLayer`** — Toàn bộ render phụ thuộc Compose `rememberGraphicsLayer()`. Nếu cần export ở resolution cao hơn (print quality), cần approach khác.
5. **`ScrapbookEditorScreen.kt` có 790 dòng** — Nên tách không? Hay team chấp nhận file size này?
6. **TextDialog** reuse từ `ui/collection/CollectionsScreen.kt` — Editor import nó. Nếu sửa UI dialog, ảnh hưởng cả Collections.
7. **No unit tests cho Editor** — Folder `test/` và `androidTest/` có tồn tại nhưng chưa xác nhận có test case nào cho Editor.

### Không tìm thấy TODO / FIXME / mock trong code
- Grep `TODO`, `FIXME`, `mock` trên toàn bộ source: **không có kết quả** → code khá clean, không có debt markers.
