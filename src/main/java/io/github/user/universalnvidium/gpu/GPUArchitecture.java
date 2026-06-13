package io.github.user.universalnvidium.gpu;

public enum GPUArchitecture {
    UNKNOWN("Unknown GPU", -1),
    UNSUPPORTED("Unsupported GPU", -1),
    KEPLER("Kepler (GTX 600/700)", 0),
    MAXWELL("Maxwell (GTX 800/900)", 1),
    PASCAL("Pascal (GTX 1000)", 2),
    TURING("Turing (GTX 1600/RTX 2000)", 3),
    AMPERE("Ampere (RTX 3000)", 4),
    ADA_LOVELACE("Ada Lovelace (RTX 4000)", 5),
    BLACKWELL("Blackwell (RTX 5000)", 6);

    private final String displayName;
    private final int tier;

    GPUArchitecture(String displayName, int tier) {
        this.displayName = displayName;
        this.tier = tier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public boolean supportsMeshShaders() {
        return this.ordinal() >= GPUArchitecture.TURING.ordinal();
    }

    public boolean supportsBindlessTextures() {
        return this.ordinal() >= GPUArchitecture.KEPLER.ordinal();
    }

    public boolean supportsMultiDrawIndirect() {
        return this.ordinal() >= GPUArchitecture.MAXWELL.ordinal();
    }

    public boolean supportsPersistentBuffers() {
        return this.ordinal() >= GPUArchitecture.KEPLER.ordinal();
    }

    public boolean supportsComputeShaders() {
        return this.ordinal() >= GPUArchitecture.KEPLER.ordinal();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
