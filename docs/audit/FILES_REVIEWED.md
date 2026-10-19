# Files Reviewed Inventory

**Repository:** `com.abhisheksati.mydoc`  
**Total Non-Build / Non-VCS Files Audited:** 202  
**Audit Coverage:** 100% of codebase, manifests, build scripts, Gradle configs, ProGuard rules, tests, and resources.

---

## 1. Build, Packaging, CI/CD & Configuration (13 files)
1. `/.gitignore` - Git ignore rules for Android/Gradle/Keystore
2. `/build.gradle.kts` - Top-level root Gradle build script
3. `/gradle.properties` - JVM arguments, AndroidX & Kotlin compiler settings
4. `/gradlew` - Unix Gradle wrapper executable
5. `/gradlew.bat` - Windows Gradle wrapper script
6. `/local.properties` - Android SDK path specification
7. `/README.md` - Project documentation and architecture summary
8. `/settings.gradle.kts` - Module definitions and plugin management
9. `/skills-lock.json` - Skills system lockfile
10. `/.github/workflows/build.yml` - CI build and test workflow
11. `/.github/workflows/release.yml` - Release packaging and GitHub release publishing
12. `/gradle/libs.versions.toml` - Version catalog for all dependencies and plugins
13. `/gradle/wrapper/gradle-wrapper.properties` - Gradle wrapper distribution configuration

---

## 2. App Module Build & Packaging (5 files)
14. `/app/build.gradle.kts` - App module Gradle configuration (plugins, dependencies, compileOptions, compose)
15. `/app/proguard-rules.pro` - R8 / ProGuard obfuscation and keep rules
16. `/app/keystore/debug.keystore` - Debug signing keystore checked into repository
17. `/app/src/main/baseline-prof.txt` - Baseline profile definitions
18. `/app/src/main/AndroidManifest.xml` - App manifest, permissions, activities, providers

---

## 3. Resources & XML Configurations (11 files)
19. `/app/src/main/res/drawable/ic_launcher_background.xml` - Adaptive icon background
20. `/app/src/main/res/drawable/ic_launcher_foreground.xml` - Adaptive icon vector foreground
21. `/app/src/main/res/font/inter.ttf` - Custom bundled Inter font asset
22. `/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` - Launcher icon definition
23. `/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` - Round launcher icon definition
24. `/app/src/main/res/values/strings.xml` - App string resources
25. `/app/src/main/res/values/themes.xml` - Android system window themes
26. `/app/src/main/res/xml/backup_rules.xml` - Android 12+ backup inclusion/exclusion rules
27. `/app/src/main/res/xml/data_extraction_rules.xml` - Android 12+ cloud/device data transfer rules
28. `/app/src/main/res/xml/file_paths.xml` - FileProvider shared storage path mappings
29. `/gradle/wrapper/gradle-wrapper.jar` - Gradle wrapper execution binary

---

## 4. Core Architecture, Application & Navigation (2 files)
30. `/app/src/main/java/com/abhisheksati/mydoc/MainActivity.kt` - Single Activity host, edge-to-edge setup, intent handling
31. `/app/src/main/java/com/abhisheksati/mydoc/MyDocApp.kt` - Application subclass, database & service initialization

---

## 5. Domain Models & Repositories (14 files)
32. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/AppRelease.kt` - GitHub release and asset model
33. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/DocumentCategory.kt` - Document format category enum
34. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/DocumentFile.kt` - Core document model and metadata
35. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/FileManagerModels.kt` - File management operations and filter state
36. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/HomeTab.kt` - Bottom navigation tab enum
37. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/MediaItem.kt` - Audio, photo, video item domain models
38. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/ReadingMode.kt` - Viewer reading mode configuration
39. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/StorageItem.kt` - Folder, file, and disk item abstractions
40. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/StorageVolume.kt` - Internal/external storage mount representations
41. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/TrashItem.kt` - Soft-deleted document and media item model
42. `/app/src/main/java/com/abhisheksati/mydoc/domain/model/ViewState.kt` - Page index, zoom, scroll offset persistence model
43. `/app/src/main/java/com/abhisheksati/mydoc/domain/repository/DocumentRepository.kt` - Document database repository interface
44. `/app/src/main/java/com/abhisheksati/mydoc/domain/repository/MediaRepository.kt` - Media store query repository interface
45. `/app/src/main/java/com/abhisheksati/mydoc/domain/repository/StorageRepository.kt` - Storage directory browsing interface

---

