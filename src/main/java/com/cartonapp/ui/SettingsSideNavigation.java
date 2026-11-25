package com.cartonapp.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/** 205px settings side navigation matching the Make structure. */
public final class SettingsSideNavigation extends JPanel {
    private final Map<String,JButton> items=new LinkedHashMap<>(); private final Consumer<String> onSelection;
    public SettingsSideNavigation(Consumer<String> onSelection){super(new GridLayout(0,1,0,1));this.onSelection=onSelection;setBackground(Theme.CARD);setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),BorderFactory.createEmptyBorder(7,7,7,7)));}
    public void addItem(String id,String text){JButton b=new JButton(text);b.setHorizontalAlignment(JButton.RIGHT);b.setFont(Theme.font(Font.PLAIN,12));b.setForeground(Theme.MUTED_TEXT);b.setBackground(Theme.CARD);b.setBorder(BorderFactory.createMatteBorder(0,0,0,3,Theme.CARD));b.setFocusPainted(false);b.addActionListener(e->{select(id);onSelection.accept(id);});items.put(id,b);add(b);}
    public void select(String id){items.forEach((key,b)->{boolean active=key.equals(id);b.setBackground(active?new Color(0xED,0xF5,0xFD):Theme.CARD);b.setForeground(active?new Color(0x12,0x5D,0xA9):Theme.MUTED_TEXT);b.setFont(Theme.font(active?Font.BOLD:Font.PLAIN,12));b.setBorder(BorderFactory.createMatteBorder(0,0,0,3,active?Theme.PRIMARY:Theme.CARD));});}
}
