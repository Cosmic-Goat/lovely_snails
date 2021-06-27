/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.lovely_snails.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.lambdaurora.lovely_snails.LovelySnails;
import dev.lambdaurora.lovely_snails.entity.SnailEntity;
import dev.lambdaurora.lovely_snails.registry.LovelySnailsRegistry;
import dev.lambdaurora.lovely_snails.screen.SnailScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Represents the snail inventory screen.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class SnailInventoryScreen extends HandledScreen<SnailScreenHandler> {
    private static final Identifier TEXTURE = LovelySnails.id("textures/gui/container/snail.png");
    private final SnailEntity entity;
    private float mouseX;
    private float mouseY;
    private EnderChestButton enderChestButton;

    public SnailInventoryScreen(SnailScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, handler.snail().getDisplayName());
        this.titleX -= 19;
        this.entity = handler.snail();
    }

    private void clearEnderChestListener() {
        if (this.enderChestButton != null && this.getScreenHandler().getInventory() instanceof SimpleInventory inventory) {
            inventory.removeListener(this.enderChestButton);
        }
        this.enderChestButton = null;
    }

    @Override
    protected void init() {
        super.init();
        this.clearEnderChestListener();

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        this.addDrawableChild(this.enderChestButton = new EnderChestButton(x + 7, y + 35 + 18));
        if (this.getScreenHandler().getInventory() instanceof SimpleInventory inventory) {
            inventory.addListener(this.enderChestButton);
        }
    }

    @Override
    public void removed() {
        super.removed();
        this.clearEnderChestListener();
    }

    @Override
    public void onClose() {
        super.onClose();
        this.clearEnderChestListener();
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        this.drawTexture(matrices, x - 19, y, 0, 0, this.backgroundWidth + 19, this.backgroundHeight);
        /*if (this.entity instanceof AbstractDonkeyEntity) {
            AbstractDonkeyEntity abstractDonkeyEntity = (AbstractDonkeyEntity) this.entity;
            if (abstractDonkeyEntity.hasChest()) {
                this.drawTexture(matrices, i + 79, j + 17, 0, this.backgroundHeight, abstractDonkeyEntity.getInventoryColumns() * 18, 54);
            }
        }*/

        if (this.entity.canBeSaddled()) {
            this.drawTexture(matrices, x + 7, y + 35 - 18, 18, this.backgroundHeight + 54, 18, 18);
        }

        this.drawTexture(matrices, x + 7, y + 35, 36, this.backgroundHeight + 54, 18, 18);

        if (!this.entity.isBaby()) {
            for (int row = y + 17; row <= y + 35 + 18; row += 18) {
                this.drawTexture(matrices, x + 7 - 18, row, 54, this.backgroundHeight + 54, 18, 18);
            }
        }

        InventoryScreen.drawEntity(x + 51, y + 60, 17,
                (x + 51) - this.mouseX,
                (y + 75 - 50) - this.mouseY,
                this.entity);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    private class EnderChestButton extends TexturedButtonWidget implements InventoryChangedListener {
        public EnderChestButton(int x, int y) {
            super(x, y, 18, 18, 0, 0, 18, LovelySnails.id("textures/gui/snail_ender_chest_button.png"),
                    18, 36,
                    btn -> {
                        var buffer = PacketByteBufs.create();
                        buffer.writeVarInt(SnailInventoryScreen.this.getScreenHandler().snail().getId());
                        ClientPlayNetworking.send(LovelySnailsRegistry.SNAIL_OPEN_ENDER_CHEST_PACKET, buffer);

                        var snail = SnailInventoryScreen.this.getScreenHandler().snail();
                        var world = snail.getEntityWorld();
                        world.playSound(MinecraftClient.getInstance().player, snail.getBlockPos(),
                                SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS,
                                .5f, snail.getRandom().nextFloat() * .1f + .9f);
                    });
        }

        @Override
        public void onInventoryChanged(Inventory sender) {
            this.visible = this.active = SnailInventoryScreen.this.getScreenHandler().hasEnderChest();
        }
    }
}