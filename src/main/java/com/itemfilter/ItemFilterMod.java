// TODO:
// - Add translation key instead of literals
package com.itemfilter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Set;
import net.minecraft.world.PersistentStateManager;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class ItemFilterMod implements ModInitializer {
    private static final String STATE_NAME = "item_filter";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("itemfilter")
                .then(literal("add")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            String playerUuid = player.getUuidAsString();
                            FilterState state = getFilterState(context.getSource().getServer());
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.literal("Your item filter is locked."), false);
                                return 0;
                            }
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (filter.contains(item)) {
                                player.sendMessage(Text.literal("Your " + mode + " already contains " + item.getName().getString() + "."), false);
                                return 0;
                            }
                            state.addFilter(playerUuid, item);
                            player.sendMessage(Text.literal("Added " + item.getName().getString() + " to your " + mode + "."), false);
                            return 1;
                        })
                    )
                )
                .then(literal("remove")
                    .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                            String playerUuid = player.getUuidAsString();
                            FilterState state = getFilterState(context.getSource().getServer());
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.literal("Your item filter is locked."), false);
                                return 0;
                            }
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (!filter.contains(item)) {
                                player.sendMessage(Text.literal("Your " + mode + " does not contain " + item.getName().getString() + "."), false);
                                return 0;
                            }
                            state.removeFilter(playerUuid, item);
                            player.sendMessage(Text.literal("Removed " + item.getName().getString() + " from your " + mode + "."), false);
                            return 1;
                        })
                    )
                )
                .then(literal("list")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        FilterState state = getFilterState(context.getSource().getServer());
                        String playerUuid = player.getUuidAsString();
                        String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                        if (state.getFilterHidden(playerUuid)) {
                            player.sendMessage(Text.literal("Your item filter is hidden."), false);
                            return 0;
                        }
                        Set<Item> filter = state.getFilter(playerUuid);
                        if (filter.isEmpty()) {
                            player.sendMessage(Text.literal("Your " + mode + " is empty."), false);
                        } else {
                            player.sendMessage(Text.literal("Your " + mode + "ed items:"), false);
                            for (Item item : filter) {
                                player.sendMessage(Text.literal("- " + item.getName().getString()), false);
                            }
                        }
                        return 1;
                    })
                )
                .then(literal("mode")
                    .then(literal("blacklist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.literal("Your item filter is locked."), false);
                                return 0;
                            }

                            if (state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.literal("You are already in blacklist mode; nothing changed."), false);
                            } else {
                                state.setFilterStatus(playerUuid, true);
                                player.sendMessage(Text.literal("Switched to blacklist mode."), false);
                            }

                            return 1;
                        })
                    )
                    .then(literal("whitelist")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            if (state.getFilterLocked(playerUuid)) {
                                player.sendMessage(Text.literal("Your item filter is locked."), false);
                                return 0;
                            }

                            if (!state.getFilterStatus(playerUuid)) {
                                player.sendMessage(Text.literal("You are already in whitelist mode; nothing changed."), false);
                            } else {
                                state.setFilterStatus(playerUuid, false);
                                player.sendMessage(Text.literal("Switched to whitelist mode."), false);
                            }

                            return 1;
                        })
                    )
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                        FilterState state = getFilterState(context.getSource().getServer());
                        String playerUuid = player.getUuidAsString();
                        String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                        if (state.getFilterHidden(playerUuid)) {
                            player.sendMessage(Text.literal("Your item filter is hidden."), false);
                            return 0;
                        }

                        player.sendMessage(Text.literal("Your item filter mode is set to " + mode + "."), false);

                        return 1;
                    })
                )
                .then(argument("player", EntityArgumentType.player())
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(literal("add")
                        .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                                String playerUuid = player.getUuidAsString();
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                                Set<Item> filter = state.getFilter(playerUuid);
                                if (filter.contains(item)) {
                                    player.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + " already contains " + item.getName().getString() + "."), false);
                                    return 0;
                                }
                                state.addFilter(playerUuid, item);
                                executor.sendMessage(Text.literal("Added " + item.getName().getString() + " to " + player.getName().getString() + "'s " + mode + "."), false);
                                return 1;
                            })
                        )
                    )
                    .then(literal("remove")
                        .then(argument("item", ItemStackArgumentType.itemStack(registryAccess))
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                Item item = ItemStackArgumentType.getItemStackArgument(context, "item").getItem();
                                String playerUuid = player.getUuidAsString();
                                FilterState state = getFilterState(context.getSource().getServer());
                                String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                                Set<Item> filter = state.getFilter(playerUuid);
                                if (!filter.contains(item)) {
                                    player.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + " does not contain " + item.getName().getString() + "."), false);
                                    return 0;
                                }
                                state.removeFilter(playerUuid, item);
                                executor.sendMessage(Text.literal("Removed " + item.getName().getString() + " from " + player.getName().getString() + "'s " + mode + "."), false);
                                return 1;
                            })
                        )
                    )
                    .then(literal("list")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";
                            Set<Item> filter = state.getFilter(playerUuid);
                            if (filter.isEmpty()) {
                                executor.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + " is empty."), false);
                            } else {
                                executor.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + "ed items:"), false);
                                for (Item item : filter) {
                                    executor.sendMessage(Text.literal("- " + item.getName().getString()), false);
                                }
                            }
                            return 1;
                        })
                    )
                    .then(literal("mode")
                        .then(literal("blacklist")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                FilterState state = getFilterState(context.getSource().getServer());
                                String playerUuid = player.getUuidAsString();

                                if (state.getFilterStatus(playerUuid)) {
                                    executor.sendMessage(Text.literal(player.getName().getString() + " is already in blacklist mode; nothing changed."), false);
                                } else {
                                    state.setFilterStatus(playerUuid, true);
                                    executor.sendMessage(Text.literal("Switched " + player.getName().getString() + " to blacklist mode."), false);
                                }

                                return 1;
                            })
                        )
                        .then(literal("whitelist")
                            .executes(context -> {
                                ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                                ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                                FilterState state = getFilterState(context.getSource().getServer());
                                String playerUuid = player.getUuidAsString();

                                if (!state.getFilterStatus(playerUuid)) {
                                    executor.sendMessage(Text.literal(player.getName().getString() + " is already in whitelist mode; nothing changed."), false);
                                } else {
                                    state.setFilterStatus(playerUuid, false);
                                    executor.sendMessage(Text.literal("Switched " + player.getName().getString() + " to whitelist mode."), false);
                                }

                                return 1;
                            })
                        )
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";

                            executor.sendMessage(Text.literal(player.getName().getString() + "'s item filter mode is set to " + mode + "."), false);

                            return 1;
                        })
                    )
                    .then(literal("lock")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";

                            state.setFilterLocked(playerUuid, true);
                            executor.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + " is now locked."), false);

                            return 1;
                        })
                    )
                    .then(literal("unlock")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";

                            state.setFilterLocked(playerUuid, false);
                            executor.sendMessage(Text.literal(player.getName().getString() + "'s " + mode + " is now unlocked."), false);

                            return 1;
                        })
                    )
                    .then(literal("hide")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";

                            state.setFilterHidden(playerUuid, true);
                            executor.sendMessage(Text.literal(player.getName().getString() + " can no longer view their " + mode + "."), false);

                            return 1;
                        })
                    )
                    .then(literal("show")
                        .executes(context -> {
                            ServerPlayerEntity executor = context.getSource().getPlayerOrThrow();
                            ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                            FilterState state = getFilterState(context.getSource().getServer());
                            String playerUuid = player.getUuidAsString();
                            String mode = state.getFilterStatus(playerUuid) ? "blacklist" : "whitelist";

                            state.setFilterHidden(playerUuid, false);
                            executor.sendMessage(Text.literal(player.getName().getString() + " can now view their " + mode + "."), false);

                            return 1;
                        })
                    )
                )
            );
        });
    }

    public static Set<Item> getPlayerFilter(ServerPlayerEntity player) {
        FilterState state = getFilterState(player.getServer());
        return state.getFilter(player.getUuidAsString());
    }

    public static FilterState getFilterState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getOverworld().getPersistentStateManager();
        return stateManager.getOrCreate(FilterState::fromNbt, FilterState::new, STATE_NAME);
    }
}