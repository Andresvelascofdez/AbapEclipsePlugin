==================================================
IP BOX / INTELLECTUAL PROPERTY DOCUMENTATION RULE
==================================================

This project may be used as supporting technical documentation for an IP Box application.

For that reason, the repository must maintain a clear, factual and auditable technical development record.

Important product and ownership context:

- The product idea, functional concept, business use case, technical direction and acceptance decisions come from the project owner: Andres Velasco / ANVEL Consulting / ERP Utilities Services Ltd.
- The documentation must describe the software as a proprietary ABAP/Eclipse product with its own requirements, architecture, validation history and product evolution.
- The documentation must focus on technical facts: what was built, why it was built, how it was validated, what limitations remain and how each feature supports the ABAP/Eclipse consulting workflow.
- Do not describe the project as generated automatically or as authored by external tooling.
- Do not fabricate evidence, dates, time spent, test results or decisions.
- All documentation must remain truthful, technically accurate and auditable.

The repository must maintain an IP Box documentation folder:

/docs/ip-box/

This folder should contain:

1. development_log.md
   A chronological record of meaningful development steps.
   Each entry should include:

- date
- feature/module worked on
- technical objective
- implementation summary
- files changed
- user/business reason
- validation status
- open limitations
- product decision/review notes

2. architecture_decisions.md
   A record of important technical decisions.
   Each decision should include:

- decision title
- context
- options considered
- selected option
- reason for selection
- expected benefit
- risks/limitations
- relation to SAP/ABAP/Eclipse/ADT use case
- product decision owner or rationale

3. feature_register.md
   A structured list of implemented and planned features.
   Each feature should include:

- feature name
- phase
- status
- business purpose
- technical description
- files/modules involved
- validation method
- IP relevance / productivity relevance

4. evidence_register.md
   A register of evidence that can later support IP Box documentation.
   Examples:

- screenshots to be taken manually
- demo scenarios
- test cases
- commit references
- build logs
- changelog entries
- design notes
- manual validation notes

5. test_and_validation_log.md
   A record of manual and automated validation.
   Each entry should include:

- test date
- tested feature
- test scenario
- expected result
- actual result
- status
- issues found
- follow-up required
- reviewer/validator

Documentation maintenance rule:

Whenever a feature is implemented or changed, update the relevant product documentation files.

At minimum, every meaningful implementation must update:

- README.md
- CHANGELOG.md
- /docs/ip-box/development_log.md
- /docs/ip-box/feature_register.md

When a technical decision is made or clarified, also update:

- /docs/ip-box/architecture_decisions.md

When a feature is tested or manual validation is described, also update:

- /docs/ip-box/test_and_validation_log.md
- /docs/ip-box/evidence_register.md when relevant evidence exists or remains pending

Important:
The purpose is not to create artificial legal claims.
The purpose is to preserve a truthful technical and development record showing:

- what was built
- why it was built
- which technical problems it solves
- which product and architecture decisions were made
- how the software evolved over time
- how the tool may improve productivity in SAP ABAP/Eclipse consulting work
- what evidence exists for design, build, testing and validation

Do not invent productivity percentages, financial values, tax conclusions or legal statements.
If such numbers are needed later, leave placeholders for the project owner or tax advisor.

Add a disclaimer in the IP Box documentation:
“This documentation is a technical development record. It is not legal or tax advice. Final IP Box eligibility, valuation and income attribution must be reviewed by qualified advisors.”

IP Box documentation should support the position that:

- the tool is proprietary software developed under the direction of the project owner
- the tool solves a specific technical/business problem in SAP ABAP/Eclipse consulting workflows
- the tool has a documented architecture and development history
- the tool includes original requirements, design decisions, testing and validation
- the tool has a traceable product roadmap and evidence register

GitHub publishing rule:

- After completing each key task or milestone, commit and push the relevant changes to the correct GitHub repository for this project: https://github.com/Andresvelascofdez/AbapEclipsePlugin.
- Before pushing, verify that the local git remote points to Andresvelascofdez/AbapEclipsePlugin.
- Never push this project's changes to any unrelated repository.
- If the git remote is missing or points to the wrong repository, stop, report the issue, and confirm the safe repository setup before pushing.
