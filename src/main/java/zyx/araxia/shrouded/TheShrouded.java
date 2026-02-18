package zyx.araxia.shrouded;

import org.bukkit.plugin.java.JavaPlugin;
import zyx.araxia.shrouded.commands.LobbyRegisterCommand;
import zyx.araxia.shrouded.commands.SignRegisterCommand;
import zyx.araxia.shrouded.listener.ClassSelectMenuListener;
import zyx.araxia.shrouded.listener.SignClickListener;
import zyx.araxia.shrouded.lobby.LobbyManager;

public class TheShrouded extends JavaPlugin {

    private LobbyManager lobbyManager;

    @Override
    public void onEnable() {
        lobbyManager = new LobbyManager(this);

        getCommand("shrouded.register.lobby").setExecutor(new LobbyRegisterCommand(this, lobbyManager));
        getCommand("shrouded.register.sign").setExecutor(new SignRegisterCommand(lobbyManager));

        getServer().getPluginManager().registerEvents(new SignClickListener(lobbyManager), this);
        getServer().getPluginManager().registerEvents(new ClassSelectMenuListener(lobbyManager), this);

        getLogger().info("TheShrouded has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TheShrouded has been disabled!");
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }
}