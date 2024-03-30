package org.thingnes.hitster

import android.os.Bundle
import android.util.Log
import android.widget.Switch
import androidx.activity.ComponentActivity
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


private const val SPOTIFY_CLIENT_ID = "70ee3efe833341efa88d18f13f154c60"
private const val SPOTIFY_REDIRECT_URI = "http://org.thingnes.hitster/callback"

class MainActivity : ComponentActivity(), BarcodeCallback {
    private var spotifyRemote: SpotifyAppRemote? = null
    private var barcodeView: DecoratedBarcodeView? = null
    private var switchView: Switch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        barcodeView?.setStatusText("")

        switchView = findViewById(R.id.scanner_switch)
        switchView?.setOnCheckedChangeListener { view, _ ->
            Log.d("MainActivity", "Running onCheckedChangeListener")
            if (view.isChecked) {
                Log.d("MainActivity", "Running decodeSingle")
                barcodeView?.resume()
                barcodeView?.decodeSingle(this)
            } else {
                barcodeView?.pause()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SpotifyAppRemote.connect(
            this,
            ConnectionParams.Builder(SPOTIFY_CLIENT_ID)
                .setRedirectUri(SPOTIFY_REDIRECT_URI)
                .showAuthView(true)
                .build(),
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    this@MainActivity.spotifyRemote = appRemote
                    Log.d("MainActivity", "Successfully connected to app remote")
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("MainActivity", throwable.message, throwable)
                }
            })
    }

    override fun barcodeResult(result: BarcodeResult?) {
        Thread {
            result?.let {
                getSpotifyTrackId(result)?.let { id ->
                    Log.i("MainActivity", "Playing track $id")
                    spotifyRemote?.playerApi?.play("spotify:track:$id")
                }
            }

            switchView?.post {
                switchView?.isChecked = false
            }
        }.start()
    }

    private fun getSpotifyTrackId(result: BarcodeResult?): String? {
        val spotifyUrlRegex = Regex("track/(\\w+)\\?")

        return try {
            val url = URL(result?.text)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.instanceFollowRedirects = false

            val id = if (connection.responseCode in intArrayOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP
                )
            ) {
                val redirectUrl = connection.getHeaderField("Location")
                Log.d("MainActivity", "Redirected to $redirectUrl")
                spotifyUrlRegex.find(redirectUrl)?.groupValues?.get(1)
            } else {
                Log.d("MainActivity", "No redirect, using $url")
                spotifyUrlRegex.find(url.toString())?.groupValues?.get(1)
            }

            connection.disconnect()
            id
        } catch (e: MalformedURLException) {
            Log.d("MainActivity", "URL is malformed, skipping HTTP call")
            result?.text
        }
    }

    override fun onPause() {
        super.onPause()
        barcodeView?.pause()
    }

    override fun onResume() {
        super.onResume()
        barcodeView?.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView?.pauseAndWait()
    }

    override fun onStop() {
        super.onStop()
        spotifyRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }
}