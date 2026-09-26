package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;
public final class EncryptedCallScreen extends Screen {
    private int page,ticks;
    private final java.util.Map<String,UUID> resolvedContacts=new java.util.HashMap<>();
    private final java.util.Set<String> checkedContacts=new java.util.HashSet<>();
    private boolean resolvingContact;
    private UUID contactId(dev.yukiinotenshi.simplephonepromax.config.ModConfig.Contact c){return resolvedContacts.getOrDefault(c.uuid,UUID.fromString(c.uuid));}
    private void refreshOneContact(){if(resolvingContact||!CallControlService.available())return;for(var contact:SimpleVoiceCallClient.config.contacts){
       if(contact.uuid==null||contact.number==null||!(contact.server==null||contact.server.isBlank()||ServerProfiles.matches(contact.server))||!checkedContacts.add(contact.uuid))continue;
       resolvingContact=true;dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.resolveFreshAsync(contact.number).whenComplete((id,e)->client.execute(()->{resolvingContact=false;if(id!=null)resolvedContacts.put(contact.uuid,id);if(client.currentScreen==this)clearAndInit();}));break;
    }}
    public EncryptedCallScreen(){super(Text.literal("Шифрованные звонки — пока не работают"));}
    protected void init(){int w=Math.min(480,width-24),x=(width-w)/2,y=100;
        if(EncryptedCalls.hasIncoming()&&!EncryptedCalls.active()){
            addDrawableChild(ButtonWidget.builder(Text.literal("Принять"),b->{EncryptedCalls.accept();clearAndInit();}).dimensions(x,y,w/2-2,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Отклонить"),b->{EncryptedCalls.end();clearAndInit();}).dimensions(x+w/2+2,y,w/2-2,20).build());
        }else if(EncryptedCalls.active()){
            if(!EncryptedCalls.comparison().isEmpty())addDrawableChild(ButtonWidget.builder(Text.literal("Скопировать код"),b->client.keyboard.setClipboard(EncryptedCalls.comparison())).dimensions(x,y+56,w,20).build());
            if(EncryptedCalls.needsVerification())addDrawableChild(ButtonWidget.builder(Text.literal("Коды совпадают — разрешить голос"),b->{EncryptedCalls.verify();clearAndInit();}).dimensions(x,y,w,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Завершить / код не совпадает"),b->{EncryptedCalls.end();clearAndInit();}).dimensions(x,y+28,w,20).build());
        }else{
            var contacts=SimpleVoiceCallClient.config.contacts.stream().filter(c->{try{return (c.server==null||c.server.isBlank()||ServerProfiles.matches(c.server))&&CallControlService.supports(contactId(c),"encrypted_voice_v1");}catch(Exception e){return false;}}).toList();
            int count=Math.max(1,(height-160)/24);page=Math.min(page,Math.max(0,(contacts.size()-1)/count));
            for(int i=page*count;i<Math.min(contacts.size(),(page+1)*count);i++){var contact=contacts.get(i);addDrawableChild(ButtonWidget.builder(Text.literal(contact.name),b->EncryptedCalls.call(contactId(contact))).dimensions(x,y+(i-page*count)*24,w,20).build());}
            if(contacts.isEmpty())addDrawableChild(ButtonWidget.builder(Text.literal("Совместимых абонентов пока нет"),b->{}).dimensions(x,y,w,20).build()).active=false;
            addDrawableChild(ButtonWidget.builder(Text.literal("←"),b->{if(page>0)page--;clearAndInit();}).dimensions(x,height-50,40,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("→"),b->{if((page+1)*count<contacts.size())page++;clearAndInit();}).dimensions(x+44,height-50,40,20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Свернуть"),b->close()).dimensions(x+w-120,height-26,120,20).build());
    }
    public void tick(){if(++ticks%20==0){refreshOneContact();clearAndInit();}}
    public void render(DrawContext c,int mx,int my,float delta){PhoneGuiTextures.fullBackground(c,width,height,mx,my);c.drawCenteredTextWithShadow(textRenderer,title,width/2,12,0xFFFFFFFF);c.drawWrappedTextWithShadow(textRenderer,Text.literal(EncryptedCalls.status()),12,32,width-24,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,Text.literal(EncryptedCalls.comparison()),width/2,62,0xFF9DE58D);c.drawWrappedTextWithShadow(textRenderer,Text.literal("Голос через ретранслятор. До включения голоса сравните весь код лично или через другой доверенный канал."),12,76,width-24,0xFFFFD07A);super.render(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
    public void close(){client.setScreen(new PhoneMainScreen());}public boolean shouldPause(){return false;}
}

