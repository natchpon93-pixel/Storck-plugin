package com.mcinvest.market;

import com.mcinvest.McInvest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * หัวใจของฝั่งราคา
 * - โหลดรายการสินทรัพย์จาก config
 * - ดึงราคาแบบ async ตามรอบเวลา แล้วเก็บลง cache
 * - เมนูทุกหน้าอ่านจาก cache ตัวนี้ ไม่ยิง API เองเด็ดขาด
 */
public class MarketManager {

    /** ตลาดหุ้นสหรัฐ ใช้โซนเวลานี้ ระบบจะคิด DST ให้อัตโนมัติ */
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_OPEN = LocalTime.of(9, 30);
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);

    private final McInvest plugin;
    private final Map<String, Asset> assets = new LinkedHashMap<>();
    private final Map<String, Quote> quotes = new ConcurrentHashMap<>();

    private PriceProvider provider;
    private BukkitTask refreshTask;
    private volatile long lastUpdate = 0L;
    private volatile boolean refreshing = false;

    public MarketManager(McInvest plugin) {
        this.plugin = plugin;
    }

    /** โหลด config ใหม่ทั้งหมด เรียกได้ทั้งตอน enable และตอน /invest reload */
    public void reload() {
        stop();
        assets.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("assets");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                String symbol = s.getString("symbol", id.toUpperCase());
                String display = s.getString("display", "&f" + symbol);
                double base = s.getDouble("base", 100.0);

                Material icon = Material.matchMaterial(s.getString("icon", "PAPER"));
                if (icon == null) {
                    plugin.getLogger().warning("ไอคอนของ " + id + " ไม่ถูกต้อง ใช้ PAPER แทน");
                    icon = Material.PAPER;
                }
                assets.put(id, new Asset(id, symbol, display, icon, base));
            }
        }

        String mode = plugin.getConfig().getString("market.provider", "mock").toLowerCase();
        String token = plugin.getConfig().getString("market.finnhub-token", "");

        if ("finnhub".equals(mode)) {
            if (token == null || token.isBlank()) {
                plugin.getLogger().warning("ตั้ง provider เป็น finnhub แต่ยังไม่ได้ใส่ finnhub-token -> สลับกลับไปใช้โหมด mock");
                provider = new MockPriceProvider();
            } else {
                provider = new FinnhubPriceProvider(token, msg -> plugin.getLogger().warning(msg));
            }
        } else {
            provider = new MockPriceProvider();
        }

        plugin.getLogger().info("โหลดสินทรัพย์ " + assets.size() + " รายการ | แหล่งราคา: " + provider.name());
        start();
    }

    public void start() {
        int minutes = Math.max(1, plugin.getConfig().getInt("market.refresh-minutes", 5));
        long ticks = minutes * 60L * 20L;

        // ดึงรอบแรกหลังเซิร์ฟบูตเสร็จ 3 วินาที แล้ววนซ้ำตามรอบ
        refreshTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::refreshNow, 60L, ticks);
    }

    public void stop() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
    }

    /** ดึงราคาใหม่ ต้องถูกเรียกจาก async thread เท่านั้น */
    public void refreshNow() {
        if (refreshing || assets.isEmpty() || provider == null) {
            return;
        }
        refreshing = true;
        try {
            Map<String, Quote> fresh = provider.fetch(assets.values());
            if (!fresh.isEmpty()) {
                quotes.putAll(fresh);
                lastUpdate = System.currentTimeMillis();
            }
        } catch (Exception e) {
            plugin.getLogger().warning("อัปเดตราคาไม่สำเร็จ: " + e.getMessage());
        } finally {
            refreshing = false;
        }
    }

    /** สั่งอัปเดตทันทีจากที่ไหนก็ได้ ระบบจะโยนไปทำบน async thread ให้เอง */
    public void requestRefreshAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::refreshNow);
    }

    public Map<String, Asset> getAssets() {
        return Collections.unmodifiableMap(assets);
    }

    public Asset getAsset(String id) {
        return assets.get(id);
    }

    public Quote getQuote(String assetId) {
        return quotes.get(assetId);
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public String getProviderName() {
        return provider == null ? "-" : provider.name();
    }

    /** ราคาในเงินของเซิร์ฟเวอร์ = ราคา USD คูณตัวคูณใน config */
    public double toServerMoney(double usdPrice) {
        return usdPrice * plugin.getConfig().getDouble("market.currency-multiplier", 10.0);
    }

    /** ตลาดหุ้นสหรัฐเปิดอยู่ไหม (จันทร์-ศุกร์ 09:30-16:00 เวลานิวยอร์ก) */
    public boolean isMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(MARKET_ZONE);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        return !time.isBefore(MARKET_OPEN) && time.isBefore(MARKET_CLOSE);
    }
}
