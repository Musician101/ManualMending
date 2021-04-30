package io.musician101.manualmending.common;

import java.io.IOException;

public interface Config {

    boolean repairAll();

    void load() throws IOException;
}
