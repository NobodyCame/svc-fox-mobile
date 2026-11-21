package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.*;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;

public class PhoneSettingsScreen extends Screen {
 private String category=""; private int page; private int x,y,w,bottom;
 private final List<Runnable> controls=new ArrayList<>();
 private boolean callBusy(){return SimpleVoiceCallClient.callManager.isInCall()||dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy();}
 private ModConfig config(){return SimpleVoiceCallClient.config;}
 public PhoneSettingsScreen(){super(Component.literal("Настройки"));}
 protected void init(){
  var f=PhoneGuiTextures.layout(width,height);x=f.x()+16;w=f.width()-32;y=f.y()+30;bottom=f.bottom()-32;
  controls.clear();
  switch(category){
   case "" -> {for(String name:List.of("Рингтон","Громкость","Уведомления","Вызовы","Оператор и данные"))button(name,()->{category=name;page=0;rebuildWidgets();});}
   case "Рингтон" -> {
    String[] ids={"nokia","office","iphone","blackberry","atomic","samsung","silent"};
    String[] names={"Nokia","Офисный","iPhone","Blackberry","Atomic Bell","Samsung","Без звука"};
    int index=Math.max(0,Arrays.asList(ids).indexOf(config().ringtone));
    button("Мелодия: "+names[index],()->{stopPreview();config().ringtone=ids[(index+1)%ids.length];config().useCustomRingtone=false;save();});
    button("Свой файл: "+(config().useCustomRingtone?"вкл.":"выкл."),()->{config().useCustomRingtone=!config().useCustomRingtone;save();});
    button("Рингтон слышен рядом через SVC: "+on(config().transmitRingtoneInCall),()->{config().transmitRingtoneInCall=!config().transmitRingtoneInCall;save();});
    Path dir=FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID).resolve("ringtones");
    List<String> files=new ArrayList<>();try{Files.createDirectories(dir);try(var paths=Files.list(dir)){paths.filter(Files::isRegularFile).map(p->p.getFileName().toString()).filter(n->n.toLowerCase().matches(".*\\.(ogg|wav|mp3)$")).sorted().forEach(files::add);}}catch(Exception ignored){}
    button("Файл: "+(files.isEmpty()?"нет файлов":Objects.toString(config().customRingtone,"не выбран")),()->{if(!files.isEmpty()){config().customRingtone=files.get((files.indexOf(config().customRingtone)+1)%files.size());config().useCustomRingtone=true;save();}});
    button("Открыть папку рингтонов",()->{try{Files.createDirectories(dir);java.awt.Desktop.getDesktop().open(dir.toFile());}catch(Exception e){PhoneMessages.show("Не удалось открыть папку рингтонов: "+Objects.toString(e.getMessage(),e.getClass().getSimpleName()));}});
    button("Прослушать / остановить",()->{if(SimpleVoiceCallClient.soundManager.isRingtonePlaying())stopPreview();else SimpleVoiceCallClient.soundManager.playRing(false);},!callBusy());
    button("Рингтоны контактов",()->minecraft.setScreenAndShow(new ContactRingtonesScreen(this)));
   }
   case "Громкость" -> {
    volume("Рингтон",()->config().ringtoneVolume,v->config().ringtoneVolume=v);
    volume("Уведомления",()->config().notificationVolume,v->config().notificationVolume=v);
    volume("Набор номера",()->config().dialingVolume,v->config().dialingVolume=v);
    volume("Занято",()->config().busyVolume,v->config().busyVolume=v);
    volume("Нет ответа",()->config().unavailableVolume,v->config().unavailableVolume=v);
    volume("Вторая линия",()->config().waitingVolume,v->config().waitingVolume=v);
    volume("Вне сети",()->config().offlineVolume,v->config().offlineVolume=v);
    volume("Разговор завершён",()->config().endedVolume,v->config().endedVolume=v);
    volume("Автоответчик",()->config().voicemailVolume,v->config().voicemailVolume=v);
    volume("Номер не найден",()->config().invalidNumberVolume,v->config().invalidNumberVolume=v);
   }
   case "Уведомления" -> {
    var ids=List.of("chaos","alpha","beep_once","cosmic-radio");
    button("Звук: "+config().notificationSound,()->{PhoneNotification.stopPreview();config().notificationSound=ids.get((ids.indexOf(config().notificationSound)+1)%ids.size());save();});
    volume("Громкость",()->config().notificationVolume,v->{config().notificationVolume=v;PhoneNotification.stopPreview();});
    button("Прослушать уведомление",PhoneNotification::preview);
    button("Остановить прослушивание",PhoneNotification::stopPreview);
   }
   case "Вызовы" -> {
    button("Входящие: "+switch(config().doNotDisturb){case "favorites"->"избранные";case "none"->"никто";default->"все";},()->{config().doNotDisturb=switch(config().doNotDisturb){case "all"->"favorites";case "favorites"->"none";default->"all";};save();});
    button("Вторая линия: "+on(config().allowCallWaiting),()->{config().allowCallWaiting=!config().allowCallWaiting;save();});
    button("Тихие часы: "+on(config().quietSchedule),()->{config().quietSchedule=!config().quietSchedule;save();});
    button("Начало: "+config().quietStartHour+":00",()->{config().quietStartHour=(config().quietStartHour+1)%24;save();});
    button("Конец: "+config().quietEndHour+":00",()->{config().quietEndHour=(config().quietEndHour+1)%24;save();});
    button("Закрытая группа: "+on(config().passwordProtectedCalls),()->{config().passwordProtectedCalls=!config().passwordProtectedCalls;save();},!callBusy());
    for(String id:new ArrayList<>(config().blockedPlayers))button("Разблокировать: "+id,()->{config().removeBlocked(id);save();});
    if(SimpleVoiceCallClient.callManager.isInActiveCall())button("Управление группой",()->minecraft.setScreenAndShow(new GroupParticipantsScreen()));
   }
   case "Оператор и данные" -> {
    button("Оператор: "+dev.yukiinotenshi.simplephonepromax.compat.CallStandard.label(config().callStandard),()->minecraft.setScreenAndShow(new PhoneCallStandardScreen(this)),!callBusy());
    button("Показывать мой номер всем: "+on(config().showOwnNumberInDirectory),()->{config().showOwnNumberInDirectory=!config().showOwnNumberInDirectory;save();BackendNumberService.registerSelfAsync(minecraft);},BackendNumberService.isEnabled());
    button("Мой номер: "+PhoneNumberManager.formatNumber(PhoneNumberManager.getMyDisplayNumber()),()->minecraft.keyboardHandler.setClipboard(PhoneNumberManager.getMyDisplayNumber()));
    button("Настройки оператора",()->minecraft.setScreenAndShow(new PhoneBackendSettingsScreen(this)));
    button("Проверить связь",()->minecraft.setScreenAndShow(new ConnectionDiagnosticsScreen(this)));
    button("Обновить номер",()->BackendNumberService.registerSelfAsync(minecraft));
    button("Диагностика и резервная копия",()->minecraft.setScreenAndShow(new PhoneMaintenanceScreen(this)));
    button("Привязать старые контакты к серверу",()->{for(var c:config().contacts)if(c.server==null||c.server.isBlank())c.server=ServerProfiles.current();save();});
   }
  }
  int count=Math.max(1,(bottom-y-26)/24), pages=Math.max(1,(controls.size()+count-1)/count);page=Math.min(page,pages-1);
  for(int i=page*count;i<Math.min(controls.size(),(page+1)*count);i++)controls.get(i).run();
  if(pages>1){addRenderableWidget(Button.builder(Component.literal("←"),b->{page=Math.max(0,page-1);rebuildWidgets();}).bounds(x,bottom-24,40,20).build());addRenderableWidget(Button.builder(Component.literal((page+1)+" / "+pages+" →"),b->{page=(page+1)%pages;rebuildWidgets();}).bounds(x+44,bottom-24,90,20).build());}
  addRenderableWidget(Button.builder(Component.literal(category.isEmpty()?"Готово":"К категориям"),b->{stopPreview();if(category.isEmpty())onClose();else{category="";page=0;rebuildWidgets();}}).bounds(x,bottom,w,20).build());
 }
 private static String on(boolean v){return v?"вкл.":"выкл.";}
 private void save(){config().save();rebuildWidgets();}
 private void button(String text,Runnable action){button(text,action,true);}
 private void button(String text,Runnable action,boolean active){controls.add(()->{var b=addRenderableWidget(Button.builder(Component.literal(text),ignored->action.run()).bounds(x,y,w,20).build());b.active=active;y+=24;});}
 private void volume(String label,Supplier<Float> get,Consumer<Float> set){controls.add(()->{addRenderableWidget(new AbstractSliderButton(x,y,w,20,Component.empty(),get.get()){
  {updateMessage();}
  protected void updateMessage(){setMessage(Component.literal(label+": "+(value<0.005?"выкл.":Math.round(value*100)+"%")));}
  protected void applyValue(){set.accept((float)value);config().save();}
 });y+=24;});}
 private void stopPreview(){PhoneNotification.stopPreview();if(SimpleVoiceCallClient.soundManager!=null)SimpleVoiceCallClient.soundManager.stopRing();}
 public void removed(){stopPreview();}
 public void onClose(){stopPreview();config().save();minecraft.setScreenAndShow(dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.busy()?new PrivateCallScreen():callBusy()?new ActiveCallScreen():new PhoneMainScreen());}
 public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,mx,my);c.centeredText(font,category.isEmpty()?"Настройки":category,f.centerX(),f.y()+12,0xFFFFFFFF);super.extractRenderState(c,mx,my,d);}
 public boolean isPauseScreen(){return false;}
}

