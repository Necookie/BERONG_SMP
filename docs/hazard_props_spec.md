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

## 🍳 Culinary Classroom Zone (20 props)

### 36. Chef's Prep Table (`berongsmp:chefs_prep_drawers`)

- **Target Room:** Culinary Classroom / Kitchen Lab

- **Normal State:** Stainless prep table, drawers closed.

- **Hazardous State:** An open drawer with an oily dish-rag draped over the edge, right beside a nearby burner — radiant heat slowly cooks the rag toward ignition. Distinct from `contaminated_kitchen_bin` (a closed bin of rags, not exposed workstation fabric).

- **Failure Consequence (400 ticks):** The rag self-ignites, spreading to nearby combustibles. Prevention moves the rag clear of the burner.

### 37. Culinary Lab Refrigerator (`berongsmp:culinary_fridge`)

- **Target Room:** Culinary Classroom / Kitchen Lab

- **Normal State:** Tall reach-in fridge, running quietly.

- **Hazardous State:** Dust-caked condenser coils and a failing start-relay overheat the compressor — sparks and light smoke at the base. The mod's first refrigerator; deliberately an electrical/compressor angle, not a coolant-gas leak.

- **Failure Consequence (450 ticks):** The relay sparks and ignites — a **Class C** electrical fire. Prevention unplugs the fridge and clears the dust from the coils.

### 38. Student Lab Microwave (`berongsmp:student_lab_microwave`)

- **Target Room:** Culinary Classroom shared student bench

- **Normal State:** A countertop microwave, door closed.

- **Hazardous State:** Metal cutlery or foil left inside arcs violently — blue-white sparks behind the door glass. Distinct from `overloaded_microwave` (faculty-pantry unit, runaway orange-hot heating cycle) — this one is cold-but-arcing.

- **Failure Consequence (350 ticks):** The arcing ignites — a **Class C** electrical fire. Prevention opens the door and pulls the foil out.

### 39. Corroded Gas Line Joint (`berongsmp:corroded_gas_line_joint`) — **Kitchen Class F/K**

- **Target Room:** Culinary Classroom kitchen wall

- **Normal State:** A gas-line elbow joint, sealed.

- **Hazardous State:** Rust and corrosion pinholes weep gas at the joint — haze telegraphs the invisible leak. Distinct from `leaking_gas_valve` (an opened handwheel valve) — here the permanent pipe infrastructure itself is failing from age.

- **Failure Consequence (300 ticks):** The gas cloud flashes over a **2-block radius**. **Only the wet chemical extinguisher may defuse it.** Prevention shuts the upstream line and clamps the joint.

### 40. Gas Range, Stuck Burner (`berongsmp:gas_range_stuck_burner`) — **Kitchen Class F/K**

- **Target Room:** Culinary Classroom kitchen line

- **Normal State:** A full 4-burner gas range, knobs responsive.

- **Hazardous State:** A melted, jammed control knob leaves a burner running wild with no pan on it — flames lick up the side. Distinct from `unattended_grease_pan` (a pan of oil on a burner) — here the appliance's own control has failed.

- **Failure Consequence (300 ticks):** The wild burner ignites the range and counter. **Only the wet chemical extinguisher may defuse it.** Prevention reaches behind and shuts the range's gas cock.

### 41. Commercial Stand Mixer (`berongsmp:commercial_stand_mixer`)

- **Target Room:** Culinary Classroom prep counter

- **Normal State:** A planetary stand mixer, idle.

- **Hazardous State:** The motor jams under stiff bread dough and overheats, smoking from the head housing.

- **Failure Consequence (400 ticks):** The overheated motor ignites — a **Class C** fire. Prevention cuts power and clears the jammed dough.

### 42. Gas Deck Oven (`berongsmp:gas_deck_oven`) — **Kitchen Class F/K**

- **Target Room:** Culinary Classroom baking area

- **Normal State:** A gas baking deck oven, door sealed.

- **Hazardous State:** A stuck thermostat and a door cracked ajar radiate fierce heat onto nearby paper liners/proofing trays. Distinct from `commercial_deep_fryer` (an oil-vat auto-ignition, not radiant heat from an oven door).

- **Failure Consequence (350 ticks):** Radiant heat ignites nearby combustibles. **Only the wet chemical extinguisher may defuse it.** Prevention closes the door and shuts the gas.

