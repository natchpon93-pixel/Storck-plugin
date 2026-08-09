package com.mcinvest.market;

import java.util.Collection;
import java.util.Map;

/**
 * แหล่งข้อมูลราคา
 * เมธอด fetch จะถูกเรียกจาก async thread เสมอ ห้ามไปแตะ Bukkit API ข้างใน
 */
public interface PriceProvider {

    String name();

    /**
     * @return แม็ป assetId -> Quote (ตัวไหนดึงไม่ได้ให้ข้ามไป ไม่ต้องใส่ในผลลัพธ์)
     */
    Map<String, Quote> fetch(Collection<Asset> assets) throws Exception;
}
