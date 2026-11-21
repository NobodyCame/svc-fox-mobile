package dev.yukiinotenshi.simplephonepromax.phone;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
public final class PhoneMessages {
 public static void show(String text){var c=Minecraft.getInstance();if(c.player==null)return;
  String s=text.replaceAll("^[^\\p{L}\\p{N}]+","").replaceAll("\\s*\\([^)]*(?:таймаут|ждём|45 сек)[^)]*\\)","").replaceAll("^45 сек.*","Нет ответа").replace("Simple Voice Chat","сеть");
  s=s.replace("Подожди 5 секунд перед следующим звонком","Повторите вызов позже").replace("Звонок не доставлен: собеседник заблокировал звонки от тебя","Вызов недоступен").replace("Абонент занят, перезвоните позже","Абонент занят").replace("Подключились к групповому звонку — ","Группа: ").replace("Звонок принят — ","На связи: ").replace("Разговор окончен. Длительность ","Разговор: ");
  c.player.sendSystemMessage(Component.literal(s).withStyle(ChatFormatting.YELLOW));
 }
}

