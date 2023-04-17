package main

import main.json.AnimeFileMerger
import main.json.CharacterStaffMerger
import main.json.CharactersIdGen
import main.scrapers.AnimeScraper
import main.scrapers.CharactersScraper
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if(args.isEmpty()) {
        println("No args supplied, exiting")
        exitProcess(0)
    }

    when(args[0]) {
        "animescraper" -> {
            val scraper = AnimeScraper();
            scraper.start()
        }
        "animemerger" -> {
            val merger = AnimeFileMerger()
            merger.start()
        }
        "gencharactersidmap" -> {
            val gen = CharactersIdGen()
            gen.start()
        }
        "scrapeanimecharacters" -> {
            val scraper = CharactersScraper()
            scraper.start()
        }
        "charactermerger" -> {
            val merger = CharacterStaffMerger()
            merger.start()
        }
    }

}