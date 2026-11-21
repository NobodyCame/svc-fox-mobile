package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class ContactRingtonesScreen extends Screen {
    private final Screen parent;private int page;
    public ContactRingtonesScreen(Screen parent){super(Component.literal("Рингтоны контактов"));this.parent=parent;}
    private static java.util.List<String> choices(){
      var list=new java.util.ArrayList<>(java.util.List.of("nokia","office","iphone","blackberry","atomic","samsung","silent"));
      var dir=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID).resolve("ringtones");
      try(var files=java.nio.file.Files.list(dir)){files.filter(java.nio.file.Files::isRegularFile).map(p->p.getFileName().toString()).filter(n->n.toLowerCase().matches(".*\\.(mp3|ogg|wav)$")).sorted().forEach(n->list.add("file:"+n));}catch(Exception ignored){}return list;
    }
    protected void init(){var contacts=SimpleVoiceCallClient.config.contacts.stream().filter(c->ServerProfiles.matches(c.server)&&c.uuid!=null).toList();int rows=Math.max(1,(height-100)/26),start=page*rows;
        if(start>=contacts.size()&&page>0){page--;rebuildWidgets();return;}
        int w=Math.min(460,width-20),x=(width-w)/2;
        for(int i=start;i<Math.min(start+rows,contacts.size());i++){var contact=contacts.get(i);String key=ServerProfiles.current()+"/"+contact.uuid;var pref=SimpleVoiceCallClient.config.contactRingtones.get(key);int y=38+(i-start)*26;
            addRenderableWidget(Button.builder(Component.literal(contact.name+": "+(pref==null?"общий":pref.ringtone)),b->{var c=SimpleVoiceCallClient.config;var p=c.contactRingtones.computeIfAbsent(key,k->new ModConfig.RingtonePreference());var choices=choices();p.ringtone=choices.get((choices.indexOf(p.ringtone)+1)%choices.size());c.save();rebuildWidgets();}).bounds(x,y,w-110,20).build());
            addRenderableWidget(Button.builder(Component.literal(pref==null?"Общая":Math.round(pref.volume*100)+"%"),b->{var c=SimpleVoiceCallClient.config;var p=c.contactRingtones.computeIfAbsent(key,k->new ModConfig.RingtonePreference());p.volume=(Math.round(p.volume*10)+1)%11/10f;c.save();rebuildWidgets();}).bounds(x+w-106,y,66,20).build());
            addRenderableWidget(Button.builder(Component.literal("↺"),b->{SimpleVoiceCallClient.config.contactRingtones.remove(key);SimpleVoiceCallClient.config.save();rebuildWidgets();}).bounds(x+w-36,y,36,20).build());
        }
        addRenderableWidget(Button.builder(Component.literal("←"),b->{if(page>0)page--;rebuildWidgets();}).bounds(x,height-50,50,20).build());
        addRenderableWidget(Button.builder(Component.literal("→"),b->{if((page+1)*rows<contacts.size())page++;rebuildWidgets();}).bounds(x+54,height-50,50,20).build());
        addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(x+110,height-50,w-110,20).build());
    }
    public void extractRenderState(GuiGraphicsExtractor c,int x,int y,float d){PhoneGuiTextures.fullBackground(c,width,height,x,y);c.centeredText(font,title,width/2,14,0xFFFFFFFF);super.extractRenderState(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
    public void onClose(){minecraft.setScreenAndShow(parent);}public boolean isPauseScreen(){return false;}
}