### 43. Induction Cooktop (`berongsmp:induction_cooktop_station`)

- **Target Room:** Culinary Classroom demo counter

- **Normal State:** A glass-top induction hob, cooling fan running.

- **Hazardous State:** The cooling fan fails and the power electronics overheat under a boiled-dry pan — no open flame, no oil; purely electrical/electromagnetic.

- **Failure Consequence (400 ticks):** The overheated electronics ignite — a **Class C** fire. Prevention switches it off at the wall and lifts the pan.

### 44. Rice Cooker Bank (`berongsmp:rice_cooker_bank`)

- **Target Room:** Culinary Classroom counter

- **Normal State:** A row of student rice cookers, cycling normally.

- **Hazardous State:** One cooker boils dry — its exposed heating element scorches the plastic base, the "keep warm" cycle never cutting out.

- **Failure Consequence (350 ticks):** The scorched base ignites — a **Class C** fire. Prevention unplugs the bank and adds water.

### 45. Espresso Machine (`berongsmp:espresso_machine`)

- **Target Room:** Culinary Classroom / Canteen café counter

- **Normal State:** A café-style espresso machine, pressure nominal.

- **Hazardous State:** A scale-blocked pressure-relief valve lets the boiler over-pressurize — steam blasts from the group head and the control board shorts.

- **Failure Consequence (450 ticks):** The shorted board ignites — a **Class C** fire. Prevention bleeds the pressure and kills the power.

### 46. Hot Water Urn (`berongsmp:hot_water_urn`)

- **Target Room:** Canteen / Culinary Classroom counter

- **Normal State:** A tall tea/coffee boiler urn, spigot sealed.

- **Hazardous State:** The spigot drips water down onto the live base electrics — the classic water-meets-electricity short.

- **Failure Consequence (400 ticks):** The base electrics spark and ignite — a **Class C** fire. Prevention unplugs it and wipes the base dry.

### 47. Countertop Toaster Oven (`berongsmp:toaster_oven_crumb`)

- **Target Room:** Culinary Classroom student counter

- **Normal State:** A toaster oven, crumb tray clean.

- **Hazardous State:** The crumb tray is packed with old crumbs and grease, igniting directly under the glowing element.

- **Failure Consequence (350 ticks):** The crumb-tray fire spreads — a **Class A** ordinary-combustible fire. Prevention switches it off and empties the crumb tray.

### 48. Dry Goods Pantry Shelf (`berongsmp:dry_goods_pantry_shelf`)

- **Target Room:** Culinary Classroom pantry wall

- **Normal State:** A shelf of sealed flour/sugar sacks.

- **Hazardous State:** Open sacks beside a hot light-fixture ballast give off airborne flour dust — a real deflagration hazard, distinct from `reagent_storage_shelf` (chemicals) and `archive_box_stack` (paper).

- **Failure Consequence (500 ticks):** The dust cloud flash-ignites a **2-block radius** — the widest culinary blast, matching the reagent shelf. Prevention seals the sacks and moves them from the hot fixture.

### 49. Kitchen Exhaust Duct (`berongsmp:grease_duct_run`) — **Kitchen Class F/K**

- **Target Room:** Culinary Classroom ceiling above the cooking line

- **Normal State:** A ceiling exhaust duct, interior clean.

- **Hazardous State:** Flammable grease cakes the duct interior — a hidden fire risk in the ceiling void, distinct from `grease_clogged_hood` (the hood canopy directly over the stove, not the overhead ductwork).

- **Failure Consequence (450 ticks):** Flames travel the duct run. **Only the wet chemical extinguisher may defuse it.** Prevention scrapes the grease from the accessible duct panel.

### 50. Dish Sanitizer (`berongsmp:commercial_dish_sanitizer`)

- **Target Room:** Culinary Classroom under-counter run

- **Normal State:** A high-temp under-counter dishwasher, wash cycle normal.

- **Hazardous State:** The booster heating element boils dry while door-seal steam seeps into the control board.

- **Failure Consequence (450 ticks):** Steam-shorted electronics ignite — a **Class C** fire. Prevention kills power and tops up the wash tank.

### 51. Sink Garbage Disposal (`berongsmp:garbage_disposal_unit`)

- **Target Room:** Culinary Classroom sink station (pairs with `sink`)

- **Normal State:** An under-sink food-waste disposal, grinder free.

