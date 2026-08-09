package com.mcinvest.gui;

import com.mcinvest.McInvest;
import com.mcinvest.market.Asset;
import com.mcinvest.market.Quote;
import com.mcinvest.portfolio.Holding;
import com.mcinvest.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

/** หน้าซื้อขายของสินทรัพย์ตัวเดียว */
public final class AssetMenu {

    private static final int SIZE = 45;
    private static final int[] BUY_SLOTS = {19, 20, 21, 22};
    private static final int[] SELL_SLOTS = {28, 29, 30, 31};

    private AssetMenu() {
    }

    public static void open(McInvest plugin, Player player, Asset asset) {
        MenuHolder holder = new MenuHolder(asset.id());
        Inventory inv = Bukkit.createInventory(holder, SIZE, Text.of("&8ซื้อขาย &7| &f" + stripColor(asset.display())));
        holder.setInventory(inv);

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, Text.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        Quote quote = plugin.getMarketManager().getQuote(asset.id());
        Holding holding = plugin.getPortfolioStore().viewHoldings(player.getUniqueId()).get(asset.id());
        double unitPrice = quote == null ? 0 : plugin.getMarketManager().toServerMoney(quote.price());
        double feePercent = plugin.getConfig().getDouble("market.fee-percent", 1.0);

        // ป้ายข้อมูลด้านบน
        List<String> info = new ArrayList<>();
        if (quote == null) {
            info.add("&cยังไม่มีข้อมูลราคา");
        } else {
            info.add("&7ราคาตลาดโลก: &f$" + Text.money(quote.price()));
            info.add("&7ราคาในเซิร์ฟ: &e" + Text.money(unitPrice) + " &7/ หน่วย");
            info.add("&7เปลี่ยนแปลงวันนี้: " + Text.signed(quote.changePercent()));
            info.add("");
            info.add("&7ค่าธรรมเนียม: &f" + Text.money(feePercent) + "%");
            info.add("&7เงินของคุณ: &a" + Text.money(plugin.getEconomy().getBalance(player)));
            if (holding != null && !holding.isEmpty()) {
                double profit = unitPrice * holding.getQty() - holding.getTotalCost();
                info.add("");
                info.add("&7ถืออยู่: &f" + Text.qty(holding.getQty()) + " หน่วย");
                info.add("&7ต้นทุนเฉลี่ย: &f" + Text.money(holding.avgCost()));
                info.add("&7กำไร/ขาดทุน: " + (profit >= 0 ? "&a+" : "&c") + Text.money(profit));
            }
            if (!plugin.getMarketManager().isMarketOpen()) {
                info.add("");
                info.add("&cตลาดปิดอยู่ ราคานี้คือราคาปิดล่าสุด");
            }
        }
        inv.setItem(4, Text.item(asset.icon(), asset.display(), info));

        List<Double> amounts = plugin.getConfig().getDoubleList("market.trade-amounts");
        if (amounts.isEmpty()) {
            amounts = List.of(0.1, 0.5, 1.0, 5.0);
        }

        inv.setItem(10, Text.item(Material.LIME_STAINED_GLASS_PANE, "&a&lซื้อ", "&7เลือกจำนวนทางขวา"));
        inv.setItem(37, Text.item(Material.RED_STAINED_GLASS_PANE, "&c&lขาย", "&7เลือกจำนวนทางขวา"));

        for (int i = 0; i < BUY_SLOTS.length && i < amounts.size(); i++) {
            double amount = amounts.get(i);
            double cost = unitPrice * amount;
            double total = cost * (1 + feePercent / 100.0);

            inv.setItem(BUY_SLOTS[i], Text.item(Material.LIME_DYE,
                    "&a&lซื้อ " + Text.qty(amount) + " หน่วย",
                    "&7ราคา: &f" + Text.money(cost),
                    "&7รวมค่าธรรมเนียม: &e" + Text.money(total),
                    "",
                    "&eคลิกเพื่อยืนยัน"));
            holder.bind(BUY_SLOTS[i], "buy:" + amount);

            double proceeds = cost * (1 - feePercent / 100.0);
            inv.setItem(SELL_SLOTS[i], Text.item(Material.RED_DYE,
                    "&c&lขาย " + Text.qty(amount) + " หน่วย",
                    "&7มูลค่า: &f" + Text.money(cost),
                    "&7ได้รับสุทธิ: &e" + Text.money(proceeds),
                    "",
                    "&eคลิกเพื่อยืนยัน"));
            holder.bind(SELL_SLOTS[i], "sell:" + amount);
        }

        // ขายทั้งหมด
        double allQty = holding == null ? 0 : holding.getQty();
        inv.setItem(34, Text.item(Material.HOPPER, "&6&lขายทั้งหมด",
                "&7จำนวน: &f" + Text.qty(allQty) + " หน่วย",
                "&7ได้รับสุทธิราว: &e" + Text.money(unitPrice * allQty * (1 - feePercent / 100.0)),
                "",
                allQty > 0 ? "&eคลิกเพื่อยืนยัน" : "&cคุณยังไม่ได้ถือสินทรัพย์นี้"));
        holder.bind(34, "sellall");

        inv.setItem(40, Text.item(Material.ARROW, "&f&lกลับไปหน้าตลาด"));
        holder.bind(40, "market");

        player.openInventory(inv);
    }

    static String stripColor(String input) {
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
