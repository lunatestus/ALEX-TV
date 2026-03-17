@file:OptIn(ExperimentalComposeUiApi::class)

package com.alex.tv.ui.screens

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.source.TrackGroupArray
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.core.content.res.ResourcesCompat
import com.alex.tv.R
import com.alex.tv.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    streamUrl: String,
    title: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            parameters = buildUponParameters()
                .setTunnelingEnabled(false)
                .build()
        }
    }
    val exoPlayer = remember {
        val loadControl = DefaultLoadControl.Builder()
            .setBackBuffer(20_000, false)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var playbackState by remember { mutableIntStateOf(Player.STATE_IDLE) }
    var isBuffering by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }
    var retryToken by remember { mutableLongStateOf(0L) }
    var pendingAudioFallbackParams by remember { mutableStateOf<DefaultTrackSelector.Parameters?>(null) }
    var pendingAudioFallbackLabel by remember { mutableStateOf<String?>(null) }
    var showCaptionMenu by remember { mutableStateOf(false) }
    var showAudioMenu by remember { mutableStateOf(false) }
    val isMenuOpen = showCaptionMenu || showAudioMenu
    val scope = rememberCoroutineScope()
    
    val seekbarFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val captionFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val settingsFocusRequester = remember { FocusRequester() }
    val screenFocusRequester = remember { FocusRequester() }
    var resumeOnStart by remember { mutableStateOf(false) }

    BackHandler(enabled = isMenuOpen) {
        showCaptionMenu = false
        showAudioMenu = false
        showControls = true
        lastInteraction = System.currentTimeMillis()
        playPauseFocusRequester.requestFocus()
    }

    // Listen to exoPlayer play state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingState: Boolean) {
                isPlaying = isPlayingState
            }

            override fun onPlayerError(error: PlaybackException) {
                val isAudioDecodeError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED,
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
                    PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> true
                    else -> false
                }
                val fallbackParams = pendingAudioFallbackParams
                if (isAudioDecodeError && fallbackParams != null) {
                    trackSelector.parameters = fallbackParams
                    pendingAudioFallbackParams = null
                    val label = pendingAudioFallbackLabel
                    pendingAudioFallbackLabel = null
                    playbackError = if (!label.isNullOrBlank()) {
                        "Audio track \"$label\" isn't supported on this device. Reverted to previous."
                    } else {
                        "Selected audio track isn't supported on this device. Reverted to previous."
                    }
                    isBuffering = false
                    val shouldResume = exoPlayer.playWhenReady
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = shouldResume
                    retryCount = 0
                    return
                }
                playbackError = error.message ?: "Playback error"
                isBuffering = false
                if (retryCount < 2) {
                    retryCount += 1
                    scope.launch {
                        delay(800L * retryCount)
                        retryToken = System.currentTimeMillis()
                    }
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY && playbackError != null) {
                    playbackError = null
                }
                if (state == Player.STATE_READY) {
                    pendingAudioFallbackParams = null
                    pendingAudioFallbackLabel = null
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    resumeOnStart = exoPlayer.isPlaying
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeOnStart && playbackError == null) {
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(streamUrl, retryToken) {
        playbackError = null
        val shouldResume = if (retryToken != 0L) exoPlayer.playWhenReady else true
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        if (!streamUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = shouldResume
        } else {
            playbackError = "Stream unavailable"
        }
    }

    LaunchedEffect(streamUrl) {
        // Reset track overrides when loading a new item to avoid stale audio selections.
        resetTrackOverrides(trackSelector)
        retryCount = 0
        retryToken = 0L
        pendingAudioFallbackParams = null
        pendingAudioFallbackLabel = null
        showCaptionMenu = false
        showAudioMenu = false
    }

    LaunchedEffect(Unit) {
        screenFocusRequester.requestFocus()
    }

    LaunchedEffect(lastInteraction, showControls, isMenuOpen) {
        if (showControls && !isMenuOpen) {
            delay(3000)
            if (System.currentTimeMillis() - lastInteraction >= 3000) {
                showControls = false
                screenFocusRequester.requestFocus() // Steal focus so hidden controls don't keep it
            }
        }
    }

    LaunchedEffect(showControls, isMenuOpen) {
        if (showControls && !isMenuOpen) {
            playPauseFocusRequester.requestFocus() // Return focus to controls when shown
        }
    }

    // Wrap AndroidView in a focusable box to capture D-Pad events when controls are hidden
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(screenFocusRequester)
            .focusable(!isMenuOpen)
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    if (isMenuOpen) {
                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                            showCaptionMenu = false
                            showAudioMenu = false
                            showControls = true
                            playPauseFocusRequester.requestFocus()
                        }
                        return@onKeyEvent true
                    }
                    when (keyCode) {
                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                            if (isMenuOpen) {
                                showCaptionMenu = false
                                showAudioMenu = false
                                showControls = true
                                playPauseFocusRequester.requestFocus()
                                true
                            } else if (showControls) {
                                showControls = false
                                screenFocusRequester.requestFocus()
                                true
                            } else {
                                onClose()
                                true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                        KeyEvent.KEYCODE_SPACE -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                showControls = true
                            }
                            if (playbackError == null) {
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                val offset = if (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -10000L else 10000L
                                if (playbackError == null) {
                                    val rawDuration = exoPlayer.duration
                                    val safeDuration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else Long.MAX_VALUE
                                    val next = (exoPlayer.currentPosition + offset).coerceAtLeast(0L).coerceAtMost(safeDuration)
                                    exoPlayer.seekTo(next)
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> {
                            lastInteraction = System.currentTimeMillis()
                            if (!showControls) {
                                showControls = true
                                true
                            } else {
                                false
                            }
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = {
                val subtitleTypeface = ResourcesCompat.getFont(it, R.font.dm_sans_regular)
                PlayerView(it).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = isPlaying
                    isFocusable = false
                    isFocusableInTouchMode = false
                    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    subtitleView?.apply {
                        setStyle(
                            CaptionStyleCompat(
                                AndroidColor.WHITE,
                                AndroidColor.BLACK,
                                AndroidColor.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_NONE,
                                AndroidColor.BLACK,
                                subtitleTypeface
                            )
                        )
                    }
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.keepScreenOn = isPlaying
            },
            modifier = Modifier.fillMaxSize()
        )

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = playbackError ?: "Playback error",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = DmSans
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Press Back to exit",
                        color = Color(0xB3FFFFFF),
                        fontSize = 12.sp,
                        fontFamily = DmSans
                    )
                }
            }
        }

        // Use alpha instead of AnimatedVisibility so the UI tree doesn't change, 
        // which prevents focus from dropping when controls hide.
        val controlsAlpha by animateFloatAsState(
            targetValue = if (showControls) 1f else 0f, 
            animationSpec = tween(300)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(controlsAlpha)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)), startY = 0.6f))
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
                PlayerSeekBar(
                    exoPlayer = exoPlayer,
                    onInteraction = { lastInteraction = System.currentTimeMillis() },
                    showControls = showControls,
                    controlsEnabled = showControls && !isMenuOpen,
                    focusRequester = seekbarFocusRequester,
                    downRequester = playPauseFocusRequester
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(
                            icon = if (isPlaying) PlayerIcons.Pause else PlayerIcons.Play,
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                if (isPlaying) exoPlayer.pause() else exoPlayer.play() 
                            },
                            isPrimary = true,
                            modifier = Modifier.focusRequester(playPauseFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                right = rewindFocusRequester
                                left = FocusRequester.Cancel
                            },
                            enabled = showControls && !isMenuOpen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = PlayerIcons.Rewind,
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                val next = (exoPlayer.currentPosition - 10000).coerceAtLeast(0L)
                                exoPlayer.seekTo(next)
                            },
                            modifier = Modifier.focusRequester(rewindFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = playPauseFocusRequester
                                right = forwardFocusRequester
                            },
                            enabled = showControls && !isMenuOpen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = PlayerIcons.Forward,
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                val rawDuration = exoPlayer.duration
                                val safeDuration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else Long.MAX_VALUE
                                val next = (exoPlayer.currentPosition + 10000).coerceAtMost(safeDuration)
                                exoPlayer.seekTo(next)
                            },
                            modifier = Modifier.focusRequester(forwardFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = rewindFocusRequester
                                right = captionFocusRequester
                            },
                            enabled = showControls && !isMenuOpen
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlayerButton(
                            icon = painterResource(id = R.drawable.ic_caption),
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                showCaptionMenu = true
                                showAudioMenu = false
                                showControls = true
                            },
                            modifier = Modifier.focusRequester(captionFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = forwardFocusRequester
                                right = audioFocusRequester
                            },
                            enabled = showControls && !isMenuOpen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = painterResource(id = R.drawable.ic_audio),
                            onClick = { 
                                lastInteraction = System.currentTimeMillis()
                                showAudioMenu = true
                                showCaptionMenu = false
                                showControls = true
                            },
                            modifier = Modifier.focusRequester(audioFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = captionFocusRequester
                                right = settingsFocusRequester
                            },
                            enabled = showControls && !isMenuOpen
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        PlayerButton(
                            icon = PlayerIcons.Settings, 
                            onClick = { lastInteraction = System.currentTimeMillis() },
                            modifier = Modifier.focusRequester(settingsFocusRequester),
                            focusProps = {
                                up = seekbarFocusRequester
                                left = audioFocusRequester
                                right = FocusRequester.Cancel
                            },
                            enabled = showControls && !isMenuOpen
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xD9000000), Color.Transparent)
                    )
                )
                .alpha(controlsAlpha)
        )
        Text(
            text = title,
            color = TextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = DmSans,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxWidth()
                .alpha(controlsAlpha)
        )

        if (isMenuOpen) {
            val menuType = if (showCaptionMenu) C.TRACK_TYPE_TEXT else C.TRACK_TYPE_AUDIO
            val menuTitle = if (showCaptionMenu) "Subtitles" else "Audio"
            TrackSelectionMenu(
                title = menuTitle,
                exoPlayer = exoPlayer,
                trackSelector = trackSelector,
                trackType = menuType,
                onAudioSelectionAttempt = { previousParams, option ->
                    pendingAudioFallbackParams = previousParams
                    pendingAudioFallbackLabel = option.label
                },
                onDismiss = {
                    showCaptionMenu = false
                    showAudioMenu = false
                    lastInteraction = System.currentTimeMillis()
                    playPauseFocusRequester.requestFocus()
                }
            )
        }
    }
}

