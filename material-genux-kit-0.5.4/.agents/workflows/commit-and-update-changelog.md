---
description: Automatically update the changelog by date using the existing format, and create a conventional commit.
---

This workflow helps you commit the current changes, using Conventional Commits, and accurately keep your changelog updated with the correct format based on the date.

1. **Review Changes**: Run `git status` and `git diff` to analyze the staged and unstaged changes.
2. **Analyze Existing Changelog Format**: Use the `view_file` tool to read the existing `CHANGELOG.md` file. You must strictly follow the same formatting (headers, bullet points, spacing) as the existing document. If it groups by date, follow the exact same date format. 
3. **Update Changelog**: Read the diff and generate a concise summary of the changes. Group these changes under today's date adhering to the `CHANGELOG.md` format analyzed in the previous step. Prepend or insert this new entry in the `CHANGELOG.md` file. Provide a brief summary of the updated changelog to the user for review.
4. **Stage Changes**:
Run `git add .` to stage the changes including the updated changelog.
5. **Create Conventional Commit**: Generate a commit message that strictly follows the **Conventional Commits** specification (e.g., `feat: ...`, `fix: ...`, `refactor: ...`, `chore: ...`). Explain the commit format briefly to the user. Then, run the `git commit -m "<message>"` command to finalize the commit.