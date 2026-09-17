#!/usr/bin/env python3
"""Paint valid 64x64 Minecraft skins for Papa (Steve/wide) and Mama (Alex/slim)."""

from pathlib import Path

from PIL import Image, ImageDraw

SKIN = (224, 176, 144, 255)
SKIN_SHADOW = (196, 148, 116, 255)
BLONDE = (244, 196, 48, 255)
BLONDE_DARK = (212, 164, 32, 255)
WHITE = (245, 245, 245, 255)
WHITE_SHADOW = (210, 210, 210, 255)
NAVY = (30, 58, 95, 255)
NAVY_SHADOW = (22, 42, 72, 255)
BROWN = (92, 64, 51, 255)
BROWN_SHADOW = (70, 48, 38, 255)
SHOE = (26, 26, 26, 255)
EYE_WHITE = (250, 250, 250, 255)
EYE_DARK = (32, 32, 32, 255)
MOUTH = (160, 90, 80, 255)
DARK_BLONDE = (139, 105, 52, 255)
DARK_BLONDE_LIGHT = (196, 163, 90, 255)
BURGUNDY = (122, 46, 58, 255)
BURGUNDY_SHADOW = (90, 32, 42, 255)
JEAN = (30, 58, 95, 255)
JEAN_SHADOW = (22, 42, 70, 255)
TRANSPARENT = (0, 0, 0, 0)


def fill(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], color: tuple[int, int, int, int]) -> None:
    x, y, w, h = box
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def cuboid(
    draw: ImageDraw.ImageDraw,
    *,
    top: tuple[int, int],
    bottom: tuple[int, int],
    right: tuple[int, int],
    front: tuple[int, int],
    left: tuple[int, int],
    back: tuple[int, int],
    size: tuple[int, int, int],
    top_c,
    bottom_c,
    right_c,
    front_c,
    left_c,
    back_c,
) -> None:
    w, h, d = size
    fill(draw, (*top, w, d), top_c)
    fill(draw, (*bottom, w, d), bottom_c)
    fill(draw, (*right, d, h), right_c)
    fill(draw, (*front, w, h), front_c)
    fill(draw, (*left, d, h), left_c)
    fill(draw, (*back, w, h), back_c)


def draw_head_face(px, ox: int, oy: int, hair, hair_dark, skin, beard=None) -> None:
    # 8x8 front at (ox, oy)
    for x in range(8):
        for y in range(2):
            px[ox + x, oy + y] = hair if y == 0 else hair_dark
    for x in range(8):
        for y in range(2, 8):
            px[ox + x, oy + y] = skin
    # eyes
    px[ox + 1, oy + 3] = EYE_WHITE
    px[ox + 2, oy + 3] = EYE_DARK
    px[ox + 5, oy + 3] = EYE_DARK
    px[ox + 6, oy + 3] = EYE_WHITE
    px[ox + 2, oy + 4] = EYE_DARK
    px[ox + 5, oy + 4] = EYE_DARK
    if beard:
        for x in range(1, 7):
            px[ox + x, oy + 6] = beard
            px[ox + x, oy + 7] = WHITE_SHADOW if x in (1, 6) else beard
        px[ox + 3, oy + 5] = beard
        px[ox + 4, oy + 5] = beard
    else:
        px[ox + 3, oy + 5] = MOUTH
        px[ox + 4, oy + 5] = MOUTH


