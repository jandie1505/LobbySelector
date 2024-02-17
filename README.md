# LobbySelector
Displays CloudNet lobby services inside an inventory menu. Also supports silent lobbies.

## Requirements
- CloudNet v4 (v3 is not supported, tested with v4.0.0-RC9)
- A lobby task
- A lobby plugin which supports custom items that run commands when they are clicked (only if you want to open the lobby selector with an item)

## Installation
1. Download the plugin from the releases page
2. Put the plugin jar into the plugins folder of your lobby template
3. Restart a lobby service and copy the configuration from that service to your lobby template
4. Make your configuration changes
5. Restart all lobby services to apply the lobby selector to all services

## Commands
With the command `/lobbyselector`, the lobby selector menu can be opened.  There are no more commands.

## Permissions
| Permission | Description |
|--|--|
| `lobbyselector.use` | Allows the usage of the command `/lobbyselector`. |
| `lobbyselector.silentlobby` | Allows to see silent lobbies in the lobby selector menu. |

## Configuration
| Value | Description |
|--|--|
| `lobbyTask`         | The lobby task name.                                                                                                                                                                                                                   |
| `inventoryTitle`    | The name of the lobby selector menu/inventory.                                                                                                                                                                                         |
| `hideFullServices`  | When enabled, full services will be hidden from the lobby selector menu.                                                                                                                                                               |
| `enableSilentLobby` | When enabled, services from the task `silentLobbyTask` will also be displayed in the lobby selector menu.                                                                                                                              |
| `silentLobbyTask`   | The silent lobby task name. Services will only be shown when `enableSilentLobby` is set to `true`.                                                                                                                                     |
| `serverItems`       | A JSONObject with the following keys: `default` (Default Item), `full` (Full service), `silentHub` (SilentHub Service) and `current` (Current Service). The value for all of them is a JSONObject described in the ServerItem section. |

### ServerItem
| Value | Description |
|--|--|
| `material`        | The material of the service displayed in the lobby selector menu.                                                     |
| `name`            | The name of the service displayed in the lobby selector menu. Can contain placeholders from the placeholders section. |
| `lore`            | The item lore as a JSON Array of Strings (1 element = 1 line).                                                        |
| `enchanted`       | When set to true, the item has the enchantment glint effect.                                                          |
| `customModelData` | The custom model data of the item. Set to a negative value to disable.                                                |

### Placeholders
The following placeholders can be used in the config options `name`, `nameFull` and `nameCurrent`:
| Placeholder | Description |
|--|--|
| `{service}` | The name of that service. |
| `{players}` | The player count of that service. |
| `{max_players}` | The maximum player count of that service. |

## Setup for lobby plugins
Your lobby plugin needs to support custom items that run commands if they are clicked.  
The two most used lobby plugins (DeluxeHub and SuperLobbyDeluxe) do support this.  
Then you need to simply run the command `/lobbyselector` as the player who clicked the item.

## Known Issue: Nothing happens when clicking on server items
It can happen that if you click on a server item nothing will happen.
There is a CloudNet bug which causes this. Check if you are shown in /cloud players online (or players online in cloudnet console).
If this is not the case, the plugin can't move you because it uses the CloudNet API for that.
This CloudNet Bug is triggered when you use RC9 with a server version higher than 1.20.1 (also with ViaVersion).
You can read more about that bug [here](https://github.com/CloudNetService/CloudNet-v3/issues/1310).
