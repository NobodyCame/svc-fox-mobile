package dev.yukiinotenshi.simplephonepromax.phone;
import net.minecraft.client.MinecraftClient;
import java.util.Locale;
public final class ServerProfiles {
   public static String current(){var c=MinecraftClient.getInstance();return c==null||c.getCurrentServerEntry()==null?"singleplayer":c.getCurrentServerEntry().address.trim().toLowerCase(Locale.ROOT);}
   public static boolean matches(String server){return current().equals(server);}
   public static String contactKey(java.util.UUID id){return current()+"/"+id;}
}

