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
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
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

        if (event.getCurrentItem().getItemMeta().getLore() == null) {
            return;
        }

        if (event.getCurrentItem().getItemMeta().getLore().isEmpty()) {
            return;
        }

        CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
        WrapperConfiguration wrapperConfiguration = InjectionLayer.ext().instance(WrapperConfiguration.class);

        if (cloudServiceProvider == null || wrapperConfiguration == null) {
            return;
        }

        ServiceInfoSnapshot service = cloudServiceProvider.serviceByName(event.getCurrentItem().getItemMeta().getLore().get(0));

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

        PlayerManager playerManager = serviceRegistry.firstProvider(PlayerManager.class);

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

            String name = service.name();
            int players = service.readProperty(BridgeDocProperties.PLAYERS).size();
            int maxPlayers = service.readProperty(BridgeDocProperties.MAX_PLAYERS);

            if (this.configManager.getConfig().optBoolean("hideFullServices", false) && players >= maxPlayers && !currentService) {
                continue;
            }

            ItemStack item = new ItemStack(Material.AIR);

            if (currentService) {
                item.setType(Objects.requireNonNullElse(Material.getMaterial(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("typeCurrent", "")), Material.EMERALD));
                this.setTitle(item, this.handlePlaceholders(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("nameCurrent", "&7{service} ({players}/{max_players})"), name, players, maxPlayers));
            } else if (players >= maxPlayers) {
                item.setType(Objects.requireNonNullElse(Material.getMaterial(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("typeFull", "")), Material.BARRIER));
                this.setTitle(item, this.handlePlaceholders(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("nameFull", "&c{service} ({players}/{max_players})"), name, players, maxPlayers));
            } else {
                item.setType(Objects.requireNonNullElse(Material.getMaterial(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("type", "")), Material.EGG));
                this.setTitle(item, this.handlePlaceholders(this.configManager.getConfig().optJSONObject("serverItem", new JSONObject()).optString("name", "&a{service} (current) ({players}/{max_players})"), name, players, maxPlayers));
            }

            if (item.getItemMeta() == null) {
                item.setItemMeta(this.getServer().getItemFactory().getItemMeta(item.getType()));
            }

            ItemMeta meta = item.getItemMeta();

            List<String> lore = new ArrayList<>();
            lore.add(service.name());
            meta.setLore(lore);

            item.setItemMeta(meta);

            inventory.setItem(slot, item);

            slot++;
        }

        return inventory;
    }

    private void setTitle(ItemStack item, String name) {

        if (item.getItemMeta() == null) {
            item.setItemMeta(this.getServer().getItemFactory().getItemMeta(item.getType()));
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);

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

        serverItemConfig.put("type", Material.EGG.name());
        serverItemConfig.put("typeFull", Material.BARRIER.name());
        serverItemConfig.put("typeCurrent", Material.EMERALD.name());
        serverItemConfig.put("name", "&7{service} ({players}/{max_players})");
        serverItemConfig.put("nameFull", "&c{service} ({players}/{max_players})");
        serverItemConfig.put("nameCurrent", "&a{service} (current) ({players}/{max_players})");

        config.put("serverItem", serverItemConfig);

        return config;
    }
}