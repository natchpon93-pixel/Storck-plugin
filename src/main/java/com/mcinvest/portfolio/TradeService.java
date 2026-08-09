package com.mcinvest.portfolio;

import com.mcinvest.McInvest;
import com.mcinvest.market.Asset;
import com.mcinvest.market.Quote;
import com.mcinvest.util.Text;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ตรรกะการซื้อขายทั้งหมด
 *
 * ทุกเมธอดในคลาสนี้ต้องถูกเรียกจาก main thread เท่านั้น
 * เพราะ Vault และ CMI ไม่ปลอดภัยต่อการเรียกข้ามเธรด
 */
public class TradeService {

    public record Result(boolean success, String message) {
    }

    private final McInvest plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public TradeService(McInvest plugin) {
        this.plugin = plugin;
    }

    /** กันคลิกรัว ป้องกันการยิงคำสั่งซ้ำจากการดับเบิลคลิก */
    public boolean onCooldown(Player player) {
        long now = System.currentTimeMillis();
        long cooldownMs = plugin.getConfig().getLong("market.click-cooldown-ms", 400L);
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < cooldownMs) {
            return true;
        }
        cooldowns.put(player.getUniqueId(), now);
        return false;
    }

    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    public Result buy(Player player, Asset asset, double qty) {
        Result check = commonChecks(qty);
        if (check != null) {
            return check;
        }

        Quote quote = plugin.getMarketManager().getQuote(asset.id());
        if (quote == null) {
            return fail("price-unavailable");
        }
        if (!tradingAllowed()) {
            return fail("market-closed");
        }

        double unitPrice = plugin.getMarketManager().toServerMoney(quote.price());
        double gross = unitPrice * qty;
        double fee = gross * feeRate();
        double total = gross + fee;

        if (!plugin.getEconomy().has(player, total)) {
            return new Result(false, msg("not-enough-money").replace("%cost%", Text.money(total)));
        }

        EconomyResponse response = plugin.getEconomy().withdrawPlayer(player, total);
        if (!response.transactionSuccess()) {
            return new Result(false, "&cตัดเงินไม่สำเร็จ: " + response.errorMessage);
        }

        Holding holding = plugin.getPortfolioStore().getHolding(player.getUniqueId(), asset.id());
        holding.add(qty, total);
        plugin.getPortfolioStore().rememberName(player.getUniqueId(), player.getName());
        plugin.getPortfolioStore().markDirty();

        String message = msg("bought")
                .replace("%asset%", stripColor(asset.display()))
                .replace("%qty%", Text.qty(qty))
                .replace("%cost%", Text.money(total));
        return new Result(true, message);
    }

    public Result sell(Player player, Asset asset, double qty) {
        Result check = commonChecks(qty);
        if (check != null) {
            return check;
        }

        Holding holding = plugin.getPortfolioStore().getHolding(player.getUniqueId(), asset.id());
        if (holding.getQty() < qty) {
            return fail("not-enough-holding");
        }

        Quote quote = plugin.getMarketManager().getQuote(asset.id());
        if (quote == null) {
            return fail("price-unavailable");
        }
        if (!tradingAllowed()) {
            return fail("market-closed");
        }

        double unitPrice = plugin.getMarketManager().toServerMoney(quote.price());
        double gross = unitPrice * qty;
        double fee = gross * feeRate();
        double net = gross - fee;

        EconomyResponse response = plugin.getEconomy().depositPlayer(player, net);
        if (!response.transactionSuccess()) {
            return new Result(false, "&cโอนเงินเข้าไม่สำเร็จ: " + response.errorMessage);
        }

        holding.remove(qty);
        if (holding.isEmpty()) {
            plugin.getPortfolioStore().getHoldings(player.getUniqueId()).remove(asset.id());
        }
        plugin.getPortfolioStore().rememberName(player.getUniqueId(), player.getName());
        plugin.getPortfolioStore().markDirty();

        String message = msg("sold")
                .replace("%asset%", stripColor(asset.display()))
                .replace("%qty%", Text.qty(qty))
                .replace("%amount%", Text.money(net));
        return new Result(true, message);
    }

    /** มูลค่ารวมของพอร์ตในหน่วยเงินเซิร์ฟเวอร์ */
    public double portfolioValue(UUID uuid) {
        double sum = 0.0;
        for (Map.Entry<String, Holding> entry : plugin.getPortfolioStore().viewHoldings(uuid).entrySet()) {
            Quote quote = plugin.getMarketManager().getQuote(entry.getKey());
            if (quote != null) {
                sum += plugin.getMarketManager().toServerMoney(quote.price()) * entry.getValue().getQty();
            }
        }
        return sum;
    }

    private Result commonChecks(double qty) {
        double min = plugin.getConfig().getDouble("market.min-order", 0.01);
        if (qty < min) {
            return new Result(false, "&cจำนวนขั้นต่ำคือ " + Text.qty(min) + " หน่วย");
        }
        return null;
    }

    private boolean tradingAllowed() {
        if (plugin.getMarketManager().isMarketOpen()) {
            return true;
        }
        return plugin.getConfig().getBoolean("market.allow-trade-when-closed", true);
    }

    private double feeRate() {
        return plugin.getConfig().getDouble("market.fee-percent", 1.0) / 100.0;
    }

    private Result fail(String key) {
        return new Result(false, msg(key));
    }

    private String msg(String key) {
        return plugin.getConfig().getString("messages." + key, "&c(ไม่พบข้อความ: " + key + ")");
    }

    private static String stripColor(String input) {
        return input.replaceAll("&[0-9a-fk-orA-FK-OR]", "");
    }
}
