package app.afterhours;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The parts of the desktop helper that Afterhours needs on a phone, answered inside the app:
 * feeds, Apple's podcast directory, cover art and the website fallback. Plain Java, so it can be tested off the phone.
 */
final class Api {
    static final String VERSION = "1.0.0";
    static final String APP_UA = "Mozilla/5.0 (compatible; Afterhours/" + VERSION + "; Android podcast player)";
    static final String BROWSER_UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36";
    static final String ACCEPT = "application/rss+xml, application/xml;q=0.9, text/xml;q=0.9, application/json;q=0.8, text/html;q=0.7, */*;q=0.5";
    static final Set<Integer> RETRY = new LinkedHashSet<>(Arrays.asList(403, 405, 406, 429, 451, 503));
    static final Pattern AUDIO = Pattern.compile("https?://[^\\s\"'<>\\\\]+?\\.(?:mp3|m4a|m4b|aac|ogg|oga|opus|wav|flac)(?:\\?[^\\s\"'<>\\\\]*)?", Pattern.CASE_INSENSITIVE);
    static final Pattern ENCLOSURE = Pattern.compile("<enclosure\\b", Pattern.CASE_INSENSITIVE);
    static final Pattern TAG_ATTR = Pattern.compile("([\\w:-]+)\\s*=\\s*(\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    static final Pattern EPISODE_HINT = Pattern.compile("(episode|/ep[-_/]?\\d|/e/\\d|podcast|/show|/listen|/audio|/radio|/archives?/|/\\d{4}/\\d{2}/|/p/|/posts?/|/blog/)", Pattern.CASE_INSENSITIVE);
    static final Pattern SKIP_LINK = Pattern.compile("(about|contact|privacy|terms|login|sign-?in|sign-?up|register|cart|shop|store|/tags?/|categor|/author/|search|subscribe|donate|\\.(?:jpe?g|png|gif|svg|webp|pdf|zip)(?:\\?|$))", Pattern.CASE_INSENSITIVE);
    static final String[] GUESSES = {"feed", "feed/podcast", "podcast/feed", "rss", "rss.xml", "feed.xml", "podcast.xml", "index.xml", "?format=rss"};

    interface Assets { InputStream open(String name) throws IOException; }

    static final class Reply {
        final int status; final String type; final byte[] body; final Map<String, String> headers = new LinkedHashMap<>();
        Reply(int status, String type, byte[] body) { this.status = status; this.type = type; this.body = body; }
        static Reply json(String json) { return new Reply(200, "application/json", json.getBytes(StandardCharsets.UTF_8)); }
        static Reply error(int status, String message) { return new Reply(status, "application/json", ("{\"error\":" + q(message) + "}").getBytes(StandardCharsets.UTF_8)); }
    }

    static final class ApiError extends RuntimeException {
        final int status;
        ApiError(int status, String message) { super(message); this.status = status; }
    }

    static final class Fetched { byte[] data; String type = ""; String finalUrl = ""; }

    private final Assets assets;
    private final String appleBase;
    private final Object appleLock = new Object();
    private long appleLast = 0;
    private final Map<String, byte[]> appleCache = new LinkedHashMap<String, byte[]>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, byte[]> e) { return size() > 200; }
    };

    Api(Assets assets, String appleBase) {
        this.assets = assets;
        this.appleBase = appleBase.replaceAll("/+$", "");
    }

    // ---------------------------------------------------------------- routing
    Reply handle(String path, String rawQuery, String method) {
        Map<String, String> q = query(rawQuery);
        try {
            if ("POST".equalsIgnoreCase(method)) {
                if (path.equals("/api/captions/clear")) return Reply.json("{\"removed\":0}");
                return Reply.error(403, "That isn’t available on phones.");
            }
            switch (path) {
                case "/": case "/index.html": return asset("index.html", "text/html; charset=utf-8");
                case "/icon.png": return asset("icon.png", "image/png");
                case "/manifest.webmanifest": return new Reply(200, "application/manifest+json", "{\"name\":\"Afterhours\",\"display\":\"standalone\",\"start_url\":\"/\"}".getBytes(StandardCharsets.UTF_8));
                case "/api/ping": return Reply.json("{\"app\":\"afterhours\",\"name\":\"Afterhours\",\"version\":" + q(VERSION) + ",\"platform\":\"android\",\"client\":\"android\"}");
                case "/api/fetch": {
                    Fetched f = fetch(q.get("url"), 25 << 20, false, 25000);
                    Reply r = new Reply(200, f.type.isEmpty() ? "application/octet-stream" : f.type, f.data);
                    r.headers.put("X-Final-URL", f.finalUrl);
                    return r;
                }
                case "/api/feedinfo": return Reply.json(feedInfo(q.get("url")));
                case "/api/image": return image(q.get("url"));
                case "/api/search": {
                    String term = q.getOrDefault("term", "").trim();
                    if (term.isEmpty()) return Reply.error(400, "Type something to search for.");
                    if (term.length() > 120) term = term.substring(0, 120);
                    int limit = clamp(parseInt(q.get("limit"), 20), 1, 50);
                    return new Reply(200, "application/json", apple("/search", "media=podcast&entity=podcast&term=" + enc(term) + "&country=" + country(q) + "&limit=" + limit));
                }
                case "/api/lookup": {
                    String ids = q.getOrDefault("id", "").replaceAll("[^0-9,]", "");
                    if (ids.isEmpty()) return Reply.error(400, "No podcast IDs given.");
                    return new Reply(200, "application/json", apple("/lookup", "id=" + ids + "&country=" + country(q)));
                }
                case "/api/crawl": return Reply.json(crawl(q.get("url"), q.getOrDefault("title", "")));
                case "/api/captions":
                    return Reply.json("{\"available\":false,\"engine\":\"\",\"models\":[],\"sizes\":{},\"versions\":{},\"install\":\"Auto captions need Afterhours on a computer.\"}");
                case "/api/prefs": return Reply.json("{\"window\":false,\"browser\":\"\",\"native\":false}");
                case "/api/phone": return Reply.json("{\"on\":false,\"url\":\"\",\"address\":\"\"}");
                default:
                    if (path.startsWith("/api/captions/")) return Reply.error(501, "Auto captions aren’t available on phones yet.");
                    return Reply.error(404, "Not found.");
            }
        } catch (ApiError e) {
            return Reply.error(e.status, e.getMessage());
        } catch (Exception e) {
            return Reply.error(502, "Couldn’t load that address (" + e.getClass().getSimpleName() + ").");
        }
    }

    private Reply asset(String name, String type) throws IOException {
        try (InputStream in = assets.open(name)) { return new Reply(200, type, readAll(in, 32 << 20, false)); }
    }

    // ---------------------------------------------------------------- network
    Fetched fetch(String url, int max, boolean partial, int timeout) {
        if (url == null || !(url.startsWith("http://") || url.startsWith("https://"))) throw new ApiError(400, "That isn't a web address Afterhours can open.");
        try {
            return open(url, max, partial, timeout, false);
        } catch (HttpStatus e) {
            if (!RETRY.contains(e.code)) throw new ApiError(502, "The site answered with an error (" + e.code + ").");
            try { return open(url, max, partial, timeout, true); }
            catch (HttpStatus e2) { throw new ApiError(502, "The site answered with an error (" + e2.code + ")."); }
            catch (IOException e2) { throw network(e2); }
        } catch (IOException e) {
            throw network(e);
        }
    }

    static final class HttpStatus extends IOException { final int code; HttpStatus(int code) { super("HTTP " + code); this.code = code; } }

    private static ApiError network(IOException e) {
        if (e instanceof java.net.SocketTimeoutException) return new ApiError(504, "The site took too long to answer.");
        if (e instanceof ApiIo) return new ApiError(((ApiIo) e).status, e.getMessage());
        return new ApiError(502, "Couldn't reach the site (" + e.getClass().getSimpleName() + ").");
    }

    static final class ApiIo extends IOException { final int status; ApiIo(int s, String m) { super(m); status = s; } }

    private Fetched open(String url, int max, boolean partial, int timeout, boolean browser) throws IOException {
        String current = url;
        for (int hop = 0; hop < 8; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(current).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(timeout);
            c.setReadTimeout(timeout);
            c.setRequestProperty("User-Agent", browser ? BROWSER_UA : APP_UA);
            c.setRequestProperty("Accept", ACCEPT);
            c.setRequestProperty("Accept-Encoding", "gzip");
            if (browser) c.setRequestProperty("Accept-Language", "en-GB,en;q=0.9,ja;q=0.8,zh;q=0.7");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400 && c.getHeaderField("Location") != null) {
                current = new URL(new URL(current), c.getHeaderField("Location")).toString();
                c.disconnect();
                continue;
            }
            if (code >= 400) { c.disconnect(); throw new HttpStatus(code); }
            InputStream in = c.getInputStream();
            if ("gzip".equalsIgnoreCase(c.getContentEncoding())) in = new GZIPInputStream(in);
            Fetched f = new Fetched();
            try { f.data = readAll(in, max, partial); } finally { in.close(); c.disconnect(); }
            f.type = c.getContentType() == null ? "" : c.getContentType();
            f.finalUrl = current;
            return f;
        }
        throw new ApiIo(502, "The site redirected too many times.");
    }

    static byte[] readAll(InputStream in, int max, boolean partial) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
            if (out.size() > max) {
                if (partial) return Arrays.copyOf(out.toByteArray(), max);
                throw new ApiIo(413, "That file is too large to load.");
            }
        }
        return out.toByteArray();
    }

    static String decode(Fetched f) {
        String head = new String(f.data, 0, Math.min(f.data.length, 300), StandardCharsets.ISO_8859_1);
        Matcher m = Pattern.compile("<\\?xml[^>]*encoding=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE).matcher(head);
        String enc = m.find() ? m.group(1) : null;
        if (enc == null) { Matcher h = Pattern.compile("charset=[\"']?([^;\"']+)", Pattern.CASE_INSENSITIVE).matcher(f.type); if (h.find()) enc = h.group(1).trim(); }
        try { return new String(f.data, enc == null ? StandardCharsets.UTF_8 : Charset.forName(enc)); }
        catch (Exception e) { return new String(f.data, StandardCharsets.UTF_8); }
    }

    // Apple's directory allows about 20 requests a minute: requests are paced and cached.
    private byte[] apple(String path, String query) {
        String url = appleBase + path + "?" + query;
        synchronized (appleLock) {
            byte[] hit = appleCache.get(url);
            if (hit != null) return hit;
            long wait = 3100 - (System.currentTimeMillis() - appleLast);
            if (wait > 0 && appleBase.startsWith("https://itunes.apple.com")) {
                try { Thread.sleep(wait); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
            try {
                byte[] data = fetch(url, 4 << 20, false, 20000).data;
                appleCache.put(url, data);
                return data;
            } catch (ApiError e) {
                if (e.getMessage().contains("(403)") || e.getMessage().contains("(429)"))
                    throw new ApiError(429, "Apple’s podcast directory asked Afterhours to slow down. Try again in a minute.");
                throw e;
            } finally {
                appleLast = System.currentTimeMillis();
            }
        }
    }

    private Reply image(String url) {
        Fetched f = fetch(url, 8 << 20, false, 20000);
        String type = f.type.toLowerCase(Locale.ROOT);
        if (!type.startsWith("image/")) {
            byte[] d = f.data;
            if (d.length > 3 && (d[0] & 0xff) == 0x89 && d[1] == 'P') type = "image/png";
            else if (d.length > 3 && (d[0] & 0xff) == 0xff && (d[1] & 0xff) == 0xd8) type = "image/jpeg";
            else if (d.length > 11 && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') type = "image/webp";
            else throw new ApiError(415, "That address isn't an image.");
        }
        Reply r = new Reply(200, type, f.data);
        r.headers.put("Cache-Control", "private, max-age=604800");
        return r;
    }

    // ---------------------------------------------------------------- feeds and websites
    String feedInfo(String url) {
        Fetched f = fetch(url, 768 << 10, true, 20000);
        String text = decode(f);
        int cut = indexOf(text, "<item[\\s>]");
        String head = cut >= 0 ? text.substring(0, cut) : text;
        String noImage = head.replaceAll("(?is)<image\\b.*?</image>", "");
        String image = first("<itunes:image\\b[^>]*\\bhref\\s*=\\s*[\"']([^\"']+)[\"']", head);
        if (image.isEmpty()) image = first("(?s)<image\\b[^>]*>.*?<url[^>]*>(.*?)</url>", head);
        return "{\"title\":" + q(first("(?s)<title[^>]*>(.*?)</title>", noImage)) + ",\"author\":" + q(first("(?s)<itunes:author[^>]*>(.*?)</itunes:author>", head))
                + ",\"image\":" + q(image) + ",\"link\":" + q(first("(?s)<link[^>]*>(.*?)</link>", noImage)) + ",\"finalUrl\":" + q(f.finalUrl) + "}";
    }

    String crawl(String url, String hint) {
        Fetched f = fetch(url, 4 << 20, true, 15000);
        String text = decode(f), base = f.finalUrl;
        if (looksLikeRss(text)) {
            if (!ENCLOSURE.matcher(text).find()) throw new ApiError(422, "That feed has no audio episodes.");
            return result("rss", base, base, "{}", "[]");
        }
        Map<String, String> meta = metas(text);
        String pageTitle = clean(meta.getOrDefault("og:title", first("(?s)<title[^>]*>(.*?)</title>", text)));
        String siteTitle = clean(meta.getOrDefault("og:site_name", pageTitle));
        String image = meta.containsKey("og:image") ? resolve(base, meta.get("og:image")) : "";
        String show = "{\"title\":" + q(siteTitle) + ",\"image\":" + q(image) + ",\"description\":" + q(clean(meta.getOrDefault("og:description", meta.getOrDefault("description", "")))) + ",\"link\":" + q(base) + "}";
        List<String[]> anchors = anchors(text, base);
        List<String> explicit = new ArrayList<>();
        Matcher lm = Pattern.compile("(?i)<link\\b[^>]*>").matcher(text);
        while (lm.find()) {
            Map<String, String> a = attrs(lm.group());
            String rel = a.getOrDefault("rel", "").toLowerCase(Locale.ROOT), type = a.getOrDefault("type", "").toLowerCase(Locale.ROOT);
            if (rel.contains("alternate") && (type.contains("rss") || type.contains("atom")) && a.containsKey("href")) explicit.add(resolve(base, a.get("href")));
        }
        for (String[] an : anchors) if (Pattern.compile("(?i)(/feed\\b|/rss\\b|rss\\.xml|feed\\.xml|podcast\\.xml|format=rss)").matcher(an[0]).find()) explicit.add(an[0]);
        Matcher am = Pattern.compile("podcasts\\.apple\\.com/[^\"'\\s<>]*?/id(\\d{5,12})").matcher(text);
        if (am.find()) {
            try {
                Matcher fm = Pattern.compile("\"feedUrl\"\\s*:\\s*\"([^\"]+)\"").matcher(new String(apple("/lookup", "id=" + am.group(1) + "&country=gb"), StandardCharsets.UTF_8));
                if (fm.find()) explicit.add(fm.group(1).replace("\\/", "/"));
            } catch (ApiError ignored) { }
        }
        String root = rootOf(base), page = base.split("[?#]")[0];
        if (!page.endsWith("/")) page += "/";
        List<String> guesses = new ArrayList<>();
        for (String g : GUESSES) { guesses.add(resolve(page, g)); guesses.add(resolve(root, g)); }
        String feed = firstFeed(explicit, null);
        if (feed.isEmpty()) feed = firstFeed(guesses, new String[]{hint, siteTitle, pageTitle});
        if (!feed.isEmpty()) return result("rss", feed, base, show, "[]");

        List<String> eps = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        pageEpisodes(text, base, pageTitle, image, eps, seen, false);
        if (eps.size() < 3) {
            ExecutorService pool = Executors.newFixedThreadPool(4);
            List<Future<String>> jobs = new ArrayList<>();
            for (String link : episodeLinks(anchors, base)) jobs.add(pool.submit(() -> episodePage(link)));
            for (Future<String> job : jobs) {
                try {
                    String one = job.get(15, TimeUnit.SECONDS);
                    if (one != null) {
                        String u = one.substring(0, one.indexOf('\n'));
                        if (seen.add(u)) eps.add(one.substring(one.indexOf('\n') + 1));
                    }
                } catch (Exception ignored) { }
            }
            pool.shutdownNow();
        }
        if (eps.isEmpty()) throw new ApiError(404, "Afterhours couldn’t find a feed or any episodes on that page.");
        return result("web", "", base, show, "[" + String.join(",", eps.subList(0, Math.min(200, eps.size()))) + "]");
    }

    private void pageEpisodes(String text, String base, String pageTitle, String image, List<String> out, Set<String> seen, boolean single) {
        Map<String, String> meta = metas(text);
        long date = toMillis(meta.getOrDefault("article:published_time", first("<time[^>]*datetime=[\"']([^\"']+)[\"']", text)));
        String desc = clean(meta.getOrDefault("og:description", meta.getOrDefault("description", "")));
        List<String[]> found = new ArrayList<>();
        for (String k : new String[]{"og:audio", "og:audio:url", "og:audio:secure_url", "twitter:player:stream"})
            if (meta.containsKey(k)) found.add(new String[]{resolve(base, meta.get(k)), pageTitle});
        Matcher sm = Pattern.compile("(?i)<(?:audio|source)\\b[^>]*\\bsrc\\s*=\\s*[\"']([^\"']+)[\"']").matcher(text);
        while (sm.find()) found.add(new String[]{resolve(base, sm.group(1)), pageTitle});
        for (String[] a : anchors(text, base)) if (AUDIO.matcher(a[0]).matches()) found.add(new String[]{a[0], a[1].isEmpty() ? pageTitle : a[1]});
        String flat = text.replace("\\/", "/");
        Matcher jm = AUDIO.matcher(flat);
        while (jm.find() && found.size() < 120) {   // audio inside a page's embedded data: use the nearest title before it
            String before = flat.substring(Math.max(0, jm.start() - 800), jm.start());
            Matcher tm = Pattern.compile("\"(?:title|name)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(before);
            String title = "";
            while (tm.find()) title = tm.group(1);
            found.add(new String[]{jm.group(), title.isEmpty() ? pageTitle : unescapeJson(title)});
        }
        for (String[] e : found) {
            if (single && !out.isEmpty()) break;
            if (!seen.add(e[0])) continue;
            out.add("{\"url\":" + q(e[0]) + ",\"title\":" + q(clean(e[1])) + ",\"date\":" + date + ",\"duration\":0,\"image\":" + q(image) + ",\"desc\":" + q(desc) + ",\"guid\":\"\"}");
        }
    }

    private String episodePage(String link) {
        try {
            Fetched f = fetch(link, 2 << 20, true, 12000);
            String text = decode(f);
            Map<String, String> meta = metas(text);
            String title = clean(meta.getOrDefault("og:title", first("(?s)<title[^>]*>(.*?)</title>", text)));
            String image = meta.containsKey("og:image") ? resolve(f.finalUrl, meta.get("og:image")) : "";
            List<String> one = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            pageEpisodes(text, f.finalUrl, title, image, one, seen, true);
            if (one.isEmpty()) return null;
            return seen.iterator().next() + "\n" + one.get(0);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String firstFeed(List<String> candidates, String[] wants) {
        Set<String> tried = new LinkedHashSet<>();
        for (String c : candidates) {
            if (!c.startsWith("http") || !tried.add(c) || tried.size() > 14) continue;
            try {
                Fetched f = fetch(c, 2 << 20, true, 10000);
                String t = decode(f);
                if (!looksLikeRss(t) || !ENCLOSURE.matcher(t).find()) continue;
                if (wants != null) {
                    String title = first("(?s)<title[^>]*>(.*?)</title>", t.replaceAll("(?is)<image\\b.*?</image>", ""));
                    boolean ok = false;
                    for (String w : wants) if (w != null && !w.isEmpty() && related(title, w)) ok = true;
                    if (!ok) continue;
                }
                return f.finalUrl;
            } catch (ApiError ignored) { }
        }
        return "";
    }

    private static List<String> episodeLinks(List<String[]> anchors, String base) {
        String host = hostOf(base), here = base.split("#")[0].replaceAll("/$", "");
        List<String> out = new ArrayList<>();
        for (String[] a : anchors) {
            String h = hostOf(a[0]);
            if (!(h.equals(host) || h.endsWith("." + host))) continue;
            String clean = a[0].split("#")[0];
            String path = clean.replaceFirst("^https?://[^/]+", "");
            if (clean.replaceAll("/$", "").equals(here) || out.contains(clean) || SKIP_LINK.matcher(path).find() || !EPISODE_HINT.matcher(path).find()) continue;
            out.add(clean);
            if (out.size() >= 12) break;
        }
        return out;
    }

    // ---------------------------------------------------------------- small helpers
    static boolean looksLikeRss(String text) {
        String head = text.substring(0, Math.min(text.length(), 5000)).toLowerCase(Locale.ROOT);
        return head.contains("<rss") || (head.contains("<channel") && text.toLowerCase(Locale.ROOT).contains("<item"));
    }

    static boolean related(String a, String b) {
        String na = a.toLowerCase(Locale.ROOT).replaceAll("[\\W_]+", ""), nb = b.toLowerCase(Locale.ROOT).replaceAll("[\\W_]+", "");
        if (na.isEmpty() || nb.isEmpty()) return false;
        if (na.contains(nb) || nb.contains(na)) return true;
        Set<String> wa = words(a);
        wa.retainAll(words(b));
        if (!wa.isEmpty()) return true;
        int shared = 0;
        for (int i = 0; i + 1 < na.length(); i++) if (Character.UnicodeScript.of(na.codePointAt(i)) != Character.UnicodeScript.LATIN && nb.contains(na.substring(i, i + 2))) shared++;
        return shared >= 2;
    }

    private static Set<String> words(String s) {
        Set<String> out = new LinkedHashSet<>();
        for (String w : s.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
            if (w.length() >= 3 && !Arrays.asList("the", "and", "with", "from", "podcast", "podcasts", "show", "radio", "official", "episodes", "episode", "home", "feed", "rss").contains(w)) out.add(w);
        return out;
    }

    private static Map<String, String> metas(String text) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = Pattern.compile("(?i)<meta\\b[^>]*>").matcher(text);
        while (m.find()) {
            Map<String, String> a = attrs(m.group());
            String key = a.getOrDefault("property", a.getOrDefault("name", a.getOrDefault("itemprop", ""))).toLowerCase(Locale.ROOT);
            if (!key.isEmpty() && a.containsKey("content") && !out.containsKey(key)) out.put(key, a.get("content").trim());
        }
        return out;
    }

    private static List<String[]> anchors(String text, String base) {
        List<String[]> out = new ArrayList<>();
        Matcher m = Pattern.compile("(?is)<a\\b([^>]*)>(.*?)</a>").matcher(text);
        while (m.find()) {
            String href = attrs(m.group(1)).get("href");
            if (href != null && !href.startsWith("#") && !href.startsWith("javascript:") && !href.startsWith("mailto:"))
                out.add(new String[]{resolve(base, href.trim()), clean(m.group(2))});
        }
        return out;
    }

    private static Map<String, String> attrs(String tag) {
        Map<String, String> out = new LinkedHashMap<>();
        Matcher m = TAG_ATTR.matcher(tag);
        while (m.find()) {
            String v = m.group(3) != null ? m.group(3) : m.group(4) != null ? m.group(4) : m.group(5);
            out.put(m.group(1).toLowerCase(Locale.ROOT), unescapeHtml(v));
        }
        return out;
    }

    static String result(String source, String feedUrl, String pageUrl, String show, String episodes) {
        return "{\"source\":" + q(source) + ",\"feedUrl\":" + q(feedUrl) + ",\"pageUrl\":" + q(pageUrl) + ",\"show\":" + show + ",\"episodes\":" + episodes + "}";
    }

    static long toMillis(String s) {
        if (s == null || s.trim().isEmpty()) return 0;
        s = s.trim();
        try { return OffsetDateTime.parse(s.replace("Z", "+00:00")).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        try { return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        try { return LocalDate.parse(s.substring(0, 10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        return 0;
    }

    static String clean(String s) {
        if (s == null) return "";
        s = s.replaceAll("(?s)<!\\[CDATA\\[(.*?)]]>", "$1").replaceAll("<[^>]+>", " ");
        return unescapeHtml(s).replaceAll("\\s+", " ").trim();
    }

    static String unescapeHtml(String s) {
        if (s == null) return "";
        Matcher m = Pattern.compile("&(#x?[0-9a-fA-F]+|amp|lt|gt|quot|apos|nbsp|rsquo|lsquo|rdquo|ldquo|hellip|mdash|ndash);").matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String e = m.group(1), r;
            switch (e) {
                case "amp": r = "&"; break; case "lt": r = "<"; break; case "gt": r = ">"; break; case "quot": r = "\""; break;
                case "apos": r = "'"; break; case "nbsp": r = " "; break; case "rsquo": r = "’"; break; case "lsquo": r = "‘"; break;
                case "rdquo": r = "”"; break; case "ldquo": r = "“"; break; case "hellip": r = "…"; break; case "mdash": r = "—"; break; case "ndash": r = "–"; break;
                default:
                    try { r = new String(Character.toChars(e.startsWith("#x") || e.startsWith("#X") ? Integer.parseInt(e.substring(2), 16) : Integer.parseInt(e.substring(1)))); }
                    catch (Exception x) { r = m.group(); }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(r));
        }
        m.appendTail(out);
        return out.toString();
    }

    static String unescapeJson(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'u' && i + 4 < s.length()) { try { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; } catch (NumberFormatException e) { b.append(n); } }
                else b.append(n == 'n' ? '\n' : n == 't' ? '\t' : n);
            } else b.append(c);
        }
        return b.toString();
    }

    static String q(String s) {
        if (s == null) return "\"\"";
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break; case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break; case '\r': b.append("\\r"); break; case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
            }
        }
        return b.append('"').toString();
    }

    static Map<String, String> query(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null) return out;
        for (String part : raw.split("&")) {
            int eq = part.indexOf('=');
            try {
                if (eq > 0) out.put(URLDecoder.decode(part.substring(0, eq), "UTF-8"), URLDecoder.decode(part.substring(eq + 1), "UTF-8"));
            } catch (Exception ignored) { }
        }
        return out;
    }

    private static String first(String regex, String text) { Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text); return m.find() ? clean(m.group(1)) : ""; }
    private static int indexOf(String text, String regex) { Matcher m = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text); return m.find() ? m.start() : -1; }
    private static String resolve(String base, String href) { try { return new URL(new URL(base), href).toString(); } catch (Exception e) { return href; } }
    private static String rootOf(String url) { try { URL u = new URL(url); return u.getProtocol() + "://" + u.getAuthority() + "/"; } catch (Exception e) { return url; } }
    private static String hostOf(String url) { try { String h = new URL(url).getHost().toLowerCase(Locale.ROOT); return h.startsWith("www.") ? h.substring(4) : h; } catch (Exception e) { return ""; } }
    private static String enc(String s) { try { return java.net.URLEncoder.encode(s, "UTF-8"); } catch (Exception e) { return s; } }
    private static String country(Map<String, String> q) { String c = q.getOrDefault("country", "gb").toLowerCase(Locale.ROOT); return c.matches("[a-z]{2}") ? c : "gb"; }
    private static int parseInt(String s, int d) { try { return Integer.parseInt(s); } catch (Exception e) { return d; } }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
