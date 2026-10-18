"""Generate editable Maple Hush brand SVG assets from one shared mark."""

from __future__ import annotations

import json
from pathlib import Path
from xml.etree import ElementTree

OUT = Path(__file__).parent / "assets"
OUT.mkdir(exist_ok=True)

COLORS = {
    "primary-500": "#FF2D3D",
    "primary-400": "#FF6A00",
    "primary-300": "#FF9A3C",
    "primary-200": "#FFC347",
    "primary-100": "#FFE9C2",
    "success": "#22C55E",
    "info": "#3B82F6",
    "warning": "#F59E0B",
    "error": "#EF4444",
    "text-primary": "#1F2937",
    "text-secondary": "#6B7280",
    "border": "#E5E7EB",
    "background": "#F8FAFC",
    "neutral-900": "#0F172A",
    "neutral-800": "#1E293B",
    "neutral-700": "#334155",
    "neutral-600": "#475569",
    "neutral-500": "#64748B",
    "neutral-400": "#94A3B8",
    "neutral-300": "#CBD5E1",
    "neutral-200": "#E2E8F0",
    "neutral-100": "#F1F5F9",
    "neutral-50": "#F8FAFC",
}

# Two deliberately simple, separate sweeps. They share no hidden raster layers.
UPPER = (
    "M26 77 C18 65 20 51 28 42 C37 32 51 27 62 22 "
    "C68 19 74 15 77 11 C79 9 80 10 81 15 "
    "C84 32 79 44 70 51 C63 57 54 60 45 64 "
    "C36 68 31 74 28 79 C27 80 26 79 26 77 Z"
)
LOWER = (
    "M34 91 C30 81 36 70 46 64 C54 59 63 57 70 52 "
    "C74 49 77 46 79 43 C81 41 82 43 82 48 "
    "C84 63 78 72 67 78 C58 83 44 85 38 94 "
    "C36 97 35 95 34 91 Z"
)


def save(name: str, svg: str) -> None:
    ElementTree.fromstring(svg)
    (OUT / name).write_text(svg, encoding="utf-8")


def gradient_def() -> str:
    return (
        '<defs><linearGradient id="brand" x1="8" y1="72" x2="91" y2="13" '
        'gradientUnits="userSpaceOnUse">'
        '<stop stop-color="#FF2D3D"/><stop offset="0.48" stop-color="#FF6A00"/>'
        '<stop offset="1" stop-color="#FFC347"/></linearGradient></defs>'
    )


def mark_paths(fill: str = "url(#brand)", outline: bool = False) -> str:
    paint = (
        f'fill="none" stroke="{fill}" stroke-width="1.8" stroke-linejoin="round"'
        if outline
        else f'fill="{fill}"'
    )
    return f'<path d="{UPPER}" {paint}/><path d="{LOWER}" {paint}/>'


def logo(fill: str = "url(#brand)", outline: bool = False) -> str:
    defs = gradient_def() if fill == "url(#brand)" else ""
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" '
        'viewBox="0 0 100 100">'
        + defs + mark_paths(fill, outline) + '</svg>'
    )


def app_icon(background: str, inverse: bool = False) -> str:
    defs = gradient_def() if not inverse else (
        '<defs><linearGradient id="tile" x1="0" y1="200" x2="512" y2="0" '
        'gradientUnits="userSpaceOnUse"><stop stop-color="#FF2D3D"/>'
        '<stop offset="0.52" stop-color="#FF6A00"/>'
        '<stop offset="1" stop-color="#FFC347"/></linearGradient></defs>'
    )
    fill = "#FFFFFF" if inverse else "url(#brand)"
    bg = "url(#tile)" if inverse else background
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" '
        'viewBox="0 0 512 512">'
        + defs + f'<path d="M108 0 H404 Q512 0 512 108 V404 Q512 512 404 512 H108 Q0 512 0 404 V108 Q0 0 108 0 Z" fill="{bg}"/>'
        + f'<g transform="translate(71 57) scale(3.7)">{mark_paths(fill)}</g></svg>'
    )


def favicon(size: int) -> str:
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        'viewBox="0 0 100 100">'
        + gradient_def() + mark_paths() + '</svg>'
    )