def papa_skin() -> Image.Image:
    img = Image.new("RGBA", (64, 64), TRANSPARENT)
    d = ImageDraw.Draw(img)
    # Head
    cuboid(
        d,
        top=(8, 0),
        bottom=(16, 0),
        right=(0, 8),
        front=(8, 8),
        left=(16, 8),
        back=(24, 8),
        size=(8, 8, 8),
        top_c=BLONDE,
        bottom_c=SKIN,
        right_c=BLONDE_DARK,
        front_c=SKIN,
        left_c=BLONDE_DARK,
        back_c=BLONDE,
    )
    px = img.load()
    draw_head_face(px, 8, 8, BLONDE, BLONDE_DARK, SKIN, beard=WHITE)
    # Hair on sides/back already blonde; add bangs already in face
    # Hat layer: extra hair + beard volume
    cuboid(
        d,
        top=(40, 0),
        bottom=(48, 0),
        right=(32, 8),
        front=(40, 8),
        left=(48, 8),
        back=(56, 8),
        size=(8, 8, 8),
        top_c=BLONDE,
        bottom_c=TRANSPARENT,
        right_c=BLONDE_DARK,
        front_c=TRANSPARENT,
        left_c=BLONDE_DARK,
        back_c=BLONDE,
    )
    px = img.load()
    for x in range(2, 6):
        px[40 + x, 8] = BLONDE
    for x in range(1, 7):
        px[40 + x, 14] = WHITE
        px[40 + x, 15] = WHITE_SHADOW
    # Body navy
    cuboid(
        d,
        top=(20, 16),
        bottom=(28, 16),
        right=(16, 20),
        front=(20, 20),
        left=(28, 20),
        back=(32, 20),
        size=(8, 12, 4),
        top_c=NAVY,
        bottom_c=NAVY_SHADOW,
        right_c=NAVY_SHADOW,
        front_c=NAVY,
        left_c=NAVY_SHADOW,
        back_c=NAVY_SHADOW,
    )
    # Legs brown, shoes last 3 rows
    for origin, front, right, left, back, top, bottom in (
        ((4, 16), (4, 20), (0, 20), (8, 20), (12, 20), (4, 16), (8, 16)),  # right
        ((20, 48), (20, 52), (16, 52), (24, 52), (28, 52), (20, 48), (24, 48)),  # left
    ):
        cuboid(
            d,
            top=top,
            bottom=bottom,
            right=right,
            front=front,
            left=left,
            back=back,
            size=(4, 12, 4),
            top_c=BROWN,
            bottom_c=SHOE,
            right_c=BROWN_SHADOW,
            front_c=BROWN,
            left_c=BROWN_SHADOW,
            back_c=BROWN_SHADOW,
        )
        px = img.load()
        for x in range(4):
            for y in range(9, 12):
                px[front[0] + x, front[1] + y] = SHOE
                px[right[0] + x, right[1] + y] = SHOE
                px[left[0] + x, left[1] + y] = SHOE
                px[back[0] + x, back[1] + y] = SHOE
    # Arms: short sleeve navy top 4, rest skin (Steve 4px)
    for top, bottom, right, front, left, back in (
        ((44, 16), (48, 16), (40, 20), (44, 20), (48, 20), (52, 20)),
        ((36, 48), (40, 48), (32, 52), (36, 52), (40, 52), (44, 52)),
    ):
        cuboid(
            d,
            top=top,
            bottom=bottom,
            right=right,
            front=front,
            left=left,
            back=back,
            size=(4, 12, 4),
            top_c=NAVY,
            bottom_c=SKIN_SHADOW,
            right_c=SKIN_SHADOW,
            front_c=SKIN,
            left_c=SKIN_SHADOW,
            back_c=SKIN,
        )
        px = img.load()
        for x in range(4):
            for y in range(4):
                px[front[0] + x, front[1] + y] = NAVY
                px[right[0] + x, right[1] + y] = NAVY_SHADOW
                px[left[0] + x, left[1] + y] = NAVY_SHADOW
                px[back[0] + x, back[1] + y] = NAVY
    return img


