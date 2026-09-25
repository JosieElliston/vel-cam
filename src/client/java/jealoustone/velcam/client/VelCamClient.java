package jealoustone.velcam.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

public class VelCamClient implements ClientModInitializer {
	private static final int CROSSHAIR_SIZE = 15;
	private static final Identifier CROSSHAIR_SPRITE = Identifier.withDefaultNamespace("hud/crosshair");
	private static final Identifier LOOK_CROSSHAIR = Identifier.fromNamespaceAndPath("vel-cam", "look_crosshair");
	private static boolean enabled;
	private static boolean cameraFollowingVelocity;

	@Override
	public void onInitializeClient() {
		KeyMapping toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.vel-cam.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_C,
				KeyMapping.Category.MISC
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				enabled = !enabled;
				if (!enabled) {
					cameraFollowingVelocity = false;
				}

				if (client.player != null) {
					client.player.sendSystemMessage(Component.translatable(
							"message.vel-cam.toggled",
							Component.translatable(enabled ? "options.on" : "options.off")
					));
				}
			}
		});

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.CROSSHAIR,
				LOOK_CROSSHAIR,
				VelCamClient::extractLookCrosshair
		);
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setCameraFollowingVelocity(boolean followingVelocity) {
		cameraFollowingVelocity = followingVelocity;
	}

	private static void extractLookCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (!enabled || !cameraFollowingVelocity || player == null
				|| !client.options.getCameraType().isFirstPerson()) {
			return;
		}

		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
		Vec3 lookDirection = player.getViewVector(partialTick);
		Camera camera = client.gameRenderer.mainCamera();
		Vector3fc cameraForward = camera.forwardVector();
		double facingCamera = lookDirection.x * cameraForward.x()
				+ lookDirection.y * cameraForward.y()
				+ lookDirection.z * cameraForward.z();
		if (facingCamera <= 0.0) {
			return;
		}

		Vec3 projected = client.gameRenderer.projectPointToScreen(camera.position().add(lookDirection));
		if (!Double.isFinite(projected.x) || !Double.isFinite(projected.y)
				|| Math.abs(projected.x) > 1.0 || Math.abs(projected.y) > 1.0) {
			return;
		}

		int crosshairX = (int) Math.floor(
				(projected.x + 1.0) * graphics.guiWidth() * 0.5 - CROSSHAIR_SIZE * 0.5
		);
		int crosshairY = (int) Math.floor(
				(1.0 - projected.y) * graphics.guiHeight() * 0.5 - CROSSHAIR_SIZE * 0.5
		);
		int vanillaCrosshairX = (graphics.guiWidth() - CROSSHAIR_SIZE) / 2;
		int vanillaCrosshairY = (graphics.guiHeight() - CROSSHAIR_SIZE) / 2;
		if (Math.abs(crosshairX - vanillaCrosshairX) <= 2
				&& Math.abs(crosshairY - vanillaCrosshairY) <= 2) {
			return;
		}

		graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				CROSSHAIR_SPRITE,
				crosshairX + 1,
				crosshairY + 1,
				CROSSHAIR_SIZE,
				CROSSHAIR_SIZE,
				0xFF000000
		);
		graphics.blitSprite(
				RenderPipelines.GUI_TEXTURED,
				CROSSHAIR_SPRITE,
				crosshairX,
				crosshairY,
				CROSSHAIR_SIZE,
				CROSSHAIR_SIZE,
				0xFFFFFFFF
		);
	}
}
