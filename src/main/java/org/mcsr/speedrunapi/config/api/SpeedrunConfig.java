package org.mcsr.speedrunapi.config.api;

import org.mcsr.speedrunapi.config.SpeedrunConfigAPI;
import org.mcsr.speedrunapi.config.option.*;

import java.io.File;
import java.util.Map;

/**
 * Provides a custom config screen, can also be used by mods using their own config system to show up in the config list.
 * <p>
 * Register by adding your class implementing {@link SpeedrunConfig} to your mod's fabric.mod.json like this:
 * <p>
 * "custom": [ "speedrunapi": [ "config": "a.b.c.ABCConfig" ] ]
 */
public interface SpeedrunConfig extends SpeedrunConfigStorage {

    /**
     * @return Returns the mod ID of the mod owning the config.
     */
    String modID();

    /**
     * Initializes the config, creating all the {@link Option}'s it provides.
     * <p>
     * Mod Authors can override this method to add {@link CustomOption}'s.
     *
     * @return Returns a {@link Map} of all of this configs {@link Option}'s mapped to their ID's.
     */
    default Map<String, Option<?>> init() {
        return this.init(this, "");
    }

    /**
     * Mod Authors can override this method to change the config file location, for example to add global config files.
     *
     * @apiNote The directory of the returned file gets created by this method.
     * @return Returns the file the config should be saved to.
     */
    default File getConfigFile() {
        return SpeedrunConfigAPI.getConfigDir().resolve(this.modID() + ".json").toFile();
    }

    /**
     * Mod Authors can override this method to make the config unavailable, for example during runs.
     *
     * @return Returns {@code true} if the config is available.
     */
    default boolean isAvailable() {
        return true;
    }
}
