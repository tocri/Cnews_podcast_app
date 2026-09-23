package fr.perso.cnewsauto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ArtworkLoader(private val scope: CoroutineScope) {
    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val slots = Semaphore(2)
    fun load(url: String?, view: ImageView) {
        if (view.tag == url) return
        view.tag = url
        view.setImageResource(R.drawable.ic_podcast)
        if (url == null || !RssParser.validAudio(url)) return
        cache.get(url)?.let { view.setImageBitmap(it); return }
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                slots.withPermit {
                    try {
                        cache.get(url) ?: client.newCall(Request.Builder().url(url).build()).execute().use responseBlock@ { response ->
                            if (!response.isSuccessful) return@responseBlock null
                            val bytes = response.body?.byteStream()?.use streamBlock@ { input ->
                                val output = java.io.ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                while (true) { val n = input.read(buffer); if (n < 0) break; if (output.size() + n > 5_000_000) return@streamBlock null; output.write(buffer, 0, n) }
                                output.toByteArray()
                            } ?: return@responseBlock null
                            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                            if (options.outWidth <= 0 || options.outHeight <= 0) return@responseBlock null
                            options.inSampleSize = 1
                            while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) options.inSampleSize *= 2
                            options.inJustDecodeBounds = false
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.also { cache.put(url, it) }
                        }
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { null }
                }
            }
            if (view.tag == url && bitmap != null) view.setImageBitmap(bitmap)
        }
    }
}
