/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

/**
 * Replaces the vanilla block selection outline. VetmoBlockOutlineWorldRendererMixin cancels vanilla's
 * drawBlockOutline and records the block vanilla wanted to outline, so every vanilla visibility rule
 * (F1, spectator, adventure mode, world border...) still applies. We draw it in Render3DEvent the same frame.
 */
public class BlockOutline extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Double> lineWidth = sgGeneral.add(new DoubleSetting.Builder()
        .name("line-width")
        .description("Thickness of the outline in (approximate) pixels. 1 uses plain 1px lines.")
        .defaultValue(2.5)
        .range(1, 10)
        .sliderRange(1, 8)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> fill = sgGeneral.add(new BoolSetting.Builder()
        .name("fill")
        .description("Fill the selected block with a translucent color.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> throughWalls = sgGeneral.add(new BoolSetting.Builder()
        .name("through-walls")
        .description("Render the outline on top of everything instead of depth-tested like vanilla.")
        .defaultValue(false)
        .build()
    );

    // Colors

    private final Setting<SettingColor> lineColor = sgColors.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Color of the outline.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> fillColor = sgColors.add(new ColorSetting.Builder()
        .name("fill-color")
        .description("Color of the fill.")
        .defaultValue(new SettingColor(255, 40, 40, 45))
        .visible(fill::get)
        .build()
    );

    private BlockPos targetPos;
    private BlockState targetState;

    public BlockOutline() {
        super(Categories.Visuals, "block-outline", "Custom color, thickness and fill for the block selection outline.");
    }

    @Override
    public void onDeactivate() {
        targetPos = null;
        targetState = null;
    }

    /** Called by the mixin instead of vanilla's outline. Returns true if vanilla should be cancelled. */
    public boolean replaceVanilla(BlockPos pos, BlockState state) {
        if (!isActive()) return false;
        targetPos = pos.toImmutable();
        targetState = state;
        return true;
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        BlockPos pos = targetPos;
        BlockState state = targetState;
        targetPos = null;
        targetState = null;

        if (pos == null || state == null || mc.world == null) return;

        VoxelShape shape = state.getOutlineShape(mc.world, pos, mc.getCameraEntity() != null ? ShapeContext.of(mc.getCameraEntity()) : ShapeContext.absent());
        if (shape.isEmpty()) return;

        Renderer3D renderer = throughWalls.get() ? event.renderer : event.depthRenderer;
        double x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // Fill
        if (fill.get()) {
            double e = 0.002;
            for (Box b : shape.getBoundingBoxes()) {
                renderer.boxSides(x + b.minX - e, y + b.minY - e, z + b.minZ - e, x + b.maxX + e, y + b.maxY + e, z + b.maxZ + e, fillColor.get(), 0);
            }
        }

        // Outline
        SettingColor color = lineColor.get();
        double width = lineWidth.get();

        if (width <= 1.05) {
            shape.forEachEdge((x1, y1, z1, x2, y2, z2) ->
                renderer.line(x + x1, y + y1, z + z1, x + x2, y + y2, z + z2, color)
            );
            return;
        }

        // Core profile can't widen GL lines, so draw each edge as a thin box. Scale by distance so the
        // thickness stays roughly constant on screen (~0.0013 blocks per pixel per block of distance at 70 FOV / 1080p).
        double dx = x + 0.5 - event.offsetX, dy = y + 0.5 - event.offsetY, dz = z + 0.5 - event.offsetZ;
        double dist = Math.max(1, Math.sqrt(dx * dx + dy * dy + dz * dz));
        double h = width * 0.5 * 0.0013 * dist;

        shape.forEachEdge((x1, y1, z1, x2, y2, z2) ->
            renderer.boxSides(
                x + Math.min(x1, x2) - h, y + Math.min(y1, y2) - h, z + Math.min(z1, z2) - h,
                x + Math.max(x1, x2) + h, y + Math.max(y1, y2) + h, z + Math.max(z1, z2) + h,
                color, 0
            )
        );
    }
}
