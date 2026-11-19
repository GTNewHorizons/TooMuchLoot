
package dmillerw.tml.helper;

import java.util.Collection;

import cpw.mods.fml.common.FMLLog;

/**
 * @author dmillerw
 */
public class LogHelper {

    private static final int SAMPLE_LIMIT = 3;

    public static boolean log = true;

    public static void warn(String msg, boolean big) {
        if (big) FMLLog.bigWarning("[TooMuchLoot]: %s", msg);
        else FMLLog.warning("[TooMuchLoot]: %s", msg);
    }

    public static void logParse(String file) {
        if (log) FMLLog.info("[TooMuchLoot]: Parsing [%s]", file);
    }

    public static void logOverride(String key) {
        if (log) FMLLog.info("[TooMuchLoot]: Overriding loot-table [%s]", key);
    }

    public static void logOverrideDiscards(String key, Collection<String> lost) {
        StringBuilder sample = new StringBuilder();
        int shown = 0;
        for (String item : lost) {
            if (shown == SAMPLE_LIMIT) {
                sample.append(", <").append(lost.size() - shown).append(" more>");
                break;
            }
            if (shown > 0) sample.append(", ");
            sample.append(item);
            shown++;
        }
        FMLLog.warning(
                "[TooMuchLoot]: OVERRIDE of loot-table [%s] discards %d ChestGenHooks-registered item(s) "
                        + "with no entry in the XML: %s",
                key,
                lost.size(),
                sample);
    }

    public static void logOverrideError(String key, String file) {
        if (log) FMLLog.warning(
                "[TooMuchLoot]: Failed to override loot-table [%s] as defined in [%s]. It has already been overridden!",
                key,
                file);
    }

    public static void logAddition(String key, String display) {
        if (log) FMLLog.info("[TooMuchLoot]: Adding [%s] to [%s]", display, key);
    }

    public static void logRemoval(String key, String display) {
        if (log) FMLLog.info("[TooMuchLoot]: Removed [%s] from [%s]", display, key);
    }
}
