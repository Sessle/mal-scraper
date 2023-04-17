package main.model.character

import main.model.people.Person

data class Character(
    var anime_id: Int? = null,
    var character_id: Int? = null,
    var character_url: String? = null,
    var image_url: String? = null,
    var name: String? = null,
    var type: String? = null,
    var people: List<Person>? = null
)