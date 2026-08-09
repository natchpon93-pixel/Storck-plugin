package com.mcinvest.portfolio;

/**
 * สิ่งที่ผู้เล่นถืออยู่ในสินทรัพย์ 1 ตัว
 *
 * qty       = จำนวนหน่วย (ทศนิยมได้ เพราะซื้อเศษหน่วยได้)
 * totalCost = เงินในเกมที่จ่ายไปทั้งหมดรวมค่าธรรมเนียม ใช้คิดต้นทุนเฉลี่ยและกำไร/ขาดทุน
 */
public class Holding {

    private double qty;
    private double totalCost;

    public Holding() {
        this(0.0, 0.0);
    }

    public Holding(double qty, double totalCost) {
        this.qty = qty;
        this.totalCost = totalCost;
    }

    public double getQty() {
        return qty;
    }

    public double getTotalCost() {
        return totalCost;
    }

    /** ต้นทุนเฉลี่ยต่อหน่วย */
    public double avgCost() {
        return qty <= 0 ? 0.0 : totalCost / qty;
    }

    public void add(double amount, double cost) {
        this.qty += amount;
        this.totalCost += cost;
    }

    /**
     * ตัดจำนวนที่ขายออก พร้อมลดต้นทุนตามสัดส่วน
     * ทำแบบนี้ต้นทุนเฉลี่ยของส่วนที่เหลือจะไม่เพี้ยน
     */
    public void remove(double amount) {
        if (amount >= qty) {
            qty = 0.0;
            totalCost = 0.0;
            return;
        }
        double ratio = amount / qty;
        totalCost -= totalCost * ratio;
        qty -= amount;
    }

    public boolean isEmpty() {
        return qty <= 0.0000001;
    }
}
