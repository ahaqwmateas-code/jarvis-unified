const C = "jarvis-v1";
self.addEventListener("install", function(e){ self.skipWaiting(); });
self.addEventListener("activate", function(e){ e.waitUntil(self.clients.claim()); });
self.addEventListener("fetch", function(e){
  var u = new URL(e.request.url);
  if (u.pathname.indexOf("/api") === 0) {
    e.respondWith(fetch(e.request).catch(function(){ return caches.match(e.request); }));
    return;
  }
  e.respondWith(
    caches.open(C).then(function(c){
      return c.match(e.request).then(function(cached){
        var net = fetch(e.request).then(function(r){
          if (r.ok) c.put(e.request, r.clone());
          return r;
        }).catch(function(){ return cached; });
        return cached || net;
      });
    })
  );
});
