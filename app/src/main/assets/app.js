// ── Config ──
const API_KEY = '8bd45cfb804f84ce85fa6accd833d6a1';
const BASE    = 'https://api.themoviedb.org/3';
const IMG     = 'https://image.tmdb.org/t/p';

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
  rows: [],         // will hold references to focusable arrays per row
  movies: [],       // parallel array: movies[rowIdx][colIdx] = movie data
};

// ── Helpers ──
function tmdbFetch(path) {
  return fetch(`${BASE}${path}`).then(r => r.json());
}

function posterURL(path, size = 'w300') {
  return path ? `${IMG}/${size}${path}` : '';
}

function backdropURL(path) {
  return path ? `${IMG}/original${path}` : '';
}

function year(dateStr) {
  return dateStr ? dateStr.split('-')[0] : '';
}

// ── Hero ──
let heroTimer = null;
function setHero(movie) {
  const backdrop = document.getElementById('hero-backdrop');
  const content  = document.getElementById('hero-content');

  // Cancel any pending transition
  clearTimeout(heroTimer);

  // Fade out
  backdrop.className = 'fade-out';
  content.className  = 'fade-out';

  heroTimer = setTimeout(() => {
    // Swap content while invisible
    backdrop.style.backgroundImage = `url(${backdropURL(movie.backdrop_path)})`;
    document.getElementById('hero-title').textContent = movie.title || movie.name;
    document.getElementById('hero-overview').textContent = movie.overview;

    // Fade in
    backdrop.className = 'fade-in';
    content.className  = 'fade-in';
  }, 150);
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

  navPills.forEach(p => p.classList.toggle('active', p.dataset.page === page));
  currentPage = page;

  if (page === 'home') {
    overlay.classList.add('hidden');
    hero.style.display = '';
    content.style.display = '';
  } else {
    overlay.classList.remove('hidden');
    hero.style.display = 'none';
    content.style.display = 'none';
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
  const row = nav.rows[nav.area] || [];
  nav.col = clamp(nav.col, 0, row.length - 1);
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
  const targetScroll = rowEl.offsetTop - 16;
  content.scrollTo({ top: targetScroll, behavior: 'smooth' });
}

function scrollIntoRow(el) {
  const scroll = el.closest('.row-scroll');
  if (!scroll) return;
  const elRect = el.getBoundingClientRect();
  const scrollRect = scroll.getBoundingClientRect();
  if (elRect.left < scrollRect.left + 56) {
    scroll.scrollLeft -= (scrollRect.left + 56 - elRect.left + 20);
  } else if (elRect.right > scrollRect.right - 56) {
    scroll.scrollLeft += (elRect.right - scrollRect.right + 56 + 20);
  }
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function totalContentRows() {
  return ROWS.length;
}

function handleKey(e) {
  const key = e.key;
  if (!['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Enter'].includes(key)) return;
  e.preventDefault();

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
          nav.col = 0;
        }
      } else if (nav.area < totalContentRows() - 1) {
        nav.area++;
      }
      break;

    case 'ArrowUp':
      if (nav.area === 'nav') return;
      if (nav.area === 0) {
        nav.area = 'nav';
        nav.col = navPills.findIndex(p => p.dataset.page === currentPage);
      } else {
        nav.area--;
      }
      break;

    case 'Enter':
      return;
  }

  focusCurrent();
}

document.addEventListener('keydown', handleKey);

// ── Boot ──
init();
