package alexiil.mc.lib.multipart.api;

import net.minecraft.util.Identifier;

import alexiil.mc.lib.multipart.impl.LmpInternalOnly;

/** Special {@link PartDefinition} solely for {@link MissingPart} to use. */
public final class MissingPartDefinition extends PartDefinition {

    @LmpInternalOnly
    private MissingPartDefinition(Identifier identifier) {
        super(identifier);
    }

}
