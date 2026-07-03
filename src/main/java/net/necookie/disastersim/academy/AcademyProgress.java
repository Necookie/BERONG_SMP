package net.necookie.disastersim.academy;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * One player's progress through the Academy — 4 independently-advancing phases (one per room/NPC)
 * plus the scoring inputs Capt. Morfe's evaluation reads. Mutable; always accessed through
 * {@link AcademySavedData#mutate} so every change is paired with a {@code setDirty()} call.
 */
public final class AcademyProgress {

    private CruzPhase cruzPhase;
    private ReyesPhase reyesPhase;
    private SantosPhase santosPhase;
    private MorfePhase morfePhase;
    private int movementMistakes;
    private int fireCorrectUses;
    private int fireWrongUses;
    private boolean quakeCompliant;
    private boolean dropAndRollPerformed;

    public AcademyProgress() {
        this(CruzPhase.NOT_STARTED, ReyesPhase.NOT_STARTED, SantosPhase.NOT_STARTED, MorfePhase.NOT_STARTED,
                0, 0, 0, false, false);
    }

    public AcademyProgress(CruzPhase cruzPhase, ReyesPhase reyesPhase, SantosPhase santosPhase, MorfePhase morfePhase,
                            int movementMistakes, int fireCorrectUses, int fireWrongUses,
                            boolean quakeCompliant, boolean dropAndRollPerformed) {
        this.cruzPhase = cruzPhase;
        this.reyesPhase = reyesPhase;
        this.santosPhase = santosPhase;
        this.morfePhase = morfePhase;
        this.movementMistakes = movementMistakes;
        this.fireCorrectUses = fireCorrectUses;
        this.fireWrongUses = fireWrongUses;
        this.quakeCompliant = quakeCompliant;
        this.dropAndRollPerformed = dropAndRollPerformed;
    }

    public static final Codec<AcademyProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            enumCodec(CruzPhase.class, CruzPhase.NOT_STARTED).fieldOf("cruz_phase").forGetter(AcademyProgress::cruzPhase),
            enumCodec(ReyesPhase.class, ReyesPhase.NOT_STARTED).fieldOf("reyes_phase").forGetter(AcademyProgress::reyesPhase),
            enumCodec(SantosPhase.class, SantosPhase.NOT_STARTED).fieldOf("santos_phase").forGetter(AcademyProgress::santosPhase),
            enumCodec(MorfePhase.class, MorfePhase.NOT_STARTED).fieldOf("morfe_phase").forGetter(AcademyProgress::morfePhase),
            Codec.INT.fieldOf("movement_mistakes").forGetter(AcademyProgress::movementMistakes),
            Codec.INT.fieldOf("fire_correct_uses").forGetter(AcademyProgress::fireCorrectUses),
            Codec.INT.fieldOf("fire_wrong_uses").forGetter(AcademyProgress::fireWrongUses),
            Codec.BOOL.fieldOf("quake_compliant").forGetter(AcademyProgress::quakeCompliant),
            Codec.BOOL.fieldOf("drop_and_roll_performed").forGetter(AcademyProgress::dropAndRollPerformed)
    ).apply(instance, AcademyProgress::new));

    /** Same string-with-fallback idiom as {@code TutorialSavedData}'s STAGE_CODEC, generalized per enum type. */
    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type, E fallback) {
        return Codec.STRING.xmap(
                s -> { try { return Enum.valueOf(type, s); } catch (Exception e) { return fallback; } },
                Enum::name
        );
    }

    public CruzPhase cruzPhase() { return cruzPhase; }
    public ReyesPhase reyesPhase() { return reyesPhase; }
    public SantosPhase santosPhase() { return santosPhase; }
    public MorfePhase morfePhase() { return morfePhase; }
    public int movementMistakes() { return movementMistakes; }
    public int fireCorrectUses() { return fireCorrectUses; }
    public int fireWrongUses() { return fireWrongUses; }
    public boolean quakeCompliant() { return quakeCompliant; }
    public boolean dropAndRollPerformed() { return dropAndRollPerformed; }

    public void setCruzPhase(CruzPhase phase) { this.cruzPhase = phase; }
    public void setReyesPhase(ReyesPhase phase) { this.reyesPhase = phase; }
    public void setSantosPhase(SantosPhase phase) { this.santosPhase = phase; }
    public void setMorfePhase(MorfePhase phase) { this.morfePhase = phase; }
    public void addMovementMistake() { this.movementMistakes++; }
    public void addFireCorrectUse() { this.fireCorrectUses++; }
    public void addFireWrongUse() { this.fireWrongUses++; }
    public void setQuakeCompliant(boolean compliant) { this.quakeCompliant = compliant; }
    public void setDropAndRollPerformed(boolean performed) { this.dropAndRollPerformed = performed; }

    /** Resets every phase and scoring counter to a clean slate — used on a Capt. Morfe fail-retry. */
    public void resetAll() {
        cruzPhase = CruzPhase.NOT_STARTED;
        reyesPhase = ReyesPhase.NOT_STARTED;
        santosPhase = SantosPhase.NOT_STARTED;
        morfePhase = MorfePhase.NOT_STARTED;
        movementMistakes = 0;
        fireCorrectUses = 0;
        fireWrongUses = 0;
        quakeCompliant = false;
        dropAndRollPerformed = false;
    }
}
