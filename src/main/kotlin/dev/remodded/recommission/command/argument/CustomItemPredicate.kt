package dev.remodded.recommission.command.argument

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.CustomArgumentType
import io.papermc.paper.command.brigadier.argument.predicate.ItemStackPredicate
import org.bukkit.inventory.ItemStack

class CustomItemPredicate : CustomArgumentType<CustomItemPredicate.Result, ItemStackPredicate> {
    val argument = ArgumentTypes.itemPredicate()

    override fun parse(reader: StringReader): Result {
        val mark = reader.cursor
        val predicate = argument.parse(reader)
        return Result(predicate, reader.string.substring(mark, reader.cursor))
    }

    override fun getNativeType(): ArgumentType<ItemStackPredicate> = argument

    data class Result(val predicate: ItemStackPredicate, val predicateString: String) : ItemStackPredicate by predicate {
        fun tryGetSingleItem(): ItemStack? {
            return try {
                ArgumentTypes.itemStack().parse(StringReader(predicateString))
            } catch (_: CommandSyntaxException) {
                null
            }
        }

        companion object {
            fun fromString(predicateString: String): Result {
                val predicate = ArgumentTypes.itemPredicate().parse(StringReader(predicateString))
                return Result(predicate, predicateString)
            }
        }
    }
}
