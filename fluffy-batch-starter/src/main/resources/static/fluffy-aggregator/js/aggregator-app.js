/* Fluffy Aggregator Dashboard – React + Material-UI application.
 * Uses React.createElement (no JSX) so that no build step is required.
 * Dependencies loaded via CDN: React 18, ReactDOM 18, Material-UI 5.
 */
(function () {
  'use strict';

  var e = React.createElement;
  var MUI = MaterialUI;

  /* ── Palette ──────────────────────────────────────────────────── */
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

  /* ── Summary Card ─────────────────────────────────────────────── */
  function SummaryCard(props) {
    return e(MUI.Card, { sx: { minWidth: 160, textAlign: 'center' } },
      e(MUI.CardContent, null,
        e(MUI.Typography, { variant: 'h3', color: props.color || 'text.primary' }, props.value),
        e(MUI.Typography, { variant: 'body2', color: 'text.secondary' }, props.label)
      )
    );
  }

  /* ── Aggregate Metric Cards ───────────────────────────────────── */
  function AggregateCards(props) {
    var d = props.data;
    if (!d) return null;
    var cards = [
      { label: 'Total Queued',    value: d.totalQueued,    color: 'warning.main' },
      { label: 'Total Running',   value: d.totalRunning,   color: 'info.main' },
      { label: 'Total Succeeded', value: d.totalSucceeded, color: 'success.main' },
      { label: 'Total Failed',    value: d.totalFailed,    color: 'error.main' }
    ];
    return e(MUI.Box, { sx: { display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 } },
      cards.map(function (c, i) {
        return e(SummaryCard, { key: i, label: c.label, value: c.value, color: c.color });
      })
    );
  }

  /* ── Per-Node Table ───────────────────────────────────────────── */
  function NodeTable(props) {
    var nodes = (props.data && props.data.nodeSummaries) || [];
    if (nodes.length === 0) {
      return e(MUI.Typography, { sx: { mt: 2 }, color: 'text.secondary' },
        'No node data available. Ensure aggregator nodes are configured.');
    }
    return e(MUI.TableContainer, { component: MUI.Paper, sx: { mt: 2 } },
      e(MUI.Table, { size: 'small' },
        e(MUI.TableHead, null,
          e(MUI.TableRow, { sx: { '& th': { fontWeight: 700 } } },
            e(MUI.TableCell, null, 'Node'),
            e(MUI.TableCell, { align: 'right' }, 'Queued'),
            e(MUI.TableCell, { align: 'right' }, 'Running'),
            e(MUI.TableCell, { align: 'right' }, 'Succeeded'),
            e(MUI.TableCell, { align: 'right' }, 'Failed'),
            e(MUI.TableCell, null, 'Dashboard')
          )
        ),
        e(MUI.TableBody, null,
          nodes.map(function (n) {
            return e(MUI.TableRow, { key: n.nodeId },
              e(MUI.TableCell, null, n.nodeId),
              e(MUI.TableCell, { align: 'right' }, n.queued),
              e(MUI.TableCell, { align: 'right' }, n.running),
              e(MUI.TableCell, { align: 'right' }, n.succeeded),
              e(MUI.TableCell, { align: 'right' }, n.failed),
              e(MUI.TableCell, null,
                n.dashboardAvailable
                  ? e(MUI.Button, {
                      size: 'small',
                      variant: 'outlined',
                      href: n.dashboardUrl,
                      target: '_blank',
                      rel: 'noopener'
                    }, 'Open')
                  : e(MUI.Chip, { label: 'N/A', size: 'small', variant: 'outlined' })
              )
            );
          })
        )
      )
    );
  }

  /* ── Main App Component ───────────────────────────────────────── */
  function App() {
    var dataState    = React.useState(null);
    var errorState   = React.useState(null);
    var loadingState = React.useState(true);
    var configState  = React.useState(null);

    var data    = dataState[0],    setData    = dataState[1];
    var error   = errorState[0],   setError   = errorState[1];
    var loading = loadingState[0], setLoading = loadingState[1];
    var config  = configState[0],  setConfig  = configState[1];

    function loadData() {
      return fetchJSON('/api/aggregator/summary')
        .then(function (d) { setData(d); setError(null); })
        .catch(function (e) { setError(e.message); })
        .finally(function () { setLoading(false); });
    }

    function handleRefresh() {
      setLoading(true);
      fetchJSON('/api/aggregator/refresh')
        .then(function (d) { setData(d); setError(null); })
        .catch(function (e) { setError(e.message); })
        .finally(function () { setLoading(false); });
    }

    // Initial load + periodic polling
    React.useEffect(function () {
      fetchJSON('/api/aggregator/config')
        .then(function (c) { setConfig(c); })
        .catch(function () { /* ignore */ });
      loadData();
      var interval = setInterval(loadData, 10000);
      return function () { clearInterval(interval); };
    }, []);

    // Update interval when config is known
    React.useEffect(function () {
      if (!config) return;
      var ms = (config.pollIntervalSeconds || 10) * 1000;
      var interval = setInterval(loadData, ms);
      return function () { clearInterval(interval); };
    }, [config]);

    return e(MUI.ThemeProvider, { theme: theme },
      e(MUI.CssBaseline),
      e(MUI.AppBar, { position: 'static', sx: { mb: 3 } },
        e(MUI.Toolbar, null,
          e(MUI.Typography, { variant: 'h6', sx: { flexGrow: 1 } }, 'Fluffy Aggregator Dashboard'),
          config && e(MUI.Chip, {
            label: config.nodeCount + ' node(s)',
            color: 'secondary',
            size: 'small',
            sx: { mr: 2 }
          }),
          e(MUI.Button, { color: 'inherit', onClick: handleRefresh, disabled: loading }, 'Refresh')
        )
      ),
      e(MUI.Container, { maxWidth: 'lg' },
        loading && !data && e(MUI.Box, { sx: { display: 'flex', justifyContent: 'center', mt: 6 } },
          e(MUI.CircularProgress)
        ),
        error && e(MUI.Alert, { severity: 'error', sx: { mb: 2 } }, 'Error: ' + error),
        data && e(React.Fragment, null,
          e(MUI.Typography, { variant: 'h5', gutterBottom: true }, 'Aggregated Metrics'),
          e(AggregateCards, { data: data }),
          e(MUI.Divider, { sx: { my: 3 } }),
          e(MUI.Typography, { variant: 'h5', gutterBottom: true }, 'Per-Node Breakdown'),
          e(NodeTable, { data: data })
        )
      )
    );
  }

  /* ── Mount ────────────────────────────────────────────────────── */
  var root = ReactDOM.createRoot(document.getElementById('root'));
  root.render(e(App));
})();
