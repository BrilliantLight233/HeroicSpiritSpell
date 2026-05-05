package com.lin.heroic_spirit_spell.command;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Map;

/**
 * ????????? {@link MagicData#getPlayerCooldowns()} ?? {@link SpellSelectionManager} ??????????
 * ????????????????? {@link PlayerCooldowns#syncToPlayer(ServerPlayer)} ???????? UI??
 */
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class SpellCooldownCommand {

    private static final SimpleCommandExceptionType BAD_SLOT =
            new SimpleCommandExceptionType(Component.translatable("command.heroic_spirit_spell.spellcooldown.error.bad_slot"));
    private static final SimpleCommandExceptionType EMPTY_SLOT =
            new SimpleCommandExceptionType(Component.translatable("command.heroic_spirit_spell.spellcooldown.error.empty_slot"));

    private SpellCooldownCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spellcooldown")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(SpellCooldownCommand::executeGet))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(SpellCooldownCommand::executeSet)))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(SpellCooldownCommand::executeAdd)))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(SpellCooldownCommand::executeRemove)))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("slot", IntegerArgumentType.integer(0))
                                                .executes(SpellCooldownCommand::executeReset)))));
    }

    private static int executeGet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        String spellId = resolveSpellIdOrThrow(target, slot);
        int ticks = getRemainingTicks(MagicData.getPlayerMagicData(target).getPlayerCooldowns(), spellId);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.heroic_spirit_spell.spellcooldown.get.success",
                        target.getDisplayName(),
                        slot,
                        ticks),
                false);
        return ticks;
    }

    private static int executeSet(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int value = IntegerArgumentType.getInteger(ctx, "value");
        String spellId = resolveSpellIdOrThrow(target, slot);
        PlayerCooldowns cds = MagicData.getPlayerMagicData(target).getPlayerCooldowns();
        if (value == 0) {
            cds.removeCooldown(spellId);
        } else {
            cds.addCooldown(spellId, Math.max(value, 1), value);
        }
        cds.syncToPlayer(target);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.heroic_spirit_spell.spellcooldown.set.success",
                        target.getDisplayName(),
                        slot,
                        value),
                true);
        return value;
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int delta = IntegerArgumentType.getInteger(ctx, "value");
        String spellId = resolveSpellIdOrThrow(target, slot);
        PlayerCooldowns cds = MagicData.getPlayerMagicData(target).getPlayerCooldowns();
        int current = getRemainingTicks(cds, spellId);
        int next = current + delta;
        if (next <= 0) {
            cds.removeCooldown(spellId);
        } else {
            int maxBar = next;
            CooldownInstance old = getInstance(cds, spellId);
            if (old != null) {
                maxBar = Math.max(old.getSpellCooldown(), next);
            }
            cds.addCooldown(spellId, Math.max(maxBar, 1), next);
        }
        cds.syncToPlayer(target);
        int after = getRemainingTicks(cds, spellId);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.heroic_spirit_spell.spellcooldown.add.success",
                        target.getDisplayName(),
                        slot,
                        delta,
                        after),
                true);
        return after;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        int delta = IntegerArgumentType.getInteger(ctx, "value");
        String spellId = resolveSpellIdOrThrow(target, slot);
        PlayerCooldowns cds = MagicData.getPlayerMagicData(target).getPlayerCooldowns();
        CooldownInstance old = getInstance(cds, spellId);
        if (old == null) {
            ctx.getSource().sendSuccess(
                    () -> Component.translatable("command.heroic_spirit_spell.spellcooldown.remove.noop"),
                    false);
            return 0;
        }
        int next = Math.max(0, old.getCooldownRemaining() - delta);
        if (next <= 0) {
            cds.removeCooldown(spellId);
        } else {
            int maxBar = Math.max(old.getSpellCooldown(), next);
            cds.addCooldown(spellId, Math.max(maxBar, 1), next);
        }
        cds.syncToPlayer(target);
        ctx.getSource().sendSuccess(
                () -> Component.translatable(
                        "command.heroic_spirit_spell.spellcooldown.remove.success",
                        target.getDisplayName(),
                        slot,
                        getRemainingTicks(cds, spellId)),
                true);
        return next;
    }

    private static int executeReset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        String spellId = resolveSpellIdOrThrow(target, slot);
        PlayerCooldowns cds = MagicData.getPlayerMagicData(target).getPlayerCooldowns();
        boolean had = cds.removeCooldown(spellId);
        if (had) {
            cds.syncToPlayer(target);
        }
        ctx.getSource().sendSuccess(
                () -> had
                        ? Component.translatable(
                        "command.heroic_spirit_spell.spellcooldown.reset.success",
                        target.getDisplayName(),
                        slot)
                        : Component.translatable("command.heroic_spirit_spell.spellcooldown.reset.noop"),
                true);
        return had ? 1 : 0;
    }

    private static String resolveSpellIdOrThrow(ServerPlayer player, int slot) throws CommandSyntaxException {
        var manager = new SpellSelectionManager(player);
        SpellSelectionManager.SelectionOption option = manager.getSpellSlot(slot);
        if (option == null) {
            throw BAD_SLOT.create();
        }
        if (option.spellData == SpellData.EMPTY) {
            throw EMPTY_SLOT.create();
        }
        return option.spellData.getSpell().getSpellId();
    }

    @SuppressWarnings("unchecked")
    private static CooldownInstance getInstance(PlayerCooldowns cds, String spellId) {
        Map<String, CooldownInstance> map = (Map<String, CooldownInstance>) (Map<?, ?>) cds.getSpellCooldowns();
        return map.get(spellId);
    }

    private static int getRemainingTicks(PlayerCooldowns cds, String spellId) {
        CooldownInstance inst = getInstance(cds, spellId);
        if (inst == null) {
            return 0;
        }
        return Math.max(0, inst.getCooldownRemaining());
    }
}
