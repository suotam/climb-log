package com.example.climblog.di

import android.content.Context
import androidx.room.Room
import com.example.climblog.data.local.ClimbLogDatabase
import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.AscentDao
import com.example.climblog.data.local.dao.OutdoorSessionDao
import com.example.climblog.data.local.dao.PhotoDao
import com.example.climblog.data.local.dao.RouteCommentDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.dao.WallDao
import com.example.climblog.data.local.dao.WishlistDao
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.data.repository.RouteCommentRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.data.repository.WallRepository
import com.example.climblog.data.repository.WishlistRepository
import com.example.climblog.data.repository.impl.AreaRepositoryImpl
import com.example.climblog.data.repository.impl.AscentRepositoryImpl
import com.example.climblog.data.repository.impl.OutdoorSessionRepositoryImpl
import com.example.climblog.data.repository.impl.PhotoRepositoryImpl
import com.example.climblog.data.repository.impl.RouteCommentRepositoryImpl
import com.example.climblog.data.repository.impl.RouteRepositoryImpl
import com.example.climblog.data.repository.impl.WallRepositoryImpl
import com.example.climblog.data.repository.impl.WishlistRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ClimbLogDatabase =
        Room.databaseBuilder(context, ClimbLogDatabase::class.java, "climblog.db")
            .addMigrations(ClimbLogDatabase.MIGRATION_5_6, ClimbLogDatabase.MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideAreaDao(db: ClimbLogDatabase): AreaDao = db.areaDao()
    @Provides fun provideSectorDao(db: ClimbLogDatabase): SectorDao = db.sectorDao()
    @Provides fun provideRouteDao(db: ClimbLogDatabase): RouteDao = db.routeDao()
    @Provides fun provideAscentDao(db: ClimbLogDatabase): AscentDao = db.ascentDao()
    @Provides fun providePhotoDao(db: ClimbLogDatabase): PhotoDao = db.photoDao()
    @Provides fun provideWishlistDao(db: ClimbLogDatabase): WishlistDao = db.wishlistDao()
    @Provides fun provideRouteCommentDao(db: ClimbLogDatabase): RouteCommentDao = db.routeCommentDao()
    @Provides fun provideWallDao(db: ClimbLogDatabase): WallDao = db.wallDao()
    @Provides fun provideOutdoorSessionDao(db: ClimbLogDatabase): OutdoorSessionDao = db.outdoorSessionDao()

    @Provides @Singleton
    fun provideAreaRepository(impl: AreaRepositoryImpl): AreaRepository = impl

    @Provides @Singleton
    fun provideRouteRepository(impl: RouteRepositoryImpl): RouteRepository = impl

    @Provides @Singleton
    fun provideAscentRepository(impl: AscentRepositoryImpl): AscentRepository = impl

    @Provides @Singleton
    fun providePhotoRepository(impl: PhotoRepositoryImpl): PhotoRepository = impl

    @Provides @Singleton
    fun provideWishlistRepository(impl: WishlistRepositoryImpl): WishlistRepository = impl

    @Provides @Singleton
    fun provideRouteCommentRepository(impl: RouteCommentRepositoryImpl): RouteCommentRepository = impl

    @Provides @Singleton
    fun provideWallRepository(impl: WallRepositoryImpl): WallRepository = impl

    @Provides @Singleton
    fun provideOutdoorSessionRepository(impl: OutdoorSessionRepositoryImpl): OutdoorSessionRepository = impl
}
