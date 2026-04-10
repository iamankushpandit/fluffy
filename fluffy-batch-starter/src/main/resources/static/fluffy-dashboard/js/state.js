/**
 * Fluffy Batch Dashboard — State Management
 */
const FluffyState = (() => {
  'use strict';

  let _jobs = [];
  let _executions = [];
  let _statusFilter = 'ALL';
  let _searchQuery = '';
  let _config = { title: 'Fluffy Batch Dashboard', refreshInterval: 5, authEnabled: false };
  const _listeners = [];

  function subscribe(fn) { _listeners.push(fn); }
  function _notify() { _listeners.forEach(fn => fn()); }

  return {
    subscribe,

    get jobs() { return _jobs; },
    set jobs(v) { _jobs = v; _notify(); },

    get executions() { return _executions; },
    set executions(v) { _executions = v; _notify(); },

    get statusFilter() { return _statusFilter; },
    set statusFilter(v) { _statusFilter = v; _notify(); },

    get searchQuery() { return _searchQuery; },
    set searchQuery(v) { _searchQuery = v; _notify(); },

    get config() { return _config; },
    set config(v) { _config = v; _notify(); },

    filteredExecutions() {
      let list = _executions;
      if (_statusFilter !== 'ALL') {
        list = list.filter(e => e.status === _statusFilter);
      }
      if (_searchQuery.trim()) {
        const q = _searchQuery.trim().toLowerCase();
        list = list.filter(e =>
          (e.jobName && e.jobName.toLowerCase().includes(q)) ||
          (String(e.jobId).includes(q))
        );
      }
      return list;
    },

    statusCounts() {
      const counts = { ALL: _executions.length, IN_QUEUE: 0, STARTED: 0, IN_PROGRESS: 0, SUCCESS: 0, FAILURE: 0, STOPPED: 0 };
      _executions.forEach(e => { if (counts[e.status] !== undefined) counts[e.status]++; });
      return counts;
    }
  };
})();
