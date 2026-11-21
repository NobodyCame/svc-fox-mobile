package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class CallHistoryScreen extends Screen {
    private int page;private boolean missedOnly,allServers;private String query="";
    public CallHistoryScreen(){super(Component.literal("История звонков"));}
    private List<ModConfig.CallHistoryEntry> entries(){return SimpleVoiceCallClient.config.callHistory.stream().filter(e->allServers||ServerProfiles.matches(e.server)).filter(e->!missedOnly||Objects.toString(e.type,"").startsWith("missed")||"dnd".equals(e.type)).filter(e->(Objects.toString(e.name,"")+" "+Objects.toString(e.number,"")).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();}
    protected void init(){var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,top=f.y()+30,footer=f.bottom()-30;
        EditBox search=new EditBox(font,x,top,w-64,20,Component.literal("Имя или номер"));search.setValue(query);search.setResponder(value->{query=value;page=0;});addRenderableWidget(search);search.setHint(Component.literal("Имя или номер"));addRenderableWidget(Button.builder(Component.literal("Поиск"),b->{page=0;rebuildWidgets();}).bounds(x+w-60,top,60,20).build());
        addRenderableWidget(Button.builder(Component.literal(missedOnly?"Пропущенные":"Все звонки"),b->{missedOnly=!missedOnly;page=0;rebuildWidgets();}).bounds(x,top+24,w/2-2,20).build());
        addRenderableWidget(Button.builder(Component.literal(allServers?"Все серверы":"Этот сервер"),b->{allServers=!allServers;page=0;rebuildWidgets();}).bounds(x+w/2+2,top+24,w/2-2,20).build());
        int count=Math.max(1,(footer-top-56)/24);var rows=entries();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
        for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var entry=rows.get(i);String date=new java.text.SimpleDateFormat("dd.MM HH:mm").format(new Date(entry.timestampMs));String label=date+"  "+Objects.toString(entry.name,entry.number)+"  "+reason(entry.type)+(entry.count>1?" ×"+entry.count:"");addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,w-12)),b->minecraft.setScreenAndShow(new Detail(this,entry))).bounds(x,top+50+(i-page*count)*24,w,20).build());}
        addRenderableWidget(Button.builder(Component.literal("←"),b->{if(page>0)page--;rebuildWidgets();}).bounds(x,footer,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<rows.size())page++;rebuildWidgets();}).bounds(x+44,footer,40,20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x+w-100,footer,100,20).build());
    }
    private static String reason(String t){if(t==null)return "Причина неизвестна";return switch(t){case "dnd"->"Тихий режим";case "dnd_remote"->"Не беспокоить";case "missed_in"->"Пропущен";case "missed_out"->"Нет ответа";case "busy_out","busy_in"->"Занят";case "declined_in","declined_out"->"Отклонён";case "completed_in","completed_out","ended"->"Разговор завершён";case "canceled_in","canceled_out"->"Отменён";case "transferred"->"Переадресован";default->t;};}
    public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.centeredText(font,title,f.centerX(),f.y()+10,0xFFFFFFFF);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
    public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
    private static final class Detail extends Screen {
        private final Screen parent;private final ModConfig.CallHistoryEntry entry;private boolean resolving;
        Detail(Screen parent,ModConfig.CallHistoryEntry e){super(Component.literal(Objects.toString(e.name,"Звонок")));this.parent=parent;entry=e;}
        protected void init(){var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,y=f.y()+100;
            var person=Button.builder(Component.literal("Позвонить тому же игроку"),b->{try{dial(UUID.fromString(entry.uuid));}catch(Exception ignored){}}).bounds(x,y,w,20).build();person.active=ServerProfiles.matches(entry.server);addRenderableWidget(person);
            if(entry.dialedNumber!=null&&!entry.dialedNumber.isBlank()){var number=Button.builder(Component.literal("Набрать заново: "+entry.dialedNumber),b->{if(resolving)return;resolving=true;PhoneDialer.resolve(entry.dialedNumber).whenComplete((id,e)->minecraft.execute(()->{resolving=false;if(dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)&&e==null&&id!=null&&PhoneDialer.call(id,entry.name,entry.dialedNumber))minecraft.setScreenAndShow(new ActiveCallScreen());}));}).bounds(x,y+24,w,20).build();number.active=ServerProfiles.matches(entry.server);addRenderableWidget(number);}
            addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,y+52,w,20).build());
        }
        private void dial(UUID id){if(PhoneDialer.call(id,entry.name))minecraft.setScreenAndShow(new ActiveCallScreen());}
        public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.centeredText(font,title,f.centerX(),f.y()+10,0xFFFFFFFF);c.textWithWordWrap(font,Component.literal(reason(entry.type)+"; длительность: "+entry.durationSec+" с\nНабрано: "+Objects.toString(entry.dialedNumber,"—")+"; номер игрока: "+Objects.toString(entry.number,"—")+"\nСервер: "+Objects.toString(entry.server,"старая запись без сервера")),f.x()+16,f.y()+30,f.width()-32,0xFFFFFFFF);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
        public void onClose(){minecraft.setScreenAndShow(parent);}public boolean isPauseScreen(){return false;}
    }
}