// Extract seekbar to scope state reads and prevent whole-screen recomposition
@Composable
fun PlayerSeekBar(
    exoPlayer: ExoPlayer, 
    onInteraction: () -> Unit,
    showControls: Boolean,
    controlsEnabled: Boolean,
    focusRequester: FocusRequester,
    downRequester: FocusRequester
) {
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var lastSeekTime by remember { mutableLongStateOf(0L) }
    var isProgressFocused by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var seekJob by remember { mutableStateOf<Job?>(null) }
    var seekDirection by remember { mutableIntStateOf(0) }
    var seekStartTime by remember { mutableLongStateOf(0L) }
    var didSeekDuringHold by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            seekJob?.cancel()
            seekJob = null
        }
    }

    LaunchedEffect(exoPlayer, showControls) {
        while (true) {
            if (!showControls) {
                delay(500)
                continue
            }
            // Don't update from player if user is actively seeking (debounce)
            if (System.currentTimeMillis() - lastSeekTime > 500) {
                currentPosition = exoPlayer.currentPosition
            }
            val rawDuration = exoPlayer.duration
            duration = if (rawDuration > 0 && rawDuration != C.TIME_UNSET) rawDuration else 0L
            delay(250) // Reduced refresh rate to save CPU
        }
    }

    val isDurationKnown = duration > 0L
    val isSeekable = isDurationKnown && exoPlayer.isCurrentMediaItemSeekable

    val barHeight by animateDpAsState(targetValue = if (isProgressFocused) 8.dp else 3.dp)
    val dotSize by animateDpAsState(targetValue = if (isProgressFocused) 14.dp else 0.dp)
    
    val progress = if (isDurationKnown) currentPosition.toFloat() / duration.toFloat() else 0f
    // Don't animate the progress bar filling, as it fights with the user seeking
    val animatedProgress = progress.coerceIn(0f, 1f)
    val containerHeight = 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .focusProperties { down = downRequester }
            .onFocusChanged { 
                if (isProgressFocused != it.isFocused) {
                    isProgressFocused = it.isFocused
                }
                if (it.isFocused) onInteraction()
            }
            .onKeyEvent { keyEvent ->
                onInteraction()

                val isLeft = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                val isRight = keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                if (!isSeekable) return@onKeyEvent false

                if (isLeft || isRight) {
                    if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        lastSeekTime = System.currentTimeMillis()
                        val newDirection = if (isLeft) -1 else 1
                        if (seekDirection != newDirection) {
                            seekStartTime = System.currentTimeMillis()
                        } else if (seekStartTime == 0L) {
                            seekStartTime = System.currentTimeMillis()
                        }
                        seekDirection = newDirection
                        didSeekDuringHold = false
                        if (seekJob == null) {
                            seekJob = scope.launch {
                                // Wait briefly before treating as a long-press.
                                delay(150)
                                while (true) {
                                    val elapsed = (System.currentTimeMillis() - seekStartTime).coerceAtLeast(0L)
                                    // Smooth acceleration: faster the longer the hold, capped.
                                    val accelSteps = (elapsed / 200L).coerceAtMost(14L)
                                    val baseVelocity = 120_000L // ms per second
                                    val velocity = baseVelocity + accelSteps * 80_000L
                                    val tickMs = 40L
                                    val step = (velocity * tickMs / 1000L) * seekDirection
                                    val next = (currentPosition + step).coerceAtLeast(0L)
                                    currentPosition = next.coerceAtMost(duration)
                                    didSeekDuringHold = true
                                    lastSeekTime = System.currentTimeMillis()
                                    onInteraction()
                                    delay(tickMs)
                                }
                            }
                        }
                        true
                    } else if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        lastSeekTime = System.currentTimeMillis()
                        seekStartTime = 0L
                        seekJob?.cancel()
                        seekJob = null
                        if (!didSeekDuringHold) {
                            val step = 10_000L * seekDirection
                            val next = (currentPosition + step).coerceAtLeast(0L).coerceAtMost(duration)
                            currentPosition = next
                        }
                        // Commit the final position to ExoPlayer once user releases the button.
                        exoPlayer.seekTo(currentPosition)
                        true
                    } else {
                        false
                    }
                } else false
            }
            // Only focusable if controls are shown
            .focusable(controlsEnabled)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (isDurationKnown) formatTime(currentPosition) else "--:--",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                fontFamily = DmSans,
                maxLines = 1,
                modifier = Modifier.width(56.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .height(containerHeight)
                    .clipToBounds(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(CircleShape)
                        .background(ProgressTrack)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(ProgressFill)
                    )
                }
                val maxOffset = (maxWidth - dotSize).coerceAtLeast(0.dp)
                val dotOffset = (maxWidth * animatedProgress - dotSize / 2).coerceIn(0.dp, maxOffset)
                Box(
                    modifier = Modifier
                        .offset(x = dotOffset)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                formatTimeOrUnknown(duration),
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                fontFamily = DmSans,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.width(56.dp)
            )
        }
    }
}

