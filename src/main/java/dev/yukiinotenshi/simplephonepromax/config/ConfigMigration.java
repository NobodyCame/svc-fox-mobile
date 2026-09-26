package dev.yukiinotenshi.simplephonepromax.config;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** Copies the previous installation's files once, leaving originals as a rollback copy. */
public final class ConfigMigration {
   private static final String OLD_ID = "simple-phone-pro-max-edition";
   private static final String NEW_ID = "svc-fox-mobile";

   private ConfigMigration() { }

   public static void run() {
      Path config = FabricLoader.getInstance().getConfigDir();
      copyIfMissing(config.resolve(OLD_ID + ".json"), config.resolve(NEW_ID + ".json"));
      copyIfMissing(config.resolve(OLD_ID + "-phone-numbers.json"), config.resolve(NEW_ID + "-phone-numbers.json"));
      copyIfMissing(config.resolve(OLD_ID + "-backend-cache.json"), config.resolve(NEW_ID + "-backend-cache.json"));
      copyTree(config.resolve(OLD_ID), config.resolve(NEW_ID));
   }

   private static void copyIfMissing(Path source, Path target) {
      try {
         if (Files.isRegularFile(source) && !Files.exists(target)) {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
         }
      } catch (IOException e) {
         SimpleVoiceCallClient.LOGGER.warn("Could not migrate a Fox Mobile settings file", e);
      }
   }

   private static void copyTree(Path source, Path target) {
      if (!Files.isDirectory(source)) return;
      try (var paths = Files.walk(source)) {
         paths.forEach(path -> {
            try {
               Path relative = source.relativize(path);
               Path destination = target.resolve(relative);
               if (Files.isDirectory(path)) Files.createDirectories(destination);
               else if (!Files.exists(destination)) Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
            } catch (IOException e) {
               SimpleVoiceCallClient.LOGGER.warn("Could not migrate Fox Mobile data: {}", path.getFileName());
            }
         });
      } catch (IOException e) {
         SimpleVoiceCallClient.LOGGER.warn("Could not migrate the previous mod data directory", e);
      }
   }
}

