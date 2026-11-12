/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.ui.component.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.spotube.plugin_interfaces.plugin_apis.metadata.album.MetadataAlbum
import org.spotube.plugin_interfaces.plugin_apis.metadata.artist.MetadataArtist
import org.spotube.plugin_interfaces.plugin_apis.metadata.playlist.MetadataPlaylist
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.plugin_interfaces.plugin_apis.metadata.user.MetadataUser
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueCollectionEntry
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.navigation.NavigationCommands
import org.spotube.spotube.core.navigation.Routes
import org.spotube.spotube.core.remote.CollectionPlaybackKey
import org.spotube.spotube.core.remote.PlaybackDestinationAction
import org.spotube.spotube.core.remote.RemoteCollectionType
import org.spotube.spotube.core.remote.RemotePlaybackController
import org.spotube.spotube.modules.shell.LocalShellActions
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.media_artist
import spotube.composeapp.generated.resources.media_user
import spotube.composeapp.generated.resources.pages_added_to_queue
import spotube.composeapp.generated.resources.pages_playing

/*
 * Cover tiles wired to the app: navigation, play (or pause/resume when it is the playing
 * collection), add to queue through the destination picker + a shell toast, and the "Playing"
 * state. Home's shelf grid and the Library grid share them.
 */

/** A playlist or album tile. [type] picks the route, playback type and shared-element key. */
@Composable
fun CollectionCoverTile(
    type: RemoteCollectionType,
    id: String,
    title: String,
    subtitle: String?,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    topStart: (@Composable () -> Unit)? = null,
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject()
) {
    val scope = rememberCoroutineScope()
    val shellActions = LocalShellActions.current
    val currentCollection by audioPlayerQueue.currentCollectionEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val busy by remotePlaybackController.busyCollections.collectAsStateWithLifecycle()
    val isCurrent = when (type) {
        RemoteCollectionType.Playlist -> (currentCollection as? QueueCollectionEntry.Playlist)?.id == id
        RemoteCollectionType.Album -> (currentCollection as? QueueCollectionEntry.Album)?.id == id
        else -> false
    }
    val isPlaying = isCurrent && playerState == PlayerState.PLAYING
    val addedMessage = stringResource(Res.string.pages_added_to_queue, title)

    CoverTile(
        title = title,
        subtitle = subtitle,
        imageModel = imageUrl,
        modifier = modifier,
        onClick = {
            when (type) {
                RemoteCollectionType.Album -> navigationCommands.navigateTo(Routes.Album(id))
                else -> navigationCommands.navigateTo(Routes.Playlist(id))
            }
        },
        onPlay = {
            if (isCurrent) {
                scope.launch { if (isPlaying) audioPlayer.pause() else audioPlayer.play() }
            } else {
                remotePlaybackController.requestCollectionPlay(type, id, title)
            }
        },
        onAddToQueue = {
            remotePlaybackController.requestCollectionAddToQueue(type, id, title)
            shellActions.showToast(addedMessage, imageUrl)
        },
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        playLoading = CollectionPlaybackKey(type, id, PlaybackDestinationAction.Play) in busy,
        addToQueueLoading = CollectionPlaybackKey(type, id, PlaybackDestinationAction.AddToQueue) in busy,
        sharedElementKey = if (type == RemoteCollectionType.Album) "album_art_$id" else "playlist_art_$id",
        playingLabel = stringResource(Res.string.pages_playing),
        topStart = topStart
    )
}

@Composable
fun PlaylistCoverTile(
    playlist: MetadataPlaylist,
    modifier: Modifier = Modifier,
    subtitle: String? = playlist.owner?.displayName ?: playlist.owner?.username
) {
    CollectionCoverTile(
        type = RemoteCollectionType.Playlist,
        id = playlist.id,
        title = playlist.title,
        subtitle = subtitle,
        imageUrl = playlist.thumbnails.firstOrNull()?.url,
        modifier = modifier
    )
}

@Composable
fun AlbumCoverTile(
    album: MetadataAlbum,
    modifier: Modifier = Modifier,
    subtitle: String? = album.artists.joinToString { it.name }
) {
    CollectionCoverTile(
        type = RemoteCollectionType.Album,
        id = album.id,
        title = album.title,
        subtitle = subtitle,
        imageUrl = album.thumbnails.firstOrNull()?.url,
        modifier = modifier
    )
}

/** Artists are round and only open their page. */
@Composable
fun ArtistCoverTile(
    artist: MetadataArtist,
    modifier: Modifier = Modifier,
    subtitle: String? = stringResource(Res.string.media_artist),
    navigationCommands: NavigationCommands = koinInject()
) {
    CoverTile(
        title = artist.name,
        subtitle = subtitle,
        imageModel = artist.thumbnails.firstOrNull()?.url,
        onClick = { navigationCommands.navigateTo(Routes.Artist(artist.id)) },
        modifier = modifier,
        circle = true
    )
}

/** Users have no page yet: the tile only shows them. */
@Composable
fun UserCoverTile(user: MetadataUser, modifier: Modifier = Modifier) {
    CoverTile(
        title = user.displayName ?: user.username,
        subtitle = stringResource(Res.string.media_user),
        imageModel = user.thumbnails.firstOrNull()?.url,
        onClick = {},
        modifier = modifier,
        circle = true
    )
}

/** A single song: play starts it alone, the tile opens its album. */
@Composable
fun TrackCoverTile(
    track: MetadataTrack,
    modifier: Modifier = Modifier,
    topStart: (@Composable () -> Unit)? = null,
    audioPlayer: AudioPlayerInterface = koinInject(),
    audioPlayerQueue: AudioPlayerQueue = koinInject(),
    navigationCommands: NavigationCommands = koinInject(),
    remotePlaybackController: RemotePlaybackController = koinInject()
) {
    val scope = rememberCoroutineScope()
    val shellActions = LocalShellActions.current
    val currentEntry by audioPlayerQueue.currentQueueEntryFlow.collectAsStateWithLifecycle()
    val playerState by audioPlayer.playerStateFlow.collectAsStateWithLifecycle()
    val isCurrent = (currentEntry as? QueueEntry.StreamingTrack)?.track?.id == track.id
    val isPlaying = isCurrent && playerState == PlayerState.PLAYING
    val imageUrl = (track.album?.thumbnails ?: track.thumbnails)?.firstOrNull()?.url
    val addedMessage = stringResource(Res.string.pages_added_to_queue, track.title)

    CoverTile(
        title = track.title,
        subtitle = track.artists.joinToString { it.name },
        imageModel = imageUrl,
        modifier = modifier,
        onClick = { track.album?.id?.let { navigationCommands.navigateTo(Routes.Album(it)) } },
        onPlay = {
            scope.launch {
                if (isCurrent) {
                    if (isPlaying) audioPlayer.pause() else audioPlayer.play()
                } else {
                    audioPlayerQueue.load(
                        entries = listOf(QueueEntry.StreamingTrack(track = track, url = "")),
                        autoPlay = true,
                        startPosition = 0,
                        collectionEntry = null
                    )
                }
            }
        },
        onAddToQueue = {
            remotePlaybackController.requestTrackAddToQueue(track)
            shellActions.showToast(addedMessage, imageUrl)
        },
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        playingLabel = stringResource(Res.string.pages_playing),
        topStart = topStart
    )
}
