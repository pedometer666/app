'use strict';
/**
 * Pedometer Companion — offline desktop stats explorer.
 * Consumes the JSON package exported by the Android app (schemaVersion 1).
 * No dependencies, no network: everything renders from localStorage.
 */

// ─────────────────────────────  DOM helpers  ─────────────────────────────
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

function toast(message) {
  const node = $('#toast');
  node.textContent = message;
  node.classList.add('show');
  setTimeout(() => node.classList.remove('show'), 2600);
}

// ─────────────────────────────  State & persistence  ─────────────────────────────
const LS_EXPORT = 'pedometer.companion.export';
const LS_UNIT = 'pedometer.companion.unit';

const state = {
  data: null,        // normalized export { profile, days, summary, exportedAt }
  unit: null,        // 'metric' | 'imperial' | null (null => follow export profile)
  tab: 'daily',
  dailyMonth: null,  // 'YYYY-MM'
  monthlyYear: null, // 'YYYY'
  compareGran: 'month',
  compareA: null,
  compareB: null,
};

// ─────────────────────────────  Formatting  ─────────────────────────────
const METERS_PER_MILE = 1609.344;
const fmtInt = (n) => Math.round(Number(n) || 0).toLocaleString('en-US');
const fmtCompact = (n) =>
  new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(Number(n) || 0);
const effectiveUnit = () => state.unit ?? (state.data?.profile?.useMetric === false ? 'imperial' : 'metric');
const distanceUnit = () => (effectiveUnit() === 'metric' ? 'km' : 'mi');
const distValue = (meters) => (effectiveUnit() === 'metric' ? meters / 1000 : meters / METERS_PER_MILE);
const fmtDistance = (meters, digits = 2) => `${distValue(meters).toFixed(digits)} ${distanceUnit()}`;
const fmtKcal = (kcal) => `${fmtInt(kcal)} kcal`;
const pad2 = (n) => String(n).padStart(2, '0');
const MONTH_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MONTH_FULL = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const parseDate = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, (m || 1) - 1, d || 1); };
const monthLabel = (key) => { const [y, m] = key.split('-').map(Number); return `${MONTH_FULL[(m || 1) - 1]} ${y}`; };
const dayLabel = (iso) => parseDate(iso).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' });
const daysInMonth = (key) => { const [y, m] = key.split('-').map(Number); return new Date(y, m, 0).getDate(); };
const percentDelta = (cur, prev) => (prev > 0 ? ((cur - prev) / prev) * 100 : null);
const deltaHtml = (pct, suffix = '') => {
  if (pct === null || !Number.isFinite(pct)) return '<span class="goal-miss">—</span>';
  const up = pct >= 0;
  return `<span class="${up ? 'delta-up' : 'delta-down'}">${up ? '▲' : '▼'} ${Math.abs(pct).toFixed(1)}%${suffix ? ' ' + suffix : ''}</span>`;
};

// ─────────────────────────────  Export parsing  ─────────────────────────────
function normalizeExport(raw) {
  if (!raw || typeof raw !== 'object') throw new Error('That file is not a JSON object.');
  if (raw.app !== 'pedometer') throw new Error('Not a Pedometer export — expected "app": "pedometer".');
  const version = Number(raw.schemaVersion);
  if (!Number.isFinite(version) || version < 1) throw new Error('Unsupported or missing schemaVersion.');
  if (!Array.isArray(raw.days)) throw new Error('The export is missing the "days" array.');

  const profile = Object.assign(
    { dailyGoalSteps: 10000, strideLengthCm: 75, bodyWeightKg: 70, useMetric: true, backgroundTrackingEnabled: true },
    raw.profile || {}
  );

  const days = [];
  for (const entry of raw.days) {
    if (!entry || typeof entry.date !== 'string') continue;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(entry.date)) continue;
    days.push({
      date: entry.date,
      steps: Number(entry.steps) || 0,
      calories: Number(entry.calories) || 0,
      distanceMeters: Number(entry.distanceMeters) || 0,
    });
  }
  days.sort((a, b) => a.date.localeCompare(b.date));
  if (days.length === 0) throw new Error('The export contains no usable day records.');

  return { profile, days, summary: raw.summary || null, exportedAt: raw.exportedAt || null };
}

