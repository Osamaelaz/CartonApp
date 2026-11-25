package com.cartonapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Labelled text/combo/date field with optional error message. */
public final class FormField extends JPanel {
    private final JLabel validation=new JLabel();
    private final JTextField input;
    public FormField(String label,String value,String suffix){this(label,value,suffix,false,false);}
    public FormField(String label,String value,String suffix,boolean disabled,boolean placeholder){ super(new BorderLayout(0,3));applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);setOpaque(false);JLabel name=new JLabel(label);name.setHorizontalAlignment(JLabel.RIGHT);name.setFont(Theme.font(Font.BOLD,11));name.setForeground(Theme.LABEL_TEXT);add(name,BorderLayout.NORTH);JPanel field=new JPanel(new BorderLayout());field.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);field.setPreferredSize(new Dimension(1,Theme.FIELD_HEIGHT));field.setMinimumSize(new Dimension(1,Theme.FIELD_HEIGHT));field.setBackground(disabled?new Color(0xEC,0xF2,0xF9):Theme.FIELD);field.setBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER));input=new JTextField(placeholder?"":value);input.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);input.setFont(Theme.font(Font.PLAIN,12));input.setHorizontalAlignment(JTextField.CENTER);input.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));input.setEnabled(!disabled);javax.swing.Icon fieldIcon=FieldIcons.iconFor(label,16);if(fieldIcon!=null)input.putClientProperty("JTextField.leadingIcon",fieldIcon);input.setDisabledTextColor(Theme.NOTE_TEXT);if(placeholder)input.putClientProperty("JTextField.placeholderText",value);field.add(input);if(suffix!=null){JLabel end=new JLabel(suffix,JLabel.CENTER);end.setFont(Theme.font(Font.BOLD,11));end.setForeground(Theme.MUTED_TEXT);end.setOpaque(true);end.setBackground(new Color(0xF4,0xF6,0xF8));end.setBorder(BorderFactory.createMatteBorder(0,1,0,0,new Color(0xD4,0xDB,0xE3)));field.add(end,BorderLayout.WEST);}add(field,BorderLayout.CENTER);validation.setHorizontalAlignment(JLabel.RIGHT);validation.setFont(Theme.font(Font.PLAIN,10));validation.setForeground(Theme.ERROR);validation.setPreferredSize(new Dimension(1,13));validation.setMinimumSize(new Dimension(1,13));validation.setMaximumSize(new Dimension(Integer.MAX_VALUE,13));validation.setText(" ");validation.setVisible(true);add(validation,BorderLayout.SOUTH); }
    public void showValidation(String message){validation.setText(message);validation.setVisible(true);}
    public boolean isBlank(){return input.getText().trim().isEmpty();}
    public String text(){return input.getText().trim();}
    public void setText(String value){input.setText(value==null?"":value);}
    public JTextField getInput(){return input;}
    public void setReadOnly(){input.setEditable(false);input.setBackground(new Color(0xF1,0xF3,0xF5));input.setDisabledTextColor(new Color(0x2E,0x3A,0x48));}
    public static JComboBox<String> combo(String... values){JComboBox<String> c=new JComboBox<>(values);c.applyComponentOrientation(java.awt.ComponentOrientation.RIGHT_TO_LEFT);c.setFont(Theme.font(Font.PLAIN,12));c.setPreferredSize(new Dimension(1,Theme.FIELD_HEIGHT));c.setMinimumSize(new Dimension(1,Theme.FIELD_HEIGHT));c.setBackground(Theme.FIELD);c.setBorder(BorderFactory.createLineBorder(Theme.FIELD_BORDER));c.setRenderer(new javax.swing.plaf.basic.BasicComboBoxRenderer(){ {setHorizontalAlignment(JLabel.CENTER);setBorder(BorderFactory.createEmptyBorder(0,10,0,10));} });return c;}
}
