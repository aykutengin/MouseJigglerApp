# AI Agent Instructions & Guidelines

This document provides context and rules for AI models (Copilot, Cursor, etc.) interacting with this repository.

## Role & Behavior
- Act as a Senior Java/Desktop App Developer.
- Prioritize system stability and low CPU overhead.
- Keep the UI responsive and non-blocking.

## Coding Standards
- **Java Version:** Use Java 25+ features where applicable.
- **Project Structure:** Follow the Java Module System (`module-info.java`).
- **Concurrency:** Use modern threading (like Virtual Threads or SwingWorker) instead of raw `Thread.sleep()` in the UI thread.
- **Logging:** Prefer standard Java Logging API over `System.out.println`.

## Project Constraints
- **Minimalism:** Do not add heavy external libraries unless strictly necessary for core functionality.
- **Safety:** Always ensure there is a clear "Emergency Stop" mechanism for the mouse jiggling loop.
- **OS Compatibility:** Ensure mouse movement logic works across Windows, macOS, and Linux.

## Specific Logic Rules
- When generating mouse movements, use a range of 1-5 pixels.
- Implement "Randomness" in intervals to mimic human behavior more accurately.