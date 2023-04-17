package main.json

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import main.model.anime.Anime
import main.model.anime.AnimeRelation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AnimeFileMerger {

    data class FormattedAnime(val id: Int, val synopsis: String? = null, val image_url: String? = null, val title_synonyms: List<String>? = null, val title_romaji: String, val title_japanese: String? = null, val title_english: String? = null, val title_german: String? = null, val title_spanish: String? = null, val title_french: String? = null, val type: String? = null, val episodes: Int? = null, val status: String? = null, val aired: String? = null, val season: String? = null, val year: Int? = null, val broadcast: String? = null, val source: String? = null, val rating: String? = null, val DemographicId: Int? = null)
    data class AnimeRelation(val ParentAnimeId: Int, val ChildAnimeId: Int, val relation: String)
    data class AnimeMangaRelation(val AnimeId: Int, val MangaId: Int, val relation: String)
    data class Producer(val name: String)
    data class Licensor(val name: String)
    data class Studio(val name: String)
    data class Genre(val name: String)
    data class Theme(val name: String)
    data class Demographic(val name: String)
    data class AnimeProducerRelation(val AnimeId: Int, val ProducerId: Int)
    data class AnimeLicensorRelation(val AnimeId: Int, val LicensorId: Int)
    data class AnimeStudioRelation(val AnimeId: Int, val StudioId: Int)
    data class AnimeGenreRelation(val AnimeId: Int, val GenreId: Int)
    data class AnimeThemeRelation(val AnimeId: Int, val GenreId: Int)

    private val producers = mutableListOf<Producer>()
    private val licensors = mutableListOf<Licensor>()
    private val studios = mutableListOf<Studio>()
    private val genres = mutableListOf<Genre>()
    private val themes = mutableListOf<Theme>()
    private val demographics = mutableListOf<Demographic>()

    fun start() {
        println("Starting..")

        println("Reading files..")
        val anime = readMerged().sortedBy { it.mal_id }

        println("Getting all Producers, Licensors, Studios, Genres, Themes, and Demographics..")
        getThings(anime)

        println("Separating and formatting anime and relations..")
        val formattedAnime = formatAnime(anime)
        val animeRelations = formatAnimeRelations(anime)
        val producerRelations = getAnimeProducerRelations(anime)
        val licensorRelations = getAnimeLicensorRelations(anime)
        val studioRelations = getAnimeStudioRelations(anime)
        val genreRelations = getAnimeGenreRelations(anime)
        val themeRelations = getAnimeThemeRelations(anime)

        println("Writing files..")
        writeFile(formattedAnime, "anime")
        writeFile(animeRelations, "animeRelations")

        writeFile(producers, "producers")
        writeFile(licensors, "licensors")
        writeFile(studios, "studios")
        writeFile(genres, "genres")
        writeFile(themes, "themes")
        writeFile(demographics, "demographics")

        writeFile(producerRelations, "producerRelations")
        writeFile(licensorRelations, "licensorRelations")
        writeFile(studioRelations, "studioRelations")
        writeFile(genreRelations, "genreRelations")
        writeFile(themeRelations, "themeRelations")

        /*println("Writing merged..")
        writeMerged(anime)*/
    }

    private fun readFiles(): List<Anime> {
        val anime = mutableListOf<Anime>()

        Files.newDirectoryStream(
            Paths.get("./json/mal/output/anime/scraped/")
        ).use { stream ->
            for (path in stream) {
                val reader = Files.newInputStream(path)
                val mapper = jacksonObjectMapper()

                reader.use {
                    anime.add(mapper.readValue(reader))
                }
            }
        }

        return anime.toList()
    }

    private fun readMerged(): List<Anime> {
        val path = Paths.get("./json/mal/output/anime/merged.json")
        val reader = Files.newInputStream(path)
        val mapper = jacksonObjectMapper()

        reader.use {
            return mapper.readValue(reader)
        }
    }

    private fun getThings(merged: List<Anime>) {
        val producers = mutableListOf<String>()
        val licensors = mutableListOf<String>()
        val studios = mutableListOf<String>()
        val genres = mutableListOf<String>()
        val themes = mutableListOf<String>()
        val demographics = mutableListOf<String>()

        merged.forEach {
            if(it.producers != null) {
                it.producers!!.forEach { producer ->
                    if(producer !in producers) {
                        producers.add(producer)
                    }
                }
            }

            if(it.licensors != null) {
                it.licensors!!.forEach { licensor ->
                    if(licensor !in licensors) {
                        licensors.add(licensor)
                    }
                }
            }

            if(it.studios != null) {
                it.studios!!.forEach { studio ->
                    if(studio !in studios) {
                        studios.add(studio)
                    }
                }
            }

            if(it.genres != null) {
                it.genres!!.forEach { genre ->
                    if(genre !in genres) {
                        genres.add(genre)
                    }
                }
            }

            if(it.themes != null) {
                it.themes!!.forEach { theme ->
                    if(theme !in themes) {
                        themes.add(theme)
                    }
                }
            }

            if(it.demographic != null) {
                if(it.demographic !in demographics) {
                    demographics.add(it.demographic!!)
                }
            }
        }

        producers.forEach {
            this.producers.add(Producer(it))
        }

        licensors.forEach {
            this.licensors.add(Licensor(it))
        }

        studios.forEach {
            this.studios.add(Studio(it))
        }

        genres.forEach {
            this.genres.add(Genre(it))
        }

        themes.forEach {
            this.themes.add(Theme(it))
        }

        demographics.forEach {
            this.demographics.add(Demographic(it))
        }

    }

    private fun formatAnime(anime: List<Anime>): List<FormattedAnime> {
        val formattedAnime = mutableListOf<FormattedAnime>()

        anime.forEach {
            if(it.mal_id != null && it.title_romaji != null) {
                formattedAnime.add(
                    FormattedAnime(
                        it.mal_id!!,
                        it.synopsis,
                        it.image_url,
                        it.title_synonyms,
                        it.title_romaji!!,
                        it.title_japanese,
                        it.title_english,
                        it.title_german,
                        it.title_spanish,
                        it.title_french,
                        it.type,
                        it.episodes,
                        it.status,
                        it.aired,
                        it.season,
                        it.year,
                        it.broadcast,
                        it.source,
                        it.rating,
                        DemographicId = if(it.demographic == null) null else demographics.indexOf(demographics.find { demo -> it.demographic == demo.name })
                    )
                )
            }
        }

        return formattedAnime
    }

    private fun formatAnimeRelations(anime: List<Anime>): List<AnimeRelation> {
        val animeRelations = mutableListOf<AnimeRelation>()

        anime.forEach {
            if(it.relations == null && it.mal_id != null) {
                return@forEach
            }
            it.relations!!.filter { rel -> rel.category == "anime" }.forEach { relation ->
                animeRelations.add(AnimeRelation(it.mal_id!!, relation.mal_id, relation.type))
            }
        }

        return animeRelations.toList().sortedBy { it.ParentAnimeId }
    }

    private fun formatAnimeMangaRelations(anime: List<Anime>) {

    }

    private fun getAnimeProducerRelations(anime: List<Anime>): List<AnimeProducerRelation> {
        val animeProducerRelations = mutableListOf<AnimeProducerRelation>()

        anime.forEach {
            if(it.producers == null || it.mal_id == null) {
                return@forEach
            }
            it.producers!!.forEach { producer ->
                animeProducerRelations.add(AnimeProducerRelation(it.mal_id!!, producers.indexOf(producers.find { prod -> prod.name == producer})))
            }
        }

        return animeProducerRelations.toList().sortedBy { it.AnimeId }
    }

    private fun getAnimeLicensorRelations(anime: List<Anime>): List<AnimeLicensorRelation> {
        val animeLicensorRelations = mutableListOf<AnimeLicensorRelation>()

        anime.forEach {
            if(it.licensors == null || it.mal_id == null) {
                return@forEach
            }
            it.licensors!!.forEach { licensor ->
                animeLicensorRelations.add(AnimeLicensorRelation(it.mal_id!!, licensors.indexOf(licensors.find { lic -> lic.name == licensor})))
            }
        }

        return animeLicensorRelations.toList().sortedBy { it.AnimeId }
    }

    private fun getAnimeStudioRelations(anime: List<Anime>): List<AnimeStudioRelation> {
        val animeStudioRelations = mutableListOf<AnimeStudioRelation>()

        anime.forEach {
            if(it.studios == null || it.mal_id == null) {
                return@forEach
            }
            it.studios!!.forEach { studio ->
                animeStudioRelations.add(AnimeStudioRelation(it.mal_id!!, studios.indexOf(studios.find { stu -> stu.name == studio})))
            }
        }

        return animeStudioRelations.toList().sortedBy { it.AnimeId }
    }

    private fun getAnimeGenreRelations(anime: List<Anime>): List<AnimeGenreRelation> {
        val animeGenreRelations = mutableListOf<AnimeGenreRelation>()

        anime.forEach {
            if(it.genres == null || it.mal_id == null) {
                return@forEach
            }
            it.genres!!.forEach { genre ->
                animeGenreRelations.add(AnimeGenreRelation(it.mal_id!!, genres.indexOf(genres.find { gen -> gen.name == genre})))
            }
        }

        return animeGenreRelations.toList().sortedBy { it.AnimeId }
    }

    private fun getAnimeThemeRelations(anime: List<Anime>): List<AnimeThemeRelation> {
        val animeThemeRelations = mutableListOf<AnimeThemeRelation>()

        anime.forEach {
            if(it.themes == null || it.mal_id == null) {
                return@forEach
            }
            it.themes!!.forEach { theme ->
                animeThemeRelations.add(AnimeThemeRelation(it.mal_id!!, themes.indexOf(themes.find { the -> the.name == theme})))
            }
        }

        return animeThemeRelations.toList().sortedBy { it.AnimeId }
    }

    private fun <T> writeFile(data: T, file: String) {
        val outputPath: Path = Paths.get("./json/mal/output/anime/${file}.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, data)
        }
    }
}