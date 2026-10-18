package dmillerw.tml.data.chest;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.Test;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.Side;
import dmillerw.tml.TooMuchLoot;

public class ChestLootLoaderOverrideTest {

    @Test
    public void queuedAdditionRetainsRegisteredItem() throws Throwable {
        runIsolated("addition");
    }

    @Test
    public void discardedRegisteredItemIsStillReported() throws Throwable {
        runIsolated("discard");
    }

    @Test
    public void overrideRetainsRegisteredItem() throws Throwable {
        runIsolated("retain");
    }

    private void runIsolated(String scenario) throws Throwable {
        ClassLoader previousContext = Thread.currentThread().getContextClassLoader();
        LaunchClassLoader previousLoader = Launch.classLoader;
        Map<String, Object> previousBlackboard = Launch.blackboard;
        PrintStream previousOut = System.out;
        PrintStream previousErr = System.err;
        // Forge's registries require a LaunchClassLoader, even for headless loader tests.
        try (LaunchClassLoader loader = new LaunchClassLoader(
                ((URLClassLoader) getClass().getClassLoader()).getURLs())) {
            Launch.classLoader = loader;
            Launch.blackboard = new HashMap<String, Object>();
            Thread.currentThread().setContextClassLoader(loader);
            loader.loadClass("cpw.mods.fml.common.Loader").getMethod("injectData", Object[].class).invoke(
                    null,
                    (Object) new Object[] { "7", "99", "40", "1614", "1.7.10", "9.05", new File("."),
                            new ArrayList<String>() });
            loader.loadClass(getClass().getName() + "$Fixture").getMethod("verify", String.class)
                    .invoke(null, scenario);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } finally {
            Thread.currentThread().setContextClassLoader(previousContext);
            Launch.classLoader = previousLoader;
            Launch.blackboard = previousBlackboard;
            System.setOut(previousOut);
            System.setErr(previousErr);
        }
    }

    public static class Fixture {

        public static void verify(String scenario) throws Exception {
            Field side = FMLRelaunchLog.class.getDeclaredField("side");
            side.setAccessible(true);
            side.set(null, Side.SERVER);
            Bootstrap.func_151354_b();
            TooMuchLoot.chestInfo = ChestGenHooks.class.getDeclaredField("chestInfo");
            TooMuchLoot.chestInfo.setAccessible(true);
            TooMuchLoot.contents = ChestGenHooks.class.getDeclaredField("contents");
            TooMuchLoot.contents.setAccessible(true);

            String category = "override.regression";
            ChestGenHooks.getInfo(category)
                    .addItem(new WeightedRandomChestContent(new ItemStack(Items.apple), 1, 1, 10));
            Path directory = Files.createTempDirectory("tml-override-regression-");
            WarningCapture capture = new WarningCapture();
            Logger logger = (Logger) LogManager.getLogger("FML");
            capture.start();
            logger.addAppender(capture);
            try {
                String overrideItem = scenario.equals("retain") ? "apple" : "diamond";
                writeLoot(directory.resolve("override.xml"), "OVERRIDE", overrideItem);
                if (scenario.equals("addition")) {
                    writeLoot(directory.resolve("add.xml"), "ADD", "apple");
                }

                if (!ChestLootLoader.loadFiles(directory.toFile())) {
                    throw new AssertionError("Valid XML must still load successfully");
                }
                String warning = capture.warnings.toString();
                if (scenario.equals("discard")) {
                    if (capture.warnings.size() != 1 || !warning.contains("minecraft:apple@0")) {
                        throw new AssertionError("Discarded apple must be reported: " + warning);
                    }
                } else if (!capture.warnings.isEmpty()) {
                    throw new AssertionError("Retained apple must not be reported as discarded: " + warning);
                }

                List<WeightedRandomChestContent> contents = (List<WeightedRandomChestContent>) TooMuchLoot.contents
                        .get(ChestGenHooks.getInfo(category));
                int expectedSize = scenario.equals("addition") ? 2 : 1;
                if (contents.size() != expectedSize || contents.get(0).theItemId.getItem()
                        != (scenario.equals("retain") ? Items.apple : Items.diamond)) {
                    throw new AssertionError("OVERRIDE contents or loading order changed");
                }
                if (scenario.equals("addition") && contents.get(1).theItemId.getItem() != Items.apple) {
                    throw new AssertionError("ADD must still be applied after OVERRIDE");
                }
                if (ChestGenHooks.getInfo(category).getMin() != 2 || ChestGenHooks.getInfo(category).getMax() != 3) {
                    throw new AssertionError("OVERRIDE count bounds changed");
                }
            } finally {
                logger.removeAppender(capture);
                capture.stop();
                try (Stream<Path> paths = Files.list(directory)) {
                    for (Path path : (Iterable<Path>) paths::iterator) {
                        Files.delete(path);
                    }
                }
                Files.delete(directory);
            }
        }

        private static void writeLoot(Path file, String mode, String item) throws Exception {
            String xml = "<LootGroup category=\"override.regression\" loading_mode=\"" + mode
                    + "\" count_min=\"2\" count_max=\"3\"><loot item=\"minecraft:"
                    + item
                    + "\" damage=\"0\" weight=\"10\" count_min=\"1\" count_max=\"1\"/></LootGroup>";
            Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static class WarningCapture extends AbstractAppender {

        private final List<String> warnings = new ArrayList<String>();

        private WarningCapture() {
            super("override-warning-regression", null, null);
        }

        @Override
        public void append(LogEvent event) {
            String message = event.getMessage().getFormattedMessage();
            if (message.contains("OVERRIDE of loot-table [override.regression]")) {
                warnings.add(message);
            }
        }
    }
}
