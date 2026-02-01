# PR Review Feedback

This document captures feedback from Forge main branch developers on pull requests. Use this feedback to inform future development decisions and avoid repeating mistakes.

---

## Key Patterns & Guidelines

**IMPORTANT: Claude must follow these patterns when writing code in this branch.**

This section summarizes recurring themes from PR feedback for quick reference.

### General Principles
- **Keep it simple:** Code should be simple, easy to follow, and use as few lines as possible while still achieving the desired functionality.
- **Minimal diff:** Prefer small, focused changes over large refactors. The fewer lines changed, the easier to review and less risk of introducing bugs.
- **Minimize core changes:** Network-specific logic should be isolated in dedicated subclasses (e.g., `NetworkGuiGame`) rather than added to core classes like `AbstractGuiGame`.

### Code Style
- **Avoid duplicate functions:** Before creating new helper methods, search the file for existing functions with equivalent logic. Reuse rather than duplicate.
- **Check hotkey conflicts:** When assigning keyboard shortcuts, search for `VK_F[key]` and `getKeyStroke` in the codebase to ensure no conflicts with hardcoded menu accelerators (e.g., F1=Help, F11=Fullscreen).
- **Wrap parseInt/parseLong in try-catch:** System property parsing should handle `NumberFormatException` gracefully with fallback to defaults.
- **Add @Override annotations:** When implementing interface methods, always add `@Override` annotation.
- **Meaningful toString():** Classes used in logging/debugging should override `toString()` rather than inheriting from Object.

### Architecture
- **Demand-driven computation:** Expensive operations (iterating all cards, getting all abilities) should only be performed when actually needed, not proactively or on every update cycle.
- **Keep engine clean:** GUI-specific logic (UI hints, styling) belongs in View classes, not in forge-game engine classes like Player.java or PhaseHandler.java.
- **Check for mobile GUI:** Desktop-only features must check `GuiBase.getInterface().isLibgdxPort()` and return early/disable for mobile. Users switching between desktop and mobile share preferences.
- **Isolate network code:** Network-specific functionality should be in dedicated classes (NetworkGuiGame, NetGameController) rather than polluting core game classes.

### Network-Specific Guidelines
- **Delta sync efficiency:** When modifying TrackableObject properties, ensure delta tracking is properly maintained to avoid full-state fallbacks.
- **Reconnection safety:** Any changes to game initialization sequence must maintain reconnection compatibility - session establishment before state transmission.
- **Serialization compatibility:** Changes to serialized objects must maintain backwards compatibility or include migration logic.
- **Thread safety:** Network code handles concurrent operations - ensure proper synchronization when accessing shared state.

### Performance
- **Check cost of helpers:** Consider the performance cost of helper methods that might be called frequently (e.g., on every priority pass or network update).
- **Bandwidth awareness:** Network operations should minimize data transfer - prefer delta updates over full state when possible.

### Testing
- **Run batch tests:** Before submitting network PRs, run `/batchtest` to validate changes don't regress network stability.
- **Document test results:** Update `.documentation/Testing.md` with results from comprehensive test runs.
- **Headless CI compatibility:** Test classes must not depend on GUI components (`FOptionPane`, `JOptionPane`, etc.) that fail in headless CI environments. Use headless alternatives or skip GUI-dependent tests in CI.
- **Multi-process test isolation:** Tests spawning subprocesses must handle classpath/JAR discovery robustly across different environments (local dev vs CI).

### Documentation
- **Update NetworkPlay.md:** Core architectural changes should be reflected in `.documentation/NetworkPlay.md`.
- **Track known issues:** Add bugs to `.documentation/Debugging.md` with full context.

---

## Feedback Log

### PR #9642 - Network Multiplayer Optimization (2026-02-01)

**Developer Comments:**

| Reviewer | Feedback |
|----------|----------|
| **Hanmac** | Will review `serializeChangedOnly` parts; thinks some changes could be added early |
| **tool4ever** | Positive on merging overall; will review for AI hallucinations before final approval |

**Copilot Automated Review (30 comments):**

| Issue | Count | Action Required |
|-------|-------|-----------------|
| Uncaught `NumberFormatException` | ~16 | Wrap `parseInt`/`parseLong` in try-catch with defaults |
| Unused container contents | 2 | Review and remove if truly unused |
| Array bounds risk | 2 | Add bounds checking |
| Null safety (`gameView`) | 1 | Add null guard |
| Missing `@Override` | 1 | Add annotation to `updateDependencies` |
| Default `toString()` | 2 | Override in `NetGuiGame`, `GameView` |

**CI Build Failures (14 tests):**

| Category | Tests Affected | Root Cause | Fix Required |
|----------|----------------|------------|--------------|
| GUI Init | `testSequentialFiveGames`, `testSequentialIsolatedLogFiles`, `testSequentialThreeGames` | `NoClassDefFoundError: FOptionPane` - GUI class init in headless CI | Remove GUI dependencies from test harness |
| Multi-process | `testParallelTwoGames`, `testParallelThreeGames`, `testParallelFiveGames`, `testConfigurableParallel` | Subprocess JAR/classpath discovery fails in CI | Fix classpath handling for CI environment |
| Timeout | `testGameTestHarnessFactory` | Game didn't complete in 150s | Investigate CI resource constraints |
| Assertion | `testDeltaVsFullStateComparison` | Delta size > full state assertion | Review test logic or delta implementation |
| Success Rate | `runComprehensiveDeltaSyncTest`, `runMultiplayerOnlyTest`, `runQuickDeltaSyncTest`, `runTwoPlayerOnlyTest` | 0% success rate in CI | Depends on fixing above issues |

**Priority Actions:**
1. Fix `FOptionPane` dependency - use headless-safe alternatives in test harness
2. Fix multi-process classpath discovery for CI environment
3. Address Copilot code quality comments (NumberFormatException handling)
4. Review delta size assertion test logic

---

## Notes

- When PR feedback is received, add it to the Feedback Log section above
- If feedback reveals a recurring theme, update the Key Patterns & Guidelines section
- Reference specific PR numbers and dates for traceability
- Network PRs should reference test run results from `.documentation/Testing.md`
