package com.applebooks.android.reader.pdf

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.applebooks.android.domain.model.PageTurnEffect
import com.applebooks.android.domain.model.ReadingTheme
import eu.wewox.pagecurl.page.PageCurl
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: PdfReaderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    var showSettingsSheet by remember { mutableStateOf(false) }

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
            else -> {
                val tintFilter = remember(theme) {
                    when (theme) {
                        ReadingTheme.WHITE -> null
                        ReadingTheme.SEPIA -> ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    0.94f, 0f, 0f, 0f, 10f,
                                    0f, 0.89f, 0f, 0f, 5f,
                                    0f, 0f, 0.80f, 0f, -15f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                        ReadingTheme.GRAY -> ColorFilter.colorMatrix(
                            ColorMatrix().apply { setToSaturation(0f) }.also { matrix ->
                                val scale = ColorMatrix(
                                    floatArrayOf(
                                        0.35f, 0f, 0f, 0f, 50f,
                                        0f, 0.35f, 0f, 0f, 50f,
                                        0f, 0f, 0.35f, 0f, 50f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                                matrix.timesAssign(scale)
                            }
                        )
                        ReadingTheme.DARK -> ColorFilter.colorMatrix(
                            ColorMatrix(
                                floatArrayOf(
                                    -0.8f, 0f, 0f, 0f, 230f,
                                    0f, -0.8f, 0f, 0f, 230f,
                                    0f, 0f, -0.8f, 0f, 225f,
                                    0f, 0f, 0f, 1f, 0f
                                )
                            )
                        )
                    }
                }

                PdfPageContent(
                    state = state,
                    tintFilter = tintFilter,
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
            ReaderOverlay(
                state = state,
                onBack = onNavigateBack,
                onPageSlide = { page -> viewModel.goToPage(page) },
                onSettingsClick = { showSettingsSheet = true },
                isDarkTheme = isDarkTheme
            )
        }

        if (showSettingsSheet) {
            ReaderSettingsSheet(
                currentTheme = state.readingTheme,
                currentEffect = state.pageTurnEffect,
                onThemeSelected = { viewModel.updateReadingTheme(it) },
                onEffectSelected = { viewModel.updatePageTurnEffect(it) },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun PdfPageContent(
    state: PdfReaderState,
    tintFilter: ColorFilter?,
    viewModel: PdfReaderViewModel,
    onCenterTap: () -> Unit
) {
    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }

    LaunchedEffect(state.totalPages) {
        if (state.totalPages > 0) {
            val bitmap = viewModel.renderPage(state.currentPage)
            if (bitmap != null) {
                renderedBitmaps[state.currentPage] = bitmap
            }
        }
    }

    when (state.pageTurnEffect) {
        PageTurnEffect.CURL -> {
            PageCurl(
                count = state.totalPages,
            ) { pageIndex ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    renderedBitmaps = renderedBitmaps,
                    tintFilter = tintFilter,
                    backgroundColor = state.readingTheme.backgroundColor,
                    onCenterTap = onCenterTap
                )
            }

            LaunchedEffect(Unit) {
                viewModel.preloadAdjacentPages(state.currentPage)
            }
        }
        PageTurnEffect.SLIDE -> {
            val pagerState = rememberPagerState(
                initialPage = state.currentPage,
                pageCount = { state.totalPages }
            )

            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.currentPage }
                    .distinctUntilChanged()
                    .collect { page ->
                        viewModel.goToPage(page)
                        viewModel.preloadAdjacentPages(page)
                    }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                PdfPageItem(
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    renderedBitmaps = renderedBitmaps,
                    tintFilter = tintFilter,
                    backgroundColor = state.readingTheme.backgroundColor,
                    onCenterTap = onCenterTap
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    viewModel: PdfReaderViewModel,
    renderedBitmaps: MutableMap<Int, Bitmap>,
    tintFilter: ColorFilter?,
    backgroundColor: Color,
    onCenterTap: () -> Unit
) {
    val bitmap = renderedBitmaps[pageIndex]

    LaunchedEffect(pageIndex) {
        if (renderedBitmaps[pageIndex] == null) {
            val rendered = viewModel.renderPage(pageIndex)
            if (rendered != null) {
                renderedBitmaps[pageIndex] = rendered
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCenterTap() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = tintFilter
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 2.dp,
                color = if (backgroundColor == Color.White || backgroundColor == Color(0xFFF4ECD8)) {
                    Color.Gray
                } else {
                    Color.LightGray
                }
            )
        }
    }
}

@Composable
private fun ReaderOverlay(
    state: PdfReaderState,
    onBack: () -> Unit,
    onPageSlide: (Int) -> Unit,
    onSettingsClick: () -> Unit,
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

            Text(
                text = state.book?.title ?: "",
                color = overlayTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Page ${state.currentPage + 1} of ${state.totalPages}",
                color = overlayTextColor.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.padding(end = 4.dp)
            )

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Reader settings",
                    tint = overlayTextColor
                )
            }
        }

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
                    text = "${state.currentPage + 1}",
                    color = overlayTextColor,
                    fontSize = 13.sp,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )

                Slider(
                    value = state.currentPage.toFloat(),
                    onValueChange = { onPageSlide(it.toInt()) },
                    valueRange = 0f..(state.totalPages - 1).coerceAtLeast(0).toFloat(),
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Text(
                    text = "${state.totalPages}",
                    color = overlayTextColor,
                    fontSize = 13.sp,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    currentTheme: ReadingTheme,
    currentEffect: PageTurnEffect,
    onThemeSelected: (ReadingTheme) -> Unit,
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
                            text = effect.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
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