def splash(kind: str) -> str:
    backgrounds = {
        "light": (
            '<rect width="360" height="780" fill="#FFF9F5"/>'
            '<path d="M0 635 Q92 560 174 636 T360 603 V780 H0Z" fill="#FFE9C2" opacity=".55"/>'
            '<path d="M0 695 Q97 635 185 696 T360 670 V780 H0Z" fill="#FF9A3C" opacity=".17"/>'
        ),
        "landscape": (
            '<rect width="360" height="780" fill="#FFF3E5"/>'
            '<circle cx="266" cy="384" r="66" fill="#FFC347" opacity=".47"/>'
            '<path d="M0 510 Q87 418 163 499 T360 480 V780 H0Z" fill="#FFC347"/>'
            '<path d="M0 575 Q90 498 180 565 T360 541 V780 H0Z" fill="#FF9A3C"/>'
            '<path d="M0 664 Q94 583 186 650 T360 624 V780 H0Z" fill="#FF6A00"/>'
            '<path d="M0 731 Q92 666 180 727 T360 697 V780 H0Z" fill="#FF2D3D"/>'
        ),
        "inverse": (
            '<defs><linearGradient id="splash" x1="0" y1="780" x2="360" y2="0" '
            'gradientUnits="userSpaceOnUse"><stop stop-color="#FF2D3D"/>'
            '<stop offset=".55" stop-color="#FF6A00"/><stop offset="1" stop-color="#FFC347"/>'
            '</linearGradient></defs><rect width="360" height="780" fill="url(#splash)"/>'
            '<path d="M0 640 Q175 480 360 560 V780 H0Z" fill="#FF2D3D" opacity=".22"/>'
        ),
    }
    white = kind == "inverse"
    defs = "" if white else gradient_def()
    mark = mark_paths("#FFFFFF" if white else "url(#brand)")
    text_color = "#FFFFFF" if white else "#1F2937"
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="360" height="780" '
        'viewBox="0 0 360 780">'
        + defs + backgrounds[kind]
        + f'<g transform="translate(90 216) scale(1.8)">{mark}</g>'
        + f'<text x="180" y="442" fill="{text_color}" text-anchor="middle" '
        'font-family="Inter,Arial,sans-serif" font-size="31" font-weight="700">Maple Hush</text>'
        + f'<text x="180" y="470" fill="{text_color}" text-anchor="middle" '
        'font-family="Inter,Arial,sans-serif" font-size="13" letter-spacing="7">ADMIN</text>'
        '</svg>'
    )


def background(kind: str) -> str:
    if kind == "banner":
        width, height = 1200, 360
        shapes = (
            '<rect width="1200" height="360" fill="#FFF9F5"/>'
            '<path d="M0 125 Q195 49 381 133 T758 136 T1200 45 V360 H0Z" fill="#FFE9C2"/>'
            '<path d="M0 169 Q210 83 414 165 T798 181 T1200 95 V360 H0Z" fill="#FFC347" opacity=".8"/>'
            '<path d="M0 219 Q181 140 404 218 T811 237 T1200 152 V360 H0Z" fill="#FF9A3C" opacity=".86"/>'
            '<path d="M0 275 Q208 198 405 263 T806 279 T1200 221 V360 H0Z" fill="#FF6A00" opacity=".9"/>'
            '<path d="M0 325 Q207 254 401 314 T810 325 T1200 283 V360 H0Z" fill="#FF2D3D"/>'
        )
    else:
        width, height = 480, 640
        base = {
            "light": "#FFF9F5",
            "pattern": "#FFE9C2",
            "gradient": "#FF6A00",
        }[kind]
        shapes = (
            f'<rect width="480" height="640" fill="{base}"/>'
            '<path d="M-85 426 Q109 252 291 415 T568 360 V720 H-85Z" '
            'fill="#FFC347" opacity=".35"/>'
            '<path d="M-90 517 Q110 356 294 510 T570 458 V720 H-90Z" '
            'fill="#FF9A3C" opacity=".30"/>'
            '<path d="M-90 592 Q113 432 300 590 T570 537 V720 H-90Z" '
            'fill="#FF2D3D" opacity=".21"/>'
        )
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}">{shapes}</svg>'
    )


