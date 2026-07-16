package net.necookie.disastersim.academy;

import java.util.Map;

/**
 * Real per-line audio duration (in ticks, +6-tick/0.3s trailing buffer), keyed by the same
 * soundKey strings used in {@link AcademyDialogue} and {@code sounds.json}. Generated from
 * ffprobe against every academy/<character>/<NNN>.ogg (2026-07-17) after real ElevenLabs
 * recordings replaced the old word-count caption-pacing guess, which was undershooting
 * real speech length on 69 of 72 lines (by up to ~8 seconds) and causing the next line's
 * stop-sound to cut the current one off mid-sentence.
 *
 * <p>Not a runtime ogg-parsing lookup on purpose — these files are fixed, bundled assets
 * that only change on a rebuild, so a baked-in table avoids adding an audio-decoding
 * dependency just to read Vorbis header duration at startup. Regenerate with:
 * {@code python3 scripts/generate_academy_voice_durations.py} any time a voice line is
 * re-recorded.
 */
public final class AcademyVoiceDurations {

    private static final Map<String, Integer> TICKS = Map.ofEntries(
            Map.entry("academy.officer_cruz.001", 208),
            Map.entry("academy.officer_cruz.002", 312),
            Map.entry("academy.officer_cruz.003", 232),
            Map.entry("academy.officer_cruz.004", 174),
            Map.entry("academy.officer_cruz.005", 182),
            Map.entry("academy.officer_cruz.006", 187),
            Map.entry("academy.officer_cruz.007", 173),
            Map.entry("academy.officer_cruz.008", 200),
            Map.entry("academy.officer_cruz.009", 174),
            Map.entry("academy.officer_cruz.010", 272),
            Map.entry("academy.officer_cruz.011", 173),
            Map.entry("academy.officer_cruz.012", 128),
            Map.entry("academy.officer_cruz.013", 147),
            Map.entry("academy.officer_cruz.014", 66),
            Map.entry("academy.officer_cruz.015", 70),
            Map.entry("academy.officer_cruz.016", 78),
            Map.entry("academy.officer_cruz.017", 86),
            Map.entry("academy.officer_cruz.018", 75),
            Map.entry("academy.officer_cruz.019", 64),
            Map.entry("academy.officer_cruz.020", 208),
            Map.entry("academy.officer_cruz.021", 248),
            Map.entry("academy.sgt_reyes.001", 331),
            Map.entry("academy.sgt_reyes.002", 320),
            Map.entry("academy.sgt_reyes.003", 202),
            Map.entry("academy.sgt_reyes.004", 144),
            Map.entry("academy.sgt_reyes.005", 192),
            Map.entry("academy.sgt_reyes.006", 139),
            Map.entry("academy.sgt_reyes.007", 142),
            Map.entry("academy.sgt_reyes.008", 155),
            Map.entry("academy.sgt_reyes.009", 170),
            Map.entry("academy.sgt_reyes.010", 154),
            Map.entry("academy.sgt_reyes.011", 178),
            Map.entry("academy.sgt_reyes.012", 152),
            Map.entry("academy.sgt_reyes.013", 114),
            Map.entry("academy.sgt_reyes.014", 141),
            Map.entry("academy.sgt_reyes.015", 206),
            Map.entry("academy.sgt_reyes.016", 152),
            Map.entry("academy.sgt_reyes.017", 131),
            Map.entry("academy.sgt_reyes.018", 238),
            Map.entry("academy.sgt_reyes.019", 246),
            Map.entry("academy.sgt_reyes.020", 221),
            Map.entry("academy.sgt_reyes.021", 128),
            Map.entry("academy.sgt_reyes.022", 254),
            Map.entry("academy.sgt_reyes.023", 178),
            Map.entry("academy.sgt_reyes.024", 210),
            Map.entry("academy.sgt_reyes.025", 269),
            Map.entry("academy.sgt_reyes.026", 347),
            Map.entry("academy.sgt_reyes.027", 208),
            Map.entry("academy.sgt_reyes.028", 32),
            Map.entry("academy.sgt_reyes.029", 224),
            Map.entry("academy.sgt_reyes.030", 126),
            Map.entry("academy.sgt_reyes.031", 338),
            Map.entry("academy.sgt_reyes.032", 163),
            Map.entry("academy.sgt_reyes.033", 226),
            Map.entry("academy.sgt_santos.001", 261),
            Map.entry("academy.sgt_santos.002", 213),
            Map.entry("academy.sgt_santos.003", 198),
            Map.entry("academy.sgt_santos.004", 194),
            Map.entry("academy.sgt_santos.005", 93),
            Map.entry("academy.sgt_santos.006", 154),
            Map.entry("academy.sgt_santos.007", 166),
            Map.entry("academy.sgt_santos.008", 154),
            Map.entry("academy.sgt_santos.009", 208),
            Map.entry("academy.sgt_santos.010", 214),
            Map.entry("academy.capt_morfe.001", 243),
            Map.entry("academy.capt_morfe.002", 154),
            Map.entry("academy.capt_morfe.003", 192),
            Map.entry("academy.capt_morfe.004", 211),
            Map.entry("academy.capt_morfe.005", 264),
            Map.entry("academy.capt_morfe.006", 363),
            Map.entry("academy.capt_morfe.007", 154),
            Map.entry("academy.capt_morfe.008", 260)
    );

    /** Ticks to keep a line on screen/audible for, or -1 if soundKey is null/unknown (caller should fall back to a text-based estimate). */
    public static int ticksFor(String soundKey) {
        if (soundKey == null) return -1;
        Integer ticks = TICKS.get(soundKey);
        return ticks != null ? ticks : -1;
    }

    private AcademyVoiceDurations() {}
}
