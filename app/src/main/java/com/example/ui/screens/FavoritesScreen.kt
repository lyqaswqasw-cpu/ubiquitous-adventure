package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.FavoriteCategoryFilter
import com.example.data.model.FavoriteMediaSnapshot
import com.example.data.model.LibraryMediaItem
import com.example.data.model.LiveChannel
import com.example.data.repository.FavoritesManager
import com.example.ui.components.OrderedColorfulDivider
import com.example.ui.components.tvFocusableClickable
import com.example.ui.theme.FoulCardDark
import com.example.ui.theme.FoulDivider
import com.example.ui.theme.FoulLiveGreen
import com.example.ui.theme.FoulLiveRed
import com.example.ui.theme.FoulPurpleBright
import com.example.ui.theme.FoulPurpleDeep
import com.example.ui.theme.FoulPurplePrimary
import com.example.ui.theme.FoulTextSecondary

@Composable
fun FavoritesScreen(
    allChannels: List<LiveChannel>,
    allLibraryItems: List<LibraryMediaItem>,
    onPlayChannel: (LiveChannel) -> Unit,
    onOpenMediaItem: (LibraryMediaItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        FavoritesManager.initialize(context)
    }

    BackHandler {
        onBack()
    }

    val favoriteChannelIds by FavoritesManager.favoriteChannelIds.collectAsState()
    val favoriteMediaMap by FavoritesManager.favoriteMediaMap.collectAsState()

    var selectedFilter by remember { mutableStateOf(FavoriteCategoryFilter.ALL) }
    var isAddPickerOpen by remember { mutableStateOf(false) }

    val favoriteChannels = remember(allChannels, favoriteChannelIds) {
        allChannels.filter { favoriteChannelIds.contains(it.id) }
    }
    val favoriteMovies = remember(favoriteMediaMap) {
        favoriteMediaMap.values.filter { !it.isSeries }.sortedByDescending { it.addedAt }
    }
    val favoriteSeries = remember(favoriteMediaMap) {
        favoriteMediaMap.values.filter { it.isSeries }.sortedByDescending { it.addedAt }
    }

    if (isAddPickerOpen) {
        AddToFavoritesPickerDialog(
            allChannels = allChannels,
            allLibraryItems = allLibraryItems,
            favoriteChannelIds = favoriteChannelIds,
            favoriteMediaMap = favoriteMediaMap,
            onToggleChannel = { ch -> FavoritesManager.toggleChannelFavorite(context, ch) },
            onToggleMedia = { item -> FavoritesManager.toggleMediaFavorite(context, item) },
            onDismiss = { isAddPickerOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030206))
            .statusBarsPadding()
            .testTag("favorites_screen")
    ) {
        // Sleek Top Header with Back Button + Title + Add to Favorites Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = Color(0xFF101016),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("favorites_back_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF2D55),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "المفضلة",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Surface(
                onClick = { isAddPickerOpen = true },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF862BEB),
                border = BorderStroke(1.dp, Color(0xFFC16AFF)),
                modifier = Modifier.testTag("favorites_open_add_picker_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة للمفضلة",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "إضافة للمفضلة",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        OrderedColorfulDivider(thickness = 1.dp)

        // Segmented Category Tabs: الكل | القنوات المباشرة | الأفلام | المسلسلات
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF06040C))
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FavoriteCategoryFilter.entries) { filter ->
                val isSelected = selectedFilter == filter
                val count = when (filter) {
                    FavoriteCategoryFilter.ALL -> favoriteChannels.size + favoriteMovies.size + favoriteSeries.size
                    FavoriteCategoryFilter.CHANNELS -> favoriteChannels.size
                    FavoriteCategoryFilter.MOVIES -> favoriteMovies.size
                    FavoriteCategoryFilter.SERIES -> favoriteSeries.size
                }
                Surface(
                    onClick = { selectedFilter = filter },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF862BEB) else Color(0xFF101016),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) Color(0xFFC16AFF) else Color(0xFF241E33)
                    ),
                    modifier = Modifier.testTag("favorites_filter_${filter.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = filter.labelAr,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.Black.copy(alpha = 0.35f) else Color(0xFF1D172B)
                        ) {
                            Text(
                                text = count.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        val showChannels = selectedFilter == FavoriteCategoryFilter.ALL || selectedFilter == FavoriteCategoryFilter.CHANNELS
        val showMovies = selectedFilter == FavoriteCategoryFilter.ALL || selectedFilter == FavoriteCategoryFilter.MOVIES
        val showSeries = selectedFilter == FavoriteCategoryFilter.ALL || selectedFilter == FavoriteCategoryFilter.SERIES

        val isEmptyForCurrentFilter = when (selectedFilter) {
            FavoriteCategoryFilter.ALL -> favoriteChannels.isEmpty() && favoriteMovies.isEmpty() && favoriteSeries.isEmpty()
            FavoriteCategoryFilter.CHANNELS -> favoriteChannels.isEmpty()
            FavoriteCategoryFilter.MOVIES -> favoriteMovies.isEmpty()
            FavoriteCategoryFilter.SERIES -> favoriteSeries.isEmpty()
        }

        if (isEmptyForCurrentFilter) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF140D22),
                        border = BorderStroke(1.2.dp, Color(0xFF862BEB).copy(alpha = 0.55f)),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color(0xFFFF2D55),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Text(
                        text = "قائمة المفضلة فارغة في هذا القسم",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "اضغط على زر «إضافة للمفضلة» في الأعلى أو علامة القلب بجانب أي قناة أو فيلم أو مسلسل لإضافته هنا.",
                        color = FoulTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { isAddPickerOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF862BEB)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "إضافة قنوات وأفلام ومسلسلات للمفضلة",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Favorite Live Channels Section
                if (showChannels && favoriteChannels.isNotEmpty()) {
                    item(key = "header_fav_channels") {
                        FavoriteSectionHeader(
                            title = "القنوات المباشرة المفضلة (${favoriteChannels.size})",
                            icon = Icons.Default.LiveTv
                        )
                    }
                    items(favoriteChannels, key = { "fav_ch_${it.id}" }) { channel ->
                        FavoriteChannelRowCard(
                            channel = channel,
                            onPlay = { onPlayChannel(channel) },
                            onRemove = { FavoritesManager.removeChannelFavorite(context, channel.id) }
                        )
                    }
                }

                // 2. Favorite Movies Section
                if (showMovies && favoriteMovies.isNotEmpty()) {
                    item(key = "header_fav_movies") {
                        FavoriteSectionHeader(
                            title = "الأفلام المفضلة (${favoriteMovies.size})",
                            icon = Icons.Default.Movie
                        )
                    }
                    items(favoriteMovies, key = { "fav_mov_${it.id}" }) { snap ->
                        FavoriteMediaRowCard(
                            snapshot = snap,
                            typeLabel = "فيلم",
                            onOpen = {
                                val resolved = allLibraryItems.find { it.id == snap.id }
                                    ?: FavoritesManager.snapshotToLibraryMediaItem(snap)
                                onOpenMediaItem(resolved)
                            },
                            onRemove = { FavoritesManager.removeMediaFavorite(context, snap.id) }
                        )
                    }
                }

                // 3. Favorite Series Section
                if (showSeries && favoriteSeries.isNotEmpty()) {
                    item(key = "header_fav_series") {
                        FavoriteSectionHeader(
                            title = "المسلسلات المفضلة (${favoriteSeries.size})",
                            icon = Icons.Default.Tv
                        )
                    }
                    items(favoriteSeries, key = { "fav_ser_${it.id}" }) { snap ->
                        FavoriteMediaRowCard(
                            snapshot = snap,
                            typeLabel = "مسلسل",
                            onOpen = {
                                val resolved = allLibraryItems.find { it.id == snap.id }
                                    ?: FavoritesManager.snapshotToLibraryMediaItem(snap)
                                onOpenMediaItem(resolved)
                            },
                            onRemove = { FavoritesManager.removeMediaFavorite(context, snap.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF862BEB),
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun FavoriteChannelRowCard(
    channel: LiveChannel,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF101014),
        border = BorderStroke(1.dp, Color(0xFF241E33)),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusableClickable(
                shape = RoundedCornerShape(16.dp),
                scaleOnFocus = 1.02f,
                onClick = onPlay
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF191524))
                        .border(1.dp, Color(0xFF6321A8), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (channel.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = channel.imageUrl,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(6.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = channel.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(FoulLiveGreen)
                        )
                        Text(
                            text = channel.category.ifBlank { "بث مباشر" },
                            color = FoulTextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onPlay,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF862BEB)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "تشغيل",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تشغيل",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FoulLiveRed.copy(alpha = 0.14f))
                        .testTag("remove_fav_channel_${channel.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف من المفضلة",
                        tint = Color(0xFFFF6E6E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteMediaRowCard(
    snapshot: FavoriteMediaSnapshot,
    typeLabel: String,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF101014),
        border = BorderStroke(1.dp, Color(0xFF241E33)),
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusableClickable(
                shape = RoundedCornerShape(16.dp),
                scaleOnFocus = 1.02f,
                onClick = onOpen
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(66.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF191524))
                        .border(1.dp, Color(0xFF6321A8), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (snapshot.posterUrl.isNotBlank()) {
                        AsyncImage(
                            model = snapshot.posterUrl,
                            contentDescription = snapshot.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (snapshot.isSeries) Icons.Default.Tv else Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = snapshot.name,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF862BEB).copy(alpha = 0.22f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                color = Color(0xFFD59BFF),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                        if (snapshot.releaseYear.isNotBlank()) {
                            Text(
                                text = snapshot.releaseYear,
                                color = FoulTextSecondary,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onOpen,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF862BEB)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "مشاهدة",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "مشاهدة",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FoulLiveRed.copy(alpha = 0.14f))
                        .testTag("remove_fav_media_${snapshot.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "حذف من المفضلة",
                        tint = Color(0xFFFF6E6E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddToFavoritesPickerDialog(
    allChannels: List<LiveChannel>,
    allLibraryItems: List<LibraryMediaItem>,
    favoriteChannelIds: Set<String>,
    favoriteMediaMap: Map<String, FavoriteMediaSnapshot>,
    onToggleChannel: (LiveChannel) -> Unit,
    onToggleMedia: (LibraryMediaItem) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0 = Channels, 1 = Movies, 2 = Series

    val filteredChannels = remember(allChannels, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allChannels.take(60)
        else allChannels.filter { it.name.lowercase().contains(q) }.take(60)
    }
    val filteredMovies = remember(allLibraryItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val list = allLibraryItems.filter { !it.isSeries }
        if (q.isEmpty()) list.take(60)
        else list.filter { it.lowerName.contains(q) }.take(60)
    }
    val filteredSeries = remember(allLibraryItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val list = allLibraryItems.filter { it.isSeries }
        if (q.isEmpty()) list.take(60)
        else list.filter { it.lowerName.contains(q) }.take(60)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF0B0912),
            border = BorderStroke(1.2.dp, Color(0xFF6321A8)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إضافة أو حذف من المفضلة",
                        color = Color.White,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = FoulTextSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن قناة أو فيلم أو مسلسل...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FoulTextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF862BEB),
                        unfocusedBorderColor = Color(0xFF282238),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("القنوات", "الأفلام", "المسلسلات").forEachIndexed { idx, label ->
                        val selected = activeTab == idx
                        Surface(
                            onClick = { activeTab = idx },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) Color(0xFF862BEB) else Color(0xFF14111D),
                            border = BorderStroke(1.dp, if (selected) Color(0xFFC16AFF) else Color(0xFF262038)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 12.5.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            items(filteredChannels, key = { "pick_ch_${it.id}" }) { ch ->
                                val isFav = favoriteChannelIds.contains(ch.id)
                                Surface(
                                    onClick = { onToggleChannel(ch) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF12101A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isFav) Color(0xFFFF2D55) else Color(0xFF231D33)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ch.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة للمفضلة",
                                            tint = if (isFav) Color(0xFFFF2D55) else FoulTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            items(filteredMovies, key = { "pick_mov_${it.id}" }) { mov ->
                                val isFav = favoriteMediaMap.containsKey(mov.id)
                                Surface(
                                    onClick = { onToggleMedia(mov) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF12101A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isFav) Color(0xFFFF2D55) else Color(0xFF231D33)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = mov.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة للمفضلة",
                                            tint = if (isFav) Color(0xFFFF2D55) else FoulTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            items(filteredSeries, key = { "pick_ser_${it.id}" }) { ser ->
                                val isFav = favoriteMediaMap.containsKey(ser.id)
                                Surface(
                                    onClick = { onToggleMedia(ser) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF12101A),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isFav) Color(0xFFFF2D55) else Color(0xFF231D33)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = ser.name,
                                            color = Color.White,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isFav) "إزالة من المفضلة" else "إضافة للمفضلة",
                                            tint = if (isFav) Color(0xFFFF2D55) else FoulTextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
