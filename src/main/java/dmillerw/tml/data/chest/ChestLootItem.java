
package dmillerw.tml.data.chest;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.WeightedRandomChestContent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameData;

public class ChestLootItem {

    /// Marks an [#item] as a MaterialLib material and shape pair. Declared here, not on [MaterialLibStacks], so the
    /// gate in [#toItemStack] names no MaterialLib-holding class ahead of the mod check.
    private static final String MATERIALLIB_PREFIX = "ml:";

    public static ChestLootItem fromChestContent(WeightedRandomChestContent weightedRandomChestContent) {
        ChestLootItem lootItem = new ChestLootItem();
        lootItem.item = GameData.getItemRegistry().getNameForObject(weightedRandomChestContent.theItemId.getItem());
        lootItem.damage = weightedRandomChestContent.theItemId.getItemDamage();
        if (weightedRandomChestContent.theItemId.hasTagCompound())
            lootItem.nbt = weightedRandomChestContent.theItemId.getTagCompound();

        lootItem.weight = weightedRandomChestContent.itemWeight;
        lootItem.count_min = weightedRandomChestContent.theMinimumChanceToGenerateItem;
        lootItem.count_max = weightedRandomChestContent.theMaximumChanceToGenerateItem;
        return lootItem;
    }

    public String item;
    public int damage = 0;
    public NBTTagCompound nbt = null;
    public int weight;
    public int count_min = -1;
    public int count_max = -1;

    public ChestLootItem checkCountValues() {
        if (count_min == -1 && count_max == -1) {
            count_min = 1;
            count_max = 1;
        } else if (count_min == -1) {
            count_min = count_max;
        } else if (count_max == -1) {
            count_max = count_min;
        }

        return this;
    }

    /// The stack this entry names. A `ml:<Material>:<shape>` item takes the metadata MaterialLib assigned it this
    /// session, discarding [#damage]; one that resolves to nothing falls through to the unknown-item stack a bad
    /// registry name yields.
    public ItemStack toItemStack() {
        if (item != null && item.startsWith(MATERIALLIB_PREFIX) && Loader.isModLoaded("materiallib")) {
            ItemStack resolved = MaterialLibStacks.resolve(item);
            if (resolved != null) {
                if (nbt != null) resolved.setTagCompound(nbt);
                return resolved;
            }
        }

        ItemStack itemStack = new ItemStack(GameData.getItemRegistry().getObject(item), 1, damage);
        if (nbt != null) itemStack.setTagCompound(nbt);
        return itemStack;
    }

    public WeightedRandomChestContent toChestContent() {
        return new WeightedRandomChestContent(toItemStack(), count_min, count_max, weight);
    }
}
