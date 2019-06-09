package alexiil.mc.lib.multipart.api.render;

/** Used by the static part baker to generate the quad list. */
public abstract class PartModelKey {

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}
