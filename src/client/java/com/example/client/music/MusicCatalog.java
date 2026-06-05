package com.example.client.music;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class MusicCatalog {
	private static final String[] SUPPORTED_EXTENSIONS = {"mp3", "ogg"};
	private final Path musicFolder;

	public MusicCatalog(Path musicFolder) {
		this.musicFolder = musicFolder;
	}

	public Path getMusicFolder() {
		return musicFolder;
	}

	public List<MusicTrack> scan() {
		try {
			Files.createDirectories(musicFolder);
		} catch (IOException exception) {
			return Collections.emptyList();
		}

		try (Stream<Path> files = Files.walk(musicFolder)) {
			List<MusicTrack> tracks = new ArrayList<>();
			files.filter(Files::isRegularFile)
				.filter(this::isSupported)
				.forEach(path -> tracks.add(new MusicTrack(path, normalizeDisplayName(musicFolder.relativize(path)))));
			tracks.sort(Comparator.comparing(MusicTrack::getDisplayName, String.CASE_INSENSITIVE_ORDER));
			return Collections.unmodifiableList(tracks);
		} catch (IOException exception) {
			return Collections.emptyList();
		}
	}

	private boolean isSupported(Path path) {
		String fileName = path.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex < 0) {
			return false;
		}
		String extension = fileName.substring(dotIndex + 1).toLowerCase();
		for (String supported : SUPPORTED_EXTENSIONS) {
			if (supported.equals(extension)) {
				return true;
			}
		}
		return false;
	}

	private String normalizeDisplayName(Path relativePath) {
		String displayName = relativePath.toString();
		return displayName.replace(File.separatorChar, '/');
	}
}
