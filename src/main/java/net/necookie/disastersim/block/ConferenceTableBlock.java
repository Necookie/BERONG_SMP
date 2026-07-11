package net.necookie.disastersim.block;

/**
 * Dark-laminate boardroom table on chrome legs. Reuses {@link TableBlock}'s self-connecting
 * NORTH/SOUTH/EAST/WEST shape logic unchanged — a subclass instance only ever connects to other
 * {@code ConferenceTableBlock} instances (the check is {@code neighbourState.is(this)}), so tiling
 * conference tables merges them into one long boardroom table the same way study tables do.
 */
public class ConferenceTableBlock extends TableBlock {

    public ConferenceTableBlock(Properties props) {
        super(props);
    }
}
