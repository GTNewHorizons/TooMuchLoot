
package dmillerw.tml.data.chest;

import net.minecraft.item.ItemStack;

import com.ruling_0.materiallib.api.StackResolver;

import dmillerw.tml.helper.LogHelper;

/// Resolves the `ml:<Material>:<shape>` item ids MaterialLib-aware loot files carry. Such an entry names its item by
/// material and shape instead of by registry name and metadata, so it survives sessions that renumber MaterialLib's
/// metadata.
///
/// This is the only class holding MaterialLib types, so the rest of TooMuchLoot loads without MaterialLib installed.
/// Every caller checks `Loader.isModLoaded("materiallib")` before naming it.
public final class MaterialLibStacks {

    private static int resolved;
    private static int invalid;

    private MaterialLibStacks() {}

    /// The stack `id` names, carrying the metadata MaterialLib assigned the material this session, or null when the
    /// id is malformed or names nothing MaterialLib registers.
    public static ItemStack resolve(String id) {
        String[] parts = id.split(":");
        if (parts.length != 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
            LogHelper.warn(
                    String.format("Malformed MaterialLib item id [%s], expected ml:<Material>:<shape>", id),
                    false);
            invalid++;
            return null;
        }

        ItemStack stack = StackResolver.getStack(parts[1], parts[2], 1);
        if (stack == null) {
            invalid++;
            return null;
        }

        resolved++;
        return stack;
    }

    /// Reports the tally of the loot load pass that just finished and starts a fresh one.
    public static void logPass() {
        if (resolved + invalid > 0) {
            LogHelper.logMaterialLibPass(resolved, invalid);
        }
        resolved = 0;
        invalid = 0;
    }
}
