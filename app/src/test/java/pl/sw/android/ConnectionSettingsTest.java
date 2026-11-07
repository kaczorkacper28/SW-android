package pl.sw.android;

import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/** Dependency-free checks; run this class with a JDK after compiling ConnectionSettings. */
public final class ConnectionSettingsTest {
    private static int checks;

    private ConnectionSettingsTest() {}

    public static void main(String[] args) throws Exception {
        validOrigins();
        invalidOrigins();
        accessKeys();
        endpoints();
        connectionFactory();
        System.out.println("ConnectionSettings: " + checks + " checks passed.");
    }

    private static void validOrigins() {
        normalizes("http://127.0.0.1:3000", "http://127.0.0.1:3000");
        normalizes("http://127.0.0.1:3000/", "http://127.0.0.1:3000");
        normalizes("http://localhost:3000", "http://localhost:3000");
        normalizes("HTTP://LOCALHOST:3000/", "http://localhost:3000");
        normalizes("http://localhost", "http://localhost");
        normalizes("http://127.0.0.1:1", "http://127.0.0.1:1");
        normalizes("http://localhost:65535", "http://localhost:65535");
        normalizes("https://api.example.test", "https://api.example.test");
        normalizes("https://api.example.test:8443/", "https://api.example.test:8443");
        normalizes("  https://api.example.test/  ", "https://api.example.test");
        normalizes("https://192.0.2.10", "https://192.0.2.10");
        normalizes("https://localhost:3000", "https://localhost:3000");

        ConnectionSettings defaults = new ConnectionSettings(ConnectionSettings.DEFAULT_BASE_URL, "");
        equal("http://127.0.0.1:3000", defaults.baseUrl, "default Termux origin");
        equal("", defaults.accessKey, "empty key remains empty");
    }

    private static void invalidOrigins() {
        String[] invalid = {
            "", "   ", "localhost:3000", "//localhost:3000", "/api/officers",
            "file:///tmp/backend", "ftp://api.example.test", "javascript:alert(1)",
            "http://api.example.test", "http://192.168.1.20:3000", "http://0.0.0.0:3000",
            "http://127.0.0.2:3000", "http://127.1:3000", "http://2130706433:3000",
            "http://[::1]:3000", "http://localhost.evil.test:3000", "http://localhost.:3000",
            "http://user@localhost:3000", "https://user:password@api.example.test",
            "http://localhost@api.example.test", "https://api.example.test/path",
            "https://api.example.test/api/", "https://api.example.test//",
            "https://api.example.test?key=value", "https://api.example.test?",
            "https://api.example.test#fragment", "https://api.example.test#",
            "http://localhost:0", "http://localhost:65536", "http://localhost:-1",
            "http://localhost:not-a-port", "http://localhost:", "https://", "https://api example.test",
            "http://localhost\\@api.example.test", "http://localhost:3000\n.evil.test"
        };
        for (String value : invalid) {
            rejects(() -> ConnectionSettings.normalizeBaseUrl(value), "origin " + printable(value));
        }
    }

    private static void accessKeys() {
        String password = "  this is a password żółw 🔑  ";
        ConnectionSettings settings = new ConnectionSettings("http://localhost:3000/", password);
        equal("http://localhost:3000", settings.baseUrl, "constructor normalizes origin");
        equal(password, settings.accessKey, "constructor preserves password spaces and Unicode");
        equal(encoded(password), settings.authorizationHeader(), "Unicode password is UTF-8 base64");
        equal("123456789012", new ConnectionSettings("http://localhost:3000", "123456789012").accessKey, "minimum password length");
        String maximum = repeated('x', 1024);
        equal(maximum, new ConnectionSettings("http://localhost:3000", maximum).accessKey, "maximum password length");
        ConnectionSettings empty = new ConnectionSettings("http://localhost:3000", "");
        equal("", empty.accessKey, "empty password is allowed until configured");
        equal("", empty.authorizationHeader(), "empty password has no authorization header");
        for (String value : new String[] { "123456789012", "1234567890123", "12345678901234", "🔑🔑🔑🔑🔑🔑", maximum }) {
            equal(encoded(value), new ConnectionSettings("http://localhost:3000", value).authorizationHeader(), "base64 padding and Unicode");
        }

        String[] invalid = {
            "12345678901", repeated('x', 1025), "long-password\rpart", "long-password\npart",
            "long-password\tpart", "long-password\u0000part", "long-password\u007fpart",
            "long-password\u0085part", "long-password\uD800part", "long-password\uDC00part"
        };
        for (String key : invalid) {
            rejects(() -> new ConnectionSettings("http://localhost:3000", key), "invalid password length, control, or surrogate");
        }
    }

