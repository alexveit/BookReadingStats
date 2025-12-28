# BookReadingStats Code Audit Prompt

## PROMPT

```
I'm uploading my BookReadingStats Android codebase for a periodic code audit. Please analyze the code and identify:

## 1. ERRORS & BUGS
- Kotlin null safety issues or type mismatches
- Coroutine scope leaks or improper cancellation
- Unhandled exceptions in suspend functions
- Race conditions in Flow collection
- Broken or incomplete functionality

## 2. CODE QUALITY
- Functions that are too long (>40 lines)
- Duplicated logic that should be centralized
- Inconsistent patterns across similar ViewModels/screens
- Poor error handling (silent catch blocks)
- Missing null checks on platform types

## 3. ARCHITECTURE & ORGANIZATION
- ViewModels doing too much (should be split or delegated)
- Logic in wrong layer (UI vs ViewModel vs Repository vs DataSource)
- Improper separation between data/domain/ui layers
- Dead code or unused functions
- Files that have grown too large (>400 lines)

## 4. ANDROID LIFECYCLE
- State not surviving configuration changes
- State not surviving process death where it should
- Improper SavedStateHandle usage
- Service binding lifecycle issues
- Flows collected without lifecycle awareness

## 5. COMPOSE & UI
- Unnecessary recompositions
- Missing remember/derivedStateOf where beneficial
- State hoisting violations
- Side effects outside LaunchedEffect/DisposableEffect
- Missing loading/error states in UI

## 6. DATABASE & SYNC INTEGRITY
- Room queries that could return stale data
- Sync status transitions that could lose data
- Missing transactions for multi-table operations
- Foreign key violations waiting to happen
- Race conditions between local writes and sync

## 7. PERFORMANCE
- Blocking calls on Main dispatcher
- N+1 query patterns in repositories
- Large Flow emissions that could be throttled
- Missing indices on frequently queried columns
- Unnecessary object allocations in hot paths

## 8. SECURITY & DATA
- Hardcoded credentials or API keys
- Sensitive data logged in production
- Missing input validation before DB writes
- Supabase RLS gaps

## 9. MAINTAINABILITY
- Missing or outdated KDoc comments
- Inconsistent naming conventions
- Magic numbers that should be constants
- Complex conditionals that need simplification
- Stringly-typed code that should use enums/sealed classes

## 10. TECHNICAL DEBT
- TODO/FIXME comments that need addressing
- Stubbed out functionality (e.g., import)
- Deprecated Android APIs still in use
- Room migrations that could be consolidated
- Workarounds that should be properly fixed

---

**Output Format:**

For each issue found, provide:
1. **File & Line**: Where the issue is
2. **Severity**: Critical / High / Medium / Low
3. **Category**: Which of the 10 categories above
4. **Issue**: What's wrong
5. **Fix**: Recommended solution with code snippet if helpful

Prioritize by severity. Group related issues together.

After the detailed list, provide:
- **Top 5 Quick Wins**: Easy fixes with high impact
- **Top 3 Refactoring Priorities**: Larger efforts worth planning
- **Testing Gaps**: Areas most in need of unit/instrumented tests

---

**Context:**
- Kotlin + Jetpack Compose UI
- MVVM + Repository pattern with offline-first sync
- Room database (local) + Supabase (remote)
- Hilt dependency injection
- Foreground TimerService for reading sessions
- WorkManager for background sync
- Key flows: Book CRUD, Reading Sessions, Timer, Statistics

**Architecture layers:**
- `data/local/` - Room DAOs and entities
- `data/remote/` - Supabase data source and DTOs
- `data/repository/` - Repository implementations
- `domain/` - Business models with computed properties
- `ui/` - Compose screens and ViewModels
- `service/` - TimerService (foreground)
```

---

## USAGE

1. Generate fresh codebase snapshot (create a script or zip manually):
   ```bash
   # From project root
   zip -r bookstats_audit_$(date +%Y%m%d).zip \
     app/src/main/java \
     app/build.gradle.kts \
     build.gradle.kts \
     gradle/libs.versions.toml \
     -x "*.class" -x "*.apk"
   ```

2. Start new Claude chat

3. Upload the zip file

4. Paste the prompt above

5. Review findings and create issues/tasks

---

## SUGGESTED FREQUENCY

- **Weekly**: Quick scan during active development
- **Monthly**: Full audit with action items
- **Before Play Store releases**: Comprehensive review
- **After adding sync/timer features**: Check for race conditions

---

## FOLLOW-UP PROMPTS

**Deep dive on sync logic:**
```
Analyze the sync flow in SyncManager and repositories. Are there any race conditions or data loss scenarios?
```

**Timer service review:**
```
Review TimerService and TimerViewModel interaction. Is state properly preserved across all lifecycle scenarios?
```

**Generate fixes:**
```
For issue #X, provide the complete fixed code.
```

**Room schema review:**
```
Analyze the Room entities and migrations. Any integrity issues or missing indices?
```

**Compose performance:**
```
Which screens have the most recomposition issues? Show me the fixes.
```
