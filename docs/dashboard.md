# Dashboard

Fluffy Batch Starter includes an optional, built-in dashboard for monitoring and controlling batch jobs. The dashboard is a lightweight, static HTML/CSS/JS UI served directly from the starter — no separate frontend build system is required.

## Enabling the Dashboard

The dashboard is **enabled by default**. To explicitly control it, add the following to your `application.properties` or `application.yml`:

```properties
# Enable or disable the dashboard (default: true)
fluffy.batch.dashboard.enabled=true

# Dashboard URL path (default: /fluffy-dashboard)
fluffy.batch.dashboard.path=/fluffy-dashboard

# Dashboard title displayed in the header
fluffy.batch.dashboard.title=Fluffy Batch Dashboard

# Auto-refresh interval in seconds (default: 5)
fluffy.batch.dashboard.refresh-interval=5

# Enable bearer token authentication for API calls (default: false)
fluffy.batch.dashboard.auth-enabled=false
```

To disable the dashboard entirely:

```properties
fluffy.batch.dashboard.enabled=false
```

## Accessing the Dashboard

Once your application is running, open:

```
http://localhost:8080/fluffy-dashboard/index.html
```

Replace `localhost:8080` with your actual host and port.

## Features

### Job Summary
The top of the dashboard shows summary cards with:
- Number of registered jobs
- Total executions
- Active (queued + started + in-progress) count
- Success, failure, and stopped counts

### Registered Jobs Table
Shows all jobs registered with the starter, including:
- Job name and description
- Execution mode (sync/async)
- Max concurrency
- Required parameters
- **Start** action button

### Executions Table
Lists all job executions with:
- Execution ID
- Job name
- Status badge (color-coded)
- Requested by
- Start and end times
- Queue position (if queued)
- Actions: **Details**, **Retry**, **Stop**

### Filtering and Search
- Filter executions by status: ALL, IN_QUEUE, STARTED, IN_PROGRESS, SUCCESS, FAILURE, STOPPED
- Search by job name or execution ID

### Start a Job
Click **Start** on any registered job to open a form dialog that:
- Dynamically renders inputs for required parameters
- Allows optional arguments and requested-by fields
- Submits to `POST /api/jobs/{jobName}/start`

### Retry and Stop
- **Retry** is available for completed, failed, or stopped executions
- **Stop** is available for queued, started, or in-progress executions
- Actions are hidden when not applicable

### Execution Details
Click **Details** to view a modal with full execution information including parameters, timing, and error messages.

### Auto-Refresh
- Execution data refreshes automatically at the configured interval
- Use the **Pause** / **Refresh** buttons in the header to control refresh behavior
- Overlapping refresh calls are prevented

### Theme Toggle
Click the **Theme** button in the header to switch between light and dark modes. The preference is stored in `sessionStorage`.

## Authentication

If your API requires bearer token authentication:

1. Set `fluffy.batch.dashboard.auth-enabled=true`
2. A token input field appears in the dashboard header
3. Enter your bearer token — it is stored in `sessionStorage` (not `localStorage`)
4. The token is sent as `Authorization: Bearer <token>` on all API calls
5. On a 401 response:
   - The token is cleared
   - Auto-refresh is paused
   - A notification is shown

Authentication is optional — if `auth-enabled` is `false` (the default), no token field is shown and API calls are made without an Authorization header.

## Dashboard Configuration Endpoint

The dashboard reads its configuration from:

```
GET /api/jobs/dashboard/config
```

This returns the configured title, refresh interval, and auth-enabled flag.

## Screenshots

_Screenshots placeholder — add screenshots of the dashboard here._

## Architecture

The dashboard consists of:
- `index.html` — main page
- `css/dashboard.css` — styles with light/dark theme support
- `js/api-client.js` — thin API wrapper for all REST calls
- `js/state.js` — client-side state management
- `js/render.js` — DOM rendering logic
- `js/events.js` — event handlers and user actions
- `js/app.js` — application bootstrap and auto-refresh

All assets are served as static resources from `classpath:/static/fluffy-dashboard/` and are bundled within the starter JAR.

## Limitations

- The dashboard is a read/control UI — the backend remains the source of truth
- No pagination yet for large execution histories
- No server-sent events — uses polling via auto-refresh
- No built-in role-based access control — use Spring Security if needed

## Multi-Node Aggregator Dashboard

For multi-node deployments, Fluffy also provides an **aggregator dashboard** that
collects job metrics from every node and presents a unified view built with
React and Material-UI.

Each node exposes `GET /api/jobs/summary` with job counts and a dashboard
availability flag.  The aggregator polls these summaries and serves a React-based
UI at `/fluffy-aggregator`.

See [Aggregator Dashboard](aggregator.md) for full details.