// ─────────────────────────────  Aggregation  ─────────────────────────────
function summarizeDays(days) {
  let steps = 0, distanceMeters = 0, calories = 0, best = null, activeDays = 0;
  for (const d of days) {
    steps += d.steps; distanceMeters += d.distanceMeters; calories += d.calories;
    if (d.steps > 0) activeDays += 1;
    if (!best || d.steps > best.steps) best = d;
  }
  return { steps, distanceMeters, calories, best, activeDays, days: days.length };
}

function groupByKey(days, keyFn) {
  const map = new Map();
  for (const d of days) {
    const key = keyFn(d);
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(d);
  }
  return map;
}

const monthGroups = (days) => groupByKey(days, (d) => d.date.slice(0, 7));
const yearGroups = (days) => groupByKey(days, (d) => d.date.slice(0, 4));

// ─────────────────────────────  SVG bar chart  ─────────────────────────────
function renderBarChart(container, { labels, values, goal = null, highlightIndex = -1 }) {
  const width = 1000, height = 235, padT = 20, padB = 30, padL = 6, padR = 6;
  const innerH = height - padB - padT;
  const numericMax = Math.max(0, ...values, goal || 0);
  const max = numericMax > 0 ? numericMax * 1.15 : 1;
  const n = values.length;
  const slot = (width - padL - padR) / n;
  const barW = Math.min(slot * 0.66, 56);
  const showValueLabels = n <= 13;
  const parts = [];

  for (let i = 1; i <= 4; i += 1) {
    const y = padT + innerH - (innerH * i) / 4;
    parts.push(`<line class="grid-line" x1="${padL}" y1="${y.toFixed(1)}" x2="${width - padR}" y2="${y.toFixed(1)}"/>`);
  }

  values.forEach((value, i) => {
    const x = padL + slot * i + (slot - barW) / 2;
    const h = Math.max(innerH * (value / max), value > 0 ? 2.5 : 0);
    const y = padT + innerH - h;
    const fill = i === highlightIndex ? '#34D399' : 'rgba(52, 211, 153, 0.45)';
    parts.push(
      `<rect x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${barW.toFixed(1)}" height="${h.toFixed(1)}" rx="4" fill="${fill}">` +
      `<title>${escapeHtml(labels[i])}: ${fmtInt(value)} steps</title></rect>`
    );
    if (showValueLabels) {
      parts.push(`<text class="bar-value" x="${(x + barW / 2).toFixed(1)}" y="${(y - 5).toFixed(1)}" text-anchor="middle">${fmtCompact(value)}</text>`);
    }
    parts.push(`<text class="axis-label" x="${(x + barW / 2).toFixed(1)}" y="${height - 8}" text-anchor="middle">${escapeHtml(labels[i])}</text>`);
  });

  if (goal != null && goal > 0 && goal <= max) {
    const y = padT + innerH - innerH * (goal / max);
    parts.push(`<line class="goal-line" x1="${padL}" y1="${y.toFixed(1)}" x2="${width - padR}" y2="${y.toFixed(1)}"/>`);
    parts.push(`<text class="goal-label" x="${width - padR}" y="${(y - 4).toFixed(1)}" text-anchor="end">goal ${fmtInt(goal)}</text>`);
  }

  container.innerHTML = `<svg viewBox="0 0 ${width} ${height}" role="img" aria-label="Bar chart">${parts.join('')}</svg>`;
}

function statChip(label, valueHtml) {
  return `<div class="stat-chip"><span class="sc-label">${escapeHtml(label)}</span><span class="sc-value">${valueHtml}</span></div>`;
}

// ─────────────────────────────  Header KPIs & status  ─────────────────────────────
function renderKpis() {
  const sum = summarizeDays(state.data.days);
  $('#kpi-steps').textContent = fmtInt(sum.steps);
  $('#kpi-active-days').textContent = `${fmtInt(sum.activeDays)} active days · since ${state.data.days[0].date}`;
  $('#kpi-distance').textContent = fmtDistance(sum.distanceMeters, 1);
  $('#kpi-calories').textContent = fmtInt(sum.calories);
  $('#kpi-best').textContent = sum.best ? fmtInt(sum.best.steps) : '—';
  $('#kpi-best-date').textContent = sum.best ? `steps on ${dayLabel(sum.best.date)}` : '';
}

