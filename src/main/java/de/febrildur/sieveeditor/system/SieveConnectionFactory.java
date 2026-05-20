package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import com.fluffypeople.managesieve.ManageSieveClient;

public interface SieveConnectionFactory {
    ManageSieveClient create();
}
