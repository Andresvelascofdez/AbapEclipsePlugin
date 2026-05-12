# Evidence Register

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

| Evidence Item | Status | Location/Reference | Notes |
| --- | --- | --- | --- |
| Initial automated validation output | Available locally | `scripts/test.ps1` run on 2026-05-11 | Command completed with `All core tests passed.` and `Validation completed successfully.` |
| Eclipse runtime smoke test output | Available locally | `scripts/test-eclipse.ps1` run on 2026-05-12 | Clean Eclipse runtime smoke test passed against `C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse`. |
| Persisted workspace smoke test output | Available locally | `scripts/test-eclipse.ps1 -WorkspaceTemplate C:\Users\Admin\runtime-EclipseApplication -KeepPersistedState` run on 2026-05-12 | Smoke test passed with persisted workbench state copied from the runtime workspace. |
| Eclipse runtime screenshot | TODO/TBC | To be captured manually by project owner | Capture `ABAP Chat` view after runtime launch using anonymised ABAP snippet only. |
| Live OpenAI smoke test output | TODO/TBC | `scripts/smoke-openai.ps1` | Requires a valid local `.env`; do not commit logs containing API keys or client data. |
| Exported plug-in artifact | TODO/TBC | Eclipse PDE export destination | Record artifact name and Eclipse version after manual export. |
| GitHub commit reference | Available | `Andresvelascofdez/AbapEclipsePlugin` | Initial scaffold pushed as commit `09793a4645a734b7d8e60751a199a4facf5400a6`. |
| Owner acceptance notes | TODO/TBC | `human_contribution_log.md` | Add after project owner reviews and accepts or rejects the milestone. |