## 6. Data Access Layer & Local Persistence (18 files)
46. `/app/src/main/java/com/abhisheksati/mydoc/data/local/AppDatabase.kt` - Room database declaration and type converters
47. `/app/src/main/java/com/abhisheksati/mydoc/data/local/DocumentStreamResolver.kt` - Resolves File and Content URIs into input streams
48. `/app/src/main/java/com/abhisheksati/mydoc/data/local/FolderScanner.kt` - Recursive directory scanner for indexing
49. `/app/src/main/java/com/abhisheksati/mydoc/data/local/PinnedFoldersManager.kt` - Pinned folder state management in SharedPreferences
50. `/app/src/main/java/com/abhisheksati/mydoc/data/local/SettingsRepository.kt` - App settings and user preferences in SharedPreferences
51. `/app/src/main/java/com/abhisheksati/mydoc/data/local/StoragePermissionHelper.kt` - Runtime permission checking for storage
52. `/app/src/main/java/com/abhisheksati/mydoc/data/local/dao/FavoriteDao.kt` - Room DAO for bookmarked documents
53. `/app/src/main/java/com/abhisheksati/mydoc/data/local/dao/MediaPlaybackDao.kt` - Room DAO for media playback resume timestamps
54. `/app/src/main/java/com/abhisheksati/mydoc/data/local/dao/RecentDao.kt` - Room DAO for recent document access history
55. `/app/src/main/java/com/abhisheksati/mydoc/data/local/dao/TrashDao.kt` - Room DAO for recycle bin metadata
56. `/app/src/main/java/com/abhisheksati/mydoc/data/local/dao/ViewStateDao.kt` - Room DAO for document reading positions
57. `/app/src/main/java/com/abhisheksati/mydoc/data/local/entity/FavoriteDocumentEntity.kt` - Room entity for favorites
58. `/app/src/main/java/com/abhisheksati/mydoc/data/local/entity/MediaPlaybackEntity.kt` - Room entity for video resume timestamps
59. `/app/src/main/java/com/abhisheksati/mydoc/data/local/entity/RecentDocumentEntity.kt` - Room entity for recent files
60. `/app/src/main/java/com/abhisheksati/mydoc/data/local/entity/TrashEntity.kt` - Room entity for recycle bin items
61. `/app/src/main/java/com/abhisheksati/mydoc/data/local/entity/ViewStateEntity.kt` - Room entity for document view state
62. `/app/src/main/java/com/abhisheksati/mydoc/data/repository/DocumentRepositoryImpl.kt` - Implementation of DocumentRepository
63. `/app/src/main/java/com/abhisheksati/mydoc/data/repository/MediaRepositoryImpl.kt` - MediaStore queries for photos, videos, audio
64. `/app/src/main/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryImpl.kt` - File system queries for storage manager

---

## 7. ViewModels (5 files)
65. `/app/src/main/java/com/abhisheksati/mydoc/viewmodel/FileManagerViewModel.kt` - File manager state, navigation, operations
66. `/app/src/main/java/com/abhisheksati/mydoc/viewmodel/HomeViewModel.kt` - Home screen recents, favorites, categories
67. `/app/src/main/java/com/abhisheksati/mydoc/viewmodel/MediaViewModel.kt` - Photo/video gallery queries and grouping
68. `/app/src/main/java/com/abhisheksati/mydoc/viewmodel/SpreadsheetViewModel.kt` - Sheet navigation and cell rendering state
69. `/app/src/main/java/com/abhisheksati/mydoc/viewmodel/ViewerViewModel.kt` - Master document viewer state controller

---

