package com.mcinvest.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ตัวช่วยเรื่องข้อความและไอเทม
 * แปลงโค้ดสีแบบเก่า (&a &e &c) ให้เป็น Component ของ Adventure ที่ Paper ใช้
 */
public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QTY = new DecimalFormat("#,##0.####");

    private Text() {
    }

    /** แปลงสตริงที่มีโค้ดสี &x ให้เป็น Component และปิด italic ที่ Minecraft ใส่มาเองในชื่อไอเทม */
    public static Component of(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    public static List<Component> lore(String... lines) {
        List<Component> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(of(line));
        }
        return out;
    }

    public static List<Component> lore(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(of(line));
        }
        return out;
    }

    /** ฟอร์แมตเงิน เช่น 1234.5 -> 1,234.50 */
    public static String money(double value) {
        return MONEY.format(value);
    }

    /** ฟอร์แมตจำนวนหน่วย ตัดศูนย์ท้ายทิ้ง เช่น 1.5000 -> 1.5 */
    public static String qty(double value) {
        return QTY.format(value);
    }

    /** ใส่สีตามบวก/ลบ เช่น +2.35% เป็นสีเขียว, -1.10% เป็นสีแดง */
    public static String signed(double percent) {
        String color = percent > 0 ? "&a+" : (percent < 0 ? "&c" : "&7");
        return color + MONEY.format(percent) + "%";
    }

    public static ItemStack item(Material material, String name, String... loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(of(name));
            if (loreLines.length > 0) {
                meta.lore(lore(loreLines));
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack item(Material material, String name, List<String> loreLines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(of(name));
            meta.lore(lore(loreLines));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** ไอเทมกระจกใสไว้ถมช่องว่างในเมนู */
    public static ItemStack filler(Material material) {
        return item(material, "&r");
    }

    public static List<String> list(String... lines) {
        return new ArrayList<>(Arrays.asList(lines));
    }
}
