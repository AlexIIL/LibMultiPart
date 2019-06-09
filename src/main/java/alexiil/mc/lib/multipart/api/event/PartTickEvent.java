package alexiil.mc.lib.multipart.api.event;

/** Fired once per block entity tick. */
public final class PartTickEvent extends MultiPartEvent {
    public static final PartTickEvent INSTANCE = new PartTickEvent();

    private PartTickEvent() {}
}
