package dev.remodded.recommission

import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.AbstractTranslationStore
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import net.kyori.adventure.util.UTF8ResourceBundleControl
import java.util.*


object CommissionLang {
    private val translations = TranslationStore.messageFormat(Key.key("recommission", "lang"))

    init {
        GlobalTranslator.translator().addSource(translations)
    }

    fun reload() {
        AbstractTranslationStore::class.java.getDeclaredField("translations").apply {
            isAccessible = true
            (get(translations) as MutableMap<*, *>).clear()
        }

        for (lang in setOf(Locale.US, Locale.FRENCH, Locale.of("pl", "PL"))) {
            val bundle = ResourceBundle.getBundle("lang.Bundle", lang, UTF8ResourceBundleControl.utf8ResourceBundleControl())
            translations.registerAll(lang, bundle, true)
        }
    }
}
