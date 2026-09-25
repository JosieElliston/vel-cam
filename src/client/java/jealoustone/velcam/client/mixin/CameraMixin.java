package jealoustone.velcam.client.mixin;

import jealoustone.velcam.client.VelCamClient;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
	private static final double MINIMUM_VELOCITY_SQUARED = 1.0E-7;

	@Unique
	private LocalPlayer velCam$trackedPlayer;

	@Unique
	private int velCam$sampledTick = Integer.MIN_VALUE;

	@Unique
	private Vec3 velCam$previousVelocity = Vec3.ZERO;

	@Unique
	private Vec3 velCam$currentVelocity = Vec3.ZERO;

	@Shadow
	protected abstract void setRotation(float yaw, float pitch);

	@Inject(
			method = "setup",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V", shift = At.Shift.AFTER)
	)
	private void velCam$facePlayerVelocity(Level level, Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
		velCam$setVelocityRotation(entity, partialTick);
	}

	@Inject(
			method = "setup",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(Lnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER)
	)
	private void velCam$facePlayerVelocityInMinecart(Level level, Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
		velCam$setVelocityRotation(entity, partialTick);
	}

	@Unique
	private void velCam$setVelocityRotation(Entity entity, float partialTick) {
		VelCamClient.setCameraFollowingVelocity(false);

		if (!(entity instanceof LocalPlayer player)) {
			return;
		}

		Vec3 sampledVelocity = new Vec3(
				player.getX() - player.xo,
				player.getY() - player.yo,
				player.getZ() - player.zo
		);
		velCam$sampleVelocity(player, sampledVelocity);

		if (!VelCamClient.isEnabled()) {
			return;
		}

		double frameProgress = Mth.clamp(partialTick, 0.0F, 1.0F);
		Vec3 velocity = velCam$previousVelocity.lerp(velCam$currentVelocity, frameProgress);
		if (velocity.lengthSqr() <= MINIMUM_VELOCITY_SQUARED) {
			return;
		}

		double horizontalSpeed = velocity.horizontalDistance();
		float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
		float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, horizontalSpeed));
		setRotation(yaw, pitch);
		VelCamClient.setCameraFollowingVelocity(true);
	}

	@Unique
	private void velCam$sampleVelocity(LocalPlayer player, Vec3 velocity) {
		if (player != velCam$trackedPlayer) {
			velCam$trackedPlayer = player;
			velCam$sampledTick = player.tickCount;
			velCam$previousVelocity = velocity;
			velCam$currentVelocity = velocity;
			return;
		}

		if (player.tickCount != velCam$sampledTick) {
			velCam$sampledTick = player.tickCount;
			velCam$previousVelocity = velCam$currentVelocity;
			velCam$currentVelocity = velocity;
		}
	}
}
