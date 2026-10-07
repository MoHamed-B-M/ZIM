# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-07

### Objective
Major release introducing Material 3 Expressive design system, rich text editing, drag-and-drop attachments, in-app updater, and a redesigned settings experience. Audio notes feature was added then removed in favor of a streamlined note-taking experience.

### New Features
#### Material 3 Expressive Pull-to-Refresh (#1793228)
- **Description**: Replaced custom elastic pull-to-refresh indicator with the new `PullToRefreshDefaults.LoadingIndicator` from Material 3 Expressive API
- **Impact**: Modern, expressive refresh experience with smooth animations and consistent design system integration

#### Rich Text Notes with Drag-and-Drop Attachments (#5efd47d)
- **Description**: Added HTML-formatted notes with a toolbar editor, markdown as an option, and drag-and-drop image/file attachment with drop hints
- **Impact**: Significantly enhanced note editing capabilities with rich formatting and media support

#### In-App Updater with Signature Verification (#e2c5481)
- **Description**: Built-in beta/stable update channels with mipmap icon sets and cryptographic signature verification gate
- **Impact**: Users can now update the app directly without Play Store, with security verification

#### Redesigned Settings Experience (#1178471, #6c103b0)
- **Description**: Dark grouped cards, segmented list items, About card, unified bottom dock with floating nav bar and scalloped FAB
- **Impact**: Modern, accessible settings UI with better organization and visual hierarchy

#### Selectable App Icons (#ffdb65e)
- **Description**: Three selectable app icons with a picker in settings
- **Impact**: User personalization option for app appearance

### Bug Fixes
- **[Build]**: Fixed release build compilation errors with Compose imports and Float*Dp operand order (#3a79554)
- **[Audio]**: Fixed six compile errors in audio button and floating toolbar (#199c88b)
- **[Permissions]**: Declared REQUEST_INSTALL_PACKAGES to prevent grant check crashes (#4d61954)
- **[Onboarding]**: Fixed compile errors in pager navigation, Map lookup, TermsScreen args (#21210fd)
- **[Focus]**: Fixed focus crash from receive-content MIME list containing */* (#3cf7b3e)
- **[Navigation]**: Fixed duplicate Updates route and wavy indicator compatibility with M3 1.3.2 (#68eb6d4)
- **[Editor]**: Fixed receive-content listener overloads with URI-gated attach (#d0e58cf)
- **[Launch]**: Fixed launch crash requiring AppCompat theme for AppCompatActivity base (#90bd21d)
- **[Settings]**: Fixed settings crash from raster previews vs adaptive XML (#a85cca2)
- **[Editor]**: Fixed rich editor compilation issues (TextAlign, label-free spans, link handling) (#e53d810)
- **[UI]**: Fixed menu overlap with inline dock pills replacing overlay FabMenu (#9bc5c7b)
- **[Dialogs]**: Fixed dialog double-fire with one-shot custom actions (#d8e54e5)

### Technical Improvements
#### Performance
- Keyed grid items with placement animations for stable list rendering (#d92f9c6)
- Dropped staggered animateItem in favor of key-based fixes (#5879464)

#### Architecture
- Migrated to EasyNotes base with Hilt DI, widgets, and full settings (#db04187, #0129f39)
- Package renamed to com.zimapp.zim (#d5a0062)
- Room via KAPT instead of KSP for Kotlin 2.3.20 compatibility (#bdc9f1a)
- Material icons extended pinned to 1.7.8 (#23acffe)

#### UI/UX
- Unified bottom dock with floating nav bar and scalloped FAB (#6c103b0)
- Floating toolbar with rich editor, undo/redo, find, tags (#0fdc2db)
- Install-permission onboarding with contained loading indicator (#ba5e9dc)
- Terms of Service updated to "ZIM" across all locales (#4c2c0ad)

#### CI/CD
- CircleCI APK build with artifact upload (#b4bf790, #510fd21)
- Fixed CI keystore signing with rootProject.file resolution (#1f3c0c4)
- Full beta/main workflow with signature verification (#bdc9f1a)
- Dropped obsolete kotlin.android plugin (built into AGP 9) (#2561e05)
- Fixed m3color dependency on JitPack (#147965e)

### Removed Features
- **Audio Notes**: Audio recording, multi-clip playback, and wavy progress indicators were implemented (#1b200ac, #6152a64, #067084b, #15e56e1) but subsequently removed entirely (#f59aac8) to streamline the note-taking experience
- **Donations/Support**: Removed donation flow, Play Store flavor, and billing integration (#9a1d714)

### Database Migrations
✅ [OK] No database migrations required

### Statistics
- Total commits: 66
- Features: 4
- Bug Fixes: 13
- Performance: 2
- Refactoring: 14
- Maintenance/Chore: 27
- Documentation: 1
- Removed Features: 1 (audio notes)

---

## [1.7.0] - Previous Release
*Previous release tag: beta-latest*