package net.jandie1505.lobbyselector;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import net.jandie1505.configmanager.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LobbySelector extends JavaPlugin implements Listener, InventoryHolder, CommandExecutor, TabCompleter {
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this.getDefaultConfigValues(), false, this.getDataFolder(), "config.json");
        this.configManager.reloadConfig();

        this.getServer().getPluginManager().registerEvents(this, this);

        PluginCommand command = this.getCommand("lobbyselector");

        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getInventory() == null || event.getInventory().getHolder() != this) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null) {
            return;
        }

        if (event.getCurrentItem().getItemMeta() == null) {
            return;
        }

        if (!event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "service"))) {
            return;
        }

        CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
        WrapperConfiguration wrapperConfiguration = InjectionLayer.ext().instance(WrapperConfiguration.class);

        if (cloudServiceProvider == null || wrapperConfiguration == null) {
            return;
        }

        ServiceInfoSnapshot service = cloudServiceProvider.serviceByName(event.getCurrentItem().getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(this, "service"), PersistentDataType.STRING, ""));

        if (service == null) {
            return;
        }

        if (!this.isValidLobbyService(service, event.getWhoClicked().hasPermission("lobbyselector.silenthub"))) {
            return;
        }

        if (wrapperConfiguration.serviceInfoSnapshot().name().equals(service.name())) {
            return;
        }

        ServiceRegistry serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);

        if (serviceRegistry == null) {
            return;
        }

        PlayerManager playerManager = serviceRegistry.defaultInstance(PlayerManager.class);

        if (playerManager == null) {
            return;
        }

        CloudPlayer player = playerManager.onlinePlayer(event.getWhoClicked().getUniqueId());

        if (player == null) {
            return;
        }

        PlayerExecutor playerExecutor = playerManager.playerExecutor(player.uniqueId());

        if (playerExecutor == null) {
            return;
        }

        event.getWhoClicked().closeInventory();
        playerExecutor.connect(service.name());

    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (event.getInventory() == null || event.getInventory().getHolder() != this) {
            return;
        }

        event.setCancelled(true);

    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCommand can only be executed by players");
            return true;
        }

        if (!sender.hasPermission("lobbyselector.use")) {
            sender.sendMessage("§cNo permission");
            return true;
        }

        ((Player) sender).openInventory(this.getLobbySelector(sender.hasPermission("lobbyselector.silentlobby")));

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.getServer().createInventory(this, 9, "§c§mLobby Selector");
    }

    public Inventory getLobbySelector(boolean allowSilentLobby) {
        CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
        WrapperConfiguration wrapperConfiguration = InjectionLayer.ext().instance(WrapperConfiguration.class);

        if (cloudServiceProvider == null || wrapperConfiguration == null) {
            return this.getInventory();
        }

        List<ServiceInfoSnapshot> services = new ArrayList<>();

        if (allowSilentLobby && this.configManager.getConfig().optBoolean("enableSilentLobby", false)) {
            services.addAll(cloudServiceProvider.servicesByTask(this.configManager.getConfig().optString("silentLobbyTask", "SilentLobby")));
        }

        services.addAll(cloudServiceProvider.servicesByTask(this.configManager.getConfig().optString("lobbyTask", "Lobby")));

        Collections.reverse(services);

        int inventorySize = ((services.size() / 9) + 1) * 9;

        if (inventorySize > 54) {
            inventorySize = 54;
        }

        if (inventorySize < 9) {
            inventorySize = 9;
        }

        Inventory inventory = this.getServer().createInventory(this, inventorySize, ChatColor.translateAlternateColorCodes('&', this.configManager.getConfig().optString("inventoryTitle", "Lobby Selector:")));

        int slot = 0;
        for (ServiceInfoSnapshot service : services) {

            if (slot > inventorySize - 1) {
                break;
            }

            if (!service.readProperty(BridgeDocProperties.IS_ONLINE)) {
                continue;
            }

            boolean currentService = wrapperConfiguration.serviceInfoSnapshot().name().equals(service.name());
            boolean isSilentHubService = this.isSilentHubService(service);

            String name = service.name();
            int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
            int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);

            if (this.configManager.getConfig().optBoolean("hideFullServices", false) && players >= maxPlayers && !currentService) {
                continue;
            }

            ItemStack item;

            if (currentService) {
                item = this.buildSelectorItem(this.configManager.getConfig().optJSONObject("serverItems", new JSONObject()).optJSONObject("current", new JSONObject()), name, players, maxPlayers);
            } else if (players >= maxPlayers) {
                item = this.buildSelectorItem(this.configManager.getConfig().optJSONObject("serverItems", new JSONObject()).optJSONObject("full", new JSONObject()), name, players, maxPlayers);
            } else if (isSilentHubService) {
                item = this.buildSelectorItem(this.configManager.getConfig().optJSONObject("serverItems", new JSONObject()).optJSONObject("silentHub", new JSONObject()), name, players, maxPlayers);
            } else {
                item = this.buildSelectorItem(this.configManager.getConfig().optJSONObject("serverItems", new JSONObject()).optJSONObject("default", new JSONObject()), name, players, maxPlayers);
            }

            if (item.getItemMeta() == null) {
                item.setItemMeta(this.getServer().getItemFactory().getItemMeta(item.getType()));
            }

            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "service"), PersistentDataType.STRING, service.name());
            item.setItemMeta(meta);

            inventory.setItem(slot, item);

            slot++;
        }

        return inventory;
    }

    private ItemStack buildSelectorItem(JSONObject json, String serviceName, int players, int maxPlayers) {
        Material type = Material.getMaterial(json.optString("material", ""));

        if (type == null) {
            ItemStack errorItem = new ItemStack(Material.BARRIER);
            ItemMeta errorMeta = this.getServer().getItemFactory().getItemMeta(errorItem.getType());
            errorMeta.setDisplayName("INVALID ITEM CONFIGURATION");
            errorItem.setItemMeta(errorMeta);
            return errorItem;
        }

        ItemStack itemStack = new ItemStack(type);
        ItemMeta itemMeta = this.getServer().getItemFactory().getItemMeta(itemStack.getType());

        // NAME
        if (json.has("name")) {
            itemMeta.setDisplayName(this.handlePlaceholders(json.optString("name", "INVALID STRING VALUE"), serviceName, players, maxPlayers));
        }

        // LORE
        if (json.has("lore")) {
            List<String> lore = new ArrayList<>();

            for (Object value : json.optJSONArray("lore", new JSONArray())) {

                if (!(value instanceof String)) {
                    lore.add("INVALID STRING VALUE");
                    continue;
                }

                lore.add(this.handlePlaceholders((String) value, serviceName, players, maxPlayers));

            }

            itemMeta.setLore(lore);

        }

        // ENCHANTED
        if (json.optBoolean("enchanted", false)) {
            itemMeta.addEnchant(Enchantment.FORTUNE, 1, true);
            itemMeta.addItemFlags(ItemFlag.values());
        }

        // CUSTOM MODEL DATA
        if (json.optInt("customModelData", -1) >= 0) {
            itemMeta.setCustomModelData(json.optInt("customModelData", 0));
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack.clone();
    }

    private String handlePlaceholders(String string, String serviceName, int players, int maxPlayers) {
        string = ChatColor.translateAlternateColorCodes('&', string);
        string = string.replace("{service}", serviceName);
        string = string.replace("{players}", String.valueOf(players));
        string = string.replace("{max_players}", String.valueOf(maxPlayers));
        return string;
    }

    private boolean isValidLobbyService(ServiceInfoSnapshot service, boolean enableSilentHub) {

        if (this.configManager.getConfig().optString("lobbyTask") != null && service.serviceId().taskName().equals(this.configManager.getConfig().optString("lobbyTask"))) {
            return true;
        }

        if (enableSilentHub && this.isSilentHubService(service)) {
            return true;
        }

        return false;
    }

    private boolean isSilentHubService(ServiceInfoSnapshot service) {
        return this.configManager.getConfig().optBoolean("enableSilentLobby", false) && this.configManager.getConfig().optString("silentLobbyTask") != null && service.serviceId().taskName().equals(this.configManager.getConfig().optString("silentLobbyTask"));
    }

    private JSONObject getDefaultConfigValues() {
        JSONObject config = new JSONObject();

        config.put("lobbyTask", "Lobby");
        config.put("inventoryTitle", "&lLobby Selector:");
        config.put("hideFullServices", false);
        config.put("enableSilentLobby", false);
        config.put("silentLobbyTask", "SilentLobby");

        JSONObject serverItemConfig = new JSONObject();

        JSONObject defItemConfig = new JSONObject();
        defItemConfig.put("material", Material.EGG.name());
        defItemConfig.put("name", "&7{service} ({players}/{max_players})");
        defItemConfig.put("lore", new JSONArray());
        defItemConfig.put("enchanted", false);
        defItemConfig.put("customModelData", -1);
        serverItemConfig.put("default", defItemConfig);

        JSONObject silentHubConfig = new JSONObject();
        silentHubConfig.put("material", Material.TNT.name());
        silentHubConfig.put("name", "&7{service} ({players}/{max_players})");
        silentHubConfig.put("lore", new JSONArray());
        silentHubConfig.put("enchanted", false);
        silentHubConfig.put("customModelData", -1);
        serverItemConfig.put("silentHub", silentHubConfig);

        JSONObject fullItemConfig = new JSONObject();
        fullItemConfig.put("material", Material.BARRIER.name());
        fullItemConfig.put("name", "&c{service} ({players}/{max_players})");
        fullItemConfig.put("lore", new JSONArray());
        fullItemConfig.put("enchanted", false);
        fullItemConfig.put("customModelData", -1);
        serverItemConfig.put("full", fullItemConfig);

        JSONObject currentItemConfig = new JSONObject();
        currentItemConfig.put("material", Material.EMERALD.name());
        currentItemConfig.put("name", "&a{service} (current) ({players}/{max_players})");
        currentItemConfig.put("lore", new JSONArray());
        currentItemConfig.put("enchanted", true);
        currentItemConfig.put("customModelData", -1);
        serverItemConfig.put("current", currentItemConfig);

        config.put("serverItems", serverItemConfig);

        return config;
    }
}