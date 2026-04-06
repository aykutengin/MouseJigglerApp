# Project Context

**Core Objective:** To provide a simple, non-intrusive way to keep a computer session active.

**Technical Architecture:**
- The app uses a background loop to trigger mouse events.
- UI is built with minimalist principles for ease of use.
- Interaction with the OS is handled via JAVA awt API.

**AI Development Notes:**
- Priority is given to stability and low CPU usage.
- Ensure mouse movements are small (1-5 pixels) to avoid interrupting the user's actual work.
- Always check if the "Stop" flag is active before the next movement cycle.