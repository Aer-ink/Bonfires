package uk.co.wehavecookies56.bonfires;


import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.DimensionManager;
import uk.co.wehavecookies56.bonfires.world.BonfireWorldSavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Created by Toby on 17/12/2016.
 */
class CommandBonfires extends CommandBase {

    @Override
    @Nonnull
    public List<String> getAliases() {
        List<String> aliases = new ArrayList<>();
        aliases.add("bonfires");
        aliases.add("bonfirelist");
        return aliases;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    @Nonnull
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos) {
        List<String> filters = new ArrayList<>();
        filters.add("all");
        filters.add("dim");
        filters.add("name");
        filters.add("owner");
        filters.add("radius");
        if (args.length == 1 && !filters.contains(args[0])) {
            return filters;
        } else if (args.length == 2 && args[1].isEmpty()) {
            if (args[0].equals("dim")) {
                List<Integer> dimList = new ArrayList<>(Arrays.asList(DimensionManager.getStaticDimensionIDs()));
                dimList = Lists.reverse(dimList);
                Function<Integer, String> toString = input -> toString();
                return Lists.transform(dimList, toString::apply);
            }
            else if (args[0].equals("owner")) {
                if (sender.getServer() != null)
                    return new ArrayList<>(Arrays.asList(sender.getServer().getPlayerProfileCache().getUsernames()));
            }
        }
        return super.getTabCompletions(server, sender, args, pos);
    }

    @Override
    @Nonnull
    public String getName() {
        return "bonfires";
    }

    @Override
    @Nonnull
    public String getUsage(@Nonnull ICommandSender sender) {
        return LocalStrings.COMMAND_BONFIRES_USAGE;
    }

    private static void listQueriedBonfires(List<Bonfire> query, ICommandSender sender) {
        query.forEach((bonfires -> {
            if (sender.getServer()!= null) {
                GameProfile owner = sender.getServer().getPlayerProfileCache().getProfileByUUID(bonfires.getOwner());
                String name = new TextComponentTranslation(LocalStrings.COMMAND_NA).getUnformattedComponentText();
                if (owner != null) {
                    name = owner.getName();
                }
                TextComponentTranslation messageName = new TextComponentTranslation(LocalStrings.COMMAND_NAME, bonfires.getName());
                TextComponentTranslation messageID = new TextComponentTranslation(LocalStrings.COMMAND_ID, bonfires.getId());
                TextComponentTranslation messageOwner = new TextComponentTranslation(LocalStrings.COMMAND_OWNER, name);
                TextComponentTranslation messagePos = new TextComponentTranslation(LocalStrings.COMMAND_POS, bonfires.getPos().getX(), bonfires.getPos().getY(), bonfires.getPos().getZ());

                sender.sendMessage(new TextComponentString(messageName.getUnformattedText() + " " + messageID.getUnformattedText() + " " + messageOwner.getUnformattedText() + " " + messagePos.getUnformattedText()));
            }
        }));
    }

