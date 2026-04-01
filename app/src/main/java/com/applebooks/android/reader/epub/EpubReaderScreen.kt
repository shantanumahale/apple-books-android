package com.applebooks.android.reader.epub

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReaderFont
import com.applebooks.android.domain.model.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpubReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: EpubReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, view)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            WindowCompat.setDecorFitsSystemWindows(window, true)
            viewModel.saveProgress()
        }
    }

    BackHandler { onNavigateBack() }

    val theme = state.readingTheme
    val bgColor = theme.backgroundColor
    val isDarkTheme = theme == ReadingTheme.GRAY || theme == ReadingTheme.DARK

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = if (isDarkTheme) Color.White else MaterialTheme.colorScheme.primary
                )
            }
            state.error != null -> {
                Text(
                    text = state.error ?: "Unknown error",
                    color = theme.textColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            }
            state.epubBook != null -> {
                EpubWebViewContent(
                    state = state,
                    viewModel = viewModel,
                    onCenterTap = { viewModel.toggleOverlay() }
                )
            }
        }

        AnimatedVisibility(
            visible = state.showOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            EpubReaderOverlay(
                state = state,
                onBack = onNavigateBack,
                onChapterSlide = { chapter -> viewModel.goToChapter(chapter) },
                onSettingsClick = { showSettingsSheet = true },
                onTocClick = { showTocSheet = true },
                isDarkTheme = isDarkTheme
            )
        }

        if (showSettingsSheet) {
            EpubSettingsSheet(
                currentTheme = state.readingTheme,
                currentFont = state.readerFont,
                currentFontSize = state.fontSize,
                currentEffect = state.pageTurnEffect,
                onThemeSelected = { viewModel.updateReadingTheme(it) },
                onFontSelected = { viewModel.updateReaderFont(it) },
                onFontSizeChanged = { viewModel.updateFontSize(it) },
                onEffectSelected = { viewModel.updatePageTurnEffect(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }

        if (showTocSheet) {
            TocSheet(
                tocEntries = state.tableOfContents,
                chapters = state.epubBook?.chapters ?: emptyList(),
                currentChapterIndex = state.currentChapterIndex,
                onChapterSelected = { index ->
                    viewModel.goToChapter(index)
                    showTocSheet = false
                },
                onDismiss = { showTocSheet = false }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EpubWebViewContent(
    state: EpubReaderState,
    viewModel: EpubReaderViewModel,
    onCenterTap: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedChapter by remember { mutableStateOf(-1) }
    var lastLoadedTheme by remember { mutableStateOf<ReadingTheme?>(null) }
    var lastLoadedFont by remember { mutableStateOf<ReaderFont?>(null) }
    var lastLoadedFontSize by remember { mutableStateOf(-1) }

    val currentChapterIndex = state.currentChapterIndex
    val currentTheme = state.readingTheme
    val currentFont = state.readerFont
    val currentFontSize = state.fontSize

    LaunchedEffect(currentChapterIndex, currentTheme, currentFont, currentFontSize) {
        val webView = webViewRef ?: return@LaunchedEffect
        val needsReload = currentChapterIndex != lastLoadedChapter ||
                currentTheme != lastLoadedTheme ||
                currentFont != lastLoadedFont ||
                currentFontSize != lastLoadedFontSize

        if (needsReload) {
            val rawHtml = viewModel.getChapterHtml(currentChapterIndex) ?: return@LaunchedEffect
            val styledHtml = viewModel.buildStyledHtml(rawHtml, currentTheme, currentFont, currentFontSize)
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                styledHtml,
                "text/html",
                "UTF-8",
                null
            )
            lastLoadedChapter = currentChapterIndex
            lastLoadedTheme = currentTheme
            lastLoadedFont = currentFont
            lastLoadedFontSize = currentFontSize
        }
    }

    val scrollPositionToRestore = if (lastLoadedChapter == -1) state.scrollPosition else 0f

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.setSupportZoom(false)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onScrollChanged(progress: Float) {
                        viewModel.updateScrollPosition(progress)
                    }

                    @JavascriptInterface
                    fun onCenterTapped() {
                        onCenterTap()
                    }
                }, "EpubBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return true
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (scrollPositionToRestore > 0f) {
                            view?.evaluateJavascript("scrollToProgress($scrollPositionToRestore)", null)
                        }
                    }
                }

                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_UP -> {
                            val viewWidth = v.width
                            val viewHeight = v.height
                            val x = event.x
                            val y = event.y
                            val centerXStart = viewWidth * 0.3f
                            val centerXEnd = viewWidth * 0.7f
                            val centerYStart = viewHeight * 0.3f
                            val centerYEnd = viewHeight * 0.7f

                            if (x in centerXStart..centerXEnd && y in centerYStart..centerYEnd) {
                                onCenterTap()
                            }
                        }
                    }
                    false
                }

                webViewRef = this

                val rawHtml = viewModel.getChapterHtml(state.currentChapterIndex)
                if (rawHtml != null) {
                    val styledHtml = viewModel.buildStyledHtml(
                        rawHtml, state.readingTheme, state.readerFont, state.fontSize
                    )
                    loadDataWithBaseURL(
                        "file:///android_asset/",
                        styledHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )
                    lastLoadedChapter = state.currentChapterIndex
                    lastLoadedTheme = state.readingTheme
                    lastLoadedFont = state.readerFont
                    lastLoadedFontSize = state.fontSize
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { }
    )
}

