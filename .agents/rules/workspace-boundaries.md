# Workspace Permission Rules & Boundary Constraints

## 1. Workspace Directory Scope (Full Access & Autonomous Execution)
- **Scope**: Current workspace root (`d:\code\an26\an-melon-geo-fence-todo`) and all nested subdirectories.
- **Authority**: Full, unrestricted upfront access for reading, writing, editing, creating, deleting, and script/tool execution.
- **Autonomy**: Do NOT prompt the user for interactive confirmations or approvals for any operation fully confined within this directory tree.

## 2. External Directory Scope (Strictly Restricted)
- **Scope**: Any path outside the workspace root (`../`, `~/`, `C:\`, `/tmp`, temporary directories, or other drives).
- **Restriction**: The agent must NEVER read, create, modify, execute, or delete files or directories outside the workspace root without explicit manual permission from the user first.
- **Workflow**: Always ask explicitly and wait for affirmative user consent before performing any external filesystem operation.

## 3. Commands & Execution
- **Working Directory**: CLI commands, builds, tests, and scripts must always be scoped with working directory (`Cwd`) within the workspace root.
- **External Interaction**: Any command that reads from, writes to, downloads to, or alters paths or configurations outside the current workspace requires explicit interactive user confirmation before execution.
