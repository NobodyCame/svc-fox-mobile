package dev.yukiinotenshi.simplephonepromax.phone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
public final class PhoneDialer {
   private static final java.util.Map<UUID,String> DIALED=new java.util.concurrent.ConcurrentHashMap<>();
   public static String lastDialedFor(UUID id){return id==null?null:DIALED.get(id);}
   public static CompletableFuture<UUID> resolve(String number) {
      return dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.tryNumber(PhoneNumberManager.onlyDigits(number)).thenCompose(handled->handled?CompletableFuture.completedFuture(null):resolvePlayer(number));
   }
   private static CompletableFuture<UUID> resolvePlayer(String number) {
      if(dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy()&&PhoneNumberManager.isValidShortCode(PhoneNumberManager.onlyDigits(number)))return CompletableFuture.completedFuture(null);
      if(dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy())return CompletableFuture.completedFuture(PhoneNumberManager.findUuidByNumber(number));
      return BackendNumberService.resolveFreshAsync(number).thenApply(id->{if(id!=null)DIALED.put(id,PhoneNumberManager.onlyDigits(number));return id;});
   }
   public static boolean call(UUID target,String name) { return call(target,name,null); }
   public static boolean call(UUID target,String name,String dialed) {
      if(dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.busy())return false;
      var client=net.minecraft.client.MinecraftClient.getInstance();
      if(target!=null&&!SimpleVoiceCallClient.callManager.isInCall()&&client.player!=null&&(!Boolean.TRUE.equals(client.getNetworkHandler()!=null&&client.getNetworkHandler().getPlayerListEntry(target)!=null)||Boolean.FALSE.equals(dev.yukiinotenshi.simplephonepromax.network.CallControlService.hasPhone(target)))){
         client.setScreen(new dev.yukiinotenshi.simplephonepromax.gui.DialAttemptScreen(name,target,"offline"));return false;
      }
      if(target!=null){DIALED.remove(target);if(dialed!=null&&!dialed.isBlank())DIALED.put(target,PhoneNumberManager.onlyDigits(dialed));}
      if(!SimpleVoiceCallClient.callManager.startOutgoingCall(name,target))return false;
      ModNetworking.sendCallRequest(target,name);return true;
   }
}

