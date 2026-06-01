package com.example.walkmanapp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.walkmanapp.fragments.AlbumsFragment
import com.example.walkmanapp.fragments.PlayerFragment
import com.example.walkmanapp.fragments.PlaylistsFragment
import com.example.walkmanapp.R
import com.example.walkmanapp.fragments.SongsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver.handleIntent

private val REQUEST_AUDIO_PERMISSION = 100

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handleIntent(intent)

        checkAudioPermission()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // fragment inicial
        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_player
            replaceFragment(PlayerFragment())
        }

        bottomNav.setOnItemSelectedListener {

            when(it.itemId) {

                R.id.nav_songs -> {
                    replaceFragment(SongsFragment())
                    true
                }

                R.id.nav_player -> {
                    replaceFragment(PlayerFragment())
                    true
                }

                R.id.nav_albums -> {
                    replaceFragment(AlbumsFragment())
                    true
                }

                R.id.nav_playlists -> {
                    replaceFragment(PlaylistsFragment())
                    true
                }

                else -> false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {

        val openPlayer =
            intent?.getBooleanExtra(
                "open_player",
                false
            ) ?: false

        if (openPlayer) {

            val bottomNav =
                findViewById<BottomNavigationView>(
                    R.id.bottomNavigation
                )

            bottomNav.selectedItemId =
                R.id.nav_player
        }
    }

    // Solicitar permiso de audio
    private fun checkAudioPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                    REQUEST_AUDIO_PERMISSION
                )
            }

        } else {

            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_AUDIO_PERMISSION
                )
            }
        }
    }

    // Cambiar fragment
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}