package com.huhx0015.hxaudio.audio;

import android.content.Context;
import android.util.Log;
import com.huhx0015.hxaudio.builder.HXMusicBuilder;
import com.huhx0015.hxaudio.interfaces.HXMusicEngineListener;
import com.huhx0015.hxaudio.interfaces.HXMusicListener;
import com.huhx0015.hxaudio.model.HXMusicItem;

/** -----------------------------------------------------------------------------------------------
 *  [HXMusic] CLASS
 *  DEVELOPER: Michael Yoon Huh (Huh X0015)
 *  DESCRIPTION: HXMusic class is a singleton class that handles state and directs actions to the
 *  HXMusicEngine class and also listens for MediaPlayer events coming from HXMusicEngine.
 *  -----------------------------------------------------------------------------------------------
 */

public class HXMusic implements HXMusicEngineListener {

    /** CLASS VARIABLES ________________________________________________________________________ **/

    // AUDIO VARIABLES:
    private boolean isGapless; // Used to determine if gapless mode has been enabled or not.
    private boolean isLooped; // Used to determine if the current music has looping enabled or not.
    private int musicPosition; // Used for tracking the current music position.
    private HXMusicEngine hxMusicEngine; // Responsible for the control and playback of the MediaPlayer object.
    private HXMusicItem hxMusicItem; // References the current HXMusicItem that stores information about the current music.
    private HXMusicStatus hxMusicStatus = HXMusicStatus.NOT_READY; // Used to determine the current status of the music.

    // INSTANCE VARIABLES:
    private static HXMusic hxMusic; // Instance variable for HXMusic.

    // LISTENER VARIABLES:
    private HXMusicListener musicListener; // Interface for listening for events from the MediaPlayer object.

    // LOGGING VARIABLES:
    private static final String LOG_TAG = HXMusic.class.getSimpleName(); // Used for logging output to logcat.

    /** ENUM ___________________________________________________________________________________ **/

    private enum HXMusicStatus {
        NOT_READY,
        READY,
        PLAYING,
        PAUSED,
        STOPPED,
        DISABLED
    }

    /** INSTANCE METHOD ________________________________________________________________________ **/

    // instance(): Returns the hxMusic instance.
    public static HXMusic instance() {
        if (hxMusic == null) {
            hxMusic = new HXMusic();
        }
        return hxMusic;
    }

    /** BUILDER METHOD _________________________________________________________________________ **/

    // music(): The main builder method used for constructing a HXMusicBuilder object for use with
    // the HXMusic class.
    public static HXMusicBuilder music() {
        instance();
        return new HXMusicBuilder();
    }

    /** INITIALIZATION METHODS _________________________________________________________________ **/

    // initMusic(): Prepares the MediaPlayer objects for music playback with the specified music
    // parameters.
    public synchronized void initMusic(HXMusicItem music, int position, boolean isGapless,
                                       boolean isLooped, Context context) {

        // Checks the current music status to determine if the specified music can be played or not.
        if (checkStatus(music)) {

            this.hxMusicItem = music;
            this.musicPosition = position;
            this.isGapless = isGapless;
            this.isLooped = isLooped;

            // Creates a new HXMusicEngine object if not initialized.
            if (hxMusicEngine == null) {
                hxMusicEngine = new HXMusicEngine();
                hxMusicEngine.setListener(this);
            }

            // Readies the HXMusicEngine for the music.
            hxMusicEngine.initMusicEngine(music, position, isGapless, isLooped, context);
        }
    }

    // checkStatus(): Verifies if the HXMusicItem object is valid and is used to determine if the
    // specified music can be played or not.
    private boolean checkStatus(HXMusicItem music) {

        if (hxMusicStatus.equals(HXMusicStatus.DISABLED)) {
            Log.e(LOG_TAG, "ERROR: checkStatus(): Music has been currently disabled.");
            return false;
        } else if (music == null) {
            Log.e(LOG_TAG, "ERROR: checkStatus(): Music item was null.");
            return false;
        } else if (music.getMusicResource() == 0 && music.getMusicUrl() == null) {
            Log.e(LOG_TAG, "ERROR: checkStatus(): No music resource or url was specified.");
            return false;
        } else if (hxMusicItem != null && (hxMusicItem.getMusicResource() == music.getMusicResource())) {
            if (hxMusicEngine != null && hxMusicEngine.isPlaying()) {
                Log.e(LOG_TAG, "ERROR: checkStatus(): Specified song is already playing!");
                return false;
            }
        }

        return true;
    }

    /** INTERFACE METHODS ______________________________________________________________________ **/

    // onMusicEnginePrepared(): Called by the HXMusicEngine when HXMusicEngine's MediaPlayer object
    // calls onPrepared().
    @Override
    public void onMusicEnginePrepared() {
        hxMusicStatus = HXMusicStatus.PLAYING;

        // Invokes the associated listener call.
        if (musicListener != null) {
            musicListener.onMusicPrepared(hxMusicItem);
        }
    }