function renderStatusBar(fileName) {
  const bar = $('#data-status');
  bar.hidden = false;
  const exported = state.data.exportedAt ? new Date(state.data.exportedAt).toLocaleString('en-US') : 'unknown time';
  $('#data-status-text').textContent =
    `Loaded ${state.data.days.length} recorded days · exported ${exported}${fileName ? ` · ${fileName}` : ''}`;
}

// ─────────────────────────────  Daily tab  ─────────────────────────────
function renderDaily() {
  const days = state.data.days;
  const lastMonth = days[days.length - 1].date.slice(0, 7);
  if (!state.dailyMonth) state.dailyMonth = lastMonth;
  $('#daily-month').value = state.dailyMonth;

  const month = state.dailyMonth;
  const goal = state.data.profile.dailyGoalSteps;
  const totalDays = daysInMonth(month);
  const byDate = new Map(days.map((d) => [d.date, d]));

  const labels = [], values = [];
  let monthSteps = 0, monthDistance = 0, monthCalories = 0, bestDay = null, goalDays = 0, recordedDays = 0;
  for (let dayNum = 1; dayNum <= totalDays; dayNum += 1) {
    const iso = `${month}-${pad2(dayNum)}`;
    const record = byDate.get(iso);
    labels.push(String(dayNum));
    values.push(record ? record.steps : 0);
    if (record) {
      monthSteps += record.steps; monthDistance += record.distanceMeters; monthCalories += record.calories;
      recordedDays += 1;
      if (record.steps >= goal) goalDays += 1;
      if (!bestDay || record.steps > bestDay.steps) bestDay = record;
    }
  }

  const nowDate = new Date();
  const currentIso = `${nowDate.getFullYear()}-${pad2(nowDate.getMonth() + 1)}-${pad2(nowDate.getDate())}`;
  const highlightIndex = currentIso.startsWith(month) ? Number(currentIso.slice(8, 10)) - 1 : -1;

  $('#daily-strip').innerHTML = [
    statChip('Month steps', `${fmtInt(monthSteps)}`),
    statChip('Avg / active day', `${fmtInt(recordedDays ? monthSteps / recordedDays : 0)}`),
    statChip('Best day', bestDay ? `${fmtInt(bestDay.steps)} <small>· ${bestDay.date.slice(8, 10)} ${MONTH_SHORT[Number(month.slice(5, 7)) - 1]}</small>` : '—'),
    statChip('Distance', fmtDistance(monthDistance, 2)),
    statChip('Calories', fmtKcal(monthCalories)),
    statChip('Days at goal', `${goalDays}<small> / ${recordedDays} recorded</small>`),
  ].join('');

  renderBarChart($('#daily-chart'), { labels, values, goal, highlightIndex });
  $('#daily-goal-note').textContent = `Dashed amber line marks your daily goal of ${fmtInt(goal)} steps. Days without a record show as zero.`;

  const tbody = $('#daily-table tbody');
  const rows = [];
  for (let dayNum = totalDays; dayNum >= 1; dayNum -= 1) {
    const iso = `${month}-${pad2(dayNum)}`;
    const record = byDate.get(iso);
    if (!record) continue;
    const pct = goal > 0 ? (record.steps / goal) * 100 : 0;
    const hit = record.steps >= goal;
    rows.push(
      `<tr><td>${dayLabel(iso)}</td><td class="num">${fmtInt(record.steps)}</td>` +
      `<td class="num ${hit ? 'goal-hit' : 'goal-miss'}">${pct.toFixed(0)}%</td>` +
      `<td class="num">${fmtDistance(record.distanceMeters, 2)}</td><td class="num">${fmtInt(record.calories)}</td></tr>`
    );
  }
  tbody.innerHTML = rows.join('') || `<tr><td colspan="5" class="goal-miss">No recorded days in ${escapeHtml(monthLabel(month))}.</td></tr>`;
}

function dailyCsvRows() {
  const month = state.dailyMonth;
  const rows = [['date', 'steps', 'calories_kcal', 'distance', 'unit', 'percent_of_goal']];
  for (const d of state.data.days.filter((d) => d.date.startsWith(month))) {
    const pct = state.data.profile.dailyGoalSteps > 0
      ? ((d.steps / state.data.profile.dailyGoalSteps) * 100).toFixed(1) : '';
    rows.push([d.date, d.steps, d.calories.toFixed(1), distValue(d.distanceMeters).toFixed(3), distanceUnit(), pct]);
  }
  return rows;
}

