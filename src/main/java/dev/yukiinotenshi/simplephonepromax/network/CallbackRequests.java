package dev.yukiinotenshi.simplephonepromax.network;
import com.google.gson.*;
import java.util.*;
import dev.yukiinotenshi.simplephonepromax.gui.PhoneNotification;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.minecraft.client.MinecraftClient;
public final class CallbackRequests {
 private static JsonArray inbox=new JsonArray();private static final Set<String> seen=new HashSet<>();private static String context="";
 public static JsonArray inbox(){return inbox;}
 public static void poll(JsonArray rows){String current=ServerProfiles.current();if(!current.equals(context)){inbox=new JsonArray();seen.clear();context=current;}inbox=rows;
  for(var e:rows){var r=e.getAsJsonObject();String id=r.get("id").getAsString();if(seen.add(id)){
   UUID source=UUID.fromString(r.get("source").getAsString());if(CallPolicy.allows(source,false)&&CallPolicy.admit(source,"callback:"+id))PhoneNotification.show("Просьба перезвонить",r.get("name").getAsString());else dismiss(id);
  }}
 }
 public static void send(UUID target){var b=new JsonObject();b.addProperty("target",target.toString());CallControlService.extension("callback-send",b).whenComplete((r,e)->MinecraftClient.getInstance().execute(()->PhoneMessages.show(e==null?"Просьба перезвонить отправлена":"Не удалось отправить: абонент недоступен или просьба уже отправлена")));}
 public static void dismiss(String id){var b=new JsonObject();b.addProperty("id",id);CallControlService.extension("callback-dismiss",b);}
}