## 8. Rendering Engines & Format Parsers (14 files)
70. `/app/src/main/java/com/abhisheksati/mydoc/rendering/cache/TileBitmapCache.kt` - LruCache for PDF page render tiles
71. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/AndroidPdfRenderer.kt` - Fallback Android PdfRenderer wrapper
72. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/DocumentRenderer.kt` - Generic document renderer interface
73. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/MuPdfRenderer.kt` - MuPDF native document renderer implementation
74. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/libreoffice/LibreOfficeDocumentRenderer.kt` - LibreOffice DocumentRenderer wrapper
75. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/libreoffice/LibreOfficeEngineManager.kt` - Dynamic APK downloader & runtime .so loader
76. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/presentation/LibreOfficePresentationRenderer.kt` - LibreOffice PPTX fallback renderer
77. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/presentation/PptxParser.kt` - Custom XML SAX parser for PPTX slides
78. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/spreadsheet/SheetData.kt` - Models for rows, cells, formulas
79. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/spreadsheet/SpreadsheetParser.kt` - Custom XML parser for XLSX/CSV sheets
80. `/app/src/main/java/com/abhisheksati/mydoc/rendering/engine/word/DocxParser.kt` - Custom XML parser for DOCX paragraphs and tables
81. `/app/src/main/java/org/libreoffice/kit/Document.java` - JNI bridge stub for LibreOffice document
82. `/app/src/main/java/org/libreoffice/kit/LibreOfficeKit.java` - JNI bridge stub for LibreOffice init
83. `/app/src/main/java/org/libreoffice/kit/Office.java` - JNI bridge stub for LibreOffice office instance

---

## 9. Security, Cryptography & Vault (3 files)
84. `/app/src/main/java/com/abhisheksati/mydoc/security/AppLockScreen.kt` - PIN / Biometric lock barrier screen
85. `/app/src/main/java/com/abhisheksati/mydoc/security/BiometricLockHelper.kt` - AndroidX BiometricPrompt authentication helper
86. `/app/src/main/java/com/abhisheksati/mydoc/security/vault/VaultManager.kt` - AES-GCM encrypted document vault storage

---

## 10. Tools, Engines & Management (14 files)
87. `/app/src/main/java/com/abhisheksati/mydoc/tools/appmanager/AppManagerHelper.kt` - Package manager query and APK backup utility
88. `/app/src/main/java/com/abhisheksati/mydoc/tools/cleaner/StorageCleanerManager.kt` - Duplicate file & junk cache scanner
89. `/app/src/main/java/com/abhisheksati/mydoc/tools/engine/PdfToolsEngine.kt` - MuPDF-based merge, split, compress, watermark engine
90. `/app/src/main/java/com/abhisheksati/mydoc/tools/model/PdfTool.kt` - PDF tool catalog definitions
91. `/app/src/main/java/com/abhisheksati/mydoc/tools/model/ToolModels.kt` - Cleaner, scanner, and conversion models
92. `/app/src/main/java/com/abhisheksati/mydoc/tools/security/SignatureStorage.kt` - Vector / bitmap digital signature store
93. `/app/src/main/java/com/abhisheksati/mydoc/tools/tts/DocumentTtsManager.kt` - Text-to-speech engine wrapper
94. `/app/src/main/java/com/abhisheksati/mydoc/tools/ui/ToolRunnerSheet.kt` - Modal bottom sheet executing tool jobs
95. `/app/src/main/java/com/abhisheksati/mydoc/tools/ui/ToolsScreen.kt` - Tools catalog tab UI
96. `/app/src/main/java/com/abhisheksati/mydoc/tools/ui/components/CameraScanSheet.kt` - CameraX document scanner sheet
97. `/app/src/main/java/com/abhisheksati/mydoc/tools/ui/components/ReorderPagesSheet.kt` - PDF page reordering and deletion grid
98. `/app/src/main/java/com/abhisheksati/mydoc/tools/ui/components/SignPdfSheet.kt` - Canvas signature capture modal
99. `/app/src/main/java/com/abhisheksati/mydoc/tools/viewmodel/ToolsViewModel.kt` - Tools tab and job execution ViewModel
100. `/app/src/main/java/com/abhisheksati/mydoc/media/audio/AudioExtractor.kt` - MediaExtractor + MediaMuxer audio extraction utility

---

## 11. Network, Updates & OCR (6 files)
101. `/app/src/main/java/com/abhisheksati/mydoc/network/WifiServerManager.kt` - Embedded NanoHTTPD server for Wi-Fi file sharing
102. `/app/src/main/java/com/abhisheksati/mydoc/ocr/OcrEngine.kt` - Google MLKit on-device text recognition engine
103. `/app/src/main/java/com/abhisheksati/mydoc/ocr/OcrResultSheet.kt` - Text display and copy sheet for OCR results
104. `/app/src/main/java/com/abhisheksati/mydoc/update/ApkDownloader.kt` - HttpURLConnection streaming APK downloader
105. `/app/src/main/java/com/abhisheksati/mydoc/update/GitHubUpdateService.kt` - GitHub API release tag fetcher
106. `/app/src/main/java/com/abhisheksati/mydoc/update/PackageInstallerHelper.kt` - Intent-based APK package installer
107. `/app/src/main/java/com/abhisheksati/mydoc/update/UpdateManager.kt` - Update lifecycle orchestrator

---

## 12. Design System Components & Tokens (17 files)
108. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppBottomSheet.kt` - Standardized modal bottom sheet container
109. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppDialog.kt` - Standardized confirmation dialog
110. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppDivider.kt` - Subtle hairline divider
111. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppEmptyState.kt` - Zero-state placeholder illustration and text
112. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppFileRow.kt` - Standardized file list item row
113. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppFilterChip.kt` - Custom styled filter chips
114. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppProgressIndicator.kt` - Linear and circular progress indicators
115. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppSearchField.kt` - Minimal search input field
116. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppSectionHeader.kt` - Section header title
117. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppTabHeader.kt` - Unified top navigation tab header
118. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/AppToolbar.kt` - Unified top app bar
119. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/components/DocumentFormatBadge.kt` - Color-coded file extension badge
120. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/preview/DesignSystemPreviewScreen.kt` - Unused catalog preview screen
121. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/tokens/AppColors.kt` - Primary design system color tokens
122. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/tokens/AppSpacing.kt` - Spacing and dimension tokens
123. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/tokens/AppTheme.kt` - Design system MaterialTheme wrapper
124. `/app/src/main/java/com/abhisheksati/mydoc/designsystem/tokens/AppTypography.kt` - Typography style hierarchy

---

## 13. UI & Screens (34 files)
125. `/app/src/main/java/com/abhisheksati/mydoc/ui/appmanager/AppManagerSheet.kt` - Installed apps management and backup sheet
126. `/app/src/main/java/com/abhisheksati/mydoc/ui/cleaner/StorageCleanerSheet.kt` - Storage cleanup results and action sheet
127. `/app/src/main/java/com/abhisheksati/mydoc/ui/common/AppDivider.kt` - Duplicate divider component
128. `/app/src/main/java/com/abhisheksati/mydoc/ui/common/EmptyState.kt` - Duplicate empty state component
129. `/app/src/main/java/com/abhisheksati/mydoc/ui/filemanager/FileManagerDialogs.kt` - New folder, rename, delete dialogs
130. `/app/src/main/java/com/abhisheksati/mydoc/ui/filemanager/FileManagerScreen.kt` - Files tab directory browser
131. `/app/src/main/java/com/abhisheksati/mydoc/ui/filemanager/StorageItemComponents.kt` - Storage breadcrumbs and volume selectors
132. `/app/src/main/java/com/abhisheksati/mydoc/ui/filemanager/TrashBinSheet.kt` - Recycle bin review and restoration sheet
133. `/app/src/main/java/com/abhisheksati/mydoc/ui/home/DocumentDetailsDialog.kt` - File details and metadata dialog
134. `/app/src/main/java/com/abhisheksati/mydoc/ui/home/DocumentListRow.kt` - Document row with swipe actions
135. `/app/src/main/java/com/abhisheksati/mydoc/ui/home/DocumentsTabContent.kt` - Documents tab content list and filters
136. `/app/src/main/java/com/abhisheksati/mydoc/ui/home/HomeScreen.kt` - Root bottom navigation controller and scaffold
137. `/app/src/main/java/com/abhisheksati/mydoc/ui/home/SearchAndFilterBar.kt` - Search header on Documents tab
138. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/MediaTabContent.kt` - Photos & Videos tab with grid views
139. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/PhotoEditorScreen.kt` - Crop, rotate, and filter screen
140. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/PhotoInfoSheet.kt` - EXIF metadata details sheet
141. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/PhotoViewerScreen.kt` - Fullscreen photo viewer with zoom
142. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/VideoPlayerScreen.kt` - Fullscreen LibVLC video player
143. `/app/src/main/java/com/abhisheksati/mydoc/ui/media/ZoomableImage.kt` - Pointer-input zoom and pan container
144. `/app/src/main/java/com/abhisheksati/mydoc/ui/network/WifiSharingSheet.kt` - Local Wi-Fi sharing QR code and IP display
145. `/app/src/main/java/com/abhisheksati/mydoc/ui/settings/SettingsScreen.kt` - Settings tab (theme, vault, updater, info)
146. `/app/src/main/java/com/abhisheksati/mydoc/ui/theme/Color.kt` - Legacy theme color definitions
147. `/app/src/main/java/com/abhisheksati/mydoc/ui/theme/Shape.kt` - Legacy shape tokens
148. `/app/src/main/java/com/abhisheksati/mydoc/ui/theme/Spacing.kt` - Legacy spacing tokens
149. `/app/src/main/java/com/abhisheksati/mydoc/ui/theme/Theme.kt` - Legacy Theme composable
150. `/app/src/main/java/com/abhisheksati/mydoc/ui/theme/Typography.kt` - Legacy Typography tokens
151. `/app/src/main/java/com/abhisheksati/mydoc/ui/tts/DocumentTtsBar.kt` - Floating TTS audio player controls
152. `/app/src/main/java/com/abhisheksati/mydoc/ui/vault/VaultSheet.kt` - Encrypted safe vault explorer sheet
153. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/DocumentViewerScreen.kt` - Format router and viewer orchestrator
154. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/pdf/PdfViewerScreen.kt` - Core PDF reader with tiled canvas
155. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/presentation/PresentationViewerScreen.kt` - PPTX slide deck viewer
156. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/spreadsheet/SpreadsheetViewerScreen.kt` - Spreadsheet grid viewer
157. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/text/TextViewerScreen.kt` - Plain text and markdown viewer
158. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/word/WordViewerScreen.kt` - DOCX document paragraph viewer

