package io.github.overlordsiii.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

@Config(name = "dimensional-anchor")
public class DimensionalAnchorConfig implements ConfigData {

	@Comment("Makes a configured XP cost required in order to use dimensional anchor")
	public boolean xpTpCosts = true;

	@Comment("If user does not reach the required XP cost, certain consequences can be inflicted upon the user such as the user TP'ing to the wrong location. Requires \"xpTpCosts\" to be turned on.")
	public boolean xpConsequence = true;



}
