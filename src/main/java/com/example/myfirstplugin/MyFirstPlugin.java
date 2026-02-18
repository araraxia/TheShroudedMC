package com.example.myfirstplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MyFirstPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // This runs when the plugin is enabled
        getLogger().info("MyFirstPlugin has been enabled!");
        
        // Register event listener
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        // This runs when the plugin is disabled
        getLogger().info("MyFirstPlugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("hello")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.sendMessage(ChatColor.GREEN + "Hello, " + player.getName() + "!");
                player.sendMessage(ChatColor.YELLOW + "Welcome to your first plugin!");
            } else {
                sender.sendMessage("This command can only be used by players!");
            }
            return true;
        }
        return false;
    }

