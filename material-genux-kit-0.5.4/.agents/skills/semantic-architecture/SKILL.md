---
name: semantic-architecture
description: Standards for modular, type-safe, and semantic React component organization.
---
# Semantic Component Architecture Skill

Goal: Maintain a 100% modular, type-safe, and semantic codebase where every component and page is isolated, easy to test, and follows the "one function per file" rule.

## When to Use This Skill

You MUST use this skill whenever you are:
- **Creating a new UI component** (even if it seems "small").
- **Adding a new Page or View** to the application.
- **Refactoring existing code** to improve readability or maintainability.
- **Organizing folders** or setting up project structure for a new feature.

## Core Principles

1.  **Modular Folder Structure**: Every non-trivial component or page must have its own dedicated directory.
2.  **Explicit Type Definitions**: Every component directory must contain a `types.ts` file for its props and local interfaces.
3.  **Single Export per File**: Each file should export exactly one main function or component.
4.  **Helper Isolation**: Internal logic that doesn't render UI should be extracted into a `helpers/` subfolder, with one function per file.

## Directory Structure

When creating a new component or page, follow this exact structure:

```text
[component-name]/
├── index.tsx      # Main component implementation (Export 1 component)
├── types.ts       # Type definitions for the component (Props, States)
└── helpers/       # (Optional) Domain-specific utility functions
    ├── helper1.ts # Export 1 function
    └── helper2.ts # Export 1 function
```

## Implementation Workflow

### 1. Types First (`types.ts`)
Always define the props interface before writing the component.
```typescript
// types.ts
import { SharedType } from '@/types/shared';

export interface ComponentNameProps {
  data: SharedType;
  onAction: () => void;
}
```

### 2. Implementation (`index.tsx`)
Keep the component focused on UI rendering. Use helpers for complexity.
```tsx
// index.tsx
import { ComponentNameProps } from './types';
import { processData } from './helpers/processData';

export function ComponentName({ data, onAction }: ComponentNameProps) {
  const processed = processData(data);
  return <div onClick={onAction}>{processed}</div>;
}
```

### 3. Logic Extraction (`helpers/`)
If a calculation or data transformation takes more than 5 lines, extract it.
```typescript
// helpers/processData.ts
export function processData(input: any): string {
  // complex logic here
  return "result";
}
```

## Global Standards

- **Shared Types**: Put models used by multiple features in `src/types/`.
- **Shared Data**: Put mock data or constants in `src/data/`.
- **Pages vs Components**:
    - `src/pages/`: Orchestrators for distinct routes/views.
    - `src/components/`: Reusable UI blocks, categorized by domain (e.g., `video/`, `layout/`, `shared/`).

## Self-Correction Checklist

Before finishing a task, ask yourself:
1. Did I put more than one function in a single file? (Fix: Extract to `helpers/`).
2. Is there a `types.ts` for this component? (Fix: Create it).
3. Is mock data mixed with UI? (Fix: Move to `src/data/`).
4. Is the component in its own folder? (Fix: Create folder).
