package dev.remodded.recommission.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.remodded.recommission.Commission
import dev.remodded.recommission.ReCommission
import dev.remodded.recommission.command.argument.CustomItemPredicate
import dev.remodded.recommission.gui.CommissionClaimMenu
import dev.remodded.recommission.gui.CommissionDonateMenu
import io.papermc.paper.adventure.AdventureComponent
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.concurrent.CompletableFuture

@Suppress("UnstableApiUsage")
object CommissionCommand {
    fun register(): LiteralCommandNode<CommandSourceStack> =
        literal("commission")
            .then(
                literal("test")
                    .executes { ctx ->
//                        val config = YamlConfiguration.loadConfiguration(Path("plugins/ReCommission/config.yml").toFile())
//                        val list = config.getList("test")
//                        println(list)
//                        println(list?.size)
//                        println(list?.get(0)?.javaClass)

                        1
                    }
            )
            .then(
                literal("reload")
                    .requires { it.sender.hasPermission("recommission.command.commission.manage") }
                    .executes { ctx ->
                        ctx.source.sender.sendMessage(Component.translatable("commission.command.commission.reloading"))
                        ReCommission.INSTANCE.reload()
                        ctx.source.sender.sendMessage(Component.translatable("commission.command.commission.reloaded"))
                        1
                    }
            )
            .then(
                literal("create")
                    .requires { it.sender.hasPermission("recommission.command.commission.manage") }
                    .then(
                        argument("commission", StringArgumentType.string())
                            .requires { it.sender.hasPermission("recommission.command.commission.manage") }
                            .executes(::createCommission)
                    )
            )
            .then(argument("commission", StringArgumentType.string())
                .suggests(::suggestCommissions)
                .then(literal("add")
                    .requires { it.sender.hasPermission("recommission.command.commission.manage") }
                    .then(argument("item", CustomItemPredicate())
                        .then(argument("amount", IntegerArgumentType.integer(1))
                            .then(argument("display", ArgumentTypes.itemStack())
                                .executes { ctx -> addCommissionItem(ctx, IntegerArgumentType.getInteger(ctx, "amount")) }
                            )
                        )
                        .then(argument("display", ArgumentTypes.itemStack())
                            .executes { ctx -> addCommissionItem(ctx, 1) }
                        )
                    )
                )
                .then(literal("claim")
                    .requires { it.sender.hasPermission("recommission.command.commission.claim") }
                    .executes(::claimCommission)
                )
                .then(literal("open")
                    .executes(::openCommission)
                )
                .executes(::openCommission)
            )
            .build()

    private fun createCommission(ctx: CommandContext<CommandSourceStack>): Int {
        val name = StringArgumentType.getString(ctx, "commission")

        if (ReCommission.INSTANCE.commissions.any { it.name == name }) {
            ctx.source.sender.sendMessage(
                Component.translatable("commission.command.commission.create.exists", NamedTextColor.RED)
                    .arguments(Component.text(name))
            )
            return 0
        }

        val commission = Commission(name, mutableListOf())
        ReCommission.INSTANCE.commissions.add(commission)
        commission.save()

        ctx.source.sender.sendMessage(
            Component.translatable("commission.command.commission.create", NamedTextColor.GREEN)
                .arguments(Component.text(name))
        )
        return Command.SINGLE_SUCCESS
    }

    private fun addCommissionItem(ctx: CommandContext<CommandSourceStack>, count: Int): Int {
        val commission = getCommission(ctx)
        val predicate = ctx.getArgument("item", CustomItemPredicate.Result::class.java)
        val display = ctx.getArgument("display", ItemStack::class.java)

        commission.items.add(Commission.Item(predicate, display, count))
        commission.update()
        commission.save()

        ctx.source.sender.sendMessage(
            Component.translatable("commission.command.commission.add", NamedTextColor.GREEN)
                .arguments(Component.text(commission.name))
        )
        return Command.SINGLE_SUCCESS
    }

    private fun claimCommission(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.executor
        if (player !is Player)
            throw NOT_PLAYER_EXCEPTION.create(player)

        val commission = getCommission(ctx)

        if (!commission.isFulfilled())
            throw SimpleCommandExceptionType(AdventureComponent(
                Component.translatable("commission.command.commission.claim.not_fulfilled")
                    .arguments(Component.text(commission.name))
            )).create()

        CommissionClaimMenu.open(player, commission)

        return 1
    }

    private fun openCommission(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.executor
        if (player !is Player)
            throw NOT_PLAYER_EXCEPTION.create(player)

        val commission = getCommission(ctx)

        CommissionDonateMenu.open(player, commission)

        return Command.SINGLE_SUCCESS
    }


    private val NOT_PLAYER_EXCEPTION = DynamicCommandExceptionType { target -> AdventureComponent(
        Component.translatable("commission.command.commission.error.not_player")
            .arguments(Component.text(target as String))
    )}
    private val UNKNOWN_COMMISSION_EXCEPTION = DynamicCommandExceptionType { commissionName -> AdventureComponent(
        Component.translatable("commission.command.commission.error.unknown")
            .arguments(Component.text(commissionName as String))
    )}

    private fun getCommission(ctx: CommandContext<CommandSourceStack>): Commission {
        val commissionName = StringArgumentType.getString(ctx, "commission")
        return ReCommission.INSTANCE.commissions.find { it.name == commissionName } ?:
            throw UNKNOWN_COMMISSION_EXCEPTION.create(commissionName)
    }

    private fun suggestCommissions(ctx: CommandContext<CommandSourceStack>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        for (commission in ReCommission.Companion.INSTANCE.commissions) {
            builder.suggest(commission.name)
        }
        return builder.buildFuture()
    }
}