    // onMusicEngineCompletion(): Called by the HXMusicEngine when HXMusicEngine's MediaPlayer object
    // calls onCompletion().
    @Override
    public void onMusicEngineCompletion() {
        hxMusicStatus = HXMusicStatus.STOPPED;

        // Invokes the associated listener call.
        if (musicListener != null) {
            musicListener.onMusicCompletion(hxMusicItem);
        }
    }

    // onMusicEngineBufferingUpdate(): Called by the HXMusicEngine when HXMusicEngine's MediaPlayer
    // object calls onBufferingUpdate().
    @Override
    public void onMusicEngineBufferingUpdate(int percent) {

        // Invokes the associated listener call.
        if (musicListener != null) {
            musicListener.onMusicBufferingUpdate(hxMusicItem, percent);
        }
    }

    /** MUSIC ACTION METHODS ___________________________________________________________________ **/

    // isPlaying(): Determines if a music is currently playing in the background.
    public static boolean isPlaying() {
        return hxMusic.hxMusicEngine != null && hxMusic.hxMusicEngine.isPlaying();
    }

    // pauseMusic(): Pauses any music playing in the background.
    public static void pauseMusic() {
        if (hxMusic != null && hxMusic.hxMusicEngine != null) {
            hxMusic.musicPosition = hxMusic.hxMusicEngine.pauseMusic();
            hxMusic.hxMusicStatus = HXMusicStatus.PAUSED;  // Indicates that the music is currently paused.

            // Invokes the associated listener call.
            if (hxMusic.musicListener != null && hxMusic.hxMusicItem != null) {
                hxMusic.musicListener.onMusicPause(hxMusic.hxMusicItem);
            }
        }
    }

    // resumeMusic(): Resumes playback of the current music.
    public static boolean resumeMusic(Context context) {

        if (context == null || context.getApplicationContext() == null) {
            Log.e(LOG_TAG, "ERROR: resumeMusic(): Context cannot be null.");
            return false;
        } else if (hxMusic != null && hxMusic.hxMusicStatus.equals(HXMusicStatus.PAUSED) &&
                hxMusic.hxMusicEngine != null) {

            boolean resumeMusicState = hxMusic.hxMusicEngine.initMusicEngine(hxMusic.hxMusicItem,
                    hxMusic.musicPosition, hxMusic.isGapless, hxMusic.isLooped,
                    context.getApplicationContext());

            if (resumeMusicState) {

                // Invokes the associated listener call.
                if (hxMusic.musicListener != null && hxMusic.hxMusicItem != null) {
                    hxMusic.musicListener.onMusicResume(hxMusic.hxMusicItem);
                    return true;
                }
            }
        }

        Log.e(LOG_TAG, "ERROR: resumeMusic(): Music could not be resumed.");
        return false;
    }

    //  stopMusic(): Stops any music playing in the background.
    public static boolean stopMusic() {

        if (hxMusic != null && hxMusic.hxMusicEngine != null) {
            boolean stopMusicState = hxMusic.hxMusicEngine.stopMusic();

            if (stopMusicState) {
                hxMusic.hxMusicStatus = HXMusicStatus.STOPPED;

                // Invokes the associated listener call.
                if (hxMusic.musicListener != null && hxMusic.hxMusicItem != null) {
                    hxMusic.musicListener.onMusicStop(hxMusic.hxMusicItem);
                }

                return true;
            }
        }

        return false;
    }

    /** MUSIC HELPER METHODS ___________________________________________________________________ **/

    // clear(): Releases resources held by the MediaPlayer object and clears this object. This
    // method should be called when the singleton object is no longer in use.
    public static void clear() {
        if (hxMusic != null && hxMusic.hxMusicEngine != null) {
            hxMusic.hxMusicEngine.release();
        }
        hxMusic = null;
    }

    // enable(): Used for enabling and disabling music playback.
    public static void enable(boolean isEnabled) {
        instance();

        if (isEnabled) {
            hxMusic.hxMusicStatus = HXMusicStatus.READY;
        } else {
            hxMusic.hxMusicStatus = HXMusicStatus.DISABLED;
        }
    }

    // getPosition(): Returns the current music position.
    public static int getPosition() {
        if (hxMusic != null) {
            return hxMusic.musicPosition;
        } else {
            return 0;
        }
    }

    // getStatus(): Returns the current music status of this object.
    public static String getStatus() {
        if (hxMusic != null) {
            return hxMusic.hxMusicStatus.toString();
        } else {
            return HXMusicStatus.NOT_READY.toString();
        }
    }

    // setListener(): Sets the HXMusicListener interface for this class.
    public static void setListener(HXMusicListener listener) {
        instance();
        hxMusic.musicListener = listener;
    }
}
