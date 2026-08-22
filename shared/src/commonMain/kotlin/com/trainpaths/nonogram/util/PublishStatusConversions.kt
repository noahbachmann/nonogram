package com.trainpaths.nonogram.util

import com.trainpaths.nonogram.classes.PublishStatus

/** The database stores the ordinal; Firestore stores the name. */
internal fun PublishStatus.toLong(): Long = ordinal.toLong()

internal fun Long.toPublishStatus(): PublishStatus =
    PublishStatus.entries.getOrElse(toInt()) { PublishStatus.NONE }

/** Unknown or absent names read as [PublishStatus.NONE], so an older writer can never crash a newer reader. */
internal fun String?.toPublishStatus(): PublishStatus =
    PublishStatus.entries.firstOrNull { it.name == this } ?: PublishStatus.NONE