    @Override
    public void execute(@Nonnull MinecraftServer server, @Nonnull ICommandSender sender, @Nonnull String[] args) throws CommandException {
        if (args.length == 2) {
            if (args[0].toLowerCase().equals("dim")) {
                try {
                    if (DimensionManager.isDimensionRegistered(Integer.parseInt(args[1]))) {
                        List<Bonfire> query = BonfireWorldSavedData.get(server.getEntityWorld()).bonfires.getBonfiresByDimension(Integer.parseInt(args[1]));
                        if (query.isEmpty()) {
                            TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_DIM_NOMATCH, Integer.parseInt(args[1]));
                            message.getStyle().setColor(TextFormatting.RED);
                            sender.sendMessage(message);
                        } else {
                            if (sender.getServer() != null) {
                                TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_DIM_MATCH, query.size(), sender.getServer().getWorld(Integer.parseInt(args[1])).provider.getDimensionType().getName() + "(" + args[1] + ")");
                                sender.sendMessage(message);
                                listQueriedBonfires(query, sender);
                            }
                        }
                    } else {
                        TextComponentTranslation error = new TextComponentTranslation(LocalStrings.COMMAND_DIM_NODIM, Integer.parseInt(args[1]));
                        error.getStyle().setColor(TextFormatting.DARK_RED);
                        sender.sendMessage(error);
                    }
                } catch (NumberFormatException e) {
                    TextComponentTranslation error = new TextComponentTranslation(LocalStrings.COMMAND_DIM_INVALID, args[1]);
                    error.getStyle().setColor(TextFormatting.DARK_RED);
                    sender.sendMessage(error);
                }

            } else if (args[0].toLowerCase().equals("owner")) {
                if (sender.getServer() != null) {
                    GameProfile ownerProfile = sender.getServer().getPlayerProfileCache().getGameProfileForUsername(args[1]);
                    if (ownerProfile != null) {
                        UUID ownerID = ownerProfile.getId();
                        if (ownerID != null) {
                            List<Bonfire> query = BonfireWorldSavedData.get(server.getEntityWorld()).bonfires.getBonfiresByOwner(ownerID);
                            if (query.isEmpty()) {
                                TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_NOMATCH, args[1]);
                                message.getStyle().setColor(TextFormatting.RED);
                                sender.sendMessage(message);
                            } else {
                                TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_MATCH, query.size(), args[1]);
                                sender.sendMessage(message);
                                listQueriedBonfires(query, sender);
                            }
                        } else {
                            TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_NOUSER, args[1]);
                            message.getStyle().setColor(TextFormatting.DARK_RED);
                            sender.sendMessage(message);
                        }
                    }
                }
            } else if (args[0].toLowerCase().equals("name")) {
                List<Bonfire> query = BonfireWorldSavedData.get(server.getEntityWorld()).bonfires.getBonfiresByName(args[1]);
                if (query.isEmpty()) {
                    TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_NOMATCH, args[1]);
                    message.getStyle().setColor(TextFormatting.RED);
                    sender.sendMessage(message);
                } else {
                    TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_MATCH, query.size(), args[1]);
                    sender.sendMessage(message);
                    listQueriedBonfires(query, sender);
                }
            } else if (args[0].toLowerCase().equals("radius")) {
                try {
                    List<Bonfire> query = BonfireWorldSavedData.get(server.getEntityWorld()).bonfires.getBonfiresInRadius(sender.getPosition(), Integer.parseInt(args[1]), sender.getEntityWorld().provider.getDimension());
                    if (query.isEmpty()) {
                        TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_RADIUS_NOMATCH, args[1]);
                        message.getStyle().setColor(TextFormatting.RED);
                        sender.sendMessage(message);
                    } else {
                        TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_RADIUS_MATCH, query.size(), args[1]);
                        sender.sendMessage(message);
                        listQueriedBonfires(query, sender);
                    }
                } catch (NumberFormatException e) {
                    TextComponentTranslation error = new TextComponentTranslation(LocalStrings.COMMAND_RADIUS_INVALID, args[1]);
                    error.getStyle().setColor(TextFormatting.DARK_RED);
                    sender.sendMessage(error);
                }
            } else {
                TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_FILTER_INVALID);
                message.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(message);
            }
        } else if (args.length == 1) {
            if (args[0].toLowerCase().equals("filters")) {
                sender.sendMessage(new TextComponentTranslation(LocalStrings.COMMAND_ALL_DESC, "all"));
                sender.sendMessage(new TextComponentTranslation(LocalStrings.COMMAND_DIM_DESC, "dim"));
                sender.sendMessage(new TextComponentTranslation(LocalStrings.COMMAND_NAME_DESC, "name"));
                sender.sendMessage(new TextComponentTranslation(LocalStrings.COMMAND_OWNER_DESC, "owner"));
                sender.sendMessage(new TextComponentTranslation(LocalStrings.COMMAND_RADIUS_DESC, "radius"));
            } else if (args[0].toLowerCase().equals("all")) {
                List<Bonfire> query = new ArrayList<>(BonfireWorldSavedData.get(server.getEntityWorld()).bonfires.getBonfires().values());
                if (query.isEmpty()) {
                    TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_NOMATCH, "all");
                    message.getStyle().setColor(TextFormatting.RED);
                    sender.sendMessage(message);
                } else {
                    TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_MATCH, query.size(), "all");
                    sender.sendMessage(message);
                    listQueriedBonfires(query, sender);
                }
            } else {
                TextComponentTranslation message = new TextComponentTranslation(LocalStrings.COMMAND_FILTER_INVALID);
                message.getStyle().setColor(TextFormatting.RED);
                sender.sendMessage(message);
            }
        } else {
            TextComponentTranslation message = new TextComponentTranslation(getUsage(sender));
            message.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(message);
        }
    }
}
