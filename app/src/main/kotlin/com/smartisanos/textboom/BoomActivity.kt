package com.cashewteam.novatext.android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.core.view.WindowCompat
import com.cashewteam.novatext.android.data.BigBangSettings
import com.cashewteam.novatext.android.data.CppJiebaTokenizer
import com.cashewteam.novatext.android.domain.capture.TextSessionCoordinator
import com.cashewteam.novatext.android.service.BoomOcrLauncher
import com.cashewteam.novatext.android.service.FloatingBallService
import com.cashewteam.novatext.android.util.LogUtils

class BoomActivity : ComponentActivity() {
    private var boomChipPage: BoomChipPage? = null
    private lateinit var legacyContentView: View
    private lateinit var settings: BigBangSettings
    private var launchTouchX = -1
    private var launchTouchY = -1
    private var currentText = ""
    private var currentSegment: IntArray? = null
    private var manualOcrSourceToken: String? = null
    private var floatingBallHideToken: Int? = null
    private var animatedDismissRequester: (() -> Unit)? = null
    private var translationInProgress by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = BigBangSettings.get(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        launchTouchX = intent.getIntExtra("boom_startx", -1)
        launchTouchY = intent.getIntExtra("boom_starty", -1)
        manualOcrSourceToken = intent.getStringExtra(EXTRA_MANUAL_OCR_SOURCE_TOKEN)

        legacyContentView = layoutInflater.inflate(R.layout.boom_activity_layout, null, false)
        legacyContentView.findViewById<View>(R.id.boom_page).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            if (intent.getBooleanExtra(OcrLaunchActivity.EXTRA_SKIP_LEGACY_FADE_IN, false)) {
                visibility = View.VISIBLE
                alpha = 1f
            } else {
                BoomAnimator.makeFadeIn(this, BoomAnimator.BOOM_DURATION)
            }
        }
        boomChipPage = BoomChipPage(this, legacyContentView, false).also { page ->
            page.restoreSelectedState(savedInstanceState?.getSerializable(SELECTED_STATE))
            page.setOnAdjacentRequestListener(object : BoomChipPage.OnAdjacentRequestListener {
                override fun onAdjacentRequest(direction: String) {
                    loadAdjacent(direction)
                }
            })
        }

        setContent {
            BigBangOverlayContent(
                contentView = legacyContentView,
                touchX = launchTouchX,
                touchY = launchTouchY,
                manualOcrSourceToken = manualOcrSourceToken,
                classicOverlayStyleEnabled = settings.isClassicOverlayStyleEnabled,
                contextAppendActionsEnabled = settings.isContextAppendActionsEnabled,
                translationInProgress = translationInProgress,
                defaultTranslationTarget = settings.translationTargetLanguage,
                currentTextProvider = { currentText },
                onDismissRequesterChanged = { animatedDismissRequester = it },
                onDismissRequest = { shouldDismissPage() },
                onDismissFinished = { finish() },
                onOcr = { reopenManualOcr() },
                onTranslate = {
                    settings.setTranslationTargetLanguage(it)
                    translateCurrentText(it)
                },
                onEditedTextCommitted = { replaceCurrentText(it) },
                onSelectAll = { selectAll() },
                onShareAll = { shareAll() },
                onOpenSettings = { openSettings() },
                onPreviousText = { requestAdjacent(BoomEdgeActionPolicy.DIRECTION_BEFORE) },
                onNextText = { requestAdjacent(BoomEdgeActionPolicy.DIRECTION_AFTER) },
            )
        }

