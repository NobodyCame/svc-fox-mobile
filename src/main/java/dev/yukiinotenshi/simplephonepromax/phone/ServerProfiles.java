package dev.yukiinotenshi.simplephonepromax.phone;
import java.util.Locale;
import net.minecraft.client.Minecraft;
public final class ServerProfiles {
   public static String current(){var c=Minecraft.getInstance();return c==null||c.getCurrentServer()==null?"singleplayer":c.getCurrentServer().ip.trim().toLowerCase(Locale.ROOT);}
   public static boolean matches(String server){return current().equals(server);}
   public static String contactKey(java.util.UUID id){return current()+"/"+id;}
}

