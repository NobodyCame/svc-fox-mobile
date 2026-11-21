package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.network.*;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class EncryptedCallScreen extends Screen {
    private int page,ticks;
    private final java.util.Map<String,UUID> resolvedContacts=new java.util.HashMap<>();
    private final java.util.Set<String> checkedContacts=new java.util.HashSet<>();
    private boolean resolvingContact;
    private UUID contactId(dev.yukiinotenshi.simplephonepromax.config.ModConfig.Contact c){return resolvedContacts.getOrDefault(c.uuid,UUID.fromString(c.uuid));}
    private void refreshOneContact(){if(resolvingContact||!CallControlService.available())return;for(var contact:SimpleVoiceCallClient.config.contacts){
       if(contact.uuid==null||contact.number==null||!(contact.server==null||contact.server.isBlank()||ServerProfiles.matches(contact.server))||!checkedContacts.add(contact.uuid))continue;
       resolvingContact=true;dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService.resolveFreshAsync(contact.number).whenComplete((id,e)->minecraft.execute(()->{resolvingContact=false;if(id!=null)resolvedContacts.put(contact.uuid,id);if(dev.yukiinotenshi.simplephonepromax.gui.ScreenTracker.isCurrent(this))rebuildWidgets();}));break;
    }}
    public EncryptedCallScreen(){super(Component.literal("Шифрованные звонки — пока не работают"));}
    protected void init(){int w=Math.min(480,width-24),x=(width-w)/2,y=100;
        if(EncryptedCalls.hasIncoming()&&!EncryptedCalls.active()){
            addRenderableWidget(Button.builder(Component.literal("Принять"),b->{EncryptedCalls.accept();rebuildWidgets();}).bounds(x,y,w/2-2,20).build());
            addRenderableWidget(Button.builder(Component.literal("Отклонить"),b->{EncryptedCalls.end();rebuildWidgets();}).bounds(x+w/2+2,y,w/2-2,20).build());
        }else if(EncryptedCalls.active()){
            if(!EncryptedCalls.comparison().isEmpty())addRenderableWidget(Button.builder(Component.literal("Скопировать код"),b->minecraft.keyboardHandler.setClipboard(EncryptedCalls.comparison())).bounds(x,y+56,w,20).build());
            if(EncryptedCalls.needsVerification())addRenderableWidget(Button.builder(Component.literal("Коды совпадают — разрешить голос"),b->{EncryptedCalls.verify();rebuildWidgets();}).bounds(x,y,w,20).build());
            addRenderableWidget(Button.builder(Component.literal("Завершить / код не совпадает"),b->{EncryptedCalls.end();rebuildWidgets();}).bounds(x,y+28,w,20).build());
        }else{
            var contacts=SimpleVoiceCallClient.config.contacts.stream().filter(c->{try{return (c.server==null||c.server.isBlank()||ServerProfiles.matches(c.server))&&CallControlService.supports(contactId(c),"encrypted_voice_v1");}catch(Exception e){return false;}}).toList();
            int count=Math.max(1,(height-160)/24);page=Math.min(page,Math.max(0,(contacts.size()-1)/count));
            for(int i=page*count;i<Math.min(contacts.size(),(page+1)*count);i++){var contact=contacts.get(i);addRenderableWidget(Button.builder(Component.literal(contact.name),b->EncryptedCalls.call(contactId(contact))).bounds(x,y+(i-page*count)*24,w,20).build());}
            if(contacts.isEmpty())addRenderableWidget(Button.builder(Component.literal("Совместимых абонентов пока нет"),b->{}).bounds(x,y,w,20).build()).active=false;
            addRenderableWidget(Button.builder(Component.literal("←"),b->{if(page>0)page--;rebuildWidgets();}).bounds(x,height-50,40,20).build());
            addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*count<contacts.size())page++;rebuildWidgets();}).bounds(x+44,height-50,40,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Свернуть"),b->onClose()).bounds(x+w-120,height-26,120,20).build());
    }
    public void tick(){if(++ticks%20==0){refreshOneContact();rebuildWidgets();}}
    public void extractRenderState(GuiGraphicsExtractor c,int mx,int my,float delta){PhoneGuiTextures.fullBackground(c,width,height,mx,my);c.centeredText(font,title,width/2,12,0xFFFFFFFF);c.textWithWordWrap(font,Component.literal(EncryptedCalls.status()),12,32,width-24,0xFFFFFFFF);c.centeredText(font,Component.literal(EncryptedCalls.comparison()),width/2,62,0xFF9DE58D);c.textWithWordWrap(font,Component.literal("Голос через ретранслятор. До включения голоса сравните весь код лично или через другой доверенный канал."),12,76,width-24,0xFFFFD07A);super.extractRenderState(c,mx,my,delta);PhoneGuiTextures.widgets(this,c,mx,my);}
    public void onClose(){minecraft.setScreenAndShow(new PhoneMainScreen());}public boolean isPauseScreen(){return false;}
}