- **Hazardous State:** A dropped utensil jams the grinder — the motor stalls and burns out its windings.

- **Failure Consequence (350 ticks):** The burnt-out motor ignites — a **Class C** fire. Prevention cuts power and frees the jam.

### 52. UV Knife Sterilizer (`berongsmp:knife_sterilizer_cabinet`)

- **Target Room:** Culinary Classroom kitchen wall

- **Normal State:** A wall-mounted UV knife/utensil sterilizing cabinet, lamp cycling normally.

- **Hazardous State:** The UV-lamp ballast overheats, scorching the plastic housing — a violet glow leaks at the seams.

- **Failure Consequence (400 ticks):** The scorched housing ignites — a **Class C** fire. Prevention switches off the lamp and lets it cool.

### 53. Chafing Steam Table (`berongsmp:sterno_steam_table`)

- **Target Room:** Culinary Classroom buffet/serving line

- **Normal State:** A buffet steam table, chafing burners unlit.

- **Hazardous State:** Canned gel-fuel (Sterno) burners left lit under a boiled-dry water pan — an open flammable-gel flame, distinct from every electric hazard in the set.

- **Failure Consequence (300 ticks):** The gel-fuel flares up — a **Class B** flammable-liquid fire (any extinguisher). Prevention snuffs the gel-fuel cans and covers them.

### 54. Electric Convection Oven (`berongsmp:convection_oven`)

- **Target Room:** Culinary Classroom baking area

- **Normal State:** An electric convection/combi oven, running normally.

- **Hazardous State:** The heating element and circulation-fan motor short out behind a failed door gasket. Deliberately paired with `gas_deck_oven`'s matching full-height silhouette — same shape, different fuel/failure/extinguisher, teaching the gas-vs-electric distinction.

- **Failure Consequence (400 ticks):** The shorted element/fan ignites — a **Class C** fire. Prevention trips its breaker and opens the door to vent.

### 55. Lechon Rotisserie (`berongsmp:lechon_rotisserie_spit`) — **Kitchen Class F/K**

- **Target Room:** Outdoor/covered culinary cooking area

- **Normal State:** A charcoal rotisserie spit, coals banked low.

- **Hazardous State:** Live coals left unattended — rendered fat dripping onto the coals threatens a flare-up. The only solid-fuel (charcoal) hazard in the mod.

- **Failure Consequence (300 ticks):** Dripping fat flares the coals into a fire. **Only the wet chemical extinguisher may defuse it.** Prevention rakes the coals down and shields the drip pan.

## 🏢 Conference Room Zone (props 56–65, added 2026-07-12)

Real-world basis: wet chemical (aqueous, potassium-acetate) agent is conductive and unsafe on live
electrical fires, and the wrong agent for flammable-vapor fires. All ten of these props are
energized-equipment or vapor hazards **except `smoldering_planter`** (a plain Class A soil fire) —
**only ABC or CO2 may defuse the other nine; wet chemical is refused with a warning.**

### 56. Portable Space Heater (`berongsmp:portable_space_heater`)

- **Target Room:** Conference Room

- **Normal State:** An electric space heater, unplugged, cold coils.

- **Hazardous State:** Tipped against a stack of loose papers, coils glowing orange through the grille.

- **Failure Consequence (300 ticks):** The heater ignites the papers against its grille. **ABC/CO2 only.** Prevention unplugs it and clears the papers.

### 57. Halogen Floor Lamp (`berongsmp:halogen_floor_lamp`)

- **Target Room:** Conference Room / Lounge

- **Normal State:** A torchiere floor lamp, standing upright, bowl cool.

- **Hazardous State:** Tipped against a curtain, the halogen bowl running white-hot.

- **Failure Consequence (350 ticks):** The white-hot bowl scorches the curtain alight. **ABC/CO2 only.** Prevention stands the lamp upright, clear of fabric.

### 58. Jammed Projection Screen (`berongsmp:jammed_projection_screen`)

- **Target Room:** Conference Room ceiling

- **Normal State:** A retractable ceiling projection screen, fully retracted, motor idle.

- **Hazardous State:** The tube motor stalls mid-retract, housing smoking. Distinct from `ceiling_projector` (the projector unit itself, not the screen).

- **Failure Consequence (450 ticks):** The stalled motor overheats and ignites its housing. **ABC/CO2 only.** Prevention cuts power to the motor.

