# Material GenUX Kit Starter Template

This starter template provides the boilerplate required to create a Google Material Design application using gBreeze, React, Tailwind CSS, and Vite.

## Getting Started

### Requirements

Ensure you have installed [Node.js](https://nodejs.org/en/download/).

Ensure browser tools are enabled in Jetski.
  - Press "⌘ + ," (or click "Jetski > Settings > Jetski Settings" in the menu bar)
  - Click Browser in the sidebar
  - Enable "Browser Tools"
  - [Video Example](https://screencast.googleplex.com/cast/NjY4NzU2MDM4OTYyMzgwOHw4MTdhYzcxNi0zMA)

### Prototyping

Then, tell the agent what you want to build.
Jetski will initialize the project, open Chrome, and start building!

## Example Prompts

- "Show me a dashboard of AI usage for my team."
- "Make a simple calculator app."
- "Build the Youtube frontpage, but make it use Material Design."

## AI Agent Instructions

If you are an AI assistant helping to generate UI or modify this project, please consult the following agent skills for detailed rules, guidelines, and context:

- **`genui` skill (`.agents/skills/genui/SKILL.md`)**: Start here. The overarching guidelines and rules for creating Material UI using the gBreeze library.
- **`gbreeze` skill (`.agents/skills/gbreeze/SKILL.md`)**: Full specifics on gBreeze variables, sub-systems, colors, and components.
- **`design` skill (`.agents/skills/design/SKILL.md`)**: Comprehensive Material layout patterns, breakpoints, spacing grids, and elevation rules.

## Deployment

Deploying this app is done using the Zipline CLI. Simply run the following command to build the production bundle and upload the assets:

```bash
npm run deploy
```

If it's your first time, the CLI will interactively guide you through authentication.
