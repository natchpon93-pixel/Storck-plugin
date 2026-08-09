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
import java.util.Map;

/** หน้าสรุปพอร์ต โชว์ทุกตัวที่ถืออยู่พร้อมกำไรขาดทุน */
public final class PortfolioMenu {

    private static final int SIZE = 54;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private PortfolioMenu() {
    }

    public static void open(McInvest plugin, Player player) {
        MenuHolder holder = new MenuHolder("");
        Inventory inv = Bukkit.createInventory(holder, SIZE, Text.of("&8พอร์ตของ &f" + player.getName()));
        holder.setInventory(inv);

        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, Text.filler(Material.BLACK_STAINED_GLASS_PANE));
        }

        Map<String, Holding> holdings = plugin.getPortfolioStore().viewHoldings(player.getUniqueId());
        double totalValue = 0.0;
        double totalCost = 0.0;
        int index = 0;

        for (Map.Entry<String, Holding> entry : holdings.entrySet()) {
            if (index >= SLOTS.length) {
                break;
            }
            Asset asset = plugin.getMarketManager().getAsset(entry.getKey());
            Holding holding = entry.getValue();
            if (asset == null || holding.isEmpty()) {
                continue;
            }

            Quote quote = plugin.getMarketManager().getQuote(asset.id());
            List<String> lore = new ArrayList<>();
            lore.add("&7ถืออยู่: &f" + Text.qty(holding.getQty()) + " หน่วย");
            lore.add("&7ต้นทุนรวม: &f" + Text.money(holding.getTotalCost()));
            lore.add("&7ต้นทุนเฉลี่ย: &f" + Text.money(holding.avgCost()));

            if (quote != null) {
                double unitPrice = plugin.getMarketManager().toServerMoney(quote.price());
                double value = unitPrice * holding.getQty();
                double profit = value - holding.getTotalCost();
                double profitPercent = holding.getTotalCost() > 0 ? profit / holding.getTotalCost() * 100.0 : 0.0;

                totalValue += value;
                totalCost += holding.getTotalCost();

                lore.add("&7มูลค่าตอนนี้: &e" + Text.money(value));
                lore.add("&7กำไร/ขาดทุน: " + (profit >= 0 ? "&a+" : "&c") + Text.money(profit)
                        + " &7(" + Text.signed(profitPercent) + "&7)");
            } else {
                lore.add("&cยังไม่มีข้อมูลราคา");
            }
            lore.add("");
            lore.add("&eคลิกเพื่อไปหน้าซื้อขาย");

            int slot = SLOTS[index++];
            inv.setItem(slot, Text.item(asset.icon(), asset.display(), lore));
            holder.bind(slot, "asset:" + asset.id());
        }

        if (index == 0) {
            inv.setItem(22, Text.item(Material.COBWEB, "&7&lพอร์ตยังว่างอยู่",
                    "&7กลับไปหน้าตลาดแล้วเริ่มลงทุนได้เลย"));
        }

        double totalProfit = totalValue - totalCost;
        double totalProfitPercent = totalCost > 0 ? totalProfit / totalCost * 100.0 : 0.0;

        inv.setItem(49, Text.item(Material.GOLD_BLOCK, "&6&lสรุปพอร์ตทั้งหมด",
                "&7ต้นทุนรวม: &f" + Text.money(totalCost),
                "&7มูลค่าปัจจุบัน: &e" + Text.money(totalValue),
                "&7กำไร/ขาดทุนรวม: " + (totalProfit >= 0 ? "&a+" : "&c") + Text.money(totalProfit)
                        + " &7(" + Text.signed(totalProfitPercent) + "&7)",
                "",
                "&7เงินสด: &a" + Text.money(plugin.getEconomy().getBalance(player))));

        inv.setItem(45, Text.item(Material.ARROW, "&f&lกลับไปหน้าตลาด"));
        holder.bind(45, "market");

        inv.setItem(53, Text.item(Material.BARRIER, "&c&lปิดเมนู"));
        holder.bind(53, "close");

        player.openInventory(inv);
    }
}
