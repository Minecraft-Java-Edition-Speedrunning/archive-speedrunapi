package org.mcsr.speedrunapi.config.option;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.mcsr.speedrunapi.config.api.SpeedrunConfig;
import org.mcsr.speedrunapi.config.api.SpeedrunConfigStorage;
import org.mcsr.speedrunapi.config.exceptions.SpeedrunConfigAPIException;

import java.lang.reflect.Field;

public class LongOption extends WholeNumberOption<Long> {

    public LongOption(SpeedrunConfig config, SpeedrunConfigStorage configStorage, Field option) {
        super(config, configStorage, option);
    }

    @Override
    public @NotNull Long get() {
        try {
            return this.option.getLong(this.configStorage);
        } catch (IllegalAccessException e) {
            throw new SpeedrunConfigAPIException(e);
        }
    }

    @Override
    public void set(@NotNull Long value) {
        long min = this.getMin();
        long max = this.getMax();
        long intervals = this.getIntervals();

        if (this.bounds.enforce()) {
            value = MathHelper.clamp(value, min, max);
        }

        if (intervals != 0) {
            long remainder = (value - min) % intervals;
            value = value - remainder + (remainder * 2 >= intervals ? intervals : 0);
        }

        try {
            if (this.setter != null) {
                this.setter.invoke(this.configStorage, value);
            }
            this.option.setLong(this.configStorage, MathHelper.clamp(value, this.getMin(), this.getMax()));
        } catch (ReflectiveOperationException e) {
            throw new SpeedrunConfigAPIException(e);
        }
    }

    @Override
    public void fromJson(JsonElement jsonElement) {
        this.set(jsonElement.getAsLong());
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.get());
    }

    @Override
    public void setLong(long value) {
        this.set(value);
    }
}
