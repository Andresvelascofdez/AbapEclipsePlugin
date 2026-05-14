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

## 2026-05-13 - API Key Configuration Report

- Requirement/problem reported by owner: `.env` exists, but the Eclipse view still showed `OPENAI_API_KEY is required`.
- Evidence provided by owner: screenshot of the ABAP Chat view showing the missing API key message after pressing `Ask`.
- Functional decision made by owner: use `.env` in the project for testing.
- Codex response: implemented Eclipse workspace `.env` discovery and validated it with Eclipse smoke tests plus a live OpenAI smoke test.
- Manual testing by owner: TODO/TBC after pulling the fixed version and relaunching Eclipse.

## 2026-05-14 - Free-Form Open-Editor Chat Feedback

- Requirement/problem reported by owner: the initial button-driven workflow was not automatic enough; the desired behaviour is closer to a chat that can read the relevant open working context.
- Functional decision made by owner: the plug-in should read all open Eclipse editor tabs by default when asking a question, without manual context-loading buttons or a visible context panel.
- Evidence provided by owner: screenshot of a Z report opened in ADT and ABAP Chat returning useful output.
- Codex response: simplified the UI, made `Ask` gather all open text editors automatically, preserved user-confirmed/manual code application, and updated validation/documentation.
- Manual testing by owner: owner reported that the automatic open-editor behaviour works reasonably.

## 2026-05-14 - Question Clearing And Documentation Request

- Requirement/problem reported by owner: after asking, the text remained in the `Question` box.
- Functional decision made by owner: each accepted question should clear the input box, and all documentation including IP Box records should reflect the current workflow.
- Codex response: updated `ChatView` to clear `Question` after a valid ask request, refreshed README, installation guide, test plan and IP Box records, and will rerun validation before publishing.
- Manual testing by owner: TODO/TBC after installing the updated bundle.
