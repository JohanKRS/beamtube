package ytslim.data

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File

/**
 * Instância única de [Json] compartilhada pelos caches e repositórios do app (evita manter vários
 * caches internos de descritores iguais em memória).
 */
internal val AppJson = Json { ignoreUnknownKeys = true }

/** Lê e decodifica [file] em streaming, sem carregar o JSON inteiro como String UTF-16 na memória. */
@OptIn(ExperimentalSerializationApi::class)
internal fun <T> File.readJson(deserializer: DeserializationStrategy<T>): T =
    BufferedInputStream(inputStream()).use { AppJson.decodeFromStream(deserializer, it) }

/** Serializa [value] em streaming direto para [file] (evita alocar String + ByteArray inteiros). */
@OptIn(ExperimentalSerializationApi::class)
internal fun <T> File.writeJson(serializer: SerializationStrategy<T>, value: T) {
    BufferedOutputStream(outputStream()).use { AppJson.encodeToStream(serializer, value, it) }
}