@Composable
fun PlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusProps: (androidx.compose.ui.focus.FocusProperties.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 44.dp else 40.dp
    val focusedScale = if (isPrimary) 1.0f else 1.1f
    val scale by animateFloatAsState(targetValue = if (isFocused) focusedScale else 1f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .focusProperties { focusProps?.invoke(this) }
            .onFocusChanged {
                if (isFocused != it.isFocused) {
                    isFocused = it.isFocused
                }
            }
            .focusable(enabled)
            .onKeyEvent { keyEvent ->
                if (!enabled) return@onKeyEvent false
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_SPACE -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(enabled = enabled) { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(if (isPrimary) 28.dp else 20.dp)
        )
    }
}

@Composable
fun PlayerButton(
    icon: androidx.compose.ui.graphics.painter.Painter,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusProps: (androidx.compose.ui.focus.FocusProperties.() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val size = if (isPrimary) 44.dp else 40.dp
    val focusedScale = if (isPrimary) 1.0f else 1.1f
    val scale by animateFloatAsState(targetValue = if (isFocused) focusedScale else 1f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .focusProperties { focusProps?.invoke(this) }
            .onFocusChanged {
                if (isFocused != it.isFocused) {
                    isFocused = it.isFocused
                }
            }
            .focusable(enabled)
            .onKeyEvent { keyEvent ->
                if (!enabled) return@onKeyEvent false
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_SPACE -> {
                            onClick()
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable(enabled = enabled) { onClick() }
            .background(if (isFocused) Color.White else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = if (isFocused) Color.Black else Color.White,
            modifier = Modifier.size(if (isPrimary) 28.dp else 20.dp)
        )
    }
}

private data class TrackOption(
    val label: String,
    val group: Tracks.Group?,
    val trackIndex: Int?,
    val groupIndex: Int?,
    val isSelected: Boolean,
    val isSupported: Boolean,
    val isOff: Boolean = false,
    val isAuto: Boolean = false
)

@Suppress("DEPRECATION")
private fun resetTrackOverrides(trackSelector: DefaultTrackSelector) {
    trackSelector.parameters = trackSelector.parameters.buildUpon()
        .clearSelectionOverrides()
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        .build()
}

@Composable
@Suppress("DEPRECATION")
private fun TrackSelectionMenu(
    title: String,
    exoPlayer: ExoPlayer,
    trackSelector: DefaultTrackSelector,
    trackType: Int,
    onAudioSelectionAttempt: (DefaultTrackSelector.Parameters, TrackOption) -> Unit,
    onDismiss: () -> Unit
) {
    val tracks = exoPlayer.currentTracks
    val mapped = trackSelector.currentMappedTrackInfo
    val rendererIndex = mapped?.run {
        (0 until rendererCount).firstOrNull { getRendererType(it) == trackType }
    }
    val isTypeDisabled = rendererIndex?.let { trackSelector.parameters.getRendererDisabled(it) } ?: false
    val trackGroups = if (rendererIndex != null) mapped.getTrackGroups(rendererIndex) else null
    val selectionOverride = remember(rendererIndex, trackGroups, trackSelector.parameters) {
        if (rendererIndex != null && trackGroups != null) {
            trackSelector.parameters.getSelectionOverride(rendererIndex, trackGroups)
        } else null
    }
    val hasExplicitOverride = selectionOverride != null
    val options = remember(tracks, trackType, isTypeDisabled, hasExplicitOverride, trackGroups) {
        val built = mutableListOf<TrackOption>()
        val autoSelected = !isTypeDisabled && !hasExplicitOverride
        built.add(
            TrackOption(
                label = "Auto",
                group = null,
                trackIndex = null,
                groupIndex = null,
                isSelected = autoSelected,
                isSupported = true,
                isAuto = true
            )
        )
        if (trackType == C.TRACK_TYPE_TEXT || trackType == C.TRACK_TYPE_AUDIO) {
            built.add(
                TrackOption(
                    label = "Off",
                    group = null,
                    trackIndex = null,
                    groupIndex = null,
                    isSelected = isTypeDisabled,
                    isSupported = true,
                    isOff = true
                )
            )
        }
        tracks.groups.forEach { group ->
            if (group.type != trackType) return@forEach
            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(i)
                val label = format.label ?: format.language ?: "Track ${i + 1}"
                val groupIndex = trackGroups?.let { findGroupIndex(it, trackGroup) }
                val isSupported = group.isTrackSupported(i) && groupIndex != null
                built.add(
                    TrackOption(
                        label = label,
                        group = group,
                        trackIndex = i,
                        groupIndex = groupIndex,
                        isSelected = group.isTrackSelected(i) && !isTypeDisabled,
                        isSupported = isSupported
                    )
                )
            }
        }
        built
    }

    val focusRequesters = remember(options.size) { List(options.size) { FocusRequester() } }
    val selectedIndex = options.indexOfFirst { it.isSelected }
    val firstEnabledIndex = options.indexOfFirst { it.isSupported }
    val initialFocusIndex = when {
        selectedIndex >= 0 && options.getOrNull(selectedIndex)?.isSupported == true -> selectedIndex
        firstEnabledIndex >= 0 -> firstEnabledIndex
        else -> 0
    }
    val hasRealTracks = options.any { !it.isOff && !it.isAuto && it.group != null }

    LaunchedEffect(options.size, initialFocusIndex) {
        if (options.isNotEmpty() && initialFocusIndex in options.indices) {
            // Wait a frame so LazyColumn items are placed before requesting focus.
            withFrameNanos { }
            focusRequesters[initialFocusIndex].requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(min = 240.dp, max = 320.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF101010))
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* consume */ }
        ) {
            Text(
                text = title,
                color = TextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = DmSans
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (!hasRealTracks) {
                Text(
                    text = "No tracks available",
                    color = Color(0xB3FFFFFF),
                    fontSize = 12.sp,
                    fontFamily = DmSans
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.heightIn(max = 260.dp)
            ) {
                itemsIndexed(options) { index, option ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = option.isSelected
                    val isEnabled = option.isSupported
                    val rowBg = when {
                        isFocused -> Color.White
                        isSelected -> Color(0x1AFFFFFF)
                        else -> Color.Transparent
                    }
                    val textColor = when {
                        !isEnabled -> Color(0x66FFFFFF)
                        isFocused -> Color.Black
                        else -> Color.White
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequesters[index])
                            .focusProperties {
                                if (index == 0) up = FocusRequester.Cancel
                                if (index == options.lastIndex) down = FocusRequester.Cancel
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            }
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusable(isEnabled)
                            .onKeyEvent { keyEvent ->
                                if (!isEnabled) return@onKeyEvent false
                                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (keyEvent.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_CENTER,
                                        KeyEvent.KEYCODE_ENTER,
                                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                            if (trackType == C.TRACK_TYPE_AUDIO) {
                                                onAudioSelectionAttempt(trackSelector.parameters, option)
                                            }
                                            applyTrackSelection(trackSelector, trackType, rendererIndex, trackGroups, option)
                                            onDismiss()
                                            true
                                        }
                                        KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                                            onDismiss()
                                            true
                                        }
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .clickable(enabled = isEnabled) {
                                if (trackType == C.TRACK_TYPE_AUDIO) {
                                    onAudioSelectionAttempt(trackSelector.parameters, option)
                                }
                                applyTrackSelection(trackSelector, trackType, rendererIndex, trackGroups, option)
                                onDismiss()
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(rowBg)
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option.label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontFamily = DmSans,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_circle_check),
                                contentDescription = null,
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (index != options.lastIndex) {
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun applyTrackSelection(
    trackSelector: DefaultTrackSelector,
    trackType: Int,
    rendererIndex: Int?,
    trackGroups: TrackGroupArray?,
    option: TrackOption
) {
    if (!option.isSupported) return
    val rIdx = rendererIndex ?: return
    val groups = trackGroups ?: return
    if (groups.length == 0) return

    val paramsBuilder = trackSelector.parameters.buildUpon()
        .setTrackTypeDisabled(trackType, option.isOff)
        .clearSelectionOverrides(rIdx)

    if (option.isAuto) {
        // Auto = no override, renderer enabled
        paramsBuilder.setTrackTypeDisabled(trackType, false)
    } else if (!option.isOff && option.groupIndex != null && option.trackIndex != null) {
        val groupIndex = option.groupIndex
        val trackIndex = option.trackIndex
        if (groupIndex !in 0 until groups.length) return
        val group = groups[groupIndex]
        if (trackIndex !in 0 until group.length) return
        paramsBuilder.setSelectionOverride(
            rIdx,
            groups,
            DefaultTrackSelector.SelectionOverride(groupIndex, trackIndex)
        )
    }

    trackSelector.parameters = paramsBuilder.build()
}

private fun findGroupIndex(trackGroups: TrackGroupArray, target: TrackGroup): Int? {
    for (i in 0 until trackGroups.length) {
        val group = trackGroups[i]
        if (group === target) return i
        if (group.length != target.length) continue
        var allMatch = true
        for (t in 0 until group.length) {
            if (!formatsSimilar(group.getFormat(t), target.getFormat(t))) {
                allMatch = false
                break
            }
        }
        if (allMatch) return i
    }
    return null
}

private fun formatsSimilar(a: Format, b: Format): Boolean {
    if (a.id != b.id) return false
    if (a.sampleMimeType != b.sampleMimeType) return false
    if (a.language != b.language) return false
    if (a.label != b.label) return false
    if (a.bitrate != b.bitrate) return false
    return true
}

fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatTimeOrUnknown(milliseconds: Long): String {
    if (milliseconds <= 0L) return "--:--"
    return formatTime(milliseconds)
}