### 59. Overheating Video Wall (`berongsmp:overheating_video_wall`)

- **Target Room:** Conference Room / Boardroom wall

- **Normal State:** A multi-panel LED video wall, dark, standby.

- **Hazardous State:** The driver stack overheats behind the panels; one panel glitches out. Distinct from `smartboard_inverter` (water-ingress failure, not overheating electronics).

- **Failure Consequence (400 ticks):** The overheated driver stack ignites behind the panels. **ABC/CO2 only.** Prevention powers down the wall to let it cool.

### 60. Aerosol Freshener Dispenser (`berongsmp:aerosol_freshener_dispenser`)

- **Target Room:** Conference Room / Corridor wall

- **Normal State:** A wall-mounted automatic air-freshener dispenser, idle.

- **Hazardous State:** Mounted above a heat register, misting flammable propellant.

- **Failure Consequence (500 ticks):** The propellant mist finds the heat register and flashes alight. **ABC/CO2 only.** Prevention relocates the dispenser away from the register.

### 61. Smothered Laptop (`berongsmp:smothered_laptop`)

- **Target Room:** Conference Room / Lounge sofa

- **Normal State:** A laptop sitting on a hard surface, vents clear.

- **Hazardous State:** Left running with its vents buried in sofa cushions, case browning.

- **Failure Consequence (450 ticks):** The smothered battery overheats and ignites the sofa. **ABC/CO2 only.** Prevention pulls the laptop clear of the cushions.

### 62. Smoldering Planter (`berongsmp:smoldering_planter`)

- **Target Room:** Conference Room / Office floor decor

- **Normal State:** A clean decorative planter, healthy plant.

- **Hazardous State:** A discarded cigarette smolders in dry peat potting mix. Distinct from `plastic_trash_bin` (a vape in a bin, not a planter).

- **Failure Consequence (500 ticks):** The smoldering peat finally catches. **Any extinguisher defuses it** — the one Conference Room prop that is a plain Class A fire, not electrical. Prevention digs out the butt and waters the peat.

### 63. Pinched TV Cord (`berongsmp:pinched_tv_cord`)

- **Target Room:** Conference Room wall, behind a mounted display

- **Normal State:** A tidy power cord run behind the display bracket.

- **Hazardous State:** The cord is crushed flat by the mount, insulation split, bare copper arcing. Distinct from `frayed_console_wire` (worn fraying on open floor, not a crush injury behind a mount).

- **Failure Consequence (400 ticks):** The crushed cord arcs and ignites the wall. **ABC/CO2 only.** Prevention reroutes the cord clear of the pinch point.

### 64. Venting UPS Battery (`berongsmp:venting_ups_battery`)

- **Target Room:** Conference Room, under a credenza

- **Normal State:** An under-furniture UPS backup battery, green status.

- **Hazardous State:** A failing lead-acid cell off-gasses hydrogen, case bulging. Distinct from `pa_system_backup` (a failing PA power panel, not a UPS battery bank).

- **Failure Consequence (450 ticks, 2-block radius):** The venting hydrogen flashes over. **ABC/CO2 only.** Prevention pulls the failing battery and vents the cabinet.

### 65. Faulty Dimmer Switch (`berongsmp:faulty_dimmer_switch`)

- **Target Room:** Conference Room wall

- **Normal State:** A clean wall dimmer switch, normal load.

- **Hazardous State:** Over-lamped, the triac buzzes and scorches the faceplate. Distinct from `overloaded_wall_outlet` (a receptacle drawing too much current, not a lighting control).

- **Failure Consequence (350 ticks):** The scorched dimmer ignites the wall. **ABC/CO2 only.** Prevention removes the extra lamp load.

## 🗄 Office Zone (props 66–75, added 2026-07-12)

All ten of these props are energized-electrical hazards. **Only ABC or CO2 may defuse them — wet
chemical is refused with a warning** (see the Conference Room Zone rule above).

### 66. Jammed Paper Shredder (`berongsmp:jammed_paper_shredder`)

- **Target Room:** Office

- **Normal State:** An idle paper shredder, motor free.

- **Hazardous State:** Overfed with a thick sheaf, motor stalled against the cutters, paper-dust smoke rising.

- **Failure Consequence (350 ticks):** The stalled motor overheats and ignites the paper dust. **ABC/CO2 only.** Prevention clears the jammed sheaf.

