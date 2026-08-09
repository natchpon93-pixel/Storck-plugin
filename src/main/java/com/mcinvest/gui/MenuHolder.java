package com.mcinvest.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * ตัวยึดเมนู ใช้ 2 อย่าง
 * 1. บอกว่าหน้าต่างที่ผู้เล่นเปิดอยู่เป็นของ McInvest จริง (กันไปยุ่งกับหีบของคนอื่น)
 * 2. เก็บว่าแต่ละช่องกดแล้วให้ทำอะไร
 */
public class MenuHolder implements InventoryHolder {

    private final String context;
    private final Map<Integer, String> actions = new HashMap<>();
    private Inventory inventory;

    public MenuHolder(String context) {
        this.context = context;
    }

    /** เก็บ id ของสินทรัพย์ที่หน้านี้กำลังดูอยู่ (หน้าตลาดรวมจะเป็นค่าว่าง) */
    public String getContext() {
        return context;
    }

    public void bind(int slot, String action) {
        actions.put(slot, action);
    }

    public String actionAt(int slot) {
        return actions.get(slot);
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
