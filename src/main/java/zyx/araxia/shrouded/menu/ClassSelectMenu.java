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
        int size = Math.max(9, (int) Math.ceil(PlayerClass.regularClasses().length / 9.0) * 9);

        // Create a new inventory with a custom holder to identify it later in the click listener
        Inventory inv = Bukkit.createInventory(new ClassMenuHolder(), size,
                Component.text("Choose Your Class", NamedTextColor.DARK_PURPLE));

        // Iterate through the PlayerClass values and create an item for each one
        for (PlayerClass cls : PlayerClass.regularClasses()) {
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
