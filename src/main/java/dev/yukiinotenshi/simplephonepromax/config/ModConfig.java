package dev.yukiinotenshi.simplephonepromax.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModConfig {
   private static final Logger LOGGER = LoggerFactory.getLogger("Fox Mobile");
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String CONFIG_FILE_NAME = "svc-fox-mobile.json";
   private static final String DEFAULT_BACKEND_URL = dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.URL;
   public String wallpaper = "default";
   public String ringtone = "nokia";
   public String notificationSound="chaos";public float notificationVolume=.7f;
   public float ringtoneVolume = 0.8f;
   public float offlineVolume=.7f, endedVolume=.7f, voicemailVolume=.7f, invalidNumberVolume=.7f;
   public float dialingVolume = 0.7f, busyVolume = 0.7f, unavailableVolume = 0.7f;
   public float waitingVolume = 0.5f;
   public String waitingRingtone = "digital";
   public String doNotDisturb = "all";
   public boolean quietSchedule = false;
   public int quietStartHour = 23, quietEndHour = 8;
   public boolean allowCallWaiting = true;
   public java.util.Map<String, RingtonePreference> contactRingtones = new java.util.HashMap<>();
   public List<ModConfig.Contact> contacts = new ArrayList<>();
   public List<String> metPlayers = new ArrayList<>();
   public List<ModConfig.CallHistoryEntry> callHistory = new ArrayList<>();
   public boolean useCustomWallpaper = false;
   public String customWallpaper = "";
   public boolean useCustomRingtone = false;
   public String customRingtone = "";
   public boolean transmitRingtoneInCall = false;
   public boolean muted = false;
   public boolean passwordProtectedCalls = true;
   public boolean passwordProtectedCallsInitialized = false;
   public boolean backendNumbersEnabled = true;
   public boolean showOwnNumberInDirectory = false;
   public boolean legacyNumberMode = false;
   public String callStandard = "fox";
   public String backendBaseUrl = DEFAULT_BACKEND_URL;
   public String callControlSecret = "";
   public String backendDeviceId = "";
   public String backendAdminToken = "";
   public String backendCachedNumber = "";
   public String backendStatus = "not_configured";
   public java.util.Map<String,Long> temporaryMutes=new java.util.HashMap<>();
   public java.util.Map<String,String> lastDialedNumbers=new java.util.HashMap<>();
   public List<String> blockedPlayers = new ArrayList<>();
   public List<String> favoriteContacts = new ArrayList<>();
   public boolean useGradientWallpaper = false;
   public int gradientTopLeft = -871038925;
   public int gradientTopRight = -869072794;
   public int gradientBottomLeft = -865717334;
   public int gradientBottomRight = -861243393;

   private static float clampVolume(float v) { return Float.isFinite(v) ? Math.max(0, Math.min(1,v)) : 0.7f; }

   public void load() {
      File configDir = FabricLoader.getInstance().getConfigDir().toFile();
      if (!configDir.exists()) {
         configDir.mkdirs();
      }

      File configFile = new File(configDir, CONFIG_FILE_NAME);
      boolean backendAddressMigrated = false;
      boolean passwordSettingMigrated = false;
      if (configFile.exists()) {
         try (FileReader reader = new FileReader(configFile)) {
            ModConfig loaded = (ModConfig)GSON.fromJson(reader, ModConfig.class);
            if (loaded != null) {
               this.offlineVolume=clampVolume(loaded.offlineVolume);this.endedVolume=clampVolume(loaded.endedVolume);this.voicemailVolume=clampVolume(loaded.voicemailVolume);this.invalidNumberVolume=clampVolume(loaded.invalidNumberVolume);
               this.dialingVolume = clampVolume(loaded.dialingVolume);
               this.busyVolume = clampVolume(loaded.busyVolume);
               this.unavailableVolume = clampVolume(loaded.unavailableVolume);
               this.notificationSound=loaded.notificationSound==null?"chaos":loaded.notificationSound;this.notificationVolume=clampVolume(loaded.notificationVolume);
               this.ringtoneVolume = Math.max(0,Math.min(1,loaded.ringtoneVolume));
               this.waitingVolume = Math.max(0,Math.min(1,loaded.waitingVolume));
               this.waitingRingtone = loaded.waitingRingtone != null ? loaded.waitingRingtone : "digital";
               this.doNotDisturb = loaded.doNotDisturb != null ? loaded.doNotDisturb : "all";
               this.quietSchedule = loaded.quietSchedule;this.quietStartHour=Math.floorMod(loaded.quietStartHour,24);this.quietEndHour=Math.floorMod(loaded.quietEndHour,24);
               this.allowCallWaiting = loaded.allowCallWaiting;
               this.transmitRingtoneInCall = loaded.transmitRingtoneInCall;
               this.contactRingtones = loaded.contactRingtones != null ? loaded.contactRingtones : new java.util.HashMap<>();
               this.wallpaper = loaded.wallpaper != null ? loaded.wallpaper : "default";
               this.ringtone = loaded.ringtone != null && !java.util.Set.of("default","classic","digital").contains(loaded.ringtone) ? loaded.ringtone : "nokia";
               this.contacts = loaded.contacts != null ? loaded.contacts : new ArrayList<>();
               this.metPlayers = loaded.metPlayers != null ? loaded.metPlayers : new ArrayList<>();
               this.useCustomWallpaper = loaded.useCustomWallpaper;
               this.customWallpaper = loaded.customWallpaper != null ? loaded.customWallpaper : "";
               this.useCustomRingtone = loaded.useCustomRingtone;
               this.customRingtone = loaded.customRingtone != null ? loaded.customRingtone : "";
               this.muted = loaded.muted;
               this.passwordProtectedCalls = loaded.passwordProtectedCallsInitialized ? loaded.passwordProtectedCalls : true;
               passwordSettingMigrated = !loaded.passwordProtectedCallsInitialized;
               this.passwordProtectedCallsInitialized = true;
               this.backendNumbersEnabled = loaded.backendNumbersEnabled;
               this.showOwnNumberInDirectory = loaded.showOwnNumberInDirectory;
               this.legacyNumberMode = loaded.legacyNumberMode;
               this.callStandard = "legacy".equals(loaded.callStandard) || "yoghurt".equals(loaded.callStandard) ? "legacy" : "fox";
               String previousBackendUrl=loaded.backendBaseUrl;
               this.backendBaseUrl = dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.upgrade(previousBackendUrl != null && !previousBackendUrl.isBlank() ? previousBackendUrl : DEFAULT_BACKEND_URL);
               backendAddressMigrated=!java.util.Objects.equals(previousBackendUrl,this.backendBaseUrl);
               this.callControlSecret = loaded.callControlSecret != null ? loaded.callControlSecret : "";
               this.backendDeviceId = loaded.backendDeviceId != null ? loaded.backendDeviceId : "";
               this.backendAdminToken = loaded.backendAdminToken != null ? loaded.backendAdminToken : "";
               this.backendCachedNumber = loaded.backendCachedNumber != null ? loaded.backendCachedNumber : "";
               this.backendStatus = loaded.backendStatus != null ? loaded.backendStatus : "not_configured";
               this.temporaryMutes=loaded.temporaryMutes!=null?loaded.temporaryMutes:new java.util.HashMap<>();
               this.lastDialedNumbers=loaded.lastDialedNumbers!=null?loaded.lastDialedNumbers:new java.util.HashMap<>();
               this.blockedPlayers = loaded.blockedPlayers != null ? loaded.blockedPlayers : new ArrayList<>();
               this.favoriteContacts = loaded.favoriteContacts != null ? loaded.favoriteContacts : new ArrayList<>();
               this.callHistory = loaded.callHistory != null ? loaded.callHistory : new ArrayList<>();
               this.useGradientWallpaper = loaded.useGradientWallpaper;
               this.gradientTopLeft = loaded.gradientTopLeft != 0 ? loaded.gradientTopLeft : -871038925;
               this.gradientTopRight = loaded.gradientTopRight != 0 ? loaded.gradientTopRight : -869072794;
               this.gradientBottomLeft = loaded.gradientBottomLeft != 0 ? loaded.gradientBottomLeft : -865717334;
               this.gradientBottomRight = loaded.gradientBottomRight != 0 ? loaded.gradientBottomRight : -861243393;
            }
         } catch (IOException e) {
            LOGGER.error("Failed to load config", e);
            this.backupBrokenConfig(configFile);
            this.save();
         }
      } else {
         this.passwordProtectedCallsInitialized = true;
         this.save();
      }
      // Save after closing the reader; Windows will not allow truncating the open config file.
      if (backendAddressMigrated || passwordSettingMigrated) this.save();
   }

   private void backupBrokenConfig(File configFile) {
      try {
         if (configFile != null && configFile.exists()) {
            File backup = new File(configFile.getParentFile(), configFile.getName() + ".broken-" + System.currentTimeMillis() + ".bak");
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (Throwable ignored) {
      }
   }

   public void save() {
      File configDir = FabricLoader.getInstance().getConfigDir().toFile();
      if (!configDir.exists()) {
         configDir.mkdirs();
      }

      File configFile = new File(configDir, CONFIG_FILE_NAME);

      try (FileWriter writer = new FileWriter(configFile)) {
         GSON.toJson(this, writer);
      } catch (IOException e) {
         LOGGER.error("Failed to save config", e);
      }
   }

   public boolean hasContact(String numberOrUuid) {
      if (numberOrUuid == null) {
         return false;
      }

      for (ModConfig.Contact c : this.contacts) {
         if (!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server)) continue;
         if (c.number != null && c.number.equals(numberOrUuid)) {
            return true;
         }

         if (c.uuid != null && c.uuid.equals(numberOrUuid)) {
            return true;
         }
      }

      return false;
   }

   public void addContact(ModConfig.Contact c) {
      if (c != null && ((c.number != null && !c.number.isBlank()) || (c.uuid != null && !c.uuid.isBlank()))) {
         for (int i = 0; i < this.contacts.size(); i++) {
            ModConfig.Contact x = this.contacts.get(i);
            if (!java.util.Objects.equals(x.server,c.server)) continue;
            if (c.uuid != null && x.uuid != null && x.uuid.equals(c.uuid)) {
               this.contacts.set(i, c);
               this.save();
               return;
            }

            if (c.number != null && !c.number.isBlank() && x.number != null && x.number.equals(c.number)) {
               this.contacts.set(i, c);
               this.save();
               return;
            }
         }

         this.contacts.add(c);
         this.save();
      }
   }

   public void removeContact(String numberOrUuid) {
      if (numberOrUuid != null) {
         this.contacts
            .removeIf(
               c -> dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server) && (c.number != null && c.number.equals(numberOrUuid)
                  || c.uuid != null && c.uuid.equals(numberOrUuid)
                  || c.name != null && c.name.equalsIgnoreCase(numberOrUuid))
            );
         this.save();
      }
   }

   public boolean hasMet(String uuidOrName) {
      if (uuidOrName == null) {
         return false;
      }

      for (String s : this.metPlayers) {
         if (s != null && s.equalsIgnoreCase(uuidOrName)) {
            return true;
         }
      }

      for (ModConfig.Contact c : this.contacts) {
         if (!dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.matches(c.server)) continue;
         if (c.uuid != null && c.uuid.equalsIgnoreCase(uuidOrName)) {
            return true;
         }

         if (c.name != null && c.name.equalsIgnoreCase(uuidOrName)) {
            return true;
         }

         if (c.number != null && c.number.equalsIgnoreCase(uuidOrName)) {
            return true;
         }
      }

      return false;
   }

   public void addMet(String uuidOrName) {
      if (uuidOrName != null && !uuidOrName.isEmpty()) {
         if (!this.metPlayers.contains(uuidOrName)) {
            this.metPlayers.add(uuidOrName);
            this.save();
         }
      }
   }

   public boolean isBlocked(UUID uuid) {
      return uuid != null && this.isBlocked(uuid.toString());
   }

   public boolean isBlocked(String value) {
      String normalized = normalizeBlockKey(value);
      if (normalized.isEmpty()) {
         return false;
      }

      for (String blocked : this.blockedPlayers) {
         if (normalizeBlockKey(blocked).equals(normalized)) {
            return true;
         }
      }

      return false;
   }

   public void addBlocked(UUID uuid, String fallbackName) {
      String key = uuid != null ? uuid.toString() : fallbackName;
      this.addBlocked(key);
   }

   public void addBlocked(String value) {
      String normalized = normalizeBlockKey(value);
      if (normalized.isEmpty() || this.isBlocked(normalized)) {
         return;
      }

      this.blockedPlayers.add(normalized);
      this.save();
   }

   public void removeBlocked(String value) {
      String normalized = normalizeBlockKey(value);
      if (normalized.isEmpty()) {
         return;
      }

      this.blockedPlayers.removeIf(item -> normalizeBlockKey(item).equals(normalized));
      this.save();
   }

   private static String normalizeBlockKey(String value) {
      return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
   }

   public static class RingtonePreference { public float volume = 0.8f; public String ringtone = "nokia"; }
   public static class CallHistoryEntry {
      public String server;
      public String dialedNumber;
      public String reason;
      public int count = 1;
      public String name;
      public String uuid;
      public String number;
      public String type;
      public int durationSec;
      public long timestampMs;
   }

   public static class Contact {
      public String server;
      public String name;
      public String nickname;
      public String number;
      public String uuid;

      public Contact() {
      }

      public Contact(String name, String number, String uuid) {
         this.server = dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles.current();
         this.name = name;
         this.number = number;
         this.uuid = uuid;
      }
   }
}



