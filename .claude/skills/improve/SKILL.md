---
name: improve
description: Analyzes conversation history and improves CLAUDE.md, architectural_patterns.md, and custom skills using Anthropic best practices. Identifies patterns where Claude performed well or poorly and captures learnings into project instructions.
argument-hint: [focus-area]
disable-model-invocation: true
---

# Improve Project Instructions

Analyze conversation history and improve Claude Code instructions for this project, following [Anthropic's CLAUDE.md best practices](https://www.anthropic.com/engineering/claude-code-best-practices).

## Target Files

| File | Contains |
|------|----------|
| `CLAUDE.md` | Project overview, build commands, quick reference |
| `.claude/docs/architectural_patterns.md` | Detailed patterns, code conventions |
| `.claude/skills/*/SKILL.md` | Custom skill workflows |
| `.claude/settings.json` | Permission allowlist patterns |

## Anthropic Best Practices Reference

### CLAUDE.md Structure (WHAT/WHY/HOW Framework)

- **WHAT**: Tech stack, project structure, codebase map (especially important for monorepos)
- **WHY**: Project purpose, what components do
- **HOW**: Build commands, test commands, verification steps, project-specific quirks

### Length Guidelines

- **Target**: 100-200 lines maximum for root CLAUDE.md
- **Hard limit**: Under 300 lines - if exceeding, move details to per-folder CLAUDE.md files
- **Principle**: "Less is more" - only include what actually improves Claude's performance

### Formatting Best Practices

- Keep concise and human-readable - no required format
- Use section headers, bullet points, and brief descriptions
- Add emphasis keywords ("IMPORTANT", "YOU MUST") for critical instructions
- Avoid adding content without testing its effectiveness

### Skills Best Practices

- Store reusable workflows in `.claude/skills/*/SKILL.md`
- Support `$ARGUMENTS` for parameterization
- Include clear frontmatter with name, description, and argument-hint
- Keep workflows focused - one purpose per skill

## Workflow Checklist

```
- [ ] Step 0: Check session tracking (.claude/improve_last_run.txt)
- [ ] Step 1: Analyze conversation history since last run
- [ ] Step 2: Read current instruction files
- [ ] Step 3: Identify improvements (applying Anthropic best practices)
- [ ] Step 4: Present findings to user
- [ ] Step 5: Implement approved changes
- [ ] Step 6: Update session tracking file
```

## Step 0: Session Tracking

Read `.claude/improve_last_run.txt` to find when /improve was last run.

- **If exists**: Focus on conversation history AFTER that timestamp
- **If missing**: Review all available history

## Step 1: Analyze History

Review conversation history for:
- Tasks performed and their outcomes
- Confusion or misunderstandings
- Missing context that caused issues
- Successful patterns worth documenting

## Step 2: Read Current Files

Read all target files to understand current state. Check line counts against Anthropic guidelines.

## Step 3: Identify Improvements

Apply Anthropic best practices when identifying improvements:

**Content Quality:**
- Remove content that doesn't demonstrably improve Claude's performance
- Consolidate duplicate information
- Move detailed patterns from CLAUDE.md to architectural_patterns.md
- Add emphasis keywords only for truly critical instructions

**Structure:**
- Ensure WHAT/WHY/HOW sections are clear
- Verify concise command descriptions exist
- Check that testing/verification steps are documented

**Length:**
- Flag if CLAUDE.md exceeds 200 lines
- Suggest moving content to subdirectory files if over 300 lines

**Permissions:**
- Add frequently-granted commands to settings.json

## Step 4: Present Findings

For each suggestion:

| Field | Description |
|-------|-------------|
| **Target File** | Which file to modify |
| **Issue** | What problem exists |
| **Best Practice** | Relevant Anthropic guideline |
| **Change** | Specific modification |
| **Benefit** | Expected improvement |

Wait for user approval before implementing.

## Step 5: Implement Changes

For each approved change:
1. State file and section being modified
2. Use Edit tool to make change
3. Cite which Anthropic best practice it addresses

## Step 6: Update Tracking

After completing, update `.claude/improve_last_run.txt`:
```
YYYY-MM-DD HH:MM:SS
Last reviewed: [brief summary]
Applied best practices: [list any Anthropic guidelines applied]
```

## Guidelines Summary

| File | Target Length | Content Scope |
|------|--------------|---------------|
| CLAUDE.md | 100-200 lines | High-level overview, commands, quick reference |
| architectural_patterns.md | As needed | Detailed patterns, conventions |
| Skills | Focused | Single workflow per skill |
| settings.json | N/A | Permission allowlist patterns (`:*` suffix) |

If $ARGUMENTS provided, focus on that area.

## Focus Area

$ARGUMENTS
