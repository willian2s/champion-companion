package br.com.mwconsulting.championcompanion.core.database

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection

/** Migration manual: preserva todas as linhas v5 e torna proveniência de import opcional na atividade. */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execute("ALTER TABLE workout_activities RENAME TO workout_activities_v5")
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_activities (
                id TEXT NOT NULL,
                origin TEXT NOT NULL,
                deviceId TEXT,
                parserValidity TEXT,
                importedAtEpochMillis INTEGER,
                parserVersion TEXT,
                diagnosticPayloadId TEXT,
                startedAtEpochMillis INTEGER NOT NULL,
                durationNanos INTEGER NOT NULL,
                steps INTEGER NOT NULL,
                caloriesKilocalories REAL,
                originalDistanceMeters REAL,
                estimatedDistanceMeters REAL,
                calibratedDistanceMeters REAL,
                importedAveragePaceSecondsPerKilometer REAL,
                importedAverageSpeedMetersPerSecond REAL,
                calibratedAveragePaceSecondsPerKilometer REAL,
                calibratedAverageSpeedMetersPerSecond REAL,
                averageBpm INTEGER,
                minimumBpm INTEGER,
                maximumBpm INTEGER,
                suggestedType TEXT NOT NULL,
                title TEXT,
                notes TEXT,
                confirmedType TEXT,
                workflowState TEXT NOT NULL,
                reviewedAtEpochMillis INTEGER,
                calibrationId TEXT,
                appWorkoutSessionId TEXT,
                recordedAtEpochMillis INTEGER NOT NULL,
                endedAtEpochMillis INTEGER NOT NULL,
                stepsAvailable INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(deviceId) REFERENCES devices(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(diagnosticPayloadId) REFERENCES diagnostic_payloads(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        connection.execute(
            """
            INSERT INTO workout_activities (
                id, origin, deviceId, parserValidity, importedAtEpochMillis, parserVersion,
                diagnosticPayloadId, startedAtEpochMillis, durationNanos, steps,
                caloriesKilocalories, originalDistanceMeters, estimatedDistanceMeters,
                calibratedDistanceMeters, importedAveragePaceSecondsPerKilometer,
                importedAverageSpeedMetersPerSecond, calibratedAveragePaceSecondsPerKilometer,
                calibratedAverageSpeedMetersPerSecond, averageBpm, minimumBpm, maximumBpm,
                suggestedType, title, notes, confirmedType, workflowState, reviewedAtEpochMillis,
                calibrationId, appWorkoutSessionId, recordedAtEpochMillis, endedAtEpochMillis,
                stepsAvailable
            )
            SELECT
                id, origin, deviceId, parserValidity, importedAtEpochMillis, parserVersion,
                diagnosticPayloadId, startedAtEpochMillis, durationNanos, steps,
                caloriesKilocalories, originalDistanceMeters, estimatedDistanceMeters,
                calibratedDistanceMeters, importedAveragePaceSecondsPerKilometer,
                importedAverageSpeedMetersPerSecond, calibratedAveragePaceSecondsPerKilometer,
                calibratedAverageSpeedMetersPerSecond, averageBpm, minimumBpm, maximumBpm,
                suggestedType, title, notes, confirmedType, workflowState, reviewedAtEpochMillis,
                calibrationId, NULL, importedAtEpochMillis,
                startedAtEpochMillis + durationNanos / 1000000, 1
            FROM workout_activities_v5
            """.trimIndent(),
        )

        rebuildWatchWorkoutImports(connection)
        rebuildWorkoutIntervals(connection)
        rebuildWorkoutHeartRateSamples(connection)
        rebuildTreadmillCalibrations(connection)

        connection.execute("ALTER TABLE app_workout_sessions RENAME TO app_workout_sessions_v5")
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS app_workout_sessions (
                id TEXT NOT NULL,
                type TEXT NOT NULL,
                createdAtEpochMillis INTEGER NOT NULL,
                startedAtEpochMillis INTEGER,
                endedAtEpochMillis INTEGER,
                activeDurationNanos INTEGER NOT NULL,
                pausedDurationNanos INTEGER NOT NULL,
                activeSinceEpochMillis INTEGER,
                pausedSinceEpochMillis INTEGER,
                state TEXT NOT NULL,
                steps INTEGER NOT NULL,
                distanceMeters REAL,
                distanceAbsenceReason TEXT,
                caloriesKilocalories REAL,
                caloriesAbsenceReason TEXT,
                averageBpm INTEGER,
                averageBpmAbsenceReason TEXT,
                minimumBpm INTEGER,
                minimumBpmAbsenceReason TEXT,
                maximumBpm INTEGER,
                maximumBpmAbsenceReason TEXT,
                workoutActivityId TEXT,
                lastTransitionAtEpochMillis INTEGER NOT NULL,
                associatedDeviceId TEXT,
                revision INTEGER NOT NULL,
                runtimeGeneration INTEGER NOT NULL,
                stepsAvailable INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(workoutActivityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(associatedDeviceId) REFERENCES devices(id) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        connection.execute(
            """
            INSERT INTO app_workout_sessions (
                id, type, createdAtEpochMillis, startedAtEpochMillis, endedAtEpochMillis,
                activeDurationNanos, pausedDurationNanos, activeSinceEpochMillis,
                pausedSinceEpochMillis, state, steps, distanceMeters, distanceAbsenceReason,
                caloriesKilocalories, caloriesAbsenceReason,
                averageBpm, averageBpmAbsenceReason, minimumBpm, minimumBpmAbsenceReason,
                maximumBpm, maximumBpmAbsenceReason, workoutActivityId,
                lastTransitionAtEpochMillis, associatedDeviceId, revision, runtimeGeneration,
                stepsAvailable
            )
            SELECT
                id, type, createdAtEpochMillis, startedAtEpochMillis,
                CASE WHEN state = 'DISCARDED' THEN
                    MAX(
                        createdAtEpochMillis,
                        COALESCE(startedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(endedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(activeSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(pausedSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(startedAtEpochMillis, createdAtEpochMillis) +
                            activeDurationNanos / 1000000 +
                            pausedDurationNanos / 1000000 +
                            ((activeDurationNanos % 1000000 + pausedDurationNanos % 1000000 + 999999) / 1000000)
                    )
                ELSE endedAtEpochMillis END,
                activeDurationNanos, pausedDurationNanos, activeSinceEpochMillis,
                pausedSinceEpochMillis, state, steps, distanceMeters,
                CASE WHEN distanceMeters IS NULL THEN 'NOT_PROVIDED_BY_SOURCE' END,
                caloriesKilocalories,
                CASE WHEN caloriesKilocalories IS NULL THEN 'NOT_PROVIDED_BY_SOURCE' END,
                averageBpm,
                CASE WHEN averageBpm IS NULL THEN 'NOT_PROVIDED_BY_SOURCE' END,
                minimumBpm,
                CASE WHEN minimumBpm IS NULL THEN 'NOT_PROVIDED_BY_SOURCE' END,
                maximumBpm,
                CASE WHEN maximumBpm IS NULL THEN 'NOT_PROVIDED_BY_SOURCE' END,
                workoutActivityId,
                CASE WHEN state = 'DISCARDED' THEN
                    MAX(
                        createdAtEpochMillis,
                        COALESCE(startedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(endedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(activeSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(pausedSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(startedAtEpochMillis, createdAtEpochMillis) +
                            activeDurationNanos / 1000000 +
                            pausedDurationNanos / 1000000 +
                            ((activeDurationNanos % 1000000 + pausedDurationNanos % 1000000 + 999999) / 1000000)
                    )
                ELSE
                    MAX(
                        createdAtEpochMillis,
                        COALESCE(startedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(endedAtEpochMillis, createdAtEpochMillis),
                        COALESCE(activeSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(pausedSinceEpochMillis, createdAtEpochMillis),
                        COALESCE(
                            startedAtEpochMillis + (activeDurationNanos + pausedDurationNanos) / 1000000,
                            createdAtEpochMillis
                        )
                    )
                END,
                NULL,
                0,
                CASE WHEN startedAtEpochMillis IS NULL THEN 0 ELSE 1 END,
                1
            FROM app_workout_sessions_v5
            """.trimIndent(),
        )

        rebuildHeartRateMeasurements(connection)

        connection.execute("DROP TABLE app_workout_sessions_v5")
        connection.execute("DROP TABLE workout_activities_v5")

        connection.execute("CREATE INDEX IF NOT EXISTS index_workout_activities_deviceId ON workout_activities(deviceId)")
        connection.execute("CREATE INDEX IF NOT EXISTS index_workout_activities_diagnosticPayloadId ON workout_activities(diagnosticPayloadId)")
        connection.execute("CREATE INDEX IF NOT EXISTS index_workout_activities_workflowState ON workout_activities(workflowState)")
        connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_activities_appWorkoutSessionId ON workout_activities(appWorkoutSessionId)")
        connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_app_workout_sessions_workoutActivityId ON app_workout_sessions(workoutActivityId)")
        connection.execute("CREATE INDEX IF NOT EXISTS index_app_workout_sessions_state ON app_workout_sessions(state)")

        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_routes (
                id TEXT NOT NULL,
                sessionId TEXT NOT NULL,
                workoutType TEXT NOT NULL,
                state TEXT NOT NULL,
                startedAtEpochMillis INTEGER NOT NULL,
                endedAtEpochMillis INTEGER,
                acceptedPointCount INTEGER NOT NULL,
                segmentCount INTEGER NOT NULL,
                distanceMeters REAL NOT NULL,
                averageHorizontalAccuracyMeters REAL,
                revision INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(sessionId) REFERENCES app_workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_routes_sessionId ON workout_routes(sessionId)")
        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_route_points (
                id TEXT NOT NULL,
                routeId TEXT NOT NULL,
                sequence INTEGER NOT NULL,
                sampledAtEpochMillis INTEGER NOT NULL,
                latitudeDegrees REAL NOT NULL,
                longitudeDegrees REAL NOT NULL,
                altitudeMeters REAL,
                horizontalAccuracyMeters REAL NOT NULL,
                verticalAccuracyMeters REAL,
                speedMetersPerSecond REAL,
                bearingDegrees REAL,
                segmentIndex INTEGER NOT NULL,
                segmentStartReason TEXT,
                segmentStartedAtEpochMillis INTEGER,
                segmentDistanceMeters REAL,
                PRIMARY KEY(id),
                FOREIGN KEY(routeId) REFERENCES workout_routes(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_route_points_routeId_sequence " +
                "ON workout_route_points(routeId, sequence)",
        )
    }
}

/** Permite múltiplos deltas de passos e congela o payload técnico de cada operação. */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execute(
            "ALTER TABLE health_sync_operations ADD COLUMN operationSequence INTEGER NOT NULL DEFAULT 0",
        )
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN dailyStepsPayloadKind TEXT")
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN previousTotalSteps INTEGER")
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN currentTotalSteps INTEGER")
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN stepDelta INTEGER")
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN stepStartTimeEpochMillis INTEGER")
        connection.execute("ALTER TABLE health_sync_operations ADD COLUMN stepEndTimeEpochMillis INTEGER")
        connection.execute("DROP INDEX IF EXISTS index_health_sync_operations_aggregateId_recordType")
        connection.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "index_health_sync_operations_aggregateId_recordType_operationSequence " +
                "ON health_sync_operations(aggregateId, recordType, operationSequence)",
        )
    }
}

/** Restaura ordem temporal de atividades reimportadas já revisadas. */
val MIGRATION_7_8: Migration = object : Migration(7, 8) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execute(
            "UPDATE workout_activities SET recordedAtEpochMillis = reviewedAtEpochMillis " +
                "WHERE reviewedAtEpochMillis IS NOT NULL " +
                "AND reviewedAtEpochMillis >= startedAtEpochMillis " +
                "AND reviewedAtEpochMillis < recordedAtEpochMillis",
        )
    }
}

