#!/usr/bin/env python3
"""Regenerates AcademyVoiceDurations.java from the real duration of every academy voice ogg.

Run from the repo root after re-recording any voiceover/<character>/<NNN>.ogg (and copying the
matching file into src/main/resources/assets/berongsmp/sounds/academy/<character>/<NNN>.ogg):

    python3 scripts/generate_academy_voice_durations.py

Requires ffprobe (part of ffmpeg) on PATH.
"""
import math
import os
import subprocess

LINE_COUNTS = {"officer_cruz": 21, "sgt_reyes": 33, "sgt_santos": 10, "capt_morfe": 8}
BUFFER_TICKS = 6  # ~0.3s trailing pause before auto-advancing, so a line doesn't feel clipped
ASSET_ROOT = "src/main/resources/assets/berongsmp/sounds/academy"
OUTPUT = "src/main/java/net/necookie/disastersim/academy/AcademyVoiceDurations.java"


def ogg_duration_ticks(path: str) -> int:
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=noprint_wrappers=1:nokey=1", path],
        capture_output=True, text=True, check=True,
    )
    return math.ceil(float(out.stdout.strip()) * 20) + BUFFER_TICKS


def main():
    entries = []
    for folder, count in LINE_COUNTS.items():
        for i in range(1, count + 1):
            num = f"{i:03d}"
            ticks = ogg_duration_ticks(os.path.join(ASSET_ROOT, folder, f"{num}.ogg"))
            entries.append((folder, num, ticks))

    with open(OUTPUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("package net.necookie.disastersim.academy;\n\n")
        f.write("import java.util.Map;\n\n")
        f.write("/**\n")
        f.write(" * Real per-line audio duration (in ticks, +6-tick/0.3s trailing buffer), keyed by the same\n")
        f.write(" * soundKey strings used in {@link AcademyDialogue} and {@code sounds.json}. Generated from\n")
        f.write(" * ffprobe against every academy/<character>/<NNN>.ogg (2026-07-17) after real ElevenLabs\n")
        f.write(" * recordings replaced the old word-count caption-pacing guess, which was undershooting\n")
        f.write(" * real speech length on 69 of 72 lines (by up to ~8 seconds) and causing the next line's\n")
        f.write(" * stop-sound to cut the current one off mid-sentence.\n")
        f.write(" *\n")
        f.write(" * <p>Not a runtime ogg-parsing lookup on purpose — these files are fixed, bundled assets\n")
        f.write(" * that only change on a rebuild, so a baked-in table avoids adding an audio-decoding\n")
        f.write(" * dependency just to read Vorbis header duration at startup. Regenerate with:\n")
        f.write(" * {@code python3 scripts/generate_academy_voice_durations.py} any time a voice line is\n")
        f.write(" * re-recorded.\n")
        f.write(" */\n")
        f.write("public final class AcademyVoiceDurations {\n\n")
        f.write("    private static final Map<String, Integer> TICKS = Map.ofEntries(\n")
        f.write(",\n".join(
            f'            Map.entry("academy.{folder}.{num}", {ticks})'
            for folder, num, ticks in entries
        ))
        f.write("\n    );\n\n")
        f.write("    /** Ticks to keep a line on screen/audible for, or -1 if soundKey is null/unknown (caller should fall back to a text-based estimate). */\n")
        f.write("    public static int ticksFor(String soundKey) {\n")
        f.write("        if (soundKey == null) return -1;\n")
        f.write("        Integer ticks = TICKS.get(soundKey);\n")
        f.write("        return ticks != null ? ticks : -1;\n")
        f.write("    }\n\n")
        f.write("    private AcademyVoiceDurations() {}\n")
        f.write("}\n")

    print(f"wrote {len(entries)} entries to {OUTPUT}")


if __name__ == "__main__":
    main()
