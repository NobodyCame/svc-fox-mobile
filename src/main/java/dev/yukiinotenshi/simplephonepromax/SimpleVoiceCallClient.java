/*
 * Copyright (c) 2026 YukiiNoTenshi. All rights reserved.
 *
 * Fox Mobile client source is protected by copyright. Public availability,
 * including review or processing by AI tools, does not grant permission to
 * reproduce or adapt protected expression from this work. Unauthorized use
 * may violate Civil Code of the Russian Federation (Part IV), including
 * Articles 1229 and 1270; Article 1301 provides civil remedies for proven
 * infringement. Computer programs and their source/object code are protected
 * under Article 1261. Article 1259 protects expression, not ideas, methods,
 * or functionality as such. Authorship and the author's name are addressed
 * separately by Article 1265.
 *
 * References:
 * https://www.consultant.ru/document/cons_doc_LAW_64629/98ad2641f95945c4b7956150260564c8b44028d9/  (Art. 1229)
 * https://www.consultant.ru/document/cons_doc_LAW_64629/dffcf0b87b80ff38f430dc822a0074e76ccd41a0/  (Art. 1270)
 * https://www.consultant.ru/document/cons_doc_LAW_64629/ce1359ed5b9bd99896d7a496c7887e7c223a2cbc/  (Art. 1261)
 * https://www.consultant.ru/document/cons_doc_LAW_64629/be05678dc42ddc67aae5be9ba9beebd367fb9a3f/  (Art. 1259)
 * https://www.consultant.ru/document/cons_doc_LAW_64629/01cf40c9e42efacdb8dff7cdd410e2542bbbfdf4/  (Art. 1265)
 * https://www.consultant.ru/document/cons_doc_LAW_64629/c2f79b53ce582e92680379e2ebd23eeb9fb7855a/  (Art. 1301)
 *
 * This notice is informational and does not replace applicable law or a
 * separate written license. Whether particular conduct infringes copyright
 * depends on the facts and applicable law; AI use alone does not establish
 * infringement. See LICENSE for the project terms.
 */
package dev.yukiinotenshi.simplephonepromax;

import dev.yukiinotenshi.simplephonepromax.call.CallManager;
import dev.yukiinotenshi.simplephonepromax.config.ModConfig;
import dev.yukiinotenshi.simplephonepromax.gui.IncomingCallHud;
import dev.yukiinotenshi.simplephonepromax.network.ModNetworking;
import dev.yukiinotenshi.simplephonepromax.phone.BackendNumberService;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneDetector;
import dev.yukiinotenshi.simplephonepromax.phone.PhoneNumberManager;
import dev.yukiinotenshi.simplephonepromax.sound.SoundManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleVoiceCallClient implements ClientModInitializer {
   public static final String MOD_ID = "svc-fox-mobile";
   public static final String RESOURCE_ID = MOD_ID;
   public static final Logger LOGGER = LoggerFactory.getLogger("Fox Mobile");
   public static final String AUTHOR = "YukiiNoTenshi";
   public static ModConfig config;
   public static CallManager callManager;
   public static SoundManager soundManager;
   public static PhoneDetector phoneDetector;

   public void onInitializeClient() {
      LOGGER.info("Fox Mobile initializing...");
      dev.yukiinotenshi.simplephonepromax.config.ConfigMigration.run();
      config = new ModConfig();
      config.load();
      dev.yukiinotenshi.simplephonepromax.compat.CallStandard.initialize();
      callManager = dev.yukiinotenshi.simplephonepromax.compat.CallStandard.isLegacy() ? new dev.yukiinotenshi.simplephonepromax.compat.LegacyCallManager() : new CallManager();
      soundManager = new SoundManager();
      phoneDetector = new PhoneDetector();
      PhoneNumberManager.load();
      BackendNumberService.load();
      ModNetworking.register();
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         callManager.tick();
         soundManager.tick();
         phoneDetector.tick(client);
         BackendNumberService.tick(client);
         ModNetworking.tick();
         dev.yukiinotenshi.simplephonepromax.network.VoicemailClient.tick();
         dev.yukiinotenshi.simplephonepromax.network.OrganizationCalls.tick();
         dev.yukiinotenshi.simplephonepromax.network.EncryptedCalls.tick();
         dev.yukiinotenshi.simplephonepromax.network.PrivateCalls.tick();
         dev.yukiinotenshi.simplephonepromax.sound.OperatorSounds.tick();
      });
      HudRenderCallback.EVENT.register((drawContext, tickCounter) -> IncomingCallHud.render(drawContext));
      LOGGER.info("Fox Mobile initialized successfully");
   }
}



