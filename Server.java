import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dependency-free Java 8 server for the Student ID Card Generator demo. */
public class Server {
    private static final int PORT = 8080;
    private static final int MAX_BODY = 32768;
    private static final Path ROOT = Paths.get(".").toAbsolutePath().normalize();
    private static final Path DATA_DIR = ROOT.resolve("data");
    private static final Path STORE = DATA_DIR.resolve("students.jsonl");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(DATA_DIR);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/api/health", new ApiHandler());
        server.createContext("/api/students", new ApiHandler());
        server.createContext("/", new StaticHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Student ID app running at http://localhost:" + PORT);
        System.out.println("Student records are stored locally in " + STORE);
    }

    private static class ApiHandler implements HttpHandler {
        public void handle(HttpExchange x) throws IOException {
            String path = x.getRequestURI().getPath();
            try {
                if ("/api/health".equals(path) && "GET".equals(x.getRequestMethod())) {
                    send(x, 200, "application/json; charset=utf-8", "{\"status\":\"ok\"}");
                    return;
                }
                if ("/api/students".equals(path) && "GET".equals(x.getRequestMethod())) {
                    send(x, 200, "application/json; charset=utf-8", readStudents());
                    return;
                }
                if ("/api/students".equals(path) && "POST".equals(x.getRequestMethod())) {
                    Map<String, String> fields = parseForm(readBody(x.getRequestBody()));
                    String[] required = {"email", "name", "studentId", "degree", "department", "year", "academicYears"};
                    for (String key : required) {
                        if (fields.get(key) == null || fields.get(key).trim().isEmpty()) {
                            send(x, 400, "application/json; charset=utf-8", "{\"error\":\"Missing required field: " + key + "\"}");
                            return;
                        }
                    }
                    if (!fields.get("email").trim().matches("(?i)^[^\\s@]+@srmap\\.edu\\.in$")) {
                        send(x, 403, "application/json; charset=utf-8", "{\"error\":\"Use a valid @srmap.edu.in email address\"}");
                        return;
                    }
                    Map<String, String> record = new LinkedHashMap<String, String>();
                    record.put("id", UUID.randomUUID().toString());
                    record.put("createdAt", Instant.now().toString());
                    for (String key : required) record.put(key, fields.get(key).trim());
                    appendStudent(record);
                    send(x, 201, "application/json; charset=utf-8", toJson(record));
                    return;
                }
                send(x, 405, "application/json; charset=utf-8", "{\"error\":\"Method not allowed\"}");
            } catch (IllegalArgumentException e) {
                send(x, 400, "application/json; charset=utf-8", "{\"error\":\"Invalid request\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(x, 500, "application/json; charset=utf-8", "{\"error\":\"Could not save or read student records\"}");
            }
        }
    }

    private static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange x) throws IOException {
            if (!"GET".equals(x.getRequestMethod())) {
                send(x, 405, "text/plain; charset=utf-8", "Method not allowed");
                return;
            }
            String requested = x.getRequestURI().getPath();
            if (requested == null || "/".equals(requested)) requested = "/index.html";
            Path file = ROOT.resolve("." + requested).normalize();
            if (!file.startsWith(ROOT) || !Files.isRegularFile(file)) {
                send(x, 404, "text/plain; charset=utf-8", "Not found");
                return;
            }
            String name = file.getFileName().toString().toLowerCase();
            String type = name.endsWith(".html") ? "text/html; charset=utf-8" :
                    name.endsWith(".css") ? "text/css; charset=utf-8" :
                    name.endsWith(".js") ? "application/javascript; charset=utf-8" :
                    name.endsWith(".jpeg") || name.endsWith(".jpg") ? "image/jpeg" : "application/octet-stream";
            byte[] bytes = Files.readAllBytes(file);
            x.getResponseHeaders().set("Content-Type", type);
            x.sendResponseHeaders(200, bytes.length);
            x.getResponseBody().write(bytes);
            x.close();
        }
    }

    private static synchronized void appendStudent(Map<String, String> record) throws IOException {
        String line = toJson(record) + "\n";
        Files.write(STORE, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static synchronized String readStudents() throws IOException {
        List<String> lines = Files.exists(STORE) ? Files.readAllLines(STORE, StandardCharsets.UTF_8) : new ArrayList<String>();
        StringBuilder out = new StringBuilder("[");
        boolean first = true;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            if (!first) out.append(',');
            out.append(line);
            first = false;
        }
        return out.append(']').toString();
    }

    private static String readBody(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int count;
        while ((count = in.read(buffer)) != -1) {
            if (out.size() + count > MAX_BODY) throw new IllegalArgumentException("Request too large");
            out.write(buffer, 0, count);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> result = new LinkedHashMap<String, String>();
        if (body.isEmpty()) return result;
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], "UTF-8");
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], "UTF-8") : "";
            result.put(key, value);
        }
        return result;
    }

    private static String toJson(Map<String, String> record) {
        StringBuilder out = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : record.entrySet()) {
            if (!first) out.append(',');
            out.append('"').append(escape(entry.getKey())).append("\":\"").append(escape(entry.getValue())).append('"');
            first = false;
        }
        return out.append('}').toString();
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') out.append("\\\"");
            else if (c == '\\') out.append("\\\\");
            else if (c == '\n') out.append("\\n");
            else if (c == '\r') out.append("\\r");
            else if (c == '\t') out.append("\\t");
            else if (c < 32) out.append(String.format("\\u%04x", (int)c));
            else out.append(c);
        }
        return out.toString();
    }

    private static void send(HttpExchange x, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        x.getResponseHeaders().set("Content-Type", contentType);
        x.getResponseHeaders().set("Cache-Control", "no-store");
        x.sendResponseHeaders(status, bytes.length);
        x.getResponseBody().write(bytes);
        x.close();
    }
}
