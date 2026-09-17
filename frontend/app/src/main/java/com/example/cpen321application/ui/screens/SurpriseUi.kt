package com.example.cpen321application.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cpen321application.viewmodel.CaughtPokemon
import com.example.cpen321application.viewmodel.Rarity
import com.example.cpen321application.viewmodel.SurpriseViewModel
import kotlinx.coroutines.delay

// Border color for pokemon card based on rarity
fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> Color(0xFF9E9E9E)     // Grey
    Rarity.RARE -> Color(0xFF4A90D9)       // Blue
    Rarity.EPIC -> Color(0xFF9B59B6)       // Purple
    Rarity.EXCLUSIVE -> Color(0xFFFFD700)  // Gold
}

// Pokeball sprite
@Composable
fun PokeballView(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    rotationDegrees: Float = 0f,
    scale: Float = 1f,
    onClick: (() -> Unit)? = null
) {
    AsyncImage(
        model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png",
        contentDescription = "Pokeball",
        modifier = modifier
            .size(size)
            .graphicsLayer(rotationZ = rotationDegrees, scaleX = scale, scaleY = scale)
            .let { if (onClick != null) it.clickable { onClick() } else it }
    )
}

// Pokemon Card
@Composable
private fun RevealedPokemonCard(
    pokemon: CaughtPokemon,
    imageSize: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val borderColor = rarityColor(pokemon.rarity)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.let { if (onClick != null) it.clickable { onClick() } else it }
    ) {
        Box(
            modifier = Modifier
                .size(imageSize)
                .border(width = 3.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = pokemon.spriteUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(pokemon.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("${"★".repeat(pokemon.rarity.stars)} ${pokemon.rarity.label}", color = borderColor, fontSize = 12.sp)
    }
}

// Pokemon details card
@Composable
fun PokemonDetailDialog(pokemon: CaughtPokemon, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Text(text = pokemon.name, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(width = 2.dp, color = rarityColor(pokemon.rarity), shape = RoundedCornerShape(16.dp))
                        .padding(8.dp)
                ) {
                    AsyncImage(
                        model = pokemon.spriteUrl,
                        contentDescription = pokemon.name,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                DetailRow("ID", "#${pokemon.id}")
                DetailRow("Rarity", "${"★".repeat(pokemon.rarity.stars)} ${pokemon.rarity.label}")
                DetailRow("Types", pokemon.types.joinToString(", "))
                DetailRow("BST", "${pokemon.bst}")
                DetailRow("Height", "${pokemon.height / 10.0} m")
                DetailRow("Weight", "${pokemon.weight / 10.0} kg")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)
        Text(text = value)
    }
}

// Screen: finished timer, closed pokeball opens -> reveal

@Composable
fun SurpriseRevealScreen(
    isPokeballOpened: Boolean,
    surpriseViewModel: SurpriseViewModel,
    onOpenPokeball: () -> Unit,
    onReset: () -> Unit
) {
    if (!isPokeballOpened) {
        Text("Time's up!", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))
        PokeballView(onClick = onOpenPokeball)
        Text("Tap to open!", modifier = Modifier.padding(top = 8.dp))
        return
    }

    val uiState by surpriseViewModel.uiState.collectAsState()
    var phase by remember { mutableStateOf(RevealPhase.SHAKING) }
    val rotation = remember { Animatable(0f) }
    val burstScale = remember { Animatable(1f) }
    val revealScale = remember { Animatable(0f) }
    var showDetailAfterReveal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Pull the pokemon
        surpriseViewModel.pull()

        // Animation: Shakes 3 times
        repeat(3) {
            rotation.animateTo(-18f, tween(90))
            rotation.animateTo(18f, tween(90))
            rotation.animateTo(-10f, tween(80))
            rotation.animateTo(10f, tween(80))
            rotation.animateTo(0f, tween(80))
            delay(150)
        }

        // Animation: Pokeball opens
        phase = RevealPhase.BURST
        burstScale.animateTo(1.6f, tween(220))
        burstScale.animateTo(0f, tween(180))

        // Reveals the card
        phase = RevealPhase.REVEALED
        revealScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (phase) {
            RevealPhase.SHAKING -> {
                PokeballView(rotationDegrees = rotation.value)
            }
            RevealPhase.BURST -> PokeballView(scale = burstScale.value)
            RevealPhase.REVEALED -> {
                uiState.lastPull?.let {
                    RevealedPokemonCard(
                        pokemon = it,
                        imageSize = 200.dp,
                        modifier = Modifier.graphicsLayer(
                            scaleX = revealScale.value,
                            scaleY = revealScale.value
                        ),
                        onClick = { showDetailAfterReveal = true }
                    )

                    if (showDetailAfterReveal) {
                        PokemonDetailDialog(it, onDismiss = { showDetailAfterReveal = false })
                    }
                } ?: uiState.error?.let {
                    Text("Error: $it", fontSize = 14.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onReset) { Text("Reset") }
    }
}

private enum class RevealPhase { SHAKING, BURST, REVEALED }

// Screen: See collected Pokemon
@Composable
fun SurpriseCollectionScreen(
    collection: List<CaughtPokemon>,
    onBack: () -> Unit,
    onClearAll: () -> Unit
) {
    var selectedPokemon by remember { mutableStateOf<CaughtPokemon?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Collection (${collection.size})", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(collection.reversed()) { pokemon ->
                RevealedPokemonCard(pokemon, imageSize = 90.dp, onClick = { selectedPokemon = pokemon })
            }
        }

        if (selectedPokemon != null) {
            PokemonDetailDialog(selectedPokemon!!, onDismiss = { selectedPokemon = null })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onClearAll) { Text("Clear All") }
        }
    }
}