# Each icon has a 24x24 viewBox and is purposely built from editable strokes.
ICONS = {
    "home": '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1z"/>',
    "dashboard": '<rect x="3" y="12" width="4" height="9" rx="1"/><rect x="10" y="7" width="4" height="14" rx="1"/><rect x="17" y="3" width="4" height="18" rx="1"/>',
    "workspace": '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
    "menu": '<path d="M4 6h16M4 12h16M4 18h16"/>',
    "users": '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2z"/>',
    "roles": '<circle cx="9" cy="8" r="3"/><path d="M2 20v-2a7 7 0 0 1 14 0v2zM17 5a3 3 0 0 1 0 6M17 14a6 6 0 0 1 5 6"/>',
    "department": '<rect x="9" y="2" width="6" height="5" rx="1"/><rect x="2" y="17" width="6" height="5" rx="1"/><rect x="16" y="17" width="6" height="5" rx="1"/><path d="M12 7v5M5 17v-5h14v5"/>',
    "settings": '<circle cx="12" cy="12" r="3"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9 7 7M17 17l2.1 2.1M19.1 4.9 17 7M7 17l-2.1 2.1"/>',
    "add": '<circle cx="12" cy="12" r="10"/><path d="M12 7v10M7 12h10"/>',
    "edit": '<path d="m4 17 12-12 3 3L7 20l-4 1zM14 7l3 3"/>',
    "delete": '<path d="M4 6h16M9 6V4h6v2M6 6l1 15h10l1-15M10 10v7M14 10v7"/>',
    "search": '<circle cx="10.5" cy="10.5" r="7.5"/><path d="m16 16 6 6"/>',
    "filter": '<path d="M3 4h18l-7 8v7l-4 2v-9z"/>',
    "refresh": '<path d="M20 7a9 9 0 0 0-15-2L3 7M3 3v4h4M4 17a9 9 0 0 0 15 2l2-2M21 21v-4h-4"/>',
    "download": '<path d="M12 3v12m-5-5 5 5 5-5M4 18v3h16v-3"/>',
    "upload": '<path d="M12 15V3m-5 5 5-5 5 5M4 18v3h16v-3"/>',
    "message": '<path d="M3 4h18v14H8l-5 4z"/><path d="M7 10h10"/>',
    "mail": '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m3 6 9 7 9-7"/>',
    "notification": '<path d="M5 18h14l-2-3V9a5 5 0 0 0-10 0v6zM10 21h4"/>',
    "help": '<circle cx="12" cy="12" r="10"/><path d="M9 9a3 3 0 1 1 5 2c-1 1-2 1.5-2 3M12 18h.01"/>',
    "share": '<circle cx="5" cy="12" r="2"/><circle cx="19" cy="5" r="2"/><circle cx="19" cy="19" r="2"/><path d="m7 11 10-5M7 13l10 5"/>',
    "link": '<path d="M10 13a5 5 0 0 0 7 .5l3-3a5 5 0 0 0-7-7l-2 2M14 11a5 5 0 0 0-7-.5l-3 3a5 5 0 0 0 7 7l2-2"/>',
    "calendar": '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18"/>',
    "clock": '<circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/>',
    "chart": '<path d="M3 21h18M5 17V9h3v8M11 17V4h3v13M17 17v-6h3v6"/>',
    "table": '<rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 10h18M3 16h18M10 3v18"/>',
    "folder": '<path d="M3 6h7l2 2h9v12H3z"/>',
    "document": '<path d="M5 2h9l5 5v15H5zM14 2v5h5M9 12h6M9 16h6"/>',
    "lock": '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3"/>',
    "shield": '<path d="m12 2 8 3v6c0 6-3.5 9-8 11-4.5-2-8-5-8-11V5z"/><path d="m8 12 3 3 5-6"/>',
    "check": '<circle cx="12" cy="12" r="10"/><path d="m7 12 3 3 7-7"/>',
    "warning": '<path d="m12 2 10 19H2zM12 9v5M12 18h.01"/>',
}


def ui_icon(name: str, color: str = "#64748B") -> str:
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" '
        'viewBox="0 0 24 24" fill="none" '
        f'stroke="{color}" stroke-width="2" stroke-linecap="round" '
        f'stroke-linejoin="round">{ICONS[name]}</svg>'
    )


for name, fill, outline in [
    ("logo-gradient.svg", "url(#brand)", False),
    ("logo-red.svg", "#FF2D3D", False),
    ("logo-orange.svg", "#FF6A00", False),
    ("logo-gold.svg", "#FFC347", False),
    ("logo-white.svg", "#FFFFFF", False),
    ("logo-gray.svg", "#64748B", False),
    ("logo-outline.svg", "#334155", True),
]:
    save(name, logo(fill, outline))

for name, background_color, inverse in [
    ("app-icon-light.svg", "#FFF9F5", False),
    ("app-icon-dark.svg", "#0F172A", False),
    ("app-icon-inverse.svg", "", True),
]:
    save(name, app_icon(background_color, inverse))

for size in (16, 32, 64):
    save(f"favicon-{size}.svg", favicon(size))

for kind in ("light", "landscape", "inverse"):
    save(f"splash-{kind}.svg", splash(kind))

for kind in ("banner", "light", "pattern", "gradient"):
    save(f"background-{kind}.svg", background(kind))

for name in ICONS:
    save(f"icon-{name}.svg", ui_icon(name))

for state, color in [
    ("default", "#64748B"),
    ("hover", "#FF6A00"),
    ("selected", "#FF2D3D"),
    ("disabled", "#CBD5E1"),
]:
    save(f"icon-home-{state}.svg", ui_icon("home", color))

(OUT / "tokens.json").write_text(
    json.dumps({"colors": COLORS, "gradient": ["#FF2D3D", "#FFC347"]}, indent=2),
    encoding="utf-8",
)

print(f"Generated {len(list(OUT.glob('*.svg')))} SVG assets in {OUT}")
