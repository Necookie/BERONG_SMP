## 🪵 Lecture Area / Classroom Zone

### 1. Trash Bin with Paper Waste (`firesim:plastic_trash_bin`)

- **Target Room:** Main Restroom / Corridor
    
- **Normal State (`has_vape=false`):** A clean, standard plastic waste container block. Safe to leave alone.
    
- **Hazardous State (`has_vape=true`):** Overfilled with paper towels and emitting small grey smoke particles from a hidden, smoldering vape battery.
    
- **Failure Consequence:** Spawns a **Class A (Red)** structural fire.
    

### 2. Desktop Power Strip (`firesim:daisy_chain_extension`)

- **Target Room:** Lecture Hall / Group Study Rooms
    
- **Normal State (`overloaded=false`):** A regular extension cord strip lying flat on a student desk with standard devices plugged in safely.
    
- **Hazardous State (`overloaded=true`):** Three extension cords plugged into one another in a chain, actively throwing electric spark particles from the wall junction.
    
- **Failure Consequence:** Spawns a **Class E (Green)** electrical wall fire.
    

### 3. Woodshop Floor Substrate (`firesim:woodshop_sawdust_layer`)

- **Target Room:** Industrial Arts / Vocational Shop Class
    
- **Normal State (`accumulation_level=0`):** Clean, standard concrete or tile shop floor blocks.
    
- **Hazardous State (`accumulation_level=5`):** A thick, beige carpet-like block layer representing massive sawdust accumulation around the machinery.
    
- **Failure Consequence:** Flash-ignites a 3x3 area into a **Class A (Red)** fire.
    

### 4. Stage Lighting Rig (`firesim:stage_spotlight`)

- **Target Room:** Theater Stage / Main Auditorium
    
- **Normal State (`thermal_status=nominal`):** The spotlight is turned off or operating at a safe temperature away from flammable sets.
    
- **Hazardous State (`thermal_status=overheated`):** The spotlight is on, projecting a blinding cone of light particles directly into wool curtains while emitting wavy heat distortion particles.
    
- **Failure Consequence:** Ignites the curtains into a climbing **Class A (Red)** structural fire.
    

### 5. Administrative File Stack (`firesim:archive_box_stack`)

- **Target Room:** Admin Records Room / Faculty Archive
    
- **Normal State (`proximity_hazard=false`):** Record boxes stored neatly on metal shelves away from building appliances.
    
- **Hazardous State (`proximity_hazard=true`):** Cardboard boxes stacked directly flush against a hot, active radiator wall pipe, emitting subtle brown smoke.
    
- **Failure Consequence:** Triggers a deep, smoldering **Class A (Red)** archive fire.
    

## ⚡ Laboratory Zone

### 6. Classroom Computer (`firesim:dust_choked_pc`)

- **Target Room:** Computer Laboratory / ICT Testing Room
    
- **Normal State (`ventilation=clear`):** A standard desktop computer tower running quietly with clear space around its vents.
    
- **Hazardous State (`ventilation=blocked`):** A student backpack asset model is physically wedged against the rear fan, trapping heavy dust particles inside the machine block.
    
- **Failure Consequence:** Power supply unit pops, creating a **Class E (Green)** hardware fire.
    

### 7. Mobile Device Cart (`firesim:charging_cart`)

- **Target Room:** STEM Research Lab
    
- **Normal State (`charge_cycle=disconnected`):** A mobile device cabinet displaying solid green indicator LEDs. The unit is silent and completely safe.
    
- **Hazardous State (`charge_cycle=overloaded`):** The cart hums aggressively with a 60Hz loop sound while a red warning indicator light flashes, showing an unsafe power draw.
    
- **Failure Consequence:** Explodes due to battery thermal runaway, forcing an immediate transition to Phase 2.
    

### 8. Recreation Console Wiring (`firesim:frayed_console_wire`)

- **Target Room:** Student Recreation Lounge / STEM Breakroom
    
- **Normal State (`wire_integrity=insulated`):** A clean, black power cable lying flat on the floor connecting the lounge TV.
    
- **Hazardous State (`wire_integrity=frayed`):** The cord texture shows exposed bare copper wires that periodically emit blue electric arc particles.
    
- **Failure Consequence:** Arcs and ignites the carpet block directly underneath into an electrical fire.
    

### 9. Hallway Vending Machine (`firesim:malfunctioning_vending`)

- **Target Room:** Main Laboratory Access Corridor
    
- **Normal State (`circuit_state=isolated`):** A functional vending machine dispensing items normally, or safely powered down with a dark screen.
    
- **Hazardous State (`circuit_state=shorted`):** The internal display matrix glitches out with scrambled symbols while grey smoke leaks out from the coin slot.
    
