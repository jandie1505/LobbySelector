# LobbySelector
Displays CloudNet lobby services inside an inventory menu. Also supports silent lobbies.

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
| `lobbyTask` | The lobby task name. |
| `inventoryTitle` | The name of the lobby selector menu/inventory. |
| `hideFullServices` | When enabled, full services will be hidden from the lobby selector menu. |
| `enableSilentLobby` | When enabled, services from the task `silentLobbyTask` will also be displayed in the lobby selector menu. |
| `silentLobbyTask` | The silent lobby task name. Services will only be shown when `enableSilentLobby` is set to `true`. |
| `serverItem` | See ServerItem |

### ServerItem
| Value | Description |
|--|--|
| `type` | The material of a service displayed in the lobby selector menu which is neither full nor the current service. |
| `typeFull` | The material of a full service displayed in the lobby selector menu. |
| `typeCurrent` | The material of the current service displayed in the lobby selector menu. |
| `name` | The item name of a service displayed in the lobby selector menu which is neither full nor the current service. |
| `nameFull` | The item name of a full service displayed in the lobby selector menu. |
| `nameCurrent` | The item name of the current service displayed in the lobby selector menu. |

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
