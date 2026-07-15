package de.febrildur.sieveeditor.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.HashSet;
import java.util.Set;

import org.fife.ui.rtextarea.LineNumberList;
import org.fife.ui.rtextarea.RTextArea;

public class ErrorLineNumberList extends LineNumberList {

    private final Set<Integer> errorLines = new HashSet<>();
    private static final Color ERROR_COLOR = new Color(180, 40, 40);

    public ErrorLineNumberList(RTextArea textArea) {
        super(textArea);
    }

    public void setErrorLines(Set<Integer> lines) {
        errorLines.clear();
        errorLines.addAll(lines);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (errorLines.isEmpty()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(ERROR_COLOR);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(getFont());

        FontRenderContext frc = g2d.getFontRenderContext();
        int textOffset = getPreferredWidthForCurrentLineCount()
            - g2d.getFontMetrics().stringWidth("9999") - 4;

        RTextArea textArea = this.textArea;
        int lineCount = textArea.getLineCount();
        int visibleTop = textArea.getVisibleRect().y;
        int visibleBottom = visibleTop + textArea.getVisibleRect().height;

        for (int line : errorLines) {
            if (line < 0 || line >= lineCount) {
                continue;
            }
            try {
                java.awt.Rectangle lineRect = textArea.modelToView(
                    textArea.getLineStartOffset(line));
                if (lineRect == null || lineRect.y + lineRect.height < visibleTop
                    || lineRect.y > visibleBottom) {
                    continue;
                }
                String text = String.valueOf(line + 1);
                GlyphVector gv = g2d.getFont().createGlyphVector(frc, text);
                g2d.drawGlyphVector(gv, textOffset,
                    lineRect.y + lineRect.height - 3);
            } catch (javax.swing.text.BadLocationException e) {
                // line might not exist anymore
            }
        }
        g2d.dispose();
    }

    private int getPreferredWidthForCurrentLineCount() {
        return getPreferredSize().width;
    }
}
