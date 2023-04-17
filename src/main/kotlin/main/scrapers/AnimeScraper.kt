package main.scrapers

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.eachText
import it.skrape.selects.html5.*
import main.model.anime.Anime
import main.model.anime.AnimeRelation
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

class AnimeScraper {

    fun start() {
        println("Starting..")

        //Get starting id
        val startingId = getStartingId()

        //Load list of ids and filter out ids we've already scraped, then sort
        val ids = loadIds(Paths.get("./json/mal/input/ids.json")).filter { it >= startingId }.sorted()

        //Loop through remaining ids and scrape
        ids.forEach {
            val start = System.currentTimeMillis()

            println(it)

            try {
                scrape(it)
            } catch(e: Exception) {
                if(e.message == "404") {
                    return@forEach
                } else {
                    //TODO: catch timeout exception and rate limit exception and restart loop
                    //TODO: add rate limit exception
                    e.printStackTrace();
                }
            }

            val timeSpent = System.currentTimeMillis() - start

            if(timeSpent < 2000) {
                Thread.sleep(2000 - timeSpent)
            }
        }

    }

    private fun getStartingId(): Int {

        //Get highest number from files in output/scraped
        var startingId = 0

        Files.newDirectoryStream(
            Paths.get("./json/mal/output/scraped/")
        ).use { stream ->
            for (path in stream) {
                val id = path.nameWithoutExtension.toInt()
                if(id > startingId) {
                    startingId = id
                }
            }
        }

        return startingId + 1
    }

    private fun loadIds(path: Path): List<Int> {
        val reader = Files.newInputStream(path)
        val mapper = jacksonObjectMapper()

        reader.use {
            return mapper.readValue(reader)
        }
    }

    fun scrape(id: Int) {
        val extracted = skrape(HttpFetcher) {

            request {
                url = "https://myanimelist.net/anime/${id}"
                timeout = 30000
            }

            extractIt<Anime> {
                val status = status { code }

                if(status == 404) {
                    throw Exception("404")
                }

                htmlDocument {
                    relaxed = true

                    it.mal_id = id
                    it.synopsis = p {
                        withAttribute = "itemprop" to "description"

                        findFirst { ownText }
                    }

                    it.image_url = img {
                        withAttribute = "itemprop" to "image"

                        findFirst { attribute("data-src") }
                    }

                    it.title_romaji = h1 {
                        withClass = "title-name"

                        strong {
                            findFirst { ownText }
                        }
                    }

                    val spaceits = div(".spaceit_pad") { findAll { this }}

                    it.title_synonyms = spaceits.firstOrNull { it.text.contains("Synonyms:") }?.ownText?.split(", ")
                    it.title_japanese = spaceits.firstOrNull { it.text.contains("Japanese:") }?.ownText
                    it.title_english = spaceits.firstOrNull { it.text.contains("English:") }?.ownText
                    it.title_german = spaceits.firstOrNull { it.text.contains("German:") }?.ownText
                    it.title_spanish = spaceits.firstOrNull { it.text.contains("Spanish:") }?.ownText
                    it.title_french = spaceits.firstOrNull { it.text.contains("French:") }?.ownText
                    it.type = spaceits.firstOrNull { it.text.contains("Type:") }?.a {
                        findFirst { ownText }
                    }

                    val episodes = spaceits.firstOrNull { it.span { findFirst { ownText } } == "Episodes:" }?.ownText
                    if(episodes != null && episodes != "Unknown") {
                        it.episodes = episodes.toInt()
                    }

                    it.status = spaceits.firstOrNull { it.text.contains("Status:") }?.ownText
                    it.aired = spaceits.firstOrNull { it.text.contains("Aired:") }?.ownText

                    val premiered = spaceits.firstOrNull { it.text.contains("Premiered:") }?.a {
                        findFirst { ownText }
                    }?.split(" ")

                    if(premiered != null && premiered[0] != "") {
                        it.season = premiered[0]
                        it.year = premiered[1].toInt()
                    }

                    it.broadcast = spaceits.firstOrNull { it.text.contains("Broadcast:") }?.ownText
                    it.producers = spaceits.firstOrNull { it.text.contains("Producers:") }?.a {
                        findAll { eachText }
                    }
                    it.licensors = spaceits.firstOrNull { it.text.contains("Licensors:") }?.a {
                        findAll { eachText }
                    }
                    it.studios = spaceits.firstOrNull { it.text.contains("Studios:") }?.a {
                        findAll { eachText }
                    }
                    it.source = spaceits.firstOrNull { it.text.contains("Source:") }?.ownText
                    it.genres = spaceits.firstOrNull { it.text.contains("Genre") || it.text.contains("Genres") }?.a {
                        findAll { eachText }
                    }
                    it.themes = spaceits.firstOrNull { it.text.contains("Theme:") || it.text.contains("Themes:") }?.a {
                        findAll { eachText }
                    }
                    it.demographic = spaceits.firstOrNull { it.text.contains("Demographic:") || it.text.contains("Demographics:") }?.a {
                        findFirst { ownText }
                    }
                    it.duration = spaceits.firstOrNull { it.text.contains("Duration") }?.ownText
                    it.rating = spaceits.firstOrNull { it.text.contains("Rating") }?.ownText

                    val relationTable = table(".anime_detail_related_anime") { findFirst { this }}

                    /** Since relaxed mode is on, relationTable will not throw an exception if not found
                     * However, since we're performing operations on these elements, we have to check if null */
                    if(relationTable.isPresent && relationTable.element.parentNode() != null) {
                        val relations = mutableListOf<AnimeRelation>()
                        val tableBody = relationTable.tbody { findFirst { this }}
                        val tableRows = tableBody.tr { findAll { this }}

                        tableRows.forEach { row ->
                            val type = row.td { findFirst { ownText }}.dropLast(1)
                            val links = row.td { findSecond { eachHref }}
                            val names = row.td { findSecond { a { findAll { eachText }}}}

                            links.forEachIndexed { index, link ->
                                /** Sometimes MyAnimeList has broken <a> tags in the relations table - ignore these */
                                if(names[index].isEmpty()) {
                                    return@forEachIndexed
                                }

                                val splitLink = link.split("/")
                                val category = splitLink[1]
                                val malId = splitLink[2].toInt()

                                relations.add(AnimeRelation(type = type, category = category, mal_id = malId, name = names[index], url = link))
                            }
                        }

                        it.relations = relations
                    }

                    it.charactersUrl = findAll { eachHref.filter { it.contains("/characters") }}.first()
                }
            }

        }

        //Write file
        val outputPath: Path = Paths.get("./json/mal/output/scraped/${id}.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, extracted)
        }
    }
}