package pl.flezy.tempbuild;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.serdes.commons.SerdesCommons;
import eu.okaeri.configs.validator.okaeri.OkaeriValidator;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import org.bukkit.plugin.java.JavaPlugin;
import pl.flezy.tempbuild.command.TempBuildCommand;
import pl.flezy.tempbuild.config.Config;
import pl.flezy.tempbuild.listener.BuildListener;
import pl.flezy.tempbuild.listener.FireListener;
import pl.flezy.tempbuild.manager.BlockDecayManager;

import java.io.File;

public final class TempBuild extends JavaPlugin {

    private static TempBuild instance;
    public Config config;
    public StateFlag TEMP_BUILD_FLAG;

    @Override
    public void onLoad() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag("temp-build", false);
            registry.register(flag);
            TEMP_BUILD_FLAG = flag;
        } catch (FlagConflictException e) {
            Flag<?> existing = registry.get("temp-build");
            if (existing instanceof StateFlag) {
                TEMP_BUILD_FLAG = (StateFlag) existing;
            } else {
                getLogger().warning("Flag 'temp-build' already exists and is not a StateFlag!");
            }
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        loadConfig();

        getServer().getPluginManager().registerEvents(new BuildListener(), this);
        getServer().getPluginManager().registerEvents(new FireListener(), this);

        getCommand("tempbuild").setExecutor(new TempBuildCommand());
        getCommand("tempbuild").setTabCompleter(new TempBuildCommand());

        BlockDecayManager.initialize();
    }

    private void loadConfig() {
        config = (Config) ConfigManager.create(Config.class)
                .withConfigurer(new OkaeriValidator(new YamlBukkitConfigurer()))
                .withSerdesPack(registry -> {
                    registry.register(new SerdesCommons());
                    registry.register(new SerdesBukkit());
                })
                .withBindFile(new File(this.getDataFolder(), "config.yml"))
                .saveDefaults()
                .load(true);
    }

    public static TempBuild getInstance() {
        return instance;
    }

    public void reload() {
        config.load();
    }
}
