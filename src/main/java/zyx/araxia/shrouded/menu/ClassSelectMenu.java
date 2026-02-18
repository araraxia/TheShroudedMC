package zyx.araxia.shrouded.menu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import zyx.araxia.shrouded.game.PlayerClass;

import java.util.List;

public class ClassSelectMenu {

    private ClassSelectMenu() {}

    /**
     * Opens the class selection inventory for the given player.
     * One slot is created per {@link PlayerClass} value.
     */
    public static void open(Player player) {
        // Round up to the nearest multiple of 9 for inventory size
        int size = Math.max(9, (int) Math.ceil(PlayerClass.values().length / 9.0) * 9);
        Inventory inv = Bukkit.createInventory(new ClassMenuHolder(), size,
                ChatColor.DARK_PURPLE + "Choose Your Class");

        for (PlayerClass cls : PlayerClass.values()) {
            ItemStack item = new ItemStack(cls.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + cls.getDisplayName());
            meta.setLore(List.of(
                    ChatColor.GRAY + cls.getDescription(),
                    "",
                    ChatColor.YELLOW + "Click to select!"
            ));
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }
}
