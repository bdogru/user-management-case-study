# AI collaboration

Claude Code writes, Codex reviews, the owner decides and merges. Per step: the review findings that
changed the work, disputes and owner decisions.

## Step 1 — Backend

- **F-1 (PR #1, plan).** Codex found that T1 could not reliably catch a locale-dependent
  `toLowerCase()`, because it relied on the machine's locale. T1 now sets the `tr_TR` default locale
  itself. Codex's premise, that Maven runs the tests in `en_US`, was wrong: the Surefire report in
  PR #2 shows the test JVM ran as `tr_TR`. The fix was needed anyway, because CI runs on Ubuntu.
- **PR #2 (backend code).** Approved in the first round, without findings.
- **Disputes:** none. **Owner decisions:** none.
