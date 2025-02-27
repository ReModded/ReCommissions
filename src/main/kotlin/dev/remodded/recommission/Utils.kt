@file:Suppress("unused")

package dev.remodded.recommission

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.GlobalTranslator
import java.util.Locale


/**
 * Removes all elements from this collection that satisfy the given predicate.
 *
 * @param action the predicate used to filter the elements
 * @return True
 */
inline fun <T> MutableIterable<T>.foreachConsume(action: (T) -> Boolean): Boolean {
    var removed = false
    val iterator = iterator()
    while (iterator.hasNext())
        if (action(iterator.next())) {
            iterator.remove()
            removed = true
        }

    return removed
}

fun Component.translate(locale: Locale): Component {
    if (this is TranslatableComponent)
        return GlobalTranslator.render(this, locale)
    return this
}
