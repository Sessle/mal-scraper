package main.json

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import main.model.anime.Anime
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CharactersIdGen {

    fun start() {
        println("Starting..")

        println("Reading file..")
        val anime = readFiles()

        println("Generating ids map")
        val idsMap = getIdsMap(anime)

        println("Writing ids..")
        writeIds(idsMap)
    }

    private fun readFiles(): List<Anime> {
        val reader = Files.newInputStream(Paths.get("./json/mal/output/anime/merged.json"))
        val mapper = jacksonObjectMapper()

        reader.use {
            return mapper.readValue(reader)
        }
    }

    private fun getIdsMap(anime: List<Anime>): Map<Int, String> {
        val idsMap = mutableMapOf<Int, String>()
        anime.forEach {
            if(it.mal_id != null && it.charactersUrl != null) {
                idsMap[it.mal_id!!] = it.charactersUrl!!
            }
        }

        return idsMap.toMap()
    }

    private fun writeIds(idsMap: Map<Int, String>) {
        val outputPath: Path = Paths.get("./json/mal/input/characterIdsMap.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, idsMap)
        }
    }
}