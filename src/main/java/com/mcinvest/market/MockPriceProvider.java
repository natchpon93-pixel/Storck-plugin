package com.mcinvest.market;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ราคาปลอมสำหรับทดสอบ ไม่ต้องต่อเน็ต ไม่ต้องมี API key
 *
 * ใช้วิธี random walk คือเอาราคาล่าสุดมาบวก/ลบแบบสุ่มทีละนิด
 * ผลที่ได้จะดูเหมือนกราฟหุ้นจริงมากกว่าการสุ่มเลขใหม่ทุกครั้ง
 */
public class MockPriceProvider implements PriceProvider {

    private final Map<String, Double> current = new ConcurrentHashMap<>();
    private final Map<String, Double> dayOpen = new ConcurrentHashMap<>();
    private long lastDayReset = System.currentTimeMillis();

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public Map<String, Quote> fetch(Collection<Asset> assets) {
        // รีเซ็ต "ราคาเปิดวัน" ทุก 24 ชั่วโมง เพื่อให้ % เปลี่ยนแปลงไม่บวมเกินจริง
        long now = System.currentTimeMillis();
        if (now - lastDayReset > 86_400_000L) {
            lastDayReset = now;
            dayOpen.clear();
        }

        Map<String, Quote> result = new HashMap<>();
        for (Asset asset : assets) {
            double base = asset.base() > 0 ? asset.base() : 100.0;
            double last = current.getOrDefault(asset.id(), base);
            dayOpen.putIfAbsent(asset.id(), last);

            // ความผันผวนต่อรอบประมาณ 0.6% และมีแรงดึงกลับเข้าหาราคาฐาน
            double shock = ThreadLocalRandom.current().nextGaussian() * 0.006;
            double pullBack = (base - last) / base * 0.02;
            double next = last * (1.0 + shock + pullBack);

            // กันราคาหลุดต่ำผิดปกติ
            if (next < base * 0.3) {
                next = base * 0.3;
            }

            current.put(asset.id(), next);
            result.put(asset.id(), new Quote(next, dayOpen.get(asset.id()), now));
        }
        return result;
    }
}
