# Priority #2 Implementation Plan
# Replace NanoRedux with ViewModel + StateFlow
# Branch: priority-2-viewmodel-refactor

## Branch Created
- Branch name: `priority-2-viewmodel-refactor`
- Based on: `master` (latest commit: aa30fbf Merge PR #91)
- Goal: Migrate from custom NanoRedux Store to standard ViewModel + StateFlow

---

## 1. Create FeedViewModel (NEW FILE)

**Path:** `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedViewModel.kt`

```kotlin
package com.github.jetbrains.rssreader.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emit
import kotlinx.coroutines.flow.mutableStateFlow
import kotlinx.coroutines.launch
import com.github.jetbrains.rssreader.core.RssReader
import com.github.jetbrains.rssreader.domain.RssFeed
import com.github.jetbrains.rssreader.domain.FeedSideEffect
import kotlinx.coroutines.flow.SharedFlow

class FeedViewModel(
    private val rssReader: RssReader = KoinInject.instance().get()
) : ViewModel() {

    // UI STATE: lifecycle-scoped, retains across config changes
    private val _state = mutableStateFlow(
        FeedState(false, emptyList(), selectedFeed = null)
    )
    val state: StateFlow<FeedState> = _state.asStateFlow()

    // EFFECTS: one-shot events delivered to composables
    private val _effects = MutableSharedFlow<FeedSideEffect>()
    val effects: Flow<FeedSideEffect> = _effects.asFlow()

    // PUBLIC API: called from composables
    suspend fun loadFeeds(force: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(progress = true)
            try {
                val allFeeds = rssReader.getAllFeeds(force)
                _state.value = _state.value.copy(
                    progress = false,
                    feeds = allFeeds
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(progress = false)
                _effects.emit(FeedSideEffect.Error(e))
            }
        }
    }

    suspend fun addFeed(url: String) {
        viewModelScope.launch {
            try {
                rssReader.addFeed(url)
                _effects.emit(FeedSideEffect.FeedAdded)
                loadFeeds(false)
            } catch (e: Exception) {
                _effects.emit(FeedSideEffect.Error(e))
            }
        }
    }

    suspend fun deleteFeed(url: String) {
        viewModelScope.launch {
            try {
                rssReader.deleteFeed(url)
                _effects.emit(FeedSideEffect.FeedDeleted)
                loadFeeds(false)
            } catch (e: Exception) {
                _effects.emit(FeedSideEffect.Error(e))
            }
        }
    }

    suspend fun selectFeed(feed: RssFeed?) {
        _state.value = _state.value.copy(selectedFeed = feed)
    }
}
```

**Dependencies (add to shared/build.gradle.kts):**
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.7.0")
```

---

## 2. Remove/Replace FeedStore (MODIFY)

**Current:** `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/app/FeedStore.kt`
**Action:** Delete or convert to empty delegate

**If keeping as delegate:**
```kotlin
// FeedStore now delegates to ViewModel via Koin
class FeedStore private constructor(
    private val viewModel: FeedViewModel
) : CoroutineScope by MainScope() {
    // Minimal: only keep if other code still depends on it
    override val state: StateFlow<FeedState> = viewModel.state
    override val effects: Flow<FeedSideEffect> = viewModel.effects
    override val coroutineScope = viewModelScope
    
    // Remove dispatch() entirely — composables call viewModel methods directly
    // Remove observeState()/observeSideEffect()
}
```

**Better:** Delete FeedStore entirely, update all dependents to use ViewModel.

---

## 3. Update RssReaderApp (Desktop) — MODIFY

**Path:** `shared/src/commonMain/kotlin/com/github/jetbrains/rssreader/ui/RssReaderApp.kt`

**Changes:**
- Replace `koinInject<FeedStore>()` with `koinInject<FeedViewModel>()`
- Replace `store.observeState().collectAsState()` with `viewModel.state.asState()`
- Replace `store.observeSideEffect().collectAsState()` with `viewModel.effects.collectAsStateWithLifecycle()`
- Call `viewModel.loadFeeds()`, `viewModel.addFeed()`, `viewModel.deleteFeed()` instead of `store.dispatch()`
- Replace `FeedAction.Refresh`, `FeedAction.Add`, `FeedAction.Delete` usage with direct ViewModel method calls

**Key code transformations:**

```kotlin
// BEFORE (NanoRedux):
val store: FeedStore = koinInject<FeedStore>()
val progress by store.observeState().collectAsState(null)
val effects by store.observeSideEffect().collectAsState(null)

// AFTER (ViewModel):
val viewModel: FeedViewModel = koinInject()
val progress by viewModel.state.asState().progress
val effects by viewModel.effects.collectAsStateWithLifecycle()

// Trigger actions:
onRefreshButtonClick = { viewModel.loadFeeds(true) }
onAddClick = { /* get url, then */ viewModel.addFeed(url) }
onDeleteClick = { url, feed; viewModel.deleteFeed(url) }
```

---

## 4. Update iOS Compose UI — MODIFY

**Path:** `iosMain/.../FeedListScreen.kt` (or equivalent)

**Changes:**
- Replace Store injection with ViewModel injection via Koin
- Observe `state` via `viewModel.state.asState()`
- Collect effects via `viewModel.effects.collectAsStateWithLifecycle()`
- Call ViewModel suspend functions directly

**Key transformation:**
```kotlin
// BEFORE:
val store: FeedStore = koinInject()
val state by store.observeState().collectAsState()
val effects by store.observeSideEffect().collectAsState()

// AFTER:
val viewModel: FeedViewModel = koinInject()
val state by viewModel.state.asState()
val effects by viewModel.effects.collectAsStateWithLifecycle()
```

All composable function signatures (`FeedItemList`, `FeedList`, etc.) remain the same — only the state source changes.

---

## 5. Update Android App UI — MODIFY

**Path:** `androidApp/src/main/.../MainScreen.kt` / `FeedListScreen.kt`

**Changes:**
- Use `viewModel()` to get `FeedViewModel`
- Observe `state` via `liveData`/`asAndroidXLifecycle()` or `asState()` depending on setup
- Call `viewModel.loadFeeds()`, etc. via button onClicks

**Example:**
```kotlin
@Composable
fun MainScreen(
    onRefresh: () -> Unit,
    onAddClick: () -> Unit
) {
    val viewModel: FeedViewModel = viewModel()
    val by by viewModel.state.asState()  // or liveData { ... }
    
    Button(onClick = viewModel.loadFeeds(true)) {
        Text("Refresh")
    }
    
    Button(onClick = { /* show add dialog, then */ viewModel.addFeed(url) }) {
        Text("Add Feed")
    }
}
```

---

## 6. Remove NanoRedux Dependencies — MODIFY

**Build.gradle.kts (shared):**
- Remove any NanoRedux-specific dependencies if still present
- Keep Koin for ViewModel injection only
- Keep `kotlinx.coroutines` for `viewModelScope`

**Sealed classes to KEEP** (they're good, just not used as store actions anymore):
- `FeedAction` — can be kept or removed if no longer referenced
- `FeedSideEffect` — keep for the effect system, now emitted from ViewModel

**Sealed classes to potentially REMOVE:**
- `FeedAction` — if no composable calls `store.dispatch(action)` anymore
- Confirm no remaining references before removal

---

## 7. Update Tests — MODIFY/ADD

**Add:** `shared/src/test/.../FeedViewModelTest.kt` (Hilt-based)

```kotlin
@HiltAndroidTest
class FeedViewModelTest {
    @get:Rule
    var hiltRule: HiltRule = HiltRule.create()

    @Test
    fun `loadFeeds sets progress and emits data`() = runTest {
        val viewModel = hiltRule.hiltAndroidRule.injector().get(FeedViewModel::class.java)
        
        viewModel.loadFeeds(force = true)
        
        assertTrue(viewModel.state.value.progress)
        // Verify effects flow if needed
    }
}
```

**Update:** Any UI tests that previously used `store.dispatch()` to now call `viewModel.loadFeeds()`, etc.

---

## 8. Verify Build

```bash
# From project root
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
./gradlew :shared:compileKotlin --no-configuration-cache
./gradlew :androidApp:compileDebugKotlin --no-configuration-cache
```

Expected: `BUILD SUCCESSFUL` with no NanoRedux errors.

---

## 9. Manual Testing Checklist

- [ ] Feed list loads on app start (progress shows, then data appears)
- [ ] Refresh button triggers `viewModel.loadFeeds(true)`
- [ ] Add feed dialog → submit → feed appears in list
- [ ] Delete feed (swipe/delete button) → removed from list
- [ ] Error snackbar appears on network failures
- [ ] App stays in correct state on rotation (test with configChanges or emulator rotation)
- [ ] No memory leaks (verify viewModelScope cancels properly)
- [ ] iOS Compose UI builds and shows same behavior (if on macOS)

---

## 10. Documentation Updates

- Update README to reflect ViewModel-based architecture
- Remove references to "NanoRedux store" or "custom Redux implementation"
- Document how to add new feed via ViewModel methods
- Note: State is retained across configuration changes via ViewModel

---

## Branch & PR Workflow

```bash
# 1. Already done: created branch
git checkout -b priority-2-viewmodel-refactor master

# 2. Make all changes above
#    - Create FeedViewModel.kt
#    - Modify RssReaderApp.kt (desktop)
#    - Modify iOS UI files
#    - Modify Android UI files
#    - Update build.gradle.kts
#    - Delete or convert FeedStore.kt

# 3. Commit and push
git add .
git commit -m "feat: migrate from NanoRedux to ViewModel + StateFlow (priority #2)
- Create FeedViewModel with lifecycle-scoped state and effects
- Replace FeedStore usage in desktop, Android, and iOS UI
- Remove custom CoroutineScope(Dispatchers.Main) leakage
- Migrate from action-based dispatch to method-based ViewModel API"

# 4. Push to fork
git push origin priority-2-viewmodel-refactor

# 5. Create PR on GitHub
#    - Title: "Migrate from NanoRedux to ViewModel + StateFlow (Priority #2)"
#    - Description: See PRIORITY-2-IMPLEMENTATION-PLAN.md
#    - Reviewers: self or team
#    - Link: compare view with master
```

---

## Success Criteria

- [ ] `./gradlew assembleDebug --no-configuration-cache` builds successfully
- [ ] Feed list UI works across all three platforms (Android, iOS, desktop)
- [ ] State rotates correctly (preserves expanded/collapsed state)
- [ ] No `CoroutineScope(Dispatchers.Main)` leaks in profiler
- [ ] Error handling shows proper snackbar/Toast messages
- [ ] ViewModel tests pass (if added)

---

*Plan version: 1.0 — Created for antigravity CLI agent execution on branch priority-2-viewmodel-refactor*
