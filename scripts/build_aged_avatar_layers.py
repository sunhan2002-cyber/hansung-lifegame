"""정원률 아바타 레이어(avatar_v4)에서 나이 든 얼굴 · 음영 레이어를 만든다 (2026-11-05 피드백 5번).

원본 PNG는 바꾸지 않는다. 새 파일만 images/avatar_age/에 만든다.
- eyes_aged_{middle,old_age}_{eyesId}.png : 눈 밑 처짐 · 눈가 · 팔자 · 이마 주름. 노년은 눈 반짝임을 줄이고 눈썹을 희게.
- hair_aged_{middle,old_age}_{hairId}.png : 중년은 관자놀이 흰머리, 노년은 더 흰 머리 + 이마가 드러나게 앞머리를 걷어 낸다.
- shade_{period}_{hairId}.png : 머리카락 아래 그림자 · 얼굴 오른쪽 음영 · 턱 밑 그림자 (모든 시기, 입체감).
- face_sheet_style_reference.png : 사진 → AI 얼굴 시트 요청에 함께 보내는 화풍 · 칸 배치 참고(기본 외형 머리 6개).

주름 · 음영은 반투명 어두운 점이라 피부색(밝은 · 구릿빛 · 짙은)과 상관없이 같은 파일을 쓴다.
사용: python scripts/build_aged_avatar_layers.py
"""
import zlib
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/assets/images/avatar_v4"
FACE = ROOT / "face"
# avatar_v4는 원본 115장만 두는 폴더(validate_week10_assets.py)라 새 레이어는 따로 둔다.
OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/images/avatar_age"
PERIODS = ["infant", "child", "teen", "young_adult", "middle", "old_age"]
AGED = ["middle", "old_age"]
HAIRS = ["hair_default", "hair_bob_brown", "hair_long_black", "hair_curly_brown"]
EYES = ["eyes_default", "eyes_big", "eyes_narrow"]

LINE = (70, 34, 24)  # 주름 · 그림자 색 (반투명으로 얹는다)


def load(path: Path) -> np.ndarray:
    return np.array(Image.open(path).convert("RGBA")).astype(np.int32)


def save(arr: np.ndarray, path: Path) -> None:
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(path, optimize=True)


def lum(px: np.ndarray) -> np.ndarray:
    return 0.299 * px[..., 0] + 0.587 * px[..., 1] + 0.114 * px[..., 2]


class Face:
    """한 시기의 얼굴 기준점. 피부 · 눈 레이어에서 찾는다."""

    def __init__(self, period: str, eyes_id: str = "eyes_default"):
        skin = load(FACE / f"skin_{period}_skin_default_standard.png")
        eyes = load(FACE / f"eyes_{period}_{eyes_id}.png")
        self.skin_alpha = skin[..., 3] > 0
        ey, ex = np.nonzero(eyes[..., 3])
        self.eye_top, self.eye_bottom = int(ey.min()), int(ey.max())
        self.eye_left, self.eye_right = int(ex.min()), int(ex.max())
        self.cx = (self.eye_left + self.eye_right) // 2
        # 눈썹과 눈을 나눈다: 눈썹 · 눈 사이에 비는 줄이 있다.
        rows = sorted(set(ey.tolist()))
        gap = next((r for a, r in zip(rows, rows[1:]) if r - a > 1), None)
        self.brow_bottom = ([a for a, r in zip(rows, rows[1:]) if r - a > 1] or [self.eye_top])[0]
        self.eye_start = gap if gap is not None else self.eye_top
        sy, _ = np.nonzero(self.skin_alpha)
        self.skin_top = int(sy.min())
        # 입: 눈 아래 가운데 쪽 어두운 가로선, 턱: 가운데 세로줄에서 입 아래 첫 외곽선
        dark = (lum(skin) < 90) & self.skin_alpha
        x0 = self.cx - 16
        band = dark[self.eye_bottom + 4:, x0:self.cx + 17]
        ys, xs = np.nonzero(band)
        counts = np.bincount(ys)
        mouth_row = int(np.argmax(counts))
        self.mouth_y = self.eye_bottom + 4 + mouth_row
        mx = sorted((xs[ys == mouth_row] + x0).tolist())
        # 가운데에 가장 가까운 연속 구간만 입으로 본다
        runs, run = [], [mx[0]]
        for x in mx[1:]:
            if x - run[-1] <= 2:
                run.append(x)
            else:
                runs.append(run)
                run = [x]
        runs.append(run)
        best = max(runs, key=len)
        self.mouth_left, self.mouth_right = best[0], best[-1]
        nose = dark[self.eye_bottom + 2:self.mouth_y - 2, self.cx - 8:self.cx + 9]
        ny, nx = np.nonzero(nose)
        self.nose_y = self.eye_bottom + 2 + int(ny.mean()) if len(ny) else (self.eye_bottom + self.mouth_y) // 2
        self.nose_x = self.cx - 8 + int(nx.mean()) if len(nx) else self.cx
        col = dark[self.mouth_y + 3:, self.cx]
        self.chin = self.mouth_y + 3 + int(np.argmax(col)) if col.any() else self.mouth_y + 12
        # 얼굴 좌우 끝: 눈썹 높이(귀보다 위)에서 잰다
        row = np.nonzero(self.skin_alpha[self.brow_bottom])[0]
        self.face_left, self.face_right = int(row.min()), int(row.max())
        self.width = self.eye_right - self.eye_left


