package dev.yukiinotenshi.simplephonepromax.gui;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneMessages;
import dev.yukiinotenshi.simplephonepromax.phone.ServerProfiles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Adds or edits a local contact label and its changeable phone number. */
public class PhoneNickEditScreen extends Screen {
   private final Screen parent;
   private final UUID targetUuid;
   private final String realName;
   private final String initialNickname;
   private final String initialNumber;
   private EditBox nameField;
   private EditBox numberField;
   private final List<Button> texturedButtons = new ArrayList<>();

   public PhoneNickEditScreen(Screen parent, UUID targetUuid, String targetNumber, String realName, String currentCustomName) {
      super(targetUuid == null ? Component.literal("Добавить контакт") : Component.translatable("phone.contact.edit.title"));
      this.parent = parent;
      this.targetUuid = targetUuid;
      this.realName = realName != null ? realName : "";
      this.initialNickname = currentCustomName != null ? currentCustomName : "";
      this.initialNumber = PhoneNumberManager.onlyDigits(targetNumber);
   }

   protected void init() {
      super.init();
      this.texturedButtons.clear();
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      int cx = frame.centerX();
      int w = Math.min(frame.px(158), frame.width() - frame.px(28));
      int h = Math.max(12, frame.px(13));
      int x = cx - w / 2;
      int nameY = frame.y() + frame.px(55);
      this.nameField = new EditBox(this.font, x, nameY, w, h, Component.translatable("phone.contact.edit.name"));
      this.nameField.setMaxLength(48);
      this.nameField.setHint(Component.translatable("phone.contact.edit.name_placeholder"));
      this.nameField.setValue(this.initialNickname);
      this.addRenderableWidget(this.nameField);

      int numberY = frame.y() + frame.px(79);
      this.numberField = new EditBox(this.font, x, numberY, w, h, Component.translatable("phone.contact.edit.number_label"));
      this.numberField.setMaxLength(18);
      this.numberField.setHint(Component.translatable("phone.contact.edit.number_placeholder"));
      this.numberField.setValue(this.initialNumber);
      this.addRenderableWidget(this.numberField);
      this.setInitialFocus(this.nameField);

      int buttonY = frame.y() + frame.px(103);
      int gap = Math.max(2, frame.px(2));
      int buttonW = (w - 2 * gap) / 3;
      this.addTexturedButton(Button.builder(Component.translatable("phone.contact.edit.save"), b -> this.saveContact())
         .size(buttonW, h).pos(x, buttonY).build());
      this.addTexturedButton(Button.builder(Component.translatable("phone.contact.edit.cancel"), b -> this.returnToParent())
         .size(buttonW, h).pos(x + buttonW + gap, buttonY).build());
      this.addTexturedButton(Button.builder(Component.translatable("phone.contact.edit.reset"), b -> {
         this.nameField.setValue(this.realName);
         this.saveContact();
      }).size(buttonW, h).pos(x + 2 * (buttonW + gap), buttonY).build());
   }

   private void addTexturedButton(Button button) {
      this.texturedButtons.add(button);
      this.addRenderableWidget(button);
   }

   private void saveContact() {
      String nickname = this.nameField == null ? "" : this.nameField.getValue().trim();
      String number = PhoneNumberManager.onlyDigits(this.numberField == null ? "" : this.numberField.getValue());
      boolean validNumber = PhoneNumberManager.isValidKnownNumber(number);
      if (nickname.isEmpty() || !validNumber) {
         PhoneMessages.show(nickname.isEmpty() ? "Введите имя контакта" : "Номер должен содержать 1–3, 6 или 7–15 цифр");
         return;
      }

      String uuid = this.targetUuid == null ? null : this.targetUuid.toString();
      ModConfig.Contact found = null;
      for (ModConfig.Contact contact : SimpleVoiceCallClient.config.contacts) {
         if (!ServerProfiles.matches(contact.server)) continue;
         String savedNumber = PhoneNumberManager.onlyDigits(contact.number);
         if ((!this.initialNumber.isEmpty() && this.initialNumber.equals(savedNumber))
            || (this.initialNumber.isEmpty() && savedNumber.isEmpty() && uuid != null && uuid.equals(contact.uuid))
            || number.equals(savedNumber)) {
            found = contact;
            break;
         }
      }
      if (found == null) {
         found = new ModConfig.Contact(this.realName, number, uuid);
         SimpleVoiceCallClient.config.contacts.add(found);
      }
      found.name = this.realName.isBlank() ? nickname : this.realName;
      found.nickname = nickname;
      found.number = number;
      if (uuid != null) found.uuid = uuid;
      found.server = ServerProfiles.current();
      SimpleVoiceCallClient.config.save();
      PhoneMessages.show("Контакт сохранён: " + nickname);
      this.returnToParent();
   }

   private void returnToParent() {
      if (this.minecraft != null) this.minecraft.setScreenAndShow(this.parent != null ? this.parent : new PhoneContactsScreen());
   }

   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      PhoneGuiTextures.Frame frame = PhoneGuiTextures.layout(this.width, this.height);
      PhoneGuiTextures.drawUniversalBackground(context, frame, mouseX, mouseY);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.font, this.title, frame.centerX(), frame.y() + frame.px(8), frame.px(170), 0xFFFFE070);
      PhoneGuiTextures.drawCenteredTrimmedText(context, this.font, Component.literal(this.realName.isBlank() ? "Номер без сохранённого имени" : this.realName),
         frame.centerX(), frame.y() + frame.px(28), frame.px(170), 0xFFFFFFFF);
      PhoneGuiTextures.drawTrimmedText(context, this.font, "Подпись контакта", this.nameField.getX(), this.nameField.getY() - this.font.lineHeight - 2, this.nameField.getWidth(), 0xFFFFE070, true);
      PhoneGuiTextures.drawTrimmedText(context, this.font, "Номер (можно изменить позже)", this.numberField.getX(), this.numberField.getY() - this.font.lineHeight - 2, this.numberField.getWidth(), 0xFFFFE070, true);
      super.extractRenderState(context, mouseX, mouseY, delta);
      PhoneGuiTextures.widgets(this, context, mouseX, mouseY);
      for (Button button : this.texturedButtons) PhoneGuiTextures.drawButton(context, this.font, button, mouseX, mouseY);
   }

   public boolean isPauseScreen() { return false; }
   public boolean shouldCloseOnEsc() { return true; }
   public void onClose() { this.returnToParent(); }
}
