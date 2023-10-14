package org.mcsr.speedrunapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunConfigScreenProvider;
import org.mcsr.speedrunapi.config.api.annotations.InitializeOn;
import org.mcsr.speedrunapi.config.exceptions.InvalidConfigException;
import org.mcsr.speedrunapi.config.exceptions.NoSuchConfigException;
import org.mcsr.speedrunapi.config.exceptions.SpeedrunConfigAPIException;
import org.mcsr.speedrunapi.config.screen.SpeedrunConfigScreen;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.*;

public class SpeedrunConfigAPI {

    private static final EnumMap<InitializeOn.InitPoint, Map<ModContainer, Class<? extends SpeedrunConfig>>> CONFIGS_TO_INITIALIZE = new EnumMap<>(InitializeOn.InitPoint.class);
    private static final Map<String, SpeedrunConfigContainer<?>> CONFIGS = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, SpeedrunConfigScreenProvider> CUSTOM_CONFIG_SCREENS = Collections.synchronizedMap(new HashMap<>());
    protected static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("mcsr");
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public static void initialize() {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                CustomValue customValues = mod.getMetadata().getCustomValues().get("speedrunapi");
                if (customValues != null) {
                    CustomValue.CvObject customObject = customValues.getAsObject();

                    CustomValue config = customObject.get("config");
                    if (config != null) {
                        Class<?> configClass = Class.forName(config.getAsString());
                        if (SpeedrunConfig.class.isAssignableFrom(configClass)) {
                            InitializeOn initializeOn = configClass.getAnnotation(InitializeOn.class);
                            CONFIGS_TO_INITIALIZE.computeIfAbsent(initializeOn != null ? initializeOn.value() : InitializeOn.InitPoint.ONINITIALIZE, initPoint -> new HashMap<>()).put(mod, (Class<? extends SpeedrunConfig>) configClass);
                        }
                    }

                    CustomValue screen = customObject.get("screen");
                    if (screen != null) {
                        Class<?> screenProviderClass = Class.forName(screen.getAsString());
                        if (SpeedrunConfigScreenProvider.class.isAssignableFrom(screenProviderClass)) {
                            CUSTOM_CONFIG_SCREENS.put(mod.getMetadata().getId(), (SpeedrunConfigScreenProvider) constructClass(screenProviderClass));
                        } else {
                            throw new SpeedrunConfigAPIException("Provided config screen provider class from " + mod.getMetadata().getId() + " does not implement SpeedrunConfigScreenProvider.");
                        }
                    }
                }
            } catch (ClassCastException e) {
                throw new SpeedrunConfigAPIException("Faulty fabric.mod.json values from " + mod.getMetadata().getId() + ".", e);
            } catch (ClassNotFoundException e) {
                throw new SpeedrunConfigAPIException("Provided class from " + mod.getMetadata().getId() + " does not exist.", e);
            } catch (ReflectiveOperationException e) {
                throw new SpeedrunConfigAPIException(e);
            }
        }
    }

    public static void onPreLaunch() {
        initialize();

        registerConfigsForInitPoint(InitializeOn.InitPoint.PRELAUNCH);
    }

    public static void onInitialize() {
        registerConfigsForInitPoint(InitializeOn.InitPoint.ONINITIALIZE);
    }

    public static void onPostLaunch() {
        registerConfigsForInitPoint(InitializeOn.InitPoint.POSTLAUNCH);
    }

    private static void registerConfigsForInitPoint(InitializeOn.InitPoint initPoint) {
        Map<ModContainer, Class<? extends SpeedrunConfig>> configsToInitialize = CONFIGS_TO_INITIALIZE.get(initPoint);
        if (configsToInitialize != null) {
            configsToInitialize.forEach(SpeedrunConfigAPI::register);
            configsToInitialize.clear();
        }
    }

    private static <T extends SpeedrunConfig> void register(ModContainer mod, Class<T> configClass) {
        String modID = mod.getMetadata().getId();

        if (CONFIGS.containsKey(modID)) {
            throw new SpeedrunConfigAPIException("Config for " + modID + " is already registered!");
        }

        try {
            SpeedrunConfigContainer<T> config = new SpeedrunConfigContainer<>(constructClass(configClass), mod);
            if (modID.equals(config.getConfig().modID())) {
                CONFIGS.put(modID, config);
            } else {
                throw new InvalidConfigException("The provided SpeedrunConfig's mod ID (" + config.getConfig().modID() + ") doesn't match the providers mod ID (" + modID + ").");
            }
        } catch (ReflectiveOperationException e) {
            throw new SpeedrunConfigAPIException("Failed to build config for " + modID, e);
        }
    }
    
    private static <T> T constructClass(Class<T> aClass) throws ReflectiveOperationException {
        Constructor<T> constructor = aClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static SpeedrunConfigContainer<?> getConfig(String modID) throws NoSuchConfigException {
        SpeedrunConfigContainer<?> config = CONFIGS.get(modID);
        if (config != null) {
            return config;
        } else {
            throw new NoSuchConfigException();
        }
    }

    /**
     * Retrieves the requested option's value from the {@link SpeedrunConfig} linked to the given mod ID.
     *
     * @param modID - The mod ID of the mod owning the option.
     * @param option - The name of the option.
     * @return Returns the option's value.
     *
     * @throws NoSuchConfigException - If the given mod does not exist, does not provide a {@link SpeedrunConfig} or does not have the requested option.
     */
    public static Object getConfigValue(String modID, String option) throws NoSuchConfigException {
        return getConfig(modID).getOption(option).get();
    }

    /**
     * Wraps the result of {@link SpeedrunConfigAPI#getConfigValue} in an {@link Optional}.
     * Returns {@link Optional#empty} if a {@link NoSuchConfigException} is thrown.
     * <p>
     * This will not catch any other {@link SpeedrunConfigAPIException}'s that may be thrown.
     *
     * @param modID - The mod ID of the mod owning the option.
     * @param option - The name of the option.
     * @return Returns an {@link Optional} of the option's value.
     *
     * @see SpeedrunConfigAPI#getConfigValue
     */
    public static Optional<Object> getConfigValueOptionally(String modID, String option) {
        try {
            return Optional.of(getConfigValue(modID, option));
        } catch (NoSuchConfigException e) {
            return Optional.empty();
        }
    }

    /**
     * Sets the requested option's value from the {@link SpeedrunConfig} linked to the given mod ID.
     *
     * @param modID - The mod ID of the mod owning the option.
     * @param option - The name of the option.
     * @param value - The value to set the option to.
     *
     * @throws NoSuchConfigException - If the given mod does not exist, does not provide a {@link SpeedrunConfig} or does not have the requested option.
     */
    public static void setConfigValue(String modID, String option, Object value) throws NoSuchConfigException {
        getConfig(modID).getOption(option).setUnsafely(value);
    }

    /**
     * Calls {@link SpeedrunConfigAPI#setConfigValue}.
     * Returns {@code true} if the option is set successfully, {@code false} if a {@link NoSuchConfigException} is thrown.
     * <p>
     * This will not catch any other {@link SpeedrunConfigAPIException}'s that may be thrown.
     *
     * @param modID - The mod ID of the mod owning the option.
     * @param option - The name of the option.
     * @param value - The value to set the option to.
     *
     * @see SpeedrunConfigAPI#setConfigValue 
     */
    public static boolean setConfigValueOptionally(String modID, String option, Object value) {
        try {
            setConfigValue(modID, option, value);
            return true;
        } catch (NoSuchConfigException e) {
            return false;
        }
    }

    public static Map<ModContainer, SpeedrunConfigScreenProvider> getModConfigScreenProviders() {
        Map<ModContainer, SpeedrunConfigScreenProvider> configScreenProviders = new HashMap<>();
        CUSTOM_CONFIG_SCREENS.forEach((modID, configScreenProvider) -> configScreenProviders.put(FabricLoader.getInstance().getModContainer(modID).orElseThrow(SpeedrunConfigAPIException::new), configScreenProvider));
        CONFIGS.forEach((modID, config) -> configScreenProviders.putIfAbsent(config.getModContainer(), parent -> new SpeedrunConfigScreen(config, parent)));
        return configScreenProviders;
    }
}
