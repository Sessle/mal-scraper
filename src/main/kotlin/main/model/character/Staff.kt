package main.model.character

data class Staff(
    var anime_id: Int? = null,
    var image_url: String? = null,
    var name: String? = null,
    var roles: List<String>? = null
)
