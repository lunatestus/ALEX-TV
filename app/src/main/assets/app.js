// ── Config ──
const API_KEY = '8bd45cfb804f84ce85fa6accd833d6a1';
const BASE    = 'https://api.themoviedb.org/3';
const IMG     = 'https://image.tmdb.org/t/p';
const BACKDROP_SIZE = 'w1280';
const TUNNEL_ROOT = 'https://lunatestus003--vibe-backend-tunnel.modal.run';
const LIBRARY_ROOT_PATH = '/media';

// ── Endpoints ──
const ROWS = [
  { id: 'trending',    url: `/trending/movie/week?api_key=${API_KEY}`,  type: 'movie' },
  { id: 'popular',     url: `/movie/popular?api_key=${API_KEY}`,       type: 'movie' },
  { id: 'top_rated',   url: `/movie/top_rated?api_key=${API_KEY}`,     type: 'movie' },
  { id: 'now_playing', url: `/movie/now_playing?api_key=${API_KEY}`,   type: 'movie' },
  { id: 'upcoming',    url: `/movie/upcoming?api_key=${API_KEY}`,      type: 'movie' },
  { id: 'popular_tv',  url: `/tv/popular?api_key=${API_KEY}`,          type: 'tv' },
  { id: 'latest_tv',   url: `/tv/on_the_air?api_key=${API_KEY}`,      type: 'tv' },
];

// ── State ──
const nav = {
  area: 0,
  col: 0,
  cols: [],         // remember last column per row
  rows: [],         // will hold references to focusable arrays per row
  movies: [],       // parallel array: movies[rowIdx][colIdx] = movie data
  libraryIndex: 0,
  libraryItems: [],
};

const NAV_MIN_INTERVAL_MS = 160;
let navLastTime = 0;
let navQueuedKey = null;
let navQueueTimer = null;
let lastRowScrollIndex = null;

const libraryState = {
  tunnelUrl: null,
  path: LIBRARY_ROOT_PATH,
  items: [],
  loading: false,
  hasLoaded: false,
};

// ── Helpers ──
const nativeBridge = typeof window !== 'undefined' ? window.AndroidBridge : null;
const nativeCallbacks = {};
let nativeCbSeq = 0;
const LIBRARY_ROOT = LIBRARY_ROOT_PATH;

function nativeFetchJson(url) {
  return new Promise((resolve, reject) => {
    const id = `cb_${Date.now()}_${nativeCbSeq++}`;
    nativeCallbacks[id] = { resolve, reject };
    if (!nativeBridge || typeof nativeBridge.fetchJson !== 'function') {
      delete nativeCallbacks[id];
      reject(new Error('Native bridge unavailable'));
      return;
    }
    nativeBridge.fetchJson(url, id);
  });
}

function updateNavState() {
  if (!nativeBridge || typeof nativeBridge.setNavState !== 'function') return;
  nativeBridge.setNavState(currentPage, libraryState.path || LIBRARY_ROOT);
}

window.__nativeFetchResolve = function(id, ok, payload) {
  const cb = nativeCallbacks[id];
  if (!cb) return;
  delete nativeCallbacks[id];
  if (ok) {
    try {
      cb.resolve(JSON.parse(payload));
    } catch (err) {
      cb.reject(err);
    }
  } else {
    cb.reject(new Error(payload || 'Native fetch failed'));
  }
};

async function fetchJson(url) {
  if (nativeBridge && typeof nativeBridge.fetchJson === 'function') {
    return nativeFetchJson(url);
  }
  const res = await fetch(url);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

function tmdbFetch(path) {
  return fetch(`${BASE}${path}`).then(r => r.json());
}

function posterURL(path, size = 'w300') {
  return path ? `${IMG}/${size}${path}` : '';
}

function backdropURL(path, size = BACKDROP_SIZE) {
  return path ? `${IMG}/${size}${path}` : '';
}

function year(dateStr) {
  return dateStr ? dateStr.split('-')[0] : '';
}

function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  let size = bytes;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit++;
  }
  const digits = size >= 10 || unit === 0 ? 0 : 1;
  return `${size.toFixed(digits)} ${units[unit]}`;
}

