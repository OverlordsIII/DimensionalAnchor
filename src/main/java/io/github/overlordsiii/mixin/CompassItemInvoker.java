package io.github.overlordsiii.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.item.CompassItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

@Mixin(CompassItem.class)
public interface CompassItemInvoker {

	@Invoker("getLodestoneDimension")
	static Optional<RegistryKey<World>> callGetLodestoneDimension(NbtCompound nbt) {
		throw new UnsupportedOperationException();
	}
}
