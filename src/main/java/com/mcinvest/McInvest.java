package com.mcinvest;

import com.mcinvest.command.InvestCommand;
import com.mcinvest.gui.GuiListener;
import com.mcinvest.market.MarketManager;
import com.mcinvest.portfolio.PortfolioStore;
import com.mcinvest.portfolio.TradeService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class McInvest extends JavaPlugin {

    private Economy economy;
    private MarketManager marketManager;
    private PortfolioStore portfolioStore;
    private TradeService tradeService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        if (!hookEconomy()) {
            getLogger().severe("=======================================================");
            getLogger().severe(" ไม่พบระบบเงินที่เชื่อมกับ Vault");
            getLogger().severe(" เช็คว่า Vault ติดตั้งแล้ว และ CMI เปิด module Economy อยู่");
            getLogger().severe(" (ไฟล์ plugins/CMI/modules.yml -> Economy: true)");
            getLogger().severe("=======================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // เตือนถ้าระบบเงินไม่รองรับทศนิยม เพราะการซื้อเศษหน่วยจะทำให้เงินปัดเพี้ยน
        int digits = economy.fractionalDigits();
        if (digits == 0) {
            getLogger().warning("ระบบเงินตั้งค่าเป็นจำนวนเต็ม (ไม่มีทศนิยม)");
            getLogger().warning("แนะนำให้เปิดทศนิยมใน CMI ไม่งั้นยอดเงินจะถูกปัดจนคำนวณกำไรเพี้ยน");
        }

        portfolioStore = new PortfolioStore(this);
        portfolioStore.load();

        marketManager = new MarketManager(this);
        marketManager.reload();

        tradeService = new TradeService(this);

        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        PluginCommand command = getCommand("invest");
        if (command != null) {
            InvestCommand executor = new InvestCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }

        // บันทึกพอร์ตอัตโนมัติ ทำบน async thread จะได้ไม่หน่วงเซิร์ฟ
        long autosave = Math.max(30L, getConfig().getLong("storage.autosave-seconds", 300L)) * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> portfolioStore.saveIfDirty(), autosave, autosave);

        getLogger().info("McInvest เปิดใช้งานแล้ว | ระบบเงิน: " + economy.getName());
    }

    @Override
    public void onDisable() {
        if (marketManager != null) {
            marketManager.stop();
        }
        if (portfolioStore != null) {
            portfolioStore.save();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    private boolean hookEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        economy = provider.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public PortfolioStore getPortfolioStore() {
        return portfolioStore;
    }

    public TradeService getTradeService() {
        return tradeService;
    }
}
