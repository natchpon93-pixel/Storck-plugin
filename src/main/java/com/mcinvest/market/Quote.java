package com.mcinvest.market;

/**
 * ราคาล่าสุดของสินทรัพย์ 1 ตัว
 *
 * @param price         ราคาปัจจุบัน (USD)
 * @param previousClose ราคาปิดของวันก่อนหน้า ใช้คำนวณ % เปลี่ยนแปลง
 * @param fetchedAt     เวลาที่ดึงมา (epoch millis)
 */
public record Quote(double price, double previousClose, long fetchedAt) {

    public double changePercent() {
        if (previousClose <= 0) {
            return 0.0;
        }
        return (price - previousClose) / previousClose * 100.0;
    }

    public double changeAbsolute() {
        return price - previousClose;
    }

    /** ราคาเก่าเกิน 1 ชั่วโมงถือว่าค้าง (API ล่ม หรือ ตลาดปิดยาว) */
    public boolean isStale() {
        return System.currentTimeMillis() - fetchedAt > 3_600_000L;
    }
}
