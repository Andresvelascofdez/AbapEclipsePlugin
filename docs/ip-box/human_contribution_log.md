# Human Contribution Log

This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.

## 2026-05-11 - Project Direction And Repository Boundary

- Requirement provided by owner: confirm access to `Andresvelascofdez/AbapEclipsePlugin`, read local `instrucciones.md`, and avoid mixing the project with `Andresvelascofdez/SapIsuAssistant`.
- Functional decision made by owner: use `AbapEclipsePlugin` as the GitHub destination for this project.
- Review/acceptance decision: owner confirmed that the project must not be mixed with `SapIsuAssistant`.
- Business rationale: maintain a separate project history and avoid repository contamination.

## 2026-05-11 - Development And Validation Request

- Requirement provided by owner: begin implementing the tasks in `instrucciones.md`, create a Markdown guide for Eclipse installation and testing, and test each functionality before confirming.
- Functional decision made by owner: add a recurrent push rule after key tasks.
- SAP/ABAP domain knowledge contributed by owner: the project direction is an Eclipse assistant for ABAP-related work.
- Security note: an OpenAI API key was provided in the conversation. Codex did not persist the real key in project files and recommends revoking it because it appeared in chat history.
- Manual testing by owner: TODO/TBC after Eclipse runtime validation.
- Review/acceptance decision: TODO/TBC.

## 2026-05-12 - Eclipse Import Error Report

- Requirement/problem reported by owner: importing the project into Eclipse produced errors resolving `org.eclipse.*`, SWT/JFace widgets and `ViewPart`.
- Evidence provided by owner: screenshots from the Eclipse Problems view showing unresolved `Button`, `Combo`, `Composite`, `SWT`, `ISelectionService`, `Job`, `Status` and related Eclipse types.
- Functional decision made by owner: request clarification/fix based on observed import errors.
- Codex response: adjusted Java baseline to 11, documented PDE/Target Platform requirements, and added an Eclipse prerequisite diagnostic script.
- Manual testing by owner: TODO/TBC after installing or activating PDE and cleaning/reimporting the project.

## 2026-05-12 - Runtime Validation Challenge

- Requirement/problem reported by owner: Codex had not validated the plugin inside Eclipse despite being asked to test before confirming.
- Evidence provided by owner: screenshots and `java.lang.Exception.txt` showing Eclipse could not create `com.abap.assistant.ui.ChatView` and could not resolve `com.abap.assistant` icon resources.
- Functional decision made by owner: require an Eclipse test plan, code rework, execution of those tests, and cleanup of generated junk files.
- Codex response: aligned bundle/view identity with the runtime workspace, added icon packaging, created Eclipse runtime smoke tests, and executed them against the local Eclipse installation.
- Manual testing by owner: TODO/TBC after pulling the fixed version.
