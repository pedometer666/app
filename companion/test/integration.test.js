const fs = require('fs');
const path = require('path');
const { JSDOM } = require('jsdom');

const COMPANION = path.join(__dirname, '..');
let html = fs.readFileSync(path.join(COMPANION, 'index.html'), 'utf8');
html = html
  .replace('<script src="sample-data.js"></script>', '')
  .replace('<script src="app.js"></script>', '')
  .replace(/<script>\s*if \('serviceWorker'[\s\S]*?<\/script>/, '');

const sampleJs = fs.readFileSync(path.join(COMPANION, 'sample-data.js'), 'utf8');
const appJs = fs.readFileSync(path.join(COMPANION, 'app.js'), 'utf8');

const dom = new JSDOM(html, { url: 'http://localhost/companion/index.html', runScripts: 'outside-only', pretendToBeVisual: true });
const { window } = dom;
window.URL.createObjectURL = () => 'blob:mock';
window.URL.revokeObjectURL = () => {};
window.HTMLAnchorElement.prototype.click = function () {};

const click = (sel) => $(sel).dispatchEvent(new window.MouseEvent('click', { bubbles: true }));
const change = (sel, value) => { const node = $(sel); node.value = value; node.dispatchEvent(new window.Event('change', { bubbles: true })); };
const $ = (s) => window.document.querySelector(s);
const $$ = (s) => [...window.document.querySelectorAll(s)];
const fmt = (n) => Math.round(n).toLocaleString('en-US');

let failures = 0;
const ok = (cond, msg) => { console.log((cond ? '  ok  ' : 'FAIL  ') + msg); if (!cond) failures += 1; };

window.eval(sampleJs);
window.eval(appJs);

console.log('— initial state —');
ok(!$('#empty-state').hidden, 'empty state visible before any import');
ok($('#dashboard').hidden, 'dashboard hidden before any import');
ok($('#data-status').hidden, 'status bar hidden before any import');

console.log('— load sample dataset —');
click('#btn-sample-empty');
ok($('#empty-state').hidden, 'empty state hidden after import');
ok(!$('#dashboard').hidden, 'dashboard shown after import');
const totalSteps = window.SAMPLE_PEDOMETER_EXPORT.days.reduce((s, d) => s + d.steps, 0);
ok($('#kpi-steps').textContent === fmt(totalSteps), `KPI steps = ${fmt(totalSteps)} (got ${$('#kpi-steps').textContent})`);
ok($('#kpi-distance').textContent.endsWith('km'), 'distance shown in metric by default');
ok(!$('#data-status').hidden && $('#data-status-text').textContent.includes('499'), 'status bar reports 499 recorded days');
ok($('#daily-table tbody').children.length > 0, 'daily table populated');
ok($('#daily-chart svg') !== null, 'daily chart SVG rendered');
ok($('#goal-note-check') === null || true, 'noop');

console.log('— daily tab month navigation —');
change('#daily-month', '2026-10');
const oct = window.SAMPLE_PEDOMETER_EXPORT.days.filter((d) => d.date.startsWith('2026-10'));
const octSum = oct.reduce((s, d) => s + d.steps, 0);
ok($('#daily-strip').textContent.includes(fmt(octSum)), `October strip shows ${fmt(octSum)} steps`);
ok($('#daily-table tbody').children.length === oct.length, `October table rows = ${oct.length}`);
const bestOct = oct.reduce((m, d) => (d.steps > m.steps ? d : m), oct[0]);
ok(bestOct.steps === 21340, 'October contains the 21,340-step personal best');

console.log('— monthly tab —');
click('.tab[data-tab="monthly"]');
ok($('#monthly-table tbody').children.length === 13, 'monthly table has 12 months + total row');
ok($('#monthly-chart svg') !== null, 'monthly chart SVG rendered');
ok($('#monthly-year').value === '2026', 'monthly year defaults to 2026');
ok($('#monthly-strip').textContent.length > 0, 'monthly stat strip populated');
change('#monthly-year', '2025');
ok($('#monthly-year').value === '2025', 'year selector switches to 2025');
ok($('#monthly-table tbody').children.length === 13, '2025 table also has 12 + total');

console.log('— yearly tab —');
click('.tab[data-tab="yearly"]');
ok($('#yearly-table tbody').children.length === 2, 'yearly table has 2025 + 2026');
ok($('#yearly-chart svg') !== null, 'yearly chart SVG rendered');

console.log('— compare tab —');
click('.tab[data-tab="compare"]');
ok($('#compare-grid').children.length === 5, 'compare renders 5 metric cards');
ok(/out-walks|dead even/.test($('#compare-verdict').textContent), 'compare verdict generated: ' + $('#compare-verdict').textContent.slice(0, 60));
const aBefore = $('#compare-a').value, bBefore = $('#compare-b').value;
click('#compare-swap');
ok($('#compare-a').value === bBefore && $('#compare-b').value === aBefore, 'swap exchanges A and B');
change('#compare-granularity', 'year');
ok($('#compare-a').options.length === 2, 'year granularity lists 2 periods');
change('#compare-granularity', 'day');
ok($('#compare-a').options.length === 499, 'day granularity lists 499 periods');

console.log('— unit toggle & CSV —');
click('#unit-imperial');
ok($('#kpi-distance').textContent.endsWith('mi'), 'imperial units applied to KPIs');
click('#unit-metric');
ok($('#kpi-distance').textContent.endsWith('km'), 'metric units restored');
click('.tab[data-tab="daily"]');
change('#daily-month', '2026-11');
click('#btn-daily-csv'); // must not throw with stubbed anchor/URL
click('#btn-monthly-csv');
ok(true, 'CSV export buttons did not throw');

console.log('— persistence —');
const stored = window.localStorage.getItem('pedometer.companion.export');
ok(stored && JSON.parse(stored).days.length === 499, 'export persisted to localStorage');
click('#btn-clear');
ok(!$('#empty-state').hidden, 'clear returns to empty state');
ok(window.localStorage.getItem('pedometer.companion.export') === null, 'localStorage cleared');

console.log('— invalid import (through the real file-input path) —');
(async () => {
  const badFile = new window.File(['{"app":"not-pedometer","schemaVersion":1,"days":[]}'], 'bad.json', { type: 'application/json' });
  const input = $('#file-input');
  Object.defineProperty(input, 'files', { value: [badFile], configurable: true });
  input.dispatchEvent(new window.Event('change', { bubbles: true }));
  await new Promise((resolve) => setTimeout(resolve, 100));
  ok(/pedometer/i.test($('#toast').textContent), 'invalid export rejected with toast: ' + $('#toast').textContent);
  ok(!$('#empty-state').hidden, 'stays on empty state after rejected import');
  console.log(failures === 0 ? '\nALL COMPANION TESTS PASSED' : `\n${failures} FAILURES`);
  process.exit(failures === 0 ? 0 : 1);
})();

