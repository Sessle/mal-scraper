package main.scrapers

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import it.skrape.core.htmlDocument
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extractIt
import it.skrape.fetcher.skrape
import it.skrape.selects.ElementNotFoundException
import it.skrape.selects.html5.*
import main.model.character.AnimeCharacterStaff
import main.model.character.Character
import main.model.character.Staff
import main.model.people.Person
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.nameWithoutExtension

class CharactersScraper {

    fun start() {
        println("Starting..")

        //Get starting id
        //val startingId = getStartingId()
        val scrapedIds = getScrapedIds()

        //Load list of ids and filter out ids we've already scraped, then sort
        //val ids = loadIds(Paths.get("./json/mal/input/characterIdsMap.json")).filter { it.key >= startingId }.toSortedMap()
        val ids = loadIds(Paths.get("./json/mal/input/characterIdsMap.json")).filter { it.key !in scrapedIds }.toSortedMap()

        /** Catch any unexpected exceptions */
        try {
            //Loop through remaining ids and scrape
            ids.forEach {
                val start = System.currentTimeMillis()

                println(it)

                /** Catch NotFoundException here, so we can ignore this id and continue looping */
                try {
                    scrape(it.key, it.value)
                } catch(e: NotFoundException) {
                    return@forEach
                }

                val timeSpent = System.currentTimeMillis() - start

                if(timeSpent < 2000) {
                    Thread.sleep(2000 - timeSpent)
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadIds(path: Path): Map<Int, String> {
        val reader = Files.newInputStream(path)
        val mapper = jacksonObjectMapper()

        reader.use {
            return mapper.readValue(reader)
        }
    }

    private fun getScrapedIds(): List<Int> {
        val ids = mutableListOf<Int>()

        Files.newDirectoryStream(
            Paths.get("./json/mal/output/characters/scraped/")
        ).use { stream ->
            for (path in stream) {
                val id = path.nameWithoutExtension.toInt()
                ids.add(id)
            }
        }

        return ids.toList()
    }

    private fun getStartingId(): Int {

        //Get highest number from files in output/scraped
        var startingId = 0

        Files.newDirectoryStream(
            Paths.get("./json/mal/output/characters/scraped/")
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

    fun scrape(id: Int, charactersUrl: String) {
        val extracted = skrape(HttpFetcher) {

            request {
                url = charactersUrl
                timeout = 30000
            }

            extractIt<AnimeCharacterStaff> {
                val status = status { code }

                if(status == 404) {
                    throw NotFoundException()
                }

                htmlDocument {

                    val characters = mutableListOf<Character>()

                    try {
                        val tables = table(".js-anime-character-table") { findAll { this } }

                        tables.forEach { characterTable ->
                            val body = characterTable.tbody { findFirst { this } }
                            val row = body.tr { findFirst { this } }
                            //val columns = row.td { findAll { this }}

                            val image_url =
                                row.td { findFirst { div { findFirst { a { findFirst { img { findFirst { attribute("data-src") } } } } } } } }
                            val character_url =
                                row.td { findSecond { div { 2 { a { findFirst { attribute("href") } } } } } }
                            val splitCharacterUrl = character_url.split("/")
                            val character_id = splitCharacterUrl[4].toInt()
                            val name =
                                row.td { findSecond { div { 2 { a { findFirst { h3 { findFirst { ownText } } } } } } } }
                                    .split(", ").reversed().joinToString(" ")
                            val type = row.td { findSecond { div { 3 { ownText } } } }

                            val people = mutableListOf<Person>()
                            try {
                                val peopleTable = row.td { 2 { table { findFirst { this } } } }
                                val peopleBody = peopleTable.tbody { findFirst { this } }
                                val peopleRows = peopleBody.tr { findAll { this } }


                                peopleRows.forEach { prow ->
                                    val person_url =
                                        prow.td { findFirst { div { findFirst { a { findFirst { attribute("href") } } } } } }
                                    val splitPersonUrl = person_url.split("/")
                                    val person_id = splitPersonUrl[4].toInt()
                                    val name =
                                        prow.td { findFirst { div { findFirst { a { findFirst { ownText } } } } } }
                                            .split(", ").reversed().joinToString(" ")
                                    val language = prow.td { findFirst { div { findSecond { ownText } } } }
                                    val imageUrl = prow.td {
                                        findSecond {
                                            div {
                                                findFirst {
                                                    a {
                                                        findFirst {
                                                            img {
                                                                findFirst {
                                                                    attribute("data-src")
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    people.add(Person(person_id, person_url, name, image_url = imageUrl, language))
                                }
                            } catch (e: ElementNotFoundException) {

                            }
                            characters.add(
                                Character(
                                    anime_id = id,
                                    character_id = character_id,
                                    character_url = character_url,
                                    image_url = image_url,
                                    name = name,
                                    type = type,
                                    people = if (people.isEmpty()) null else people
                                )
                            )
                        }
                    } catch (e: ElementNotFoundException) {

                    }

                    it.characters = characters

                    val staff = mutableListOf<Staff>()

                    try {
                        val staffTables = div {
                            withClass = "rightside"

                            findFirst {
                                table {
                                    findAll {
                                        this
                                    }
                                }
                            }
                        }.filter { it.classNames.isEmpty() }

                        staffTables.forEach { stable ->
                            val body = stable.tbody { findFirst { this } }
                            val row = body.tr { findFirst { this } }

                            val anime_id = id
                            val image_url =
                                row.td { findFirst { div { findFirst { a { findFirst { img { findFirst { attribute("data-src") } } } } } } } }
                            val name = row.td { findSecond { a { findFirst { ownText } } } }.split(", ").reversed()
                                .joinToString(" ")
                            val roles = row.td { findSecond { div { findFirst { small { findFirst { ownText } } } } } }
                                .split(", ")
                            staff.add(Staff(anime_id, image_url, name, roles))
                        }
                    } catch(e: ElementNotFoundException) {

                    }

                    it.staff = staff
                }
            }
        }

        //Write file
        val outputPath: Path = Paths.get("./json/mal/output/characters/scraped/${id}.json")
        val writer = Files.newBufferedWriter(outputPath)
        val mapper = jacksonObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT)

        writer.use {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, extracted)
        }
    }


}