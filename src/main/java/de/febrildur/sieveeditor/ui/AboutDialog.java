package de.febrildur.sieveeditor.ui;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class AboutDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public static final String APP_NAME = "SieveEditor";
    public static final String APP_VERSION = "1.3.1-SNAPSHOT";
    public static final String APP_DESCRIPTION = "Desktop application for editing and managing Sieve mail filter scripts on ManageSieve servers";
    public static final String COPYRIGHT = "Copyright (c) 2025 SieveEditor contributors";
    public static final String LICENSE_INFO = "Licensed under the Apache License, Version 2.0";
    public static final String GITHUB_URL = "https://github.com/lenucksi/SieveEditor";

    public static final String[][] DEPENDENCIES = {
        {"RSyntaxTextArea", "com.fifesoft:rsyntaxtextarea:3.6.2", "BSD 3-Clause"},
        {"FlatLaf", "com.formdev:flatlaf:3.7.1", "Apache 2.0"},
        {"ManageSieveJ", "com.github.lenucksi:ManageSieveJ:managesievej-v0.3.12", "Apache 2.0"},
        {"Jasypt", "org.jasypt:jasypt:1.9.3", "Apache 2.0"},
        {"Commons Codec", "commons-codec:commons-codec:1.22.0", "Apache 2.0"},
        {"KeePassXC Proxy Access", "org.purejava:keepassxc-proxy-access:1.3.1", "Apache 2.0"},
        {"java-keyring", "com.github.javakeyring:java-keyring:1.0.4", "MIT"},
        {"appdirs", "net.harawata:appdirs:1.5.0", "Apache 2.0"},
        {"SLF4J Simple", "org.slf4j:slf4j-simple:2.0.18", "MIT"},
    };

    public AboutDialog(JFrame parent) {
        super(parent, "About " + APP_NAME, true);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel headerPanel = new JPanel(new BorderLayout(5, 5));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel(APP_NAME + " " + APP_VERSION);
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea descText = new JTextArea(APP_DESCRIPTION + "\n\n" + LICENSE_INFO + "\n" + COPYRIGHT);
        descText.setEditable(false);
        descText.setOpaque(false);
        descText.setWrapStyleWord(true);
        descText.setLineWrap(true);
        descText.setFont(descText.getFont().deriveFont(12f));
        headerPanel.add(descText, BorderLayout.CENTER);

        add(headerPanel, BorderLayout.NORTH);

        StringBuilder depsText = new StringBuilder();
        depsText.append("Third-Party Dependencies\n");
        depsText.append("────────────────────────\n\n");
        for (String[] dep : DEPENDENCIES) {
            depsText.append("• ").append(dep[0]).append("\n");
            depsText.append("  ").append(dep[1]).append("\n");
            depsText.append("  License: ").append(dep[2]).append("\n\n");
        }
        depsText.append("Creator Credits\n");
        depsText.append("──────────────\n\n");
        depsText.append("Created by the SieveEditor community\n\n");
        depsText.append("GitHub: ").append(GITHUB_URL);

        JTextArea contentArea = new JTextArea(depsText.toString());
        contentArea.setEditable(false);
        contentArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        contentArea.setLineWrap(true);
        contentArea.setWrapStyleWord(true);
        contentArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(contentArea);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 10, 0, 10),
            BorderFactory.createTitledBorder("Credits & Licenses")
        ));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JButton githubButton = new JButton("Open on GitHub");
        githubButton.addActionListener(e -> openGitHub());

        buttonPanel.add(githubButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(closeButton);

        pack();
        setMinimumSize(new java.awt.Dimension(500, 400));
        if (getWidth() < 500) {
            setSize(500, getHeight());
        }
        if (getHeight() < 400) {
            setSize(getWidth(), 400);
        }
        setResizable(true);
        setLocationRelativeTo(getParent());
    }

    private void openGitHub() {
        try {
            Desktop.getDesktop().browse(new java.net.URI(GITHUB_URL));
        } catch (Exception ex) {
            // Ignore if desktop browse is not supported
        }
    }

    public static void showAboutDialog(Component parent) {
        JFrame frame = null;
        if (parent != null) {
            frame = (JFrame) SwingUtilities.getWindowAncestor(parent);
        }
        AboutDialog dialog = new AboutDialog(frame);
        dialog.setVisible(true);
    }
}
