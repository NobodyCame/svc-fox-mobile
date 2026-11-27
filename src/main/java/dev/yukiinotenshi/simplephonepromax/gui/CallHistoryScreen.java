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
    protected void init(){var f=PhoneGuiTextures.dialogLayout(width,height,142);int w=f.width()-24,x=f.x()+12,top=f.y()+24,h=height<160?14:20,footer=f.bottom()-h-5;
        int arrow=Math.min(40,w/4),rowTop=top+2*(h+4);
        EditBox search=new EditBox(font,x,top,w-64,h,Component.literal("Имя или номер"));search.setValue(query);search.setResponder(value->{query=value;page=0;});addRenderableWidget(search);search.setHint(Component.literal("Имя или номер"));addRenderableWidget(Button.builder(Component.literal("Поиск"),b->{page=0;rebuildWidgets();}).bounds(x+w-60,top,60,h).build());
        addRenderableWidget(Button.builder(Component.literal(missedOnly?"Пропущенные":"Все звонки"),b->{missedOnly=!missedOnly;page=0;rebuildWidgets();}).bounds(x,top+h+4,w/2-2,h).build());
        addRenderableWidget(Button.builder(Component.literal(allServers?"Все серверы":"Этот сервер"),b->{allServers=!allServers;page=0;rebuildWidgets();}).bounds(x+w/2+2,top+h+4,w-w/2-2,h).build());
        int count=Math.max(1,PhoneGuiTextures.pageSize(rowTop,footer,h,4));var rows=entries();page=Math.min(page,Math.max(0,(rows.size()-1)/count));
        for(int i=page*count;i<Math.min(rows.size(),(page+1)*count);i++){var entry=rows.get(i);String date=new java.text.SimpleDateFormat("dd.MM HH:mm").format(new Date(entry.timestampMs));String label=date+"  "+Objects.toString(entry.name,entry.number)+"  "+reason(entry.type)+(entry.count>1?" ×"+entry.count:"");addRenderableWidget(Button.builder(Component.literal(font.plainSubstrByWidth(label,w-12)),b->minecraft.setScreenAndShow(new Detail(this,entry))).bounds(x,rowTop+(i-page*count)*(h+4),w,h).build());}
        addRenderableWidget(Button.builder(Component.literal("←"),b->{if(page>0)page--;rebuildWidgets();}).bounds(x,footer,arrow,h).build()).active=page>0;
        addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<rows.size())page++;rebuildWidgets();}).bounds(x+arrow+4,footer,arrow,h).build()).active=(page+1)*count<rows.size();
        addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x+2*(arrow+4),footer,w-2*(arrow+4),h).build());
    }
    private static String reason(String t){if(t==null)return "Причина неизвестна";return switch(t){case "dnd"->"Тихий режим";case "dnd_remote"->"Не беспокоить";case "missed_in"->"Пропущен";case "missed_out"->"Нет ответа";case "busy_out","busy_in"->"Занят";case "declined_in","declined_out"->"Отклонён";case "completed_in","completed_out","ended"->"Разговор завершён";case "canceled_in","canceled_out"->"Отменён";case "transferred"->"Переадресован";default->t;};}
    public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);var f=PhoneGuiTextures.dialogLayout(width,height,142);PhoneGuiTextures.drawUniversalBackground(c,f,x,y);c.centeredText(font,title,f.centerX(),f.y()+10,0xFFFFFFFF);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
    public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
    private static final class Detail extends Screen {
        private final Screen parent;private final ModConfig.CallHistoryEntry entry;private boolean resolving;
        Detail(Screen parent,ModConfig.CallHistoryEntry e){super(Component.literal(Objects.toString(e.name,"Звонок")));this.parent=parent;entry=e;}
        private int buttonHeight(){return height<160?14:20;}
        private int buttonStep(){return buttonHeight()+(height<160?2:4);}
        private int firstButtonY(PhoneGuiTextures.Frame f){return f.bottom()-5-buttonHeight()-2*buttonStep();}
        protected void init(){var f=PhoneGuiTextures.dialogLayout(width,height,142);int w=f.width()-24,x=f.x()+12,h=buttonHeight();
            int y=firstButtonY(f);
            var person=Button.builder(Component.literal("Позвонить тому же игроку"),b->{try{dial(UUID.fromString(entry.uuid));}catch(Exception ignored){}}).bounds(x,y,w,h).build();person.active=ServerProfiles.matches(entry.server);addRenderableWidget(person);
            if(entry.dialedNumber!=null&&!entry.dialedNumber.isBlank()){var number=Button.builder(Component.literal("Набрать заново: "+entry.dialedNumber),b->{if(resolving)return;resolving=true;PhoneDialer.resolve(entry.dialedNumber).whenComplete((id,e)->minecraft.execute(()->{resolving=false;if(dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this)&&e==null&&id!=null&&PhoneDialer.call(id,entry.name,entry.dialedNumber))minecraft.setScreenAndShow(new ActiveCallScreen());}));}).bounds(x,y+buttonStep(),w,h).build();number.active=ServerProfiles.matches(entry.server);addRenderableWidget(number);}
            addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x,y+2*buttonStep(),w,h).build());
        }
        private void dial(UUID id){if(PhoneDialer.call(id,entry.name))minecraft.setScreenAndShow(new ActiveCallScreen());}
        public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){
            dev.yukiinotenshi.simplephonepromax.sound.WallpaperEngine.render(c,width,height);
            var f=PhoneGuiTextures.dialogLayout(width,height,142);
            PhoneGuiTextures.drawUniversalBackground(c,f,x,y);
            int w=f.width()-24, textY=f.y()+18, end=firstButtonY(f)-3;
            PhoneGuiTextures.drawCenteredTrimmedText(c,font,title,f.centerX(),f.y()+5,w,0xFFFFFFFF);
            String[] details={reason(entry.type)+" · "+entry.durationSec+" с",
                  "Набрано: "+Objects.toString(entry.dialedNumber,"—"),
                  "Номер игрока: "+Objects.toString(entry.number,"—"),
                  "Сервер: "+Objects.toString(entry.server,"старая запись без сервера")};
            for(String line:details){
                if(textY+font.lineHeight>end)break;
                PhoneGuiTextures.drawTrimmedText(c,font,line,f.x()+12,textY,w,0xFFFFFFFF,false);
                textY+=font.lineHeight;
            }
            super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);
        }
        public void onClose(){minecraft.setScreenAndShow(parent);}public boolean isPauseScreen(){return false;}
    }
}