    private static void endpoints() throws Exception {
        ConnectionSettings local = new ConnectionSettings("http://127.0.0.1:3000/", "test-password");
        equal("http://127.0.0.1:3000/api/officers", local.endpoint("/api/officers").toExternalForm(), "local endpoint");

        ConnectionSettings remote = new ConnectionSettings("https://api.example.test:8443", "test-password");
        String path = "/api/discord/messages?channelId=123%2F456&limit=50";
        URL endpoint = remote.endpoint(path);
        equal("https", endpoint.getProtocol(), "endpoint protocol");
        equal("api.example.test", endpoint.getHost(), "endpoint host");
        equal(8443, endpoint.getPort(), "endpoint port");
        equal("/api/discord/messages", endpoint.getPath(), "endpoint path");
        equal("channelId=123%2F456&limit=50", endpoint.getQuery(), "endpoint query encoding");
        equal(null, endpoint.getUserInfo(), "endpoint has no userinfo");
        equal(null, endpoint.getRef(), "endpoint has no fragment");
        equal("https://api.example.test:8443" + path, endpoint.toExternalForm(), "configured origin and query preserved");

        String[] invalid = {
            "", "api/officers", "/api", "/health", "//other.example.test/api/officers",
            "https://other.example.test/api/officers", "/api/../outside", "/api/./officers",
            "/api/%2e%2e/outside", "/api/%2E%2E/outside", "/api/%2foutside", "/api/%2Foutside",
            "/api/officers#fragment", "/api/\\other.example.test/officers", "/api/officers\n"
        };
        for (String value : invalid) {
            rejects(() -> remote.endpoint(value), "endpoint " + printable(value));
        }
    }

    private static void connectionFactory() throws Exception {
        String password = " connection password żółw 🔑 ";
        ConnectionSettings settings = new ConnectionSettings("http://127.0.0.1:3000", password);
        for (String method : new String[] { "GET", "POST" }) {
            HttpURLConnection connection = settings.open("/api/officers?limit=2", method);
            try {
                equal("http://127.0.0.1:3000/api/officers?limit=2", connection.getURL().toExternalForm(), "factory URL");
                equal(method, connection.getRequestMethod(), "factory HTTP method");
                equal(encoded(password), connection.getRequestProperty("X-SW-Key"), "factory password header");
                equal("application/json", connection.getRequestProperty("Accept"), "factory Accept header");
                equal(false, connection.getInstanceFollowRedirects(), "factory disables redirects");
                check(connection.getConnectTimeout() > 0, "factory has connect timeout");
                check(connection.getReadTimeout() > 0, "factory has read timeout");
            } finally {
                connection.disconnect();
            }
        }

        HttpURLConnection noPassword = new ConnectionSettings("http://localhost:3000", "").open("/api/officers", "GET");
        try {
            equal(null, noPassword.getRequestProperty("X-SW-Key"), "factory omits unconfigured password");
        } finally {
            noPassword.disconnect();
        }
    }

    private static String encoded(String value) {
        return "base64:" + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String repeated(char value, int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, value);
        return new String(chars);
    }

    private static void normalizes(String input, String expected) {
        equal(expected, ConnectionSettings.normalizeBaseUrl(input), "normalize " + printable(input));
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }

    private static void rejects(ThrowingAction action, String label) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException | URISyntaxException expected) {
            return;
        } catch (Exception unexpected) {
            throw new AssertionError(label + ": expected input validation failure", unexpected);
        }
        throw new AssertionError(label + ": should be rejected");
    }

    private static String printable(String value) {
        return value.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private interface ThrowingAction {
        void run() throws Exception;
    }
}
