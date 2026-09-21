---
name: boilerplate-dev
description: Development server and CLI conventions for the Material GenUX Kit boilerplate.
---
# Development Environment Guidelines

Use this skill when a user initiates a prototyping task or wants to see the application.

## 1. Initialize the Application
Before writing or modifying code, ensure the application is running.
- **Run the Dev Server**: Execute `npm run dev` to start the development server if it isn't already active.
- **Wait for Readiness**: Wait for the build process to complete and for the "Local: " URL to appear in the terminal output before proceeding.

## 2. Verify Port
By default, the Vite application runs at http://localhost:5173/. If the port is different, verify the exact port by checking `vite.config.ts`, `.env` files, or the `stdout` stream of the running dev command.

## 3. Open Browser
Ensure the user can see what's happening.
- **Open URL**: Use the `open_browser_url` tool to open the application in Chrome.
- **Visual Feedback**: If making major UI changes, consider using the `browser_subagent` to take screenshots and include them in your updates or walkthroughs.

## 4. Iteration
When iterating on the application, keep the Walkthrough updated with the latest changes. Do not remove any content from the Walkthrough unless explicitly asked to do so. Instead, append new content to the Walkthrough. The user will want to see the journey from beginning to end of how we got to the final result.

## 5. Finishing Up
When the user is finished prototyping, stop the dev server with the `manage_task` tool. Don't make assumptions about whether the user is finished prototyping or not, but ask them explicitly. Try not to ask too much, but make a reasonable guess from the context.
