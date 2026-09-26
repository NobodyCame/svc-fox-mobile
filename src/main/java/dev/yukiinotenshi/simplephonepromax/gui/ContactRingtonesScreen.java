package dev.yukiinotenshi.simplephonepromax.gui;
import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.util.*;
public final class ContactRingtonesScreen extends Screen {
    private final Screen parent;private int page;
    public ContactRingtonesScreen(Screen parent){super(Text.literal("Рингтоны контактов"));this.parent=parent;}
    private static java.util.List<String> choices(){
      var list=new java.util.ArrayList<>(java.util.List.of("nokia","office","iphone","blackberry","atomic","samsung","silent"));
      var dir=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceCallClient.MOD_ID).resolve("ringtones");
      try(var files=java.nio.file.Files.list(dir)){files.filter(java.nio.file.Files::isRegularFile).map(p->p.getFileName().toString()).filter(n->n.toLowerCase().matches(".*\\.(mp3|ogg|wav)$")).sorted().forEach(n->list.add("file:"+n));}catch(Exception ignored){}return list;
    }
    protected void init(){var contacts=SimpleVoiceCallClient.config.contacts.stream().filter(c->ServerProfiles.matches(c.server)&&c.uuid!=null).toList();int rows=Math.max(1,(height-100)/26),start=page*rows;
        if(start>=contacts.size()&&page>0){page--;clearAndInit();return;}
        int w=Math.min(460,width-20),x=(width-w)/2;
        for(int i=start;i<Math.min(start+rows,contacts.size());i++){var contact=contacts.get(i);String key=ServerProfiles.current()+"/"+contact.uuid;var pref=SimpleVoiceCallClient.config.contactRingtones.get(key);int y=38+(i-start)*26;
            addDrawableChild(ButtonWidget.builder(Text.literal(contact.name+": "+(pref==null?"общий":pref.ringtone)),b->{var c=SimpleVoiceCallClient.config;var p=c.contactRingtones.computeIfAbsent(key,k->new ModConfig.RingtonePreference());var choices=choices();p.ringtone=choices.get((choices.indexOf(p.ringtone)+1)%choices.size());c.save();clearAndInit();}).dimensions(x,y,w-110,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal(pref==null?"Общая":Math.round(pref.volume*100)+"%"),b->{var c=SimpleVoiceCallClient.config;var p=c.contactRingtones.computeIfAbsent(key,k->new ModConfig.RingtonePreference());p.volume=(Math.round(p.volume*10)+1)%11/10f;c.save();clearAndInit();}).dimensions(x+w-106,y,66,20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("↺"),b->{SimpleVoiceCallClient.config.contactRingtones.remove(key);SimpleVoiceCallClient.config.save();clearAndInit();}).dimensions(x+w-36,y,36,20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("←"),b->{if(page>0)page--;clearAndInit();}).dimensions(x,height-50,50,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("→"),b->{if((page+1)*rows<contacts.size())page++;clearAndInit();}).dimensions(x+54,height-50,50,20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"),b->close()).dimensions(x+110,height-50,w-110,20).build());
    }
    public void render(DrawContext c,int x,int y,float d){PhoneGuiTextures.fullBackground(c,width,height,x,y);c.drawCenteredTextWithShadow(textRenderer,title,width/2,14,0xFFFFFFFF);super.render(c,x,y,d);PhoneGuiTextures.widgets(this,c,x,y);}
    public void close(){client.setScreen(parent);}public boolean shouldPause(){return false;}
}

