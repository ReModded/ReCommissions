package dev.remodded.recommission

import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationRegistry
import net.kyori.adventure.util.UTF8ResourceBundleControl
import java.util.*


object CommissionLang {
    private val translations = TranslationRegistry.create(Key.key("recommission", "lang"))

    init {
        GlobalTranslator.translator().addSource(translations)
    }

    fun reload() {
        translations.javaClass.getDeclaredField("translations").apply {
            isAccessible = true
            (get(translations) as MutableMap<*, *>).clear()
        }

        for (lang in setOf(Locale.US, Locale.FRANCE, Locale.of("pl", "PL"))) {
            val bundle = ResourceBundle.getBundle("lang.Bundle", lang, UTF8ResourceBundleControl.get())
            translations.registerAll(lang, bundle, true)
        }
    }
}