// ── Hero ──
let heroTimer = null;
let heroFadeTimer = null;
let heroPending = null;
let heroCurrentId = null;
function setHero(movie) {
  const backdrop = document.getElementById('hero-backdrop');

  // Minimal transition: debounce rapid focus moves and only crossfade backdrop
  heroPending = movie;
  clearTimeout(heroTimer);
  clearTimeout(heroFadeTimer);

  heroTimer = setTimeout(() => {
    const next = heroPending;
    heroPending = null;
    if (!next) return;

    const nextId = next.id || next.title || next.name || null;
    if (nextId && nextId === heroCurrentId) return;
    heroCurrentId = nextId;

    // Fade out backdrop only
    backdrop.className = 'fade-out';

    heroFadeTimer = setTimeout(() => {
      backdrop.style.backgroundImage = `url(${backdropURL(next.backdrop_path)})`;
      document.getElementById('hero-title').textContent = next.title || next.name;
      document.getElementById('hero-year').textContent = year(next.release_date || next.first_air_date);
      document.getElementById('hero-overview').textContent = next.overview;

      // Fade in backdrop
      backdrop.className = 'fade-in';
    }, 220);
  }, 160);
}

// ── Library ──
function setLibraryStatus(text) {
  const el = document.getElementById('library-status');
  if (!el) return;
  if (!text) {
    el.textContent = '';
    el.classList.add('hidden');
    return;
  }
  el.textContent = text;
  el.classList.remove('hidden');
}

function getParentPath(path) {
  if (!path || path === LIBRARY_ROOT) return null;
  const trimmed = path.replace(/\/+$/, '');
  const idx = trimmed.lastIndexOf('/');
  if (idx <= 0) return LIBRARY_ROOT;
  const parent = trimmed.slice(0, idx);
  if (!parent.startsWith(LIBRARY_ROOT)) return LIBRARY_ROOT;
  return parent || LIBRARY_ROOT;
}

window.__libraryBack = function() {
  const parent = getParentPath(libraryState.path || LIBRARY_ROOT);
  if (!parent) {
    window.__goHome && window.__goHome();
    return;
  }
  nav.libraryIndex = 0;
  loadLibrary(parent);
};

window.__goHome = function() {
  nav.area = 'nav';
  nav.col = navPills.findIndex(p => p.dataset.page === 'home');
  switchPage('home');
  focusCurrent();
};

async function resolveTunnelUrl() {
  const data = await fetchJson(TUNNEL_ROOT);
  if (!data || !data.url) throw new Error('Tunnel URL missing');
  if (data.status && data.status !== 'running') {
    throw new Error(`Tunnel status: ${data.status}`);
  }
  return data.url;
}

async function loadLibrary(path) {
  if (libraryState.loading) return;
  libraryState.loading = true;
  setLibraryStatus('Loading library...');
  try {
    if (!libraryState.tunnelUrl) {
      libraryState.tunnelUrl = await resolveTunnelUrl();
    }
    const listUrl = `${libraryState.tunnelUrl}/list?path=${encodeURIComponent(path)}`;
    const data = await fetchJson(listUrl);
    libraryState.path = data.path || path;
    libraryState.items = Array.isArray(data.items) ? data.items : [];
    libraryState.hasLoaded = true;
    updateNavState();
    renderLibrary();
    setLibraryStatus(libraryState.items.length ? '' : 'Empty folder');
  } catch (err) {
    setLibraryStatus('Failed to load library');
  } finally {
    libraryState.loading = false;
  }
}

