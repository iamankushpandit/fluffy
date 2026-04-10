/**
 * Fluffy Batch Dashboard — Rendering
 */
const FluffyRender = (() => {
  'use strict';

  const STATUS_ORDER = ['ALL', 'IN_QUEUE', 'STARTED', 'IN_PROGRESS', 'SUCCESS', 'FAILURE', 'STOPPED'];

  function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function formatTime(ts) {
    if (!ts) return '—';
    const d = new Date(ts);
    return d.toLocaleString();
  }

  function badgeHtml(status) {
    const cls = status ? status.toLowerCase() : 'unknown';
    return '<span class="badge badge-' + escapeHtml(cls) + '">' + escapeHtml(status || 'UNKNOWN') + '</span>';
  }

  function renderTitle() {
    const el = document.getElementById('dashboard-title');
    if (el) el.textContent = FluffyState.config.title || 'Fluffy Batch Dashboard';
    document.title = FluffyState.config.title || 'Fluffy Batch Dashboard';
  }

  function renderAuthSection() {
    const section = document.getElementById('auth-section');
    if (section) {
      section.style.display = FluffyState.config.authEnabled ? 'flex' : 'none';
    }
  }

  function renderSummary() {
    const counts = FluffyState.statusCounts();
    const container = document.getElementById('summary-cards');
    if (!container) return;
    container.innerHTML =
      '<div class="summary-card"><div class="count">' + FluffyState.jobs.length + '</div><div class="label">Registered Jobs</div></div>' +
      '<div class="summary-card"><div class="count">' + counts.ALL + '</div><div class="label">Total Executions</div></div>' +
      '<div class="summary-card"><div class="count">' + (counts.IN_QUEUE + counts.STARTED + counts.IN_PROGRESS) + '</div><div class="label">Active</div></div>' +
      '<div class="summary-card"><div class="count">' + counts.SUCCESS + '</div><div class="label">Success</div></div>' +
      '<div class="summary-card"><div class="count">' + counts.FAILURE + '</div><div class="label">Failure</div></div>' +
      '<div class="summary-card"><div class="count">' + counts.STOPPED + '</div><div class="label">Stopped</div></div>';
  }

  function renderJobs() {
    const tbody = document.getElementById('jobs-tbody');
    if (!tbody) return;
    const jobs = FluffyState.jobs;
    if (jobs.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">No registered jobs found</td></tr>';
      return;
    }
    tbody.innerHTML = jobs.map(j =>
      '<tr>' +
        '<td>' + escapeHtml(j.name) + '</td>' +
        '<td>' + escapeHtml(j.description || '—') + '</td>' +
        '<td>' + (j.async ? 'Async' : 'Sync') + '</td>' +
        '<td>' + escapeHtml(String(j.maxConcurrency)) + '</td>' +
        '<td class="mono">' + escapeHtml((j.requiredParams || []).join(', ') || '—') + '</td>' +
        '<td class="actions">' +
          '<button class="btn btn-primary btn-sm" data-action="start-job" data-job="' + escapeHtml(j.name) + '" data-params="' + escapeHtml(JSON.stringify(j.requiredParams || [])) + '">Start</button>' +
        '</td>' +
      '</tr>'
    ).join('');
  }

  function renderFilters() {
    const container = document.getElementById('status-filters');
    if (!container) return;
    const counts = FluffyState.statusCounts();
    container.innerHTML = STATUS_ORDER.map(s =>
      '<button class="filter-btn' + (FluffyState.statusFilter === s ? ' active' : '') + '" data-status="' + s + '">' +
        s.replace('_', ' ') + ' (' + counts[s] + ')' +
      '</button>'
    ).join('');
  }

  function renderExecutions() {
    const tbody = document.getElementById('executions-tbody');
    if (!tbody) return;
    const list = FluffyState.filteredExecutions();
    if (list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="empty-state">No executions found</td></tr>';
      return;
    }
    tbody.innerHTML = list.map(e => {
      const canStop = ['IN_QUEUE', 'STARTED', 'IN_PROGRESS'].includes(e.status);
      const canRetry = ['SUCCESS', 'FAILURE', 'STOPPED'].includes(e.status);
      return '<tr>' +
        '<td class="mono">' + escapeHtml(String(e.jobId)) + '</td>' +
        '<td>' + escapeHtml(e.jobName) + '</td>' +
        '<td>' + badgeHtml(e.status) + '</td>' +
        '<td>' + escapeHtml(e.requestedBy || '—') + '</td>' +
        '<td>' + formatTime(e.startTime) + '</td>' +
        '<td>' + formatTime(e.endTime) + '</td>' +
        '<td>' + (e.queuePosition != null ? escapeHtml(String(e.queuePosition)) : '—') + '</td>' +
        '<td class="actions">' +
          '<button class="btn btn-sm" data-action="view-detail" data-id="' + e.jobId + '">Details</button>' +
          (canRetry ? ' <button class="btn btn-sm btn-primary" data-action="retry" data-id="' + e.jobId + '">Retry</button>' : '') +
          (canStop ? ' <button class="btn btn-sm btn-danger" data-action="stop" data-id="' + e.jobId + '">Stop</button>' : '') +
        '</td>' +
      '</tr>';
    }).join('');
  }

  function renderDetail(execution) {
    const container = document.getElementById('detail-body');
    if (!container) return;
    let paramsDisplay = '—';
    if (execution.parameters) {
      try {
        const parsed = typeof execution.parameters === 'string' ? JSON.parse(execution.parameters) : execution.parameters;
        paramsDisplay = '<pre>' + escapeHtml(JSON.stringify(parsed, null, 2)) + '</pre>';
      } catch (_) {
        paramsDisplay = '<pre>' + escapeHtml(execution.parameters) + '</pre>';
      }
    }
    container.innerHTML =
      '<div class="detail-grid">' +
        '<div class="detail-label">Execution ID</div><div class="detail-value mono">' + escapeHtml(String(execution.jobId)) + '</div>' +
        '<div class="detail-label">Job Name</div><div class="detail-value">' + escapeHtml(execution.jobName) + '</div>' +
        '<div class="detail-label">Status</div><div class="detail-value">' + badgeHtml(execution.status) + '</div>' +
        '<div class="detail-label">Requested By</div><div class="detail-value">' + escapeHtml(execution.requestedBy || '—') + '</div>' +
        '<div class="detail-label">Start Time</div><div class="detail-value">' + formatTime(execution.startTime) + '</div>' +
        '<div class="detail-label">End Time</div><div class="detail-value">' + formatTime(execution.endTime) + '</div>' +
        '<div class="detail-label">Queue Position</div><div class="detail-value">' + (execution.queuePosition != null ? execution.queuePosition : '—') + '</div>' +
        '<div class="detail-label">Parameters</div><div class="detail-value">' + paramsDisplay + '</div>' +
        '<div class="detail-label">Error Message</div><div class="detail-value">' + escapeHtml(execution.errorMessage || '—') + '</div>' +
      '</div>';
  }

  function renderStartJobForm(jobName, requiredParams) {
    const title = document.getElementById('start-job-title');
    const form = document.getElementById('start-job-form');
    if (title) title.textContent = 'Start Job: ' + jobName;
    if (!form) return;

    let html = '<input type="hidden" id="start-job-name" value="' + escapeHtml(jobName) + '">';
    if (requiredParams && requiredParams.length > 0) {
      requiredParams.forEach(p => {
        html += '<div class="form-group">' +
          '<label class="form-label" for="param-' + escapeHtml(p) + '">' + escapeHtml(p) + ' <span class="required-marker">*</span></label>' +
          '<input class="form-input" id="param-' + escapeHtml(p) + '" name="' + escapeHtml(p) + '" data-param="' + escapeHtml(p) + '" required>' +
        '</div>';
      });
    }
    html += '<div class="form-group">' +
      '<label class="form-label" for="start-job-args">Arguments (optional)</label>' +
      '<input class="form-input" id="start-job-args">' +
    '</div>';
    html += '<div class="form-group">' +
      '<label class="form-label" for="start-job-user">Requested By (optional)</label>' +
      '<input class="form-input" id="start-job-user" placeholder="anonymous">' +
    '</div>';
    html += '<button type="submit" class="btn btn-primary">Submit</button>';
    form.innerHTML = html;
  }

  function showModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('hidden');
  }

  function hideModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('hidden');
  }

  function renderAll() {
    renderTitle();
    renderAuthSection();
    renderSummary();
    renderJobs();
    renderFilters();
    renderExecutions();
  }

  return { renderAll, renderDetail, renderStartJobForm, showModal, hideModal, escapeHtml, formatTime };
})();