// ─────────────────────────────  Monthly tab  ─────────────────────────────
function renderMonthly() {
  const days = state.data.days;
  const years = [...new Set(days.map((d) => d.date.slice(0, 4)))].sort();
  if (!state.monthlyYear || !years.includes(state.monthlyYear)) state.monthlyYear = years[years.length - 1];

  const select = $('#monthly-year');
  select.innerHTML = years.map((y) => `<option value="${y}"${y === state.monthlyYear ? ' selected' : ''}>${y}</option>`).join('');

  const year = state.monthlyYear;
  const groups = monthGroups(days);
  const goal = state.data.profile.dailyGoalSteps;
  const now = new Date();
  const currentYear = String(now.getFullYear());
  const lastVisibleMonth = year === currentYear ? now.getMonth() + 1 : 12; // 1-based

  const monthKeys = [];
  const monthSummaries = [];
  const prevYearSummaries = [];
  for (let m = 1; m <= 12; m += 1) {
    const key = `${year}-${pad2(m)}`;
    monthKeys.push(MONTH_SHORT[m - 1]);
    monthSummaries.push(summarizeDays(groups.get(key) || []));
    prevYearSummaries.push(summarizeDays(groups.get(`${Number(year) - 1}-${pad2(m)}`) || []));
  }

  const yearTotals = summarizeDays(days.filter((d) => d.date.startsWith(year)));
  const ytdSteps = monthSummaries.slice(0, lastVisibleMonth).reduce((s, m) => s + m.steps, 0);
  const prevYtdSteps = prevYearSummaries.slice(0, lastVisibleMonth).reduce((s, m) => s + m.steps, 0);
  const yoyDelta = percentDelta(ytdSteps, prevYtdSteps);
  const monthsWithData = monthSummaries.filter((m) => m.days > 0);
  const bestMonth = monthsWithData.reduce((best, m) => (!best || m.steps > best.steps ? m : best), null);

  $('#monthly-strip').innerHTML = [
    statChip('Year total', fmtInt(yearTotals.steps)),
    statChip('YoY (same span)', deltaHtml(yoyDelta)),
    statChip('Avg / recorded month', fmtInt(monthsWithData.length ? yearTotals.steps / monthsWithData.length : 0)),
    statChip('Best month', bestMonth ? fmtInt(bestMonth.steps) : '—'),
    statChip('Distance', fmtDistance(yearTotals.distanceMeters, 1)),
  ].join('');

  const maxVal = Math.max(...monthSummaries.map((m) => m.steps));
  renderBarChart($('#monthly-chart'), {
    labels: monthKeys,
    values: monthSummaries.map((m) => m.steps),
    highlightIndex: year === currentYear ? now.getMonth() : -1,
  });

  const rows = monthSummaries.map((summary, i) => {
    const prev = prevYearSummaries[i];
    const yoy = summary.days > 0 || prev.days > 0 ? deltaHtml(percentDelta(summary.steps, prev.steps)) : '<span class="goal-miss">—</span>';
    const bestDay = summary.best ? fmtInt(summary.best.steps) : '—';
    const isFuture = year === currentYear && i + 1 > lastVisibleMonth;
    return `<tr><td class="${isFuture ? 'goal-miss' : ''}">${MONTH_FULL[i]}${isFuture ? ' ·' : ''}</td>` +
      `<td class="num">${summary.days ? fmtInt(summary.steps) : '—'}</td>` +
      `<td class="num">${yoy}</td>` +
      `<td class="num">${summary.days ? fmtDistance(summary.distanceMeters, 1) : '—'}</td>` +
      `<td class="num">${summary.days ? fmtInt(summary.calories) : '—'}</td>` +
      `<td class="num">${bestDay}</td></tr>`;
  });
  rows.push(
    `<tr class="current-period"><td>Total ${escapeHtml(year)}</td><td class="num">${fmtInt(yearTotals.steps)}</td>` +
    `<td class="num">${deltaHtml(yoyDelta)}</td><td class="num">${fmtDistance(yearTotals.distanceMeters, 1)}</td>` +
    `<td class="num">${fmtInt(yearTotals.calories)}</td><td class="num">${yearTotals.best ? fmtInt(yearTotals.best.steps) : '—'}</td></tr>`
  );
  $('#monthly-table tbody').innerHTML = rows.join('');
}

