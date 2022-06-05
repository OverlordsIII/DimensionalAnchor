package io.github.overlordsiii.integration;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.overlordsiii.config.DimensionalAnchorConfig;
import me.shedaniel.autoconfig.AutoConfig;

public class ModMenuIntegration implements ModMenuApi {
	/**
	 * Used to construct a new config screen instance when your mod's
	 * configuration button is selected on the mod menu screen. The
	 * screen instance parameter is the active mod menu screen.
	 *
	 * @return A factory for constructing config screen instances.
	 */
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return (parent) -> AutoConfig.getConfigScreen(DimensionalAnchorConfig.class, parent).get();
	}
}
