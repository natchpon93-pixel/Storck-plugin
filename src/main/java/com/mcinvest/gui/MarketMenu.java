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
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** หน้าแรก โชว์สินทรัพย์ทั้งหมดพร้อมราคาล่าสุด */
public final class MarketMenu {

    private static final int SIZE = 54;
    private static final int[] ASSET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    private MarketMenu() {
    }

    public static void open(McInvest plugin, Player player) {
        MenuHolder holder = new MenuHolder("");
        Inventory inv = Bukkit.createInventory(holder, SIZE, Text.of("&8ตลาดการลงทุน &7| &aMcInvest"));
        holder.setInventory(inv);

        ItemStack filler = Text.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        List<Asset> assets = new ArrayList<>(plugin.getMarketManager().getAssets().values());
        for (int i = 0; i < assets.size() && i < ASSET_SLOTS.length; i++) {
            Asset asset = assets.get(i);
            int slot = ASSET_SLOTS[i];
            inv.setItem(slot, buildAssetIcon(plugin, player, asset));
            holder.bind(slot, "asset:" + asset.id());
        }

        // สถานะตลาด
        boolean open = plugin.getMarketManager().isMarketOpen();
        long since = System.currentTimeMillis() - plugin.getMarketManager().getLastUpdate();
        String ago = plugin.getMarketManager().getLastUpdate() == 0
                ? "&cยังไม่เคยอัปเดต"
                : "&f" + Duration.ofMillis(since).toMinutes() + " นาทีที่แล้ว";

        inv.setItem(4, Text.item(Material.CLOCK,
                open ? "&a&lตลาดเปิดอยู่" : "&c&lตลาดปิดอยู่",
                "&7ตลาดหุ้นสหรัฐเปิดจันทร์ถึงศุกร์",
                "&7ประมาณ &f20:30 - 03:00 น. &7เวลาไทย",
                "",
                "&7อัปเดตราคาล่าสุด: " + ago,
                "&7แหล่งข้อมูล: &f" + plugin.getMarketManager().getProviderName()));

        // พอร์ตของเรา
        double value = plugin.getTradeService().portfolioValue(player.getUniqueId());
        inv.setItem(49, Text.item(Material.ENDER_CHEST, "&6&lพอร์ตของฉัน",
                "&7มูลค่ารวมตอนนี้: &e" + Text.money(value),
                "&7เงินสดในกระเป๋า: &e" + Text.money(plugin.getEconomy().getBalance(player)),
                "",
                "&eคลิกเพื่อดูรายละเอียด"));
        holder.bind(49, "portfolio");

        inv.setItem(53, Text.item(Material.BARRIER, "&c&lปิดเมนู"));
        holder.bind(53, "close");

        player.openInventory(inv);
    }

    private static ItemStack buildAssetIcon(McInvest plugin, Player player, Asset asset) {
        Quote quote = plugin.getMarketManager().getQuote(asset.id());
        List<String> lore = new ArrayList<>();

        if (quote == null) {
            lore.add("&cยังไม่มีข้อมูลราคา");
            lore.add("&7รอระบบดึงราคารอบถัดไป");
        } else {
            double serverPrice = plugin.getMarketManager().toServerMoney(quote.price());
            lore.add("&7สัญลักษณ์: &f" + asset.symbol());
            lore.add("&7ราคาตลาดโลก: &f$" + Text.money(quote.price()));
            lore.add("&7ราคาในเซิร์ฟ: &e" + Text.money(serverPrice) + " &7/ หน่วย");
            lore.add("&7เปลี่ยนแปลงวันนี้: " + Text.signed(quote.changePercent()));

            Holding holding = plugin.getPortfolioStore().viewHoldings(player.getUniqueId()).get(asset.id());
            if (holding != null && !holding.isEmpty()) {
                double marketValue = serverPrice * holding.getQty();
                double profit = marketValue - holding.getTotalCost();
                lore.add("");
                lore.add("&7คุณถืออยู่: &f" + Text.qty(holding.getQty()) + " หน่วย");
                lore.add("&7ต้นทุนเฉลี่ย: &f" + Text.money(holding.avgCost()));
                lore.add("&7กำไร/ขาดทุน: " + (profit >= 0 ? "&a+" : "&c") + Text.money(profit));
            }
            lore.add("");
            lore.add("&eคลิกเพื่อซื้อขาย");
        }
        return Text.item(asset.icon(), asset.display(), lore);
    }
}