function monthlyCsvRows() {
  const year = state.monthlyYear;
  const groups = monthGroups(state.data.days);
  const rows = [['month', 'steps', 'distance', 'unit', 'calories_kcal', 'recorded_days', 'best_day_steps']];
  for (let m = 1; m <= 12; m += 1) {
    const key = `${year}-${pad2(m)}`;
    const summary = summarizeDays(groups.get(key) || []);
    rows.push([key, summary.steps, distValue(summary.distanceMeters).toFixed(3), distanceUnit(), summary.calories.toFixed(1), summary.activeDays, summary.best ? summary.best.steps : 0]);
  }
  return rows;
}

// ─────────────────────────────  Yearly tab  ─────────────────────────────
function renderYearly() {
  const days = state.data.days;
  const groups = yearGroups(days);
  const years = [...groups.keys()].sort();
  if (!years.length) return;

  // Fill gaps so silent years show as zero rather than disappearing.
  const allYears = [];
  for (let y = Number(years[0]); y <= Number(years[years.length - 1]); y += 1) allYears.push(String(y));

  const summaries = allYears.map((y) => summarizeDays(groups.get(y) || []));
  renderBarChart($('#yearly-chart'), { labels: allYears, values: summaries.map((s) => s.steps) });

  $('#yearly-table tbody').innerHTML = allYears.map((y, i) => {
    const s = summaries[i];
    const avgPerActive = s.activeDays ? s.steps / s.activeDays : 0;
    return `<tr><td class="${i === allYears.length - 1 ? 'current-period' : ''}">${y}</td>` +
      `<td class="num">${s.days ? fmtInt(s.steps) : '—'}</td>` +
      `<td class="num">${s.activeDays ? fmtInt(avgPerActive) : '—'}</td>` +
      `<td class="num">${s.days ? fmtDistance(s.distanceMeters, 1) : '—'}</td>` +
      `<td class="num">${s.days ? fmtInt(s.calories) : '—'}</td>` +
      `<td class="num">${s.activeDays}</td></tr>`;
  }).join('');
}

// ─────────────────────────────  Compare tab  ─────────────────────────────
function comparisonKeys(gran) {
  const days = state.data.days;
  if (gran === 'day') return [...new Set(days.map((d) => d.date))].sort();
  if (gran === 'year') return [...new Set(days.map((d) => d.date.slice(0, 4)))].sort();
  return [...new Set(days.map((d) => d.date.slice(0, 7)))].sort();
}

function periodSummary(gran, key) {
  const prefix = gran === 'day' ? key : gran === 'month' ? key : key;
  return summarizeDays(state.data.days.filter((d) => d.date.startsWith(prefix)));
}

function periodLabel(gran, key) {
  if (gran === 'day') return dayLabel(key);
  if (gran === 'month') return monthLabel(key);
  return key;
}

