package io.github.overlordsiii.mixin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import com.google.common.collect.Lists;
import org.checkerframework.checker.units.qual.A;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CompassItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Boxes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.loader.api.FabricLoader;

import static io.github.overlordsiii.DimensionalAnchor.*;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {
	public CompassItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
	private void sendMessageToUser(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// debug
			context.getPlayer().sendMessage(new LiteralText("Linked!"), true);
		}
	}


	/**
	 * Called when an item is used by a player.
	 * The use action, by default, is bound to the right mouse button.
	 *
	 * <p>This method is called on both the logical client and logical server, so take caution when overriding this method.
	 * The logical side can be checked using {@link World#isClient() world.isClient()}.
	 *
	 * @param world the world the item was used in
	 * @param user  the player who used the item
	 * @param hand  the hand used
	 * @return a typed action result that specifies whether using the item was successful.
	 * The action result contains the new item stack that the player's hand will be set to.
	 */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (CompassItem.hasLodestone(stack) && !world.isClient()) {
			// can ignore warning since we already checked if compass is linked to lodestone
			BlockPos pos = NbtHelper.toBlockPos((NbtCompound) stack.getOrCreateNbt().get(CompassItem.LODESTONE_POS_KEY));
			Optional<RegistryKey<World>> optional = CompassItem.getLodestoneDimension(stack.getOrCreateNbt());
			if (optional.isPresent()) {
				// same dimension as user
				if (optional.get().equals(world.getRegistryKey())) {
					LOGGER.info("Dimensional Anchor used: Same dimension");
					if (CONFIG.xpTpCosts) {
						LOGGER.info("xpTpCosts enabled");
						if (user.experienceLevel >= 5) {
							LOGGER.info("same dimension travel critera met. Executing...");
							user.addExperienceLevels(-5);
							user.teleport(pos.getX(), pos.getY() + 1, pos.getZ());
							LOGGER.info("Executed.");
						} else if (CONFIG.xpConsequence) {
							LOGGER.info("Same dimension travel criteria NOT met. Executing consequences...");
							Random random = new Random();
							int rand = random.nextInt(3);
							doSameDimensionConsequences(rand, user, pos, stack, world, random);
							LOGGER.info("Executed.");
						}
						return super.use(world, user, hand);
					}
					LOGGER.info("xpTpCosts not enabled, teleporting normally...");
					user.teleport(pos.getX(), pos.getY() + 1, pos.getZ());
				}
				// different dimension
				else {
					LOGGER.info("Dimensional Anchor used: Different dimension");
					ServerWorld lodestoneDimension = Objects.requireNonNull(world.getServer()).getWorld(optional.get());
					if (CONFIG.xpTpCosts) {
						LOGGER.info("xpTpCosts enabled");
						if (user.experienceLevel >= 30) {
							LOGGER.info("different dimension travel critera met. Executing...");
							user.addExperienceLevels(-30);
							FabricDimensions.teleport(user, lodestoneDimension, new TeleportTarget(new Vec3d(pos.getX(), pos.getY() + 1, pos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
							LOGGER.info("Executed.");
						} else if (CONFIG.xpConsequence) {
							LOGGER.info("Different dimension travel criteria NOT met. Executing consequences...");
							Random random = new Random();

							doDifferentDimensionConsequences(user, stack, world, random);
							LOGGER.info("Executed.");
						}
						return super.use(world, user, hand);
					}
					FabricDimensions.teleport(user, lodestoneDimension, new TeleportTarget(new Vec3d(pos.getX(), pos.getY() + 1, pos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
				}
			}
		}
		return super.use(world, user, hand);
	}

	private void doDifferentDimensionConsequences(PlayerEntity user, ItemStack stack, World world, Random random) {
		user.damage(DamageSource.OUT_OF_WORLD, random.nextInt(6) + 5);
		stack.decrement(1);
		world.playSound(user, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.AMBIENT, 1f, 1f);

		List<World> worlds = Lists.newArrayList(Objects.requireNonNull(world.getServer()).getWorlds());
		// can cast since server only holds serverworlds
		ServerWorld randomWorld = (ServerWorld) worlds.get(random.nextInt(worlds.size()));

		BlockPos targetPos = null;

		LOGGER.info("Found random world to tp to: " + randomWorld.getRegistryKey());

		while (targetPos == null) {
			int x = random.nextInt(-15000, 15001);
			int z = random.nextInt(-15000, 15001);
			int y = random.nextInt(-63, 300);

			BlockPos attemptedPos = new BlockPos(x, y, z);
			if ((world.getBlockState(attemptedPos).isAir() && world.getBlockState(attemptedPos.up()).isAir()) || (world.getBlockState(attemptedPos).getFluidState().isIn(FluidTags.WATER)) || (world.getBlockState(attemptedPos).getFluidState().isIn(FluidTags.LAVA))) {
				targetPos = attemptedPos;
			}
		}

		LOGGER.info("Found target BlockPos: " + targetPos + ". Teleporting now...");
		FabricDimensions.teleport(user, randomWorld, new TeleportTarget(new Vec3d(targetPos.getX(), targetPos.getY() + 1, targetPos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
	}


	private void doSameDimensionConsequences(int rand, PlayerEntity user, BlockPos pos, ItemStack stack, World world, Random random) {
		switch (rand) {
			case 0 -> {
				LOGGER.info("case 0, damaging user and tp..ing to right coords");
				user.damage(DamageSource.OUT_OF_WORLD, random.nextInt(5) + 1);
				user.teleport(pos.getX(), pos.getY() + 1, pos.getZ());
			}
			case 1 -> {
				LOGGER.info("case 1, getting rid of compass");
				stack.decrement(1);
				world.playSound(user, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.AMBIENT, 1f, 1f);
			}
			case 2 -> {
				LOGGER.info("case 2, tping randomly within 25 block radius of lodestone blockPos: " + pos);
				BlockPos targetPos = null;
				for (BlockPos blockPos : BlockPos.iterateRandomly(random, 300, pos, 25)) {
					if ((world.getBlockState(blockPos).isAir() && world.getBlockState(blockPos.up()).isAir()) || (world.getBlockState(blockPos).getFluidState().isIn(FluidTags.WATER)) || (world.getBlockState(blockPos).getFluidState().isIn(FluidTags.LAVA))) {
						targetPos = blockPos;
						break;
					}
				}
				if (targetPos == null) {
					LOGGER.info("could not find suitable coord to tp too, doing same dim consequences again");
					doSameDimensionConsequences(random.nextInt(2), user, pos, stack, world, random);
				} else {
					LOGGER.info("Found target BlockPos: " + targetPos + ". Teleporting now...");
					user.teleport(targetPos.getX(), targetPos.getY(), targetPos.getZ());
				}
			}
			default -> throw new IllegalStateException("Tried to do consequences for Dimensional Anchor with a value higher than expected!");
		}
	}
}
