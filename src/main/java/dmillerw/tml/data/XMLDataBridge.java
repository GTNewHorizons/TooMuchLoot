
package dmillerw.tml.data;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;

import dmillerw.tml.data.LootGroupXML.LootEntry;
import dmillerw.tml.data.chest.ChestLootCategory;
import dmillerw.tml.data.chest.ChestLootItem;
import dmillerw.tml.helper.LogHelper;

public class XMLDataBridge {

    public static LootGroupXML parseToXMLObject(ChestLootCategory pCat) {
        LootGroupXML tGrp = new LootGroupXML();

        tGrp.mCategory = pCat.category;
        tGrp.mCount_max = pCat.count_max;
        tGrp.mCount_min = pCat.count_min;
        tGrp.mLoading_mode = pCat.loading_mode.toString();

        for (ChestLootItem tIt : pCat.loot) {
            LootEntry tEntry = new LootEntry();
            tEntry.mCount_max = tIt.count_max;
            tEntry.mCount_min = tIt.count_min;
            tEntry.mDamage = tIt.damage;
            tEntry.mIdentifier = tIt.item;
            tEntry.mWeight = tIt.weight;

            if (tIt.nbt != null) tEntry.mNBTTag = tIt.nbt.toString();

            tGrp.getLoots().add(tEntry);
        }

        return tGrp;
    }

    public static ChestLootCategory parseToChestLootCat(LootGroupXML pXMLObject) {
        ChestLootCategory tGrp = new ChestLootCategory();

        tGrp.category = pXMLObject.mCategory;
        tGrp.count_max = pXMLObject.mCount_max;
        tGrp.count_min = pXMLObject.mCount_min;
        try {
            tGrp.loading_mode = LootLoadingMode.valueOf(pXMLObject.mLoading_mode);
        } catch (Exception e) {
            tGrp.loading_mode = LootLoadingMode.OVERRIDE;
            LogHelper.warn(
                    String.format(
                            "Invalid Loading mode found in LootGroup file %s. Defaulting to OVERRIDE",
                            tGrp.category),
                    false);
        }

        List<ChestLootItem> tLootItems = new ArrayList<ChestLootItem>();
        for (LootEntry tIt : pXMLObject.getLoots()) {
            ChestLootItem tEntry = new ChestLootItem();
            tEntry.count_max = tIt.mCount_max;
            tEntry.count_min = tIt.mCount_min;
            tEntry.damage = tIt.mDamage;
            tEntry.item = tIt.mIdentifier;

            try {
                if (tIt.mNBTTag != null && !tIt.mNBTTag.isEmpty())
                    tEntry.nbt = (NBTTagCompound) JsonToNBT.func_150315_a(tIt.mNBTTag);
            } catch (Exception e) {
                LogHelper.warn(
                        String.format(
                                "Invalid NBT Tag found in LootGroup file %s, item %s",
                                tGrp.category,
                                tIt.mIdentifier),
                        false);
                tEntry.nbt = null;
            }

            tEntry.weight = tIt.mWeight;

            tLootItems.add(tEntry);
        }

        tGrp.loot = new ChestLootItem[tLootItems.size()];
        tLootItems.toArray(tGrp.loot);
        return tGrp;
    }
}
