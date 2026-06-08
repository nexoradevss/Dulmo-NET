const STATIC_CACHE = 'dolmusnet-static-v3';
const DATA_CACHE   = 'dolmusnet-data-v3';

const STATIC_FILES = [
  '/Dulmo-NET/passenger.html',
  '/Dulmo-NET/manifest.json',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css',
  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js',
  'https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js',
  'https://fonts.googleapis.com/css2?family=Cairo:wght@400;600;700&display=swap'
];

// ═══ INSTALL — تخزين الملفات الثابتة ═══
self.addEventListener('install', e => {
  e.waitUntil(
    caches.open(STATIC_CACHE)
      .then(cache => cache.addAll(STATIC_FILES))
      .then(() => self.skipWaiting())
  );
});

// ═══ ACTIVATE — حذف الكاش القديم ═══
self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys
          .filter(k => k !== STATIC_CACHE && k !== DATA_CACHE)
          .map(k => caches.delete(k))
      )
    ).then(() => clients.claim())
  );
});

// ═══ FETCH — استراتيجية ذكية حسب نوع الطلب ═══
self.addEventListener('fetch', e => {
  const url = new URL(e.request.url);

  // ── طلبات Supabase (بيانات حية) → Network First ──
  if (url.hostname.includes('supabase.co')) {
    e.respondWith(networkFirstWithCache(e.request, DATA_CACHE, 10000));
    return;
  }

  // ── خرائط OpenStreetMap (tiles) → Cache First ──
  if (url.hostname.includes('tile.openstreetmap.org')) {
    e.respondWith(cacheFirstWithNetwork(e.request, STATIC_CACHE));
    return;
  }

  // ── الملفات الثابتة (HTML/JS/CSS/fonts) → Cache First ──
  if (
    url.pathname.startsWith('/Dulmo-NET/') ||
    url.hostname.includes('unpkg.com') ||
    url.hostname.includes('cdnjs.cloudflare.com') ||
    url.hostname.includes('fonts.googleapis.com') ||
    url.hostname.includes('fonts.gstatic.com')
  ) {
    e.respondWith(cacheFirstWithNetwork(e.request, STATIC_CACHE));
    return;
  }

  // ── باقي الطلبات → Network فقط ──
  e.respondWith(fetch(e.request));
});

// ═══ Cache First: من الكاش، وإن لم يوجد جلب من الشبكة وخزّن ═══
async function cacheFirstWithNetwork(request, cacheName) {
  const cache  = await caches.open(cacheName);
  const cached = await cache.match(request);
  if (cached) return cached;
  try {
    const response = await fetch(request);
    if (response.ok) cache.put(request, response.clone());
    return response;
  } catch {
    return new Response('Offline', { status: 503 });
  }
}

// ═══ Network First: جرّب الشبكة أولاً، وإن فشلت استخدم الكاش ═══
async function networkFirstWithCache(request, cacheName, timeout) {
  const cache = await caches.open(cacheName);
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    const response = await fetch(request, { signal: controller.signal });
    clearTimeout(timer);
    if (response.ok) cache.put(request, response.clone());
    return response;
  } catch {
    const cached = await cache.match(request);
    return cached || new Response(JSON.stringify([]), {
      status: 200,
      headers: { 'Content-Type': 'application/json' }
    });
  }
}