        // Restore adjacent-pulled text across rotation
        val savedText = savedInstanceState?.getString(SAVED_TEXT)
        val savedSegment = savedInstanceState?.getIntArray(SAVED_SEGMENT)
        if (savedText != null && savedSegment != null) {
            currentText = savedText
            currentSegment = savedSegment
            handleInitialSegmentResult(savedText, savedSegment, fromSavedState = true)
        } else {
            val previewText = intent.getStringExtra(EXTRA_DEBUG_PREVIEW_TEXT)
            val inputText = previewText ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            if (inputText.isNullOrEmpty()) {
                finish()
                return
            }
            if (!intent.getBooleanExtra(EXTRA_ENABLE_ADJACENT_SESSION, false)) {
                TextSessionCoordinator.clearSession()
            }
            segmentLocally(inputText)
        }
    }

    override fun onStart() {
        super.onStart()
        if (floatingBallHideToken == null) {
            floatingBallHideToken = FloatingBallService.acquireVisibilitySuppression()
        }
        FloatingBallService.notifyBigBangShellShown()
    }

    override fun onStop() {
        FloatingBallService.releaseVisibilitySuppression(floatingBallHideToken)
        floatingBallHideToken = null
        super.onStop()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun shouldDismissPage(): Boolean {
        return boomChipPage?.handleClick() != true
    }

    fun requestAnimatedDismissFromLegacy() {
        animatedDismissRequester?.invoke() ?: finish()
    }

    private fun selectAll() {
        boomChipPage?.selectAll()
    }

    private fun shareAll() {
        val shareText = boomChipPage?.originalText ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        })
    }

    private fun requestAdjacent(direction: String) {
        if (boomChipPage?.requestAdjacent(direction) == true) {
            val message = if (direction == BoomEdgeActionPolicy.DIRECTION_BEFORE) {
                R.string.bigbang_context_append_before_started
            } else {
                R.string.bigbang_context_append_after_started
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.bigbang_adjacent_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, TextBoomSettingsActivity::class.java))
    }

    private fun translateCurrentText(targetLanguage: String) {
        if (translationInProgress) {
            return
        }
        val apiKey = settings.translationApiKey.trim()
        if (apiKey.isEmpty()) {
            Toast.makeText(this, R.string.bigbang_translate_config_required, Toast.LENGTH_SHORT).show()
            openSettings()
            return
        }
        val sourceText = currentText.ifBlank { boomChipPage?.originalText.orEmpty() }.trim()
        if (sourceText.isEmpty()) {
            Toast.makeText(this, R.string.bigbang_edit_empty_text, Toast.LENGTH_SHORT).show()
            return
        }
        translationInProgress = true
        Toast.makeText(this, R.string.bigbang_translate_started, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val translated = InlineTranslationClient.translate(
                    sourceText,
                    targetLanguage,
                    settings.translationApiUrl,
                    apiKey,
                    settings.translationModel,
                    settings.translationPromptTemplate,
                )
                runOnUiThread {
                    translationInProgress = false
                    replaceCurrentText(translated)
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "inline translation failed")
                LogUtils.e(e.message, e)
                runOnUiThread {
                    translationInProgress = false
                    Toast.makeText(this, R.string.bigbang_translate_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun replaceCurrentText(text: String) {
        val nextText = text.trim()
        if (nextText.isEmpty()) {
            Toast.makeText(this, R.string.bigbang_edit_empty_text, Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            try {
                val result = CppJiebaTokenizer.get(this).segment(nextText)
                runOnUiThread {
                    if (isFinishing) {
                        return@runOnUiThread
                    }
                    if (result == null || result.isEmpty()) {
                        Toast.makeText(this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show()
                        return@runOnUiThread
                    }
                    if (boomChipPage?.replaceWords(result, nextText, -1) == true) {
                        currentText = nextText
                        currentSegment = result
                    } else {
                        Toast.makeText(this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: RuntimeException) {
                LogUtils.e(TAG, "replace text segmentation failed")
                LogUtils.e(e.message, e)
                runOnUiThread {
                    Toast.makeText(this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun reopenManualOcr() {
        val source = ManualOcrSourceStore.get(manualOcrSourceToken) ?: return
        BoomOcrLauncher.open(
            context = this,
            imageUri = source.imageUri,
            touchX = source.touchX,
            touchY = source.touchY,
            fullscreen = source.fullscreen,
            callerPackage = source.callerPackage,
            offsetX = source.offsetX,
            offsetY = source.offsetY,
            manualOcrSourceToken = source.token,
        )
    }

    private fun segmentLocally(text: String) {
        if (DBG) {
            Log.d(TAG, "text=$text")
        }
        Thread {
            try {
                val result = CppJiebaTokenizer.get(this).segment(text)
                runOnUiThread {
                    if (!isFinishing) {
                        handleInitialSegmentResult(text, result)
                    }
                }
            } catch (e: RuntimeException) {
                LogUtils.e(TAG, "local segmentation failed")
                LogUtils.e(e.message, e)
                runOnUiThread { finish() }
            }
        }.start()
    }

    private fun handleInitialSegmentResult(text: String, result: IntArray?, fromSavedState: Boolean = false) {
        if (result == null || result.isEmpty()) {
            Log.e(TAG, "Segmentation fails for text=$text")
            finish()
            return
        }
        val touchIndex = if (fromSavedState) -1 else intent.getIntExtra("boom_index", -1)
        val touchedX = if (fromSavedState) -1 else intent.getIntExtra("boom_startx", -1)
        val touchedY = if (fromSavedState) -1 else intent.getIntExtra("boom_starty", -1)
        if (boomChipPage?.initWords(result, text, touchIndex, touchedX, touchedY) != true) {
            val log = buildString {
                result.forEach {
                    append(it)
                    append(", ")
                }
            }
            Log.w(TAG, "No words left after segment, input=$text, output=$log")
            if (intent.getStringExtra("boom_image") != null) {
                Toast.makeText(this, R.string.a_msg_no_words, Toast.LENGTH_SHORT).show()
            }
            finish()
            return
        }
        currentText = text
        currentSegment = result
    }

    private fun loadAdjacent(direction: String) {
        val adjacentText = TextSessionCoordinator.peekAdjacentText(direction)?.trim().orEmpty()
        val baseSegment = currentSegment
        if (adjacentText.isEmpty() || baseSegment == null || currentText.isEmpty()) {
            boomChipPage?.finishAdjacentPull()
            return
        }
        Thread {
            try {
                val adjacentSegment = CppJiebaTokenizer.get(this).segment(adjacentText)
                runOnUiThread {
                    if (isFinishing) {
                        return@runOnUiThread
                    }
                    if (adjacentSegment == null || adjacentSegment.isEmpty()) {
                        boomChipPage?.finishAdjacentPull()
                        return@runOnUiThread
                    }
                    val merged = mergeSegmentedText(
                        direction = direction,
                        baseText = currentText,
                        baseSegment = baseSegment,
                        adjacentText = adjacentText,
                        adjacentSegment = adjacentSegment,
                    )
                    val charOffset = if (direction == "before") adjacentText.length + 1 else 0
                    val replaced = boomChipPage?.replaceWords(
                        merged.segment,
                        merged.text,
                        merged.targetWordIndex,
                        charOffset,
                    ) == true
                    if (!replaced) {
                        boomChipPage?.finishAdjacentPull()
                        return@runOnUiThread
                    }
                    TextSessionCoordinator.loadAdjacent(direction)
                    currentText = merged.text
                    currentSegment = merged.segment
                }
            } catch (e: RuntimeException) {
                LogUtils.e(TAG, "adjacent segmentation failed")
                LogUtils.e(e.message, e)
                runOnUiThread { boomChipPage?.finishAdjacentPull() }
            }
        }.start()
    }

    private fun mergeSegmentedText(
        direction: String,
        baseText: String,
        baseSegment: IntArray,
        adjacentText: String,
        adjacentSegment: IntArray,
    ): SegmentedText {
        val separator = "\n"
        return if (direction == "before") {
            SegmentedText(
                text = adjacentText + separator + baseText,
                segment = mergeSegments(
                    first = adjacentSegment,
                    firstOffset = 0,
                    second = baseSegment,
                    secondOffset = adjacentText.length + separator.length,
                ),
                targetWordIndex = 0,
            )
        } else {
            SegmentedText(
                text = baseText + separator + adjacentText,
                segment = mergeSegments(
                    first = baseSegment,
                    firstOffset = 0,
                    second = adjacentSegment,
                    secondOffset = baseText.length + separator.length,
                ),
                targetWordIndex = wordCount(baseSegment),
            )
        }
    }

    private fun wordCount(segment: IntArray): Int {
        return splitSegment(segment).words.size / 2
    }

    private fun mergeSegments(
        first: IntArray,
        firstOffset: Int,
        second: IntArray,
        secondOffset: Int,
    ): IntArray {
        val firstSplit = splitSegment(first)
        val secondSplit = splitSegment(second)
        return buildList {
            addAll(shiftPairs(firstSplit.words, firstOffset))
            addAll(shiftPairs(secondSplit.words, secondOffset))
            add(-1)
            addAll(shiftPairs(firstSplit.punctuations, firstOffset))
            addAll(shiftPairs(secondSplit.punctuations, secondOffset))
        }.toIntArray()
    }

    private fun splitSegment(segment: IntArray): SegmentParts {
        val separatorIndex = segment.indexOfFirst { it == -1 }
        if (separatorIndex < 0) {
            return SegmentParts(words = segment.toList(), punctuations = emptyList())
        }
        return SegmentParts(
            words = segment.take(separatorIndex),
            punctuations = segment.drop(separatorIndex + 1),
        )
    }

    private fun shiftPairs(values: List<Int>, offset: Int): List<Int> {
        if (offset == 0) return values
        return values.map { it + offset }
    }

    private data class SegmentParts(
        val words: List<Int>,
        val punctuations: List<Int>,
    )

private data class SegmentedText(
        val text: String,
        val segment: IntArray,
        val targetWordIndex: Int,
    )

    override fun onSaveInstanceState(outState: Bundle) {
        boomChipPage?.captureSelectedState()?.let {
            outState.putSerializable(SELECTED_STATE, it)
        }
        if (currentSegment != null) {
            outState.putString(SAVED_TEXT, currentText)
            outState.putIntArray(SAVED_SEGMENT, currentSegment)
        }
        super.onSaveInstanceState(outState)
    }

    companion object {
        const val DBG = true
        const val EXTRA_DEBUG_PREVIEW_TEXT = "extra_debug_preview_text"
        const val EXTRA_ENABLE_ADJACENT_SESSION = "extra_enable_adjacent_session"
        const val EXTRA_MANUAL_OCR_SOURCE_TOKEN = "extra_manual_ocr_source_token"

        private const val TAG = "BoomActivity"
        private const val SELECTED_STATE = "selected_state"
        private const val SAVED_TEXT = "saved_text"
        private const val SAVED_SEGMENT = "saved_segment"
    }
}

private data class TranslationLanguage(
    val code: String,
    val name: String,
)

@Composable
private fun BigBangOverlayContent(
    contentView: View,
    touchX: Int,
    touchY: Int,
    manualOcrSourceToken: String?,
    classicOverlayStyleEnabled: Boolean,
    contextAppendActionsEnabled: Boolean,
    translationInProgress: Boolean,
    defaultTranslationTarget: String,
    currentTextProvider: () -> String,
    onDismissRequesterChanged: ((() -> Unit)?) -> Unit,
    onDismissRequest: () -> Boolean,
    onDismissFinished: () -> Unit,
    onOcr: () -> Unit,
    onTranslate: (String) -> Unit,
    onEditedTextCommitted: (String) -> Unit,
    onSelectAll: () -> Unit,
    onShareAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onPreviousText: () -> Unit,
    onNextText: () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val panelMetrics = rememberOverlayPanelMetrics(forceFullscreen = classicOverlayStyleEnabled)
    val panelBackground = if (dark) Color(0xFF171B20) else Color(0xFFF3F3F4)
    val panelBorder = if (dark) Color(0xFF2E353E) else Color(0xFFD7D7DA)
    val topBarColor = if (dark) Color(0xFF1D2126) else Color.White
    val bottomBarColor = if (dark) Color(0xFF1D2126) else Color.White
    val scrimColor = if (dark) Color.Black.copy(alpha = 0.62f) else Color.Black.copy(alpha = 0.48f)
    val shadowColor = Color.Black.copy(alpha = 0.5f)
    val panelShape = androidx.compose.foundation.shape.RoundedCornerShape(panelMetrics.cornerRadius)
    var panelVisible by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }
    var panelBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val enterProgress by animateFloatAsState(
        targetValue = if (panelVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "bigbang_panel_enter",
        finishedListener = {
            if (dismissing && it == 0f) {
                onDismissFinished()
            }
        },
    )
    val scrimProgress by animateFloatAsState(
        targetValue = if (panelVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "bigbang_scrim_enter",
    )
    val transformOrigin = remember(panelBounds, touchX, touchY) {
        val bounds = panelBounds
        if (bounds == null || touchX < 0 || touchY < 0) {
            TransformOrigin.Center
        } else {
            TransformOrigin(
                pivotFractionX = ((touchX - bounds.left) / bounds.width).coerceIn(0f, 1f),
                pivotFractionY = ((touchY - bounds.top) / bounds.height).coerceIn(0f, 1f),
            )
        }
    }
    val panelScale = 0.84f + (0.16f * enterProgress)
    val manualOcrRevision by ManualOcrSourceStore.revisionFlow().collectAsState()
    val ocrSource = remember(manualOcrSourceToken, manualOcrRevision) {
        ManualOcrSourceStore.get(manualOcrSourceToken)
    }
    val ocrEnabled = ocrSource != null
    val focusManager = LocalFocusManager.current
    val editFocusRequester = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editHadFocus by remember { mutableStateOf(false) }
    var translationMenuExpanded by remember { mutableStateOf(false) }
    val translationLanguages = remember(defaultTranslationTarget) {
        val languages = listOf(
            TranslationLanguage("zh", "中文"),
            TranslationLanguage("en", "英语"),
            TranslationLanguage("ja", "日语"),
            TranslationLanguage("ko", "韩语"),
            TranslationLanguage("zh-Hant", "繁体中文"),
            TranslationLanguage("fr", "法语"),
            TranslationLanguage("pt", "葡萄牙语"),
            TranslationLanguage("es", "西班牙语"),
            TranslationLanguage("tr", "土耳其语"),
            TranslationLanguage("ru", "俄语"),
            TranslationLanguage("ar", "阿拉伯语"),
            TranslationLanguage("th", "泰语"),
            TranslationLanguage("it", "意大利语"),
            TranslationLanguage("de", "德语"),
            TranslationLanguage("vi", "越南语"),
            TranslationLanguage("ms", "马来语"),
            TranslationLanguage("id", "印尼语"),
            TranslationLanguage("tl", "菲律宾语"),
            TranslationLanguage("hi", "印地语"),
            TranslationLanguage("pl", "波兰语"),
            TranslationLanguage("cs", "捷克语"),
            TranslationLanguage("nl", "荷兰语"),
            TranslationLanguage("km", "高棉语"),
            TranslationLanguage("my", "缅甸语"),
            TranslationLanguage("fa", "波斯语"),
            TranslationLanguage("gu", "古吉拉特语"),
            TranslationLanguage("ur", "乌尔都语"),
            TranslationLanguage("te", "泰卢固语"),
            TranslationLanguage("mr", "马拉地语"),
            TranslationLanguage("he", "希伯来语"),
            TranslationLanguage("bn", "孟加拉语"),
            TranslationLanguage("ta", "泰米尔语"),
            TranslationLanguage("uk", "乌克兰语"),
            TranslationLanguage("bo", "藏语"),
            TranslationLanguage("kk", "哈萨克语"),
            TranslationLanguage("mn", "蒙古语"),
            TranslationLanguage("ug", "维吾尔语"),
            TranslationLanguage("yue", "粤语"),
        )
        languages.sortedBy { if (it.name == defaultTranslationTarget) 0 else 1 }
    }
    val commitEditedText = {
        if (editing) {
            editing = false
            editHadFocus = false
            onEditedTextCommitted(editText)
        }
    }
    val requestDismiss = {
        if (!dismissing && onDismissRequest()) {
            translationMenuExpanded = false
            dismissing = true
            panelVisible = false
        }
    }
    val requestBlankClick = {
        if (editing) {
            focusManager.clearFocus()
        } else {
            requestDismiss()
        }
    }

    DisposableEffect(requestDismiss) {
        onDismissRequesterChanged(requestDismiss)
        onDispose {
            onDismissRequesterChanged(null)
        }
    }

    LaunchedEffect(Unit) {
        panelVisible = true
    }

    BackHandler(onBack = requestBlankClick)
    ApplyOverlaySystemBars(
        statusBarColor = if (panelMetrics.fullScreen) topBarColor else Color.Transparent,
        navigationBarColor = if (panelMetrics.fullScreen) bottomBarColor else Color.Transparent,
        darkIcons = !dark,
    )
    OverlayScene(scrimColor = scrimColor.copy(alpha = scrimColor.alpha * scrimProgress), onDismiss = requestBlankClick) {
        FloatingPanel(
            width = panelMetrics.width,
            height = panelMetrics.height,
            fillMax = panelMetrics.fullScreen,
            modifier = overlayPanelPlacement(panelMetrics)
                .onGloballyPositioned { coordinates ->
                    panelBounds = coordinates.boundsInWindow()
                }
                .graphicsLayer {
                    alpha = enterProgress
                    scaleX = panelScale
                    scaleY = panelScale
                    this.transformOrigin = transformOrigin
                },
            shape = panelShape,
            backgroundColor = panelBackground,
            borderColor = panelBorder,
            shadowColor = if (panelMetrics.multiWindow) null else shadowColor,
        ) {
            OverlayPanelScaffold(
                topBar = {
                    OverlayHeaderBar(
                        backgroundColor = topBarColor,
                        topInset = panelMetrics.topSystemInset,
                        leftInset = panelMetrics.leftSystemInset,
                        rightInset = panelMetrics.rightSystemInset,
                        leading = {
                            OverlayIconAction(
                                imageVector = if (editing) Icons.Outlined.Check else Icons.Outlined.Edit,
                                tint = if (dark) Color(0xFFD7DEE7) else Color(0xFF6F6962),
                                onClick = {
                                    if (editing) {
                                        commitEditedText()
                                    } else {
                                        editText = currentTextProvider()
                                        editing = true
                                    }
                                },
                                contentDescription = stringResource(
                                    if (editing) {
                                        R.string.bigbang_edit_done
                                    } else {
                                        R.string.bigbang_action_edit
                                    }
                                ),
                            )
                            OverlayIconAction(
                                imageVector = Icons.Outlined.SelectAll,
                                tint = if (dark) Color(0xFFF2F5F8) else Color(0xFF6C6760),
                                onClick = onSelectAll,
                                contentDescription = stringResource(R.string.bigbang_action_select_all),
                            )
                        },
                        center = {
                            androidx.compose.material3.Text(
                                text = stringResource(R.string.bigbang_overlay_title),
                                color = if (dark) Color(0xFFF2F5F8) else Color(0xFFD1CCC6),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        },
                        trailing = {
                            OverlayIconAction(
                                imageVector = Icons.Outlined.Share,
                                tint = if (dark) Color(0xFFF2F5F8) else Color(0xFF6C6760),
                                onClick = onShareAll,
                                contentDescription = stringResource(R.string.bigbang_action_share_all),
                            )
                            OverlayIconAction(
                                imageVector = Icons.Outlined.Settings,
                                tint = if (dark) Color(0xFFD7DEE7) else Color(0xFF6F6962),
                                onClick = onOpenSettings,
                                contentDescription = stringResource(R.string.text_boom_settings),
                            )
                        },
                    )
                },
                bottomBar = {
                    OverlayBottomBar(
                        backgroundColor = bottomBarColor,
                        bottomInset = panelMetrics.bottomSystemInset,
                        leftInset = panelMetrics.leftSystemInset,
                        rightInset = panelMetrics.rightSystemInset,
                        leading = {
                            OverlayIconAction(
                                imageVector = Icons.Outlined.DocumentScanner,
                                tint = if (ocrEnabled) {
                                    if (dark) Color(0xFFF2F5F8) else Color(0xFF8D8983)
                                } else {
                                    if (dark) Color(0x66F2F5F8) else Color(0x668D8983)
                                },
                                enabled = ocrEnabled,
                                onClick = onOcr,
                                contentDescription = stringResource(R.string.bigbang_action_ocr),
                            )
                        },
                        center = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (contextAppendActionsEnabled) {
                                    OverlayIconAction(
                                        iconRes = R.drawable.ic_context_append_before,
                                        tint = if (dark) Color(0xFFF2F5F8) else Color(0xFF8D8983),
                                        onClick = onPreviousText,
                                        contentDescription = stringResource(R.string.bigbang_action_append_before),
                                    )
                                    OverlayIconAction(
                                        iconRes = R.drawable.ic_context_append_after,
                                        tint = if (dark) Color(0xFFF2F5F8) else Color(0xFF8D8983),
                                        onClick = onNextText,
                                        contentDescription = stringResource(R.string.bigbang_action_append_after),
                                    )
                                }
                                OverlayIconAction(
                                    iconRes = R.drawable.boom_cancel,
                                    tint = if (dark) Color(0xFFF2F5F8) else Color(0xFF8D8983),
                                    onClick = requestDismiss,
                                    contentDescription = stringResource(R.string.search_overlay_close),
                                )
                            }
                        },
                        trailing = {
                            Box {
                                OverlayIconAction(
                                    imageVector = Icons.Outlined.Translate,
                                    tint = if (translationInProgress) {
                                        if (dark) Color(0x66F2F5F8) else Color(0x668D8983)
                                    } else {
                                        if (dark) Color(0xFFF2F5F8) else Color(0xFF8D8983)
                                    },
                                    enabled = !translationInProgress,
                                    onClick = { translationMenuExpanded = true },
                                    contentDescription = stringResource(R.string.bigbang_action_translate),
                                )
                                DropdownMenu(
                                    expanded = translationMenuExpanded,
                                    onDismissRequest = { translationMenuExpanded = false },
                                    containerColor = if (dark) Color(0xFF20252B) else Color.White,
                                ) {
                                    translationLanguages.forEach { language ->
                                        DropdownMenuItem(
                                            text = {
                                                androidx.compose.material3.Text(
                                                    text = "${language.name} (${language.code})",
                                                    color = if (dark) Color(0xFFF2F5F8) else Color(0xFF3B3B3B),
                                                )
                                            },
                                            onClick = {
                                                translationMenuExpanded = false
                                                onTranslate(language.name)
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
            ) { bodyModifier ->
                Column(modifier = bodyModifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (editing) {
                        LaunchedEffect(Unit) {
                            editFocusRequester.requestFocus()
                        }
                        BigBangEditTextBox(
                            text = editText,
                            onTextChange = { editText = it },
                            onCommit = commitEditedText,
                            onFocusChanged = { focused ->
                                if (editHadFocus && !focused) {
                                    commitEditedText()
                                }
                                if (focused) {
                                    editHadFocus = true
                                }
                            },
                            focusRequester = editFocusRequester,
                            dark = dark,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    } else {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(bottom = 4.dp),
                            factory = {
                                contentView
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BigBangEditTextBox(
    text: String,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (dark) Color(0xFF20262D) else Color.White
    val textColor = if (dark) Color(0xFFF2F5F8) else Color(0xFF333333)
    val hintColor = if (dark) Color(0xFF8E98A4) else Color(0xFF9A948D)
    Box(
        modifier = modifier
            .background(background, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChanged(it.isFocused) },
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onCommit() }),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        androidx.compose.material3.Text(
                            text = stringResource(R.string.bigbang_edit_text_hint),
                            color = hintColor,
                            fontSize = 17.sp,
                        )
                    }
                    innerTextField()
                },
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCommit,
            ) {
                androidx.compose.material3.Text(text = stringResource(R.string.bigbang_edit_done))
            }
        }
    }
}