def mama_skin() -> Image.Image:
    img = Image.new("RGBA", (64, 64), TRANSPARENT)
    d = ImageDraw.Draw(img)
    cuboid(
        d,
        top=(8, 0),
        bottom=(16, 0),
        right=(0, 8),
        front=(8, 8),
        left=(16, 8),
        back=(24, 8),
        size=(8, 8, 8),
        top_c=DARK_BLONDE,
        bottom_c=SKIN,
        right_c=DARK_BLONDE_LIGHT,
        front_c=SKIN,
        left_c=DARK_BLONDE_LIGHT,
        back_c=DARK_BLONDE,
    )
    px = img.load()
    draw_head_face(px, 8, 8, DARK_BLONDE, DARK_BLONDE_LIGHT, SKIN, beard=None)
    # Hat layer: longer hair
    cuboid(
        d,
        top=(40, 0),
        bottom=(48, 0),
        right=(32, 8),
        front=(40, 8),
        left=(48, 8),
        back=(56, 8),
        size=(8, 8, 8),
        top_c=DARK_BLONDE_LIGHT,
        bottom_c=TRANSPARENT,
        right_c=DARK_BLONDE,
        front_c=TRANSPARENT,
        left_c=DARK_BLONDE,
        back_c=DARK_BLONDE,
    )
    px = img.load()
    for x in range(8):
        px[40 + x, 8] = DARK_BLONDE_LIGHT
        px[40 + x, 9] = DARK_BLONDE
    for y in range(8):
        for x in range(8):
            px[56 + x, 8 + y] = DARK_BLONDE if y < 6 else DARK_BLONDE_LIGHT
    # Body burgundy
    cuboid(
        d,
        top=(20, 16),
        bottom=(28, 16),
        right=(16, 20),
        front=(20, 20),
        left=(28, 20),
        back=(32, 20),
        size=(8, 12, 4),
        top_c=BURGUNDY,
        bottom_c=BURGUNDY_SHADOW,
        right_c=BURGUNDY_SHADOW,
        front_c=BURGUNDY,
        left_c=BURGUNDY_SHADOW,
        back_c=BURGUNDY_SHADOW,
    )
    # Slim arms (Alex): 3px front/back
    # Right arm
    fill(d, (44, 16, 3, 4), BURGUNDY)  # top
    fill(d, (47, 16, 3, 4), SKIN_SHADOW)  # bottom
    fill(d, (40, 20, 4, 12), BURGUNDY_SHADOW)  # right
    fill(d, (44, 20, 3, 12), BURGUNDY)  # front
    fill(d, (47, 20, 4, 12), BURGUNDY_SHADOW)  # left
    fill(d, (51, 20, 3, 12), BURGUNDY)  # back
    # Left arm
    fill(d, (36, 48, 3, 4), BURGUNDY)
    fill(d, (39, 48, 3, 4), SKIN_SHADOW)
    fill(d, (32, 52, 4, 12), BURGUNDY_SHADOW)
    fill(d, (36, 52, 3, 12), BURGUNDY)
    fill(d, (39, 52, 4, 12), BURGUNDY_SHADOW)
    fill(d, (43, 52, 3, 12), BURGUNDY)
    # Hands: last 2 rows skin
    px = img.load()
    for front, w in (((44, 20), 3), ((36, 52), 3)):
        for x in range(w):
            for y in range(10, 12):
                px[front[0] + x, front[1] + y] = SKIN
    # Legs jeans + shoes
    for front, right, left, back, top, bottom in (
        ((4, 20), (0, 20), (8, 20), (12, 20), (4, 16), (8, 16)),
        ((20, 52), (16, 52), (24, 52), (28, 52), (20, 48), (24, 48)),
    ):
        cuboid(
            d,
            top=top,
            bottom=bottom,
            right=right,
            front=front,
            left=left,
            back=back,
            size=(4, 12, 4),
            top_c=JEAN,
            bottom_c=SHOE,
            right_c=JEAN_SHADOW,
            front_c=JEAN,
            left_c=JEAN_SHADOW,
            back_c=JEAN_SHADOW,
        )
        px = img.load()
        for x in range(4):
            for y in range(9, 12):
                px[front[0] + x, front[1] + y] = SHOE
                px[right[0] + x, right[1] + y] = SHOE
                px[left[0] + x, left[1] + y] = SHOE
                px[back[0] + x, back[1] + y] = SHOE
    return img


def main() -> None:
    out = Path(__file__).resolve().parents[1] / "src/main/resources/skins"
    out.mkdir(parents=True, exist_ok=True)
    papa_skin().save(out / "papa.png")
    mama_skin().save(out / "mama.png")
    print(f"wrote {out / 'papa.png'} and {out / 'mama.png'}")


if __name__ == "__main__":
    main()
