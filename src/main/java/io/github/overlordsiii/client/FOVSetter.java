package io.github.overlordsiii.client;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public class FOVSetter {
	private int fovPrevious;

	public void getFov() {
		this.fovPrevious = MinecraftClient.getInstance().options.getFov().getValue();
	}

	public void setMinecraftFov() {
		MinecraftClient.getInstance().options.getFov().setValue(this.fovPrevious);
	}
}
