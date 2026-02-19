package zyx.araxia.shrouded.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
                Component.text("Choose Your Class", NamedTextColor.DARK_PURPLE));

        for (PlayerClass cls : PlayerClass.values()) {
            ItemStack item = new ItemStack(cls.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(cls.getDisplayName(), NamedTextColor.GOLD));
            meta.lore(List.of(
                    Component.text(cls.getDescription(), NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Click to select!", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
            inv.addItem(item);
        }

        player.openInventory(inv);
    }
}
