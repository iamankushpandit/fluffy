/**
 * Fluffy Batch Dashboard — Events / Actions
 */
const FluffyEvents = (() => {
  'use strict';

  function showToast(message, type) {
    type = type || 'info';
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { if (toast.parentNode) toast.parentNode.removeChild(toast); }, 4000);
  }

  function init() {
    // Delegated click handler
    document.addEventListener('click', async (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) return;
      const action = btn.getAttribute('data-action');

      if (action === 'start-job') {
        const jobName = btn.getAttribute('data-job');
        let params = [];
        try { params = JSON.parse(btn.getAttribute('data-params') || '[]'); } catch (_) {}
        FluffyRender.renderStartJobForm(jobName, params);
        FluffyRender.showModal('start-job-modal');
      }

      if (action === 'view-detail') {
        const id = btn.getAttribute('data-id');
        try {
          const detail = await FluffyApi.getStatus(id);
          FluffyRender.renderDetail(detail);
          FluffyRender.showModal('detail-modal');
        } catch (err) {
          showToast('Failed to load details: ' + err.message, 'error');
        }
      }

      if (action === 'retry') {
        const id = btn.getAttribute('data-id');
        try {
          await FluffyApi.retryJob(id);
          showToast('Retry submitted for execution #' + id, 'success');
          FluffyApp.refresh();
        } catch (err) {
          showToast('Retry failed: ' + err.message, 'error');
        }
      }

      if (action === 'stop') {
        const id = btn.getAttribute('data-id');
        try {
          await FluffyApi.stopJob(id);
          showToast('Stop requested for execution #' + id, 'success');
          FluffyApp.refresh();
        } catch (err) {
          showToast('Stop failed: ' + err.message, 'error');
        }
      }
    });

    // Start job form submit
    const formEl = document.getElementById('start-job-form');
    if (formEl) {
      formEl.addEventListener('submit', async (e) => {
        e.preventDefault();
        const jobName = document.getElementById('start-job-name')?.value;
        if (!jobName) return;

        const paramInputs = formEl.querySelectorAll('[data-param]');
        const parameters = {};
        paramInputs.forEach(input => { parameters[input.getAttribute('data-param')] = input.value; });

        const args = document.getElementById('start-job-args')?.value || null;
        const user = document.getElementById('start-job-user')?.value || null;

        const payload = {};
        if (Object.keys(parameters).length > 0) payload.parameters = parameters;
        if (args) payload.arguments = args;
        if (user) payload.requestedBy = user;

        try {
          const result = await FluffyApi.startJob(jobName, payload);
          showToast('Job "' + jobName + '" started — execution #' + result.jobId, 'success');
          FluffyRender.hideModal('start-job-modal');
          FluffyApp.refresh();
        } catch (err) {
          showToast('Start failed: ' + err.message, 'error');
        }
      });
    }

    // Modal close buttons
    document.querySelectorAll('.modal-close').forEach(btn => {
      btn.addEventListener('click', () => {
        const modal = btn.closest('.modal-overlay');
        if (modal) modal.classList.add('hidden');
      });
    });

    // Close modal on overlay click
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
      overlay.addEventListener('click', (e) => {
        if (e.target === overlay) overlay.classList.add('hidden');
      });
    });

    // Close modal on Escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay:not(.hidden)').forEach(m => m.classList.add('hidden'));
      }
    });

    // Status filter clicks
    document.getElementById('status-filters')?.addEventListener('click', (e) => {
      const btn = e.target.closest('.filter-btn');
      if (!btn) return;
      FluffyState.statusFilter = btn.getAttribute('data-status');
    });

    // Search input
    document.getElementById('search-input')?.addEventListener('input', (e) => {
      FluffyState.searchQuery = e.target.value;
    });

    // Theme toggle
    document.getElementById('theme-toggle')?.addEventListener('click', () => {
      const current = document.documentElement.getAttribute('data-theme');
      const next = current === 'dark' ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { sessionStorage.setItem('fluffy-theme', next); } catch (_) {}
    });

    // Restore theme
    try {
      const saved = sessionStorage.getItem('fluffy-theme');
      if (saved) document.documentElement.setAttribute('data-theme', saved);
    } catch (_) {}

    // Token input
    document.getElementById('token-input')?.addEventListener('change', (e) => {
      const token = e.target.value.trim();
      FluffyApi.setToken(token);
      if (token) {
        try { sessionStorage.setItem('fluffy-token', token); } catch (_) {}
      } else {
        try { sessionStorage.removeItem('fluffy-token'); } catch (_) {}
      }
      showToast(token ? 'Token updated' : 'Token cleared', 'info');
    });

    // Restore token
    try {
      const savedToken = sessionStorage.getItem('fluffy-token');
      if (savedToken) {
        FluffyApi.setToken(savedToken);
        const input = document.getElementById('token-input');
        if (input) input.value = savedToken;
      }
    } catch (_) {}

    // Handle 401
    FluffyApi.onUnauthorized(() => {
      FluffyApi.setToken(null);
      try { sessionStorage.removeItem('fluffy-token'); } catch (_) {}
      const input = document.getElementById('token-input');
      if (input) input.value = '';
      FluffyApp.pauseRefresh();
      FluffyState.executions = [];
      showToast('Unauthorized — token cleared, auto-refresh paused', 'error');
    });
  }

  return { init, showToast };
})();
