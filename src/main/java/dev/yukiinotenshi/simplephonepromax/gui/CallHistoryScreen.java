package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import java.util.*;
public final class CallHistoryScreen extends Screen {
    private int page;private boolean missedOnly,allServers;private String query="";
    public CallHistoryScreen(){super(Text.literal("История звонков"));}
    private List<ModConfig.CallHistoryEntry> entries(){return SimpleVoiceCallClient.config.callHistory.stream().filter(e->allServers||ServerProfiles.matches(e.server)).filter(e->!missedOnly||Objects.toString(e.type,"").startsWith("missed")||"dnd".equals(e.type)).filter(e->(Objects.toString(e.name,"")+" "+Objects.toString(e.number,"")).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();}
    protected void init(){var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,top=f.y()+30,footer=f.bottom()-30;
        TextFieldWidget search=new TextFieldWidget(textRenderer,x,top,w-64,20,Text.literal("Имя или номер"));search.setText(query);search.setChangedListener(value->{query=value;page=0;});addDrawableChild(search);search.setPlaceholder(Text.literal("Имя или номер"));addDrawableChild(ButtonWidget.builder(Text.literal("Поиск"),b->{page=0;clearAndInit();}).dimensions(x+w-60,top,60,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(missedOnly?"Пропущенные":"Все звонки"),b->{missedOnly=!missedOnly;page=0;clearAndInit();}).dimensions(x,top+24,w/2-2,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(allServers?"Все серверы":"Этот сервер"),b->{allServers=!allServers;page=0;clearAndInit();}).dimensions(x+w/2+2,top+24,w/2-2,20).build());
        int count=Math.max(1,(footer-top-56)/24);var rows=entries();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
        for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var entry=rows.get(i);String date=new java.text.SimpleDateFormat("dd.MM HH:mm").format(new Date(entry.timestampMs));String label=date+"  "+Objects.toString(entry.name,entry.number)+"  "+reason(entry.type)+(entry.count>1?" ×"+entry.count:"");addDrawableChild(ButtonWidget.builder(Text.literal(textRenderer.trimToWidth(label,w-12)),b->client.setScreen(new Detail(this,entry))).dimensions(x,top+50+(i-page*count)*24,w,20).build());}
        addDrawableChild(ButtonWidget.builder(Text.literal("←"),b->{if(page>0)page--;clearAndInit();}).dimensions(x,footer,40,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("→"),b->{if((page+1)*count<rows.size())page++;clearAndInit();}).dimensions(x+44,footer,40,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x+w-100,footer,100,20).build());
    }
    private static String reason(String t){if(t==null)return "Причина неизвестна";return switch(t){case "dnd"->"Тихий режим";case "dnd_remote"->"Не беспокоить";case "missed_in"->"Пропущен";case "missed_out"->"Нет ответа";case "busy_out","busy_in"->"Занят";case "declined_in","declined_out"->"Отклонён";case "completed_in","completed_out","ended"->"Разговор завершён";case "canceled_in","canceled_out"->"Отменён";case "transferred"->"Переадресован";default->t;};}
    public void render(DrawContext c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+10,0xFFFFFFFF);super.render(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
    public void close(){client.setScreen(new PhoneMainScreen());}public boolean shouldPause(){return false;}
    private static final class Detail extends Screen {
        private final Screen parent;private final ModConfig.CallHistoryEntry entry;private boolean resolving;
        Detail(Screen parent,ModConfig.CallHistoryEntry e){super(Text.literal(Objects.toString(e.name,"Звонок")));this.parent=parent;entry=e;}
        protected void init(){var f=PhoneGuiTextures.layout(width,height);int w=f.width()-32,x=f.x()+16,y=f.y()+100;
            var person=ButtonWidget.builder(Text.literal("Позвонить тому же игроку"),b->{try{dial(UUID.fromString(entry.uuid));}catch(Exception ignored){}}).dimensions(x,y,w,20).build();person.active=ServerProfiles.matches(entry.server);addDrawableChild(person);
            if(entry.dialedNumber!=null&&!entry.dialedNumber.isBlank()){var number=ButtonWidget.builder(Text.literal("Набрать заново: "+entry.dialedNumber),b->{if(resolving)return;resolving=true;PhoneDialer.resolve(entry.dialedNumber).whenComplete((id,e)->client.execute(()->{resolving=false;if(client.currentScreen==this&&e==null&&id!=null&&PhoneDialer.call(id,entry.name,entry.dialedNumber))client.setScreen(new ActiveCallScreen());}));}).dimensions(x,y+24,w,20).build();number.active=ServerProfiles.matches(entry.server);addDrawableChild(number);}
            addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x,y+52,w,20).build());
        }
        private void dial(UUID id){if(PhoneDialer.call(id,entry.name))client.setScreen(new ActiveCallScreen());}
        public void render(DrawContext c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.layout(width,height);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.drawCenteredTextWithShadow(textRenderer,title,f.centerX(),f.y()+10,0xFFFFFFFF);c.drawWrappedTextWithShadow(textRenderer,Text.literal(reason(entry.type)+"; длительность: "+entry.durationSec+" с\nНабрано: "+Objects.toString(entry.dialedNumber,"—")+"; номер игрока: "+Objects.toString(entry.number,"—")+"\nСервер: "+Objects.toString(entry.server,"старая запись без сервера")),f.x()+16,f.y()+30,f.width()-32,0xFFFFFFFF);super.render(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
        public void close(){client.setScreen(parent);}public boolean shouldPause(){return false;}
    }
}