def blend(arr, y, x, rgb, alpha):
    """빈 칸이면 반투명 점을 놓고, 이미 칠해진 칸이면 그 위에 섞는다."""
    h, w = arr.shape[:2]
    if not (0 <= y < h and 0 <= x < w):
        return
    a = alpha / 255.0
    if arr[y, x, 3] == 0:
        arr[y, x] = [*rgb, alpha]
    else:
        arr[y, x, :3] = (arr[y, x, :3] * (1 - a) + np.array(rgb) * a).astype(np.int32)
        arr[y, x, 3] = max(arr[y, x, 3], alpha)


def stroke(arr, points, alpha, thick=2, rgb=LINE):
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            x = round(x0 + (x1 - x0) * i / n)
            y = round(y0 + (y1 - y0) * i / n)
            for dy in range(thick):
                blend(arr, y + dy, x, rgb, alpha)


def aged_eyes(period: str, eyes_id: str) -> np.ndarray:
    f = Face(period, eyes_id)
    eyes = load(FACE / f"eyes_{period}_{eyes_id}.png")
    old = period == "old_age"
    strength = 1.0 if old else 0.6
    eye_rows = slice(f.eye_start, f.eye_bottom + 1)
    if old:
        # 눈 반짝임(흰 점)을 홍채색으로 덮고, 눈썹은 희끗하게
        region = eyes[eye_rows]
        bright = (lum(region) > 200) & (region[..., 3] > 0)
        region[bright, :3] = [58, 30, 20]
        brow = eyes[: f.eye_start]
        mask = brow[..., 3] > 0
        brow[mask, :3] = (brow[mask, :3] * 0.35 + np.array([200, 196, 190]) * 0.65).astype(np.int32)
    a = int(125 * strength)
    half = f.width // 2
    wr = np.zeros_like(eyes)  # 주름은 따로 그린 뒤 피부 위에만 얹는다
    for side in (-1, 1):
        # 각 눈의 가로 범위
        if side < 0:
            xs = np.nonzero(eyes[eye_rows, f.eye_left:f.cx][..., 3].any(axis=0))[0] + f.eye_left
        else:
            xs = np.nonzero(eyes[eye_rows, f.cx:f.eye_right + 1][..., 3].any(axis=0))[0] + f.cx
        x0, x1 = int(xs.min()), int(xs.max())
        d = -1 if side < 0 else 1
        y = f.eye_bottom + 2
        # 눈 밑 처짐: 눈 폭보다 조금 짧은 아래로 둥근 선
        stroke(wr, [(x0 + 2, y), ((x0 + x1) // 2, y + 1), (x1 - 2, y)], int(a * 0.85), thick=1)
        # 눈가 주름: 바깥 눈꼬리에서 짧게 한 줄
        outer = x0 - 2 if side < 0 else x1 + 2
        mid = (f.eye_start + f.eye_bottom) // 2
        stroke(wr, [(outer, mid), (outer + 2 * d, mid + 2)], int(a * 0.8), thick=1)
        # 팔자 주름: 콧방울 옆에서 입꼬리 바깥으로 내려오는 짧은 괄호 모양
        corner = f.mouth_left if side < 0 else f.mouth_right
        sx = f.nose_x + d * 6
        mid_y = (f.nose_y + f.mouth_y) // 2 + 1
        stroke(wr, [(sx, f.nose_y + 1), (corner + d * 3, mid_y), (corner + d * 3, f.mouth_y + 3)], a, thick=1)
        if old:
            # 볼 처짐: 입꼬리 아래 짧은 선
            stroke(wr, [(corner + d * 2, f.mouth_y + 6), (corner + d * 1, f.mouth_y + 8)], int(a * 0.8), thick=1)
    if old:
        # 검버섯 몇 점 (관자놀이 · 볼)
        for x, y in [(f.face_right - 9, f.eye_top + 2), (f.face_left + 8, f.mouth_y - 4), (f.face_right - 12, f.mouth_y - 1)]:
            blend(wr, y, x, (120, 78, 52), 90)
            blend(wr, y, x + 1, (120, 78, 52), 70)
    # 이마 주름 (노년은 두 줄, 중년은 한 줄). 앞머리를 걷은 머리에서만 보인다.
    fy = f.eye_top - 6
    stroke(wr, [(f.cx - half // 2, fy), (f.cx, fy - 1), (f.cx + half // 2, fy)], int(a * 0.8), thick=1)
    if old:
        stroke(wr, [(f.cx - half // 2 + 2, fy - 5), (f.cx, fy - 6), (f.cx + half // 2 - 2, fy - 5)], int(a * 0.7), thick=1)
    # 피부 밖(배경 · 귀 바깥)과 눈 · 눈썹 위에는 주름을 두지 않는다
    wr[~f.skin_alpha] = 0
    wr[eyes[..., 3] > 0] = 0
    for yy, xx in zip(*np.nonzero(wr[..., 3])):
        blend(eyes, yy, xx, tuple(wr[yy, xx, :3]), int(wr[yy, xx, 3]))
    return eyes


def aged_hair(period: str, hair_id: str) -> np.ndarray:
    f = Face(period)
    hair = load(FACE / f"hair_{period}_{hair_id}.png")
    mask = hair[..., 3] > 0
    ys, xs = np.nonzero(mask)
    rng = np.random.default_rng(zlib.crc32(f"{period}:{hair_id}".encode()))
    if period == "middle":
        # 희끗희끗: 관자놀이 쪽으로 갈수록, 그리고 밝은 머리결(하이라이트)부터 은색이 된다
        silver = np.array([176, 173, 168])
        hl = lum(hair[mask])
        median = float(np.median(hl))
        half_face = (f.face_right - f.face_left) / 2
        face_cx = (f.face_right + f.face_left) / 2
        for (y, x), l in zip(zip(ys, xs), hl):
            side = min(1.0, max(0.0, (abs(x - face_cx) - 0.45 * half_face) / (0.55 * half_face)))
            low = min(1.0, max(0.0, (y - f.skin_top) / max(1, f.eye_bottom - f.skin_top)))
            k = 0.65 * side * low + (0.45 if l > median * 1.15 else 0.0) + (0.25 if rng.random() < 0.05 else 0.0)
            k = min(k, 0.8)
            if k > 0:
                hair[y, x, :3] = (hair[y, x, :3] * (1 - k) + silver * k).astype(np.int32)
    else:
        # 더 흰 머리
        white = np.array([232, 230, 226])
        hair[mask, :3] = (hair[mask, :3] * 0.55 + white * 0.45).astype(np.int32)
        # 이마가 드러나게: 둥근 이마선 아래로 얼굴 위에 덮인 앞머리를 걷어 낸다(피부가 있는 칸만, 옆머리는 둔다)
        span = f.eye_top - f.skin_top
        inner_l, inner_r = f.face_left + 5, f.face_right - 5
        half_w = max(1.0, (inner_r - inner_l) / 2)
        mid = (inner_r + inner_l) / 2
        cut = np.zeros_like(mask)
        for x in range(inner_l, inner_r + 1):
            t = (x - mid) / half_w
            line = f.skin_top + int(span * (0.30 + 0.22 * t * t))
            for y in range(line, f.eye_bottom + 1):
                if hair[y, x, 3] and f.skin_alpha[y, x]:
                    hair[y, x, 3] = 0
                    cut[y, x] = True
        # 새 경계 바로 위 한 칸을 어둡게 해 도트 외곽선처럼 보이게 한다
        for y, x in zip(*np.nonzero(cut)):
            if y > 0 and hair[y - 1, x, 3] and not cut[y - 1, x]:
                hair[y - 1, x, :3] = (hair[y - 1, x, :3] * 0.72).astype(np.int32)
        # 정수리 숱을 조금 줄인다: 가장 위 몇 줄은 반투명하게
        crown = ys.min()
        for y in range(crown, crown + 4):
            row = hair[y]
            row[row[..., 3] > 0, 3] = (row[row[..., 3] > 0, 3] * 0.55).astype(np.int32)
    return hair


def shade(period: str, hair: np.ndarray) -> np.ndarray:
    """머리카락 아래 그림자 · 얼굴 오른쪽 음영 · 턱 밑 그림자."""
    f = Face(period)
    h, w = hair.shape[:2]
    out = np.zeros((h, w, 4), dtype=np.int32)
    hair_mask = hair[..., 3] > 40
    skin = f.skin_alpha
    for y in range(f.skin_top, f.chin):
        for x in range(f.face_left, f.face_right + 1):
            if not skin[y, x] or hair_mask[y, x]:
                continue
            # 바로 위 1~3칸에 머리카락이 있으면 그 아래가 그늘진다
            above = hair_mask[max(0, y - 3):y, x]
            if above.any():
                depth = 3 - int(np.argmax(above[::-1]))
                blend(out, y, x, LINE, 70 if depth >= 3 else 45)
    # 얼굴 오른쪽(빛 반대편) 안쪽 4칸
    for y in range(f.eye_top, f.chin):
        row = np.nonzero(skin[y, f.cx:])[0]
        if len(row) == 0:
            continue
        right = f.cx + int(row.max())
        right = min(right, f.face_right + 6)
        for x in range(right - 5, right - 1):
            if skin[y, x] and not hair_mask[y, x]:
                blend(out, y, x, LINE, 52)
    # 턱 밑(목) 그림자
    for y in range(f.chin + 1, f.chin + 6):
        for x in range(f.cx - f.width // 2, f.cx + f.width // 2 + 1):
            if skin[y, x]:
                blend(out, y, x, LINE, 80 if y <= f.chin + 3 else 50)
    return out


def style_reference() -> Image.Image:
    """AI 얼굴 시트 요청에 함께 보내는 화풍 · 칸 배치 참고 그림. 기본 외형 머리 6개를 3열 × 2행(1536×1024)에 놓는다."""
    sheet = Image.new("RGBA", (1536, 1024), (0, 0, 0, 0))
    for index, period in enumerate(PERIODS):
        aged = period in AGED
        layers = [
            FACE / f"skin_{period}_skin_default_standard.png",
            (OUT / f"eyes_aged_{period}_eyes_default.png") if aged else FACE / f"eyes_{period}_eyes_default.png",
            OUT / f"shade_{period}_hair_default.png",
            (OUT / f"hair_aged_{period}_hair_default.png") if aged else FACE / f"hair_{period}_hair_default.png",
        ]
        head = Image.new("RGBA", (192, 312), (0, 0, 0, 0))
        for layer in layers:
            head.alpha_composite(Image.open(layer).convert("RGBA"))
        f = Face(period)
        # 턱 아래(목)는 자른다: 머리만 보낸다
        alpha = np.array(head)[..., 3]
        alpha[f.chin + 1:] = 0
        cut = np.array(head)
        cut[..., 3] = alpha
        head = Image.fromarray(cut, "RGBA")
        box = head.getbbox()
        crop = head.crop(box)
        scale = min(380 / crop.width, 380 / crop.height)
        crop = crop.resize((round(crop.width * scale), round(crop.height * scale)), Image.NEAREST)
        cx, cy = (index % 3) * 512 + 256, (index // 3) * 512 + 256
        sheet.alpha_composite(crop, (cx - crop.width // 2, cy - crop.height // 2))
    return sheet


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    made = []
    for period in AGED:
        for eyes_id in EYES:
            path = OUT / f"eyes_aged_{period}_{eyes_id}.png"
            save(aged_eyes(period, eyes_id), path)
            made.append(path)
        for hair_id in HAIRS:
            path = OUT / f"hair_aged_{period}_{hair_id}.png"
            save(aged_hair(period, hair_id), path)
            made.append(path)
    for period in PERIODS:
        for hair_id in HAIRS:
            used = OUT / f"hair_aged_{period}_{hair_id}.png" if period in AGED else FACE / f"hair_{period}_{hair_id}.png"
            path = OUT / f"shade_{period}_{hair_id}.png"
            save(shade(period, load(used)), path)
            made.append(path)
    path = OUT / "face_sheet_style_reference.png"
    style_reference().save(path, optimize=True)
    made.append(path)
    print(f"{len(made)} files")


if __name__ == "__main__":
    main()