function renderLibrary() {
  const list = document.getElementById('library-list');
  if (!list) return;
  list.innerHTML = '';

  const items = libraryState.items || [];
  if (!items.length) {
    const empty = document.createElement('div');
    empty.className = 'library-empty';
    empty.textContent = 'No items found';
    list.appendChild(empty);
    nav.libraryItems = [];
    nav.libraryIndex = 0;
    return;
  }

  const frag = document.createDocumentFragment();
  items.forEach((item, i) => {
    const row = document.createElement('div');
    row.className = 'library-item focusable';
    row.tabIndex = -1;
    row.dataset.index = String(i);
    row.dataset.path = item.path || '';
    row.dataset.type = item.type || '';

    const icon = document.createElement('div');
    icon.className = 'library-icon';
    icon.innerHTML = item.type === 'folder'
      ? '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor" class="icon icon-tabler icons-tabler-filled icon-tabler-folder"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M9 3a1 1 0 0 1 .608 .206l.1 .087l2.706 2.707h6.586a3 3 0 0 1 2.995 2.824l.005 .176v8a3 3 0 0 1 -2.824 2.995l-.176 .005h-14a3 3 0 0 1 -2.995 -2.824l-.005 -.176v-11a3 3 0 0 1 2.824 -2.995l.176 -.005h4z" /></svg>'
      : '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="currentColor" class="icon icon-tabler icons-tabler-filled icon-tabler-badge-hd"><path stroke="none" d="M0 0h24v24H0z" fill="none"/><path d="M19 4a3 3 0 0 1 3 3v10a3 3 0 0 1 -3 3h-14a3 3 0 0 1 -3 -3v-10a3 3 0 0 1 3 -3zm-4 4h-1a1 1 0 0 0 -1 1v6a1 1 0 0 0 1 1h1a3 3 0 0 0 3 -3v-2a3 3 0 0 0 -3 -3m-5 0a1 1 0 0 0 -1 1v2h-1v-2a1 1 0 0 0 -.883 -.993l-.117 -.007a1 1 0 0 0 -1 1v6a1 1 0 0 0 2 0v-2h1v2a1 1 0 0 0 .883 .993l.117 .007a1 1 0 0 0 1 -1v-6a1 1 0 0 0 -1 -1m5 2a1 1 0 0 1 1 1v2a1 1 0 0 1 -.883 .993l-.117 .007z" /></svg>';

    const name = document.createElement('div');
    name.className = 'library-name';
    name.textContent = item.name || item.path || 'Untitled';

    const left = document.createElement('div');
    left.className = 'library-left';
    left.appendChild(icon);
    left.appendChild(name);

    const meta = document.createElement('div');
    meta.className = 'library-meta';
    const typeLabel = item.type === 'folder' ? 'Folder' : 'File';
    const sizeLabel = item.type === 'file' ? formatBytes(item.size || 0) : '';
    meta.textContent = sizeLabel ? `${typeLabel} • ${sizeLabel}` : typeLabel;

    row.appendChild(left);
    row.appendChild(meta);
    frag.appendChild(row);
  });
  list.appendChild(frag);

  nav.libraryItems = Array.from(list.querySelectorAll('.library-item'));
  nav.libraryIndex = clamp(nav.libraryIndex, 0, nav.libraryItems.length - 1);
  if (currentPage === 'library' && nav.area === 'library') {
    focusCurrent();
  }
}

