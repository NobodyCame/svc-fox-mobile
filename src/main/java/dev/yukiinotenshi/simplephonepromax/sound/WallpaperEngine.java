package dev.yukiinotenshi.simplephonepromax.sound;

import dev.yukiinotenshi.simplephonepromax.SimpleVoiceCallClient;
import net.minecraft.client.gui.DrawContext;

public class WallpaperEngine {
   public static void render(DrawContext ctx, int screenWidth, int screenHeight) {
      if (ctx != null) {
         String wp = SimpleVoiceCallClient.config.wallpaper;
         if (wp == null) {
            wp = "default";
         }

         boolean useCustom = SimpleVoiceCallClient.config.useCustomWallpaper
            && SimpleVoiceCallClient.config.customWallpaper != null
            && !SimpleVoiceCallClient.config.customWallpaper.isEmpty();
         if (useCustom) {
            String fn = SimpleVoiceCallClient.config.customWallpaper.toLowerCase();
            if (fn.contains("red") || fn.contains("красн") || fn.contains("burgund") || fn.contains("бордо") || fn.contains("blood") || fn.contains("кровав")) {
               wp = "crimson";
            } else if (fn.contains("purple") || fn.contains("пурпур") || fn.contains("фиолет") || fn.contains("violet") || fn.contains("лила")) {
               wp = "purple";
            } else if (fn.contains("sunset")
               || fn.contains("закат")
               || fn.contains("orange")
               || fn.contains("оранж")
               || fn.contains("warm")
               || fn.contains("тёпл")) {
               wp = "sunset";
            } else if (fn.contains("aurora")
               || fn.contains("аврора")
               || fn.contains("северн")
               || fn.contains("polar")
               || fn.contains("полярн")
               || fn.contains("mint")
               || fn.contains("мятн")) {
               wp = "aurora";
            } else if (fn.contains("ocean")
               || fn.contains("океан")
               || fn.contains("sea")
               || fn.contains("мор")
               || fn.contains("water")
               || fn.contains("вод")
               || fn.contains("aqua")
               || fn.contains("аква")) {
               wp = "ocean";
            } else if (fn.contains("gold")
               || fn.contains("золот")
               || fn.contains("yellow")
               || fn.contains("жёлт")
               || fn.contains("yellow?")
               || fn.contains("amber")
               || fn.contains("янтар")) {
               wp = "gold";
            } else if (fn.contains("matrix")
               || fn.contains("матриц")
               || fn.contains("green")
               || fn.contains("зелён")
               || fn.contains("lime")
               || fn.contains("лайм")
               || fn.contains("lucky")
               || fn.contains("удачн")
               || fn.contains("hacker")
               || fn.contains("хакер")) {
               wp = "matrix";
            } else if (fn.contains("forest")
               || fn.contains("лес")
               || fn.contains("nature")
               || fn.contains("природ")
               || fn.contains("grass")
               || fn.contains("трав")
               || fn.contains("leaf")
               || fn.contains("лист")) {
               wp = "forest";
            } else if (fn.contains("neon")
               || fn.contains("неон")
               || fn.contains("retro")
               || fn.contains("ретро")
               || fn.contains("synth")
               || fn.contains("синт")
               || fn.contains("disco")
               || fn.contains("диско")) {
               wp = "neon";
            } else if (fn.contains("blue")
               || fn.contains("син")
               || fn.contains("sky")
               || fn.contains("небес")
               || fn.contains("navy")
               || fn.contains("тёмно-син")
               || fn.contains("ice")
               || fn.contains("лёд")) {
               wp = "blue";
            } else if (fn.contains("pink")
               || fn.contains("роз")
               || fn.contains("girl")
               || fn.contains("девочк")
               || fn.contains("barbie")
               || fn.contains("барби")
               || fn.contains("love")
               || fn.contains("любов")) {
               wp = "pink";
            } else if (!fn.contains("dark")
               && !fn.contains("тёмн")
               && !fn.contains("black")
               && !fn.contains("чёрн")
               && !fn.contains("night")
               && !fn.contains("ночь")
               && !fn.contains("goth")
               && !fn.contains("гот")) {
               int hash = Math.abs(SimpleVoiceCallClient.config.customWallpaper.hashCode());
               String[] all = new String[]{"crimson", "purple", "sunset", "aurora", "ocean", "gold", "matrix", "forest", "neon", "blue", "pink"};
               wp = all[hash % all.length];
            } else {
               wp = "dark";
            }
         }

         switch (wp.toLowerCase()) {
            case "blue":
               drawGradient(ctx, screenWidth, screenHeight, -872408525, -872402074, -872388950, -869033473);
               break;
            case "pink":
               drawGradient(ctx, screenWidth, screenHeight, -869072862, -865730492, -861260954, -855668566);
               break;
            case "green":
               drawGradient(ctx, screenWidth, screenHeight, -872406511, -872397790, -870152141, -863436954);
               break;
            case "dark":
               drawSolid(ctx, screenWidth, screenHeight, -587202560);
               break;
            case "crimson":
               drawGradient(ctx, screenWidth, screenHeight, -869662720, -865730543, -861274078, -855694797);
               break;
            case "purple":
               drawGradient(ctx, screenWidth, screenHeight, -871038925, -869072794, -865717334, -861243393);
               break;
            case "sunset":
               drawGradient(ctx, screenWidth, screenHeight, -869072879, -863489263, -855677423, -855655629);
               break;
            case "aurora":
               drawGradient(ctx, screenWidth, screenHeight, -872408525, -872389035, -867902072, -861208645);
               break;
            case "ocean":
               drawGradient(ctx, screenWidth, screenHeight, -872408542, -872393370, -872380246, -867906322);
               break;
            case "gold":
               drawGradient(ctx, screenWidth, screenHeight, -869064192, -863476224, -859006720, -855642522);
               break;
            case "matrix":
               drawGradient(ctx, screenWidth, screenHeight, -872415232, -872408576, -872393472, -870121694);
               break;
            case "forest":
               drawGradient(ctx, screenWidth, screenHeight, -872408576, -870169583, -866814174, -861208747);
               break;
            case "neon":
               drawGradient(ctx, screenWidth, screenHeight, -872415198, -870186838, -856817562, -855681554);
               break;
            default:
               int w = screenWidth;
               int h = screenHeight;
               ctx.fill(0, 0, w, h, -1442840576);
               int bars = 12;

               for (int i = 0; i < bars; i++) {
                  int y1 = i * h / bars;
                  int y2 = (i + 1) * h / bars;
                  int alpha = 51 + i * 6;
                  if (alpha > 136) {
                     alpha = 136;
                  }

                  int col = (alpha & 0xFF) << 24 | 8772;
                  ctx.fill(0, y1, w, y2, col);
               }
         }
      }
   }

