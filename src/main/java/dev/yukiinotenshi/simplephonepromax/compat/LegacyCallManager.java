package dev.yukiinotenshi.simplephonepromax.compat;

import dev.yukiinotenshi.simplephonepromax.call.CallManager;
import dev.yukiinotenshi.simplephonepromax.call.CallState;
import java.util.UUID;

/** UI facade. The Simple Voice Call manager owns the state machine and all call transitions. */
public final class LegacyCallManager extends CallManager {
   @Override public void reset(){LegacyCallBridge.manager("reset");}
   @Override public CallState getState(){try{return CallState.valueOf(LegacyCallBridge.state());}catch(IllegalArgumentException e){return CallState.NONE;}}
   @Override public void tick(){LegacyCallBridge.tick();if(getState()==CallState.INCOMING_RINGING&&!dev.yukiinotenshi.simplephonepromax.phone.CallPolicy.allows(getOtherPlayerUuid(),false)){String name=getOtherPlayerName();UUID id=getOtherPlayerUuid();LegacyCallBridge.manager("declineCall");CallManager.addHistory(name,id,"dnd",0);}}
   @Override public boolean isInCall(){return getState()!=CallState.NONE;}
   @Override public boolean isInActiveCall(){return getState()==CallState.ACTIVE;}
   @Override public boolean isInRingingCall(){return getState()==CallState.INCOMING_RINGING||getState()==CallState.OUTGOING_RINGING;}
   @Override public String getOtherPlayerName(){return (String)LegacyCallBridge.manager("getOtherPlayerName");}
   @Override public UUID getOtherPlayerUuid(){return (UUID)LegacyCallBridge.manager("getOtherPlayerUuid");}
   @Override public UUID getCallGroupId(){return (UUID)LegacyCallBridge.manager("getCallGroupId");}
   @Override public long getCallDurationMs(){Object n=LegacyCallBridge.manager("getCallDurationMs");return n instanceof Number value?value.longValue():0;}
   @Override public long getRingingStartedAtMs(){return LegacyCallBridge.ringingAt();}
   @Override public boolean startOutgoingCall(String name,UUID target){
      if(!LegacyCallBridge.ready()){LegacyCallBridge.unavailable();return false;}
      if(isInCall()||target==null)return false;
      LegacyCallBridge.manager("startOutgoingCall",name,target);
      return getState()==CallState.OUTGOING_RINGING&&target.equals(getOtherPlayerUuid());
   }
   @Override public void startIncomingCall(String name,UUID id){LegacyCallBridge.manager("startIncomingCall",name,id);}
   @Override public void acceptCall(){LegacyCallBridge.manager("acceptCall");}
   @Override public void acceptCall(UUID caller,UUID target,boolean network){LegacyCallBridge.manager("acceptCall",caller,target,network);}
   @Override public void declineCall(){LegacyCallBridge.manager("declineCall");}
   @Override public void remoteDecline(){LegacyCallBridge.manager("remoteDecline");}
   @Override public void remoteBlocked(){LegacyCallBridge.manager("remoteBlocked");}
   @Override public void remoteCancelIncoming(){LegacyCallBridge.manager("remoteCancelIncoming");}
   @Override public void connectionLost(){LegacyCallBridge.manager("connectionLost");}
   @Override public void remoteHangup(){LegacyCallBridge.manager("remoteHangup");}
   @Override public void endCall(){LegacyCallBridge.manager("endCall");}
   @Override public void remoteBusy(){} // No busy extension in the original protocol.
   @Override public void sendBlockedMessage(String name){LegacyCallBridge.manager("sendBlockedMessage",name);}
   @Override public void updateTransferredPeer(String name,UUID id){}
   @Override public void joinGroupCall(String name,UUID anchor,UUID group){} // Backend-only operation.
}