function renderCompare() {
  const gran = state.compareGran;
  const keys = comparisonKeys(gran);
  const selA = $('#compare-a'), selB = $('#compare-b');

  if (!keys.length) {
    selA.innerHTML = ''; selB.innerHTML = '';
    $('#compare-grid').innerHTML = '<p class="goal-miss">Not enough data to compare yet.</p>';
    return;
  }
  if (!state.compareA || !keys.includes(state.compareA)) state.compareA = keys[keys.length - 1];
  if (!state.compareB || !keys.includes(state.compareB)) state.compareB = keys.length > 1 ? keys[keys.length - 2] : keys[keys.length - 1];

  const options = (selected) => keys.map((k) => `<option value="${k}"${k === selected ? ' selected' : ''}>${escapeHtml(periodLabel(gran, k))}</option>`).join('');
  selA.innerHTML = options(state.compareA);
  selB.innerHTML = options(state.compareB);

  const a = periodSummary(gran, state.compareA);
  const b = periodSummary(gran, state.compareB);
  const labelA = periodLabel(gran, state.compareA);
  const labelB = periodLabel(gran, state.compareB);
  const stepDelta = a.steps - b.steps;
  const stepPct = percentDelta(a.steps, b.steps);

  $('#compare-verdict').innerHTML = stepDelta === 0
    ? `<strong>${escapeHtml(labelA)}</strong> and <strong>${escapeHtml(labelB)}</strong> are dead even — same steps, same story.`
    : `<strong>${escapeHtml(stepDelta > 0 ? labelA : labelB)}</strong> out-walks <strong>${escapeHtml(stepDelta > 0 ? labelB : labelA)}</strong> by <strong>${fmtInt(Math.abs(stepDelta))} steps</strong>${stepPct === null ? '' : ` (${Math.abs(stepPct).toFixed(1)}% ${stepDelta > 0 ? 'more' : 'fewer'})`}.`;

  const metrics = [
    { label: 'Steps', a: a.steps, b: b.steps, fmt: fmtInt, raw: true },
    { label: 'Distance', a: a.distanceMeters, b: b.distanceMeters, fmt: (v) => fmtDistance(v, 2) },
    { label: 'Calories', a: a.calories, b: b.calories, fmt: fmtKcal },
    { label: 'Active days', a: a.activeDays, b: b.activeDays, fmt: fmtInt, raw: true },
    { label: 'Best single day', a: a.best ? a.best.steps : 0, b: b.best ? b.best.steps : 0, fmt: fmtInt, raw: true },
  ];

  $('#compare-grid').innerHTML = metrics.map((m) => {
    const max = Math.max(m.a, m.b, 1);
    const delta = percentDelta(m.a, m.b);
    const aWins = m.a >= m.b;
    return `
      <div class="compare-card">
        <span class="cc-label">${escapeHtml(m.label)}</span>
        <div class="cc-values">
          <span class="cc-a">${m.fmt(m.a)}</span>
          <span class="cc-vs">vs</span>
          <span class="cc-b">${m.fmt(m.b)}</span>
        </div>
        <span class="cc-delta ${aWins ? 'delta-up' : 'delta-down'}">${aWins ? '▲' : '▼'} ${delta === null ? '—' : Math.abs(delta).toFixed(1) + '%'} <span class="goal-miss">A vs B</span></span>
        <div class="cc-bar"><div style="width: ${((m.a / max) * 100).toFixed(1)}%"></div></div>
        <div class="cc-bar"><div style="width: ${((m.b / max) * 100).toFixed(1)}%; opacity: 0.55"></div></div>
      </div>`;
  }).join('');
}

// ─────────────────────────────  CSV export  ─────────────────────────────
function downloadCsv(filename, rows) {
  const csv = rows.map((row) => row.map((cell) => {
    const text = String(cell);
    return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
  }).join(',')).join('\n');
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

// ─────────────────────────────  View switching & bootstrap  ─────────────────────────────
function renderTab(tab) {
  state.tab = tab;
  $$('.tab').forEach((button) => button.classList.toggle('is-active', button.dataset.tab === tab));
  $$('.tab-panel').forEach((panel) => { panel.hidden = panel.id !== `tab-${tab}`; });
  if (!state.data) return;
  if (tab === 'daily') renderDaily();
  else if (tab === 'monthly') renderMonthly();
  else if (tab === 'yearly') renderYearly();
  else if (tab === 'compare') renderCompare();
}

function renderAll() {
  renderKpis();
  renderTab(state.tab);
}

function applyUnitPreference(unit) {
  state.unit = unit;
  $('#unit-metric').classList.toggle('is-active', effectiveUnit() === 'metric');
  $('#unit-imperial').classList.toggle('is-active', effectiveUnit() === 'imperial');
}

function showEmptyState() {
  $('#empty-state').hidden = false;
  $('#dashboard').hidden = true;
  $('#data-status').hidden = true;
}

/** Normalized data goes in; raw JSON text is kept for persistence. */
function setData(rawText, fileName) {
  const normalized = normalizeExport(JSON.parse(rawText));
  state.data = normalized;
  state.dailyMonth = null;
  state.monthlyYear = null;
  state.compareA = null;
  state.compareB = null;
  try { localStorage.setItem(LS_EXPORT, rawText); } catch (e) { console.warn('localStorage unavailable', e); }
  applyUnitPreference(state.unit ?? (normalized.profile.useMetric === false ? 'imperial' : 'metric'));
  $('#empty-state').hidden = true;
  $('#dashboard').hidden = false;
  renderStatusBar(fileName);
  renderAll();
  toast(`Loaded ${normalized.days.length} days of history`);
}

function clearStoredData() {
  try { localStorage.removeItem(LS_EXPORT); } catch (e) { /* ignore */ }
  state.data = null;
  showEmptyState();
  toast('Stored data cleared');
}

function loadFile(file) {
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    try { setData(String(reader.result), file.name); }
    catch (error) { console.error(error); toast(error.message || 'Could not parse that file'); }
  };
  reader.onerror = () => toast('Could not read the file');
  reader.readAsText(file);
}

