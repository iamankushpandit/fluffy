/**
 * Fluffy Batch Dashboard — Application bootstrap & auto-refresh
 */
const FluffyApp = (() => {
  'use strict';

  let _refreshTimer = null;
  let _refreshing = false;
  let _paused = false;

  async function loadConfig() {
    try {
      const config = await FluffyApi.getDashboardConfig();
      FluffyState.config = config;
    } catch (_) {
      // Use defaults
    }
  }

  async function loadJobs() {
    try {
      FluffyState.jobs = await FluffyApi.listRegisteredJobs();
    } catch (err) {
      FluffyEvents.showToast('Failed to load jobs: ' + err.message, 'error');
    }
  }

  async function loadExecutions() {
    try {
      FluffyState.executions = await FluffyApi.listExecutions();
    } catch (err) {
      FluffyEvents.showToast('Failed to load executions: ' + err.message, 'error');
    }
  }

  async function refresh() {
    if (_refreshing) return;
    _refreshing = true;
    try {
      await Promise.all([loadJobs(), loadExecutions()]);
    } finally {
      _refreshing = false;
    }
  }

  function startRefresh() {
    _paused = false;
    updateRefreshIndicator();
    stopRefreshTimer();
    const interval = (FluffyState.config.refreshInterval || 5) * 1000;
    _refreshTimer = setInterval(() => {
      if (!_paused) refresh();
    }, interval);
  }

  function stopRefreshTimer() {
    if (_refreshTimer) { clearInterval(_refreshTimer); _refreshTimer = null; }
  }

  function pauseRefresh() {
    _paused = true;
    updateRefreshIndicator();
  }

  function resumeRefresh() {
    _paused = false;
    updateRefreshIndicator();
    refresh();
  }

  function togglePause() {
    if (_paused) resumeRefresh(); else pauseRefresh();
  }

  function updateRefreshIndicator() {
    const el = document.getElementById('refresh-indicator');
    if (!el) return;
    if (_paused) {
      el.textContent = '⏸ Paused';
      el.className = 'refresh-indicator paused';
    } else {
      const secs = FluffyState.config.refreshInterval || 5;
      el.textContent = '↻ ' + secs + 's';
      el.className = 'refresh-indicator';
    }
  }

  async function init() {
    await loadConfig();
    FluffyState.subscribe(() => FluffyRender.renderAll());
    FluffyEvents.init();

    // Wire refresh controls
    document.getElementById('btn-refresh')?.addEventListener('click', () => refresh());
    document.getElementById('btn-toggle-pause')?.addEventListener('click', () => togglePause());

    await refresh();
    FluffyRender.renderAll();
    startRefresh();
    updateRefreshIndicator();
  }

  // Boot
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  return { refresh, pauseRefresh, resumeRefresh };
})();
