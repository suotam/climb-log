package com.example.climblog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.AscentDao
import com.example.climblog.data.local.dao.OutdoorSessionDao
import com.example.climblog.data.local.dao.PhotoDao
import com.example.climblog.data.local.dao.RouteCommentDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.dao.WallDao
import com.example.climblog.data.local.dao.WishlistDao
import com.example.climblog.data.local.entity.AreaEntity
import com.example.climblog.data.local.entity.AscentEntity
import com.example.climblog.data.local.entity.OutdoorSessionEntity
import com.example.climblog.data.local.entity.OutdoorSessionRouteEntity
import com.example.climblog.data.local.entity.PhotoEntity
import com.example.climblog.data.local.entity.RouteCommentEntity
import com.example.climblog.data.local.entity.RouteEntity
import com.example.climblog.data.local.entity.SectorEntity
import com.example.climblog.data.local.entity.WallEntity
import com.example.climblog.data.local.entity.WallGradeEntryEntity
import com.example.climblog.data.local.entity.WallSessionEntity
import com.example.climblog.data.local.entity.WishlistEntity

@Database(
    entities = [
        AreaEntity::class,
        SectorEntity::class,
        RouteEntity::class,
        AscentEntity::class,
        PhotoEntity::class,
        WishlistEntity::class,
        RouteCommentEntity::class,
        WallEntity::class,
        WallSessionEntity::class,
        WallGradeEntryEntity::class,
        OutdoorSessionEntity::class,
        OutdoorSessionRouteEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class ClimbLogDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sectors ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routes ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite can't add FK via ALTER TABLE — recreate ascents with full schema
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `ascents_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routeId` INTEGER NOT NULL,
                        `userId` TEXT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `style` TEXT NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `personalNote` TEXT NOT NULL,
                        `publicNote` TEXT NOT NULL,
                        `photoUri` TEXT,
                        `personalGrade` TEXT,
                        `rating` INTEGER,
                        `outdoorSessionId` INTEGER,
                        `syncStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`outdoorSessionId`) REFERENCES `outdoor_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `ascents_new` (`id`, `routeId`, `userId`, `date`, `style`, `attempts`, `personalNote`, `publicNote`, `photoUri`, `personalGrade`, `rating`, `outdoorSessionId`, `syncStatus`, `createdAt`, `updatedAt`)
                    SELECT `id`, `routeId`, `userId`, `date`, `style`, `attempts`, `personalNote`, `publicNote`, `photoUri`, `personalGrade`, `rating`, NULL, `syncStatus`, `createdAt`, `updatedAt`
                    FROM `ascents`
                """.trimIndent())
                db.execSQL("DROP TABLE `ascents`")
                db.execSQL("ALTER TABLE `ascents_new` RENAME TO `ascents`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ascents_routeId` ON `ascents` (`routeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ascents_outdoorSessionId` ON `ascents` (`outdoorSessionId`)")
                // Migrate existing outdoor_session_routes to full ascents
                db.execSQL("""
                    INSERT INTO `ascents` (`routeId`, `userId`, `date`, `style`, `attempts`, `personalNote`, `publicNote`, `outdoorSessionId`, `syncStatus`, `createdAt`, `updatedAt`)
                    SELECT r.`routeId`, 'local_user', s.`date`, r.`style`, 1, '', '', r.`sessionId`, 'LOCAL', s.`createdAt`, s.`createdAt`
                    FROM `outdoor_session_routes` r
                    JOIN `outdoor_sessions` s ON r.`sessionId` = s.`id`
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create outdoor_sessions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `outdoor_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `areaId` INTEGER,
                        `customCragName` TEXT,
                        `notes` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                // Create outdoor_session_routes table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `outdoor_session_routes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `routeId` INTEGER NOT NULL,
                        `style` TEXT NOT NULL DEFAULT 'REDPOINT',
                        FOREIGN KEY(`sessionId`) REFERENCES `outdoor_sessions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outdoor_session_routes_sessionId` ON `outdoor_session_routes` (`sessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_outdoor_session_routes_routeId` ON `outdoor_session_routes` (`routeId`)")
                // Recreate photos table to add outdoorSessionId with FK (SQLite can't add FK via ALTER TABLE)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `photos_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `areaId` INTEGER,
                        `routeId` INTEGER,
                        `ascentId` INTEGER,
                        `outdoorSessionId` INTEGER,
                        `uri` TEXT NOT NULL,
                        `caption` TEXT,
                        `takenAt` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`areaId`) REFERENCES `areas`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`routeId`) REFERENCES `routes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`ascentId`) REFERENCES `ascents`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`outdoorSessionId`) REFERENCES `outdoor_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `photos_new` (`id`, `areaId`, `routeId`, `ascentId`, `outdoorSessionId`, `uri`, `caption`, `takenAt`, `syncStatus`, `createdAt`)
                    SELECT `id`, `areaId`, `routeId`, `ascentId`, NULL, `uri`, `caption`, `takenAt`, `syncStatus`, `createdAt` FROM `photos`
                """.trimIndent())
                db.execSQL("DROP TABLE `photos`")
                db.execSQL("ALTER TABLE `photos_new` RENAME TO `photos`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_areaId` ON `photos` (`areaId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_routeId` ON `photos` (`routeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_ascentId` ON `photos` (`ascentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_photos_outdoorSessionId` ON `photos` (`outdoorSessionId`)")
            }
        }
    }

    abstract fun areaDao(): AreaDao
    abstract fun sectorDao(): SectorDao
    abstract fun routeDao(): RouteDao
    abstract fun ascentDao(): AscentDao
    abstract fun photoDao(): PhotoDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun routeCommentDao(): RouteCommentDao
    abstract fun wallDao(): WallDao
    abstract fun outdoorSessionDao(): OutdoorSessionDao
}