function loadSample() {
  if (!window.SAMPLE_PEDOMETER_EXPORT) { toast('Sample dataset missing'); return; }
  try { setData(JSON.stringify(window.SAMPLE_PEDOMETER_EXPORT), 'sample dataset'); }
  catch (error) { console.error(error); toast('Sample dataset is invalid'); }
}

function init() {
  // Restore persisted unit preference early so the toggle renders correctly.
  try {
    const savedUnit = localStorage.getItem(LS_UNIT);
    if (savedUnit === 'metric' || savedUnit === 'imperial') state.unit = savedUnit;
  } catch (e) { /* ignore */ }

  // Header actions
  $('#unit-metric').addEventListener('click', () => { state.unit = 'metric'; try { localStorage.setItem(LS_UNIT, 'metric'); } catch (e) {} applyUnitPreference('metric'); if (state.data) renderAll(); });
  $('#unit-imperial').addEventListener('click', () => { state.unit = 'imperial'; try { localStorage.setItem(LS_UNIT, 'imperial'); } catch (e) {} applyUnitPreference('imperial'); if (state.data) renderAll(); });
  $('#btn-import').addEventListener('click', () => $('#file-input').click());
  $('#btn-import-empty').addEventListener('click', () => $('#file-input').click());
  $('#file-input').addEventListener('change', (event) => loadFile(event.target.files?.[0]));
  $('#btn-sample').addEventListener('click', loadSample);
  $('#btn-sample-empty').addEventListener('click', loadSample);
  $('#btn-clear').addEventListener('click', clearStoredData);
  $('#btn-print').addEventListener('click', () => window.print());

  // Drop zone
  const dropZone = $('#drop-zone');
  ['dragenter', 'dragover'].forEach((type) => dropZone.addEventListener(type, (e) => { e.preventDefault(); dropZone.classList.add('drag-over'); }));
  ['dragleave', 'drop'].forEach((type) => dropZone.addEventListener(type, (e) => { e.preventDefault(); dropZone.classList.remove('drag-over'); }));
  dropZone.addEventListener('drop', (e) => loadFile(e.dataTransfer?.files?.[0]));

  // Tabs
  $$('.tab').forEach((button) => button.addEventListener('click', () => renderTab(button.dataset.tab)));

  // Daily controls
  $('#daily-month').addEventListener('change', (e) => { state.dailyMonth = e.target.value; renderDaily(); });
  $('#btn-daily-csv').addEventListener('click', () => downloadCsv(`pedometer-${state.dailyMonth}.csv`, dailyCsvRows()));

  // Monthly controls
  $('#monthly-year').addEventListener('change', (e) => { state.monthlyYear = e.target.value; renderMonthly(); });
  $('#btn-monthly-csv').addEventListener('click', () => downloadCsv(`pedometer-${state.monthlyYear}.csv`, monthlyCsvRows()));

  // Compare controls
  $('#compare-granularity').addEventListener('change', (e) => { state.compareGran = e.target.value; state.compareA = null; state.compareB = null; renderCompare(); });
  $('#compare-a').addEventListener('change', (e) => { state.compareA = e.target.value; renderCompare(); });
  $('#compare-b').addEventListener('change', (e) => { state.compareB = e.target.value; renderCompare(); });
  $('#compare-swap').addEventListener('click', () => { const a = state.compareA; state.compareA = state.compareB; state.compareB = a; renderCompare(); });

  // Restore previous session
  let restored = false;
  try {
    const saved = localStorage.getItem(LS_EXPORT);
    if (saved) { setData(saved, null); restored = true; }
  } catch (error) {
    console.warn('Stored export could not be restored', error);
    localStorage.removeItem(LS_EXPORT);
  }
  if (!restored) { applyUnitPreference(state.unit ?? 'metric'); showEmptyState(); }
}

init();
