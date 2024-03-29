package org.thingnes.hitster

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote


private const val SPOTIFY_CLIENT_ID = "70ee3efe833341efa88d18f13f154c60"
private const val SPOTIFY_REDIRECT_URI = "http://org.thingnes.hitster/callback"

class MainActivity : ComponentActivity(), BarcodeCallback {
    private var spotifyRemote: SpotifyAppRemote? = null
    private var barcodeView: DecoratedBarcodeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        barcodeView?.setStatusText("")
        barcodeView?.decodeSingle(this)
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
                    Log.d("Spotify", "Successfully connected to app remote")
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("Spotify", throwable.message, throwable)
                }
            })
    }

    override fun barcodeResult(result: BarcodeResult?) {
        result?.let {
            spotifyRemote?.playerApi?.play("spotify:track:${it.text}")
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

    override fun onStop() {
        super.onStop()
        spotifyRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeView?.pauseAndWait()
    }
}