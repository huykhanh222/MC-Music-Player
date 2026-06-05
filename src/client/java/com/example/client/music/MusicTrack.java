package com.example.client.music;

import java.nio.file.Path;
import java.util.Objects;

public final class MusicTrack {
	private final Path path;
	private final String displayName;

	public MusicTrack(Path path, String displayName) {
		this.path = path;
		this.displayName = displayName;
	}

	public Path getPath() {
		return path;
	}

	public String getDisplayName() {
		return displayName;
	}

	@Override
	public String toString() {
		return displayName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(path, displayName);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MusicTrack)) return false;
		MusicTrack other = (MusicTrack) o;
		return Objects.equals(path, other.path) && Objects.equals(displayName, other.displayName);
	}
}