/** Migration manual aditiva: preserva legado e prepara fatos raw/measured/derived locais. */
val MIGRATION_8_9: Migration = object : Migration(8, 9) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execute("ALTER TABLE user_profiles ADD COLUMN stepLengthMeters REAL")
        connection.execute("ALTER TABLE user_profiles ADD COLUMN stepLengthSource TEXT")
        connection.execute("ALTER TABLE user_profiles ADD COLUMN stepLengthUpdatedAtEpochMillis INTEGER")

        connection.execute("ALTER TABLE workout_intervals ADD COLUMN rawMotionValue INTEGER")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN rawDistanceValue INTEGER")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN rawCaloriesValue INTEGER")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN decodedSteps INTEGER")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN effectiveDurationNanos INTEGER")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN temporalEligibilityReason TEXT")
        connection.execute("ALTER TABLE workout_intervals ADD COLUMN plausibilityStatus TEXT")

        // d4-v1 persisted the packed u24 in the legacy steps column. This bridge
        // copies only known watch imports; it deliberately does not decode it.
        connection.execute(
            """
            UPDATE workout_intervals
            SET rawMotionValue = steps
            WHERE rawMotionValue IS NULL
              AND steps BETWEEN 0 AND 16777215
              AND EXISTS (
                  SELECT 1
                  FROM workout_activities activity
                  INNER JOIN watch_workout_imports importRecord
                      ON importRecord.activityId = activity.id
                  WHERE activity.id = workout_intervals.activityId
                    AND activity.origin = 'WATCH_D3_D4'
                    AND importRecord.origin = 'WATCH_D3_D4'
                    AND importRecord.parserVersion = 'd4-v1'
              )
            """.trimIndent(),
        )

        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_derivation_snapshots (
                id TEXT NOT NULL,
                activityId TEXT NOT NULL,
                stepLengthMetersUsed REAL NOT NULL,
                stepLengthSource TEXT NOT NULL,
                derivationMethod TEXT NOT NULL,
                derivationVersion TEXT NOT NULL,
                derivedAtEpochMillis INTEGER NOT NULL,
                durationCase TEXT NOT NULL,
                totalDurationNanos INTEGER NOT NULL,
                nominalIntervalDurationNanos INTEGER NOT NULL,
                tailUncoveredNanos INTEGER NOT NULL,
                temporalEligibility TEXT NOT NULL,
                temporalEligibilityReasons TEXT,
                plausibilityPolicy TEXT NOT NULL,
                plausibilityIndicators TEXT,
                canonicalDistanceMeters REAL,
                canonicalCoveredDurationNanos INTEGER,
                diagnosticAggregateTotalSteps INTEGER,
                diagnosticAggregateStepLengthMeters REAL,
                diagnosticAggregateDistanceMeters REAL,
                derivedAtEpochSecond INTEGER NOT NULL,
                derivedAtNano INTEGER NOT NULL,
                activeActivityId TEXT,
                isActive INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(id),
                FOREIGN KEY(activityId) REFERENCES workout_activities(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execute(
            "CREATE INDEX IF NOT EXISTS index_workout_derivation_snapshots_activityId " +
                "ON workout_derivation_snapshots(activityId)",
        )
        connection.execute(
            "CREATE INDEX IF NOT EXISTS index_workout_derivation_snapshots_activityId_derivedAtEpochMillis " +
                "ON workout_derivation_snapshots(activityId, derivedAtEpochMillis)",
        )
        connection.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_derivation_snapshots_activeActivityId " +
                "ON workout_derivation_snapshots(activeActivityId)",
        )

        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_derived_intervals (
                id TEXT NOT NULL,
                snapshotId TEXT NOT NULL,
                intervalIndex INTEGER NOT NULL,
                effectiveStartEpochMillis INTEGER NOT NULL,
                effectiveStartEpochSecond INTEGER NOT NULL,
                effectiveStartNano INTEGER NOT NULL,
                effectiveDurationNanos INTEGER NOT NULL,
                decodedSteps INTEGER NOT NULL,
                canonicalIntervalDistanceMeters REAL NOT NULL,
                cadenceStepsPerMinute REAL NOT NULL,
                speedMetersPerSecond REAL NOT NULL,
                paceSecondsPerKilometer REAL,
                cumulativeSteps INTEGER NOT NULL,
                cumulativeDistanceMeters REAL NOT NULL,
                cumulativeDurationNanos INTEGER NOT NULL,
                cumulativeCadenceStepsPerMinute REAL NOT NULL,
                cumulativeSpeedMetersPerSecond REAL NOT NULL,
                cumulativePaceSecondsPerKilometer REAL,
                speedSampledAtEpochMillis INTEGER NOT NULL,
                speedSampledAtEpochSecond INTEGER NOT NULL,
                speedSampledAtNano INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(snapshotId) REFERENCES workout_derivation_snapshots(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_derived_intervals_snapshotId_intervalIndex " +
                "ON workout_derived_intervals(snapshotId, intervalIndex)",
        )

        connection.execute(
            """
            CREATE TABLE IF NOT EXISTS workout_splits (
                id TEXT NOT NULL,
                snapshotId TEXT NOT NULL,
                splitNumber INTEGER NOT NULL,
                targetDistanceMeters REAL NOT NULL,
                windowStartEpochMillis INTEGER NOT NULL,
                windowStartEpochSecond INTEGER NOT NULL,
                windowStartNano INTEGER NOT NULL,
                windowEndEpochMillis INTEGER NOT NULL,
                windowEndEpochSecond INTEGER NOT NULL,
                windowEndNano INTEGER NOT NULL,
                distanceMeters REAL NOT NULL,
                durationNanos INTEGER NOT NULL,
                averageCadenceStepsPerMinute REAL NOT NULL,
                averageSpeedMetersPerSecond REAL NOT NULL,
                averagePaceSecondsPerKilometer REAL,
                averageBpm REAL,
                bpmCoveredDurationNanos INTEGER NOT NULL,
                PRIMARY KEY(id),
                FOREIGN KEY(snapshotId) REFERENCES workout_derivation_snapshots(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        connection.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_workout_splits_snapshotId_splitNumber " +
                "ON workout_splits(snapshotId, splitNumber)",
        )
    }
}

private fun rebuildWatchWorkoutImports(connection: SQLiteConnection) {
    connection.execute("ALTER TABLE watch_workout_imports RENAME TO watch_workout_imports_v5")
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS watch_workout_imports (
            id TEXT NOT NULL, activityId TEXT NOT NULL, deviceId TEXT NOT NULL,
            origin TEXT NOT NULL, crc INTEGER NOT NULL, externalBlockIndex INTEGER NOT NULL,
            parserVersion TEXT NOT NULL, parserValidity TEXT NOT NULL,
            diagnosticPayloadId TEXT, importedAtEpochMillis INTEGER NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(activityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(deviceId) REFERENCES devices(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
            FOREIGN KEY(diagnosticPayloadId) REFERENCES diagnostic_payloads(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    connection.execute("INSERT INTO watch_workout_imports SELECT * FROM watch_workout_imports_v5")
    connection.execute("DROP TABLE watch_workout_imports_v5")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_workout_imports_activityId ON watch_workout_imports(activityId)")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_watch_workout_imports_deviceId_origin_crc ON watch_workout_imports(deviceId, origin, crc)")
    connection.execute("CREATE INDEX IF NOT EXISTS index_watch_workout_imports_diagnosticPayloadId ON watch_workout_imports(diagnosticPayloadId)")
}

private fun rebuildWorkoutIntervals(connection: SQLiteConnection) {
    connection.execute("ALTER TABLE workout_intervals RENAME TO workout_intervals_v5")
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS workout_intervals (
            id TEXT NOT NULL, activityId TEXT NOT NULL, intervalIndex INTEGER NOT NULL,
            durationNanos INTEGER NOT NULL, steps INTEGER NOT NULL, distanceMeters REAL,
            caloriesKilocalories REAL, PRIMARY KEY(id),
            FOREIGN KEY(activityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    connection.execute("INSERT INTO workout_intervals SELECT * FROM workout_intervals_v5")
    connection.execute("DROP TABLE workout_intervals_v5")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_intervals_activityId_intervalIndex ON workout_intervals(activityId, intervalIndex)")
}

private fun rebuildWorkoutHeartRateSamples(connection: SQLiteConnection) {
    connection.execute("ALTER TABLE workout_heart_rate_samples RENAME TO workout_heart_rate_samples_v5")
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS workout_heart_rate_samples (
            id TEXT NOT NULL, activityId TEXT NOT NULL, sampledAtEpochMillis INTEGER NOT NULL,
            bpm INTEGER NOT NULL, PRIMARY KEY(id),
            FOREIGN KEY(activityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    connection.execute("INSERT INTO workout_heart_rate_samples SELECT * FROM workout_heart_rate_samples_v5")
    connection.execute("DROP TABLE workout_heart_rate_samples_v5")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_workout_heart_rate_samples_activityId_sampledAtEpochMillis ON workout_heart_rate_samples(activityId, sampledAtEpochMillis)")
}

private fun rebuildTreadmillCalibrations(connection: SQLiteConnection) {
    connection.execute("ALTER TABLE treadmill_calibrations RENAME TO treadmill_calibrations_v5")
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS treadmill_calibrations (
            id TEXT NOT NULL, activityId TEXT NOT NULL, revision INTEGER NOT NULL,
            originalDistanceMeters REAL, estimatedDistanceMeters REAL, reportedDistanceMeters REAL,
            calibratedDistanceMeters REAL, factor REAL, factorAbsenceReason TEXT,
            appliedAtEpochMillis INTEGER NOT NULL, origin TEXT NOT NULL,
            averagePaceSecondsPerKilometer REAL, averageSpeedMetersPerSecond REAL,
            metricsProvenance TEXT NOT NULL, PRIMARY KEY(id),
            FOREIGN KEY(activityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
    )
    connection.execute("INSERT INTO treadmill_calibrations SELECT * FROM treadmill_calibrations_v5")
    connection.execute("DROP TABLE treadmill_calibrations_v5")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_treadmill_calibrations_activityId_revision ON treadmill_calibrations(activityId, revision)")
}

private fun rebuildHeartRateMeasurements(connection: SQLiteConnection) {
    connection.execute("ALTER TABLE heart_rate_measurements RENAME TO heart_rate_measurements_v5")
    connection.execute(
        """
        CREATE TABLE IF NOT EXISTS heart_rate_measurements (
            id TEXT NOT NULL, source TEXT NOT NULL, bpm INTEGER NOT NULL,
            measuredAtEpochMillis INTEGER, observedLocalDateTime TEXT, validity TEXT NOT NULL,
            deviceId TEXT, importedAtEpochMillis INTEGER, parserVersion TEXT,
            appWorkoutSessionId TEXT, workoutActivityId TEXT, deduplicationKey TEXT NOT NULL,
            PRIMARY KEY(id),
            FOREIGN KEY(deviceId) REFERENCES devices(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(appWorkoutSessionId) REFERENCES app_workout_sessions(id) ON UPDATE NO ACTION ON DELETE SET NULL,
            FOREIGN KEY(workoutActivityId) REFERENCES workout_activities(id) ON UPDATE NO ACTION ON DELETE SET NULL
        )
        """.trimIndent(),
    )
    connection.execute("INSERT INTO heart_rate_measurements SELECT * FROM heart_rate_measurements_v5")
    connection.execute("DROP TABLE heart_rate_measurements_v5")
    connection.execute("CREATE INDEX IF NOT EXISTS index_heart_rate_measurements_deviceId ON heart_rate_measurements(deviceId)")
    connection.execute("CREATE INDEX IF NOT EXISTS index_heart_rate_measurements_appWorkoutSessionId ON heart_rate_measurements(appWorkoutSessionId)")
    connection.execute("CREATE INDEX IF NOT EXISTS index_heart_rate_measurements_workoutActivityId ON heart_rate_measurements(workoutActivityId)")
    connection.execute("CREATE UNIQUE INDEX IF NOT EXISTS index_heart_rate_measurements_deduplicationKey ON heart_rate_measurements(deduplicationKey)")
}

private fun SQLiteConnection.execute(sql: String) {
    prepare(sql).use { statement -> statement.step() }
}
