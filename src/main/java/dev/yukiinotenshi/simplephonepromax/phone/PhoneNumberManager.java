package dev.yukiinotenshi.simplephonepromax.phone;

import com.google.gson.Gson;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.compat.LegacyCallBridge;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public class PhoneNumberManager {
   public static final PhoneNumberManager.Region PHONE_FORMAT = new PhoneNumberManager.Region("PHONE", "1", "Телефон", 10);
   public static final PhoneNumberManager.Region ORIGINAL_LEGACY_FORMAT = new PhoneNumberManager.Region("PHONE", "1", "Телефон", 5);
   private static final String FILE_NAME = "svc-fox-mobile-phone-numbers.json";
   private static final String ORIGINAL_FILE_NAME = "phone-numbers.json";
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final Map<UUID, String> uuidToNumber = new HashMap<>();
   private static final Map<String, UUID> numberToUuid = new HashMap<>();
   private static final Map<UUID, String> originalUuidToNumber = new HashMap<>();
   private static final Map<String, UUID> originalNumberToUuid = new HashMap<>();
   private static final Random RANDOM = new Random();

   private static File getFile() {
      return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME).toFile();
   }

   private static File getOriginalFile() {
      return FabricLoader.getInstance().getConfigDir().resolve(ORIGINAL_FILE_NAME).toFile();
   }

   public static PhoneNumberManager.Region getCurrentRegion() {
      return PHONE_FORMAT;
   }

   public static void load() {
      uuidToNumber.clear();
      numberToUuid.clear();
      originalUuidToNumber.clear();
      originalNumberToUuid.clear();
   }

   private static void importOriginalNumbers() {
      try {
         File f = getOriginalFile();
         if (!f.exists()) {
            return;
         }

         try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            Type type = (new TypeToken<LinkedHashMap<UUID, String>>() {}).getType();
            LinkedHashMap<UUID, String> loaded = (LinkedHashMap<UUID, String>)GSON.fromJson(r, type);
            if (loaded != null) {
               originalUuidToNumber.clear();
               originalNumberToUuid.clear();

               for (Entry<UUID, String> e : loaded.entrySet()) {
                  String clean = onlyDigits(e.getValue());
                  if (isValidOriginalShortNumber(clean) || isValidProMaxNumber(clean)) {
                     originalUuidToNumber.put(e.getKey(), clean);
                     originalNumberToUuid.put(clean, e.getKey());
                     ModNetworking.registerPhoneNumber(e.getKey(), clean);
                  }
               }
            }
         }
      } catch (Throwable ignored) {
      }
   }

   public static void save() {
      try {
         File f = getFile();
         File parent = f.getParentFile();
         if (parent != null && !parent.exists()) {
            parent.mkdirs();
         }

         LinkedHashMap<UUID, String> out = new LinkedHashMap<>(uuidToNumber);

         try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            GSON.toJson(out, w);
         }
      } catch (Exception var8) {
      }
   }

   public static String onlyDigits(String s) {
      if (s == null) {
         return "";
      }

      StringBuilder sb = new StringBuilder();

      for (char c : s.toCharArray()) {
         if (c >= '0' && c <= '9') {
            sb.append(c);
         }
      }

      return sb.toString();
   }

   private static String generateUniqueNumberForRegion(PhoneNumberManager.Region region) {
      int digits = region.digitsCount;

      for (int i = 0; i < 100000; i++) {
         StringBuilder sb = new StringBuilder();
         sb.append(region.countryCode);

         for (int d = 0; d < digits; d++) {
            sb.append(RANDOM.nextInt(10));
         }

         String n = sb.toString();
         if (!numberToUuid.containsKey(n)) {
            return n;
         }
      }

      StringBuilder sb = new StringBuilder();
      sb.append(region.countryCode);
      String t = String.valueOf(System.currentTimeMillis());

      while (sb.length() < region.countryCode.length() + digits) {
         sb.append(t.charAt(sb.length() % t.length()));
      }

      return sb.toString();
   }

   public static String regenerateMyNumber() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.player != null) {
         UUID myUuid = mc.player.getUuid();
         String oldNum = originalUuidToNumber.get(myUuid);
         if (oldNum != null) {
            originalNumberToUuid.remove(oldNum, myUuid);
         }

         String newNum = computeLegacyNumber(myUuid);
         originalUuidToNumber.put(myUuid, newNum);
         originalNumberToUuid.put(newNum, myUuid);
         ModNetworking.registerPhoneNumber(myUuid, newNum);
         return newNum;
      } else {
         return null;
      }
   }

   public static boolean setMyNumberManual(String typedNumber) {
      return false;
   }

   public static String getMyNumber() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.player != null) {
         return getNumberFor(mc.player.getUuid());
      } else {
         return onlyDigits(ORIGINAL_LEGACY_FORMAT.countryCode + "00000");
      }
   }

   private static boolean matchesRegion(String number, PhoneNumberManager.Region region) {
      return number != null && region != null && number.startsWith(region.countryCode) && number.length() == region.countryCode.length() + region.digitsCount;
   }

   public static boolean isValidProMaxNumber(String number) {
      return number != null && number.length() >= 7 && number.length() <= 11;
   }

   public static boolean isValidOriginalShortNumber(String number) {
      return number != null && number.length() == 6 && number.charAt(0) == '1';
   }

   public static boolean isValidShortCode(String number) {
      return number != null && number.length() >= 1 && number.length() <= 3;
   }

   public static boolean isValidKnownNumber(String number) {
      String clean = onlyDigits(number);
      return isValidProMaxNumber(clean) || isValidOriginalShortNumber(clean) || isValidShortCode(clean);
   }

   public static String getNumberFor(UUID uuid) {
      if (CallStandard.isLegacy()) return (String)LegacyCallBridge.original("phone.PhoneNumberManager", "getNumberFor", uuid);
      if (uuid == null) {
         return null;
      }

      return refreshLegacyNumber(uuid);
   }

   public static String getLegacyOriginalNumberFor(UUID uuid) {
      if (uuid == null) {
         return null;
      }

      return refreshLegacyNumber(uuid);
   }

   public static String getDisplayNumberFor(UUID uuid) {
      if (CallStandard.isLegacy()) return getNumberFor(uuid);
      if (uuid == null) {
         return null;
      }

      if (dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient.config != null) {
         String backend = BackendNumberService.getNumberFor(uuid);
         if (isValidKnownNumber(backend)) {
            return backend;
         }
      }

      return getNumberFor(uuid);
   }

   /** Number visible in the nearby-player directory: saved contacts or explicit owner opt-in only. */
   public static String getVisibleNumberFor(UUID uuid) {
      if (uuid == null) return null;
      if (SimpleVoiceCallClient.config != null) {
         String id = uuid.toString();
         for (ModConfig.Contact contact : SimpleVoiceCallClient.config.contacts) {
            if (!ServerProfiles.matches(contact.server) || !id.equals(contact.uuid)) continue;
            String saved = onlyDigits(contact.number);
            if (isValidKnownNumber(saved)) return saved;
         }
      }
      String shared = BackendNumberService.getPublicNumberFor(uuid);
      return isValidKnownNumber(shared) ? shared : null;
   }

   public static String getMyDisplayNumber() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.player != null) {
         String number=getDisplayNumberFor(mc.player.getUuid());
         if(isValidProMaxNumber(number))return number;
         String cached=dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient.config==null?null:dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient.config.backendCachedNumber;
         return isValidProMaxNumber(cached)?cached:number;
      }

      return getMyNumber();
   }

   public static UUID findUuidByNumber(String number) {
      if (CallStandard.isLegacy()) return (UUID)LegacyCallBridge.original("phone.PhoneNumberManager", "findUuidByNumber", number);
      if (number == null) {
         return null;
      }

      String clean = onlyDigits(number);
      if (clean.isEmpty()) {
         return null;
      }

      // Three-digit codes belong to the backend, never to a legacy suffix match.
      if (isValidShortCode(clean)) {
         return null; // A short code must be resolved online for each dial, never from stale aliases.
      }

      UUID fromNet = ModNetworking.findUuidByPhoneNumber(clean);
      if (fromNet != null) {
         return fromNet;
      }

      UUID fromBackend = BackendNumberService.findCachedUuidByNumber(clean);
      if (fromBackend != null) {
         return fromBackend;
      }

      UUID imported = originalNumberToUuid.get(clean);
      if (imported != null) {
         return imported;
      }

      for (Entry<String, UUID> e : originalNumberToUuid.entrySet()) {
         if (e.getKey().endsWith(clean) || clean.endsWith(e.getKey())) {
            return e.getValue();
         }
      }

      return null;
   }

   public static void registerPlayer(UUID uuid, String numberOrName) {
      if (CallStandard.isLegacy()) { LegacyCallBridge.original("phone.PhoneNumberManager", "registerPlayer", uuid, numberOrName); return; }
      if (uuid != null) {
         refreshLegacyNumber(uuid);
      }
   }

   private static String refreshLegacyNumber(UUID uuid) {
      if (uuid == null) {
         return null;
      }

      String old = originalUuidToNumber.get(uuid);
      if (old != null) {
         originalNumberToUuid.remove(old, uuid);
      }

      String num = computeLegacyNumber(uuid);
      originalUuidToNumber.put(uuid, num);
      originalNumberToUuid.put(num, uuid);
      ModNetworking.registerPhoneNumber(uuid, num);
      return num;
   }

   private static String computeLegacyNumber(UUID uuid) {
      return generateDeterministicNumber(uuid, ORIGINAL_LEGACY_FORMAT, Collections.emptyMap());
   }

   private static String generateDeterministicNumber(UUID uuid, PhoneNumberManager.Region region) {
      return generateDeterministicNumber(uuid, region, numberToUuid);
   }

   private static String generateDeterministicNumber(UUID uuid, PhoneNumberManager.Region region, Map<String, UUID> occupiedNumbers) {
      long seed = 0L;
      if (uuid != null) {
         seed ^= uuid.getMostSignificantBits();
         seed ^= uuid.getLeastSignificantBits() * 31L;
      }

      seed ^= region.countryCode.hashCode() * 1013L;
      Random rnd = new Random(seed);
      StringBuilder sb = new StringBuilder();
      sb.append(region.countryCode);

      for (int d = 0; d < region.digitsCount; d++) {
         sb.append(rnd.nextInt(10));
      }

      String first = sb.toString();
      UUID firstOwner = occupiedNumbers.get(first);
      if (firstOwner == null || firstOwner.equals(uuid)) {
         return first;
      }

      for (int i = 0; i < 200; i++) {
         StringBuilder sb2 = new StringBuilder();
         sb2.append(region.countryCode);

         for (int d = 0; d < region.digitsCount; d++) {
            sb2.append(rnd.nextInt(10));
         }

         String n2 = sb2.toString();
         UUID owner = occupiedNumbers.get(n2);
         if (owner == null || owner.equals(uuid)) {
            return n2;
         }
      }

      return first;
   }

   public static String formatNumber(String num) {
      if (num == null) {
         return "";
      } else {
         String c = onlyDigits(num);
         if (c.isEmpty()) {
            return "";
         } else {
            if (c.length() == 11 && c.startsWith("1")) {
               return "+" + c.charAt(0) + " " + c.substring(1, 4) + " " + c.substring(4, 7) + " " + c.substring(7, 9) + "-" + c.substring(9);
            } else if (c.length() == 11 && c.startsWith("7")) {
               return "+7 " + c.substring(1, 4) + " " + c.substring(4, 7) + "-" + c.substring(7);
            } else if (c.length() == 12 && c.startsWith("380")) {
               return "+380 " + c.substring(3, 5) + " " + c.substring(5, 8) + "-" + c.substring(8);
            } else if (c.length() == 6) {
               return "+" + c.charAt(0) + " " + c.substring(1);
            } else {
               return "+" + c;
            }
         }
      }
   }

   public static Map<String, UUID> getAllNumbers() {
      return Collections.unmodifiableMap(originalNumberToUuid);
   }

   public static void resetCache() {
      UUID myUuid = null;
      String myNumber = null;

      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.player != null) {
            myUuid = mc.player.getUuid();
            myNumber = uuidToNumber.get(myUuid);
         }
      } catch (Throwable var5) {
      }

      uuidToNumber.clear();
      numberToUuid.clear();
      originalUuidToNumber.clear();
      originalNumberToUuid.clear();

      try {
         ModNetworking.playerPhoneNumbers.clear();
         ModNetworking.phoneNumberToUuid.clear();
      } catch (Throwable var4) {
      }

      if (myUuid != null) {
         regenerateMyNumber();
      }

      try {
         File f = getFile();
         if (f.exists()) {
            f.delete();
         }
      } catch (Throwable var3) {
      }

      save();
   }

   public static class Region {
      public final String code;
      public final String countryCode;
      public final String name;
      public final int digitsCount;

      public Region(String code, String countryCode, String name, int digitsCount) {
         this.code = code;
         this.countryCode = countryCode;
         this.name = name;
         this.digitsCount = digitsCount;
      }
   }
}



