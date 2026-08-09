package com.mcinvest.market;

import org.bukkit.Material;

/**
 * สินทรัพย์ 1 ตัวที่เปิดให้ลงทุน อ่านมาจาก config
 *
 * @param id      คีย์ใน config เช่น gold, aapl
 * @param symbol  ticker ที่ใช้ยิง API เช่น GLD, AAPL
 * @param display ชื่อที่โชว์ในเมนู
 * @param icon    ไอเทมที่ใช้เป็นไอคอน
 * @param base    ราคาตั้งต้น (USD) ใช้เฉพาะโหมด mock
 */
public record Asset(String id, String symbol, String display, Material icon, double base) {
}
