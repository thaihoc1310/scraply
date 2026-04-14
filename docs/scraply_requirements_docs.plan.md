---
name: Scraply Requirements Doc
overview: "Create a comprehensive functional requirements / business logic document for the Scraply mobile app, covering all features: stamp camera, collections, scrapbook editor, calendar, and social network."
todos:
  - id: write-req-doc
    content: Write the full REQUIREMENTS.md file at docs/REQUIREMENTS.md with all sections above, expanded with acceptance-criteria-level detail.
    status: completed
isProject: false
---

# Scraply -- Functional Requirements Document

Write a single markdown file at `docs/REQUIREMENTS.md` containing the full business-logic specification organized into the sections below. All content is derived from the 12 reference screenshots and the user's answers.

## Document Structure

### 1. Product Overview
- Scraply is a creative mobile app for Android (Kotlin, Jetpack Compose, min SDK 24).
- Core loop: capture photos as decorative "stamps", organize them into collections, compose stamps and assets into scrapbook pages, and share finished scrapbooks on a built-in social feed.

### 2. Navigation
- Bottom nav with 4 tabs: **Stamp**, **Collection**, **Editor**, **Calendar**.
- Social feed accessed from a separate entry point (e.g., profile icon or dedicated section).

### 3. Stamp Camera (Stamp tab)
- Live camera viewfinder with a decorative frame overlay.
- Launch with 1 frame shape (postage-stamp perforated edge); architecture supports adding more shapes later.
- All frames are free (no premium gating).
- User can switch front/rear camera, pinch-to-zoom.
- Capture button snaps the photo; the image is auto-cropped to the frame shape.
- Post-capture flow (from new screenshots): preview of cropped stamp, optional Title field, optional Caption field, "Retake" button, "Save to Book" button.

### 4. Upload Stamp (alternate entry)
- Accessible from Collections screen (gallery import icon).
- User picks a photo from device gallery, positions it (drag + pinch-to-zoom) within the frame, then "Cut Stamp".
- Same title/caption form and save flow as camera capture.

### 5. Collections (Collection tab)
- Default "All Stamps" collection (cannot be deleted/renamed); every stamp appears here automatically.
- Users can create unlimited custom collections via "+" button.
- A stamp can belong to multiple collections simultaneously.
- Collection card shows: name, stamp count, thumbnail preview.
- "..." menu on custom collections: Rename, Delete.
- Tapping a collection opens a grid of stamp thumbnails.
- Stamps can be edited after capture: rename, re-caption, delete. (Re-crop is deferred/stretch goal.)

### 6. Scrapbook Editor (Editor tab)
- Project list showing all scrapbook projects (no Active/Draft distinction -- just a flat list).
- "+" to create a new project (user names it).
- "..." menu on project cards: Rename, Delete.
- **Canvas**: fixed-size, paper-textured background (selectable: Paper, Soft Paper, Plain, Grid).
- **Elements**: stamps from user collections, decorative assets, and text layers -- all placed via drag-and-drop.
- **Element manipulation** (finger gestures): move, resize, rotate, reorder z-layer.
- **Asset categories** (Backgrounds, Assets, Text tabs):
  - Backgrounds: Paper, Soft Paper, Plain, Grid.
  - Assets: Tape Pack, Sticker Pack, Polaroid Frame (auto-crops stamp to 1:1 + bottom margin), Paper Cuts.
  - Text: Add Text Layer, Font Presets, Text Styles (color/stroke/shadow), Captions (reusable blocks).
- **Toolbar**: back, share/export, add stamp, adjustments, palette.
- **Undo** button (single-step undo at minimum).
- **Export**: save as PNG/JPG image to device; also used when publishing to social feed.

### 7. Calendar (Calendar tab)
- Monthly view with Compact / Full toggle.
- Each date cell shows thumbnail(s) of stamps captured on that day.
- Tapping a date opens the list of stamps from that day.
- Navigate months with left/right arrows.

### 8. Social Network
- **Authentication**: Google Sign-In via Firebase Auth.
- **Profile**: username, avatar, bio, follower/following counts, gallery of scrapbooks the user has chosen to publish.
- **Feed**: global feed of published scrapbooks; posts from followed users are prioritized (shown first), then remaining posts in reverse-chronological order.
- **Interactions on a post**: Like, Comment, Save (bookmark), Fork (copy the scrapbook into the user's own Editor as a new project for further editing).
- No private accounts, no block/report, no moderation system for v1.

### 9. Offline / Online Behavior
- **Offline**: Stamp camera, collections, scrapbook editor, calendar all work fully offline.
- **Online-only**: Social feed, publishing, liking, commenting, forking, profile sync.
- Firestore offline persistence handles data caching automatically.

### 10. Technical Stack
- Language: Kotlin
- UI: Jetpack Compose
- Min SDK: 24 (Android 7.0 Nougat)
- Backend: Firebase (Auth, Firestore, Storage)
- Local storage: Room (stamps, collections, projects) synced to Firestore for social features.
- Image processing: Android Canvas / Bitmap APIs for frame cropping; consider Coil for image loading.

### 11. Data Model (high-level entities)
- **User**: uid, username, avatarUrl, bio, followerCount, followingCount
- **Stamp**: id, imageUri, frameType, title, caption, createdAt, userId
- **Collection**: id, name, isDefault, createdAt, userId
- **CollectionStamp**: collectionId, stampId (many-to-many join)
- **ScrapbookProject**: id, name, backgroundType, canvasJson (serialized element tree), createdAt, updatedAt, userId
- **PublishedScrapbook**: id, projectId, imageUrl, userId, likeCount, commentCount, createdAt
- **Comment**: id, scrapbookId, userId, text, createdAt
- **Like**: scrapbookId, userId
- **Save/Bookmark**: scrapbookId, userId
- **Follow**: followerId, followeeId
