
package dmillerw.tml.command;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
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

        EntityPlayerMP entityPlayerMP = getCommandSenderAsPlayer(commandSender);
        ItemStack item = entityPlayerMP.getCurrentEquippedItem();
        String tag;
        switch (args[0].toLowerCase()) {
            case "reload":
                ChestLootLoader.restoreCachedLootTable();
                ChestLootLoader.loadFiles(TooMuchLoot.lootFolder);
                break;
            case "generate":
                ChestLootLoader.generateFiles(TooMuchLoot.generatedFolder, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "add":
                if (item == null || args.length == 1) {
                    throw new WrongUsageException(
                            "/loot add <category> [weight] [min] [max] -- adds current item in hand to loot category");
                }
                ChestLootItem clItem = ChestLootItem.fromItemStack(item);
                tag = args[1];
                clItem.weight = (args.length > 2) ? Integer.parseInt(args[2]) : 10;
                clItem.count_min = (args.length > 3) ? Integer.parseInt(args[3]) : 1;
                clItem.count_max = (args.length > 4) ? Integer.parseInt(args[4]) : 1;
                ChestGenHooks.getInfo(tag).addItem(clItem.toChestContent());
                TooMuchLoot.chestGenCategories.add(tag); // getInfo(tag) adds new tags if they don't exist, so make them
                                                         // visible
                break;
            case "remove":
                if (item == null || args.length == 1) {
                    throw new WrongUsageException("/loot remove <category> -- removes item in hand from loot category");
                }
                tag = args[1];
                ChestGenHooks.getInfo(tag).removeItem(item);
                break;
            case "reset":
                ChestLootLoader.restoreCachedLootTable();
                break;
            case "spawndebugchest":
                if (args.length == 1)
                    throw new WrongUsageException("usage: /loot spawnDebugChest <category> [x] [y] [z]");
                int x = (int) Math.floor(func_110666_a(commandSender, entityPlayerMP.posX, "~"));
                int y = (int) Math.floor(func_110665_a(commandSender, entityPlayerMP.posY, "~", 0, 0));
                int z = (int) Math.floor(func_110666_a(commandSender, entityPlayerMP.posZ, "~"));
                tag = args[1];

                if (args.length == 5) {
                    x = (int) Math.floor(func_110666_a(commandSender, entityPlayerMP.posX, args[2]));
                    y = (int) Math.floor(func_110665_a(commandSender, entityPlayerMP.posY, args[3], 0, 0));
                    z = (int) Math.floor(func_110666_a(commandSender, entityPlayerMP.posZ, args[4]));
                }

                Random random = new Random();
                WeightedRandomChestContent[] contents = ChestGenHooks.getItems(tag, random);
                int count = ChestGenHooks.getCount(tag, random);

                entityPlayerMP.worldObj.setBlock(x, y, z, Blocks.chest, 0, 2);
                TileEntityChest tileEntity = (TileEntityChest) entityPlayerMP.worldObj.getTileEntity(x, y, z);
                if (tileEntity != null) {
                    WeightedRandomChestContent.generateChestContents(random, contents, tileEntity, count);
                } else {
                    throw new CommandException("Could not find chest!");
                }
                break;
            default:
                throw new WrongUsageException(getCommandUsage(commandSender));
        }
    }
}
