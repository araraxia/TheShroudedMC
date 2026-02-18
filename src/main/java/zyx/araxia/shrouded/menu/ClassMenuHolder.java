package zyx.araxia.shrouded.menu;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker InventoryHolder so we can reliably identify the class-select menu
 * in click events without relying on the deprecated getTitle() method.
 */
public class ClassMenuHolder implements InventoryHolder {

    @Override
    public Inventory getInventory() {
        return null; // Not used — this is purely a marker
    }
}
