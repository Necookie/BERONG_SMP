BerongSMP Academy - Instructor Voice Lines
============================================================

Each instructor folder contains one .txt per spoken line, named
NNN.txt in play order (e.g. 001.txt, 002.txt, ...).

Every .txt is copy-paste ready for ElevenLabs Text to Speech:
line 1 is a bracketed delivery tag (e.g. [warm, welcoming]) and
line 2 is the line to speak. Paste the WHOLE file's contents
(both lines) into the ElevenLabs text box as-is - the bracketed
tag tells the model how to perform the line, so you don't need
to hand-tune stability/style sliders per line.

Record a matching .ogg with the EXACT SAME NUMBER, e.g.:
  001.txt  <- paste this into ElevenLabs
  001.ogg  <- save the generated audio here, same folder

Source: src/main/java/net/necookie/disastersim/academy/AcademyDialogue.java
plus Officer Cruz's GO/STOP minigame callouts from
src/main/java/net/necookie/disastersim/academy/room1/CruzRoomManager.java
(scripted story dialogue + the Go/Stop callouts - not the
other randomized idle-nudge chat lines scattered elsewhere)

officer_cruz/  (21 lines) - Officer Cruz, Room 1 - Movement School (look/walk/jump/crouch/go-stop)
sgt_reyes/  (33 lines) - Sgt. Reyes, Room 2 - Fire Safety Drill (prevention/intervention/evacuation)
sgt_santos/  (10 lines) - Sgt. Santos, Room 3 - Earthquake Drill (drop/cover/hold on)
capt_morfe/  (8 lines) - Capt. Morfe, Room 4 - Evaluation (pass/fail certification)

Each folder's _index.txt lists every line with its dialogue
trigger state and tag, for reference while recording in order.