- **Failure Consequence:** Internal plastics catch fire, filling the corridor with blinding **Class E (Green)** smoke.
    

### 10. Overhead Media Projector (`firesim:ceiling_projector`)

- **Target Room:** AV-Equipped Chemistry Lecture Lab
    
- **Normal State (`fan_operational=standby`):** The ceiling-mounted projector is off or running with its cooling fan spinning cleanly.
    
- **Hazardous State (`fan_operational=false`):** The projector lens projects a high-brightness flickering light beam while the cooling fan model is stuck, causing status LEDs to blink warning orange.
    
- **Failure Consequence:** Bulb shatters, dropping melting plastic fire clusters onto lab benches.
    

## 🔋 Laboratory Zone (Special Tech Infrastructure)

### 11. Student Mobile Device (`firesim:swollen_phone_battery`)

- **Target Room:** Physics Lab Workbenches
    
- **Normal State (`insulated_heat=dissipated`):** A student phone item sitting uncovered on top of a hard laboratory desk surface.
    
- **Hazardous State (`insulated_heat=critical`):** The phone model is completely covered by a heavy leather jacket block, trapping thermal energy and leaking green battery gas particles.
    
- **Failure Consequence:** Battery ruptures into an intense, torch-like chemical fire.
    

### 12. Robotics Power Pack (`firesim:damaged_lipo_pack`)

- **Target Room:** Robotics Construction Lab
    
- **Normal State (`cell_pressure=contained`):** A flat, silver lithium polymer battery resting safely inside a storage cabinet or fire-retardant safety bag.
    
- **Hazardous State (`cell_pressure=unstable`):** A standalone battery pack sitting loose on a workbench that features a severe physical dent and a visibly bloated, swollen 3D model.
    
- **Failure Consequence:** Expels a violent white-hot chemical burst across a 2-block radius.
    

### 13. Student Hallway Locker (`firesim:vape_in_iron_locker`)

- **Target Room:** Student Hallway Lockers (Lab Exterior)
    
- **Normal State (`internal_hazard=false`):** A standard vertical iron storage locker containing textbooks or regular school supplies.
    
- **Hazardous State (`internal_hazard=true`):** Green spark particles snap through the locker vents accompanied by a rhythmic, metallic rattling audio clip from an unstable device inside.
    
- **Failure Consequence:** Explodes internally, superheating the locker metal and igniting neighboring lockers.
    

### 14. Emergency PA Enclosure (`firesim:pa_system_backup`)

- **Target Room:** Main Emergency Operations Center
    
- **Normal State (`load_distribution=bypassed`):** The backup power box runs on normal voltage thresholds with solid green indicator lights.
    
- **Hazardous State (`load_distribution=faulty`):** The wall-mounted panel box screams with a warning siren while its status lights flash crimson under a failing battery cell load.
    
- **Failure Consequence:** Severe **Class E (Green)** electrical panel fire accompanied by a total PA system blackout.
    

### 15. Classroom Smartboard Panel (`firesim:smartboard_inverter`)

- **Target Room:** Digital Lecture Lab
    
- **Normal State (`liquid_ingress=false`):** The interactive digital whiteboard display functions smoothly on a dry, secure wall.
    
- **Hazardous State (`liquid_ingress=true`):** Water droplet particles fall from a ceiling pipe leak directly onto the top edge of the active smartboard screen housing.
    
- **Failure Consequence:** Circuitry shorts and arcs, igniting the wall substrate behind the display screen.
    

## 🍳 Cooking Zones / Cafeteria

### 16. Kitchen Gas Cooktop (`firesim:unattended_grease_pan`)

- **Target Room:** Home Economics Cooking Lab / Kitchen
    
- **Normal State (`oil_flashpoint=cooling`):** A clean stove block with all dials set to zero (burner off) and cooking pans removed or cold.
    
- **Hazardous State (`oil_flashpoint=critical`):** A pan full of cooking oil sits over an active, flaming burner ring while the liquid oil inside bubbles violently and spits hot yellow particles.
    
- **Failure Consequence:** Erupts into a **Class F/K (Yellow)** grease fire. _Water triggers a 3x3 fiery explosion!_
    

### 17. Kitchen Range Vent (`firesim:grease_clogged_hood`)

- **Target Room:** Main Cafeteria Kitchen / Commercial Line
    
- **Normal State (`grease_saturation=0`):** Clean, stainless steel ventilation grates mounted above the cooking appliances.
    
- **Hazardous State (`grease_saturation=100`):** The underside of the exhaust hood block turns a dark, glistening brown color to represent a heavy coating of flammable grease buildup.
    
- **Failure Consequence:** Sparks ignite the ventilation ducts, causing an invisible fire inside the ceiling voids.
    

### 18. Cafeteria Disposal Bin (`firesim:contaminated_kitchen_bin`)

