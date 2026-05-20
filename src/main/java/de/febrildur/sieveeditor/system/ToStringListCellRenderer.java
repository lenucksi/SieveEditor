package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2019 Zwixx
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

public final class ToStringListCellRenderer implements ListCellRenderer {
	private final ListCellRenderer originalRenderer;
	private final ToString toString;

	public ToStringListCellRenderer(final ListCellRenderer originalRenderer, final ToString toString) {
		this.originalRenderer = originalRenderer;
		this.toString = toString;
	}

	public Component getListCellRendererComponent(final JList list, final Object value, final int index,
			final boolean isSelected, final boolean cellHasFocus) {
		return originalRenderer.getListCellRendererComponent(list, toString.toString(value), index, isSelected,
				cellHasFocus);
	}

}
