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

	@Comment("The xp cost in lvls for a user to teleport within the same dimension to the dimensional anchor. Requires \"xpTpCosts\" to be turned on. ")
	public int xpCostSameDimension = 5;

	@Comment("The xp cost in lvls for a user to teleport to the dimensional anchor when in a different dimension. Requires \"xpTpCosts\" to be turned on.")
	public int xpCostDifferentDimension = 30;

	@Comment("Enables teleportation to the end as a 'consequence'. Requires xpConsequence to be enabled")
	public boolean allowTpToEnd = true;


}
