package dev.yukiinotenshi.simplephonepromax.phone;

import com.google.gson.*;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

/** Explicit allowlist: credentials and backend identity never enter backups. */
public final class SettingsBackup {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static Path file(){return FabricLoader.getInstance().getConfigDir().resolve("simple-phone-backup.json");}
    public static final class Data {
        public int format=1;
        public List<ModConfig.Contact> contacts=new ArrayList<>();
        public List<String> favorites=new ArrayList<>();
        public String ringtone="default",customRingtone="",doNotDisturb="all";
        public boolean useCustomRingtone,quietSchedule,allowCallWaiting=true;
        public int quietStartHour=23,quietEndHour=8;
        public float ringtoneVolume=.8f,dialingVolume=.7f,busyVolume=.7f,unavailableVolume=.7f,waitingVolume=.5f;
        public Map<String,ModConfig.RingtonePreference> contactRingtones=new HashMap<>();
    }
    public static Path exportSettings()throws Exception {
        var c=SimpleVoiceCallClient.config;var d=new Data();d.contacts=c.contacts;d.favorites=c.favoriteContacts;
        d.ringtone=c.ringtone;d.customRingtone=c.customRingtone;d.useCustomRingtone=c.useCustomRingtone;
        d.doNotDisturb=c.doNotDisturb;d.quietSchedule=c.quietSchedule;d.allowCallWaiting=c.allowCallWaiting;
        d.quietStartHour=c.quietStartHour;d.quietEndHour=c.quietEndHour;d.ringtoneVolume=c.ringtoneVolume;
        d.dialingVolume=c.dialingVolume;d.busyVolume=c.busyVolume;d.unavailableVolume=c.unavailableVolume;d.waitingVolume=c.waitingVolume;d.contactRingtones=c.contactRingtones;
        Path tmp=file().resolveSibling("simple-phone-backup.tmp");Files.writeString(tmp,JSON.toJson(d));Files.move(tmp,file(),StandardCopyOption.REPLACE_EXISTING);return file();
    }
    public static Data read()throws Exception {
        if(Files.size(file())>1024*1024)throw new IllegalArgumentException("Резервная копия больше 1 МиБ");
        Data d=JSON.fromJson(Files.readString(file()),Data.class);
        if(d==null||d.format!=1||d.contacts==null||d.contacts.size()>5000||d.favorites==null||d.contactRingtones==null)throw new IllegalArgumentException("Неподдерживаемая резервная копия");
        for(var contact:d.contacts){if(contact==null||contact.uuid==null||contact.name==null||contact.name.length()>128)throw new IllegalArgumentException("Некорректный контакт");UUID.fromString(contact.uuid);}
        return d;
    }
    private static String key(ModConfig.Contact c){return Objects.toString(c.server,"")+"/"+c.uuid;}
    public static int additions(Data d){Set<String> known=new HashSet<>();for(var c:SimpleVoiceCallClient.config.contacts)known.add(key(c));int n=0;for(var c:d.contacts)if(known.add(key(c)))n++;return n;}
    private static float volume(float value){if(!Float.isFinite(value))throw new IllegalArgumentException("Некорректная громкость");return Math.max(0,Math.min(1,value));}
    public static void apply(Data d){
        // Validate all settings before touching live configuration.
        float ring=volume(d.ringtoneVolume),dial=volume(d.dialingVolume),busy=volume(d.busyVolume),off=volume(d.unavailableVolume),wait=volume(d.waitingVolume);
        if(!Set.of("all","favorites","none").contains(d.doNotDisturb)||d.ringtone==null||d.customRingtone==null)throw new IllegalArgumentException("Некорректные настройки");
        for(var entry:d.contactRingtones.entrySet()){if(entry.getValue()==null||entry.getValue().ringtone==null)throw new IllegalArgumentException("Некорректный рингтон контакта");volume(entry.getValue().volume);}
        String safeCustom=d.customRingtone.isEmpty()?"":Path.of(d.customRingtone).getFileName().toString();
        var c=SimpleVoiceCallClient.config;Set<String> known=new HashSet<>();for(var item:c.contacts)known.add(key(item));for(var item:d.contacts)if(known.add(key(item)))c.contacts.add(item);
        for(String favorite:d.favorites)if(favorite!=null&&!c.favoriteContacts.contains(favorite))c.favoriteContacts.add(favorite);
        c.ringtone=d.ringtone;c.customRingtone=safeCustom;c.useCustomRingtone=d.useCustomRingtone;
        c.ringtoneVolume=ring;c.dialingVolume=dial;c.busyVolume=busy;c.unavailableVolume=off;c.waitingVolume=wait;c.doNotDisturb=d.doNotDisturb;c.allowCallWaiting=d.allowCallWaiting;
        c.quietSchedule=d.quietSchedule;c.quietStartHour=Math.floorMod(d.quietStartHour,24);c.quietEndHour=Math.floorMod(d.quietEndHour,24);c.contactRingtones.putAll(d.contactRingtones);c.save();
    }
}