   private static void drawSolid(DrawContext ctx, int w, int h, int argb) {
      ctx.fill(0, 0, w, h, argb);
   }

   private static void drawGradient(DrawContext ctx, int w, int h, int c0, int c1, int c2, int c3) {
      int steps = 22;
      int[] cols = new int[]{c0, c1, c2, c3};

      for (int s = 0; s < steps; s++) {
         int y1 = s * h / steps;
         int y2 = (s + 1) * h / steps;
         double t = (s + 0.5) / steps;
         double p = t * (cols.length - 1);
         int lo = (int)Math.floor(p);
         int hi = Math.min(cols.length - 1, lo + 1);
         double frac = p - lo;
         int col = mixArgb(cols[lo], cols[hi], frac);
         ctx.fill(0, y1, w, y2, col);
      }
   }

   private static int mixArgb(int a, int b, double frac) {
      int aa = a >> 24 & 0xFF;
      int ar = a >> 16 & 0xFF;
      int ag = a >> 8 & 0xFF;
      int ab = a & 0xFF;
      int ba = b >> 24 & 0xFF;
      int br = b >> 16 & 0xFF;
      int bg = b >> 8 & 0xFF;
      int bb = b & 0xFF;
      int oa = (int)(aa + (ba - aa) * frac);
      int or = (int)(ar + (br - ar) * frac);
      int og = (int)(ag + (bg - ag) * frac);
      int ob = (int)(ab + (bb - ab) * frac);
      return (oa & 0xFF) << 24 | (or & 0xFF) << 16 | (og & 0xFF) << 8 | ob & 0xFF;
   }
}



