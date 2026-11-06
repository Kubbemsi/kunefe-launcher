package com.kunefesmp.menu.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KunefeMenuHolder implements InventoryHolder {
    private final String menuName;
    private Inventory inventory;

    public KunefeMenuHolder(String menuName) {
        this.menuName = menuName;
    }

    public String getMenuName() {
        return menuName;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