---

## 14. Viewer Controls & Overlays (11 files)
159. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/BrightnessDialog.kt` - Screen brightness override dialog
160. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/FastScrollbar.kt` - Draggable fast-scroll scrub bar
161. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/OutlineDrawer.kt` - PDF bookmarks / table of contents drawer
162. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/PageJumpDialog.kt` - Jump to page number dialog
163. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/PasswordDialog.kt` - Password prompt for encrypted PDFs
164. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/PrintHelper.kt` - Android PrintManager PDF print adapter
165. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/ReadingControlsBottomSheet.kt` - Night mode, invert, zoom sheet
166. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/SearchOverlay.kt` - In-document text search bar
167. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/ThumbnailStrip.kt` - Horizontal bottom page thumbnail strip
168. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/ViewerBottomBar.kt` - Document viewer action bar
169. `/app/src/main/java/com/abhisheksati/mydoc/ui/viewer/controls/ViewerTopBar.kt` - Document viewer header bar

---

## 15. Unit Tests (8 files)
170. `/app/src/test/java/com/abhisheksati/mydoc/data/repository/StorageRepositoryTest.kt` - Tests for directory scanning and sorting
171. `/app/src/test/java/com/abhisheksati/mydoc/domain/model/DocumentFileTest.kt` - Tests for file formatting, extension parsing
172. `/app/src/test/java/com/abhisheksati/mydoc/domain/model/StorageItemTest.kt` - Tests for storage models and display names
173. `/app/src/test/java/com/abhisheksati/mydoc/rendering/engine/presentation/PptxParserTest.kt` - XML parsing tests for PPTX slides
174. `/app/src/test/java/com/abhisheksati/mydoc/rendering/engine/spreadsheet/SpreadsheetParserTest.kt` - Tests for XLSX parsing
175. `/app/src/test/java/com/abhisheksati/mydoc/tools/PdfToolsTest.kt` - Tests for PDF split/merge operations
176. `/app/src/test/java/com/abhisheksati/mydoc/tools/cleaner/StorageCleanerManagerTest.kt` - Tests for duplicate detection logic
177. `/app/src/test/java/com/abhisheksati/mydoc/update/UpdateManagerTest.kt` - Tests for GitHub version comparison logic

