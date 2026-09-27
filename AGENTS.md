# Agent Constraints and Workspace Permission Boundaries

This document defines the strict operational boundaries, permission rules, and autonomous constraints for AI agents operating in this workspace (`d:\code\an26\an-melon-geo-fence-todo`). All agents and subagents must strictly adhere to these rules at all times.

---

## 1. Workspace Directory Scope (Full Access & Autonomous Execution)

- **Allowed Scope**: The workspace root (`./`, `d:\code\an26\an-melon-geo-fence-todo\`, and all subdirectories).
- **Unrestricted Authority**:
  - The agent has full, unrestricted upfront access to perform all file and development operations inside the current working directory tree.
  - Permitted actions include: reading, inspecting, writing, editing, refactoring, creating, deleting, and executing local scripts/builds/tests.
- **No Confirmation Prompts**:
  - Do **NOT** prompt the user for interactive confirmations, reviews, or approvals when performing operations that are entirely confined within this workspace directory tree.
  - Proceed autonomously to complete tasks without interruption for intra-workspace actions.

---

## 2. External Directory Scope (Strictly Restricted & Manual Approval Required)

- **Protected Scope**: Any path outside the current workspace root is strictly protected. This includes, but is not limited to:
  - Parent directories (e.g., `..`, `d:\code\an26`, `d:\code`, `D:\`).
  - User home directory (e.g., `~`, `C:\Users\Mike`, `C:\Users\Mike\.gemini`, etc.).
  - System root, drive roots, and OS directories (e.g., `C:\`, `C:\Windows`, `C:\Program Files`, `/`).
  - Temporary and cache directories (e.g., `/tmp`, `C:\Users\Mike\AppData\Local\Temp`, scratch folders outside workspace).
  - Any external drive or secondary repository.
- **Strict Prohibition**:
  - The agent must **NOT** read, inspect, create, modify, execute, or delete files or directories outside this project root.
- **Mandatory Explicit Permission**:
  - If a specific task genuinely requires reading, modifying, or executing anything outside this workspace root, the agent **MUST** explicitly ask the user and receive manual affirmative permission *before* initiating any action.
  - The permission request must state the exact path, the reason for external access, and the planned operation.

---

## 3. Commands & Execution Policy

- **Execution Working Directory (`Cwd`)**:
  - All command executions (`run_command`), builds (e.g., `./gradlew`), tests, scripts, and file modifications must be scoped and run exclusively with the working directory set inside the workspace root (`d:\code\an26\an-melon-geo-fence-todo` or its subdirectories).
- **Prohibition of External Paths in Commands**:
  - Any command attempting to access, download to, upload from, or modify paths outside the current workspace is prohibited without prior user authorization.
  - Tools and CLI commands must not write outputs, temporary files, or logs outside the workspace tree without explicit confirmation.
- **Interactive Confirmation for External Impact**:
  - Any command that would read or touch external directories, download dependencies to non-workspace locations, or alter system-wide configurations requires explicit interactive user confirmation before execution.
