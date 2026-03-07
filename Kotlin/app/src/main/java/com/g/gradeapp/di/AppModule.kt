// app/src/main/java/com/g/gradeapp/di/AppModule.kt
package com.g.gradeapp.di

import android.content.Context
import androidx.room.Room
import com.g.gradeapp.data.local.GradeDatabase
import com.g.gradeapp.data.local.SessionDao
import com.g.gradeapp.data.local.StudentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): GradeDatabase =
        Room.databaseBuilder(ctx, GradeDatabase::class.java, GradeDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideStudentDao(db: GradeDatabase): StudentDao = db.studentDao()

    @Provides @Singleton
    fun provideSessionDao(db: GradeDatabase): SessionDao = db.sessionDao()
}
