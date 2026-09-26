package dev.yukiinotenshi.simplephonepromax.phone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class BackendNumberService {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3L)).build();
   private static final String CACHE_FILE_NAME = "svc-fox-mobile-backend-cache.json";
   private static final long REGISTER_INTERVAL_MS = 30_000L;
   private static final Map<UUID, BackendNumberService.Entry> UUID_TO_ENTRY = new java.util.concurrent.ConcurrentHashMap<>();
   private static final Map<String, UUID> NUMBER_TO_UUID = new java.util.concurrent.ConcurrentHashMap<>();
   private static final Map<String, String> SHORT_TO_TARGET = new java.util.concurrent.ConcurrentHashMap<>();
   private static long lastRegisterMs;
   private static volatile boolean requestInFlight;

   private BackendNumberService() {
   }

   private static File getCacheFile() {
      return FabricLoader.getInstance().getConfigDir().resolve(CACHE_FILE_NAME).toFile();
   }

   public static void load() {
      try {
         File file = getCacheFile();
         if (!file.exists()) {
            return;
         }

         try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type type = (new TypeToken<CacheData>() {}).getType();
            CacheData data = GSON.fromJson(reader, type);
            UUID_TO_ENTRY.clear();
            NUMBER_TO_UUID.clear();
            SHORT_TO_TARGET.clear();
            if (data != null) {
               if (data.players != null) {
                  for (Entry entry : data.players.values()) {
                     registerCachedEntry(entry);
                  }
               }

               if (data.shortNumbers != null) {
                  SHORT_TO_TARGET.putAll(data.shortNumbers);
               }
            }
         }
      } catch (Throwable t) {
         SimpleVoiceCallClient.LOGGER.warn("Failed to load backend number cache", t);
      }
   }

   private static void save() {
      try {
         File file = getCacheFile();
         File parent = file.getParentFile();
         if (parent != null && !parent.exists()) {
            parent.mkdirs();
         }

         CacheData data = new CacheData();
         data.players = new LinkedHashMap<>();
         for (Map.Entry<UUID, Entry> entry : UUID_TO_ENTRY.entrySet()) {
            data.players.put(entry.getKey().toString(), entry.getValue());
         }
         data.shortNumbers = new LinkedHashMap<>(SHORT_TO_TARGET);

         try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
         }
      } catch (Throwable t) {
         SimpleVoiceCallClient.LOGGER.warn("Failed to save backend number cache", t);
      }
   }

   public static void tick(MinecraftClient client) {
      if (!isEnabled() || client == null || client.player == null) {
         return;
      }

      long now = System.currentTimeMillis();
      if (now - lastRegisterMs >= REGISTER_INTERVAL_MS) {
         lastRegisterMs = now;
         registerSelfAsync(client);
      }
   }

   public static boolean isEnabled() {
      return !dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy() && SimpleVoiceCallClient.config != null
         && SimpleVoiceCallClient.config.backendNumbersEnabled
         && SimpleVoiceCallClient.config.backendBaseUrl != null
         && !SimpleVoiceCallClient.config.backendBaseUrl.isBlank();
   }

   public static String getNameFor(UUID uuid){Entry e=uuid==null?null:UUID_TO_ENTRY.get(uuid);return e==null?null:e.lastKnownName;}
   public static String getNumberFor(UUID uuid) {
      Entry entry = uuid != null ? UUID_TO_ENTRY.get(uuid) : null;
      return entry != null ? entry.backendNumber : null;
   }

   /** Cache the operator number returned for an online peer by the call service. */
   public static void observePeerNumber(UUID uuid,String name,String number){
      if(uuid==null||number==null||!PhoneNumberManager.isValidProMaxNumber(PhoneNumberManager.onlyDigits(number)))return;
      String clean=PhoneNumberManager.onlyDigits(number);
      Entry entry=UUID_TO_ENTRY.computeIfAbsent(uuid,id->{Entry value=new Entry();value.minecraftUuid=id.toString();return value;});
      entry.minecraftUuid=uuid.toString();entry.backendNumber=clean;
      if(name!=null&&!name.isBlank())entry.lastKnownName=name;
      registerNumber(clean,uuid);save();
   }

   public static UUID findCachedUuidByNumber(String number) {
      String clean = PhoneNumberManager.onlyDigits(number);
      if (clean.isEmpty()) {
         return null;
      }

      String target = SHORT_TO_TARGET.get(clean);
      if (target != null) {
         clean = PhoneNumberManager.onlyDigits(target);
      }

      return NUMBER_TO_UUID.get(clean);
   }

   public static CompletableFuture<UUID> resolveAsync(String number) {return resolveNumber(number,false);}
   public static CompletableFuture<UUID> resolveFreshAsync(String number) {return resolveNumber(number,true);}
   private static CompletableFuture<UUID> resolveNumber(String number,boolean fresh) {
      if (dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()) return CompletableFuture.completedFuture(PhoneNumberManager.findUuidByNumber(number));
      String clean = PhoneNumberManager.onlyDigits(number);
      UUID cached = fresh || PhoneNumberManager.isValidShortCode(clean) ? null : findCachedUuidByNumber(clean);
      if (cached != null || !isEnabled()) {
         return CompletableFuture.completedFuture(cached);
      }

      HttpRequest request;
      try {
         request = HttpRequest.newBuilder(endpoint("/resolve/" + clean))
            .timeout(Duration.ofSeconds(5L))
            .GET()
            .header("Accept", "application/json")
            .build();
      } catch (Throwable t) {
         return CompletableFuture.completedFuture(null);
      }

      return dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.client(request.uri()).sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApply(response -> {
         if(response.statusCode()==404)return null;
         if (response.statusCode() < 200 || response.statusCode() >= 300) {
            if(fresh)throw new IllegalStateException("Number lookup HTTP "+response.statusCode());
            return null;
         }

         try {
            JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
            Entry entry = entryFromJson(root);
            registerCachedEntry(entry);
            if (root != null && root.has("source") && "short".equals(root.get("source").getAsString()) && root.has("target_number")) {
               SHORT_TO_TARGET.put(clean, PhoneNumberManager.onlyDigits(root.get("target_number").getAsString()));
            }
            save();
            return entry != null && entry.minecraftUuid != null ? UUID.fromString(entry.minecraftUuid) : null;
         } catch (Throwable t) {
            return null;
         }
      }).exceptionally(t -> {if(fresh)throw new java.util.concurrent.CompletionException(t);return null;});
   }

   public static void registerSelfAsync(MinecraftClient client) {
      if (requestInFlight || client == null || client.player == null || !isEnabled()) {
         return;
      }

      requestInFlight = true;
      UUID uuid = client.player.getUuid();
      String deviceId = SimpleVoiceCallClient.config.backendDeviceId;
      if (deviceId == null || deviceId.isBlank()) {
         deviceId = UUID.randomUUID().toString();
         SimpleVoiceCallClient.config.backendDeviceId = deviceId;
         SimpleVoiceCallClient.config.save();
      }

      JsonObject body = new JsonObject();
      body.addProperty("minecraft_uuid", uuid.toString());
      body.addProperty("last_known_name", client.player.getName().getString());
      body.addProperty("server_address", currentServerAddress(client));
      body.addProperty("legacy_number", PhoneNumberManager.getNumberFor(uuid));
      String originalShort = PhoneNumberManager.getLegacyOriginalNumberFor(uuid);
      if (originalShort != null) {
         body.addProperty("original_short_number", originalShort);
      }
      body.addProperty("device_id", deviceId);

      HttpRequest request;
      try {
         request = HttpRequest.newBuilder(endpoint("/register"))
            .timeout(Duration.ofSeconds(6L))
            .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();
      } catch (Throwable t) {
         requestInFlight = false;
         setStatus("bad_url");
         return;
      }

      dev.yukiinotenshi.simplephonepromax.network.OperatorHttp.client(request.uri()).sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
         requestInFlight = false;
         if (error != null || response == null || response.statusCode() < 200 || response.statusCode() >= 300) {
            setStatus("offline");
            return;
         }

         try {
            JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
            Entry entry = entryFromJson(root);
            registerCachedEntry(entry);
            if (entry != null && entry.backendNumber != null) {
               SimpleVoiceCallClient.config.backendCachedNumber = entry.backendNumber;
            }
            setStatus("online");
            save();
         } catch (Throwable t) {
            setStatus("bad_response");
         }
      });
   }

   private static void setStatus(String status) {
      try {
         SimpleVoiceCallClient.config.backendStatus = status;
         SimpleVoiceCallClient.config.save();
      } catch (Throwable ignored) {
      }
   }

   private static void registerCachedEntry(Entry entry) {
      if (entry == null || entry.minecraftUuid == null) {
         return;
      }

      try {
         UUID uuid = UUID.fromString(entry.minecraftUuid);
         NUMBER_TO_UUID.entrySet().removeIf(item -> uuid.equals(item.getValue()));
         UUID_TO_ENTRY.put(uuid, entry);
         registerNumber(entry.backendNumber, uuid);
         registerNumber(entry.legacyNumber, uuid);
         registerNumber(entry.originalShortNumber, uuid);
      } catch (Throwable ignored) {
      }
   }

   private static void registerNumber(String number, UUID uuid) {
      String clean = PhoneNumberManager.onlyDigits(number);
      if (uuid != null && PhoneNumberManager.isValidKnownNumber(clean)) {
         NUMBER_TO_UUID.put(clean, uuid);
      }
   }

   private static Entry entryFromJson(JsonObject root) {
      if (root == null) {
         return null;
      }

      Entry entry = new Entry();
      entry.minecraftUuid = getString(root, "minecraft_uuid");
      entry.lastKnownName = getString(root, "last_known_name");
      entry.serverAddress = getString(root, "server_address");
      entry.backendNumber = getString(root, "oracle_number", "backend_number", "number");
      entry.legacyNumber = getString(root, "legacy_number");
      entry.originalShortNumber = getString(root, "original_short_number");
      entry.deviceId = getString(root, "device_id");
      entry.updatedAt = getString(root, "updated_at");
      return entry.minecraftUuid != null ? entry : null;
   }

   private static String getString(JsonObject root, String... names) {
      for (String name : names) {
         if (root.has(name) && !root.get(name).isJsonNull()) {
            return root.get(name).getAsString();
         }
      }

      return null;
   }

   private static URI endpoint(String path) {
      String base = SimpleVoiceCallClient.config.backendBaseUrl.trim();
      while (base.endsWith("/")) {
         base = base.substring(0, base.length() - 1);
      }
      return URI.create(base + path);
   }

   private static String currentServerAddress(MinecraftClient client) {
      try {
         if (client.getCurrentServerEntry() != null && client.getCurrentServerEntry().address != null) {
            return client.getCurrentServerEntry().address;
         }
      } catch (Throwable ignored) {
      }

      return client.isInSingleplayer() ? "singleplayer" : "unknown";
   }

   private static class CacheData {
      Map<String, Entry> players = new LinkedHashMap<>();
      Map<String, String> shortNumbers = new LinkedHashMap<>();
   }

   private static class Entry {
      String minecraftUuid;
      String lastKnownName;
      String serverAddress;
      String backendNumber;
      String legacyNumber;
      String originalShortNumber;
      String deviceId;
      String updatedAt;
   }
}

