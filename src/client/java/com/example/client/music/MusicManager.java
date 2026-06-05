package com.example.client.music;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class MusicManager {
	private static final MusicManager INSTANCE = new MusicManager();

	private final MusicCatalog catalog;
	private final MusicPlayer player = new MusicPlayer();
	private List<MusicTrack> tracks = Collections.emptyList();
	private int currentIndex = -1;

	private MusicManager() {
		Path musicDirectory = FabricLoader.getInstance().getGameDir().resolve("music");
		this.catalog = new MusicCatalog(musicDirectory);
		scan();
	}

	public static MusicManager getInstance() {
		return INSTANCE;
	}

	public void scan() {
		List<MusicTrack> found = catalog.scan();
		synchronized (this) {
			this.tracks = new ArrayList<>(found);
			if (currentIndex >= tracks.size()) {
				currentIndex = -1;
			}
		}
	}

	public Path getMusicFolder() {
		return catalog.getMusicFolder();
	}

	public synchronized List<MusicTrack> getTracks() {
		return Collections.unmodifiableList(tracks);
	}

	public synchronized String listTracks() {
		if (tracks.isEmpty()) {
			return "No music files found in " + getMusicFolder();
		}
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tracks.size(); i++) {
			builder.append(i + 1).append(". ").append(tracks.get(i).getDisplayName());
			if (i < tracks.size() - 1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}

	public synchronized String play(String query) {
		if (tracks.isEmpty()) {
			return "No music loaded. Put mp3 or ogg files into " + getMusicFolder() + " and run /music reload.";
		}
		int index = findTrackIndex(query);
		if (index < 0) {
			return "Track not found: " + query;
		}
		currentIndex = index;
		MusicTrack track = tracks.get(index);
		player.setVolume(player.getVolume());
		player.play(track.getPath(), this::playNextAfterEnd);
		return "Playing " + track.getDisplayName();
	}

	public synchronized String stop() {
		if (player.stop()) {
			return "Music stopped.";
		}
		return "No music is playing.";
	}

	public synchronized String pause() {
		if (player.pause()) {
			return "Music paused.";
		}
		return "Music is not playing or already paused.";
	}

	public synchronized String resume() {
		if (player.resume()) {
			return "Music resumed.";
		}
		return "Music is not paused.";
	}

	public synchronized String next() {
		if (tracks.isEmpty()) {
			return "No tracks available.";
		}
		if (currentIndex < 0 || currentIndex >= tracks.size() - 1) {
			currentIndex = 0;
		} else {
			currentIndex++;
		}
		MusicTrack nextTrack = tracks.get(currentIndex);
		player.play(nextTrack.getPath(), this::playNextAfterEnd);
		return "Playing next track: " + nextTrack.getDisplayName();
	}

	public synchronized String previous() {
		if (tracks.isEmpty()) {
			return "No tracks available.";
		}
		if (currentIndex <= 0) {
			currentIndex = tracks.size() - 1;
		} else {
			currentIndex--;
		}
		MusicTrack previousTrack = tracks.get(currentIndex);
		player.play(previousTrack.getPath(), this::playNextAfterEnd);
		return "Playing previous track: " + previousTrack.getDisplayName();
	}

	public synchronized String setVolume(int percent) {
		player.setVolume(percent);
		return "Volume set to " + percent + "%.";
	}

	public synchronized String reload() {
		scan();
		if (tracks.isEmpty()) {
			return "Reload complete. No tracks found in " + getMusicFolder();
		}
		return "Reload complete. " + tracks.size() + " track(s) found.";
	}

	public synchronized String now() {
		if (currentIndex < 0 || currentIndex >= tracks.size()) {
			return "No track is currently playing.";
		}
		MusicTrack track = tracks.get(currentIndex);
		String status = player.isPaused() ? "paused" : player.isPlaying() ? "playing" : "stopped";
		return "Current track: " + track.getDisplayName() + " (" + status + ")";
	}

	public synchronized int getVolume() {
		return player.getVolume();
	}

	private int findTrackIndex(String query) {
		if (query == null) {
			return -1;
		}
		String normalized = query.trim();
		if (normalized.isEmpty()) {
			return -1;
		}
		try {
			int requested = Integer.parseInt(normalized);
			if (requested >= 1 && requested <= tracks.size()) {
				return requested - 1;
			}
		} catch (NumberFormatException ignored) {
		}
		String lowerCase = normalized.toLowerCase();
		for (int index = 0; index < tracks.size(); index++) {
			MusicTrack track = tracks.get(index);
			if (track.getDisplayName().equalsIgnoreCase(normalized) || track.getDisplayName().toLowerCase().contains(lowerCase)) {
				return index;
			}
		}
		return -1;
	}

	private void playNextAfterEnd() {
		// Placeholder for future playlist behavior.
	}
}
