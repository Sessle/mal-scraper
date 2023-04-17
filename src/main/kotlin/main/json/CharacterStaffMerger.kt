package main.json

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import main.model.anime.Anime
import main.model.character.AnimeCharacterStaff
import main.model.character.Character
import main.model.character.Staff
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class CharacterStaffMerger {

    data class Char(val id: Int, val name: String, val mal_url: String? = null, val image_url: String? = null)
    data class Staffer(val name: String, val image_url: String? = null)
    data class AnimeCharacterRelation(val animeId: Int, val characterId: Int, val type: String? = null)
    data class AnimeStaffRelation(val animeId: Int, val staffId: Int, val roles: String? = null)

    fun start() {
        println("Starting..")

        println("Reading files..")
        val charactersStaff = readFiles()//.sortedBy { it.mal_id }

        println("Separating characters and staff..")
        val characters = getAllCharacters(charactersStaff)
        val staff = getAllStaff(charactersStaff)

        println("Formatting character, staff, and relations..")
        val formattedChars = formatCharacters(characters)
        val formattedStaff = formatStaff(staff)
        val formattedAnimeCharRelations = formatAnimeCharacterRelations(characters)
        val formattedAnimeStaffRelations = formatAnimeStaffRelations(staff)

        println("Writing files..")
        writeCharacters(formattedChars)
        writeStaff(formattedStaff)
        writeAnimeCharacterRelations(formattedAnimeCharRelations)
        writeAnimeStaffRelations(formattedAnimeStaffRelations)
    }

    private fun readFiles(): List<AnimeCharacterStaff> {
        val merged = mutableListOf<AnimeCharacterStaff>()

        Files.newDirectoryStream(
            Paths.get("./json/mal/output/characters/scraped/")
        ).use { stream ->
            for (path in stream) {
                val reader = Files.newInputStream(path)
                val mapper = jacksonObjectMapper()

                reader.use {
                    merged.add(mapper.readValue(reader))
                }
            }
        }

        return merged.toList()
    }

    private fun getAllCharacters(merged: List<AnimeCharacterStaff>): List<List<Character>> {
        val chars = mutableListOf<List<main.model.character.Character>>()

        merged.forEach {
            chars.add(it.characters)
        }

        return chars.toList()
    }

    private fun getAllStaff(merged: List<AnimeCharacterStaff>): List<List<Staff>> {
        val staff = mutableListOf<List<Staff>>()

        merged.forEach {
            staff.add(it.staff)
        }

        return staff.toList()
    }

    private fun formatCharacters(animeChars: List<List<Character>>): List<Char> {
        val charactersMap = mutableMapOf<Int, Char>()

        animeChars.forEach {
            it.forEach inner@{ char ->
                if(!charactersMap.containsKey(char.character_id)) {
                    if(char.character_id == null || char.name == null) {
                        return@inner
                    }
                    val character = Char(char.character_id!!, char.name!!, char.character_url, char.image_url)
                    charactersMap[character.id] = character
                }
            }
        }

        val characters = mutableListOf<Char>()
        charactersMap.values.forEach {
            characters.add(it)
        }

        return characters.toList().sortedBy { it.id }
    }

    private fun formatStaff(animeStaff: List<List<Staff>>): List<Staffer> {
        val staffers = mutableListOf<Staffer>()

        animeStaff.forEach {
            it.forEach inner@{ staff ->
                if(!staffers.any { staffer -> staffer.name == staff.name }) {
                    if(staff.name == null) {
                        return@inner
                    }
                    staffers.add(Staffer(staff.name!!, staff.image_url))
                }
            }
        }

        return staffers.toList()
    }

    private fun formatAnimeCharacterRelations(animeCharacters: List<List<Character>>): List<AnimeCharacterRelation> {
        val relations = mutableListOf<AnimeCharacterRelation>()

        animeCharacters.forEach {
            it.forEach inner@{ char ->
                if(char.anime_id == null || char.character_id == null) {
                    return@inner
                }
                relations.add(AnimeCharacterRelation(char.anime_id!!, char.character_id!!, char.type))
            }
        }

        return relations.toList()
    }

    //Might have to do this after inserting staff because i didn't grab MyAnimeList's ids for them
    private fun formatAnimeStaffRelations(animeStaff: List<List<Staff>>): List<AnimeStaffRelation> {
        val relations = mutableListOf<AnimeStaffRelation>()

        animeStaff.forEach {
            it.forEachIndexed inner@{ index, staff ->
                if(staff.name == null || staff.anime_id == null) {
                    return@inner
                }
                val roles = if(staff.roles == null) null else staff.roles!!.joinToString(", ")
                relations.add(AnimeStaffRelation(staff.anime_id!!, index, roles))
            }
        }

        return relations.toList()
    }

    private fun writeCharacters(characters: List<Char>) {
        val outputPath: Path = Paths.get("./json/mal/output/characters/characters.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, characters)
        }
    }

    private fun writeStaff(staff: List<Staffer>) {
        val outputPath: Path = Paths.get("./json/mal/output/characters/staff.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, staff)
        }
    }

    private fun writeAnimeCharacterRelations(relations: List<AnimeCharacterRelation>) {
        val outputPath: Path = Paths.get("./json/mal/output/characters/animecharrelations.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, relations)
        }
    }

    private fun writeAnimeStaffRelations(relations: List<AnimeStaffRelation>) {
        val outputPath: Path = Paths.get("./json/mal/output/characters/animestaffrelations.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, relations)
        }
    }
}