@Composable
private fun EpubReaderOverlay(
    state: EpubReaderState,
    onBack: () -> Unit,
    onChapterSlide: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onTocClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val overlayBg = Color.Black.copy(alpha = 0.6f)
    val overlayTextColor = Color.White
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(overlayBg)
                .padding(top = statusBarPadding.calculateTopPadding())
                .padding(horizontal = 4.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = overlayTextColor
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = state.currentChapterTitle,
                    color = overlayTextColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Chapter ${state.currentChapterIndex + 1} of ${state.totalChapters}",
                    color = overlayTextColor.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onTocClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Table of Contents",
                    tint = overlayTextColor
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Reader settings",
                    tint = overlayTextColor
                )
            }
        }

        if (state.totalChapters > 1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(overlayBg)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = navBarPadding.calculateBottomPadding())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${state.currentChapterIndex + 1}",
                        color = overlayTextColor,
                        fontSize = 13.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )

                    Slider(
                        value = state.currentChapterIndex.toFloat(),
                        onValueChange = { onChapterSlide(it.toInt()) },
                        valueRange = 0f..(state.totalChapters - 1).coerceAtLeast(0).toFloat(),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )

                    Text(
                        text = "${state.totalChapters}",
                        color = overlayTextColor,
                        fontSize = 13.sp,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpubSettingsSheet(
    currentTheme: ReadingTheme,
    currentFont: ReaderFont,
    currentFontSize: Int,
    currentEffect: PageTurnEffect,
    onThemeSelected: (ReadingTheme) -> Unit,
    onFontSelected: (ReaderFont) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onEffectSelected: (PageTurnEffect) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Reading Theme",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ReadingTheme.entries.forEach { theme ->
                    ThemeCircle(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onClick = { onThemeSelected(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Font",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ReaderFont.entries.forEach { font ->
                    val isSelected = font == currentFont
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { onFontSelected(font) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = font.displayName,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Font Size",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aa",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Slider(
                    value = currentFontSize.toFloat(),
                    onValueChange = { onFontSizeChanged(it.toInt()) },
                    valueRange = 12f..32f,
                    steps = 19,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                Text(
                    text = "Aa",
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${currentFontSize}px",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Page Turn Effect",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PageTurnEffect.entries.forEach { effect ->
                    val isSelected = effect == currentEffect
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onEffectSelected(effect) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = effect.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TocSheet(
    tocEntries: List<TocEntry>,
    chapters: List<EpubChapter>,
    currentChapterIndex: Int,
    onChapterSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Table of Contents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )

            HorizontalDivider()

            if (tocEntries.isNotEmpty()) {
                LazyColumn {
                    itemsIndexed(tocEntries) { _, entry ->
                        val chapterIndex = chapters.indexOfFirst { chapter ->
                            val entryFile = entry.href.substringBefore('#').substringBefore('?')
                            val chapterFile = chapter.href.substringBefore('#').substringBefore('?')
                            entryFile == chapterFile
                        }
                        val isCurrentChapter = chapterIndex == currentChapterIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (chapterIndex >= 0) {
                                        onChapterSelected(chapterIndex)
                                    }
                                }
                                .background(
                                    if (isCurrentChapter) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = entry.title,
                                fontSize = 15.sp,
                                color = if (isCurrentChapter) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrentChapter) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                LazyColumn {
                    itemsIndexed(chapters) { index, chapter ->
                        val isCurrentChapter = index == currentChapterIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onChapterSelected(index) }
                                .background(
                                    if (isCurrentChapter) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = chapter.title,
                                fontSize = 15.sp,
                                color = if (isCurrentChapter) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isCurrentChapter) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCircle(
    theme: ReadingTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(theme.backgroundColor)
                .then(
                    if (isSelected) {
                        Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(theme.backgroundColor)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (theme == ReadingTheme.WHITE) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(theme.textColor)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = theme.displayName,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
