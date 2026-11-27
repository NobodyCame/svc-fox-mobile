package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.*;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
/** Read-only resolution first. No call or organization queue until the user confirms. */
public final class CallCheckScreen extends Screen {
 private final String number;private final UUID expectedTarget;private UUID target;private String name="Проверка номера…",error="";private boolean started,ready,organization,byNumber,voicemailEligible,voicemailChecked;private int ticks;private long nextVoicemailCheck;private Button call,secure,callback,voicemail;
 public CallCheckScreen(String number){super(Component.literal("Перед звонком"));this.number=PhoneNumberManager.onlyDigits(number);this.expectedTarget=null;byNumber=true;}
 public CallCheckScreen(UUID target,String name){super(Component.literal("Перед звонком"));this.target=target;this.expectedTarget=null;this.name=name;number=Objects.toString(PhoneNumberManager.getDisplayNumberFor(target),"");ready=true;}
 public CallCheckScreen(UUID target,String name,String number){super(Component.literal("Перед звонком"));this.target=target;this.expectedTarget=target;this.name=name;this.number=PhoneNumberManager.onlyDigits(number);byNumber=true;}
 protected void init(){var f=PhoneGuiTextures.layout(width,height);int x=f.x()+16,w=f.width()-32,y=f.bottom()-108;
  call=addRenderableWidget(Button.builder(Component.literal("Позвонить"),b->dial()).bounds(x,y,w,20).build());
  secure=addRenderableWidget(Button.builder(Component.literal("Приватный звонок"),b->PrivateCalls.call(target)).bounds(x,y+24,w/2-2,20).build());
  callback=addRenderableWidget(Button.builder(Component.literal("Попросить перезвонить"),b->{CallbackRequests.send(target);callback.active=false;}).bounds(x+w/2+2,y+24,w/2-2,20).build());
  voicemail=addRenderableWidget(Button.builder(Component.literal("Оставить голосовое"),b->minecraft.setScreenAndShow(new VoicemailScreen(target))).bounds(x,y+48,w,20).build());
  addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,f.bottom()-32,w,20).build());
  if(byNumber&&!started){started=true;resolve();}else if(ready)checkVoicemail();update();
 }
 private void checkVoicemail(){if(target==null||System.currentTimeMillis()<nextVoicemailCheck)return;voicemailChecked=true;nextVoicemailCheck=System.currentTimeMillis()+5000;UUID checked=target;VoicemailClient.eligible(checked).whenComplete((ok,e)->minecraft.execute(()->{if(dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)&&checked.equals(target)){voicemailEligible=e==null&&Boolean.TRUE.equals(ok);update();}}));}
 private void resolve(){ready=false;BackendNumberService.resolveFreshAsync(number).whenComplete((id,e)->minecraft.execute(()->{
  if(!dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this))return;
  if(expectedTarget!=null&&!expectedTarget.equals(id)){target=null;error=e==null?"Номер не принадлежит этому контакту":"Оператор недоступен";name=error;}
  else{target=id;error=e==null?"":"Оператор недоступен";name=id==null?(e==null?"Номер не найден":"Повторите проверку номера"):label(id);}
  ready=true;voicemailChecked=false;update();checkVoicemail();
  if(id==null&&e==null&&PhoneNumberManager.isValidShortCode(number)&&!CallStandard.isLegacy())CallControlService.extension("org-list",new com.google.gson.JsonObject()).whenComplete((v,ex)->minecraft.execute(()->{if(!dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)||ex!=null)return;for(var line:v.getAsJsonArray("lines")){var l=line.getAsJsonObject();if(number.equals(l.get("number").getAsString())){organization=true;name=l.get("name").getAsString();}}}));
 }));}
 private String label(UUID id){var entry=minecraft.getConnection()==null?null:minecraft.getConnection().getPlayerInfo(id);String n=entry==null?BackendNumberService.getNameFor(id):entry.getProfile().name();return n==null||n.isBlank()?id.toString():n;}
 private void update(){if(call==null)return;call.active=ready&&error.isEmpty()&&VoicechatPluginImpl.isReady()&&!PrivateCalls.busy()&&!SimpleVoiceCallClient.callManager.isInCall();secure.active=call.active&&target!=null&&CallControlService.supports(target,"private_call_v1");callback.active=ready&&target!=null&&CallControlService.supports(target,"callback_v1");voicemail.active=ready&&target!=null&&voicemailEligible&&!VoicemailClient.busy();}
 private void dial(){if(!call.active)return;
  if(organization){remember();minecraft.setScreenAndShow(new DialAttemptScreen(number));return;}
  if(!byNumber){remember();if(PhoneDialer.call(target,name,number))minecraft.setScreenAndShow(new ActiveCallScreen());return;}
  call.active=false;
  BackendNumberService.resolveFreshAsync(number).whenComplete((id,e)->minecraft.execute(()->{
    if(!dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this))return;if(e!=null){error="Не удалось перепроверить номер";update();return;}
    if(expectedTarget!=null&&!expectedTarget.equals(id)){target=null;error="Номер больше не соответствует контакту";update();PhoneMessages.show(error);return;}
   if(!Objects.equals(id,target)){target=id;name=id==null?"Номер не найден":label(id);error="";voicemailEligible=false;nextVoicemailCheck=0;checkVoicemail();update();PhoneMessages.show("Адресат изменился — проверьте номер ещё раз");return;}
   remember();minecraft.setScreenAndShow(new DialAttemptScreen(number,target,target==null?"invalid_number":null));
  }));
 }
 private void remember(){SimpleVoiceCallClient.config.lastDialedNumbers.put(ServerProfiles.current(),number);SimpleVoiceCallClient.config.save();}
 public void tick(){if(++ticks%10==0){checkVoicemail();update();}}
 public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,mx,my);c.centeredText(font,title,f.centerX(),f.y()+12,0xFFFFFFFF);
  String[] lines={number+" → "+name,"Голосовой чат: "+(VoicechatPluginImpl.isReady()?"подключён":"нет подключения"),"Набор: "+(CallStandard.isLegacy()?"Legacy через Fox Mobile":CallControlService.available()?"Fox Mobile доступен":"нет подтверждения связи"),target==null?"":("Приватный режим: "+(CallControlService.supports(target,"private_call_v1")?"поддерживается":"недоступен")),error};
  for(int i=0;i<lines.length;i++)PhoneGuiTextures.drawCenteredTrimmedText(c,font,Component.literal(lines[i]),f.centerX(),f.y()+32+i*15,f.width()-28,0xFFFFFFFF);super.extractRenderState(c,mx,my,d);
 }
 public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
}