- **Target Room:** Cafeteria Dishwashing Area
    
- **Normal State (`volatile_contents=false`):** A normal green waste bin containing standard organic food scraps and trash.
    
- **Hazardous State (`volatile_contents=true`):** Thick yellow grease residues coat the outer walls of the bin while steam particles rise from the lid due to warm cooking oils being dumped incorrectly.
    
- **Failure Consequence:** Catches fire instantly if any stray heat touches it, creating unquenchable floor flames.
    

### 19. Commercial Sandwich Press (`firesim:jammed_panini_press`)

- **Target Room:** Student Self-Service Cafe Counter
    
- **Normal State (`clamp_lock=false`):** The sandwich press grill sits wide open on the countertop, safely cutting power to the elements.
    
- **Hazardous State (`clamp_lock=true`):** The press mechanism is jammed clamped shut while active, billowing heavy black smoke columns out from its heated sides.
    
- **Failure Consequence:** Carbonized oils ignite, instantly engulfing the entire countertop line in grease flames.
    

### 20. Cafeteria Deep Fryer Vat (`firesim:commercial_deep_fryer`)

- **Target Room:** Main Cafeteria Deep Fry Line
    
- **Normal State (`thermostat_status=isolated`):** The commercial fryer maintains a safe, stable oil temperature with its indicator set to normal limits.
    
- **Hazardous State (`thermostat_status=failed`):** The front display panel flashes red warning text indicating temperatures soaring past safe limits (`450°C`) along with an intermittent high-pitch alarm tone.
    
- **Failure Consequence:** Oil reaches auto-ignition temperature, erupting into a massive kitchen grease fire.

## 🏫 School Building Zone (Items 21–30, added 2026-07-08)

### 21. Faculty Pantry Microwave (`berongsmp:overloaded_microwave`)

- **Target Room:** Faculty Pantry / Break Room

- **Normal State (`hazardous=false`):** A clean countertop microwave with a dark door and a green ready LED.

- **Hazardous State (`hazardous=true`):** The interior glows orange-hot through the door glass on a runaway heating cycle, arcing sparks and leaking smoke.

- **Failure Consequence (400 ticks):** The magnetron arcs over and sets the pantry counter alight — a **Class C (electrical)** fire.

### 22. Unattended Bunsen Burner (`berongsmp:bunsen_burner_station`)

- **Target Room:** Science Laboratory

- **Normal State:** A lab burner base sitting cold on the bench, gas tap closed.

- **Hazardous State:** An open blue flame burning with nobody at the bench — flame and blue soul-fire particles, light level 9.

- **Failure Consequence (300 ticks):** The open flame ignites the lab bench — bare-hand prevention closes the gas tap.

### 23. Leaking Reagent Shelf (`berongsmp:reagent_storage_shelf`)

- **Target Room:** Science Laboratory / Chemical Storage

- **Normal State:** A wall shelf of neatly sealed reagent bottles.

- **Hazardous State:** A tipped bottle drips reactive liquid down the shelf — fizzing fumes and drip particles.

- **Failure Consequence (500 ticks):** Mixing chemicals flash-ignite a **2-block radius** — the widest failure in the school set.

### 24. Overloaded Breaker Panel (`berongsmp:overloaded_breaker_panel`)

- **Target Room:** Utility Corridor / Electrical Room

- **Normal State:** A closed gray steel distribution panel.

- **Hazardous State:** The panel hangs open with one breaker row scorched black and glowing, throwing dense electric arcs (light 6).

- **Failure Consequence (350 ticks):** The panel flashes over into a **Class C (electrical)** fire — prevention trips the main switch.

### 25. Overheating Wall Aircon (`berongsmp:overheating_wall_aircon`)

- **Target Room:** Any Classroom / Faculty Room

- **Normal State:** A white window-type air conditioner humming normally.

- **Hazardous State:** Rust-streaked and dripping condensation into its own wiring — water drips plus intermittent sparks.

- **Failure Consequence (450 ticks):** The soaked wiring shorts and ignites — the classic water-plus-electricity lesson.

### 26. Jammed Laser Printer (`berongsmp:jammed_laser_printer`)

- **Target Room:** Faculty Room / Registrar's Office

- **Normal State:** An idle gray office laser printer.

- **Hazardous State:** Crumpled paper jammed against the hot fuser unit, billowing smoke from the output slot.

- **Failure Consequence (400 ticks):** The jammed paper reaches the fuser's ignition point — prevention clears the jam.

### 27. Unattended Shrine Candle (`berongsmp:unattended_shrine_candle`)

- **Target Room:** Lobby Shrine / Prayer Corner

- **Normal State:** A cream wax candle standing unlit on the red altar cloth.

