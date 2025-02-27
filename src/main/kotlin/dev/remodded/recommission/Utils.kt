package dev.remodded.recommission


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
