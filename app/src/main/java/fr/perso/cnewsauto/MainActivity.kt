package fr.perso.cnewsauto

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*

@UnstableApi
class MainActivity : Activity() {
    private var browser: MediaBrowser? = null
    private var connection: ListenableFuture<MediaBrowser>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val artwork = ArtworkLoader(scope)
    private var ticker: Job? = null
    private lateinit var list: ListView
    private lateinit var heading: TextView
    private lateinit var status: TextView
    private lateinit var cover: ImageView
    private lateinit var progress: SeekBar
    private lateinit var time: TextView
    private lateinit var play: Button
    private lateinit var pause: Button
    private lateinit var back: Button
    private lateinit var forward: Button
    private lateinit var jumps: LinearLayout
    private data class Anchor(val id: String?, val index: Int, val top: Int)
    private val anchors = mutableMapOf<String, Anchor>()
    private fun rememberScroll() {
        if (rows.isNotEmpty() && list.childCount > 0) {
            val index = list.firstVisiblePosition
            anchors[parent] = Anchor(rows.getOrNull(index)?.mediaId, index, list.getChildAt(0).top)
        }
    }
    private var dragging = false
    private var parent = "root"
    private var rows: List<MediaItem> = emptyList()
    private var generation = 0
    private lateinit var history: ListeningHistory
    private lateinit var adapter: EpisodeAdapter
    private val uiExecutor = java.util.concurrent.Executor { command -> runOnUiThread(command) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(size: Float, color: Int = Color.WHITE) = TextView(this).apply { textSize = size; setTextColor(color) }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; setOnClickListener { action() } }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        history = ListeningHistory(this)
        parent = state?.getString("parent") ?: "root"
        if (state?.containsKey("scroll_index") == true) anchors[parent] = Anchor(state.getString("scroll_id"), state.getInt("scroll_index"), state.getInt("scroll_top"))
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(18,22,30)) }
        layout.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(dp(12) + insets.systemWindowInsetLeft, insets.systemWindowInsetTop + dp(8), dp(12) + insets.systemWindowInsetRight, insets.systemWindowInsetBottom + dp(8)); insets
        }
        heading = label(24f)
        layout.addView(heading)
        val navigation = LinearLayout(this)
        navigation.addView(button("Émissions") { open("root") }, LinearLayout.LayoutParams(0, dp(48), 1f))
        navigation.addView(button("Android Auto") { showAutoHelp() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        layout.addView(navigation)
        jumps = LinearLayout(this).apply {
            visibility = View.GONE
            addView(button("↑ Début") { if (rows.isNotEmpty()) list.setSelection(0) }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(button("↓ Fin") { if (rows.isNotEmpty()) list.setSelection(rows.lastIndex) }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        layout.addView(jumps)
        adapter = EpisodeAdapter()
        list = ListView(this).apply { dividerHeight = dp(1); adapter = this@MainActivity.adapter }
        layout.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        val nowPlaying = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(6), 0, dp(6)) }
        cover = ImageView(this).apply { contentDescription = "Pochette du podcast"; scaleType = ImageView.ScaleType.CENTER_CROP; setImageResource(R.drawable.ic_podcast) }
        nowPlaying.addView(cover, LinearLayout.LayoutParams(dp(60), dp(60)))
        status = label(15f, Color.LTGRAY).apply { text = "Connexion…"; maxLines = 3; setPadding(dp(10),0,0,0) }
        nowPlaying.addView(status, LinearLayout.LayoutParams(0,-2,1f))
        layout.addView(nowPlaying)
        val timeline = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        play = button("▶") { browser?.let { if (it.playbackState == Player.STATE_IDLE) it.prepare(); if (it.playbackState == Player.STATE_ENDED) it.seekTo(0); it.play() } }.apply { contentDescription = "Lecture" }
        pause = button("Ⅱ") { browser?.pause() }.apply { contentDescription = "Pause" }
        progress = SeekBar(this).apply { max = 1000; contentDescription = "Position de lecture" }
        timeline.addView(play, LinearLayout.LayoutParams(dp(56), dp(48)))
        timeline.addView(progress, LinearLayout.LayoutParams(0, dp(48), 1f))
        timeline.addView(pause, LinearLayout.LayoutParams(dp(56), dp(48)))
        layout.addView(timeline)
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        back = button("−15 s") { browser?.seekBack() }
        forward = button("+30 s") { browser?.seekForward() }
        time = label(14f, Color.LTGRAY).apply { gravity = Gravity.CENTER; text = "0:00 / —" }
        controls.addView(back, LinearLayout.LayoutParams(dp(80), dp(48)))
        controls.addView(time, LinearLayout.LayoutParams(0,-2,1f))
        controls.addView(forward, LinearLayout.LayoutParams(dp(80), dp(48)))
        layout.addView(controls)
        progress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { dragging = true }
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                if (fromUser) browser?.duration?.takeIf { it > 0 }?.let { time.text = "${PlaybackProgress.clock(it * value / 1000)} / ${PlaybackProgress.clock(it)}" }
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                browser?.let { b -> if (b.isCurrentMediaItemSeekable && b.duration > 0) b.seekTo(b.duration * seekBar.progress / 1000) }
                dragging = false
            }
        })
        setContentView(layout)
        updatePlayback()
    }

    private fun showAutoHelp() {
        AlertDialog.Builder(this).setTitle("Afficher l’application dans Android Auto")
            .setMessage("Cette application personnelle nécessite l’option Sources inconnues d’Android Auto.\n\n1. Dans les paramètres Android Auto, toucher dix fois les informations de version pour activer son mode développeur.\n2. Menu ⋮ → Paramètres développeur → Sources inconnues.\n3. Personnaliser le lanceur → cocher Podcasts CNEWS · Perso.\n4. Reconnecter le téléphone à la voiture.\n\nCe réglage est distinct du débogage USB/Wi-Fi. Effectuer ces réglages à l’arrêt.")
            .setPositiveButton("Ouvrir les paramètres") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:com.google.android.projection.gearhead"))
                runCatching { startActivity(intent) }.onFailure { Toast.makeText(this, "Ouvrez Android Auto depuis les paramètres du téléphone.", Toast.LENGTH_LONG).show() }
            }.setNegativeButton("Fermer", null).show()
    }

    override fun onStart() {
        super.onStart()
        val future = MediaBrowser.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java)))
            .setConnectionHints(Bundle().apply { putBoolean("phone_ui", true) })
            .setListener(object : MediaBrowser.Listener {
                override fun onChildrenChanged(browser: MediaBrowser, parentId: String, itemCount: Int, params: MediaLibraryService.LibraryParams?) { if (parentId == parent) load() }
            }).buildAsync()
        connection = future
        future.addListener({
            if (connection !== future) return@addListener
            runCatching { future.get() }.onSuccess { b ->
                browser = b
                b.addListener(object : Player.Listener { override fun onEvents(player: Player, events: Player.Events) { updatePlayback() } })
                open(parent); updatePlayback()
            }.onFailure { status.text = "Connexion impossible. Fermez puis rouvrez l’application." }
        }, uiExecutor)
        ticker = scope.launch { while (isActive) { delay(500); updatePlayback() } }
    }
    private fun open(id: String) {
        rememberScroll()
        browser?.unsubscribe(parent)
        parent = id
        rows = emptyList(); adapter.notifyDataSetChanged()
        heading.text = if (id == "root") "Émissions CNEWS" else "Épisodes récents"
        jumps.visibility = if (id == "root") View.GONE else View.VISIBLE
        status.text = "Chargement…"
        browser?.subscribe(id, null)
        load()
    }
    private fun startEpisode(item: MediaItem, restart: Boolean = false) {
        browser?.run {
            setMediaItem(MediaItem.Builder().setMediaId(item.mediaId).build(), if (restart) 0L else C.TIME_UNSET)
            prepare(); play()
        }
    }
    private fun episodeMenu(item: MediaItem) {
        val completed = history.isCompleted(item.mediaId)
        AlertDialog.Builder(this).setTitle(item.mediaMetadata.title)
            .setItems(arrayOf(if (completed) "Marquer comme non écouté" else "Marquer comme écouté", "Recommencer au début")) { _, which ->
                if (which == 0) { history.setCompleted(item.mediaId, !completed); load() } else startEpisode(item, restart = true)
            }.show()
    }
    private fun load() {
        val b = browser ?: return
        val request = ++generation
        val future = b.getChildren(parent, 0, 60, null)
        future.addListener({
            if (request != generation || browser !== b) return@addListener
            runCatching { future.get() }.onSuccess { result ->
                if (result.resultCode != LibraryResult.RESULT_SUCCESS) { status.text = "Catalogue indisponible. Touchez Émissions et réessayez."; return@onSuccess }
                rememberScroll()
                rows = result.value?.toList() ?: emptyList()
                adapter.notifyDataSetChanged()
                anchors[parent]?.let { anchor ->
                    val index = rows.indexOfFirst { it.mediaId == anchor.id }.takeIf { it >= 0 } ?: anchor.index.coerceIn(0, maxOf(0, rows.lastIndex))
                    list.setSelectionFromTop(index, anchor.top)
                }
                updatePlayback()
            }.onFailure { status.text = "Impossible de charger les épisodes." }
        }, uiExecutor)
    }
    private inner class EpisodeAdapter : BaseAdapter() {
        override fun getCount() = rows.size
        override fun getItem(position: Int) = rows[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, group: ViewGroup): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(6), dp(12), dp(4), dp(12)); minimumHeight = dp(72)
                val texts = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; addView(label(18f)); addView(label(13f, Color.LTGRAY)) }
                addView(texts, LinearLayout.LayoutParams(0,-2,1f))
                addView(button("Reprendre") {}, LinearLayout.LayoutParams(dp(108),dp(48)))
            }
            val item = getItem(position)
            val texts = row.getChildAt(0) as LinearLayout
            val completed = history.isCompleted(item.mediaId)
            (texts.getChildAt(0) as TextView).apply { text = item.mediaMetadata.title; setTextColor(if (completed) Color.rgb(155,160,170) else Color.WHITE) }
            (texts.getChildAt(1) as TextView).apply { text = item.mediaMetadata.subtitle; visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE }
            (row.getChildAt(1) as Button).apply {
                visibility = if (item.mediaMetadata.isPlayable == true && history.position(item.mediaId) > 0) View.VISIBLE else View.GONE
                setOnClickListener { startEpisode(item) }
            }
            row.setOnClickListener { if (item.mediaMetadata.isBrowsable == true) open(item.mediaId) else startEpisode(item) }
            row.setOnLongClickListener { if (item.mediaMetadata.isPlayable == true) { episodeMenu(item); true } else false }
            return row
        }
    }
    private fun updatePlayback() {
        if (!::play.isInitialized) return
        val b = browser
        val selected = b?.currentMediaItem != null
        play.isEnabled = selected; pause.isEnabled = selected
        val seekable = selected && b!!.isCurrentMediaItemSeekable && b.duration > 0
        back.isEnabled = seekable; forward.isEnabled = seekable; progress.isEnabled = seekable
        if (!dragging) {
            val duration = b?.duration?.takeIf { it > 0 } ?: b?.mediaMetadata?.durationMs ?: 0
            val position = b?.currentPosition?.coerceAtLeast(0) ?: 0
            progress.progress = if (duration > 0) (position.toDouble() / duration * 1000).toInt().coerceIn(0,1000) else 0
            time.text = "${PlaybackProgress.clock(position)} / ${if (duration > 0) PlaybackProgress.clock(duration) else "—"}"
        }
        artwork.load(b?.mediaMetadata?.artworkUri?.toString(), cover)
        if (b == null) return
        if (selected) status.text = when {
            b.playerError != null -> "Lecture indisponible. Vérifiez Internet puis appuyez sur ▶."
            b.playbackState == Player.STATE_BUFFERING -> "Chargement · ${b.mediaMetadata.title ?: "Podcast"}"
            b.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE -> "Pause temporaire · navigation ou appel"
            else -> b.mediaMetadata.title
        } else if (rows.isNotEmpty()) status.text = "Choisissez un épisode. Appui long : options d’écoute."
    }
    override fun onSaveInstanceState(outState: Bundle) {
        rememberScroll()
        outState.putString("parent", parent)
        anchors[parent]?.let { outState.putString("scroll_id", it.id); outState.putInt("scroll_index", it.index); outState.putInt("scroll_top", it.top) }
        super.onSaveInstanceState(outState)
    }
    @Deprecated("Legacy back supported on API 26+")
    override fun onBackPressed() { if (parent != "root") open("root") else super.onBackPressed() }
    override fun onStop() { rememberScroll(); ticker?.cancel(); generation++; connection?.let(MediaController::releaseFuture); connection = null; browser = null; super.onStop() }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
