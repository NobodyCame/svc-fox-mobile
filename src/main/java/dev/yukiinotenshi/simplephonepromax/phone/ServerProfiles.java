package dev.yukiinotenshi.simplephonepromax.phone;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
public final class ServerProfiles {
   public static String current(){
      var client=Minecraft.getInstance();
      if(client==null)return "singleplayer";
      var remote=client.getCurrentServer();
      if(remote!=null)return remote.ip.trim().toLowerCase(Locale.ROOT);
      var integrated=client.getSingleplayerServer();
      if(integrated==null)return "singleplayer";
      var folder=integrated.getWorldPath(LevelResource.ROOT).normalize().getFileName();
      return folder==null ? "singleplayer" : "world:"+folder;
   }
   public static boolean matches(String server){return current().equals(server);}
   public static String contactKey(java.util.UUID id){return current()+"/"+id;}
}