- **Hazardous State:** Burning unattended beside the cloth — open flame (light 10) with wisping smoke.

- **Failure Consequence (250 ticks — the fastest in the set):** The candle tips onto the altar cloth, which catches instantly. Prevention snuffs the flame.

### 28. Leaking LPG Gas Valve (`berongsmp:leaking_gas_valve`) — **Kitchen Class F/K**

- **Target Room:** Cafeteria Kitchen Gas Line

- **Normal State:** A steel gas pipe run with a red valve handwheel, sealed.

- **Hazardous State:** Greenish gas haze hissing from the joint — invisible danger telegraphed only by the cloud and stain.

- **Failure Consequence (300 ticks):** The gas cloud finds a spark and flash-fires a **2-block radius**. **Only the yellow wet chemical extinguisher may defuse it** — ABC/CO2 give the wrong-tool warning. Prevention wrenches the valve shut.

### 29. Alcohol Dispenser Station (`berongsmp:alcohol_dispenser_station`)

- **Target Room:** Hallway / Entrance (post-pandemic sanitizer stand)

- **Normal State:** A pedestal stand holding a pump bottle of blue-tinted sanitizer.

- **Hazardous State:** The bottle leaks alcohol down the stand and pools beside a power outlet — faint blue vapor wisps.

- **Failure Consequence (500 ticks):** The pooled alcohol ignites in a ghostly blue flash fire.

### 30. Clogged Exhaust Fan (`berongsmp:clogged_exhaust_fan`)

- **Target Room:** Kitchen / Workshop Window

- **Normal State:** A wall-mounted exhaust fan spinning behind a clean grille.

- **Hazardous State:** The blades cake over with gray-brown dust, the stalled motor smoulders — smoke and ash particles.

- **Failure Consequence (450 ticks):** The overheated motor ignites its own dust cake. Prevention clears the dust off the blades.

### 31. Overloaded Wall Outlet (`berongsmp:overloaded_wall_outlet`)

- **Target Room:** Classroom / Office wall, near student or teacher desks

- **Normal State:** A plain white wall outlet faceplate, safely loaded.

- **Hazardous State:** Too many devices drawing current at once — electric sparks and light smoke leak from the faceplate.

- **Failure Consequence (350 ticks):** The outlet arcs and scorches the wall alight. Prevention unplugs the extra devices.

### 32. Jammed Circuit Breaker Box (`berongsmp:jammed_circuit_breaker`) — distinct from #24 Overloaded Breaker Panel

- **Target Room:** Utility Corridor / Electrical Room

- **Normal State:** A grey breaker box, trip switch free to move.

- **Hazardous State:** The trip lever has been physically jammed (a coin or matchstick wedged in it) so the breaker can't cut power on overload — dense sparking and the occasional ember drip. Unlike the *Overloaded Breaker Panel* (simply drawing more load than rated), here the safety device itself has been sabotaged.

- **Failure Consequence (400 ticks):** With nothing left to trip it, the box overheats and flashes over. Prevention clears the jam so the breaker can trip again.

### 33. Unsealed Solvent Shelf (`berongsmp:unsealed_solvent_shelf`) — Class B flammable liquid

- **Target Room:** Art Room / Shop-Class Storage

- **Normal State:** A wood shelf, solvent cans sealed and stored safely.

- **Hazardous State:** Paint-thinner and lacquer cans left open on the shelf, giving off flammable vapor.

- **Failure Consequence (500 ticks):** The vapor finds a spark and flashes over a **2-block radius** — the widest blast radius alongside the reagent shelf. Prevention seals the cans and moves them from any heat source.

### 34. Unattended Welding Station (`berongsmp:unattended_welding_station`)

- **Target Room:** Vocational Shop-Class Workshop

- **Normal State:** A steel welding bench, torch off.

- **Hazardous State:** The arc welder runs unattended, throwing a shower of hot spatter sparks.

- **Failure Consequence (300 ticks):** Spatter ignites nearby combustibles within **2 blocks** — the widest adjacency radius of any hazard prop. Prevention shuts off the torch and clears the spatter zone.

### 35. Leaking Butane Canister Stove (`berongsmp:leaking_butane_canister_stove`) — **Kitchen Class F/K**

- **Target Room:** Kitchen / Canteen Prep Area

- **Normal State:** A portable single-burner butane ("gasul") camp stove, canister sealed.

- **Hazardous State:** Gas leaking from the canister seam — haze and an intermittent flame lick, distinct from `leaking_gas_valve`'s fixed wall LPG line.

- **Failure Consequence (300 ticks):** The leak catches and flashes a **2-block radius** fire. **Only the yellow wet chemical extinguisher may defuse it** — ABC/CO2 give the wrong-tool warning. Prevention seals the canister and shuts the valve.
