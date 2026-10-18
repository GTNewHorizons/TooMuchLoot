
package dmillerw.tml.data.chest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import dmillerw.tml.TooMuchLoot;
import dmillerw.tml.data.LootGroupXML;
import dmillerw.tml.data.LootLoadingMode;
import dmillerw.tml.data.XMLDataBridge;
import dmillerw.tml.helper.LogHelper;

/**
 * @author dmillerw
 */
public class ChestLootLoader {

    public static FilenameFilter FILTER = new FilenameFilter() {

        @Override
        public boolean accept(File file, String s) {
            return s.endsWith(".xml");
        }
    };

    public static HashMap<String, ChestGenHooks> copyLootTable(Map<String, ChestGenHooks> lootTable) {
        HashMap<String, ChestGenHooks> newLootTable = Maps.newHashMap();
        try {
            for (String key : lootTable.keySet()) {
                ChestGenHooks old = lootTable.get(key);
                List<WeightedRandomChestContent> copiedList = Lists.newArrayList();
                for (WeightedRandomChestContent chestContent : (List<WeightedRandomChestContent>) TooMuchLoot.contents
                        .get(old)) {
                    copiedList.add(
                            new WeightedRandomChestContent(
                                    chestContent.theItemId.copy(),
                                    chestContent.theMinimumChanceToGenerateItem,
                                    chestContent.theMaximumChanceToGenerateItem,
                                    chestContent.itemWeight));
                }
                newLootTable.put(
                        key,
                        new ChestGenHooks(
                                key,
                                copiedList.toArray(new WeightedRandomChestContent[copiedList.size()]),
                                old.getMin(),
                                old.getMax()));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not copy loot tables", ex);
        }
        return newLootTable;
    }

    /** Restores the startup snapshot, returning false and logging details if restoration fails. */
    public static boolean restoreCachedLootTable() {
        try {
            Map<String, ChestGenHooks> restored = copyLootTable(TooMuchLoot.lootTableCache);
            Map<String, ChestGenHooks> active = (Map<String, ChestGenHooks>) TooMuchLoot.chestInfo
                    .get(ChestGenHooks.class);
            // Forge owns a static final map; restore its contents rather than replacing the field.
            active.clear();
            active.putAll(restored);
            TooMuchLoot.chestGenCategories.clear();
            TooMuchLoot.chestGenCategories.addAll(restored.keySet());
            return true;
        } catch (Exception ex) {
            LogHelper.warn("Could not restore the original loot tables.", false);
            ex.printStackTrace();
            return false;
        }
    }

    /** Returns true only when every requested file was written; failures may leave partial output. */
    public static boolean generateFiles(File generationDir, String... categories) {
        if (!generationDir.isDirectory() && !generationDir.mkdirs()) {
            LogHelper.warn("Could not create loot export directory: " + generationDir, false);
            return false;
        }

        if (categories == null || categories.length == 0) {
            categories = TooMuchLoot.chestGenCategories.toArray(new String[0]);
        }

        boolean success = true;
        for (String key : categories) {
            try {
                String sanitizedKey = key.replaceAll("[^a-zA-Z0-9\\._]+", "_");

                File file = new File(generationDir, sanitizedKey + ".xml");
                ChestLootCategory lootCategory = ChestLootCategory.fromChestGenHooks(key);
                lootCategory.loading_mode = LootLoadingMode.OVERRIDE;
                for (ChestLootItem lootItem : lootCategory.loot) {
                    if (lootItem.nbt == null || (lootItem.nbt != null && lootItem.nbt.hasNoTags())) lootItem.nbt = null;
                }

                LootGroupXML tXMLObject = XMLDataBridge.parseToXMLObject(lootCategory);
                try (FileOutputStream output = new FileOutputStream(file, false)) {
                    JAXBContext tJaxbCtx = JAXBContext.newInstance(LootGroupXML.class);
                    Marshaller jaxMarsh = tJaxbCtx.createMarshaller();
                    jaxMarsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                    jaxMarsh.marshal(tXMLObject, output);
                }
            } catch (Exception ex) {
                success = false;
                LogHelper.warn("Could not export loot category: " + key, false);
                ex.printStackTrace();
            }
        }
        return success;
    }

    /**
     * Identifies a loot entry by item and metadata only, excluding stack size and weight
     */
    private static String lootKey(WeightedRandomChestContent content) {
        ItemStack stack = content.theItemId;
        Object name = Item.itemRegistry.getNameForObject(stack.getItem());
        return (name != null ? name.toString() : stack.getUnlocalizedName()) + "@" + stack.getItemDamage();
    }

    /** An entry with no item cannot be named or compared. Malformed entries are ignored rather than reported. */
    private static boolean isNamable(WeightedRandomChestContent content) {
        return content != null && content.theItemId != null && content.theItemId.getItem() != null;
    }

    /**
     * Names registered items absent from both the OVERRIDE and queued ADD entries.
     */
    private static void reportOverrideDiscards(Map<String, ChestGenHooks> tempMap,
            SetMultimap<String, WeightedRandomChestContent> toAdd) throws Exception {
        Map<String, ChestGenHooks> live = (Map<String, ChestGenHooks>) TooMuchLoot.chestInfo.get(ChestGenHooks.class);

        for (Map.Entry<String, ChestGenHooks> entry : tempMap.entrySet()) {
            ChestGenHooks registered = live.get(entry.getKey());
            if (registered == null) continue; // category is new; nothing to discard

            Set<String> kept = Sets.newHashSet();
            for (WeightedRandomChestContent content : (List<WeightedRandomChestContent>) TooMuchLoot.contents
                    .get(entry.getValue())) {
                if (isNamable(content)) kept.add(lootKey(content));
            }
            for (WeightedRandomChestContent content : toAdd.get(entry.getKey())) {
                if (isNamable(content)) kept.add(lootKey(content));
            }

            List<WeightedRandomChestContent> before = (List<WeightedRandomChestContent>) TooMuchLoot.contents
                    .get(registered);
            Set<String> lost = Sets.newLinkedHashSet(); // insertion order keeps the log reproducible
            for (WeightedRandomChestContent content : before) {
                if (!isNamable(content)) continue;
                String key = lootKey(content);
                if (!kept.contains(key)) lost.add(key);
            }

            if (!lost.isEmpty()) {
                LogHelper.logOverrideDiscards(entry.getKey(), lost);
            }
        }
    }

    /** Returns true only when all files were loaded and applied; failures may leave partial changes. */
    public static boolean loadFiles(File scanDir) {
        SetMultimap<String, WeightedRandomChestContent> toAdd = HashMultimap.create();
        SetMultimap<String, ItemStack> toRemove = HashMultimap.create();
        Map<String, ChestGenHooks> tempMap = Maps.newHashMap();

        File[] files = scanDir.listFiles(FILTER);
        if (files == null) {
            LogHelper.warn("Could not read loot directory: " + scanDir, false);
            return false;
        }

        boolean success = true;
        for (File file : files) {
            LogHelper.logParse(file.getName());

            try {
                JAXBContext tJaxbCtx = JAXBContext.newInstance(LootGroupXML.class);
                Unmarshaller jaxUnmarsh = tJaxbCtx.createUnmarshaller();

                LootGroupXML tXMLObject = null;
                tXMLObject = (LootGroupXML) jaxUnmarsh.unmarshal(file);

                ChestLootCategory lootCategory = XMLDataBridge.parseToChestLootCat(tXMLObject);
                if (lootCategory.category == null) {
                    success = false;
                    LogHelper.warn("Missing loot category in " + file.getName() + ". Skipping file.", false);
                    continue;
                }
                TooMuchLoot.chestGenCategories.add(lootCategory.category);

                for (ChestLootItem chestLootItem : lootCategory.loot) {
                    chestLootItem.checkCountValues();
                }

                if (lootCategory.loading_mode == LootLoadingMode.OVERRIDE) {
                    if (!tempMap.containsKey(lootCategory.category)) {
                        LogHelper.logOverride(lootCategory.category);
                        ChestGenHooks.getInfo(lootCategory.category); // Makes sure it exists
                        tempMap.put(lootCategory.category, lootCategory.toChestGenHooks());
                    } else {
                        success = false;
                        LogHelper.logOverrideError(lootCategory.category, file.getName());
                    }
                } else if (lootCategory.loading_mode == LootLoadingMode.ADD) {
                    for (ChestLootItem lootItem : lootCategory.loot) {
                        toAdd.put(lootCategory.category, lootItem.toChestContent());
                    }
                } else if (lootCategory.loading_mode == LootLoadingMode.REMOVE) {
                    for (ChestLootItem lootItem : lootCategory.loot) {
                        toRemove.put(lootCategory.category, lootItem.toItemStack());
                    }
                }
            } catch (Exception ex) {
                success = false;
                LogHelper.warn("Could not load loot file: " + file.getName(), false);
                ex.printStackTrace();
            }
        }

        try {
            reportOverrideDiscards(tempMap, toAdd);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            if (!tempMap.isEmpty()) {
                ((HashMap<String, ChestGenHooks>) TooMuchLoot.chestInfo.get(ChestGenHooks.class)).putAll(tempMap);
            }
        } catch (Exception ex) {
            success = false;
            LogHelper.warn("Could not apply loot overrides.", false);
            ex.printStackTrace();
        }

        for (Map.Entry<String, WeightedRandomChestContent> entry : toAdd.entries()) {
            try {
                LogHelper.logAddition(entry.getKey(), entry.getValue().theItemId.getDisplayName());
                ChestGenHooks.getInfo(entry.getKey()).addItem(entry.getValue());
            } catch (Exception ex) {
                success = false;
                LogHelper.warn("Could not add loot to category: " + entry.getKey(), false);
                ex.printStackTrace();
            }
        }

        for (Map.Entry<String, ItemStack> entry : toRemove.entries()) {
            try {
                LogHelper.logRemoval(entry.getKey(), entry.getValue().getDisplayName());
                ChestGenHooks.getInfo(entry.getKey()).removeItem(entry.getValue());
            } catch (Exception ex) {
                success = false;
                LogHelper.warn("Could not remove loot from category: " + entry.getKey(), false);
                ex.printStackTrace();
            }
        }
        return success;
    }
}
