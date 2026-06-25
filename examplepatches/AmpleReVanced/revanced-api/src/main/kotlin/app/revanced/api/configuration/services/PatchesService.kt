package app.revanced.api.configuration.services

import app.revanced.api.configuration.ApiAssetPublicKey
import app.revanced.api.configuration.ApiRelease
import app.revanced.api.configuration.ApiReleaseVersion
import app.revanced.api.configuration.repository.BackendRepository
import app.revanced.api.configuration.repository.BackendRepository.BackendOrganization.BackendRepository.BackendRelease.Companion.first
import app.revanced.api.configuration.repository.ConfigurationRepository
import app.revanced.library.serializeTo
import app.revanced.patcher.patch.loadPatchesFromJar
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL

internal class PatchesService(
    private val backendRepository: BackendRepository,
    private val configurationRepository: ConfigurationRepository,
) {
    suspend fun latestRelease(prerelease: Boolean): ApiRelease {
        val patchesRelease = backendRepository.release(
            configurationRepository.organization,
            configurationRepository.patches.repository,
            prerelease,
        )

        return ApiRelease(
            patchesRelease.tag,
            patchesRelease.createdAt,
            patchesRelease.releaseNote,
            patchesRelease.assets.first(configurationRepository.patches.assetRegex).downloadUrl,
            null,
        )
    }

    suspend fun latestVersion(prerelease: Boolean): ApiReleaseVersion {
        val patchesRelease = backendRepository.release(
            configurationRepository.organization,
            configurationRepository.patches.repository,
            prerelease,
        )

        return ApiReleaseVersion(patchesRelease.tag)
    }

    private val patchesListCache = Caffeine
        .newBuilder()
        .maximumSize(1)
        .build<String, ByteArray>()

    suspend fun list(prerelease: Boolean): ByteArray {
        val patchesRelease = backendRepository.release(
            configurationRepository.organization,
            configurationRepository.patches.repository,
            prerelease,
        )

        return withContext(Dispatchers.IO) {
            patchesListCache.get(patchesRelease.tag) {
                val patchesDownloadUrl = patchesRelease.assets
                    .first(configurationRepository.patches.assetRegex).downloadUrl

                val patchesFile = kotlin.io.path.createTempFile().toFile().apply {
                    outputStream().use { URL(patchesDownloadUrl).openStream().copyTo(it) }
                }

                // Load patches without signature verification.
                val patches = loadPatchesFromJar(setOf(patchesFile))

                patchesFile.delete()

                ByteArrayOutputStream().use { stream ->
                    patches.serializeTo(outputStream = stream)

                    stream.toByteArray()
                }
            }
        }
    }

    fun publicKey() = ApiAssetPublicKey(configurationRepository.patches.publicKeyFile.readText())
}