function scrollLibraryIntoView(el) {
  if (!el) return;
  el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

async function openLibraryFile(item) {
  if (!item || !item.path) return;
  try {
    if (!libraryState.tunnelUrl) {
      libraryState.tunnelUrl = await resolveTunnelUrl();
    }
    setLibraryStatus('Opening file...');
    const url = `${libraryState.tunnelUrl}/download-url?path=${encodeURIComponent(item.path)}`;
    const data = await fetchJson(url);
    if (data && data.url) {
      if (nativeBridge && typeof nativeBridge.play === 'function') {
        nativeBridge.play(data.url, item.name || '');
      } else {
        throw new Error('Native player unavailable');
      }
    } else {
      throw new Error('Missing stream URL');
    }
  } catch (err) {
    setLibraryStatus('Unable to open file');
  }
}

function handleLibraryEnter() {
  const item = (libraryState.items || [])[nav.libraryIndex];
  if (!item) return;
  if (item.type === 'folder') {
    nav.libraryIndex = 0;
    loadLibrary(item.path);
  } else {
    openLibraryFile(item);
  }
}

function ensureLibraryLoaded() {
  if (!libraryState.hasLoaded && !libraryState.loading) {
    loadLibrary(libraryState.path || LIBRARY_ROOT_PATH);
  }
}

// ── Cards ──
function createCard(movie, rowIdx, colIdx) {
  const el = document.createElement('div');
  el.className = 'card focusable';
  el.tabIndex = -1;
  el.dataset.row = rowIdx;
  el.dataset.col = colIdx;

  const img = document.createElement('img');
  img.className = 'card-poster';
  img.alt = movie.title || movie.name;
  img.loading = 'lazy';
  img.src = posterURL(movie.poster_path);

  el.appendChild(img);
  return el;
}

function renderRow(rowEl, movies, rowIdx) {
  const scroll = rowEl.querySelector('.row-scroll');
  scroll.innerHTML = '';
  rowEl.dataset.rowIndex = rowIdx;
  const focusables = [];
  nav.movies[rowIdx] = movies;
  movies.forEach((movie, i) => {
    const card = createCard(movie, rowIdx, i);
    scroll.appendChild(card);
    focusables.push(card);
  });
  nav.rows[rowIdx] = focusables;
}

// ── Skeleton loaders ──
function showSkeletons() {
  document.querySelectorAll('.row-scroll').forEach(scroll => {
    for (let i = 0; i < 8; i++) {
      const el = document.createElement('div');
      el.className = 'card skeleton';
      el.innerHTML = '<img class="card-poster" src="" alt="">';
      scroll.appendChild(el);
    }
  });
}

// ── Init ──
async function init() {
  showSkeletons();

  // Fetch all rows in parallel
  const results = await Promise.all(ROWS.map(r => tmdbFetch(r.url)));

  // Set hero from trending #1
  const heroMovie = results[0].results[0];
  if (heroMovie) setHero(heroMovie);

  // Render each row
  const rowEls = document.querySelectorAll('.row');
  results.forEach((data, i) => {
    renderRow(rowEls[i], data.results, i);
  });

  // Start focus on first content row
  nav.area = 0;
  nav.col = 0;
  focusCurrent();
}

// ── Page Switching ──
const navPills = Array.from(document.querySelectorAll('.nav-pill'));
let currentPage = 'home';

function switchPage(page) {
  const overlay = document.getElementById('coming-soon');
  const hero = document.getElementById('hero');
  const content = document.getElementById('content');
  const library = document.getElementById('library');

  navPills.forEach(p => p.classList.toggle('active', p.dataset.page === page));
  currentPage = page;
  updateNavState();

  if (page === 'home') {
    overlay.classList.add('hidden');
    hero.style.display = '';
    content.style.display = '';
    if (library) library.classList.add('hidden');
  } else if (page === 'library') {
    overlay.classList.add('hidden');
    hero.style.display = 'none';
    content.style.display = 'none';
    if (library) library.classList.remove('hidden');
    ensureLibraryLoaded();
  } else {
    overlay.classList.remove('hidden');
    hero.style.display = 'none';
    content.style.display = 'none';
    if (library) library.classList.add('hidden');
  }
}

// ── Spatial Navigation ──
function focusCurrent() {
  if (nav.area === 'nav') {
    const pills = navPills;
    nav.col = clamp(nav.col, 0, pills.length - 1);
    const el = pills[nav.col];
    if (el) {
      el.focus({ preventScroll: true });
      switchPage(el.dataset.page);
    }
    return;
  }
  if (nav.area === 'library') {
    const items = nav.libraryItems || [];
    nav.libraryIndex = clamp(nav.libraryIndex, 0, items.length - 1);
    const el = items[nav.libraryIndex];
    if (el) {
      el.focus({ preventScroll: true });
      scrollLibraryIntoView(el);
    }
    return;
  }
  const row = nav.rows[nav.area] || [];
  nav.col = clamp(nav.col, 0, row.length - 1);
  nav.cols[nav.area] = nav.col;
  const el = row[nav.col];
  if (el) {
    el.focus({ preventScroll: true });
    scrollIntoRow(el);
    scrollToRow(el);
    const movie = (nav.movies[nav.area] || [])[nav.col];
    if (movie) setHero(movie);
  }
}

function scrollToRow(el) {
  const content = document.getElementById('content');
  const rowEl = el.closest('.row');
  if (!rowEl) return;
  const rowIdx = Number(rowEl.dataset.rowIndex);
  if (!Number.isNaN(rowIdx) && rowIdx === lastRowScrollIndex) return;
  lastRowScrollIndex = Number.isNaN(rowIdx) ? null : rowIdx;
  const targetScroll = rowEl.offsetTop - 16;
  content.scrollTo({ top: targetScroll, behavior: 'smooth' });
}

function scrollIntoRow(el) {
  const scroll = el.closest('.row-scroll');
  if (!scroll) return;
  const elRect = el.getBoundingClientRect();
  const scrollRect = scroll.getBoundingClientRect();
  const leftEdge = scrollRect.left + 56;
  const rightEdge = scrollRect.right - 56;
  let delta = 0;
  if (elRect.left < leftEdge) {
    delta = elRect.left - leftEdge - 20;
  } else if (elRect.right > rightEdge) {
    delta = elRect.right - rightEdge + 20;
  }
  if (delta !== 0) {
    scroll.scrollTo({ left: scroll.scrollLeft + delta, behavior: 'smooth' });
  }
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function totalContentRows() {
  return ROWS.length;
}

function scheduleNav(key) {
  const now = performance.now();
  const elapsed = now - navLastTime;
  const delay = Math.max(0, NAV_MIN_INTERVAL_MS - elapsed);
  if (delay === 0) {
    navLastTime = now;
    processNavKey(key);
    return;
  }
  navQueuedKey = key;
  clearTimeout(navQueueTimer);
  navQueueTimer = setTimeout(() => {
    navLastTime = performance.now();
    const queued = navQueuedKey;
    navQueuedKey = null;
    if (queued) processNavKey(queued);
  }, delay);
}

function processNavKey(key) {
  const prevArea = nav.area;
  const prevCol = nav.col;
  const prevLib = nav.libraryIndex;

  if (key === 'Enter') {
    if (nav.area === 'library') handleLibraryEnter();
    return;
  }

  if (nav.area === 'library') {
    switch (key) {
      case 'ArrowDown':
        nav.libraryIndex = clamp(nav.libraryIndex + 1, 0, (nav.libraryItems || []).length - 1);
        break;
      case 'ArrowUp':
        if (nav.libraryIndex === 0) {
          nav.area = 'nav';
          nav.col = navPills.findIndex(p => p.dataset.page === currentPage);
        } else {
          nav.libraryIndex = Math.max(0, nav.libraryIndex - 1);
        }
        break;
      default:
        break;
    }

    if (nav.area === prevArea && nav.libraryIndex === prevLib) return;
    focusCurrent();
    return;
  }

  switch (key) {
    case 'ArrowRight':
      nav.col++;
      if (nav.area === 'nav') {
        nav.col = clamp(nav.col, 0, navPills.length - 1);
      } else {
        nav.col = clamp(nav.col, 0, (nav.rows[nav.area] || []).length - 1);
      }
      break;

    case 'ArrowLeft':
      nav.col = Math.max(0, nav.col - 1);
      break;

    case 'ArrowDown':
      if (nav.area === 'nav') {
        if (currentPage === 'home') {
          nav.area = 0;
          nav.col = nav.cols[0] ?? 0;
        } else if (currentPage === 'library') {
          nav.area = 'library';
          nav.libraryIndex = 0;
        }
      } else if (nav.area < totalContentRows() - 1) {
        nav.area++;
        nav.col = nav.cols[nav.area] ?? nav.col;
      }
      break;

    case 'ArrowUp':
      if (nav.area === 'nav') return;
      if (nav.area === 0) {
        nav.area = 'nav';
        nav.col = navPills.findIndex(p => p.dataset.page === currentPage);
      } else {
        nav.area--;
        nav.col = nav.cols[nav.area] ?? nav.col;
      }
      break;

    case 'Enter':
      return;
  }

  if (nav.area === prevArea && nav.col === prevCol) return;
  focusCurrent();
}

function handleKey(e) {
  const key = e.key;
  if (!['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Enter'].includes(key)) return;
  e.preventDefault();
  scheduleNav(key);
}

document.addEventListener('keydown', handleKey);

// ── Boot ──
init();
