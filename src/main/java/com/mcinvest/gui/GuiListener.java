package com.mcinvest.gui;

import com.mcinvest.McInvest;
import com.mcinvest.market.Asset;
import com.mcinvest.portfolio.Holding;
import com.mcinvest.portfolio.TradeService;
import com.mcinvest.util.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * ดักการคลิกในเมนูของ McInvest
 *
 * จุดสำคัญ: ต้อง setCancelled(true) ทุกครั้ง
 * ไม่งั้นผู้เล่นลากไอเทมจากเมนูออกไปได้ = ดูปไอเทมฟรี
 */
public class GuiListener implements Listener {

    private final McInvest plugin;

    public GuiListener(McInvest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        // คลิกในช่องกระเป๋าตัวเอง ไม่ต้องทำอะไร (แต่ยัง cancel ไว้เพื่อความปลอดภัย)
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        String action = holder.actionAt(event.getSlot());
        if (action == null) {
            return;
        }

        if (action.equals("close")) {
            player.closeInventory();
            return;
        }
        if (action.equals("market")) {
            MarketMenu.open(plugin, player);
            return;
        }
        if (action.equals("portfolio")) {
            PortfolioMenu.open(plugin, player);
            return;
        }
        if (action.startsWith("asset:")) {
            Asset asset = plugin.getMarketManager().getAsset(action.substring(6));
            if (asset != null) {
                AssetMenu.open(plugin, player, asset);
            }
            return;
        }

        // ตั้งแต่ตรงนี้คือคำสั่งซื้อขาย ต้องมี asset ที่หน้านี้กำลังดูอยู่
        Asset asset = plugin.getMarketManager().getAsset(holder.getContext());
        if (asset == null) {
            return;
        }
        if (plugin.getTradeService().onCooldown(player)) {
            return;
        }

        TradeService.Result result;
        if (action.startsWith("buy:")) {
            result = plugin.getTradeService().buy(player, asset, parse(action.substring(4)));
        } else if (action.startsWith("sell:")) {
            result = plugin.getTradeService().sell(player, asset, parse(action.substring(5)));
        } else if (action.equals("sellall")) {
            Holding holding = plugin.getPortfolioStore().viewHoldings(player.getUniqueId()).get(asset.id());
            if (holding == null || holding.isEmpty()) {
                return;
            }
            result = plugin.getTradeService().sell(player, asset, holding.getQty());
        } else {
            return;
        }

        player.sendMessage(Text.of(plugin.getConfig().getString("messages.prefix", "") + result.message()));
        player.playSound(player.getLocation(),
                result.success() ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_VILLAGER_NO,
                1.0f, 1.0f);

        // วาดเมนูใหม่ให้ตัวเลขอัปเดตทันที
        AssetMenu.open(plugin, player, asset);
    }

    private static double parse(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
