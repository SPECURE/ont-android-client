package at.specure.androidX.logging.timber;

import android.util.Log;

import timber.log.Timber;

public class NotLoggingTree extends Timber.Tree {

    @Override
    protected void log(final int priority, final String tag, final String message, final Throwable throwable) {

    }

}
