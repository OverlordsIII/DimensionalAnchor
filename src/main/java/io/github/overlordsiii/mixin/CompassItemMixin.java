package io.github.overlordsiii.mixin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.loader.api.FabricLoader;

import static io.github.overlordsiii.DimensionalAnchor.*;

@Mixin(CompassItem.class)
public abstract class CompassItemMixin extends Item {

	private int fovPrevious;

	public CompassItemMixin(Settings settings) {
		super(settings);
	}

	@Inject(method = "useOnBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playSound(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V"))
	private void sendMessageToUser(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			// debug
			context.getPlayer().sendMessage(Text.literal("Linked!"), true);
		}
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		if (CompassItem.hasLodestone(stack)) {
			return UseAction.BOW;
		}

		return super.getUseAction(stack);
	}


	/**
	 * {@return the maximum use (right-click) time of this item, in ticks}
	 * Once a player has used an item for said number of ticks, they stop using it, and {@link Item#finishUsing} is called.
	 *
	 * @param stack
	 */
	@Override
	public int getMaxUseTime(ItemStack stack) {
		if (CompassItem.hasLodestone(stack)) {
			return 24;
		}
		return super.getMaxUseTime(stack);
	}

	@Override
	public boolean isUsedOnRelease(ItemStack stack) {
		return true;
	}



	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity entity, int remainingUseTicks) {

		if (world.isClient) {
			MinecraftClient.getInstance().options.getFov().setValue(this.fovPrevious);
			return;
		}

		if ((entity instanceof PlayerEntity user) && !world.isClient && remainingUseTicks <= 0) {
			if (CompassItem.hasLodestone(stack) && !world.isClient()) {
				// can ignore warning since we already checked if compass is linked to lodestone
				BlockPos pos = NbtHelper.toBlockPos((NbtCompound) stack.getOrCreateNbt().get(CompassItem.LODESTONE_POS_KEY));
				Optional<RegistryKey<World>> optional = CompassItemInvoker.callGetLodestoneDimension(stack.getOrCreateNbt());
				if (optional.isPresent()) {
					// same dimension as user
					if (optional.get().equals(world.getRegistryKey())) {
						info("Dimensional Anchor used: Same dimension");
						if (CONFIG.xpTpCosts) {
							info("xpTpCosts enabled");
							if (user.experienceLevel >= CONFIG.xpCostSameDimension || shouldAllowInCreative(user)) {
								info("same dimension travel critera met. Executing...");
								BlockPos tpPos = findTpPosition(pos, world);
								if (tpPos != null) {
									user.addExperienceLevels(-CONFIG.xpCostSameDimension);
									user.teleport(tpPos.getX(), tpPos.getY() + 1, tpPos.getZ());
								} else {
									user.sendMessage(Text.literal("Could not find suitable spot to teleport to! Teleport canceled"), true);
								}
								info("Executed.");
							} else if (CONFIG.xpConsequence) {
								info("Same dimension travel criteria NOT met. Executing consequences...");
								Random random = new Random();
								int rand = random.nextInt(3);
								doSameDimensionConsequences(rand, user, pos, stack, world, random);
								info("Executed.");
							}
							return;
						}
						info("xpTpCosts not enabled, teleporting normally...");
						BlockPos tpPos = findTpPosition(pos, world);
						if (tpPos != null) {
							user.addExperienceLevels(-CONFIG.xpCostSameDimension);
							user.teleport(tpPos.getX(), tpPos.getY() + 1, tpPos.getZ());
						} else {
							user.sendMessage(Text.literal("Could not find suitable spot to teleport to! Teleport canceled"), true);
						}					}
					// different dimension
					else {
						info("Dimensional Anchor used: Different dimension");
						ServerWorld lodestoneDimension = Objects.requireNonNull(world.getServer()).getWorld(optional.get());
						if (CONFIG.xpTpCosts) {
							info("xpTpCosts enabled");
							if (user.experienceLevel >= CONFIG.xpCostDifferentDimension || shouldAllowInCreative(user)) {
								info("different dimension travel critera met. Executing...");
								user.addExperienceLevels(-CONFIG.xpCostDifferentDimension);
								BlockPos tpPos = findTpPosition(pos, lodestoneDimension);
								if (tpPos != null) {
									FabricDimensions.teleport(user, lodestoneDimension, new TeleportTarget(new Vec3d(tpPos.getX(), tpPos.getY() + 1, tpPos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
								} else {
									user.sendMessage(Text.literal("Could not find suitable spot to teleport to! Teleport canceled"), true);
								}
								info("Executed.");
							} else if (CONFIG.xpConsequence) {
								info("Different dimension travel criteria NOT met. Executing consequences...");
								Random random = new Random();

								doDifferentDimensionConsequences(user, stack, world, random);
								info("Executed.");
							}
							return;
						}
						BlockPos tpPos = findTpPosition(pos, lodestoneDimension);
						if (tpPos != null) {
							FabricDimensions.teleport(user, lodestoneDimension, new TeleportTarget(new Vec3d(tpPos.getX(), tpPos.getY() + 1, tpPos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
						} else {
							user.sendMessage(Text.literal("Could not find suitable spot to teleport to! Teleport canceled"), true);
						}
					}
				}
			}
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
		if (CompassItem.hasLodestone(user.getStackInHand(hand))) {
			if (world.isClient) {
				this.fovPrevious = MinecraftClient.getInstance().options.getFov().getValue();
				return super.use(world, user, hand);
			}
			user.sendMessage(Text.literal("Charging... (let go when fully charged)"), true);
			user.setCurrentHand(hand);
		}
		return super.use(world, user, hand);
	}



	private void doDifferentDimensionConsequences(PlayerEntity user, ItemStack stack, World world, Random random) {
		user.damage(DamageSource.OUT_OF_WORLD, random.nextInt(6) + 5);
		stack.decrement(1);
		world.playSound(user, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.AMBIENT, 1f, 1f);

		List<World> worlds = Lists.newArrayList(Objects.requireNonNull(world.getServer()).getWorlds());

		if (!CONFIG.allowTpToEnd) {
			info("TP to the end disabled, removing it from worlds list...");

			info("Worlds list before: " + toStringList(worlds));

			worlds = worlds
				.stream()
				.filter(world1 -> !world1.getRegistryKey().toString().contains("minecraft:the_end"))
				.collect(Collectors.toList());

			info("Worlds list after: " + toStringList(worlds));

		}

		// can cast since server only holds serverworlds
		ServerWorld randomWorld = (ServerWorld) worlds.get(random.nextInt(worlds.size()));

		BlockPos targetPos = null;

		info("Found random world to tp to: " + randomWorld.getRegistryKey());

		while (targetPos == null) {
			int x = random.nextInt(-15000, 15001);
			int z = random.nextInt(-15000, 15001);
			int y = random.nextInt(-63, 300);

			BlockPos attemptedPos = new BlockPos(x, y, z);
			if ((world.getBlockState(attemptedPos).isAir() && world.getBlockState(attemptedPos.up()).isAir()) || (world.getBlockState(attemptedPos).getFluidState().isIn(FluidTags.WATER))) {
				targetPos = attemptedPos;
			}
		}

		info("Found target BlockPos: " + targetPos + ". Teleporting now...");
		FabricDimensions.teleport(user, randomWorld, new TeleportTarget(new Vec3d(targetPos.getX(), targetPos.getY() + 1, targetPos.getZ()), user.getVelocity(), user.getYaw(), user.getPitch()));
	}


	private void doSameDimensionConsequences(int rand, PlayerEntity user, BlockPos pos, ItemStack stack, World world, Random random) {
		switch (rand) {
			case 0 -> {
				info("case 0, damaging user and tp..ing to right coords");
				user.damage(DamageSource.GENERIC, random.nextInt(5) + 1);
				BlockPos tpPos = findTpPosition(pos, world);
				if (tpPos != null) {
					user.teleport(tpPos.getX(), tpPos.getY() + 1, tpPos.getZ());
				} else {
					user.sendMessage(Text.literal("Could not find suitable spot to teleport to! Teleport canceled"), true);
				}
			}
			case 1 -> {
				info("case 1, getting rid of compass");
				stack.decrement(1);
				world.playSound(user, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_BREAK, SoundCategory.AMBIENT, 1f, 1f);
			}
			case 2 -> {
				info("case 2, tping randomly within 25 block radius of lodestone blockPos: " + pos);
				BlockPos targetPos = null;
				for (BlockPos blockPos : BlockPos.iterateRandomly(net.minecraft.util.math.random.Random.create(), 300, pos, 25)) {
					if ((world.getBlockState(blockPos).isAir() && world.getBlockState(blockPos.up()).isAir()) || (world.getBlockState(blockPos).getFluidState().isIn(FluidTags.WATER))) {
						targetPos = blockPos;
						break;
					}
				}
				if (targetPos == null) {
					info("could not find suitable coord to tp too, doing same dim consequences again");
					doSameDimensionConsequences(random.nextInt(2), user, pos, stack, world, random);
				} else {
					info("Found target BlockPos: " + targetPos + ". Teleporting now...");
					user.teleport(targetPos.getX(), targetPos.getY(), targetPos.getZ());
				}
			}
			default -> throw new IllegalStateException("Tried to do consequences for Dimensional Anchor with a value higher than expected!");
		}
	}

	private static BlockPos findTpPosition(BlockPos lodestonePos, World world) {

		if ((world.getBlockState(lodestonePos.up()).isAir() && world.getBlockState(lodestonePos.up().up()).isAir()) || (world.getBlockState(lodestonePos).getFluidState().isIn(FluidTags.WATER))) {
			return lodestonePos;
		}

		BlockPos targetPos = null;
		for (BlockPos blockPos : BlockPos.iterateOutwards(lodestonePos, 5, 5, 5)) {
			if ((world.getBlockState(blockPos).isAir() && world.getBlockState(blockPos.up()).isAir()) || (world.getBlockState(blockPos).getFluidState().isIn(FluidTags.WATER))) {
				targetPos = blockPos;
				break;
			}
		}

		return targetPos;
	}

	/**
	 * returns if the cost should be free depending on environment
	 * @return true if tp cost is bypassed, return false if it is not
	 */
	private static boolean shouldAllowInCreative(PlayerEntity entity) {

		if (entity.isCreative()) {
			return !FabricLoader.getInstance().isDevelopmentEnvironment();
		}

		return false;
	}

	private static List<String> toStringList(List<World> worldsList) {
		return worldsList
			.stream()
			.map(World::getRegistryKey)
			.map(RegistryKey::toString)
			.collect(Collectors.toList());
	}

	private static void info(String s) {
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			LOGGER.info(s);
		}

	}
}
