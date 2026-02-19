package zyx.araxia.shrouded;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import zyx.araxia.shrouded.commands.ArenaLobbyCommand;
import zyx.araxia.shrouded.commands.ArenaRegisterCommand;
import zyx.araxia.shrouded.commands.LobbyCountdownCommand;
import zyx.araxia.shrouded.commands.LobbyRegisterCommand;
import zyx.araxia.shrouded.commands.SignRegisterCommand;
import zyx.araxia.shrouded.listener.ClassSelectMenuListener;
import zyx.araxia.shrouded.listener.SignClickListener;
import zyx.araxia.shrouded.lobby.ArenaManager;
import zyx.araxia.shrouded.lobby.LobbyManager;

public class TheShrouded extends JavaPlugin {

    private LobbyManager lobbyManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        lobbyManager = new LobbyManager(this);
        arenaManager = new ArenaManager(this);

        // Register commands
        final String registerLobbyName = "shrouded.register.lobby";
        final String registerSignName = "shrouded.register.sign";
        final String registerArenaName = "shrouded.register.arena";
        final String lobbyArenaName = "shrouded.lobby.arena";
        final String lobbyCountdownName = "shrouded.lobby.countdown";
        PluginCommand lobbyRegisterCmd = getCommand(registerLobbyName);
        PluginCommand signRegisterCmd = getCommand(registerSignName);
        PluginCommand arenaRegisterCmd = getCommand(registerArenaName);
        PluginCommand arenaLobbyCmd = getCommand(lobbyArenaName);
        PluginCommand lobbyCountdownCmd = getCommand(lobbyCountdownName);
        if (lobbyRegisterCmd != null)
            lobbyRegisterCmd.setExecutor(
                new LobbyRegisterCommand(this, lobbyManager)
            );
        if (signRegisterCmd != null)
            signRegisterCmd.setExecutor(
                new SignRegisterCommand(lobbyManager)
            );
        if (arenaRegisterCmd != null)
            arenaRegisterCmd.setExecutor(
                new ArenaRegisterCommand(this, arenaManager)
            );
        if (arenaLobbyCmd != null)
            arenaLobbyCmd.setExecutor(
                new ArenaLobbyCommand(lobbyManager, arenaManager)
            );
        if (lobbyCountdownCmd != null)
            lobbyCountdownCmd.setExecutor(
                new LobbyCountdownCommand(lobbyManager)
            );

        // Register event listeners
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

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}