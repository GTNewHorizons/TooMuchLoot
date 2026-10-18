
package dmillerw.tml.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import dmillerw.tml.TooMuchLoot;
import dmillerw.tml.data.chest.ChestLootItem;
import dmillerw.tml.data.chest.ChestLootLoader;

/**
 * @author dmillerw
 */
public class CommandChestLoot extends CommandBase {

    @Override
    public String getCommandName() {
        return "loot";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender commandSender) {
        return "/loot <add|remove|generate|reload|reset|spawnDebugChest>";
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        String[] categories = TooMuchLoot.chestGenCategories.toArray(new String[0]);
        if (args.length == 1) return getListOfStringsMatchingLastWord(
                args,
                "add",
                "remove",
                "generate",
                "reload",
                "reset",
                "spawnDebugChest");
        if (args.length > 1) {
            switch (args[0].toLowerCase()) {
                case "generate":
                    return getListOfStringsMatchingLastWord(args, categories);
                case "spawndebugchest":
                case "add":
                case "remove":
                    if (args.length == 2) return getListOfStringsMatchingLastWord(args, categories);
                default:
                    return null;
            }
        }
        return null;
    }

    @Override
    public void processCommand(ICommandSender commandSender, String[] args) {
        if (args.length < 1) throw new WrongUsageException(getCommandUsage(commandSender));

        EntityPlayerMP entityPlayerMP;
        ItemStack item;
        String tag;
        switch (args[0].toLowerCase()) {
            case "reload":
                if (args.length != 1) throw new WrongUsageException("/loot reload");
                if (!ChestLootLoader.restoreCachedLootTable()) {
                    throw new CommandException("Could not restore the original loot tables. Check the server log.");
                }
                if (!ChestLootLoader.loadFiles(TooMuchLoot.lootFolder)) {
                    throw new CommandException(
                            "Could not load all loot files. Some changes may have been applied; check the server log.");
                }
                commandSender.addChatMessage(new ChatComponentText("Reloaded custom loot tables."));
                break;
            case "generate":
                for (int i = 1; i < args.length; i++) {
                    getCategory(args[i]);
                }
                if (!ChestLootLoader
                        .generateFiles(TooMuchLoot.generatedFolder, Arrays.copyOfRange(args, 1, args.length))) {
                    throw new CommandException(
                            "Could not export all loot files. Some files may have been written; check the server log.");
                }
                commandSender.addChatMessage(
                        new ChatComponentText(
                                "Exported loot tables to " + TooMuchLoot.generatedFolder.getAbsolutePath()));
                break;
            case "add":
                item = getCommandSenderAsPlayer(commandSender).getCurrentEquippedItem();
                if (item == null || args.length < 2 || args.length > 5) {
                    throw new WrongUsageException(
                            "/loot add <category> [weight] [min] [max] -- adds current item in hand to loot category");
                }
                ChestLootItem clItem = ChestLootItem.fromItemStack(item);
                tag = args[1];
                clItem.weight = (args.length > 2) ? parseIntWithMin(commandSender, args[2], 1) : 10;
                clItem.count_min = (args.length > 3) ? parseIntWithMin(commandSender, args[3], 0) : 1;
                clItem.count_max = (args.length > 4) ? parseIntBounded(
                        commandSender,
                        args[4],
                        clItem.count_min,
                        clItem.count_min == 0 ? Integer.MAX_VALUE - 1 : Integer.MAX_VALUE) : clItem.count_min;
                ChestGenHooks category = ChestGenHooks.getInfo(tag);
                long totalWeight = clItem.weight;
                for (WeightedRandomChestContent content : getContents(category)) {
                    totalWeight += content.itemWeight;
                }
                if (totalWeight > Integer.MAX_VALUE) {
                    throw new CommandException("Total loot category weight must not exceed " + Integer.MAX_VALUE);
                }
                category.addItem(clItem.toChestContent());
                TooMuchLoot.chestGenCategories.add(tag); // getInfo(tag) adds new tags if they don't exist, so make them
                                                         // visible
                commandSender.addChatMessage(
                        new ChatComponentText("Added " + item.getDisplayName() + " to loot category '" + tag + "'."));
                break;
            case "remove":
                item = getCommandSenderAsPlayer(commandSender).getCurrentEquippedItem();
                if (item == null || args.length != 2) {
                    throw new WrongUsageException("/loot remove <category> -- removes item in hand from loot category");
                }
                tag = args[1];
                ChestGenHooks removeCategory = getCategory(tag);
                List<WeightedRandomChestContent> contentsBeforeRemoval = getContents(removeCategory);
                int previousSize = contentsBeforeRemoval.size();
                removeCategory.removeItem(item);
                if (previousSize == contentsBeforeRemoval.size()) {
                    throw new CommandException("No matching item was found in loot category '%s'.", tag);
                }
                commandSender.addChatMessage(
                        new ChatComponentText(
                                "Removed " + item.getDisplayName() + " from loot category '" + tag + "'."));
                break;
            case "reset":
                if (args.length != 1) throw new WrongUsageException("/loot reset");
                if (!ChestLootLoader.restoreCachedLootTable()) {
                    throw new CommandException("Could not restore the original loot tables. Check the server log.");
                }
                commandSender.addChatMessage(new ChatComponentText("Restored the original loot tables."));
                break;
            case "spawndebugchest":
                if (args.length < 2 || args.length > 5)
                    throw new WrongUsageException("usage: /loot spawnDebugChest <category> [x] [y] [z]");
                entityPlayerMP = getCommandSenderAsPlayer(commandSender);
                int x = (int) Math
                        .floor(func_110666_a(commandSender, entityPlayerMP.posX, args.length > 2 ? args[2] : "~"));
                int y = (int) Math.floor(
                        func_110665_a(commandSender, entityPlayerMP.posY, args.length > 3 ? args[3] : "~", 0, 0));
                int z = (int) Math
                        .floor(func_110666_a(commandSender, entityPlayerMP.posZ, args.length > 4 ? args[4] : "~"));
                tag = args[1];
                getCategory(tag);

                Random random = new Random();
                WeightedRandomChestContent[] contents = ChestGenHooks.getItems(tag, random);
                int count = ChestGenHooks.getCount(tag, random);

                if (!entityPlayerMP.worldObj.setBlock(x, y, z, Blocks.chest, 0, 2)) {
                    throw new CommandException("Could not place a loot chest at %s, %s, %s.", x, y, z);
                }
                TileEntityChest tileEntity = (TileEntityChest) entityPlayerMP.worldObj.getTileEntity(x, y, z);
                if (tileEntity != null) {
                    WeightedRandomChestContent.generateChestContents(random, contents, tileEntity, count);
                } else {
                    throw new CommandException("Could not find chest!");
                }
                commandSender.addChatMessage(
                        new ChatComponentText(
                                "Created loot chest for '" + tag + "' at " + x + ", " + y + ", " + z + "."));
                break;
            default:
                throw new WrongUsageException(getCommandUsage(commandSender));
        }
    }

    private static ChestGenHooks getCategory(String tag) {
        try {
            ChestGenHooks category = ((Map<String, ChestGenHooks>) TooMuchLoot.chestInfo.get(ChestGenHooks.class))
                    .get(tag);
            if (category == null) throw new CommandException("Unknown loot category '%s'.", tag);
            return category;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new CommandException("Could not read loot categories. Check the server log.");
        }
    }

    private static List<WeightedRandomChestContent> getContents(ChestGenHooks category) {
        try {
            return (List<WeightedRandomChestContent>) TooMuchLoot.contents.get(category);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new CommandException("Could not read loot category contents. Check the server log.");
        }
    }
}
