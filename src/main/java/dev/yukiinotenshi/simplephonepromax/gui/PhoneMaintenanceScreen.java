package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.VoicechatPluginImpl;
import dev.yukiinotenshi.simplephonepromax.compat.CallStandard;
import dev.yukiinotenshi.simplephonepromax.network.CallControlService;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public final class PhoneMaintenanceScreen extends Screen {
    private final Screen parent;
    private String status="";
    private SettingsBackup.Data pending;
    public PhoneMaintenanceScreen(Screen parent){super(Text.literal("Диагностика и резервная копия"));this.parent=parent;}
    private String report(){return "Оператор: "+CallStandard.label(SimpleVoiceCallClient.config.callStandard)+"\nSVC: "+(VoicechatPluginImpl.isReady()?"подключён":"недоступен")+"\nСигнализация: "+CallControlService.status()+"\nUUID в игре: "+(client.player==null?"нет":client.player.getUuid())+"\nКонтакты этого сервера: "+SimpleVoiceCallClient.config.contacts.stream().filter(c->ServerProfiles.matches(c.server)).count()+"\nГруппа: "+(VoicechatPluginImpl.getOwnGroupId()==null?"нет":"есть")+"\nРасширенные функции требуют совместимого клиента у собеседника.";}
    protected void init(){int w=Math.min(340,width-24),x=(width-w)/2,y=Math.max(100,height/2);
        addDrawableChild(ButtonWidget.builder(Text.literal("Экспорт диагностики без секретов"),b->{try{Path p=FabricLoader.getInstance().getConfigDir().resolve("simple-phone-diagnostics.txt");Files.writeString(p,report());status="Сохранено: config/"+p.getFileName();}catch(Exception e){status="Ошибка сохранения";}}).dimensions(x,y,w,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить резервную копию"),b->{try{SettingsBackup.exportSettings();status="Сохранено: config/simple-phone-backup.json";}catch(Exception e){status="Ошибка сохранения";}}).dimensions(x,y+24,w,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Прочитать копию и показать изменения"),b->{try{pending=SettingsBackup.read();int n=SettingsBackup.additions(pending);status="Новых контактов: "+n+"; совпадения сохранятся. Настройки будут заменены.";clearAndInit();}catch(Exception e){pending=null;status="Ошибка: "+e.getMessage();}}).dimensions(x,y+48,w,20).build());
        if(pending!=null)addDrawableChild(ButtonWidget.builder(Text.literal("Применить прочитанную копию"),b->{try{SettingsBackup.apply(pending);pending=null;status="Импорт завершён";clearAndInit();}catch(Exception e){status="Ошибка: "+e.getMessage();}}).dimensions(x,y+72,w,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x,Math.min(height-24,y+100),w,20).build());
    }
    public void render(DrawContext c,int mx,int my,float delta){PhoneGuiTextures.fullBackground(c,width,height,mx,my);c.drawCenteredTextWithShadow(textRenderer,title,width/2,12,0xFFFFFFFF);int y=30;for(String line:report().split("\n")){c.drawTextWithShadow(textRenderer,line,12,y,0xFFFFFFFF);y+=11;}c.drawWrappedTextWithShadow(textRenderer,Text.literal(status),12,height-50,width-24,0xFFFFD07A);super.render(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
    public void close(){client.setScreen(parent);}
    public boolean shouldPause(){return false;}
}

