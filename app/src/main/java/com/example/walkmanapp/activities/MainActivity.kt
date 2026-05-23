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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

    private fun replaceFragment(fragment: Fragment) {

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}