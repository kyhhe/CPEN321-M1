package com.example.cpen321application.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

private const val PREFS_NAME = "gacha_collection"
private const val COLLECTION_KEY = "caught_pokemon"
private const val MAX_DEX_ID = 898
private const val MAX_REROLL_ATTEMPTS = 5

enum class Rarity(val stars: Int, val label: String) {
    COMMON(3, "Common"),
    RARE(4, "Rare"),
    EPIC(5, "Epic"),
    EXCLUSIVE(6, "Exclusive")
}

data class CaughtPokemon(
    val id: Int,
    val name: String,
    val spriteUrl: String,
    val rarity: Rarity,
    val types: List<String> = emptyList(),
    val height: Int = 0,
    val weight: Int = 0,
    val bst: Int = 0
)

data class SurpriseUiState(
    val lastPull: CaughtPokemon? = null,
    val collection: List<CaughtPokemon> = emptyList(),
    val error: String? = null
)

class SurpriseViewModel(private val appContext: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(SurpriseUiState())
    val uiState: StateFlow<SurpriseUiState> = _uiState

    private val client = OkHttpClient()
    private var isPullingInProgress = false

    init {
        _uiState.value = _uiState.value.copy(collection = loadCollection())
    }

    fun pull() {
        if (isPullingInProgress) return
        isPullingInProgress = true

        _uiState.value = _uiState.value.copy(
            error = null,
            lastPull = null
        )

        viewModelScope.launch {
            try {
                // Roll for which tier of Pokemon
                val targetTier = rollTargetTier()
                val pokemon = withContext(Dispatchers.IO) { pullForTier(targetTier) }

                val updatedCollection = _uiState.value.collection + pokemon
                _uiState.value = _uiState.value.copy(
                    lastPull = pokemon,
                    collection = updatedCollection
                )
                withContext(Dispatchers.IO) { saveCollection(updatedCollection) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Pull failed: ${e.message}"
                )
            } finally {
                isPullingInProgress = false
            }
        }
    }

    fun clearCollection() {
        _uiState.value = _uiState.value.copy(collection = emptyList(), lastPull = null)
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    // Gacha pull for the target rarity
    private fun rollTargetTier(): Rarity {
        val roll = Random.nextInt(100)
        return when {
            roll < 1  -> Rarity.EXCLUSIVE   //  1%
            roll < 5  -> Rarity.EPIC        //  4%
            roll < 25 -> Rarity.RARE        // 20%
            else      -> Rarity.COMMON      // 75%
        }
    }

    /**
     * Attempt to pull a Pokémon that matches [targetTier].
     * Re-rolls up to [MAX_REROLL_ATTEMPTS] times; if none match, accepts the last pull.
     */
    private fun pullForTier(targetTier: Rarity): CaughtPokemon {
        repeat(MAX_REROLL_ATTEMPTS) {
            val id = Random.nextInt(1, MAX_DEX_ID + 1)
            val pokemon = fetchPokemon(id)
            if (pokemon.rarity == targetTier) return pokemon
        }
        // Fallback: accept whatever we got on the last attempt
        val id = Random.nextInt(1, MAX_DEX_ID + 1)
        return fetchPokemon(id)
    }

    // Rarity classification: based on API isLegendary, isMythical flags and base stats
    private fun classifyRarity(bst: Int, isLegendary: Boolean, isMythical: Boolean): Rarity {
        return when {
            isMythical                          -> Rarity.EXCLUSIVE
            isLegendary && bst >= 670            -> Rarity.EXCLUSIVE
            isLegendary || bst in 580..669       -> Rarity.EPIC
            bst in 480..579                      -> Rarity.RARE
            else                                 -> Rarity.COMMON
        }
    }

    private fun computeBst(statsArray: JSONArray): Int {
        var total = 0
        for (i in 0 until statsArray.length()) {
            total += statsArray.getJSONObject(i).getInt("base_stat")
        }
        return total
    }

    // API calls to getch Pokemon data
    private fun fetchPokemon(id: Int): CaughtPokemon {
        // Fetch main pokemon data
        val pokemonJson = httpGet("https://pokeapi.co/api/v2/pokemon/$id")
        val name = pokemonJson.getString("name").replaceFirstChar { it.uppercase() }
        val spriteUrl = pokemonJson.getJSONObject("sprites").getString("front_default")
        val typesArray = pokemonJson.getJSONArray("types")
        val types = (0 until typesArray.length()).map { i ->
            typesArray.getJSONObject(i).getJSONObject("type").getString("name")
                .replaceFirstChar { it.uppercase() }
        }
        val height = pokemonJson.getInt("height")
        val weight = pokemonJson.getInt("weight")
        val bst = computeBst(pokemonJson.getJSONArray("stats"))

        // Fetch species data for legendary/mythical flags
        val speciesJson = httpGet("https://pokeapi.co/api/v2/pokemon-species/$id")
        val isLegendary = speciesJson.getBoolean("is_legendary")
        val isMythical = speciesJson.getBoolean("is_mythical")

        return CaughtPokemon(
            id = id,
            name = name,
            spriteUrl = spriteUrl,
            rarity = classifyRarity(bst, isLegendary, isMythical),
            types = types,
            height = height,
            weight = weight,
            bst = bst
        )
    }

    private fun httpGet(url: String): JSONObject {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty response from $url")
            return JSONObject(body)
        }
    }

    // Persistence: Saves collected pokemon in SharedPreferences
    private fun loadCollection(): List<CaughtPokemon> {
        return try {
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(COLLECTION_KEY, null) ?: return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val typesArray = obj.optJSONArray("types")
                val types = if (typesArray != null) {
                    (0 until typesArray.length()).map { j -> typesArray.getString(j) }
                } else emptyList()

                val rarity = Rarity.valueOf(obj.getString("rarity"))

                CaughtPokemon(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    spriteUrl = obj.getString("spriteUrl"),
                    rarity = rarity,
                    types = types,
                    height = obj.optInt("height", 0),
                    weight = obj.optInt("weight", 0),
                    bst = obj.optInt("bst", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveCollection(collection: List<CaughtPokemon>) {
        val array = JSONArray()
        collection.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("name", p.name)
            obj.put("spriteUrl", p.spriteUrl)
            obj.put("rarity", p.rarity.name)

            val typesArray = JSONArray()
            p.types.forEach { typesArray.put(it) }
            obj.put("types", typesArray)

            obj.put("height", p.height)
            obj.put("weight", p.weight)
            obj.put("bst", p.bst)

            array.put(obj)
        }
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(COLLECTION_KEY, array.toString()).apply()
    }
}