---

## 16. Skill Configurations & Agent Directives (25 files)
178. `/.agents/skills/brandkit/SKILL.md`
179. `/.agents/skills/brandkit/agents/openai.yaml`
180. `/.agents/skills/design-taste-frontend-v1/SKILL.md`
181. `/.agents/skills/design-taste-frontend-v1/agents/openai.yaml`
182. `/.agents/skills/design-taste-frontend-v2-experimental/SKILL.md`
183. `/.agents/skills/design-taste-frontend-v2-experimental/agents/openai.yaml`
184. `/.agents/skills/gpt-taste/SKILL.md`
185. `/.agents/skills/gpt-taste/agents/openai.yaml`
186. `/.agents/skills/high-end-visual-design/SKILL.md`
187. `/.agents/skills/high-end-visual-design/agents/openai.yaml`
188. `/.agents/skills/image-to-code/SKILL.md`
189. `/.agents/skills/image-to-code/agents/openai.yaml`
190. `/.agents/skills/imagegen-frontend-mobile/SKILL.md`
191. `/.agents/skills/imagegen-frontend-mobile/agents/openai.yaml`
192. `/.agents/skills/imagegen-frontend-web/SKILL.md`
193. `/.agents/skills/imagegen-frontend-web/agents/openai.yaml`
194. `/.agents/skills/industrial-brutalist-ui/SKILL.md`
195. `/.agents/skills/industrial-brutalist-ui/agents/openai.yaml`
196. `/.agents/skills/minimalist-ui/SKILL.md`
197. `/.agents/skills/minimalist-ui/agents/openai.yaml`
198. `/.agents/skills/redesign-existing-projects/SKILL.md`
199. `/.agents/skills/redesign-existing-projects/agents/openai.yaml`
200. `/.agents/skills/stitch-design-taste/DESIGN.md`
201. `/.agents/skills/stitch-design-taste/SKILL.md`
202. `/.agents/skills/stitch-design-taste/agents/openai.yaml`
