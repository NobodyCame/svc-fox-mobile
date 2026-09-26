from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Callable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/resources/assets/svc-fox-mobile"
PHONE_DIR = ASSET_ROOT / "textures/gui/phone"
ITEM_DIR = ASSET_ROOT / "textures/item"

PHONE_SIZE = (640, 1040)
ICON_SIZE = 32


@dataclass(frozen=True)
class Palette:
    slug: str
    name: str
    outline: tuple[int, int, int, int]
    shadow: tuple[int, int, int, int]
    dark: tuple[int, int, int, int]
    mid: tuple[int, int, int, int]
    light: tuple[int, int, int, int]
    accent: tuple[int, int, int, int]
    accent2: tuple[int, int, int, int]
    screen: tuple[int, int, int, int]
    screen2: tuple[int, int, int, int]


PALETTES = [
    Palette(
        "netherite_gold",
        "Netherite Gold",
        (12, 11, 14, 255),
        (0, 0, 0, 90),
        (31, 29, 35, 255),
        (64, 60, 70, 255),
        (114, 107, 122, 255),
        (255, 204, 99, 255),
        (184, 137, 55, 255),
        (14, 17, 22, 238),
        (24, 30, 38, 230),
    ),
    Palette(
        "deepslate_cyan",
        "Deepslate Cyan",
        (8, 12, 15, 255),
        (0, 0, 0, 90),
        (26, 34, 38, 255),
        (55, 69, 75, 255),
        (112, 130, 135, 255),
        (92, 226, 236, 255),
        (38, 148, 166, 255),
        (8, 18, 23, 238),
        (14, 39, 48, 230),
    ),
    Palette(
        "copper_patina",
        "Copper Patina",
        (28, 13, 8, 255),
        (0, 0, 0, 90),
        (92, 46, 28, 255),
        (161, 83, 46, 255),
        (232, 141, 75, 255),
        (92, 223, 175, 255),
        (39, 143, 128, 255),
        (20, 24, 22, 238),
        (20, 44, 38, 230),
    ),
    Palette(
        "amethyst",
        "Amethyst",
        (16, 10, 22, 255),
        (0, 0, 0, 90),
        (45, 32, 70, 255),
        (87, 60, 134, 255),
        (164, 116, 220, 255),
        (237, 194, 255, 255),
        (125, 80, 190, 255),
        (15, 13, 23, 238),
        (37, 24, 54, 230),
    ),
    Palette(
        "emerald",
        "Emerald",
        (5, 19, 12, 255),
        (0, 0, 0, 90),
        (18, 66, 41, 255),
        (36, 132, 79, 255),
        (97, 210, 130, 255),
        (255, 224, 106, 255),
        (59, 176, 96, 255),
        (8, 22, 16, 238),
        (15, 47, 28, 230),
    ),
    Palette(
        "iron_redstone",
        "Iron Redstone",
        (19, 17, 17, 255),
        (0, 0, 0, 90),
        (74, 72, 70, 255),
        (137, 134, 130, 255),
        (220, 217, 207, 255),
        (255, 84, 77, 255),
        (160, 33, 33, 255),
        (21, 20, 20, 238),
        (44, 26, 27, 230),
    ),
]


def rgba(color: tuple[int, int, int, int]) -> tuple[int, int, int, int]:
    return color


