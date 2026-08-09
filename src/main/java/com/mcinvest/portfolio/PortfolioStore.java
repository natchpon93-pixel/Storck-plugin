package com.mcinvest.portfolio;

import com.mcinvest.McInvest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * เก็บพอร์ตของผู้เล่นลงไฟล์ portfolios.yml
 *
 * ใช้ YAML เพราะไม่ต้องพึ่ง library เพิ่ม ติดตั้งง่าย และเพียงพอสำหรับเซิร์ฟทั่วไป
 * ถ้าวันหลังผู้เล่นเยอะขึ้นมาก ค่อยเปลี่ยนคลาสนี้ไปใช้ SQLite ได้โดยไม่ต้องแก้ที่อื่นเลย
 */
public class PortfolioStore {

    private final McInvest plugin;
    private final File file;
    private final Map<UUID, Map<String, Holding>> data = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private volatile boolean dirty = false;

    public PortfolioStore(McInvest plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "portfolios.yml");
    }

    public void load() {
        data.clear();
        names.clear();

        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }

        for (String uuidText : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidText);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("ข้าม UUID ที่อ่านไม่ได้: " + uuidText);
                continue;
            }

            ConfigurationSection playerSection = players.getConfigurationSection(uuidText);
            if (playerSection == null) {
                continue;
            }
            String name = playerSection.getString("name", "");
            if (!name.isEmpty()) {
                names.put(uuid, name);
            }

            Map<String, Holding> holdings = new LinkedHashMap<>();
            ConfigurationSection holdingSection = playerSection.getConfigurationSection("holdings");
            if (holdingSection != null) {
                for (String assetId : holdingSection.getKeys(false)) {
                    double qty = holdingSection.getDouble(assetId + ".qty", 0.0);
                    double cost = holdingSection.getDouble(assetId + ".cost", 0.0);
                    if (qty > 0) {
                        holdings.put(assetId, new Holding(qty, cost));
                    }
                }
            }
            data.put(uuid, new ConcurrentHashMap<>(holdings));
        }
        plugin.getLogger().info("โหลดพอร์ตของผู้เล่น " + data.size() + " คน");
    }

    /** เขียนไฟล์จริง (งาน I/O เรียกจาก async thread) */
    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Holding>> entry : data.entrySet()) {
            String base = "players." + entry.getKey();
            String name = names.get(entry.getKey());
            if (name != null) {
                yaml.set(base + ".name", name);
            }
            for (Map.Entry<String, Holding> h : entry.getValue().entrySet()) {
                if (h.getValue().isEmpty()) {
                    continue;
                }
                yaml.set(base + ".holdings." + h.getKey() + ".qty", h.getValue().getQty());
                yaml.set(base + ".holdings." + h.getKey() + ".cost", h.getValue().getTotalCost());
            }
        }

        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("สร้างโฟลเดอร์ปลั๊กอินไม่สำเร็จ");
            }
            yaml.save(file);
            dirty = false;
        } catch (IOException e) {
            plugin.getLogger().severe("บันทึกพอร์ตไม่สำเร็จ: " + e.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) {
            save();
        }
    }

    public void markDirty() {
        this.dirty = true;
    }

    public Map<String, Holding> getHoldings(UUID uuid) {
        return data.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
    }

    public Map<String, Holding> viewHoldings(UUID uuid) {
        Map<String, Holding> map = data.get(uuid);
        return map == null ? Collections.emptyMap() : Collections.unmodifiableMap(map);
    }

    public Holding getHolding(UUID uuid, String assetId) {
        return getHoldings(uuid).computeIfAbsent(assetId, k -> new Holding());
    }

    public void rememberName(UUID uuid, String name) {
        names.put(uuid, name);
    }
}
