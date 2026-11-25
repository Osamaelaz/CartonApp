package com.cartonapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import com.formdev.flatlaf.FlatLightLaf;

/** Manual-review window for the shared design foundation. */
public final class ComponentsDemo {
    private ComponentsDemo() { }
    public static void main(String[] args) { SwingUtilities.invokeLater(ComponentsDemo::show); }
    private static void show() {
        FlatLightLaf.setup(); Theme.installDefaults(); JFrame frame=new JFrame("مراجعة المكونات");frame.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);frame.setSize(1366,900);
        JPanel root=new JPanel(new BorderLayout());root.setBackground(new Color(0xF1,0xF3,0xF6));TopNavigation nav=new TopNavigation(id->{});nav.select("customers");root.add(nav,BorderLayout.NORTH);
        JPanel content=new JPanel(new GridLayout(0,1,0,13));content.setBackground(new Color(0xF1,0xF3,0xF6));content.setBorder(BorderFactory.createEmptyBorder(18,28,30,28));
        JPanel metrics=new JPanel(new GridLayout(1,4,11,0));metrics.setOpaque(false);metrics.add(new MetricCard("إجمالي مرحّل هذا الشهر","527,340.00",MetricCard.Tone.BLUE));metrics.add(new MetricCard("المحصّل","342,589.50",MetricCard.Tone.GREEN));metrics.add(new MetricCard("المتبقي","184,750.50",MetricCard.Tone.ORANGE));metrics.add(new MetricCard("إجمالي العملاء","6",MetricCard.Tone.NEUTRAL));content.add(metrics);
        AppCard tableCard=new AppCard();JPanel filter=new JPanel();filter.setBackground(Theme.CARD);filter.setBorder(BorderFactory.createEmptyBorder(11,14,11,14));FormField search=new FormField("بحث","بحث بالاسم أو الكود",null);search.setPreferredSize(new java.awt.Dimension(340,58));filter.add(search);filter.add(new AppButton("+ عميل جديد",AppButton.Kind.PRIMARY));tableCard.add(filter,BorderLayout.NORTH);JTable table=new JTable(new DefaultTableModel(new Object[][]{{"C-001","الصفوة للأجهزة المنزلية","184,750.50"},{"C-002","النيل للصناعات الغذائية","72,400.00"}},new String[]{"الكود","الاسم","المديونية"}));TableStyle.apply(table);tableCard.add(new JScrollPane(table));PaginationBar pagination=new PaginationBar();pagination.update(2,0);tableCard.add(pagination,BorderLayout.SOUTH);content.add(tableCard);
        JPanel lower=new JPanel(new GridLayout(1,2,14,0));lower.setOpaque(false);SettingsSideNavigation side=new SettingsSideNavigation(id->{});for(String item:new String[]{"إعدادات التسعير","أنواع الورق","أنواع الفلوت","مكونات التسعير","الأمان وكلمة المرور","النسخة الاحتياطية"})side.addItem(item,item);side.select("إعدادات التسعير");lower.add(side);AppCard right=new AppCard();SubTabBar subtabs=new SubTabBar(id->{});subtabs.addTab("statement","كشف الحساب","3");subtabs.addTab("products","المنتجات","5");subtabs.addTab("invoices","الفواتير","12");subtabs.select("statement");right.add(subtabs,BorderLayout.NORTH);JPanel badges=new JPanel();badges.setBackground(Theme.CARD);for(StatusBadge.Tone tone:StatusBadge.Tone.values())badges.add(new StatusBadge(tone.name(),tone));badges.add(new AppButton("أساسي",AppButton.Kind.PRIMARY));badges.add(new AppButton("ثانوي",AppButton.Kind.SECONDARY));badges.add(new AppButton("خطر",AppButton.Kind.DANGER));right.add(badges);lower.add(right);content.add(lower);
        Toast toast=new Toast();toast.showMessage("تم حفظ البيانات بنجاح");content.add(toast);root.add(new JScrollPane(content),BorderLayout.CENTER);frame.setContentPane(root);frame.setLocationRelativeTo(null);frame.setVisible(true);
    }
}
