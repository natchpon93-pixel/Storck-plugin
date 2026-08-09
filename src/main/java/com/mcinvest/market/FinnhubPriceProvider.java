package com.mcinvest.market;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ดึงราคาจริงจาก Finnhub (https://finnhub.io)
 *
 * แพ็กฟรี: 60 ครั้ง/นาที ราคาดีเลย์ราว 20 นาที
 * เราจึงยิงทีละ symbol แล้วเว้นจังหวะ 1.1 วินาที กันโดนแบน
 *
 * คลาสนี้ทำงานบน async thread เท่านั้น ห้ามเรียกจาก main thread เด็ดขาด
 * เพราะการรอเน็ตจะทำให้เซิร์ฟค้างทั้งเซิร์ฟ
 */
public class FinnhubPriceProvider implements PriceProvider {

    private static final String ENDPOINT = "https://finnhub.io/api/v1/quote";
    private static final long DELAY_BETWEEN_CALLS_MS = 1100L;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final String token;
    private final Consumer<String> logger;

    public FinnhubPriceProvider(String token, Consumer<String> logger) {
        this.token = token;
        this.logger = logger;
    }

    @Override
    public String name() {
        return "finnhub";
    }

    @Override
    public Map<String, Quote> fetch(Collection<Asset> assets) {
        Map<String, Quote> result = new HashMap<>();
        boolean first = true;

        for (Asset asset : assets) {
            if (!first) {
                try {
                    Thread.sleep(DELAY_BETWEEN_CALLS_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            first = false;

            try {
                Quote quote = fetchOne(asset.symbol());
                if (quote != null) {
                    result.put(asset.id(), quote);
                }
            } catch (Exception e) {
                logger.accept("ดึงราคา " + asset.symbol() + " ไม่สำเร็จ: " + e.getMessage());
            }
        }
        return result;
    }

    private Quote fetchOne(String symbol) throws Exception {
        String url = ENDPOINT
                + "?symbol=" + URLEncoder.encode(symbol, StandardCharsets.UTF_8)
                + "&token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "McInvest-Plugin")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            logger.accept("โดน rate limit ของ Finnhub แล้ว ลองเพิ่มค่า refresh-minutes ใน config");
            return null;
        }
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            logger.accept("API key ไม่ถูกต้องหรือหมดสิทธิ์ (HTTP " + response.statusCode() + ")");
            return null;
        }
        if (response.statusCode() != 200) {
            logger.accept("Finnhub ตอบกลับ HTTP " + response.statusCode() + " สำหรับ " + symbol);
            return null;
        }

        String body = response.body();
        double price = readNumber(body, "c");
        double prevClose = readNumber(body, "pc");

        // ถ้า symbol ผิด Finnhub จะคืนค่า 0 ทั้งหมด
        if (price <= 0) {
            logger.accept("ไม่พบราคาของ " + symbol + " (เช็คว่าสะกด ticker ถูกไหม)");
            return null;
        }
        if (prevClose <= 0) {
            prevClose = price;
        }
        return new Quote(price, prevClose, System.currentTimeMillis());
    }

    /**
     * อ่านตัวเลขจาก JSON แบบง่าย
     * ผลลัพธ์ของ endpoint นี้เป็น object ชั้นเดียวที่มีแต่ตัวเลข
     * เลยไม่ต้องลาก library JSON เข้ามาให้ jar บวม
     */
    private static double readNumber(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
