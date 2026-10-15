/*
 * v2.4.0 rc15: SplitMate Loop map (MapLibre GL JS, the engine mapcn is built on).
 *
 * Kotlin -> JS:  window.splitmate.init(styleJson, overlayJson, dataJson, optionsJson)
 *                window.splitmate.setStyle(styleJson, overlayJson)      (theme change)
 *                window.splitmate.setData(dataJson)                     (day / stops change)
 *                window.splitmate.select(id | null, animate)
 *                window.splitmate.fit(animate)  /  window.splitmate.zoomBy(delta, animate)
 * JS -> Kotlin:  SplitMateBridge.onReady() / onStopTap(id) / onError(message)
 *
 * Everything arrives as JSON strings built by Kotlin; nothing from the network is ever evaluated.
 */
(function () {
  'use strict';
  var bridge = window.SplitMateBridge || { onReady: function () {}, onStopTap: function () {}, onError: function () {} };
  var map = null;
  var overlay = null;
  var data = { route: emptyFc(), points: emptyFc() };
  var selectedId = null;
  // CSS px == dp in the WebView. Top clears the credit chip, bottom the stop card, right the toolbar.
  var padding = { top: 56, bottom: 100, left: 32, right: 88 };
  var errorSent = false;

  function emptyFc() { return { type: 'FeatureCollection', features: [] }; }
  function parse(json, fallback) { try { return JSON.parse(json); } catch (e) { return fallback; } }

  function addOverlay() {
    if (!map || !overlay) return;
    if (!map.getSource('loop-route')) map.addSource('loop-route', { type: 'geojson', data: data.route });
    if (!map.getSource('loop-points')) map.addSource('loop-points', { type: 'geojson', data: data.points });
    var font = ['Noto Sans Bold'];
    var layers = [
      { id: 'loop-route-casing', type: 'line', source: 'loop-route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': overlay.routeCasing, 'line-width': ['interpolate', ['linear'], ['zoom'], 10, 6, 16, 10] } },
      { id: 'loop-route', type: 'line', source: 'loop-route',
        layout: { 'line-join': 'round', 'line-cap': 'round' },
        paint: { 'line-color': overlay.route, 'line-width': ['interpolate', ['linear'], ['zoom'], 10, 3, 16, 5] } },
      { id: 'loop-stop-selected', type: 'circle', source: 'loop-points',
        filter: ['==', ['get', 'id'], selectedId || '__none__'],
        paint: { 'circle-radius': 20, 'circle-color': overlay.route, 'circle-opacity': 0.22 } },
      { id: 'loop-stay', type: 'circle', source: 'loop-points', filter: ['==', ['get', 'kind'], 'STAY'],
        paint: { 'circle-radius': 12, 'circle-color': overlay.stay, 'circle-stroke-width': 3, 'circle-stroke-color': overlay.stopRing } },
      { id: 'loop-stay-label', type: 'symbol', source: 'loop-points', filter: ['==', ['get', 'kind'], 'STAY'],
        layout: { 'text-field': 'H', 'text-font': font, 'text-size': 12, 'text-allow-overlap': true, 'text-ignore-placement': true },
        paint: { 'text-color': overlay.onStay } },
      { id: 'loop-stop', type: 'circle', source: 'loop-points', filter: ['!=', ['get', 'kind'], 'STAY'],
        paint: { 'circle-radius': ['case', ['==', ['get', 'id'], selectedId || '__none__'], 14, 11],
                 'circle-color': overlay.stop, 'circle-stroke-width': 3, 'circle-stroke-color': overlay.stopRing } },
      { id: 'loop-stop-number', type: 'symbol', source: 'loop-points', filter: ['!=', ['get', 'kind'], 'STAY'],
        layout: { 'text-field': ['to-string', ['get', 'index']], 'text-font': font, 'text-size': 12,
                  'text-allow-overlap': true, 'text-ignore-placement': true, 'symbol-sort-key': ['get', 'index'] },
        paint: { 'text-color': overlay.onStop } }
    ];
    layers.forEach(function (l) { if (!map.getLayer(l.id)) map.addLayer(l); });
  }

  function applySelection() {
    if (!map || !map.getLayer('loop-stop')) return;
    var id = selectedId || '__none__';
    map.setFilter('loop-stop-selected', ['==', ['get', 'id'], id]);
    map.setPaintProperty('loop-stop', 'circle-radius', ['case', ['==', ['get', 'id'], id], 14, 11]);
  }

  function bounds() {
    var b = null;
    data.points.features.forEach(function (f) {
      var c = f.geometry.coordinates;
      if (!b) b = new maplibregl.LngLatBounds(c, c); else b.extend(c);
    });
    return b;
  }

  function fit(animate) {
    var b = bounds();
    if (!map || !b) return;
    map.fitBounds(b, { padding: padding, maxZoom: 16, duration: animate ? 700 : 0 });
  }

  function featureById(id) {
    for (var i = 0; i < data.points.features.length; i++) {
      if (data.points.features[i].properties.id === id) return data.points.features[i];
    }
    return null;
  }

  window.splitmate = {
    init: function (styleJson, overlayJson, dataJson, optionsJson) {
      try {
        overlay = parse(overlayJson, null);
        var d = parse(dataJson, null);
        if (d) data = d;
        var opts = parse(optionsJson, {}) || {};
        if (opts.padding) padding = opts.padding;
        map = new maplibregl.Map({
          container: 'map',
          style: parse(styleJson, null),
          attributionControl: false,
          dragRotate: false,
          pitchWithRotate: false,
          touchPitch: false,
          maxPitch: 0,
          renderWorldCopies: false,
          fadeDuration: opts.reducedMotion ? 0 : 300
        });
        map.touchZoomRotate.disableRotation();
        map.keyboard.disable();
        map.on('style.load', function () { addOverlay(); applySelection(); });
        map.once('load', function () { fit(false); bridge.onReady(); });
        map.on('click', function (e) {
          var r = 24; // ±24 px hit box (48 dp-ish target)
          var hits = map.queryRenderedFeatures([[e.point.x - r, e.point.y - r], [e.point.x + r, e.point.y + r]],
            { layers: ['loop-stop', 'loop-stay'] });
          if (hits.length) bridge.onStopTap(String(hits[0].properties.id));
        });
        map.on('error', function (e) {
          if (errorSent) return;
          errorSent = true;
          bridge.onError(String((e && e.error && e.error.message) || 'map error'));
        });
      } catch (e) {
        bridge.onError(String(e && e.message || e));
      }
    },
    setStyle: function (styleJson, overlayJson) {
      if (!map) return;
      overlay = parse(overlayJson, overlay);
      map.setStyle(parse(styleJson, null)); // 'style.load' re-adds the loop layers
    },
    setData: function (dataJson) {
      var d = parse(dataJson, null);
      if (!d || !map) return;
      data = d;
      var r = map.getSource('loop-route'); if (r) r.setData(data.route);
      var p = map.getSource('loop-points'); if (p) p.setData(data.points);
      fit(true);
    },
    select: function (id, animate) {
      selectedId = id || null;
      applySelection();
      var f = id ? featureById(id) : null;
      if (map && f) map.easeTo({ center: f.geometry.coordinates, zoom: Math.max(map.getZoom(), 14), padding: padding, duration: animate ? 600 : 0 });
    },
    fit: function (animate) { fit(animate); },
    zoomBy: function (delta, animate) {
      if (!map) return;
      map.easeTo({ zoom: map.getZoom() + delta, duration: animate ? 250 : 0 });
    }
  };
  if (bridge.onPageReady) bridge.onPageReady();
})();
