/* Fluffy Scheduler — Outlook-Style Schedule View (React + Material-UI).
 * Uses React.createElement (no JSX) so no build step is required.
 * Dependencies loaded via CDN: React 18, ReactDOM 18, Material-UI 5.
 */
(function () {
  'use strict';

  var e = React.createElement;
  var MUI = MaterialUI;

  /* ── Palette ──────────────────────────────────────────────────── */
  var JOB_COLORS = [
    '#5c6bc0', '#26a69a', '#ef5350', '#ffa726', '#66bb6a',
    '#ab47bc', '#42a5f5', '#ec407a', '#78909c', '#8d6e63'
  ];

  var theme = MUI.createTheme({
    palette: {
      primary:   { main: '#5c6bc0' },
      secondary: { main: '#26a69a' },
      success:   { main: '#66bb6a' },
      error:     { main: '#ef5350' },
      warning:   { main: '#ffa726' },
      info:      { main: '#42a5f5' }
    },
    typography: { fontFamily: '"Roboto","Helvetica","Arial",sans-serif' }
  });

  /* ── Helpers ──────────────────────────────────────────────────── */
  function fetchJSON(url) {
    return fetch(url).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }

  function postJSON(url, body) {
    return fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    }).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }

  function deleteJSON(url) {
    return fetch(url, { method: 'DELETE' }).then(function (r) {
      if (!r.ok) throw new Error('HTTP ' + r.status);
      return r.json();
    });
  }

  function getJobColor(jobName, jobNames) {
    var idx = jobNames.indexOf(jobName);
    if (idx < 0) idx = 0;
    return JOB_COLORS[idx % JOB_COLORS.length];
  }

  function formatHour(hour) {
    if (hour === 0) return '12 AM';
    if (hour < 12) return hour + ' AM';
    if (hour === 12) return '12 PM';
    return (hour - 12) + ' PM';
  }

  function isSameDay(d1, d2) {
    return d1.getFullYear() === d2.getFullYear() &&
           d1.getMonth() === d2.getMonth() &&
           d1.getDate() === d2.getDate();
  }

  function addDays(date, days) {
    var d = new Date(date);
    d.setDate(d.getDate() + days);
    return d;
  }

  function startOfDay(date) {
    var d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  function formatDate(date) {
    var opts = { weekday: 'short', month: 'short', day: 'numeric' };
    return date.toLocaleDateString(undefined, opts);
  }

  /* ── Parse upcoming triggers into time-slot entries ──────────── */
  function parseTriggers(upcomingData, viewStart, viewDays) {
    var viewEnd = addDays(viewStart, viewDays);
    var entries = [];
    (upcomingData || []).forEach(function (sched) {
      var jobName = sched.jobName || 'unknown';
      var nodeId = sched.nodeId || sched.nodeUrl || 'unknown';
      (sched.upcoming || []).forEach(function (ts) {
        var d = new Date(ts);
        if (d >= viewStart && d < viewEnd) {
          entries.push({
            jobName: jobName,
            nodeId: nodeId,
            time: d,
            hour: d.getHours(),
            cronExpression: sched.cronExpression || ''
          });
        }
      });
    });
    return entries;
  }

  /* ── Schedule Table (Outlook-style) ─────────────────────────── */
  function ScheduleGrid(props) {
    var entries = props.entries;
    var viewStart = props.viewStart;
    var viewDays = props.viewDays;
    var nodes = props.nodes;
    var jobFilter = props.jobFilter;
    var jobNames = props.jobNames;

    var filtered = entries;
    if (jobFilter && jobFilter !== 'ALL') {
      filtered = filtered.filter(function (en) { return en.jobName === jobFilter; });
    }

    // Build day columns
    var days = [];
    for (var i = 0; i < viewDays; i++) {
      days.push(addDays(viewStart, i));
    }

    // Hours 0..23
    var hours = [];
    for (var h = 0; h < 24; h++) hours.push(h);

    // Build grid: for each day, for each hour, collect entries grouped by node
    function getEntriesForSlot(day, hour) {
      return filtered.filter(function (en) {
        return isSameDay(en.time, day) && en.hour === hour;
      });
    }

    return e(MUI.Box, { sx: { overflowX: 'auto' } },
      e(MUI.Table, { size: 'small', sx: { minWidth: Math.max(600, days.length * 200 + 80) } },
        // Header
        e(MUI.TableHead, null,
          e(MUI.TableRow, { sx: { '& th': { fontWeight: 700, position: 'sticky', top: 0, background: '#fff', zIndex: 2 } } },
            e(MUI.TableCell, { sx: { width: 80 } }, 'Time'),
            days.map(function (day, di) {
              return e(MUI.TableCell, { key: di, align: 'center', sx: { minWidth: 160 } },
                formatDate(day)
              );
            })
          )
        ),
        // Body — one row per hour
        e(MUI.TableBody, null,
          hours.map(function (hour) {
            return e(MUI.TableRow, { key: hour, sx: { '&:hover': { background: '#f5f5f5' } } },
              e(MUI.TableCell, {
                sx: { fontWeight: 500, fontSize: '0.75rem', color: 'text.secondary', verticalAlign: 'top', borderRight: '1px solid #e0e0e0' }
              }, formatHour(hour)),
              days.map(function (day, di) {
                var slotEntries = getEntriesForSlot(day, hour);
                return e(MUI.TableCell, {
                  key: di,
                  sx: { verticalAlign: 'top', borderRight: '1px solid #f0f0f0', p: 0.5, minHeight: 32 }
                },
                  slotEntries.length > 0
                    ? slotEntries.map(function (en, ei) {
                        var color = getJobColor(en.jobName, jobNames);
                        return e(MUI.Chip, {
                          key: ei,
                          label: en.jobName + ' @ ' + en.nodeId,
                          size: 'small',
                          title: en.jobName + ' (' + en.cronExpression + ') on ' + en.nodeId + ' at ' + en.time.toLocaleTimeString(),
                          sx: {
                            m: 0.25,
                            backgroundColor: color,
                            color: '#fff',
                            fontSize: '0.7rem',
                            height: 22,
                            maxWidth: '100%'
                          }
                        });
                      })
                    : null
                );
              })
            );
          })
        )
      )
    );
  }

  /* ── Schedule List Table ─────────────────────────────────────── */
  function ScheduleTable(props) {
    var schedules = props.schedules || [];
    var onDelete = props.onDelete;

    if (schedules.length === 0) {
      return e(MUI.Typography, { sx: { mt: 2 }, color: 'text.secondary' },
        'No schedules configured. Use the form below to add one.');
    }

    return e(MUI.TableContainer, { component: MUI.Paper, sx: { mt: 2 } },
      e(MUI.Table, { size: 'small' },
        e(MUI.TableHead, null,
          e(MUI.TableRow, { sx: { '& th': { fontWeight: 700 } } },
            e(MUI.TableCell, null, 'Job Name'),
            e(MUI.TableCell, null, 'Cron Expression'),
            e(MUI.TableCell, null, 'Target Node'),
            e(MUI.TableCell, null, 'Node'),
            e(MUI.TableCell, null, 'Enabled'),
            e(MUI.TableCell, null, 'Last Triggered'),
            e(MUI.TableCell, null, 'Actions')
          )
        ),
        e(MUI.TableBody, null,
          schedules.map(function (s, i) {
            return e(MUI.TableRow, { key: i },
              e(MUI.TableCell, null, s.jobName),
              e(MUI.TableCell, { sx: { fontFamily: 'monospace' } }, s.cronExpression),
              e(MUI.TableCell, null, s.targetNode || '—'),
              e(MUI.TableCell, null, s.nodeId || s.nodeUrl || '—'),
              e(MUI.TableCell, null,
                e(MUI.Chip, {
                  label: s.enabled ? 'Yes' : 'No',
                  color: s.enabled ? 'success' : 'default',
                  size: 'small'
                })
              ),
              e(MUI.TableCell, null, s.lastTriggeredAt ? new Date(s.lastTriggeredAt).toLocaleString() : '—'),
              e(MUI.TableCell, null,
                e(MUI.Button, {
                  size: 'small',
                  color: 'error',
                  onClick: function () { onDelete(s.jobName, s.nodeUrl); }
                }, 'Delete')
              )
            );
          })
        )
      )
    );
  }

  /* ── Create Schedule Form ────────────────────────────────────── */
  function CreateScheduleForm(props) {
    var nodes = props.nodes || [];
    var registeredJobs = props.registeredJobs || [];
    var onCreated = props.onCreated;

    var formState = React.useState({
      jobName: '', cronExpression: '', targetNode: '', nodeUrl: nodes[0] || '', enabled: true
    });
    var form = formState[0];
    var setForm = formState[1];
    var errorState = React.useState(null);
    var error = errorState[0];
    var setError = errorState[1];

    // Get unique job names
    var jobOptions = [];
    var seen = {};
    registeredJobs.forEach(function (j) {
      if (!seen[j.name]) {
        seen[j.name] = true;
        jobOptions.push(j.name);
      }
    });

    function handleSubmit(ev) {
      ev.preventDefault();
      setError(null);
      if (!form.jobName || !form.cronExpression || !form.nodeUrl) {
        setError('Job name, cron expression, and node are required.');
        return;
      }
      postJSON('/api/scheduler/schedules', {
        jobName: form.jobName,
        cronExpression: form.cronExpression,
        targetNode: form.targetNode || null,
        nodeUrl: form.nodeUrl,
        enabled: form.enabled
      })
      .then(function () {
        setForm({ jobName: '', cronExpression: '', targetNode: '', nodeUrl: nodes[0] || '', enabled: true });
        if (onCreated) onCreated();
      })
      .catch(function (err) { setError(err.message); });
    }

    function setField(field, value) {
      setForm(function (prev) {
        var next = Object.assign({}, prev);
        next[field] = value;
        return next;
      });
    }

    return e(MUI.Paper, { sx: { p: 3, mt: 3 } },
      e(MUI.Typography, { variant: 'h6', gutterBottom: true }, 'Create / Update Schedule'),
      error && e(MUI.Alert, { severity: 'error', sx: { mb: 2 } }, error),
      e('form', { onSubmit: handleSubmit },
        e(MUI.Box, { sx: { display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' } },
          e(MUI.FormControl, { size: 'small', sx: { minWidth: 180 } },
            e(MUI.InputLabel, null, 'Job Name'),
            e(MUI.Select, {
              value: form.jobName,
              label: 'Job Name',
              onChange: function (ev) { setField('jobName', ev.target.value); }
            },
              jobOptions.map(function (name) {
                return e(MUI.MenuItem, { key: name, value: name }, name);
              })
            )
          ),
          e(MUI.TextField, {
            size: 'small',
            label: 'Cron Expression',
            placeholder: '0 0 * * * *',
            value: form.cronExpression,
            onChange: function (ev) { setField('cronExpression', ev.target.value); },
            sx: { minWidth: 200 }
          }),
          e(MUI.FormControl, { size: 'small', sx: { minWidth: 180 } },
            e(MUI.InputLabel, null, 'Target Node'),
            e(MUI.Select, {
              value: form.nodeUrl,
              label: 'Target Node',
              onChange: function (ev) { setField('nodeUrl', ev.target.value); }
            },
              nodes.map(function (n) {
                return e(MUI.MenuItem, { key: n, value: n }, n);
              })
            )
          ),
          e(MUI.TextField, {
            size: 'small',
            label: 'Pin to Node ID (optional)',
            placeholder: 'Leave blank for any',
            value: form.targetNode,
            onChange: function (ev) { setField('targetNode', ev.target.value); },
            sx: { minWidth: 180 }
          }),
          e(MUI.Button, { type: 'submit', variant: 'contained', size: 'medium' }, 'Save Schedule')
        )
      )
    );
  }

  /* ── Main App ────────────────────────────────────────────────── */
  function App() {
    var schedulesState = React.useState([]);
    var upcomingState = React.useState([]);
    var registeredJobsState = React.useState([]);
    var loadingState = React.useState(true);
    var errorState = React.useState(null);
    var viewModeState = React.useState('day'); // 'day' or 'week'
    var viewStartState = React.useState(startOfDay(new Date()));
    var jobFilterState = React.useState('ALL');
    var configState = React.useState(null);
    var tabState = React.useState(0);

    var schedules = schedulesState[0], setSchedules = schedulesState[1];
    var upcoming = upcomingState[0], setUpcoming = upcomingState[1];
    var registeredJobs = registeredJobsState[0], setRegisteredJobs = registeredJobsState[1];
    var loading = loadingState[0], setLoading = loadingState[1];
    var error = errorState[0], setError = errorState[1];
    var viewMode = viewModeState[0], setViewMode = viewModeState[1];
    var viewStart = viewStartState[0], setViewStart = viewStartState[1];
    var jobFilter = jobFilterState[0], setJobFilter = jobFilterState[1];
    var config = configState[0], setConfig = configState[1];
    var tabValue = tabState[0], setTabValue = tabState[1];

    var viewDays = viewMode === 'week' ? 7 : 1;

    function loadAll() {
      setLoading(true);
      return Promise.all([
        fetchJSON('/api/scheduler/schedules').catch(function () { return []; }),
        fetchJSON('/api/scheduler/upcoming?count=48').catch(function () { return []; }),
        fetchJSON('/api/scheduler/registered-jobs').catch(function () { return []; }),
        fetchJSON('/api/aggregator/config').catch(function () { return null; })
      ]).then(function (results) {
        setSchedules(results[0]);
        setUpcoming(results[1]);
        setRegisteredJobs(results[2]);
        if (results[3]) setConfig(results[3]);
        setError(null);
      }).catch(function (err) {
        setError(err.message);
      }).finally(function () {
        setLoading(false);
      });
    }

    React.useEffect(function () { loadAll(); }, []);

    React.useEffect(function () {
      if (config && config.title) {
        document.title = config.title + ' — Scheduler';
      }
    }, [config]);

    // Collect unique node URLs and job names
    var nodes = [];
    var nodesSeen = {};
    schedules.forEach(function (s) {
      var n = s.nodeUrl;
      if (n && !nodesSeen[n]) { nodesSeen[n] = true; nodes.push(n); }
    });
    upcoming.forEach(function (s) {
      var n = s.nodeUrl;
      if (n && !nodesSeen[n]) { nodesSeen[n] = true; nodes.push(n); }
    });
    registeredJobs.forEach(function (j) {
      var n = j.nodeUrl;
      if (n && !nodesSeen[n]) { nodesSeen[n] = true; nodes.push(n); }
    });

    var jobNames = [];
    var jobNamesSeen = {};
    upcoming.forEach(function (s) {
      if (s.jobName && !jobNamesSeen[s.jobName]) {
        jobNamesSeen[s.jobName] = true;
        jobNames.push(s.jobName);
      }
    });
    schedules.forEach(function (s) {
      if (s.jobName && !jobNamesSeen[s.jobName]) {
        jobNamesSeen[s.jobName] = true;
        jobNames.push(s.jobName);
      }
    });

    var entries = parseTriggers(upcoming, viewStart, viewDays);

    function handleDelete(jobName, nodeUrl) {
      if (!nodeUrl) return;
      deleteJSON('/api/scheduler/schedules/' + encodeURIComponent(jobName) + '?nodeUrl=' + encodeURIComponent(nodeUrl))
        .then(function () { loadAll(); })
        .catch(function (err) { setError('Delete failed: ' + err.message); });
    }

    function navigate(delta) {
      setViewStart(function (prev) { return startOfDay(addDays(prev, delta)); });
    }

    function goToday() {
      setViewStart(startOfDay(new Date()));
    }

    return e(MUI.ThemeProvider, { theme: theme },
      e(MUI.CssBaseline),
      // AppBar
      e(MUI.AppBar, { position: 'static', sx: { mb: 3 } },
        e(MUI.Toolbar, null,
          e('img', { src: '/fluffy.svg', alt: 'Fluffy', style: { width: 48, height: 48, marginRight: 12 } }),
          e(MUI.Typography, { variant: 'h6', sx: { flexGrow: 1 } }, 'Fluffy Scheduler'),
          e(MUI.Button, { color: 'inherit', href: '/fluffy-aggregator/index.html' }, 'Aggregator'),
          e(MUI.Button, { color: 'inherit', onClick: function () { loadAll(); }, disabled: loading }, 'Refresh')
        )
      ),

      e(MUI.Container, { maxWidth: 'xl' },
        loading && !schedules.length && e(MUI.Box, { sx: { display: 'flex', justifyContent: 'center', mt: 6 } },
          e(MUI.CircularProgress)
        ),
        error && e(MUI.Alert, { severity: 'error', sx: { mb: 2 } }, 'Error: ' + error),

        // Tabs: Calendar View / Schedules / Create
        e(MUI.Tabs, { value: tabValue, onChange: function (ev, v) { setTabValue(v); }, sx: { mb: 2 } },
          e(MUI.Tab, { label: 'Calendar View' }),
          e(MUI.Tab, { label: 'Schedules (' + schedules.length + ')' }),
          e(MUI.Tab, { label: 'Create Schedule' })
        ),

        // Tab 0: Calendar
        tabValue === 0 && e(React.Fragment, null,
          // Controls row
          e(MUI.Box, { sx: { display: 'flex', gap: 2, mb: 2, alignItems: 'center', flexWrap: 'wrap' } },
            e(MUI.ButtonGroup, { size: 'small' },
              e(MUI.Button, {
                variant: viewMode === 'day' ? 'contained' : 'outlined',
                onClick: function () { setViewMode('day'); }
              }, 'Day'),
              e(MUI.Button, {
                variant: viewMode === 'week' ? 'contained' : 'outlined',
                onClick: function () { setViewMode('week'); }
              }, 'Week')
            ),
            e(MUI.ButtonGroup, { size: 'small' },
              e(MUI.Button, { onClick: function () { navigate(-viewDays); } }, '◀ Prev'),
              e(MUI.Button, { onClick: goToday }, 'Today'),
              e(MUI.Button, { onClick: function () { navigate(viewDays); } }, 'Next ▶')
            ),
            e(MUI.Typography, { variant: 'subtitle1', sx: { fontWeight: 500 } },
              formatDate(viewStart) + (viewDays > 1 ? ' — ' + formatDate(addDays(viewStart, viewDays - 1)) : '')
            ),
            e(MUI.FormControl, { size: 'small', sx: { minWidth: 160 } },
              e(MUI.InputLabel, null, 'Filter Job'),
              e(MUI.Select, {
                value: jobFilter,
                label: 'Filter Job',
                onChange: function (ev) { setJobFilter(ev.target.value); }
              },
                e(MUI.MenuItem, { value: 'ALL' }, 'All Jobs'),
                jobNames.map(function (name) {
                  return e(MUI.MenuItem, { key: name, value: name }, name);
                })
              )
            ),
            // Legend
            jobNames.length > 0 && e(MUI.Box, { sx: { display: 'flex', gap: 1, flexWrap: 'wrap' } },
              jobNames.map(function (name) {
                var color = getJobColor(name, jobNames);
                return e(MUI.Chip, {
                  key: name,
                  label: name,
                  size: 'small',
                  sx: { backgroundColor: color, color: '#fff', fontSize: '0.7rem' }
                });
              })
            )
          ),
          // Calendar grid
          e(MUI.Paper, { sx: { maxHeight: '70vh', overflow: 'auto' } },
            e(ScheduleGrid, {
              entries: entries,
              viewStart: viewStart,
              viewDays: viewDays,
              nodes: nodes,
              jobFilter: jobFilter,
              jobNames: jobNames
            })
          ),
          entries.length === 0 && e(MUI.Typography, {
            sx: { mt: 2, textAlign: 'center' },
            color: 'text.secondary'
          }, 'No scheduled triggers in this time range. Try navigating or adding schedules.')
        ),

        // Tab 1: Schedule List
        tabValue === 1 && e(React.Fragment, null,
          e(MUI.Typography, { variant: 'h5', gutterBottom: true }, 'All Cron Schedules'),
          e(ScheduleTable, { schedules: schedules, onDelete: handleDelete })
        ),

        // Tab 2: Create
        tabValue === 2 && e(CreateScheduleForm, {
          nodes: nodes,
          registeredJobs: registeredJobs,
          onCreated: function () { loadAll(); setTabValue(1); }
        })
      )
    );
  }

  /* ── Mount ────────────────────────────────────────────────────── */
  var root = ReactDOM.createRoot(document.getElementById('root'));
  root.render(e(App));
})();
