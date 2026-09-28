package com.wavestudio.rtp;

import com.wavestudio.rtp.command.RtpCommand;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.dialog.RtpDialogFactory;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RtpPlugin extends JavaPlugin {
    private RtpConfig config;
    private MessageProvider messageProvider;
    private SafeLocationFinder locationFinder;
    private CooldownManager cooldownManager;
    private RtpDialogFactory dialogFactory;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("config.yml", false);

        this.config = new RtpConfig(this);
        this.messageProvider = new MessageProvider(config);
        this.cooldownManager = new CooldownManager(config.getCooldownSeconds());
        this.locationFinder = new SafeLocationFinder(this, config);
        this.dialogFactory = new RtpDialogFactory(this, config, messageProvider, locationFinder, cooldownManager);

        RtpCommand rtpCommand = new RtpCommand(this, config, messageProvider, dialogFactory, locationFinder, cooldownManager);
        getCommand("rtp").setExecutor(rtpCommand);
        getCommand("rtp").setTabCompleter(rtpCommand);

        getLogger().info("MultiDimensionRTP enabled successfully!");
        getLogger().info("By wavestudio");
    }

    @Override
    public void onDisable() {
        if (locationFinder != null) {
            locationFinder.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        getLogger().info("MultiDimensionRTP disabled.");
    }

    public RtpConfig getRtpConfig() {
        return config;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }
}