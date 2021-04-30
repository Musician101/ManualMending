package io.musician101.manualmending.common;

public interface Reference {

    String ID = "manualmending";
    String NAME = "Manual Mending";

    interface Messages {

        String PREFIX = "[" + NAME + "] ";
        String MENDING_ITEM = PREFIX + "Mending item...";
        String MENDING_COMPLETE = PREFIX + " Mending complete.";
        String MENDING_ITEMS = PREFIX + "Mending items...";
        String CONFIG_RELOADED = PREFIX + "Config reloaded.";
        String CONFIG_LOAD_ERROR = PREFIX + "An error occurred while attempting to reload the config: %s";


    }

    interface Commands {

        String MEND = "mend";
        String ALL = "all";
        String RELOAD = "mendreload";
    }

    interface Permissions {

        String MEND = ID + ".mend";
        String MEND_ALL = MEND + ".all";
        String RELOAD = ID + ".reload";
    }
}
