package tsj;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Login {

    public record Creds(String cfid, String cftoken) {}

    private static final String LOGIN_URL = "https://draftmyschedule.uwo.ca/login.cfm";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36";

    public static Creds getCreds(String username, String password)
            throws Exception {

        // HttpClient with NEVER redirect policy so we can read Set-Cookie ourselves
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // ---- Step 1: GET login page to obtain CFID / CFTOKEN cookies ----
        HttpRequest step1 = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .GET()
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,"
                        + "image/avif,image/webp,image/apng,*/*;q=0.8,"
                        + "application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "en-US,en;q=0.9")
                .header("user-agent", USER_AGENT)
                .build();

        HttpResponse<String> resp1 =
                client.send(step1, HttpResponse.BodyHandlers.ofString());

        Map<String, String> cookies = parseCookies(resp1.headers().allValues("set-cookie"));
        String cfid = cookies.get("CFID");
        String cftoken = cookies.get("CFTOKEN");

        if (cfid == null || cftoken == null) {
            throw new IllegalStateException(
                    "Did not receive CFID/CFTOKEN from step 1. Got: " + cookies);
        }

        // ---- Step 2: POST credentials with the cookies attached ----
        String body = "txtUsername=" + url(username)
                + "&txtPassword=" + url(password)
                + "&command=authenticate";

        String cookieHeader = "CFID=" + cfid + "; CFTOKEN=" + cftoken;

        HttpRequest step2 = HttpRequest.newBuilder()
                .uri(URI.create(LOGIN_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,"
                        + "image/avif,image/webp,image/apng,*/*;q=0.8,"
                        + "application/signed-exchange;v=b3;q=0.7")
                .header("accept-language", "en-US,en;q=0.9")
                .header("content-type", "application/x-www-form-urlencoded")
                .header("cookie", cookieHeader)
                .header("origin", "https://example.com")
                .header("referer", LOGIN_URL)
                .header("user-agent", USER_AGENT)
                .build();

        HttpResponse<String> resp2 =
                client.send(step2, HttpResponse.BodyHandlers.ofString());

        // The session may rotate cookies on authentication; pick up any new values.
        Map<String, String> postCookies = parseCookies(resp2.headers().allValues("set-cookie"));
        cfid = postCookies.getOrDefault("CFID", cfid);
        cftoken = postCookies.getOrDefault("CFTOKEN", cftoken);

        return new Creds(cfid, cftoken);
    }

    private static Map<String, String> parseCookies(List<String> setCookieHeaders) {
        Map<String, String> cookies = new HashMap<>();
        Pattern p = Pattern.compile("^([^=]+)=([^;]*)");
        for (String header : setCookieHeaders) {
            Matcher m = p.matcher(header.trim());
            if (m.find()) {
                cookies.put(m.group(1).trim(), m.group(2).trim());
            }
        }
        return cookies;
    }

    private static String url(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}