package org.mcsr.speedrunapi.config.api;

import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;
import org.mcsr.speedrunapi.config.screen.SpeedrunModConfigsScreen;

/**
 * Provides a custom config screen, can also be used by mods using their own config system to show up in the config list.
 * <p>
 * Register by adding your class implementing {@link SpeedrunConfigScreenProvider} to your mod's fabric.mod.json like this:
 * <p>
 * "custom": [ "speedrunapi": [ "screen": "a.b.c.ScreenProvider" ] ]
 */
public interface SpeedrunConfigScreenProvider {

    /**
     * @param parent - The active {@link SpeedrunModConfigsScreen} the config screen is opened from.
     * @return Returns a config screen for the mod providing the {@link SpeedrunConfigScreenProvider}.
     */
    @NotNull Screen createConfigScreen(Screen parent);
}
