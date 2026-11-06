package com.kunefesmp.menu.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuConfig {

    private final String id;
    private final String title;
    private final int size;
    private final String openSound;
    private final List<MenuItemConfig> items = new ArrayList<>();
    private final Map<Integer, MenuItemConfig> itemsBySlot = new HashMap<>();

    public MenuConfig(String id, String title, int size, String openSound) {
        this.id = id;
        this.title = title != null ? title : "&8Menü";
        this.size = (size > 0 && size <= 54 && size % 9 == 0) ? size : 54;
        this.openSound = openSound;
    }

    public void addItem(MenuItemConfig item) {
        items.add(item);
        for (int slot : item.getSlots()) {
            if (slot >= 0 && slot < size) {
                itemsBySlot.put(slot, item);
            }
        }
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public String getOpenSound() {
        return openSound;
    }

    public List<MenuItemConfig> getItems() {
        return items;
    }

    public MenuItemConfig getItemAt(int slot) {
        return itemsBySlot.get(slot);
    }
}
