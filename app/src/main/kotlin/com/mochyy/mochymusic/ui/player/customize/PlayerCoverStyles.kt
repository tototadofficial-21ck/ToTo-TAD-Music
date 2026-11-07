// PlayerCoverStyles.kt

package com.mochyy.mochymusic.ui.player.customize

import com.mochyy.mochymusic.i18n.UiText

import androidx.annotation.DrawableRes
import androidx.datastore.preferences.core.Preferences
import com.mochyy.mochymusic.R
import com.mochyy.mochymusic.constants.CanvasThumbnailAnimationKey
import com.mochyy.mochymusic.constants.DiscNameKey
import com.mochyy.mochymusic.constants.DiscRealisticModeKey
import com.mochyy.mochymusic.constants.DiscRotatingAnimationKey
import com.mochyy.mochymusic.constants.PlayVideoBackgroundKey
import com.mochyy.mochymusic.constants.PlayerCoverStyle
import com.mochyy.mochymusic.constants.YtVideoBackgroundLyricsSyncKey

val PlayerCoverStyle.isDisc: Boolean
    get() = when (this) {
        PlayerCoverStyle.EDGE_TO_EDGE, PlayerCoverStyle.SQUARED -> false
        else -> true
    }

val PlayerCoverStyle.usesBoxedStage: Boolean
    get() = when (this) {
        PlayerCoverStyle.EDGE_TO_EDGE,
        PlayerCoverStyle.DISC_BIG_FULL,
        PlayerCoverStyle.DISC_BIG_LABEL,
        PlayerCoverStyle.TURNTABLE,
        -> false
        else -> true
    }

val PlayerCoverStyle.isBigDisc: Boolean
    get() = this == PlayerCoverStyle.DISC_BIG_FULL || this == PlayerCoverStyle.DISC_BIG_LABEL

val PlayerCoverStyle.hasTonearm: Boolean
    get() = this == PlayerCoverStyle.DISC_BIG_LABEL

val PlayerCoverStyle.displayName: String
    get() = when (this) {
        PlayerCoverStyle.EDGE_TO_EDGE -> "Full edge to edge"
        PlayerCoverStyle.SQUARED -> "Squared"
        PlayerCoverStyle.DISC_SMALL_FULL -> "Disc Music - inside - small"
        PlayerCoverStyle.DISC_SMALL_LABEL -> "Disc Music - label - small"
        PlayerCoverStyle.DISC_BIG_FULL -> "Disc Music - inside - big"
        PlayerCoverStyle.DISC_BIG_LABEL -> "Disc Music - label - big"
        PlayerCoverStyle.DISC_ALBUM -> "Disc + album Music"
        PlayerCoverStyle.TURNTABLE -> "Turntable"
    }

sealed interface CoverOption {
    val title: String
    val description: String
    @get:DrawableRes val icon: Int

    data class Switch(
        override val title: String,
        override val description: String,
        @param:DrawableRes override val icon: Int,
        val key: Preferences.Key<Boolean>,
        val default: Boolean,
        val inverted: Boolean = false,
        val parent: Switch? = null,
    ) : CoverOption

    data class Text(
        override val title: String,
        override val description: String,
        @param:DrawableRes override val icon: Int,
        val key: Preferences.Key<String>,
        val placeholder: String,
    ) : CoverOption
}

private val AnimatedCanvasOption = CoverOption.Switch(
    title = UiText.get(R.string.toto_ui_more_animated_canvas_2d8362),
    description = UiText.get(R.string.toto_ui_more_animated_canvas_not_from_youtube_4dc757),
    icon = R.drawable.sparks,
    key = CanvasThumbnailAnimationKey,
    default = true,
)

private val YtVideoOption = CoverOption.Switch(
    title = UiText.get(R.string.toto_ui_more_yt_video_background_03403e),
    description = UiText.get(R.string.toto_ui_more_plays_yt_videos_on_canvas_fa733d),
    icon = R.drawable.slow_motion_video,
    key = PlayVideoBackgroundKey,
    default = false,
)

private val TimestampMatchingOption = CoverOption.Switch(
    title = UiText.get(R.string.appearance_timestamp_matching),
    description = UiText.get(R.string.appearance_timestamp_matching_desc),
    icon = R.drawable.lyrics,
    key = YtVideoBackgroundLyricsSyncKey,
    default = true,
    parent = YtVideoOption,
)

private val RotatingAnimationOption = CoverOption.Switch(
    title = UiText.get(R.string.toto_ui_more_rotating_animation_13b416),
    description = UiText.get(R.string.toto_ui_more_for_the_disc_7176a9),
    icon = R.drawable.refresh,
    key = DiscRotatingAnimationKey,
    default = true,
)

private val RealisticModeOption = CoverOption.Switch(
    title = UiText.get(R.string.toto_ui_more_realistic_mode_2afb99),
    description = UiText.get(R.string.toto_ui_more_for_the_disc_7176a9),
    icon = R.drawable.palette,
    key = DiscRealisticModeKey,
    default = true,
)

private val DiscNameOption = CoverOption.Text(
    title = UiText.get(R.string.toto_ui_more_disc_name_910cf2),
    description = UiText.get(R.string.toto_ui_more_printed_on_the_platter_105f40),
    icon = R.drawable.edit,
    key = DiscNameKey,
    placeholder = UiText.get(R.string.toto_ui_more_leave_empty_to_hide_35a6e1),
)

private val DiscOptions = listOf(RotatingAnimationOption, RealisticModeOption, DiscNameOption)

val PlayerCoverStyle.options: List<CoverOption>
    get() = when (this) {
        PlayerCoverStyle.SQUARED -> listOf(AnimatedCanvasOption)
        PlayerCoverStyle.EDGE_TO_EDGE -> listOf(
            YtVideoOption,
            TimestampMatchingOption,
            AnimatedCanvasOption,
        )
        else -> DiscOptions
    }
