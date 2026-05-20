package de.febrildur.sieveeditor.system;

import com.fluffypeople.managesieve.ManageSieveClient;

public interface SieveConnectionFactory {
    ManageSieveClient create();
}
