package com.mcinvest.command;

import com.mcinvest.McInvest;
import com.mcinvest.gui.MarketMenu;
import com.mcinvest.gui.PortfolioMenu;
import com.mcinvest.market.Asset;
import com.mcinvest.market.Quote;
import com.mcinvest.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InvestCommand implements CommandExecutor, TabCompleter {

    private final McInvest plugin;

    public InvestCommand(McInvest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mcinvest.admin")) {
                sender.sendMessage(Text.of(prefix + plugin.getConfig().getString("messages.no-permission", "&cไม่มีสิทธิ์")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getMarketManager().reload();
            plugin.getMarketManager().requestRefreshAsync();
            sender.sendMessage(Text.of(prefix + "&aโหลด config ใหม่เรียบร้อย กำลังดึงราคาใหม่"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("update")) {
            if (!sender.hasPermission("mcinvest.admin")) {
                sender.sendMessage(Text.of(prefix + plugin.getConfig().getString("messages.no-permission", "&cไม่มีสิทธิ์")));
                return true;
            }
            plugin.getMarketManager().requestRefreshAsync();
            sender.sendMessage(Text.of(prefix + "&aสั่งดึงราคาใหม่แล้ว"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("price")) {
            sender.sendMessage(Text.of("&8&m                                        "));
            sender.sendMessage(Text.of("&a&lราคาล่าสุด &7(" + plugin.getMarketManager().getProviderName() + ")"));
            for (Asset asset : plugin.getMarketManager().getAssets().values()) {
                Quote quote = plugin.getMarketManager().getQuote(asset.id());
                if (quote == null) {
                    sender.sendMessage(Text.of(" &7- " + asset.display() + " &8: &cไม่มีข้อมูล"));
                } else {
                    sender.sendMessage(Text.of(" &7- " + asset.display() + " &8: &e"
                            + Text.money(plugin.getMarketManager().toServerMoney(quote.price()))
                            + " &7(" + Text.signed(quote.changePercent()) + "&7)"));
                }
            }
            sender.sendMessage(Text.of("&8&m                                        "));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("คำสั่งนี้ใช้ได้เฉพาะในเกม (ลอง /invest price ได้จากคอนโซล)");
            return true;
        }
        if (!player.hasPermission("mcinvest.use")) {
            player.sendMessage(Text.of(prefix + plugin.getConfig().getString("messages.no-permission", "&cไม่มีสิทธิ์")));
            return true;
        }

        if (args.length > 0 && (args[0].equalsIgnoreCase("portfolio") || args[0].equalsIgnoreCase("port"))) {
            PortfolioMenu.open(plugin, player);
            return true;
        }

        MarketMenu.open(plugin, player);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("portfolio");
            options.add("price");
            if (sender.hasPermission("mcinvest.admin")) {
                options.add("reload");
                options.add("update");
            }
            options.removeIf(option -> !option.startsWith(args[0].toLowerCase()));
        }
        return options;
    }
}
