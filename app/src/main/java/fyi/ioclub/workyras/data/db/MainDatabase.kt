/*
 * Copyright (C) 2025 IO Club
 *
 * This file is part of Workyras.
 *
 * Workyras is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Workyras is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with Workyras.  If not, see <https://www.gnu.org/licenses/>.
 */

package fyi.ioclub.workyras.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import fyi.ioclub.workyras.data.db.dao.WorkPointDao
import fyi.ioclub.workyras.data.db.dao.WorkTagDao
import fyi.ioclub.workyras.data.db.entities.WorkPointEntity
import fyi.ioclub.workyras.data.db.entities.WorkTagEntity
import fyi.ioclub.workyras.ui.common.ToLateInit

@Database(
    entities = [WorkPointEntity::class, WorkTagEntity::class],
    version = 7,
    exportSchema = true
)
abstract class MainDatabase : RoomDatabase() {

    abstract fun workPointDao(): WorkPointDao
    abstract fun workTagDao(): WorkTagDao

    companion object : ToLateInit<Context> {

        private lateinit var applicationContext: Context

        val INSTANCE: MainDatabase by lazy {
            Room.databaseBuilder(
                applicationContext,
                MainDatabase::class.java,
                "main"
            )
//                .fallbackToDestructiveMigration()
                .build()
        }

        override val toLateInit: Boolean get() = !::applicationContext.isInitialized

        /** @param param application context. */
        override fun lateInit(param: Context) = param.let(::applicationContext::set)
    }
}
