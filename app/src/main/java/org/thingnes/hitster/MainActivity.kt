package org.thingnes.hitster

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import org.thingnes.hitster.ui.theme.HitsterTheme

private const val CLIENT_ID = "70ee3efe833341efa88d18f13f154c60"
private const val REDIRECT_URI = "http://org.thingnes.hitster/callback"

class MainActivity : ComponentActivity() {
    private var remote: SpotifyAppRemote? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HitsterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Button(onClick = {
                        remote?.playerApi?.play("spotify:track:5iFhkFf9JLA3XiROdYsg1i")
                    }) {
                        Text("Play music")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SpotifyAppRemote.connect(
            this,
            ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build(),
            object : Connector.ConnectionListener {
                override fun onConnected(appRemote: SpotifyAppRemote) {
                    remote = appRemote
                    Log.d("Spotify", "Successfully connected to app remote")
                }

                override fun onFailure(throwable: Throwable) {
                    Log.e("Spotify", throwable.message, throwable)
                }
            })
    }

    override fun onStop() {
        super.onStop()
        remote?.let {
            SpotifyAppRemote.disconnect(it)
        }
    }
}