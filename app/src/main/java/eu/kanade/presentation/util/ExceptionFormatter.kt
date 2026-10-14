package eu.kanade.presentation.util

import android.content.Context
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.MR

context(context: Context)
val Throwable.formattedMessage: String
    get() {
        when (this) {
            is SourceNotInstalledException -> return context.stringResource(MR.strings.loader_not_implemented_error)
        }
        return when (val className = this::class.simpleName) {
            "Exception", "IOException" -> message ?: className
            else -> "$className: $message"
        }
    }
