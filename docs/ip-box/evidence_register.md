# Evidence Register

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

| Evidence Item | Status | Location/Reference | Notes |
| --- | --- | --- | --- |
| Initial automated validation output | Available locally | `scripts/test.ps1` run on 2026-05-11 | Command completed with `All core tests passed.` and `Validation completed successfully.` |
| Eclipse runtime smoke test output | Available locally | `scripts/test-eclipse.ps1` run on 2026-05-12 | Clean Eclipse runtime smoke test passed against `C:\Users\Admin\Downloads\eclipse-java-2026-03-R-win32-x86_64\eclipse`. |
| Persisted workspace smoke test output | Available locally | `scripts/test-eclipse.ps1 -WorkspaceTemplate C:\Users\Admin\runtime-EclipseApplication -KeepPersistedState` run on 2026-05-12 | Smoke test passed with persisted workbench state copied from the runtime workspace. |
| Live OpenAI integration smoke test output | Available locally | `scripts/smoke-openai.ps1` run on 2026-05-13 | Local `.env` was used and the configured model returned a response. API key was not recorded. |
| Eclipse project import/build smoke test output | Available locally | `scripts/test-eclipse-project-build.ps1` run on 2026-05-14 | Clean Eclipse workspace imported and built the project without Java/PDE error markers. |
| Free-form editor context validation output | Available locally | `scripts/test.ps1`, `scripts/test-eclipse.ps1`, `scripts/test-eclipse-project-build.ps1`, `scripts/smoke-openai.ps1` run on 2026-05-14 | Tests passed after adding free-chat UI, active/open editor context loading and related-reference extraction. |
| Question clearing and documentation refresh validation | Available locally | 2026-05-14 validation commands | Local validation, Eclipse runtime smoke test, Eclipse import/build smoke test, runtime `.env` smoke test and live integration smoke test passed. Manual UI confirmation remains TODO/TBC. |
| Automatic context snapshot validation | Available locally | 2026-05-14 validation commands | Core validation, clean Eclipse runtime smoke test, Eclipse import/build smoke test, runtime `.env` smoke test, persisted-workspace smoke test and live integration smoke test passed after adding local related-source lookup, history and context summary. |
| Technical IP dossier | Available | `docs/ip-box/technical_ip_dossier.md` | Product-focused dossier covering overview, software boundary, architecture, safety model, evidence model, roadmap and advisor questions. |
| ABAP dependency/risk/review validation | Available locally | 2026-05-15 validation commands | `scripts/test.ps1` passed with unit coverage for references, custom/Z objects, risk signals, prompt summary and suggested change review header generation. Eclipse import/build and clean runtime smoke tests also passed. |
| Simplified chat UI validation | Available locally | 2026-05-17 validation commands | `scripts/test.ps1`, `scripts/test-eclipse-project-build.ps1` and `scripts/test-eclipse.ps1` passed after removing the visual context summary panel while retaining internal prompt analysis. |
| Eclipse runtime screenshot | TODO/TBC | To be captured manually | Capture `ABAP Chat` view after runtime launch using anonymised ABAP snippet only. |
| Exported plug-in artifact | TODO/TBC | Eclipse PDE export destination | Record artifact name and Eclipse version after manual export. |
| Simplified chat view screenshot | TODO/TBC | To be captured manually | Capture the updated view showing question, response and suggested-change review panels without a secondary context summary panel, using anonymised or non-confidential ABAP code. |
| Suggested change review panel evidence | TODO/TBC | Manual Eclipse screenshot/test | Capture the review panel after a response containing a fenced ABAP suggestion; confirm copy-only behaviour. |
| Side-by-side diff evidence | Planned/TBC | Future release | Record when richer side-by-side diff workflow is implemented and validated. |
| Local usage/evidence logging output | Planned/TBC | Future `data/ip_box` or configured local path | Record only if privacy-preserving usage logging is implemented. |
| GitHub commit reference | Available | `Andresvelascofdez/AbapEclipsePlugin` | Initial scaffold pushed as commit `09793a4645a734b7d8e60751a199a4facf5400a6`. |
| Product acceptance notes | TODO/TBC | `docs/ip-box/development_log.md` and release notes | Add after manual review and acceptance or rejection of each milestone. |
