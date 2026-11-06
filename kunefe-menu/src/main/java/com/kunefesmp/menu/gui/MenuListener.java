package com.kunefesmp.menu.gui;

import com.kunefesmp.menu.KunefeMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class MenuListener implements Listener {

    private final KunefeMenu plugin;

    public MenuListener(KunefeMenu plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof KunefeMenuHolder holder)) {
            return;
        }

        // Menü içindeki herhangi bir eşya hareketini kesinlikle engelle
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Tıklanan envanter menü envanteri mi kontrol et (oyuncu kendi çantasına tıklayınca da eşya taşınmasın)
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getSlot();
        MenuConfig menuConfig = plugin.getMenuManager().getMenu(holder.getMenuName());
        if (menuConfig == null) return;

        MenuItemConfig itemConfig = menuConfig.getItemAt(slot);
        if (itemConfig != null) {
            plugin.getMenuManager().executeActions(player, itemConfig);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof KunefeMenuHolder) {
            event.setCancelled(true);
        }
    }
}
