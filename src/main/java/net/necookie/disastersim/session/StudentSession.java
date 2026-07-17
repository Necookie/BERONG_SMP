package net.necookie.disastersim.session;

import java.time.Instant;
import java.util.UUID;

public class StudentSession {

    private final String studentName;
    private final String stationAccount;
    private final UUID accountUuid;
    private final Instant startTime;

    private Instant tutorialStartTime;
    private Instant tutorialEndTime;
    private String simulationType;
    private int simulationScore;
    private boolean passed;
    private String bfpNotes;
    private Double confidence;
    private String prepLevel;

    /**
     * Row ID returned by Turso on INSERT — used for subsequent UPDATE calls. {@code volatile}
     * because it's now written from a Turso executor thread ({@code SessionManager.checkin}'s
     * async insert) but read from the server thread by every {@code /bfp note/confidence/
     * prep_level/score/pass/fail} command — without a memory-visibility guarantee, a command
     * issued moments after check-in could see the field's default {@code -1} even though the
     * insert had already completed on another thread.
     */
    private volatile long dbRowId = -1;

    public StudentSession(String studentName, String stationAccount, UUID accountUuid) {
        this.studentName = studentName;
        this.stationAccount = stationAccount;
        this.accountUuid = accountUuid;
        this.startTime = Instant.now();
        this.tutorialStartTime = Instant.now();
    }

    // --- Getters ---

    public String getStudentName()    { return studentName; }
    public String getStationAccount() { return stationAccount; }
    public UUID   getAccountUuid()    { return accountUuid; }
    public Instant getStartTime()     { return startTime; }
    public Instant getTutorialStartTime() { return tutorialStartTime; }
    public Instant getTutorialEndTime()   { return tutorialEndTime; }
    public String getSimulationType() { return simulationType; }
    public int    getSimulationScore(){ return simulationScore; }
    public boolean isPassed()         { return passed; }
    public long   getDbRowId()        { return dbRowId; }
    public String getBfpNotes()       { return bfpNotes; }
    public Double getConfidence()     { return confidence; }
    public String getPrepLevel()      { return prepLevel; }

    // --- Setters ---

    public void setDbRowId(long id)              { this.dbRowId = id; }
    public void setTutorialEndTime(Instant t)    { this.tutorialEndTime = t; }
    public void setSimulationType(String type)   { this.simulationType = type; }
    public void setSimulationScore(int score)    { this.simulationScore = score; }
    public void setPassed(boolean passed)        { this.passed = passed; }
    public void setBfpNotes(String notes)        { this.bfpNotes = notes; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public void setPrepLevel(String prepLevel)   { this.prepLevel = prepLevel; }

    /** Seconds elapsed from tutorial start to completion, or -1 if not completed. */
    public long getTutorialDurationSeconds() {
        if (tutorialStartTime == null || tutorialEndTime == null) return -1;
        return tutorialEndTime.getEpochSecond() - tutorialStartTime.getEpochSecond();
    }

    @Override
    public String toString() {
        return String.format("StudentSession{student='%s', account='%s', started=%s, tutorialDone=%s, sim=%s, score=%d, passed=%b}",
                studentName, stationAccount, startTime, tutorialEndTime != null, simulationType, simulationScore, passed);
    }
}
