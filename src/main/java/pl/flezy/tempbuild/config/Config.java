package pl.flezy.tempbuild.config;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;

public class Config extends OkaeriConfig {
    @Comment("Time in seconds before placed blocks decay and disappear")
    public int blockDecayTime = 30;
    @Comment("Whether decaying blocks should drop items when they disappear")
    public boolean dropBlocks = true;
}
