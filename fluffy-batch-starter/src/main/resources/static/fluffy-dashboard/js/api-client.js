/**
 * Fluffy Batch Dashboard — API Client
 * Thin wrapper around fetch for all starter REST endpoints.
 */
const FluffyApi = (() => {
  'use strict';

  const BASE = '/api/jobs';
  let _token = null;
  let _onUnauthorized = null;

  function setToken(token) { _token = token || null; }
  function getToken() { return _token; }
  function onUnauthorized(cb) { _onUnauthorized = cb; }

  function headers(extra) {
    const h = { 'Content-Type': 'application/json' };
    if (_token) h['Authorization'] = 'Bearer ' + _token;
    return Object.assign(h, extra || {});
  }

  async function request(method, path, body) {
    const opts = { method, headers: headers() };
    if (body !== undefined) opts.body = JSON.stringify(body);
    const res = await fetch(BASE + path, opts);
    if (res.status === 401) {
      if (_onUnauthorized) _onUnauthorized();
      throw new Error('Unauthorized – check your token');
    }
    if (!res.ok) {
      let msg = res.statusText;
      try { const j = await res.json(); msg = j.message || msg; } catch (_) {}
      throw new Error(msg);
    }
    return res.json();
  }

  return {
    setToken,
    getToken,
    onUnauthorized,
    listRegisteredJobs()       { return request('GET', '/registered'); },
    listExecutions()           { return request('GET', '/executions'); },
    getStatus(executionId)     { return request('GET', '/' + executionId + '/status'); },
    startJob(jobName, payload) { return request('POST', '/' + encodeURIComponent(jobName) + '/start', payload || {}); },
    stopJob(executionId)       { return request('POST', '/' + executionId + '/stop'); },
    retryJob(executionId)      { return request('POST', '/' + executionId + '/retry'); },
    getDashboardConfig()       { return request('GET', '/dashboard/config'); },
  };
})();
