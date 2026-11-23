# AI collaboration

Claude Code writes, Codex reviews, the owner decides and merges. Per step: the review findings that
changed the work, disputes and owner decisions.

## Step 1 — Backend

- **F-1 (PR #1, plan).** Codex found that T1 could not reliably catch a locale-dependent
  `toLowerCase()`, because it relied on the machine's locale. T1 now sets the `tr_TR` default locale
  itself. Codex's premise, that Maven runs the tests in `en_US`, was wrong: the Surefire report in
  PR #2 shows the test JVM ran as `tr_TR`. The fix was needed anyway, because CI runs on Ubuntu.
- **PR #2 (backend code).** Codex approved the first version without findings; the owner then found
  two issues.
- **O-1 (PR #2).** The OpenAPI document listed `POST /api/users` as 200 although it returns 201;
  `@ResponseStatus(CREATED)` fixed it. The finding also asked to remove `backend/.gitignore` because
  the root `.gitignore` already ignores `target/`. That premise was false (the root file ignores
  only `docs/*.pdf`), and the owner decided to keep the file.
- **O-2 (PR #2).** The production classes moved from one package into one package per layer
  (`controller`, `service`, `repository`, `entity`, `dto`, `exception`), with only what another
  package uses left public.
- **Disputes:** none. **Owner decisions:** keep `backend/.gitignore` (O-1).

## Step 2 — Frontend

- **PR #4 (screen and CI job).** Codex approved it in the first round, without findings.
- **Disputes:** none. **Owner decisions:** none.
