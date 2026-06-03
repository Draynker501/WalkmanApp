package com.example.walkmanapp

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    private var instance: WalkmanDatabase? = null

    fun getDatabase(
        context: Context
    ): WalkmanDatabase {

        return instance ?: synchronized(this) {

            Room.databaseBuilder(
                context,
                WalkmanDatabase::class.java,
                "walkman.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also {

                    instance = it
                }
        }
    }
}