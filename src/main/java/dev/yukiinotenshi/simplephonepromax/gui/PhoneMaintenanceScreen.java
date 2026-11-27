package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.nio.file.*;

public final class PhoneMaintenanceScreen extends Screen {
    private final Screen parent;
    private String status="";
    private SettingsBackup.Data pending;
    public PhoneMaintenanceScreen(Screen parent){super(Component.literal("Диагностика и резервная копия"));this.parent=parent;}
    private String report(){return "Набор номера: "+CallStandard.label(SimpleVoiceCallClient.config.callStandard)+"\nSimple Voice Chat: "+(VoicechatPluginImpl.isReady()?"подключён":"недоступен")+"\nСигнализация: "+CallControlService.status()+"\nUUID в игре: "+(minecraft.player==null?"нет":minecraft.player.getUUID())+"\nКонтакты этого сервера: "+SimpleVoiceCallClient.config.contacts.stream().filter(c->ServerProfiles.matches(c.server)).count()+"\nГруппа: "+(VoicechatPluginImpl.getOwnGroupId()==null?"нет":"есть")+"\nРасширенные функции требуют совместимого клиента у собеседника.";}
    protected void init(){int w=Math.min(340,width-24),x=(width-w)/2,y=Math.max(100,height/2);
        addRenderableWidget(Button.builder(Component.literal("Экспорт диагностики без секретов"),b->{try{Path p=FabricLoader.getInstance().getConfigDir().resolve("simple-phone-diagnostics.txt");Files.writeString(p,report());status="Сохранено: config/"+p.getFileName();}catch(Exception e){status="Ошибка сохранения";}}).bounds(x,y,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Сохранить резервную копию"),b->{try{SettingsBackup.exportSettings();status="Сохранено: config/simple-phone-backup.json";}catch(Exception e){status="Ошибка сохранения";}}).bounds(x,y+24,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Прочитать копию и показать изменения"),b->{try{pending=SettingsBackup.read();int n=SettingsBackup.additions(pending);status="Новых контактов: "+n+"; совпадения сохранятся. Настройки будут заменены.";rebuildWidgets();}catch(Exception e){pending=null;status="Ошибка: "+e.getMessage();}}).bounds(x,y+48,w,20).build());
        if(pending!=null)addRenderableWidget(Button.builder(Component.literal("Применить прочитанную копию"),b->{try{SettingsBackup.apply(pending);pending=null;status="Импорт завершён";rebuildWidgets();}catch(Exception e){status="Ошибка: "+e.getMessage();}}).bounds(x,y+72,w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,Math.min(height-24,y+100),w,20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float delta){PhoneGuiTextures.fullBackground(c,width,height,mx,my);c.centeredText(font,title,width/2,12,0xFFFFFFFF);int y=30;for(String line:report().split("\n")){c.text(font,line,12,y,0xFFFFFFFF);y+=11;}c.textWithWordWrap(font,Component.literal(status),12,height-50,width-24,0xFFFFD07A);super.extractRenderState(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
    public void onClose(){minecraft.setScreenAndShow(parent);}
    public boolean isPauseScreen(){return false;}
}

