package alexiil.mc.lib.multipart.api.event;

@FunctionalInterface
public interface EventListener<E> {
    void onEvent(E event);
}
