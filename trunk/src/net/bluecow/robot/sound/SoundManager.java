/*
 * Copyright (c) 2007, Jonathan Fuerth
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Jonathan Fuerth nor the names of other
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * Created on Mar 16, 2006
 *
 * This code belongs to Jonathan Fuerth.
 */
package net.bluecow.robot.sound;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import net.bluecow.robot.resource.ResourceLoader;
import net.bluecow.robot.sound.SoundManagerEntry.EntryType;

/**
 * The SoundManager handles loading, playing, looping, and stopping
 * audio in the robot game.
 *
 * <p>
 * Since this API is for a game, it makes sure audio failures are non-
 * critical.  For example, if a clip fails to load (and addClip throws an
 * exception), it is still safe to attempt to play, loop, and stop the
 * clip which has not been loaded.  These operations simply have no effect
 * except to log the failure to the system console.
 * 
 * @author fuerth
 * @version $Id$
 */
public class SoundManager {
    
    private static boolean debugOn = false;
    
    /**
     * Maps a clip name (which is supplied by the client code) to all of the
     * information about the corresponding audio clip.
     */
    private Map<String, SoundManagerEntry> clips = new HashMap<String, SoundManagerEntry>();
    
    private ResourceLoader resourceLoader;
    
    /**
     * Tracks whether this sound manager has been closed yet.  The best
     * way to check this variable at the beginning of a method is by
     * calling {@link #checkClosed()}.
     */
    private boolean closed = false;
    
    public SoundManager(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Adds a entry to the library, so that you can later use it with
     * {@link #play(String)}, {@link #loop(String)}, and {@link #stop(String)}.
     * 
     * @param name
     *            The name of the clip you want to add
     * @param data
     *            The resource path of the clip data. For simple sound files,
     *            WAV and AIFF file formats work; others may work if additional
     *            JavaSound service providers are installed. The SoundManager
     *            also recognizes XM, S3M, and MOD files, and can play those using a
     *            custom playback class.
     * 
     * @throws RuntimeException
     *             if the sound clip cannot be loaded for any reason.
     */
    public void addEntry(String name, EntryType type, String path) {
        try {
            if (type == EntryType.CLIP) {
                Line.Info linfo = new Line.Info(Clip.class);
                Line line;
                line = AudioSystem.getLine(linfo);
                Clip clip = (Clip) line;
                //clip.addLineListener(this);
                AudioInputStream ais = AudioSystem.getAudioInputStream(resourceLoader.getResourceAsStream(path));
                clip.open(ais);
                clips.put(name, new ClipEntry(name, path, clip));
            } else if (type == EntryType.MOD) {
                clips.put(name, new ModMusic(resourceLoader, name, path));
            } else {
                throw new IllegalArgumentException(
                        "Error loading entry \"" + name + "\" from path \"" + path + "\": " +
                        "SoundManager doesn't support entry type " + type);
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't load clip: "+e.getMessage());
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't load clip: "+e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't load clip: "+e.getMessage());
        }
    }
    
    /**
     * Closes all entries in this sound manager. Once the sound manager
     * has been closed, it can no longer be used.
     */
    public void close() {
        checkClosed();
        for (SoundManagerEntry sme : getClips()) {
            sme.close();
        }
    }
    
    /**
     * Throws an {@link IllegalStateException} if this sound manager has been
     * closed; does nothing if this sound manager is still open.
     */
    private void checkClosed() {
        if (closed) throw new IllegalStateException("This sound manager has been closed.");
    }
    
    /**
     * Plays a sound once through, from beginning to end. If this sound is
     * already playing, it will be restarted from the beginning. (XXX: for some
     * sound effects, it would be better if playback could be layered so many
     * instances of a sound could be played on top of each other. One idea for
     * this: indicate "maximum polyphony" for each sound as it's loaded, then
     * have a SoundManagerEntry wrapper class SoundManagerPolyphonicEntry. The
     * polyphonic entry could contain a number of entries and use them in a
     * round-robin manner so the playback won't be cut off unless the maximum
     * polyphony is exceeded)
     */
    public void play(String name) {
        checkClosed();
        playEntry(name, false);
    }

    /**
     * Jumps playback of the named entry to its named special ending tune.  For example,
     * sound manager entries that are background music might have a special "win" ending
     * that ends the song on a happy note. If the named entry is not currently playing,
     * calling this method has no effect.
     */
    public void stop(String name, String ending) {
        checkClosed();
        SoundManagerEntry c = clips.get(name);
        if (c == null) {
            System.out.println("Can't stop clip '"+name+"' because it doesn't exist");
            return;
        }
        if (debugOn) {
            System.out.println("Stopping clip "+name);
        }
        c.stopPlaying(ending);
    }
    
    /**
     * Stops playback of the named entry immediately (no special ending tune).
     * If the entry is not currently playing, calling this method has no effect.
     */
    public void stop(String name) {
        stop(name, null);
    }
    
    /**
     * Stops playback of all entries managed by this sound manager.
     */
    public void stopAll() {
        for (SoundManagerEntry sme : getClips()) {
            sme.stopPlaying(null);
        }
    }

    /**
     * Starts looping a clip from the beginning.  The clip will continue looping
     * until you stop it with {@link #stop(String)}.
     */
    public void loop(String name) {
        checkClosed();
        playEntry(name, true);
    }

    private void playEntry(String name, boolean loop) {
        SoundManagerEntry c = clips.get(name);
        if (c == null) {
            System.out.println("Can't play clip '"+name+"' because it doesn't exist");
            return;
        }
        if (debugOn) {
            System.out.println("Starting clip "+name+" (loop="+loop+")");
        }
        try {
            c.startPlaying(loop);
        } catch (LineUnavailableException ex) {
            System.err.println("Playback of clip " + name + " failed due to unavailable line");
            ex.printStackTrace();
        }
    }

    /**
     * Returns a list of all the clips in this sound manager.
     */
    public Collection<SoundManagerEntry> getClips() {
        return Collections.unmodifiableCollection(clips.values());
    }

}
