package io.github.overlordsiii;

import io.github.overlordsiii.config.DimensionalAnchorConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.util.math.BlockPos;

import net.fabricmc.api.ModInitializer;

public class DimensionalAnchor implements ModInitializer {

	public static final DimensionalAnchorConfig CONFIG;

	public static final Logger LOGGER = LogManager.getLogger(DimensionalAnchor.class);

	static {
		AutoConfig.register(DimensionalAnchorConfig.class, JanksonConfigSerializer::new);
		CONFIG = AutoConfig.getConfigHolder(DimensionalAnchorConfig.class).getConfig();
	}

	/**
	 * Runs the mod initializer.
	 */
	@Override
	public void onInitialize() {

	}
}
