package zyx.araxia.shrouded.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadConfigCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ReloadConfigCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text("TheShrouded config reloaded.", NamedTextColor.GREEN));
        plugin.getLogger().info("config.yml reloaded by " + sender.getName());
        return true;
    }
}
