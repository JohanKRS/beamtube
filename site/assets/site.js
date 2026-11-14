/* BeamTube project site: languages, the dust in the logo, and a few small demos. No libraries. */
(function () {
  "use strict";

  var root = document.documentElement;
  var reduceMotion = window.matchMedia ? window.matchMedia("(prefers-reduced-motion: reduce)") : { matches: false };

  function store(key, value) {
    try {
      if (value === undefined) return localStorage.getItem(key);
      localStorage.setItem(key, value);
    } catch (e) {}
    return null;
  }

  /* ---- languages --------------------------------------------------------------------------- */
  // The English text is the page itself. Portuguese and Spanish come from i18n.js, by key.

  var DICT = window.BEAMTUBE_I18N || {};
  var LANG_TAGS = { en: "en", pt: "pt-BR", es: "es" };
  var ATTRS = [
    ["data-i18n-alt", "alt"],
    ["data-i18n-aria", "aria-label"],
    ["data-i18n-title", "title"],
    ["data-i18n-content", "content"]
  ];
  var english = new Map();
  var current = "en";

  function remember() {
    document.querySelectorAll("[data-i18n]").forEach(function (el) {
      english.set(el, { html: el.innerHTML });
    });
    ATTRS.forEach(function (pair) {
      document.querySelectorAll("[" + pair[0] + "]").forEach(function (el) {
        var saved = english.get(el) || {};
        saved[pair[1]] = el.getAttribute(pair[1]);
        english.set(el, saved);
      });
    });
  }

  function text(key, fallback) {
    var table = DICT[current];
    return table && Object.prototype.hasOwnProperty.call(table, key) ? table[key] : fallback;
  }

  function applyLanguage(lang) {
    current = (DICT[lang] || lang === "en") ? lang : "en";
    root.lang = LANG_TAGS[current];
    root.setAttribute("data-lang", current);
    document.querySelectorAll("[data-i18n]").forEach(function (el) {
      var key = el.getAttribute("data-i18n");
      var original = english.get(el).html;
      var value = current === "en" ? original : text(key, original);
      if (el.tagName === "TITLE") document.title = value.replace(/&amp;/g, "&");
      else el.innerHTML = value;
    });
    ATTRS.forEach(function (pair) {
      document.querySelectorAll("[" + pair[0] + "]").forEach(function (el) {
        var key = el.getAttribute(pair[0]);
        var original = english.get(el)[pair[1]];
        el.setAttribute(pair[1], current === "en" ? original : text(key, original));
      });
    });
    document.querySelectorAll(".lang button").forEach(function (b) {
      b.setAttribute("aria-pressed", String(b.getAttribute("data-lang") === current));
    });
    root.classList.remove("i18n-wait");
  }

  remember();
  applyLanguage(root.getAttribute("data-lang") || "en");

  document.querySelectorAll(".lang button").forEach(function (button) {
    button.addEventListener("click", function () {
      var lang = button.getAttribute("data-lang");
      store("beamtube-lang", lang);
      applyLanguage(lang);
    });
  });

  /* ---- the dust in the logo ---------------------------------------------------------------- */
  // Drawn between the beam and the name, in the units of the logo (make_site_assets.py,
  // HERO_VIEWBOX), the same way make_icons.py scatters it: more near the slit, fainter far from
  // it and near the edges of the beam. Here the grains drift, glint, and get stirred when the
  // pointer passes through the light.

  var lockup = document.querySelector(".lockup");
  var canvas = lockup && lockup.querySelector(".l-dust");
  if (canvas && canvas.getContext) dust(lockup, canvas);

  function dust(box, canvas) {
    var VB = { x: 100, y: 30, w: 600, h: 240 };
    // The beam in the logo: from the slit (x 160) to x 700, opening up and down.
    var X0 = 170, X1 = 700;
    function top(x) { return 89.4 - (x - 160) * 0.0933 + 3; }
    function bottom(x) { return 210 + (x - 160) * 0.0943 - 3; }
    var COLOR = "255,241,209"; // the light of the night icon, halfway to white

    var ctx = canvas.getContext("2d");
    var scale = 1, dpr = 1, width = 0;
    var grains = [];
    var start = performance.now();
    var last = start;
    var running = false, visible = true, frame = 0;
    var pointer = { x: 0, y: 0, vx: 0, vy: 0, active: false, t: 0 };

    // A soft, out-of-focus grain, drawn once and stamped.
    var sprite = document.createElement("canvas");
    sprite.width = sprite.height = 64;
    (function () {
      var g = sprite.getContext("2d");
      var r = g.createRadialGradient(32, 32, 0, 32, 32, 32);
      r.addColorStop(0, "rgba(" + COLOR + ",1)");
      r.addColorStop(0.35, "rgba(" + COLOR + ",0.6)");
      r.addColorStop(1, "rgba(" + COLOR + ",0)");
      g.fillStyle = r;
      g.fillRect(0, 0, 64, 64);
    })();

    function rand(a, b) { return a + Math.random() * (b - a); }
    function clamp(v, a, b) { return v < a ? a : v > b ? b : v; }

    function spawn(g, anywhere) {
      g = g || {};
      g.x = X0 + (X1 - X0) * Math.pow(Math.random(), 1.35);
      g.v = rand(0.04, 0.96);
      g.y = top(g.x) + (bottom(g.x) - top(g.x)) * g.v;
      var k = Math.random();
      if (k < 0.54) {
        g.soft = false;
        g.r = rand(0.55, 1.5);
        g.o = [0.3, 0.45, 0.6, 0.8, 1][Math.floor(Math.random() * 5)] * rand(0.8, 1) * 1.15;
        g.speed = 1;
      } else if (k < 0.86) {
        g.soft = true;
        g.r = rand(2.2, 4.6);
        g.o = rand(0.4, 0.8);
        g.speed = 0.75;
      } else {
        g.soft = true;
        g.r = rand(6, 13);
        g.o = rand(0.16, 0.3);
        g.speed = 0.45;
      }
      g.vx = rand(-3, 3);
      g.vy = rand(-3, 3);
      g.phase = rand(0, Math.PI * 2);
      g.twinkle = rand(0.5, 2.2);
      g.life = anywhere ? rand(0, 1) * 18 : 0;
      g.span = rand(10, 26);
      return g;
    }

    function count() {
      return width < 520 ? 80 : 120;
    }

    function resize() {
      var rect = box.getBoundingClientRect();
      if (!rect.width) return;
      width = rect.width;
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = Math.round(rect.width * dpr);
      canvas.height = Math.round(rect.height * dpr);
      scale = canvas.width / VB.w;
      while (grains.length < count()) grains.push(spawn(null, true));
      grains.length = count();
      if (!running) draw(performance.now());
    }

    // How bright a grain is where it is: brighter near the slit, fading at the edges of the beam.
    function light(g) {
      var t = clamp((g.x - X0) / (X1 - X0), 0, 1);
      var fall = Math.max(0.16, Math.pow(1 - t, 1.05));
      var span = bottom(g.x) - top(g.x);
      var v = (g.y - top(g.x)) / span;
      var edge = Math.min(v, 1 - v);
      if (edge <= 0) return 0;
      return fall * Math.min(1, 0.5 + edge * 4) * Math.min(1, edge / 0.06);
    }

    function step(dt, now) {
      var stir = pointer.active && now - pointer.t < 600;
      for (var i = 0; i < grains.length; i++) {
        var g = grains[i];
        // A slow random walk, like dust in still air, and a faint rise.
        g.vx += (Math.random() - 0.5) * 9 * dt;
        g.vy += (Math.random() - 0.5) * 9 * dt - 0.25 * dt;
        if (stir) {
          var dx = g.x - pointer.x, dy = g.y - pointer.y;
          var d2 = dx * dx + dy * dy;
          var R = 70;
          if (d2 < R * R) {
            var f = 1 - Math.sqrt(d2) / R;
            f = f * f;
            // The air follows the hand, and pushes a little to the sides.
            g.vx += (pointer.vx * 0.9 + dx * 2.2) * f * dt;
            g.vy += (pointer.vy * 0.9 + dy * 2.2) * f * dt;
          }
        }
        var damp = Math.exp(-dt / 2.5);
        g.vx *= damp;
        g.vy *= damp;
        var sp = Math.sqrt(g.vx * g.vx + g.vy * g.vy), max = 40;
        if (sp > max) { g.vx *= max / sp; g.vy *= max / sp; }
        g.x += g.vx * g.speed * dt;
        g.y += g.vy * g.speed * dt;
        g.life += dt;
        if (g.life > g.span || g.x < X0 - 4 || g.x > X1 || g.y < top(g.x) - 8 || g.y > bottom(g.x) + 8) spawn(g, false);
      }
    }

    function draw(now) {
      var t = (now - start) / 1000;
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.setTransform(scale, 0, 0, scale, -VB.x * scale, -VB.y * scale);
      // The beam opens from the slit in the first moments (as in site.css), and the dust with it.
      var still = reduceMotion.matches;
      var reveal = still ? 1e9 : VB.x + VB.w * clamp((t - 0.35) / 1.4, 0, 1) * 1.1;
      var intro = still ? 1 : clamp((t - 0.4) / 1.6, 0, 1);
      var minR = 0.55 / scale;
      for (var i = 0; i < grains.length; i++) {
        var g = grains[i];
        var a = g.o * light(g) * intro;
        if (!still) {
          a *= Math.min(1, g.life / 1.5, (g.span - g.life) / 1.5);
          if (!g.soft) a *= 0.72 + 0.28 * Math.sin(g.phase + t * g.twinkle);
        }
        a *= clamp((reveal - g.x) / 40, 0, 1);
        if (a <= 0.01) continue;
        ctx.globalAlpha = Math.min(1, a);
        if (g.soft) {
          ctx.drawImage(sprite, g.x - g.r, g.y - g.r, g.r * 2, g.r * 2);
        } else {
          ctx.fillStyle = "rgb(" + COLOR + ")";
          ctx.beginPath();
          ctx.arc(g.x, g.y, Math.max(g.r, minR), 0, Math.PI * 2);
          ctx.fill();
        }
      }
      ctx.globalAlpha = 1;
    }

    function loop(now) {
      if (!running) return;
      var dt = Math.min(0.05, (now - last) / 1000);
      last = now;
      step(dt, now);
      draw(now);
      frame = requestAnimationFrame(loop);
    }

    function play() {
      var should = visible && !reduceMotion.matches && !document.hidden;
      if (should && !running) {
        running = true;
        last = performance.now();
        frame = requestAnimationFrame(loop);
      } else if (!should && running) {
        running = false;
        cancelAnimationFrame(frame);
        draw(performance.now());
      }
    }

    function onPointer(e) {
      var rect = box.getBoundingClientRect();
      var x = VB.x + (e.clientX - rect.left) / rect.width * VB.w;
      var y = VB.y + (e.clientY - rect.top) / rect.height * VB.h;
      var now = performance.now();
      if (pointer.active) {
        var dt = Math.max(0.008, (now - pointer.t) / 1000);
        pointer.vx = clamp((x - pointer.x) / dt, -600, 600);
        pointer.vy = clamp((y - pointer.y) / dt, -600, 600);
      }
      pointer.x = x;
      pointer.y = y;
      pointer.t = now;
      pointer.active = true;
    }

    var hero = box.closest(".hero") || box;
    hero.addEventListener("pointermove", onPointer, { passive: true });
    hero.addEventListener("pointerleave", function () { pointer.active = false; }, { passive: true });

    if ("ResizeObserver" in window) new ResizeObserver(resize).observe(box);
    else window.addEventListener("resize", resize);
    if ("IntersectionObserver" in window) {
      new IntersectionObserver(function (entries) {
        visible = entries[0].isIntersecting;
        play();
      }).observe(box);
    }
    document.addEventListener("visibilitychange", play);
    if (reduceMotion.addEventListener) reduceMotion.addEventListener("change", play);

    resize();
    play();
  }

  /* ---- sleep mode: try the brightness ------------------------------------------------------- */

  var shot = document.getElementById("sleep-shot");
  document.querySelectorAll("[data-dim]").forEach(function (button, _, all) {
    button.addEventListener("click", function () {
      // 0% keeps a trace of the picture, so the demo still shows something.
      var level = Math.max(parseFloat(button.getAttribute("data-dim")), 0.03);
      if (shot) shot.style.setProperty("--b", String(level));
      all.forEach(function (b) { b.setAttribute("aria-pressed", String(b === button)); });
    });
  });

  /* ---- highlight colors ---------------------------------------------------------------------- */

  document.querySelectorAll("[data-accent]").forEach(function (button, _, all) {
    button.addEventListener("click", function () {
      var colors = button.getAttribute("data-accent").split(" ");
      root.style.setProperty("--accent", colors[0]);
      root.style.setProperty("--accent-soft", colors[1]);
      all.forEach(function (b) { b.setAttribute("aria-pressed", String(b === button)); });
    });
  });

  /* ---- copy the Obtainium address ------------------------------------------------------------ */

  var copyButton = document.getElementById("copy-url");
  var url = document.getElementById("repo-url");
  if (copyButton && url) {
    copyButton.addEventListener("click", function () {
      var done = function () {
        copyButton.textContent = text("js.copied", "Copied");
        setTimeout(function () { copyButton.textContent = text("install.copy", "Copy"); }, 2000);
      };
      var select = function () {
        var range = document.createRange();
        range.selectNodeContents(url);
        var sel = window.getSelection();
        sel.removeAllRanges();
        sel.addRange(range);
      };
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(url.textContent.trim()).then(done, select);
      } else {
        select();
      }
    });
  }

  /* ---- screenshots, bigger ------------------------------------------------------------------- */

  var box = document.getElementById("lightbox");
  if (box && box.showModal) {
    var big = box.querySelector("img");
    document.querySelectorAll(".screen img").forEach(function (img) {
      img.addEventListener("click", function () {
        big.src = img.currentSrc || img.src;
        big.alt = img.alt;
        box.showModal();
      });
    });
    box.addEventListener("click", function () { box.close(); });
  }

  /* ---- the latest release -------------------------------------------------------------------- */
  // The button goes straight to the APK of the latest release when GitHub answers; otherwise it
  // stays on the release page.

  var download = document.getElementById("download");
  var version = document.getElementById("version");
  function showRelease(info) {
    if (!info || !info.url) return;
    download.href = info.url;
    version.textContent = info.version + " · " + info.size + " MB";
    version.hidden = false;
  }
  if (download && version && window.fetch) {
    var cached = null;
    try { cached = JSON.parse(sessionStorage.getItem("beamtube-release") || "null"); } catch (e) {}
    if (cached) {
      showRelease(cached);
    } else {
      fetch("https://api.github.com/repos/JohanKRS/beamtube/releases/latest", { headers: { Accept: "application/vnd.github+json" } })
        .then(function (r) { return r.ok ? r.json() : null; })
        .then(function (release) {
          if (!release || !release.assets) return;
          var apk = release.assets.filter(function (a) { return /^beamtube-.*\.apk$/.test(a.name); })[0];
          if (!apk) return;
          var info = {
            url: apk.browser_download_url,
            version: String(release.tag_name || "").replace(/^v/, ""),
            size: Math.round(apk.size / 1e6)
          };
          try { sessionStorage.setItem("beamtube-release", JSON.stringify(info)); } catch (e) {}
          showRelease(info);
        })
        .catch(function () {});
    }
  }
})();
