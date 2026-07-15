# BerongSMP — Player Manual

This is the plain-language guide for students/players connecting to the BerongSMP
disaster simulation server. If you're hosting or administering the server instead,
see [`adminmanual.md`](adminmanual.md).

## What is BerongSMP?

A Minecraft-based fire and earthquake disaster simulation. You play through the
**Academy** tutorial (four NPC-guided rooms teaching fire safety, extinguisher use,
and drop-and-cover-hold drills), then get deployed into a graded live scenario in
**New Sim Building 2.0**, where you prevent, respond to, and evacuate from a real
unfolding fire.

## What you need

- **Minecraft Java Edition** (not Bedrock / Windows 10 Edition) — a legitimate,
  purchased copy, logged into the official Minecraft Launcher at least once.
- The BerongSMP client installer package (a `.zip` — get this from your
  instructor or the project's download link).

## Installing

1. Open the official **Minecraft Launcher** and press **Play** at least once if
   you haven't already — this provisions the Java runtime the installer relies on.
2. Extract the `BerongSMP-Client-Installer.zip` you were given. Keep
   `Install-BerongSMP.exe` and the `payload` folder together in the same location
   — don't move the `.exe` out on its own.
3. Double-click **`Install-BerongSMP.exe`**.
4. Windows will likely show a blue **"Windows protected your PC"** screen. This is
   normal for a small installer like this one — it's not a sign anything is wrong.
   Click **More info**, then **Run anyway**.
5. A console window opens and runs the setup automatically: checks your Minecraft
   installation, finds Java, silently installs the **NeoForge 26.1.2.80** mod
   loader, and drops the BerongSMP mod into your `mods` folder. This takes a
   minute or two.
6. When it prints **"Install complete!"**, press Enter to close the window.

## Connecting to the server

1. Open the **Minecraft Launcher**.
2. In the profile dropdown (above the Play button), select **NeoForge 26.1.2.80**.
3. Click **Play**.
4. From the main menu: **Multiplayer** → **Add Server**.
5. Server Address: **`berongsmp.mcsh.io`**
6. Click **Done**, then double-click the server entry to join.

## Playing through BerongSMP

1. **Register / log in** — on first visit, run `/register <username> <password>
   <student_id> <section> <full_name>` in chat to create your account. On
   returning visits, `/login <username> <password>` restores your progress
   without replaying the tutorial.
2. **The Academy (tutorial)** — walk through four rooms, each led by an NPC
   instructor:
   - **Room 1 (Officer Cruz)** — briefing and a go/stop reaction drill.
   - **Room 2 (Sgt. Reyes)** — extinguisher training: pull the pin, aim at the
     base of the fire, sweep. You'll also learn **drop-and-roll** if you catch
     fire.
   - **Room 3 (Sgt. Santos)** — earthquake response: duck, cover, and hold on
     drills.
   - **Room 4 (Capt. Morfe)** — final evaluation. Score **70+** to get certified
     and automatically deployed into the live simulation a few seconds later.
3. **New Sim Building 2.0 (graded scenario)** — three phases:
   - **Prevention** — find and defuse hazard props before they fail (right-click
     a merely-hazardous prop with a bare hand, or use the correct extinguisher
     type once it's already burning).
   - **Intervention** — extinguish any hazards that escalated into real fires.
     Match the extinguisher to the hazard: ABC (dry chemical) for ordinary
     fires, CO2 for electrical/computer fires, **wet chemical** for kitchen
     grease fires — using the wrong one won't work and you'll get a warning.
   - **Evacuation** — when the fire alarm sounds or time runs out on
     intervention, head for the nearest exit and reach the outdoor assembly
     point.
4. Your run is scored automatically and saved — instructors can review it
   afterward.

## Troubleshooting

| Problem | Fix |
|---|---|
| "Could not find a Minecraft installation" | Open the Minecraft Launcher and press Play once, then run the installer again. |
| "Could not find a Java runtime" | Same fix — the launcher needs to have run at least once to provision Java. |
| Installer window closes instantly / can't read it | Right-click `Install-BerongSMP.exe` → **Run as administrator**, or let your instructor know. |
| Can't connect to the server | Double-check you selected the **NeoForge 26.1.2.80** profile (not vanilla) before clicking Play, and that the address is typed exactly as `berongsmp.mcsh.io`. |
| Kicked with a "mods" mismatch error | Your installed NeoForge/mod version doesn't match the server's. Re-run the installer — it always installs the correct matching version. |
