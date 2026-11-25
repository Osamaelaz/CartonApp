package com.cartonapp.ui;

import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Modal with Make header, icon, bordered sections and footer actions. */
public final class AppModalDialog extends JDialog {
    private final JPanel body=new JPanel(); private final JPanel footer=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,10));
    public AppModalDialog(JFrame owner,String title,String iconName){super(owner,title,true);applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);setLayout(new BorderLayout());setBackground(Theme.CARD);add(header(title,iconName),BorderLayout.NORTH);body.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);body.setBackground(Theme.CARD);body.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));add(body);footer.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);footer.setBackground(Theme.SECTION);footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Theme.BORDER));add(footer,BorderLayout.SOUTH);setPreferredSize(new Dimension(570,500));pack();setLocationRelativeTo(owner);}
    private JPanel header(String title,String iconName){JPanel p=new JPanel(new BorderLayout()){@Override protected void paintComponent(Graphics g0){Graphics2D g=(Graphics2D)g0.create();g.setPaint(new GradientPaint(getWidth(),0,Theme.NAVY,0,0,Theme.NAVY_3));g.fillRect(0,0,getWidth(),getHeight());g.dispose();}};p.setPreferredSize(new Dimension(1,56));p.setBorder(BorderFactory.createMatteBorder(0,0,3,0,Theme.BABY));JPanel heading=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,12));heading.setOpaque(false);JLabel icon=new JLabel(Icons.icon(iconName,18,Theme.NAVY));icon.setOpaque(true);icon.setBackground(Theme.BABY);icon.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));heading.add(icon);JLabel text=new JLabel(title);text.setFont(Theme.font(Font.BOLD,15));text.setForeground(Color.WHITE);heading.add(text);p.add(heading,BorderLayout.EAST);return p;}
    public JPanel body(){return body;} public JPanel section(String title){JPanel p=new JPanel(new BorderLayout());p.setBackground(Theme.CARD);p.setBorder(BorderFactory.createLineBorder(Theme.BORDER));JLabel h=new JLabel(title);h.setOpaque(true);h.setBackground(Theme.SECTION);h.setForeground(Theme.NAVY);h.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,4,Theme.PRIMARY),BorderFactory.createEmptyBorder(10,12,10,12)));h.setFont(Theme.font(Font.BOLD,12));p.add(h,BorderLayout.NORTH);return p;} public void addFooterButton(JButton b){footer.add(b);}
}