def block_round_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    radius: int,
    fill: tuple[int, int, int, int],
) -> None:
    x0, y0, x1, y1 = xy
    steps = [
        (radius, 0),
        (radius // 2, radius // 4),
        (radius // 4, radius // 2),
        (0, radius),
    ]
    points = [
        (x0 + radius, y0),
        (x1 - radius, y0),
        (x1 - steps[1][0], y0 + steps[1][1]),
        (x1 - steps[2][0], y0 + steps[2][1]),
        (x1, y0 + radius),
        (x1, y1 - radius),
        (x1 - steps[2][0], y1 - steps[2][1]),
        (x1 - steps[1][0], y1 - steps[1][1]),
        (x1 - radius, y1),
        (x0 + radius, y1),
        (x0 + steps[1][0], y1 - steps[1][1]),
        (x0 + steps[2][0], y1 - steps[2][1]),
        (x0, y1 - radius),
        (x0, y0 + radius),
        (x0 + steps[2][0], y0 + steps[2][1]),
        (x0 + steps[1][0], y0 + steps[1][1]),
    ]
    draw.polygon(points, fill=fill)


def draw_beveled_rect(
    draw: ImageDraw.ImageDraw,
    xy: tuple[int, int, int, int],
    palette: Palette,
    radius: int = 34,
) -> None:
    x0, y0, x1, y1 = xy
    block_round_rect(draw, (x0 + 12, y0 + 16, x1 + 12, y1 + 16), radius, palette.shadow)
    block_round_rect(draw, (x0, y0, x1, y1), radius, palette.outline)
    block_round_rect(draw, (x0 + 10, y0 + 10, x1 - 10, y1 - 10), radius - 8, palette.dark)
    block_round_rect(draw, (x0 + 24, y0 + 24, x1 - 24, y1 - 24), radius - 16, palette.mid)
    block_round_rect(draw, (x0 + 44, y0 + 44, x1 - 44, y1 - 44), radius - 24, palette.dark)
    draw.line((x0 + 46, y0 + 72, x0 + 46, y1 - 88), fill=palette.light, width=6)
    draw.line((x0 + 66, y0 + 48, x1 - 90, y0 + 48), fill=palette.light, width=6)
    draw.line((x1 - 46, y0 + 94, x1 - 46, y1 - 92), fill=palette.outline, width=8)
    draw.line((x0 + 96, y1 - 46, x1 - 96, y1 - 46), fill=palette.outline, width=8)


def draw_phone_base(palette: Palette) -> Image.Image:
    img = Image.new("RGBA", PHONE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    draw_beveled_rect(draw, (58, 18, 582, 1022), palette)

    # Side buttons.
    draw.rectangle((42, 238, 60, 356), fill=palette.outline)
    draw.rectangle((46, 246, 60, 348), fill=palette.mid)
    draw.rectangle((580, 300, 598, 430), fill=palette.outline)
    draw.rectangle((580, 310, 594, 420), fill=palette.mid)

    # Speaker and camera.
    draw.rectangle((242, 78, 398, 110), fill=palette.outline)
    draw.rectangle((258, 86, 382, 102), fill=palette.dark)
    for x in range(270, 374, 20):
        draw.rectangle((x, 90, x + 8, 98), fill=palette.light)
    draw.rectangle((424, 74, 466, 116), fill=palette.outline)
    draw.rectangle((434, 84, 456, 106), fill=palette.screen2)
    draw.rectangle((440, 88, 448, 96), fill=palette.accent)

    # Screen well.
    draw.rectangle((94, 138, 546, 764), fill=palette.outline)
    draw.rectangle((106, 150, 534, 752), fill=palette.screen)
    draw.rectangle((118, 162, 522, 740), fill=palette.screen2)
    for y in range(186, 722, 36):
        draw.rectangle((126, y, 514, y + 2), fill=(255, 255, 255, 13))
    draw.rectangle((132, 178, 294, 184), fill=(255, 255, 255, 40))
    draw.rectangle((132, 190, 226, 196), fill=(255, 255, 255, 28))

    # Accent bottom control slab.
    draw.rectangle((122, 806, 518, 938), fill=palette.outline)
    draw.rectangle((134, 818, 506, 926), fill=palette.dark)
    draw.rectangle((156, 840, 484, 904), fill=(0, 0, 0, 90))

    # Pixel control gems.
    controls = [(220, 872), (320, 872), (420, 872)]
    for cx, cy in controls:
        draw.rectangle((cx - 28, cy - 28, cx + 28, cy + 28), fill=palette.outline)
        draw.rectangle((cx - 20, cy - 20, cx + 20, cy + 20), fill=palette.mid)
        draw.rectangle((cx - 12, cy - 12, cx + 12, cy + 12), fill=palette.accent)
        draw.rectangle((cx - 10, cy - 10, cx + 4, cy - 4), fill=palette.light)

    # Decorative ore-like pixels.
    ore = [
        (122, 92), (140, 96), (494, 88), (510, 94), (116, 780),
        (526, 782), (106, 958), (512, 958), (164, 58), (470, 60),
    ]
    for i, (x, y) in enumerate(ore):
        fill = palette.accent if i % 2 == 0 else palette.accent2
        draw.rectangle((x, y, x + 10, y + 10), fill=palette.outline)
        draw.rectangle((x + 2, y + 2, x + 8, y + 8), fill=fill)

    # Glass highlights.
    draw.polygon([(126, 166), (288, 166), (126, 334)], fill=(255, 255, 255, 22))
    draw.polygon([(420, 168), (518, 168), (518, 300)], fill=(255, 255, 255, 16))

    return img


def draw_wallpaper(slug: str, colors: list[tuple[int, int, int, int]]) -> Image.Image:
    img = Image.new("RGBA", (404, 578), colors[0])
    draw = ImageDraw.Draw(img)

    for y in range(0, img.height, 16):
        c = colors[(y // 16) % len(colors)]
        draw.rectangle((0, y, img.width, y + 15), fill=c)

    for i in range(0, 14):
        offset = i * 34
        c = colors[(i + 2) % len(colors)]
        draw.polygon(
            [
                (0, offset + 80),
                (92, offset + 18),
                (202, offset + 96),
                (308, offset + 34),
                (404, offset + 104),
                (404, offset + 162),
                (306, offset + 96),
                (202, offset + 154),
                (90, offset + 78),
                (0, offset + 142),
            ],
            fill=tuple([c[0], c[1], c[2], min(c[3], 190)]),
        )

    # Small pixel stars/runes.
    for idx in range(52):
        x = (idx * 73 + len(slug) * 11) % 384 + 8
        y = (idx * 47 + len(slug) * 23) % 548 + 10
        c = colors[(idx + 1) % len(colors)]
        draw.rectangle((x, y, x + 3, y + 3), fill=(c[0], c[1], c[2], 210))

    draw.rectangle((0, 0, img.width - 1, img.height - 1), outline=(255, 255, 255, 24), width=2)
    return img


def icon_canvas() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    img = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def draw_icon(kind: str, color: tuple[int, int, int, int]) -> Image.Image:
    img, draw = icon_canvas()
    dark = (18, 18, 20, 255)
    muted = (145, 145, 150, 255)
    c = color

    def rect(x0: int, y0: int, x1: int, y1: int, fill=c) -> None:
        draw.rectangle((x0, y0, x1, y1), fill=fill)

    if kind == "call":
        rect(7, 6, 12, 10)
        rect(5, 9, 10, 18)
        rect(8, 18, 14, 24)
        rect(14, 21, 24, 26)
        rect(20, 17, 26, 22)
        rect(6, 6, 13, 7, dark)
    elif kind == "hangup":
        rect(7, 20, 25, 25, (232, 66, 58, 255))
        rect(5, 16, 11, 22, (232, 66, 58, 255))
        rect(21, 16, 27, 22, (232, 66, 58, 255))
    elif kind == "contacts":
        rect(7, 7, 25, 25)
        rect(10, 10, 22, 22, (25, 25, 28, 255))
        rect(13, 12, 19, 18)
        rect(11, 20, 21, 22)
        rect(5, 10, 7, 13)
        rect(5, 17, 7, 20)
    elif kind == "settings":
        rect(14, 4, 17, 8)
        rect(14, 24, 17, 28)
        rect(4, 14, 8, 17)
        rect(24, 14, 28, 17)
        rect(8, 8, 11, 11)
        rect(21, 8, 24, 11)
        rect(8, 21, 11, 24)
        rect(21, 21, 24, 24)
        draw.rectangle((11, 11, 21, 21), outline=c, width=3)
        rect(15, 15, 17, 17, (25, 25, 28, 255))
    elif kind == "backspace":
        draw.polygon([(4, 16), (12, 8), (27, 8), (27, 24), (12, 24)], fill=c)
        rect(13, 14, 15, 16, dark)
        rect(20, 14, 22, 16, dark)
        rect(15, 12, 20, 14, dark)
        rect(15, 16, 20, 18, dark)
    elif kind == "history":
        draw.arc((5, 5, 26, 26), 35, 330, fill=c, width=4)
        draw.polygon([(5, 14), (5, 5), (13, 9)], fill=c)
        rect(15, 9, 17, 17)
        rect(17, 16, 23, 18)
    elif kind == "mute":
        rect(5, 13, 10, 19)
        draw.polygon([(10, 12), (18, 7), (18, 25), (10, 20)], fill=muted)
        rect(22, 10, 25, 22, (232, 66, 58, 255))
        rect(18, 14, 29, 17, (232, 66, 58, 255))
    elif kind == "unmute":
        rect(5, 13, 10, 19)
        draw.polygon([(10, 12), (18, 7), (18, 25), (10, 20)], fill=c)
        draw.arc((17, 10, 27, 22), -42, 42, fill=c, width=3)
        draw.arc((16, 6, 31, 26), -42, 42, fill=c, width=2)
    elif kind == "plus":
        rect(14, 6, 18, 26)
        rect(6, 14, 26, 18)
    elif kind == "close":
        rect(8, 6, 12, 10)
        rect(20, 6, 24, 10)
        rect(12, 10, 16, 14)
        rect(16, 10, 20, 14)
        rect(14, 14, 18, 18)
        rect(12, 18, 16, 22)
        rect(16, 18, 20, 22)
        rect(8, 22, 12, 26)
        rect(20, 22, 24, 26)
    elif kind == "arrow_left":
        draw.polygon([(6, 16), (18, 6), (18, 12), (26, 12), (26, 20), (18, 20), (18, 26)], fill=c)
    elif kind == "arrow_right":
        draw.polygon([(26, 16), (14, 6), (14, 12), (6, 12), (6, 20), (14, 20), (14, 26)], fill=c)
    elif kind == "heart":
        rect(8, 8, 13, 13, (232, 66, 86, 255))
        rect(19, 8, 24, 13, (232, 66, 86, 255))
        rect(6, 12, 26, 19, (232, 66, 86, 255))
        rect(10, 19, 22, 23, (232, 66, 86, 255))
        rect(14, 23, 18, 26, (232, 66, 86, 255))
    else:
        rect(8, 8, 24, 24)

    return img


def draw_phone_item() -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rectangle((4, 1, 12, 15), fill=(13, 13, 16, 255))
    draw.rectangle((5, 2, 11, 14), fill=(56, 57, 62, 255))
    draw.rectangle((6, 4, 10, 11), fill=(14, 29, 39, 255))
    draw.rectangle((6, 4, 10, 4), fill=(75, 205, 222, 255))
    draw.rectangle((7, 13, 9, 13), fill=(255, 209, 104, 255))
    draw.point((10, 2), fill=(255, 209, 104, 255))
    return img


def save_scaled(img: Image.Image, path: Path, factor: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if factor == 1:
        img.save(path)
        return
    img.resize((img.width // factor, img.height // factor), Image.Resampling.NEAREST).save(path)


def make_preview(
    images: list[tuple[str, Image.Image]],
    path: Path,
    thumb: tuple[int, int],
    columns: int,
) -> None:
    font = ImageFont.load_default()
    rows = (len(images) + columns - 1) // columns
    label_h = 22
    pad = 16
    sheet = Image.new(
        "RGBA",
        (columns * (thumb[0] + pad) + pad, rows * (thumb[1] + label_h + pad) + pad),
        (16, 16, 18, 255),
    )
    draw = ImageDraw.Draw(sheet)
    for i, (name, img) in enumerate(images):
        col = i % columns
        row = i // columns
        x = pad + col * (thumb[0] + pad)
        y = pad + row * (thumb[1] + label_h + pad)
        preview = img.resize(thumb, Image.Resampling.NEAREST)
        sheet.alpha_composite(preview, (x, y))
        draw.text((x, y + thumb[1] + 4), name, fill=(232, 232, 232, 255), font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)


def main() -> None:
    PHONE_DIR.mkdir(parents=True, exist_ok=True)
    ITEM_DIR.mkdir(parents=True, exist_ok=True)

    generated: list[str] = []
    phone_previews: list[tuple[str, Image.Image]] = []

    for palette in PALETTES:
        img = draw_phone_base(palette)
        file_2x = PHONE_DIR / f"phone_{palette.slug}_2x.png"
        file_1x = PHONE_DIR / f"phone_{palette.slug}.png"
        img.save(file_2x)
        save_scaled(img, file_1x, factor=2)
        generated.extend([file_1x.relative_to(ROOT).as_posix(), file_2x.relative_to(ROOT).as_posix()])
        phone_previews.append((palette.slug, img))

    wallpapers = {
        "blue": [(9, 22, 39, 255), (12, 45, 74, 255), (23, 92, 121, 255), (70, 178, 198, 255)],
        "pink": [(38, 15, 36, 255), (79, 26, 67, 255), (150, 62, 113, 255), (245, 154, 183, 255)],
        "green": [(10, 29, 20, 255), (19, 65, 43, 255), (44, 126, 71, 255), (108, 220, 125, 255)],
        "dark": [(9, 9, 12, 255), (20, 20, 26, 255), (34, 34, 45, 255), (84, 84, 104, 255)],
        "sunset": [(55, 19, 39, 255), (112, 45, 55, 255), (211, 92, 58, 255), (255, 183, 91, 255)],
        "aurora": [(8, 16, 25, 255), (11, 45, 58, 255), (35, 168, 123, 255), (155, 94, 228, 255)],
    }
    wallpaper_previews: list[tuple[str, Image.Image]] = []
    for slug, colors in wallpapers.items():
        img = draw_wallpaper(slug, colors)
        path = PHONE_DIR / f"wallpaper_{slug}.png"
        img.save(path)
        generated.append(path.relative_to(ROOT).as_posix())
        wallpaper_previews.append((slug, img))

    icon_kinds = [
        "call",
        "hangup",
        "contacts",
        "settings",
        "backspace",
        "history",
        "mute",
        "unmute",
        "plus",
        "close",
        "arrow_left",
        "arrow_right",
        "heart",
    ]
    icon_color = (255, 218, 143, 255)
    icon_images: list[tuple[str, Image.Image]] = []
    atlas = Image.new("RGBA", (ICON_SIZE * 8, ICON_SIZE * 2), (0, 0, 0, 0))
    for idx, kind in enumerate(icon_kinds):
        img = draw_icon(kind, icon_color)
        path = PHONE_DIR / f"icon_{kind}.png"
        img.save(path)
        generated.append(path.relative_to(ROOT).as_posix())
        icon_images.append((kind, img))
        atlas.alpha_composite(img, ((idx % 8) * ICON_SIZE, (idx // 8) * ICON_SIZE))
    atlas_path = PHONE_DIR / "icons_atlas.png"
    atlas.save(atlas_path)
    generated.append(atlas_path.relative_to(ROOT).as_posix())

    item = draw_phone_item()
    item_path = ITEM_DIR / "phone.png"
    item.save(item_path)
    generated.append(item_path.relative_to(ROOT).as_posix())

    make_preview(phone_previews, PHONE_DIR / "preview_phone_variants.png", (160, 260), 3)
    make_preview(wallpaper_previews, PHONE_DIR / "preview_wallpapers.png", (202, 289), 3)
    make_preview(icon_images, PHONE_DIR / "preview_icons.png", (64, 64), 7)
    generated.extend(
        [
            (PHONE_DIR / "preview_phone_variants.png").relative_to(ROOT).as_posix(),
            (PHONE_DIR / "preview_wallpapers.png").relative_to(ROOT).as_posix(),
            (PHONE_DIR / "preview_icons.png").relative_to(ROOT).as_posix(),
        ]
    )

    manifest = PHONE_DIR / "TEXTURES.md"
    manifest.write_text(
        "# Fox Mobile phone textures\n\n"
        "Generated by `tools/generate_phone_textures.py`.\n\n"
        "Recommended GUI usage:\n\n"
        "- Use `phone_<variant>.png` for a 320x520 logical phone panel.\n"
        "- Use `phone_<variant>_2x.png` when drawing a larger panel or when you want extra crisp scaling.\n"
        "- Draw text and buttons in code over the screen area; do not bake text into these textures.\n"
        "- Screen content safe area on 1x phone textures is roughly x=59..261, y=81..370.\n"
        "- Full 1x phone texture size is 320x520. Full 2x size is 640x1040.\n\n"
        "Generated files:\n\n"
        + "\n".join(f"- `{path}`" for path in generated)
        + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()

