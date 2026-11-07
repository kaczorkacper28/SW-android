package pl.sw.android;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** One immutable destination/password pair for a queued request. */
public final class ConnectionSettings {
    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:3000";
    public final String baseUrl;
    public final String accessKey;

    public ConnectionSettings(String address, String password) {
        baseUrl = normalizeBaseUrl(address);
        accessKey = password == null ? "" : password;
        if ((!accessKey.isEmpty() && accessKey.length() < 12) || accessKey.length() > 1024) {
            throw new IllegalArgumentException("Hasło połączenia musi mieć od 12 do 1024 znaków.");
        }
        for (int i = 0; i < accessKey.length(); i++) {
            char c = accessKey.charAt(i);
            if (Character.isISOControl(c)) throw new IllegalArgumentException("Hasło połączenia nie może zawierać znaków sterujących.");
        }
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(accessKey)) {
            throw new IllegalArgumentException("Hasło zawiera nieprawidłowe znaki.");
        }
    }

    public static String normalizeBaseUrl(String address) {
        try {
            URI uri = new URI(address == null ? "" : address.trim());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("http") && !scheme.equals("https")) || host.isEmpty()) {
                throw new IllegalArgumentException("Podaj adres backendu zaczynający się od http:// lub https://.");
            }
            if (uri.getUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null ||
                    (uri.getRawPath() != null && !uri.getRawPath().isEmpty() && !uri.getRawPath().equals("/"))) {
                throw new IllegalArgumentException("Podaj sam adres serwera, bez /api, hasła, parametrów ani fragmentu.");
            }
            if (uri.getPort() == 0 || uri.getPort() > 65535 || uri.getPort() < -1 || uri.getRawAuthority().endsWith(":")) {
                throw new IllegalArgumentException("Port serwera musi mieć wartość od 1 do 65535.");
            }
            if (scheme.equals("http") && !host.equals("127.0.0.1") && !host.equals("localhost")) {
                throw new IllegalArgumentException("HTTP jest dostępne tylko lokalnie: 127.0.0.1 lub localhost. Dla innego serwera użyj HTTPS.");
            }
            return new URI(scheme, null, host, uri.getPort(), null, null, null).toASCIIString();
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("Nieprawidłowy adres backendu.");
        }
    }

    public URL endpoint(String route) throws Exception {
        URI uri = new URI(route);
        if (!route.startsWith("/api/") || uri.isAbsolute() || uri.getRawAuthority() != null ||
                uri.getRawFragment() != null || route.indexOf('\\') >= 0 || !uri.normalize().equals(uri) ||
                uri.getRawPath().toLowerCase(Locale.ROOT).contains("%2e") || uri.getRawPath().toLowerCase(Locale.ROOT).contains("%2f")) {
            throw new IllegalArgumentException("Nieprawidłowa ścieżka żądania API.");
        }
        return new URL(baseUrl + route);
    }

    public HttpURLConnection open(String route, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) endpoint(route).openConnection();
        connection.setRequestMethod(method);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(25000);
        connection.setRequestProperty("Accept", "application/json");
        if (!accessKey.isEmpty()) connection.setRequestProperty("X-SW-Key", authorizationHeader());
        return connection;
    }

    // Equivalent to Base64.NO_WRAP, kept independent of Android for JVM tests
    // and compatible with API 24 (java.util.Base64 requires Android API 26).
    public String authorizationHeader() {
        if (accessKey.isEmpty()) return "";
        final String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        byte[] bytes = accessKey.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder("base64:");
        for (int i = 0; i < bytes.length; i += 3) {
            int a = bytes[i] & 255;
            int b = i + 1 < bytes.length ? bytes[i + 1] & 255 : 0;
            int c = i + 2 < bytes.length ? bytes[i + 2] & 255 : 0;
            encoded.append(alphabet.charAt(a >>> 2));
            encoded.append(alphabet.charAt(((a & 3) << 4) | (b >>> 4)));
            encoded.append(i + 1 < bytes.length ? alphabet.charAt(((b & 15) << 2) | (c >>> 6)) : '=');
            encoded.append(i + 2 < bytes.length ? alphabet.charAt(c & 63) : '=');
        }
        return encoded.toString();
    }
}
