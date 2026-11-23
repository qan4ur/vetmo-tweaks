/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3d;

import java.util.*;

import static meteordevelopment.meteorclient.systems.modules.visuals.VetmoHudRender.*;

public class TotemPopCounter extends Module {
    public enum FriendMode { Same, Highlight, Ignore }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> ignoreSelf = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-self")
        .description("Don't count your own pops.")
        .defaultValue(false)
        .build()
    );

    private final Setting<FriendMode> friendMode = sgGeneral.add(new EnumSetting.Builder<FriendMode>()
        .name("friends")
        .description("How friends are handled: same as others, highlighted with the friend color, or ignored.")
        .defaultValue(FriendMode.Highlight)
        .build()
    );

    private final Setting<Boolean> chat = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-message")
        .description("Print a chat message when someone pops.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> deathMessage = sgGeneral.add(new BoolSetting.Builder()
        .name("death-message")
        .description("Print a chat message when a player dies after popping.")
        .defaultValue(true)
        .visible(chat::get)
        .build()
    );

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("above-head")
        .description("Show the pop count above players' heads.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> totemIcon = sgRender.add(new BoolSetting.Builder()
        .name("totem-icon")
        .description("Draw a small totem icon next to the count.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<Boolean> floatingText = sgRender.add(new BoolSetting.Builder()
        .name("float-up")
        .description("A \"-1\" floats up and fades out on every pop.")
        .defaultValue(true)
        .visible(render::get)
        .build()
    );

    private final Setting<Double> scale = sgRender.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Badge scale.")
        .defaultValue(1.2)
        .range(0.3, 4)
        .sliderRange(0.3, 3)
        .visible(render::get)
        .build()
    );

    private final Setting<Double> height = sgRender.add(new DoubleSetting.Builder()
        .name("height")
        .description("Height above the player's head (blocks). Raise it if it overlaps nametags.")
        .defaultValue(0.95)
        .range(0, 3)
        .sliderRange(0, 2)
        .visible(render::get)
        .build()
    );

    private final Setting<Integer> range = sgRender.add(new IntSetting.Builder()
        .name("range")
        .description("Max distance to draw badges at.")
        .defaultValue(64)
        .range(1, 256)
        .sliderRange(8, 128)
        .visible(render::get)
        .build()
    );

    // Colors

    private final Setting<SettingColor> bgColor = sgColors.add(new ColorSetting.Builder()
        .name("background")
        .description("Badge background.")
        .defaultValue(new SettingColor(18, 6, 8, 190))
        .build()
    );

    private final Setting<SettingColor> accentColor = sgColors.add(new ColorSetting.Builder()
        .name("accent")
        .description("Count text and accent line.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> friendColor = sgColors.add(new ColorSetting.Builder()
        .name("friend")
        .description("Accent used for friends.")
        .defaultValue(new SettingColor(255, 170, 170))
        .visible(() -> friendMode.get() == FriendMode.Highlight)
        .build()
    );

    private final Setting<SettingColor> glowColor = sgColors.add(new ColorSetting.Builder()
        .name("pop-flash")
        .description("Flash color right after a pop.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .build()
    );

    private static final class PopState {
        int pops;
        long lastPopNs;
        final ArrayDeque<Long> floaters = new ArrayDeque<>();
    }

    private static final double BOUNCE_TIME = 0.6, FLOAT_TIME = 0.9;
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    private final Map<UUID, PopState> pops = new HashMap<>();
    private final Vector3d pos = new Vector3d();
    private ClientWorld lastWorld;

    public TotemPopCounter() {
        super(Categories.Visuals, "totem-pop-counter", "Counts totem pops per player and shows them above their heads.");
    }

    @Override
    public void onActivate() {
        pops.clear();
        lastWorld = mc.world;
    }

    @Override
    public void onDeactivate() {
        pops.clear();
    }

    /** Pop count for a player, 0 if none. Public so other modules (e.g. nametags) can read it. */
    public int getPops(PlayerEntity player) {
        PopState s = pops.get(player.getUuid());
        return s == null ? 0 : s.pops;
    }

    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        pops.clear();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        pops.clear();
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        // packets may arrive on the netty thread; do the world lookup and chat on the client thread
        if (event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == EntityStatuses.USE_TOTEM_OF_UNDYING) {
            mc.execute(() -> onPop(packet));
        } else if (event.packet instanceof PlayerRemoveS2CPacket packet) {
            List<UUID> ids = List.copyOf(packet.profileIds());
            mc.execute(() -> ids.forEach(pops::remove));
        }
    }

    private void onPop(EntityStatusS2CPacket packet) {
        if (!isActive() || mc.world == null || mc.player == null) return;
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player)) return;
        if (player == mc.player && ignoreSelf.get()) return;

        boolean friend = player != mc.player && Friends.get().isFriend(player);
        if (friend && friendMode.get() == FriendMode.Ignore) return;

        PopState state = pops.computeIfAbsent(player.getUuid(), k -> new PopState());
        state.pops++;
        state.lastPopNs = System.nanoTime();
        state.floaters.addLast(state.lastPopNs);
        while (state.floaters.size() > 6) state.floaters.removeFirst();

        if (chat.get()) {
            MutableText msg = Text.empty()
                .append(nameText(player, friend))
                .append(Text.literal(" popped ").withColor(0xFFAAAA))
                .append(Text.literal(String.valueOf(state.pops)).withColor(rgb(accentColor.get())).formatted(Formatting.BOLD))
                .append(Text.literal(state.pops == 1 ? " totem" : " totems").withColor(0xFFAAAA));
            ChatUtils.sendMsg(chatId(player), "Pops", Formatting.RED, msg);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world != lastWorld) {
            // dimension change / new world
            pops.clear();
            lastWorld = mc.world;
        }
        if (mc.world == null || pops.isEmpty()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            PopState state = pops.get(player.getUuid());
            if (state == null) continue;
            if (player.isDead() || player.getHealth() <= 0 || player.deathTime > 0) {
                pops.remove(player.getUuid());
                if (chat.get() && deathMessage.get() && state.pops > 0) {
                    boolean friend = player != mc.player && Friends.get().isFriend(player);
                    MutableText msg = Text.empty()
                        .append(nameText(player, friend))
                        .append(Text.literal(" died after popping ").withColor(0xFFAAAA))
                        .append(Text.literal(String.valueOf(state.pops)).withColor(rgb(accentColor.get())).formatted(Formatting.BOLD))
                        .append(Text.literal(state.pops == 1 ? " totem" : " totems").withColor(0xFFAAAA));
                    ChatUtils.sendMsg(chatId(player), "Pops", Formatting.RED, msg);
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (!render.get() || mc.world == null || mc.player == null || mc.options.hudHidden || pops.isEmpty()) return;

        long now = System.nanoTime();
        DrawContext ctx = event.drawContext;
        TextRenderer tr = mc.textRenderer;
        double maxSq = range.get() * range.get();

        for (PlayerEntity player : mc.world.getPlayers()) {
            PopState state = pops.get(player.getUuid());
            if (state == null || state.pops <= 0) continue;
            if (player == mc.player && mc.options.getPerspective().isFirstPerson()) continue;
            if (player.squaredDistanceTo(mc.gameRenderer.getCamera().getPos()) > maxSq) continue;
            if (player.isInvisibleTo(mc.player)) continue;

            boolean friend = player != mc.player && Friends.get().isFriend(player);
            if (friend && friendMode.get() == FriendMode.Ignore) continue;
            int accent = (friend && friendMode.get() == FriendMode.Highlight ? friendColor.get() : accentColor.get()).getPacked();

            Utils.set(pos, player, event.tickDelta);
            pos.add(0, player.getHeight() + height.get(), 0);
            if (!NametagUtils.to2D(pos, scale.get())) continue;

            NametagUtils.begin(pos, ctx);
            drawBadge(ctx, tr, state, accent, now);
            NametagUtils.end(ctx);
        }
    }

    private void drawBadge(DrawContext ctx, TextRenderer tr, PopState state, int accent, long now) {
        Matrix3x2fStack m = ctx.getMatrices();
        double since = (now - state.lastPopNs) / 1e9;

        String text = "-" + state.pops;
        int tw = tr.getWidth(text);
        int iconW = totemIcon.get() ? 12 : 0;
        int w = 4 + iconW + tw + 4;
        int h = 14;
        int x = -w / 2, y = -h;

        // bounce: overshoot then settle (damped spring)
        float bounce = 1f;
        float flash = 0f;
        if (since < BOUNCE_TIME) {
            float t = (float) since;
            bounce = 1f + 0.55f * (float) (Math.exp(-7 * t) * Math.cos(16 * t));
            flash = 1f - (float) (since / BOUNCE_TIME);
        }

        m.pushMatrix();
        m.translate(0, y + h / 2f);
        m.scale(Math.max(0.05f, bounce));
        m.translate(0, -(y + h / 2f));

        // flash halo
        if (flash > 0) {
            SettingColor g = glowColor.get();
            int gc = withAlpha(g.getPacked(), (int) (g.a * flash));
            int grow = Math.round(3 * flash);
            ctx.fill(x - grow, y - grow + 1, x + w + grow, y + h + grow - 1, gc);
            ctx.fill(x - grow + 1, y - grow, x + w + grow - 1, y + h + grow, gc);
        }

        // badge body (rounded)
        int bg = bgColor.get().getPacked();
        ctx.fill(x + 1, y, x + w - 1, y + h, bg);
        ctx.fill(x, y + 1, x + 1, y + h - 1, bg);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, accent | 0xFF000000);

        int tx = x + 4;
        if (iconW > 0) {
            m.pushMatrix();
            m.translate(tx, y + 1);
            m.scale(0.75f);
            ctx.drawItem(TOTEM, 0, 0);
            m.popMatrix();
            tx += iconW;
        }

        int textColor = lerpColor(accent | 0xFF000000, 0xFFFFFFFF, flash);
        ctx.drawText(tr, text, tx, y + 3, textColor, true);
        m.popMatrix();

        // floating "-1"s
        if (floatingText.get()) {
            Iterator<Long> it = state.floaters.iterator();
            while (it.hasNext()) {
                double t = (now - it.next()) / 1e9 / FLOAT_TIME;
                if (t >= 1) {
                    it.remove();
                    continue;
                }
                float e = easeOutCubic((float) t);
                float alpha = 1f - (float) (t * t);
                float rise = 6 + 16 * e;
                float drift = (float) Math.sin(t * Math.PI * 1.5) * 4;

                m.pushMatrix();
                m.translate(w / 2f + 2 + drift, y - rise);
                float sc = 0.9f + 0.3f * (1 - e);
                m.scale(sc);
                ctx.drawText(tr, "-1", 0, 0, withAlpha(accent, (int) (255 * MathHelper.clamp(alpha, 0, 1))), true);
                m.popMatrix();
            }
        }
    }

    private Text nameText(PlayerEntity player, boolean friend) {
        int color = friend && friendMode.get() == FriendMode.Highlight ? rgb(friendColor.get()) : rgb(accentColor.get());
        return Text.literal(player.getName().getString()).withColor(color);
    }

    private static int rgb(SettingColor c) {
        return c.getPacked() & 0xFFFFFF;
    }

    private static int chatId(PlayerEntity player) {
        return ("vetmo-pops" + player.getUuid()).hashCode();
    }
}
