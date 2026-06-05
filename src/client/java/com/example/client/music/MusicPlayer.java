package com.example.client.music;

import com.example.ExampleMod;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.CompoundControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class MusicPlayer {
	private static final int BUFFER_SIZE = 4096;

	private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r, "local-music-player");
		thread.setDaemon(true);
		return thread;
	});

	private final Object pauseLock = new Object();
	private volatile boolean playing;
	private volatile boolean paused;
	private volatile boolean stopRequested;
	private volatile int volumePercent = 80;
	private volatile SourceDataLine line;
	private volatile Future<?> currentTask;

	public synchronized void play(Path file, Runnable onEnded) {
		stop();
		playing = true;
		paused = false;
		stopRequested = false;
		currentTask = executor.submit(() -> playInternal(file, onEnded));
	}

	public synchronized boolean pause() {
		if (!playing || paused) {
			return false;
		}
		paused = true;
		if (line != null) {
			line.stop();
		}
		return true;
	}

	public synchronized boolean resume() {
		if (!playing || !paused) {
			return false;
		}
		paused = false;
		if (line != null) {
			line.start();
		}
		synchronized (pauseLock) {
			pauseLock.notifyAll();
		}
		return true;
	}

	public synchronized boolean stop() {
		if (!playing && currentTask == null) {
			return false;
		}
		stopRequested = true;
		paused = false;
		playing = false;
		if (currentTask != null) {
			currentTask.cancel(true);
			currentTask = null;
		}
		if (line != null) {
			line.stop();
			line.flush();
			line.close();
			line = null;
		}
		synchronized (pauseLock) {
			pauseLock.notifyAll();
		}
		return true;
	}

	public synchronized void setVolume(int percent) {
		volumePercent = Math.max(0, Math.min(100, percent));
		applyVolume();
	}

	public synchronized int getVolume() {
		return volumePercent;
	}

	public synchronized boolean isPlaying() {
		return playing;
	}

	public synchronized boolean isPaused() {
		return paused;
	}

	private void playInternal(Path file, Runnable onEnded) {
		try (AudioInputStream sourceStream = AudioSystem.getAudioInputStream(file.toFile())) {
			AudioFormat sourceFormat = sourceStream.getFormat();
			AudioFormat playableFormat = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,
				sourceFormat.getSampleRate(),
				16,
				sourceFormat.getChannels(),
				sourceFormat.getChannels() * 2,
				sourceFormat.getSampleRate(),
				false
			);

			try (AudioInputStream decodedStream = AudioSystem.getAudioInputStream(playableFormat, sourceStream)) {
				DataLine.Info info = new DataLine.Info(SourceDataLine.class, playableFormat);
				line = (SourceDataLine) AudioSystem.getLine(info);
				line.open(playableFormat);
				applyVolume();
				line.start();

				byte[] buffer = new byte[BUFFER_SIZE];
				int bytesRead;
				while (!stopRequested && (bytesRead = decodedStream.read(buffer, 0, buffer.length)) != -1) {
					waitWhilePaused();
					if (stopRequested) {
						break;
					}
					line.write(buffer, 0, bytesRead);
				}
				line.drain();
			}
		} catch (Exception exception) {
			ExampleMod.LOGGER.error("Unable to play music file", exception);
		} finally {
			closeLine();
			playing = false;
			paused = false;
			stopRequested = false;
			if (onEnded != null) {
				onEnded.run();
			}
		}
	}

	private void waitWhilePaused() {
		synchronized (pauseLock) {
			while (paused && !stopRequested) {
				try {
					pauseLock.wait();
				} catch (InterruptedException ignored) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	private void closeLine() {
		if (line != null) {
			try {
				line.stop();
				line.flush();
				line.close();
			} catch (Exception ignored) {
			}
			line = null;
		}
	}

	private void applyVolume() {
		if (line == null) {
			return;
		}
		if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
			FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
			float min = gain.getMinimum();
			float max = gain.getMaximum();
			float value = min + ((max - min) * volumePercent / 100f);
			gain.setValue(Math.max(min, Math.min(max, value)));
		} else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
			FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
			volumeControl.setValue(volumePercent / 100f);
		}
	}
}
