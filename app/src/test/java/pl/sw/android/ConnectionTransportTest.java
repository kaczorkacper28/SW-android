package pl.sw.android;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Real loopback transport checks, run explicitly with a JDK; no backend or Discord calls. */
public final class ConnectionTransportTest {
    private static int checks;

    private ConnectionTransportTest() {}

    public static void main(String[] args) throws Exception {
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer destination = null;
        try {
            destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            runChecks(origin, destination);
            System.out.println("ConnectionTransport: " + checks + " checks passed.");
        } finally {
            origin.stop(0);
            if (destination != null) destination.stop(0);
        }
    }

    private static void runChecks(HttpServer origin, HttpServer destination) throws Exception {
        AtomicInteger originRequests = new AtomicInteger();
        AtomicInteger destinationRequests = new AtomicInteger();
        AtomicReference<String> getMethod = new AtomicReference<>();
        AtomicReference<String> getHeader = new AtomicReference<>();
        AtomicReference<String> postMethod = new AtomicReference<>();
        AtomicReference<String> postHeader = new AtomicReference<>();
        AtomicReference<String> postBody = new AtomicReference<>();
        AtomicReference<String> redirectHeader = new AtomicReference<>();
        AtomicReference<String> leakedHeader = new AtomicReference<>();

        destination.createContext("/", exchange -> {
            destinationRequests.incrementAndGet();
            leakedHeader.set(exchange.getRequestHeaders().getFirst("X-SW-Key"));
            reply(exchange, 200, "unexpected redirect destination");
        });
        String redirectLocation = "http://127.0.0.1:" + destination.getAddress().getPort() + "/api/destination";
        origin.createContext("/api/access", exchange -> {
            originRequests.incrementAndGet();
            getMethod.set(exchange.getRequestMethod());
            getHeader.set(exchange.getRequestHeaders().getFirst("X-SW-Key"));
            reply(exchange, 200, "{\"authenticated\":true}");
        });
        origin.createContext("/api/echo", exchange -> {
            originRequests.incrementAndGet();
            postMethod.set(exchange.getRequestMethod());
            postHeader.set(exchange.getRequestHeaders().getFirst("X-SW-Key"));
            String received;
            try (InputStream requestBody = exchange.getRequestBody()) {
                received = readUtf8(requestBody);
            }
            postBody.set(received);
            reply(exchange, 200, received);
        });
        origin.createContext("/api/redirect", exchange -> {
            originRequests.incrementAndGet();
            redirectHeader.set(exchange.getRequestHeaders().getFirst("X-SW-Key"));
            exchange.getResponseHeaders().set("Location", redirectLocation);
            try {
                exchange.sendResponseHeaders(302, -1);
            } finally {
                exchange.close();
            }
        });
        origin.start();
        destination.start();

        String password = "  testowe hasło połączenia żółw 🔑  ";
        String expectedHeader = "base64:" + Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        ConnectionSettings settings = new ConnectionSettings("http://127.0.0.1:" + origin.getAddress().getPort(), password);

        HttpURLConnection get = settings.open("/api/access", "GET");
        try {
            equal(200, get.getResponseCode(), "GET status");
            try (InputStream responseBody = get.getInputStream()) {
                equal("{\"authenticated\":true}", readUtf8(responseBody), "GET response");
            }
            equal("GET", getMethod.get(), "server receives GET");
            equal(expectedHeader, getHeader.get(), "server receives UTF-8 password header on GET");
        } finally {
            get.disconnect();
        }

        String payload = "{\"content\":\"Zażółć gęślą jaźń 🔑\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection post = settings.open("/api/echo", "POST");
        post.setDoOutput(true);
        post.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        post.setFixedLengthStreamingMode(payloadBytes.length);
        try {
            try (OutputStream requestBody = post.getOutputStream()) {
                requestBody.write(payloadBytes);
            }
            equal(200, post.getResponseCode(), "POST status");
            try (InputStream responseBody = post.getInputStream()) {
                equal(payload, readUtf8(responseBody), "POST echo response");
            }
            equal("POST", postMethod.get(), "server receives POST");
            equal(expectedHeader, postHeader.get(), "server receives UTF-8 password header on POST");
            equal(payload, postBody.get(), "server receives UTF-8 POST body");
        } finally {
            post.disconnect();
        }

        HttpURLConnection redirect = settings.open("/api/redirect", "GET");
        try {
            equal(false, redirect.getInstanceFollowRedirects(), "redirect following is disabled");
            equal(302, redirect.getResponseCode(), "302 response is returned to caller");
            equal(redirectLocation, redirect.getHeaderField("Location"), "redirect target received unchanged");
            equal(expectedHeader, redirectHeader.get(), "origin receives password for redirect request");
            equal(3, originRequests.get(), "only three expected origin requests occurred");
            equal(0, destinationRequests.get(), "redirect destination receives no request");
            equal(null, leakedHeader.get(), "password is not sent to redirect destination");
        } finally {
            redirect.disconnect();
        }
    }

    private static void reply(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        try {
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    private static String readUtf8(InputStream input) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void equal(Object expected, Object actual, String label) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + ": unexpected value");
        }
    }
}