### 67. Overheated Network Cabinet (`berongsmp:overheated_network_cabinet`)

- **Target Room:** Office wall

- **Normal State:** A wall comms cabinet, green status LEDs.

- **Hazardous State:** A clogged fan filter lets the switch stack run over temperature. Distinct from `dust_choked_pc` (one desktop with a backpack on its vents, not networking infrastructure).

- **Failure Consequence (450 ticks):** The overheated switch stack ignites inside the cabinet. **ABC/CO2 only.** Prevention clears the filter.

### 68. E-Bike Charging Station (`berongsmp:ebike_charging_station`)

- **Target Room:** Office storage area

- **Normal State:** An e-bike parked, unplugged.

- **Hazardous State:** Charging overnight on a cheap, uncertified charger — the pack warms and hisses. Distinct from `swollen_phone_battery`/`damaged_lipo_pack`/`charging_cart`: a vehicle-scale pack, the modern headline indoor-fire hazard.

- **Failure Consequence (300 ticks, 2-block radius):** The pack goes into thermal runaway and bursts into flame. **ABC/CO2 only.** Prevention unplugs the cheap charger.

### 69. Failing Fluorescent Ballast (`berongsmp:failing_fluorescent_ballast`)

- **Target Room:** Office ceiling

- **Normal State:** A clean twin-tube fluorescent fixture.

- **Hazardous State:** The magnetic ballast drips tar and buzzes, tube flickering.

- **Failure Consequence (400 ticks):** The failing ballast overheats and ignites the fixture housing. **ABC/CO2 only.** Prevention replaces the ballast.

### 70. Dry Aquarium Heater (`berongsmp:dry_aquarium_heater`)

- **Target Room:** Office lobby / break room

- **Normal State:** An office fish tank, water level normal, heater submerged.

- **Hazardous State:** The water level has dropped, exposing the submersible heater to open air.

- **Failure Consequence (400 ticks):** The exposed heater scorches through the acrylic lid and ignites it. **ABC/CO2 only.** Prevention tops up the tank and unplugs the heater.

### 71. Unattended Mug Warmer (`berongsmp:unattended_mug_warmer`)

- **Target Room:** Office desk

- **Normal State:** A desk mug warmer, switched off.

- **Hazardous State:** Left on and buried under a memo pile.

- **Failure Consequence (500 ticks):** The buried warmer scorches the memo pile alight. **ABC/CO2 only.** Prevention switches it off and clears the pile.

### 72. Dusty CRT Monitor (`berongsmp:dusty_crt_monitor`)

- **Target Room:** Office storage corner

- **Normal State:** An old CRT monitor, unplugged, dark.

- **Hazardous State:** Years of dust let the flyback transformer arc through the casing.

- **Failure Consequence (450 ticks):** The flyback arc ignites the casing. **ABC/CO2 only.** Prevention unplugs it and clears the dust.

### 73. Rodent-Chewed Wiring (`berongsmp:rodent_chewed_wiring`)

- **Target Room:** Office baseboard, behind cabinets

- **Normal State:** A tidy baseboard cable run.

- **Hazardous State:** Insulation gnawed to bare copper, arcing hidden from view. Distinct from `frayed_console_wire` (worn fraying on an open floor cord, not a hidden rodent-chewed baseboard run).

- **Failure Consequence (350 ticks):** The gnawed wiring arcs and ignites the baseboard. **ABC/CO2 only.** Prevention splices and re-insulates the section.

### 74. Overheating CCTV DVR (`berongsmp:overheating_cctv_dvr`)

- **Target Room:** Office security shelf

- **Normal State:** A security DVR, drives idling normally.

- **Hazardous State:** Runs 24/7 with blocked vents, the drive bay cooking.

- **Failure Consequence (450 ticks):** The overheated drive bay ignites. **ABC/CO2 only.** Prevention clears the blocked vents.

### 75. Faulty Parol Lantern (`berongsmp:faulty_parol_lantern`)

- **Target Room:** Office lobby (hanging)

- **Normal State:** An unlit Christmas parol.

- **Hazardous State:** Substandard series lights left plugged in, wiring hot at the star points.

- **Failure Consequence (300 ticks):** The hot wiring ignites the parol. **ABC/CO2 only.** Prevention unplugs the series lights.
