package de.febrildur.sieveeditor.system;

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